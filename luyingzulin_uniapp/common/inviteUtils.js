const PENDING_INVITE_CODE_KEY = 'camp-pending-invite-code';

export const maskPhone = (mobile = '') => {
	const text = String(mobile || '').trim();
	if (text.length < 7) return text;
	return `${text.slice(0, 3)}****${text.slice(-4)}`;
};

export const normalizeInviteCode = (inviteCode = '') => String(inviteCode || '').trim().toUpperCase();

/** 邀请码须含字母，避免把 scene/userId 等纯数字（如 1001）误判为邀请码 */
export const isValidInviteCodeFormat = (inviteCode = '') => {
	const normalized = normalizeInviteCode(inviteCode);
	if (!normalized || normalized.length < 4 || normalized.length > 16) {
		return false;
	}
	if (!/^[A-Z0-9-]+$/.test(normalized)) {
		return false;
	}
	return /[A-Z]/.test(normalized);
};

export const savePendingInviteCode = (inviteCode = '') => {
	const normalized = normalizeInviteCode(inviteCode);
	if (!isValidInviteCodeFormat(normalized)) {
		return '';
	}
	uni.setStorageSync(PENDING_INVITE_CODE_KEY, normalized);
	return normalized;
};

export const getPendingInviteCode = () => {
	const normalized = normalizeInviteCode(uni.getStorageSync(PENDING_INVITE_CODE_KEY) || '');
	if (!isValidInviteCodeFormat(normalized)) {
		if (normalized) {
			clearPendingInviteCode();
		}
		return '';
	}
	return normalized;
};

export const clearPendingInviteCode = () => {
	uni.removeStorageSync(PENDING_INVITE_CODE_KEY);
};

/** 解析小程序 scene 中的邀请码（ic=XXX 或 inviteCode=XXX） */
export const parseInviteScene = (scene = '') => {
	const raw = decodeURIComponent(String(scene || '').trim());
	if (!raw) return '';
	const kvMatch = raw.match(/(?:ic|inviteCode)=([A-Za-z0-9-]+)/i);
	if (kvMatch?.[1]) {
		const code = normalizeInviteCode(kvMatch[1]);
		return isValidInviteCodeFormat(code) ? code : '';
	}
	const bare = normalizeInviteCode(raw);
	return isValidInviteCodeFormat(bare) ? bare : '';
};

/** 从启动/页面参数中提取邀请码 */
export const extractInviteCodeFromOptions = (options = {}) => {
	if (!options || typeof options !== 'object') return '';
	const direct = options.inviteCode || options.invitecode;
	if (direct) {
		const code = normalizeInviteCode(direct);
		return isValidInviteCodeFormat(code) ? code : '';
	}
	if (options.scene) {
		const fromScene = parseInviteScene(options.scene);
		if (fromScene) return fromScene;
	}
	const query = options.query;
	if (query) {
		if (typeof query === 'string') {
			const params = new URLSearchParams(query.replace(/^\?/, ''));
			const fromQuery = params.get('inviteCode') || params.get('ic');
			if (fromQuery) {
				const code = normalizeInviteCode(fromQuery);
				return isValidInviteCodeFormat(code) ? code : '';
			}
		} else if (typeof query === 'object') {
			const fromQuery = query.inviteCode || query.ic;
			if (fromQuery) {
				const code = normalizeInviteCode(fromQuery);
				return isValidInviteCodeFormat(code) ? code : '';
			}
		}
	}
	return '';
};

/** 捕获并持久化邀请码（App 启动、分享进入、扫码进入） */
export const captureInviteFromLaunchOptions = (options = {}) => {
	let code = extractInviteCodeFromOptions(options);
	// #ifdef H5
	if (!code && typeof window !== 'undefined') {
		const hash = window.location.hash || '';
		const queryIndex = hash.indexOf('?');
		if (queryIndex >= 0) {
			const params = new URLSearchParams(hash.slice(queryIndex + 1));
			code = normalizeInviteCode(params.get('inviteCode') || params.get('ic') || '');
		}
		if (!code) {
			const searchParams = new URLSearchParams(window.location.search || '');
			code = normalizeInviteCode(searchParams.get('inviteCode') || searchParams.get('ic') || '');
		}
		if (code && !isValidInviteCodeFormat(code)) {
			code = '';
		}
	}
	// #endif
	if (code) {
		savePendingInviteCode(code);
	}
	return code;
};

/** 联调演示账号（种子数据） */
export const getDefaultDemoAccount = () => ({
	account: '13900001001',
	mobile: '13900001001',
	password: '123456'
});
