import { get, postJson, resolveBusinessResponse } from '@/common/httpRequest';

const AUTH_PREFIX = 'app/base/auth';
/** 登录/注册/验证码等公开接口不携带 token */
const PUBLIC_OPTIONS = { hasToken: false };

/** 手机号密码登录 */
export function phonePasswordLogin({ phoneNo, password }) {
	return postJson(`${AUTH_PREFIX}/phonePasswordLogin`, { phoneNo, password }, PUBLIC_OPTIONS).then(resolveBusinessResponse);
}

/** 手机号验证码登录 */
export function phoneCodeLogin({ phoneNo, verifyCode }) {
	return postJson(`${AUTH_PREFIX}/phoneCodeLogin`, { phoneNo, verifyCode }, PUBLIC_OPTIONS).then(resolveBusinessResponse);
}

/** 手机号注册 */
export function phoneRegister(payload) {
	return postJson(`${AUTH_PREFIX}/phoneRegister`, payload, PUBLIC_OPTIONS).then(resolveBusinessResponse);
}

/** 忘记密码 */
export function resetPassword(payload) {
	return postJson(`${AUTH_PREFIX}/resetPassword`, payload, PUBLIC_OPTIONS).then(resolveBusinessResponse);
}

/** 微信小程序登录 */
export function wxLogin(code, options = {}) {
	return get(`${AUTH_PREFIX}/wxLogin`, { code }, { ...PUBLIC_OPTIONS, ...options }).then(resolveBusinessResponse);
}

/** 微信小程序注册绑定手机号 */
export function wxJsRegister(payload) {
	return postJson(`${AUTH_PREFIX}/wxJsRegister`, payload, PUBLIC_OPTIONS).then(resolveBusinessResponse);
}

/** 微信公众号 OAuth 登录 */
export function wxMpLogin(code) {
	return get(`${AUTH_PREFIX}/wxMpLogin`, { code }, PUBLIC_OPTIONS).then(resolveBusinessResponse);
}

/** 微信公众号注册绑定手机号 */
export function wxMpRegister(payload) {
	return postJson(`${AUTH_PREFIX}/wxMpRegister`, payload, PUBLIC_OPTIONS).then(resolveBusinessResponse);
}

/** 查询邀请码信息（失败时不弹全局 toast，由页面自行处理） */
export function fetchInviteCodeInfo(inviteCode) {
	return get(`${AUTH_PREFIX}/inviteCodeInfo`, { inviteCode }, { ...PUBLIC_OPTIONS, silentError: true }).then(resolveBusinessResponse);
}

/** 获取邀请小程序码 Base64 */
export function fetchInviteWxQrCode(inviteCode) {
	return get(`${AUTH_PREFIX}/inviteWxQrCode`, { inviteCode }, PUBLIC_OPTIONS).then(resolveBusinessResponse);
}

export const authApi = {
	phonePasswordLogin: `${AUTH_PREFIX}/phonePasswordLogin`,
	phoneCodeLogin: `${AUTH_PREFIX}/phoneCodeLogin`,
	phoneRegister: `${AUTH_PREFIX}/phoneRegister`,
	resetPassword: `${AUTH_PREFIX}/resetPassword`,
	wxLogin: `${AUTH_PREFIX}/wxLogin`,
	wxJsRegister: `${AUTH_PREFIX}/wxJsRegister`,
	wxMpLogin: `${AUTH_PREFIX}/wxMpLogin`,
	wxMpRegister: `${AUTH_PREFIX}/wxMpRegister`,
	inviteCodeInfo: `${AUTH_PREFIX}/inviteCodeInfo`,
	inviteWxQrCode: `${AUTH_PREFIX}/inviteWxQrCode`
};
