import { getH5BaseUrl } from '@/common/env';
import { resolveErrorMessage } from '@/common/errorCode';
import * as authApi from '@/common/api/auth';
import { sendPhoneCode, VERIFY_SCENE_BIZ_CODE } from '@/common/api/verifyCode';
import {
	clearPendingInviteCode,
	getDefaultDemoAccount,
	getPendingInviteCode,
	isValidInviteCodeFormat,
	maskPhone,
	normalizeInviteCode,
	savePendingInviteCode
} from '@/common/inviteUtils';
import { resolveAvatarUrl } from '@/common/constants/avatar';
import {
	clearPendingWxOpenId,
	getPendingWxOpenId,
	parseOAuthCallback,
	redirectToMpOAuth,
	savePendingWxOpenId,
	stripOAuthParamsFromUrl
} from '@/common/wxOAuth';
import { isOaOAuthEnabled } from '@/common/oaOAuthConfig';
import { buildLoginPageUrl } from '@/common/userValidators';

const AUTH_STORAGE_KEY = 'camp-auth-session';

const platformMap = {
	'mp-weixin': '微信小程序',
	'h5-wechat': '微信公众号',
	'h5': 'H5浏览器',
	'app': 'App'
};

const modeLabels = {
	password: '账号密码',
	code: '短信验证码',
	wechat: '微信快捷'
};

const defaultInvitePosterConfig = {
	brandName: '露营装备租赁',
	inviterText: '邀你一起露营',
	posterImage: ''
};

const clone = (value) => JSON.parse(JSON.stringify(value));

const resolvePhoneNo = (account = '', mobile = '') => {
	const keyword = String(mobile || account || '').trim();
	if (!keyword) {
		throw new Error('请输入手机号');
	}
	if (!/^1\d{10}$/.test(keyword)) {
		throw new Error('请使用手机号登录，当前接口不支持纯账号登录');
	}
	return keyword;
};

const resolveRegisterPlatform = () => {
	const platform = getAuthPlatform();
	return platformMap[platform] || 'H5浏览器';
};

const resolveSysPhone = () => {
	// #ifdef APP-PLUS
	// #ifdef APP-IOS
	return 2;
	// #endif
	// #ifdef APP-ANDROID
	return 1;
	// #endif
	// #endif
	return undefined;
};

/** 将后端 LoginVO 转为前端 session 结构 */
const buildSessionFromLoginVO = (loginVO, { mode, extra = {} }) => {
	const user = loginVO?.user || {};
	const phone = user.phone || '';
	const expireSeconds = Number(loginVO?.expire || 0);
	return {
		token: loginVO?.token || '',
		expiresAt: expireSeconds > 0 ? Date.now() + expireSeconds * 1000 : Date.now() + 2 * 60 * 60 * 1000,
		platform: getAuthPlatform(),
		platformLabel: getAuthPlatformLabel(),
		loginMode: mode,
		loginModeLabel: modeLabels[mode] || '快捷登录',
		userInfo: {
			userId: user.userId || 0,
			nickname: user.userName || user.nickname || '露营用户',
			avatar: resolveAvatarUrl(user.avatar),
			phone: maskPhone(phone),
			memberLabel: Number(user.isVip) === 1 ? 'VIP会员' : '普通会员',
			account: user.userName || phone,
			inviteCode: user.invitationCode || ''
		},
		loginAt: Date.now(),
		meta: {
			...extra,
			rawUser: user
		}
	};
};

const persistAuthStorage = (session) => {
	const user = session.meta?.rawUser || {};
	if (user.userId) {
		uni.setStorageSync('userId', user.userId);
	}
	if (user.phone) {
		uni.setStorageSync('phone', user.phone);
	}
	if (user.maOpenId || user.mpOpenId) {
		uni.setStorageSync('openId', user.maOpenId || user.mpOpenId);
	}
	if (user.mpOpenId) {
		uni.setStorageSync('mpOpenId', user.mpOpenId);
	}
	if (user.maOpenId) {
		uni.setStorageSync('maOpenId', user.maOpenId);
	}
};

export const getAuthPlatform = () => {
	// #ifdef MP-WEIXIN
	return 'mp-weixin';
	// #endif

	// #ifdef APP-PLUS
	return 'app';
	// #endif

	// #ifdef H5
	const ua = typeof navigator !== 'undefined' ? navigator.userAgent.toLowerCase() : '';
	return /micromessenger/.test(ua) ? 'h5-wechat' : 'h5';
	// #endif

	return 'h5';
};

export const getAuthPlatformLabel = () => platformMap[getAuthPlatform()] || '当前平台';

