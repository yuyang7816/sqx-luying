/**
 * 后端 BizException / Result 业务码与前端 Toast 文案映射
 */

/** 业务成功码 */
export const SUCCESS_CODES = [0, 200];

/** 未登录 / token 失效 */
export const UNAUTHORIZED_CODES = [401, 50002];

/** App 端业务错误码（com.sqx.app.base.enums.AppError） */
export const APP_ERROR_MESSAGES = {
	350001: '手机号未注册',
	350002: '账户状态异常',
	350003: '手机号或密码错误',
	350004: '验证码不正确',
	350005: '手机号已注册',
	350006: '用户信息不存在',
	350007: '不支持的业务代码',
	350008: '信息不存在',
	350009: '短信模板不存在',
	450006: '不支持的短信平台',
	450007: '短信发送失败'
};

/**
 * 解析接口错误提示
 * @param {object} payload 接口返回体或 Error
 */
export const resolveErrorMessage = (payload = {}) => {
	if (!payload) {
		return '请求失败';
	}
	if (typeof payload === 'string') {
		return payload;
	}
	if (payload.message && !payload.code) {
		return payload.message;
	}
	const code = payload.code ?? payload.status;
	if (code !== undefined && APP_ERROR_MESSAGES[code]) {
		return APP_ERROR_MESSAGES[code];
	}
	return payload.msg || payload.message || '请求失败，请稍后重试';
};

export const isBusinessSuccess = (data) => SUCCESS_CODES.includes(Number(data?.code)) || SUCCESS_CODES.includes(Number(data?.status));

export const isUnauthorizedCode = (code) => UNAUTHORIZED_CODES.includes(Number(code));
