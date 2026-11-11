/** M05 用户账户表单校验与登录回跳 URL 工具 */

export const MOBILE_REGEX = /^1\d{10}$/;

export const TAB_BAR_PAGES = new Set([
	'/pages/index/index',
	'/pages/category/index',
	'/pages/cart/index',
	'/pages/my/my'
]);

/** 登录页协议勾选校验 */
export const validateLoginAgreement = (agreed) => {
	if (!agreed) {
		return '请先勾选协议';
	}
	return '';
};

/** 账号密码登录表单校验 */
export const validateLoginPasswordForm = ({ account = '', password = '' } = {}) => {
	if (!String(account).trim()) {
		return '请输入账号或手机号';
	}
	if (!String(password).trim()) {
		return '请输入密码';
	}
	return '';
};

/** 验证码登录表单校验 */
export const validateLoginCodeForm = ({ mobile = '', code = '' } = {}) => {
	if (!MOBILE_REGEX.test(String(mobile).trim())) {
		return '请输入正确手机号';
	}
	if (!String(code).trim()) {
		return '请输入验证码';
	}
	return '';
};

/** 地址保存手机号格式校验 */
export const validateAddressPhone = (phone = '') => {
	if (!MOBILE_REGEX.test(String(phone).trim())) {
		return '请输入正确手机号';
	}
	return '';
};

/** 地址保存完整字段校验（与 address-edit 页一致） */
export const validateAddressSave = ({ name = '', phone = '', province = '', city = '', district = '', detailAddress = '' } = {}) => {
	if (!name || !phone || !detailAddress) {
		return '请完整填写地址信息';
	}
	const phoneMessage = validateAddressPhone(phone);
	if (phoneMessage) {
		return phoneMessage;
	}
	if (!province || !city || !district) {
		return '请选择所在地区';
	}
	return '';
};

/** 解析登录页 onLoad 传入的 redirect 参数 */
export const parseLoginRedirect = (options = {}) => {
	return options.redirect ? decodeURIComponent(options.redirect) : '';
};

/** 构建登录页 URL（含 redirect 回跳） */
export const buildLoginPageUrl = (redirectUrl = '') => {
	const target = redirectUrl ? `?redirect=${encodeURIComponent(redirectUrl)}` : '';
	return `/packageUser/login${target}`;
};

/** 构建注册页 URL（保留 redirect 回跳） */
export const buildRegisterPageUrl = (redirectUrl = '') => {
	const target = redirectUrl ? `?redirect=${encodeURIComponent(redirectUrl)}` : '';
	return `/packageUser/register${target}`;
};

/** 判断是否为 tabBar 页面（switchTab 专用） */
export const isTabBarPage = (url = '') => {
	const pathOnly = String(url || '').split('?')[0];
	return TAB_BAR_PAGES.has(pathOnly);
};

/** 登录/注册成功后统一回「我的」页（忽略 redirect，避免误入个人资料等页） */
export const resolvePostLoginUrl = (_redirectUrl = '') => '/pages/my/my';

/** 登录/注册成功后跳转 */
export const navigateAfterAuth = (_redirectUrl = '') => {
	uni.switchTab({ url: '/pages/my/my' });
};

/** 登录提交前校验（按登录模式） */
export const validateLoginSubmit = ({ mode = 'password', agreed = true, account = '', password = '', mobile = '', code = '' } = {}) => {
	const agreementMessage = validateLoginAgreement(agreed);
	if (agreementMessage) {
		return agreementMessage;
	}
	if (mode === 'password') {
		return validateLoginPasswordForm({ account, password });
	}
	if (mode === 'code') {
		return validateLoginCodeForm({ mobile, code });
	}
	return '';
};