export const getLoginModes = () => {
	const platform = getAuthPlatform();
	const allModes = [
		{ key: 'password', label: '账号密码', hint: '支持手机号+密码登录。' },
		{ key: 'code', label: '验证码', hint: '验证码登录将请求服务端短信接口。' },
		{ key: 'wechat', label: '微信快捷', hint: '根据平台走小程序登录、公众号授权或 App 微信授权。' }
	];

	if (platform === 'h5') {
		return allModes.filter((item) => item.key !== 'wechat');
	}

	if (platform === 'mp-weixin') {
		return allModes.filter((item) => item.key === 'wechat');
	}

	if (platform === 'h5-wechat' && !isOaOAuthEnabled()) {
		return allModes.filter((item) => item.key !== 'wechat');
	}

	return allModes;
};

export const getDefaultLoginMode = () => {
	const platform = getAuthPlatform();
	if (platform === 'mp-weixin') {
		return 'wechat';
	}
	if (platform === 'h5-wechat' && isOaOAuthEnabled()) {
		return 'wechat';
	}
	return 'code';
};

export const getLoginHint = (mode) => {
	const platform = getAuthPlatform();
	if (mode === 'wechat') {
		if (platform === 'mp-weixin') return '小程序端使用 uni.login 获取 code，再请求 /app/base/auth/wxLogin。';
		if (platform === 'h5-wechat') return '微信内置浏览器将对接公众号 OAuth2 授权。';
		if (platform === 'app') return 'App 端将对接微信 SDK 授权登录。';
		return '当前浏览器不支持微信快捷登录，请改用验证码或账号密码。';
	}

	if (mode === 'code') {
		return '验证码将通过 /app/base/verifyCode/sendPhoneCode 发送。';
	}
	return '请使用已注册手机号与密码登录。';
};

export const getStoredSession = () => uni.getStorageSync(AUTH_STORAGE_KEY) || null;

export const isLoggedIn = () => {
	const session = getStoredSession();
	return Boolean(session?.token && (!session.expiresAt || session.expiresAt > Date.now()));
};

export const saveSession = (session) => {
	uni.setStorageSync(AUTH_STORAGE_KEY, session);
	if (session?.token) {
		uni.setStorageSync('token', session.token);
	}
	persistAuthStorage(session);
	return session;
};

/** 将 /app/user 资料 VO 合并回本地 session */
export const syncSessionFromAppUserInfo = (vo = {}) => {
	const session = getStoredSession();
	if (!session) return null;
	const nextSession = {
		...session,
		userInfo: {
			...session.userInfo,
			userId: vo.userId || session.userInfo?.userId,
			nickname: vo.userName || session.userInfo?.nickname,
			avatar: resolveAvatarUrl(vo.avatar || session.userInfo?.avatar),
			phone: vo.phoneMasked || session.userInfo?.phone,
			memberLabel: Number(vo.isVip) === 1 ? 'VIP会员' : (vo.userTypeLabel || session.userInfo?.memberLabel || '普通会员'),
			account: vo.userName || session.userInfo?.account,
			inviteCode: vo.invitationCode || session.userInfo?.inviteCode || ''
		},
		meta: {
			...session.meta,
			rawUser: {
				...(session.meta?.rawUser || {}),
				userId: vo.userId,
				userName: vo.userName,
				avatar: vo.avatar,
				sex: vo.sex,
				age: vo.age,
				isVip: vo.isVip,
				invitationCode: vo.invitationCode
			}
		}
	};
	return saveSession(nextSession);
};

export const clearSession = () => {
	uni.removeStorageSync(AUTH_STORAGE_KEY);
	uni.removeStorageSync('token');
	uni.removeStorageSync('userId');
	uni.removeStorageSync('phone');
	uni.removeStorageSync('openId');
};

/** 校验本地 token 是否过期，过期则清理 */
export const refreshSession = () => {
	const session = getStoredSession();
	if (!session?.token) {
		return null;
	}
	if (session.expiresAt && session.expiresAt <= Date.now()) {
		clearSession();
		return null;
	}
	return session;
};

export const logout = async () => {
	clearSession();
	return true;
};

export { getDefaultDemoAccount };

export const sendAuthCode = async ({ mobile, scene = 'login' }) => {
	const bizCode = VERIFY_SCENE_BIZ_CODE[scene] || VERIFY_SCENE_BIZ_CODE.login;
	return sendPhoneCode({ phone: mobile, bizCode });
};

export { savePendingInviteCode, getPendingInviteCode, clearPendingInviteCode };

