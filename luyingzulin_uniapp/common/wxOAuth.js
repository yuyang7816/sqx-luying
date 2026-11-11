import { WX_MP_APPID } from '@/common/env';

const OAUTH_STATE_KEY = 'camp-wx-oauth-state';
const PENDING_WX_OPENID_KEY = 'camp-wx-pending-openid';

/** 是否微信内置浏览器 */
export const isWechatBrowser = () => {
	// #ifdef H5
	if (typeof navigator !== 'undefined') {
		return /micromessenger/i.test(navigator.userAgent);
	}
	// #endif
	return false;
};

/** 构建公众号 OAuth2 授权 URL（snsapi_userinfo） */
export const buildMpOAuthUrl = (redirectUri, state = '') => {
	if (!WX_MP_APPID) {
		throw new Error('未配置公众号 AppId（VITE_WX_MP_APPID）');
	}
	const normalizedRedirect = redirectUri || resolveCurrentOAuthRedirectUri();
	const oauthState = state || `camp_${Date.now()}`;
	uni.setStorageSync(OAUTH_STATE_KEY, oauthState);
	const params = [
		`appid=${encodeURIComponent(WX_MP_APPID)}`,
		`redirect_uri=${encodeURIComponent(normalizedRedirect)}`,
		'response_type=code',
		'scope=snsapi_userinfo',
		`state=${encodeURIComponent(oauthState)}`
	].join('&');
	return `https://open.weixin.qq.com/connect/oauth2/authorize?${params}#wechat_redirect`;
};

/** 当前 H5 页作为 OAuth 回跳地址（去掉 code/state 参数） */
export const resolveCurrentOAuthRedirectUri = () => {
	// #ifdef H5
	if (typeof window !== 'undefined' && window.location) {
		const url = new URL(window.location.href);
		url.searchParams.delete('code');
		url.searchParams.delete('state');
		return url.toString();
	}
	// #endif
	return '';
};

/** 从 URL 解析 OAuth 回调 code/state */
export const parseOAuthCallback = () => {
	// #ifdef H5
	if (typeof window === 'undefined' || !window.location) {
		return null;
	}
	const searchParams = new URLSearchParams(window.location.search || '');
	const hashQuery = (window.location.hash || '').split('?')[1] || '';
	const hashParams = new URLSearchParams(hashQuery);
	const code = searchParams.get('code') || hashParams.get('code');
	const state = searchParams.get('state') || hashParams.get('state');
	if (!code) {
		return null;
	}
	const expectedState = uni.getStorageSync(OAUTH_STATE_KEY);
	if (expectedState && state && expectedState !== state) {
		throw new Error('微信授权 state 校验失败');
	}
	uni.removeStorageSync(OAUTH_STATE_KEY);
	return { code, state };
	// #endif
	return null;
};

/** 清理 URL 中的 OAuth 参数，避免刷新重复登录 */
export const stripOAuthParamsFromUrl = () => {
	// #ifdef H5
	if (typeof window === 'undefined' || !window.history?.replaceState) {
		return;
	}
	const url = new URL(window.location.href);
	url.searchParams.delete('code');
	url.searchParams.delete('state');
	const hashParts = (url.hash || '').split('?');
	if (hashParts[1]) {
		const hashParams = new URLSearchParams(hashParts[1]);
		hashParams.delete('code');
		hashParams.delete('state');
		const rest = hashParams.toString();
		url.hash = rest ? `${hashParts[0]}?${rest}` : hashParts[0];
	}
	window.history.replaceState({}, document.title, url.toString());
	// #endif
};

export const savePendingWxOpenId = (openId) => {
	if (openId) {
		uni.setStorageSync(PENDING_WX_OPENID_KEY, openId);
	}
};

export const getPendingWxOpenId = () => uni.getStorageSync(PENDING_WX_OPENID_KEY) || '';

export const clearPendingWxOpenId = () => {
	uni.removeStorageSync(PENDING_WX_OPENID_KEY);
};

/** 跳转公众号 OAuth 授权 */
export const redirectToMpOAuth = (redirectUri) => {
	const url = buildMpOAuthUrl(redirectUri);
	// #ifdef H5
	window.location.href = url;
	return;
	// #endif
	throw new Error('当前平台不支持公众号 OAuth');
};
