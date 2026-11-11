import { postJson, resolveBusinessResponse } from '@/common/httpRequest';

const VERIFY_CODE_PREFIX = 'app/base/verifyCode';

/** 验证码场景与后端短信 bizCode 映射 */
export const VERIFY_SCENE_BIZ_CODE = {
	login: 'login',
	register: 'register',
	reset: 'forget'
};

/** 发送手机验证码（公开接口，无需 token） */
export function sendPhoneCode({ phone, bizCode }) {
	return postJson(`${VERIFY_CODE_PREFIX}/sendPhoneCode`, { phone, bizCode }, { hasToken: false }).then(resolveBusinessResponse);
}

export const verifyCodeApi = {
	sendPhoneCode: `${VERIFY_CODE_PREFIX}/sendPhoneCode`
};