export const getInviteCodeInfo = async (inviteCode = '') => {
	const normalized = normalizeInviteCode(inviteCode);
	if (!normalized || !isValidInviteCodeFormat(normalized)) return null;
	try {
		const vo = await authApi.fetchInviteCodeInfo(normalized);
		return {
			inviteCode: vo.inviteCode || normalized,
			inviterName: vo.inviterName || '好友邀请',
			inviterId: vo.inviterId || 0,
			inviterMobile: vo.inviterMobile || ''
		};
	} catch (error) {
		return null;
	}
};

export const buildInviteRegisterPath = (inviteCode) => `/packageUser/register?inviteCode=${encodeURIComponent(normalizeInviteCode(inviteCode))}`;

export const buildInviteRegisterUrl = (inviteCode) => {
	const normalized = normalizeInviteCode(inviteCode);
	return `${getH5BaseUrl()}#/packageUser/register?inviteCode=${encodeURIComponent(normalized)}`;
};

export const buildInvitePromoText = (profile = {}, config = {}) => {
	const posterConfig = { ...defaultInvitePosterConfig, ...config };
	const nickname = profile.nickname || '露营达人';
	const inviteCode = profile.inviteCode || '--';
	const inviteUrl = profile.inviteUrl || buildInviteRegisterUrl(inviteCode);
	const inviterLine = posterConfig.inviterText
		? `${nickname} ${posterConfig.inviterText}`
		: `${nickname} 邀请你注册`;
	return [
		`${posterConfig.brandName} · 邀请你注册`,
		inviterLine,
		`邀请码：${inviteCode}`,
		'好友注册成功后消费，你可获得分销佣金',
		`注册链接：${inviteUrl}`
	].join('\n');
};

export const getInvitePosterConfig = () => clone(defaultInvitePosterConfig);

export const loginByPassword = async ({ account, mobile, password }) => {
	const phoneNo = resolvePhoneNo(account, mobile);
	try {
		const loginVO = await authApi.phonePasswordLogin({ phoneNo, password });
		return saveSession(buildSessionFromLoginVO(loginVO, { mode: 'password', extra: { account: phoneNo } }));
	} catch (error) {
		throw new Error(resolveErrorMessage(error));
	}
};

export const loginByCode = async ({ mobile, code }) => {
	try {
		const loginVO = await authApi.phoneCodeLogin({ phoneNo: mobile, verifyCode: code });
		return saveSession(buildSessionFromLoginVO(loginVO, { mode: 'code', extra: { account: mobile } }));
	} catch (error) {
		throw new Error(resolveErrorMessage(error));
	}
};

export const registerByPassword = async ({ mobile, code, password, confirmPassword, inviteCode = '' }) => {
	const formMobile = String(mobile || '').trim();
	if (!formMobile || !password || !confirmPassword || !code) {
		throw new Error('请完整填写注册信息');
	}
	if (!/^1\d{10}$/.test(formMobile)) {
		throw new Error('请输入有效手机号');
	}
	try {
		const loginVO = await authApi.phoneRegister({
			phoneNo: formMobile,
			code,
			username: formMobile,
			password,
			sysPhone: resolveSysPhone(),
			platform: resolveRegisterPlatform(),
			superiorInvitationCode: normalizeInviteCode(inviteCode)
		});
		clearPendingInviteCode();
		return saveSession(buildSessionFromLoginVO(loginVO, {
			mode: 'password',
			extra: { account: formMobile, scene: 'register', inviteCode }
		}));
	} catch (error) {
		throw new Error(resolveErrorMessage(error));
	}
};

export const resetPassword = async ({ mobile, code, password, confirmPassword }) => {
	if (password !== confirmPassword) {
		throw new Error('两次输入的密码不一致');
	}
	try {
		await authApi.resetPassword({
			phone: mobile,
			code,
			password
		});
		return true;
	} catch (error) {
		throw new Error(resolveErrorMessage(error));
	}
};

