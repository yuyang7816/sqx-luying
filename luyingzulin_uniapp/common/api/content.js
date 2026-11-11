import { postJson, resolveBusinessResponse } from '@/common/httpRequest';
import { getServiceConfig, getActiveServiceChannel, getVisibleServiceChannels } from '@/common/serviceDefaults';

const CONFIG_KEYS = {
	userAgreement: 'app_user_agreement_html',
	privacyPolicy: 'app_privacy_policy_html',
	rentalAgreement: 'app_rental_agreement_html',
	depositAgreement: 'app_deposit_agreement_html',
	damageAgreement: 'app_damage_agreement_html',
	serviceChannels: 'app_service_channels_json',
	invitePoster: 'app_invite_poster_json',
	helpFaq: 'app_help_faq_json'
};

const CHANNEL_KEYS = ['onlineIm', 'wechat', 'phone', 'wecom'];

const defaultInvitePosterConfig = {
	brandName: '露营装备租赁',
	inviterText: '邀你一起露营',
	posterImage: ''
};

const mergeInvitePosterConfig = (raw = {}) => {
	const merged = { ...defaultInvitePosterConfig, ...(raw || {}) };
	return {
		brandName: merged.brandName || defaultInvitePosterConfig.brandName,
		inviterText: merged.inviterText || defaultInvitePosterConfig.inviterText,
		posterImage: merged.posterImage || ''
	};
};

const cloneJson = (value) => JSON.parse(JSON.stringify(value));

function resolveActiveChannel(raw = {}, channels = {}) {
	if (raw.activeChannel && CHANNEL_KEYS.includes(raw.activeChannel)) {
		return raw.activeChannel;
	}
	const enabledKey = CHANNEL_KEYS.find((key) => channels[key]?.enabled);
	return enabledKey || 'wechat';
}

function applyExclusiveChannels(channels = {}, activeChannel = 'wechat') {
	const next = cloneJson(channels);
	CHANNEL_KEYS.forEach((key) => {
		if (next[key]) {
			next[key].enabled = key === activeChannel;
		}
	});
	return next;
}

const mergeServiceChannels = (rawChannels = {}, defaults = {}) => {
	const channels = cloneJson(defaults);
	Object.keys(defaults).forEach((key) => {
		const item = rawChannels[key] || {};
		const fallback = defaults[key] || {};
		if (key === 'phone') {
			channels[key] = {
				...fallback,
				...item,
				phoneNumber: item.phoneNumber || item.value || fallback.phoneNumber,
				title: item.title || item.label || fallback.title
			};
			return;
		}
		if (key === 'wechat') {
			channels[key] = {
				...fallback,
				...item,
				wechatId: item.wechatId || item.value || fallback.wechatId,
				title: item.title || item.label || fallback.title
			};
			return;
		}
		channels[key] = { ...fallback, ...item };
	});
	return channels;
};

const mergeServiceConfig = (raw = {}) => {
	const defaults = getServiceConfig();
	const channels = mergeServiceChannels(raw.channels, defaults.channels);
	const activeChannel = resolveActiveChannel(raw, channels);
	return {
		activeChannel,
		channels: applyExclusiveChannels(channels, activeChannel)
	};
};

const AGREEMENT_TYPE_KEY_MAP = {
	user: CONFIG_KEYS.userAgreement,
	rental: CONFIG_KEYS.rentalAgreement,
	deposit: CONFIG_KEYS.depositAgreement,
	damage: CONFIG_KEYS.damageAgreement,
	privacy: CONFIG_KEYS.privacyPolicy
};

/** 解析展示城市：优先用户手动选择，否则 GPS 定位 */
export async function resolveAppCity(getSelectedCityFn, locateCurrentCityFn) {
	const selected = typeof getSelectedCityFn === 'function' ? getSelectedCityFn() : '';
	if (selected) return selected;

	if (typeof locateCurrentCityFn !== 'function') {
		throw new Error('无法获取当前城市');
	}

	const located = await locateCurrentCityFn();
	if (located) return located;

	throw new Error('无法获取当前城市');
}

/** 读取系统配置项 */
export async function fetchAppConfig(key) {
	const vo = await postJson('app/config/info', { key }).then(resolveBusinessResponse);
	return vo?.value ?? '';
}

/** 协议 HTML（rental/deposit/damage/privacy/user） */
export async function fetchAgreementHtml(type = 'user') {
	const key = AGREEMENT_TYPE_KEY_MAP[type] || CONFIG_KEYS.userAgreement;
	const html = await fetchAppConfig(key);
	return html || '';
}

/** 客服完整配置（渠道四选一） */
export async function fetchServiceConfigRemote() {
	try {
		const raw = await fetchAppConfig(CONFIG_KEYS.serviceChannels);
		const config = typeof raw === 'string' ? JSON.parse(raw) : raw;
		return mergeServiceConfig(config || {});
	} catch (error) {
		return getServiceConfig();
	}
}

/** 当前启用的客服渠道 */
export async function fetchServiceChannels() {
	try {
		const config = await fetchServiceConfigRemote();
		const channel = getActiveServiceChannel(config);
		return channel ? [channel] : [];
	} catch (error) {
		return getVisibleServiceChannels();
	}
}

/** 邀请海报配置 */
export async function fetchInvitePosterConfigRemote() {
	try {
		const raw = await fetchAppConfig(CONFIG_KEYS.invitePoster);
		const config = typeof raw === 'string' ? JSON.parse(raw) : raw;
		return mergeInvitePosterConfig(config || {});
	} catch (error) {
		return cloneJson(defaultInvitePosterConfig);
	}
}

/** 帮助页 FAQ */
export async function fetchHelpContent() {
	try {
		const raw = await fetchAppConfig(CONFIG_KEYS.helpFaq);
		return raw ? JSON.parse(raw) : null;
	} catch (error) {
		return null;
	}
}

export { CONFIG_KEYS };
