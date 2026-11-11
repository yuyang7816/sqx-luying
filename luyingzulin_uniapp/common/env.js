/**
 * uniapp 运行环境配置（M00 基础设施）
 * 对接线上服务端 https://luyingzl.xianmaxiong.com/sqx_fast/（本地联调见 .env.integration）
 */

const readViteEnv = (key) => {
	try {
		if (typeof import.meta !== 'undefined' && import.meta.env && import.meta.env[key] !== undefined) {
			return import.meta.env[key];
		}
	} catch (error) {
		// 非 Vite 构建环境忽略
	}
	return undefined;
};

const parseBoolean = (value, fallback) => {
	if (value === undefined || value === null || value === '') {
		return fallback;
	}
	if (typeof value === 'boolean') {
		return value;
	}
	const normalized = String(value).trim().toLowerCase();
	if (['true', '1', 'yes', 'on'].includes(normalized)) {
		return true;
	}
	if (['false', '0', 'no', 'off'].includes(normalized)) {
		return false;
	}
	return fallback;
};

const DEFAULT_API_BASE_URL = 'https://luyingzl.xianmaxiong.com/sqx_fast/';

/** @deprecated 已移除 Mock，始终对接真实接口 */
export const USE_MOCK = false;

/** 公众号 AppId（H5 微信内置浏览器 OAuth，可与支付 JSAPI AppId 相同） */
export const WX_MP_APPID = readViteEnv('VITE_WX_MP_APPID') || '';

/** API 基址，末尾带 / */
export const API_BASE_URL = (() => {
	const fromEnv = readViteEnv('VITE_API_BASE_URL') || DEFAULT_API_BASE_URL;
	return fromEnv.endsWith('/') ? fromEnv : `${fromEnv}/`;
})();

/** H5 站点基址（邀请链接、海报二维码），末尾不带 / */
export const H5_BASE_URL = (() => {
	const fromEnv = readViteEnv('VITE_H5_BASE_URL');
	if (fromEnv) {
		return String(fromEnv).replace(/\/+$/, '');
	}
	// #ifdef H5
	if (typeof window !== 'undefined' && window.location?.origin) {
		return window.location.origin;
	}
	// #endif
	return 'https://luyingzl.xianmaxiong.com';
})();

export const getApiBaseUrl = () => API_BASE_URL;
export const getH5BaseUrl = () => H5_BASE_URL;

export default {
	USE_MOCK,
	API_BASE_URL,
	H5_BASE_URL,
	WX_MP_APPID,
	getApiBaseUrl,
	getH5BaseUrl
};