export const loginByWechat = async (options = {}) => {
	const platform = getAuthPlatform();

	if (platform === 'h5') {
		throw new Error('当前浏览器不支持微信快捷登录');
	}

	if (platform === 'h5-wechat') {
		if (!isOaOAuthEnabled()) {
			throw new Error('公众号授权登录未开启，请使用验证码或账号密码登录');
		}
		const callback = parseOAuthCallback();
		if (callback?.code) {
			const wxResult = await authApi.wxMpLogin(callback.code);
			stripOAuthParamsFromUrl();
			if (wxResult?.openid) {
				savePendingWxOpenId(wxResult.openid);
				uni.setStorageSync('mpOpenId', wxResult.openid);
			}
			if (wxResult?.bindPhone && wxResult?.loginVO) {
				clearPendingWxOpenId();
				return saveSession(buildSessionFromLoginVO(wxResult.loginVO, {
					mode: 'wechat',
					extra: { openid: wxResult.openid, unionid: wxResult.unionid, channel: 'mp' }
				}));
			}
			throw new Error('NEED_BIND_PHONE');
		}
		if (options.startOAuth !== false) {
			redirectToMpOAuth(options.redirectUri);
			return null;
		}
		throw new Error('请通过微信授权登录');
	}

	// #ifdef MP-WEIXIN
	const requestMpWxLogin = async (allowRetry = true) => {
		const loginRes = await new Promise((resolve, reject) => {
			uni.login({
				provider: 'weixin',
				success: resolve,
				fail: () => reject(new Error('小程序登录凭证获取失败'))
			});
		});
		if (!loginRes?.code) {
			throw new Error('小程序登录凭证获取失败');
		}
		try {
			return await authApi.wxLogin(loginRes.code, { silentError: true });
		} catch (error) {
			const message = error?.msg || error?.message || '';
			const shouldRetry = allowRetry && /凭证无效|已使用|invalid code|40163|40029/i.test(message);
			if (shouldRetry) {
				return requestMpWxLogin(false);
			}
			throw error;
		}
	};

	const wxResult = await requestMpWxLogin();
	if (wxResult?.openid) {
		savePendingWxOpenId(wxResult.openid);
		uni.setStorageSync('openId', wxResult.openid);
	}
	if (wxResult?.bindPhone && wxResult?.loginVO) {
		clearPendingWxOpenId();
		return saveSession(buildSessionFromLoginVO(wxResult.loginVO, {
			mode: 'wechat',
			extra: { openid: wxResult.openid, unionid: wxResult.unionid, channel: 'ma' }
		}));
	}
	throw new Error('NEED_BIND_PHONE');
	// #endif

	// #ifdef APP-PLUS
	const appLoginRes = await new Promise((resolve, reject) => {
		uni.login({
			provider: 'weixin',
			success: resolve,
			fail: () => reject(new Error('App 微信授权失败，请确认已集成微信 SDK'))
		});
	});
	if (appLoginRes?.code) {
		const wxResult = await authApi.wxLogin(appLoginRes.code);
		if (wxResult?.bindPhone && wxResult?.loginVO) {
			return saveSession(buildSessionFromLoginVO(wxResult.loginVO, {
				mode: 'wechat',
				extra: { openid: wxResult.openid, unionid: wxResult.unionid, channel: 'app' }
			}));
		}
	}
	throw new Error('App 微信登录需绑定手机号，请改用验证码登录');
	// #endif

	throw new Error('当前平台微信登录尚未对接');
};

/** 微信授权后绑定手机号并完成登录 */
export const registerByWechat = async ({ phone, phoneCode, nickname = '', inviteCode = '' } = {}) => {
	const openId = getPendingWxOpenId();
	if (!openId) {
		throw new Error('微信授权信息已失效，请重新发起微信登录');
	}
	const platform = getAuthPlatform();
	const isMpWeixin = platform === 'mp-weixin';
	if (!phoneCode && (!phone || !/^1\d{10}$/.test(String(phone).trim()))) {
		throw new Error(isMpWeixin ? '请授权微信手机号' : '请输入有效手机号');
	}
	const registerApi = platform === 'h5-wechat' ? authApi.wxMpRegister : authApi.wxJsRegister;
	const payload = {
		openId,
		username: nickname || (phone ? String(phone).trim() : ''),
		superiorInvitationCode: normalizeInviteCode(inviteCode)
	};
	if (phoneCode) {
		payload.phoneCode = phoneCode;
	} else {
		payload.phone = String(phone).trim();
	}
	const loginVO = await registerApi(payload);
	const boundPhone = loginVO?.user?.phone || phone || '';
	clearPendingWxOpenId();
	return saveSession(buildSessionFromLoginVO(loginVO, {
		mode: 'wechat',
		extra: { account: boundPhone, scene: 'wx-register', openId }
	}));
};

/** 尝试处理 H5 公众号 OAuth 回跳（在登录页 onLoad 调用） */
export const tryCompleteMpOAuthLogin = async () => {
	if (getAuthPlatform() !== 'h5-wechat') {
		return null;
	}
	if (!parseOAuthCallback()?.code) {
		return null;
	}
	return loginByWechat({ startOAuth: false });
};

export const ensureLogin = (redirectUrl) => {
	if (refreshSession()) return true;
	uni.navigateTo({ url: buildLoginPageUrl(redirectUrl) });
	return false;
};

export { maskPhone, normalizeInviteCode };
