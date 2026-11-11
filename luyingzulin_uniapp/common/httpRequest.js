import { getApiBaseUrl } from './env';
import { isBusinessSuccess, isUnauthorizedCode, resolveErrorMessage } from './errorCode';

const LOGIN_PAGE = '/packageUser/login';
let redirectingToLogin = false;

/** 401 / token 失效：清理本地登录态并提示重新登录 */
const handleUnauthorized = (message) => {
	uni.removeStorageSync('token');
	uni.removeStorageSync('userId');
	uni.removeStorageSync('phone');
	uni.removeStorageSync('openId');
	uni.removeStorageSync('camp-auth-session');

	if (redirectingToLogin) {
		return;
	}
	redirectingToLogin = true;
	uni.showToast({
		title: message || '登录已失效，请重新登录',
		icon: 'none'
	});
	setTimeout(() => {
		const pages = getCurrentPages();
		const currentRoute = pages.length ? `/${pages[pages.length - 1].route}` : '';
		if (!currentRoute.includes('/packageUser/login')) {
			uni.navigateTo({ url: LOGIN_PAGE });
		}
		redirectingToLogin = false;
	}, 300);
};

const showBusinessError = (message) => {
	uni.showToast({
		title: message || '请求失败',
		icon: 'none'
	});
};

const appendQuery = (url, data = {}) => {
	if (!data || typeof data !== 'object') {
		return url;
	}
	const query = Object.keys(data)
		.filter((key) => data[key] !== undefined && data[key] !== null && data[key] !== '')
		.map((key) => `${encodeURIComponent(key)}=${encodeURIComponent(data[key])}`)
		.join('&');
	if (!query) {
		return url;
	}
	return `${url}${url.includes('?') ? '&' : '?'}${query}`;
};

const normalizeResponse = (result, options = {}) => {
	const payload = result?.data ?? result;
	if (payload && (typeof payload.code === 'number' || typeof payload.status === 'number')) {
		const code = payload.code ?? payload.status;
		if (isBusinessSuccess(payload)) {
			return payload;
		}
		if (isUnauthorizedCode(code)) {
			handleUnauthorized(resolveErrorMessage(payload));
			return Promise.reject(payload);
		}
		if (!options.silentError) {
			showBusinessError(resolveErrorMessage(payload));
		}
		return Promise.reject(payload);
	}
	return payload;
};

const DEFAULT_TIMEOUT = 12000;

const sendRequest = (url, data, method, header, options = {}) => {
	const {
		hasToken = true,
		silentError = false,
		timeout = DEFAULT_TIMEOUT
	} = options;
	const baseUrl = getApiBaseUrl();
	const requestUrl = `${baseUrl}${String(url || '').replace(/^\//, '')}`;
	const token = hasToken ? uni.getStorageSync('token') : '';
	const isGet = method === 'GET';
	const finalUrl = isGet ? appendQuery(requestUrl, data) : requestUrl;

	return new Promise((resolve, reject) => {
		uni.request({
			url: finalUrl,
			data: isGet ? undefined : data,
			method,
			timeout,
			header: {
				'content-type': header,
				...(hasToken && token ? { token } : {})
			},
			success: (result) => {
				Promise.resolve(normalizeResponse(result, { silentError }))
					.then(resolve)
					.catch(reject);
			},
			fail: (error) => {
				if (!silentError) {
					showBusinessError(error?.errMsg || '网络请求失败');
				}
				reject(error);
			}
		});
	});
};

export { isBusinessSuccess, resolveErrorMessage };

/** 解包 Result.data */
export const resolveBusinessResponse = (result) => {
	if (!isBusinessSuccess(result)) {
		return Promise.reject(result);
	}
	return Promise.resolve(result.data ?? result);
};

export const request = (url, data = {}, method = 'GET', header = 'application/json', options = {}) => {
	return sendRequest(url, data, method, header, options);
};

export const get = (url, data = {}, options = {}) => {
	return sendRequest(url, data, 'GET', 'application/json', options);
};

export const post = (url, data = {}, header = 'application/x-www-form-urlencoded', options = {}) => {
	return sendRequest(url, data, 'POST', header, options);
};

export const postJson = (url, data = {}, options = {}) => {
	return sendRequest(url, data, 'POST', 'application/json', options);
};

export const putJson = (url, data = {}, options = {}) => {
	return sendRequest(url, data, 'PUT', 'application/json', options);
};

export const del = (url, data = {}, options = {}) => {
	return sendRequest(url, data, 'DELETE', 'application/json', options);
};

/** 兼容旧命名：带 token 的 POST */
export const postT = (url, data = {}, header = 'application/x-www-form-urlencoded', options = {}) => {
	return post(url, data, header, options);
};

/** 兼容旧命名：带 token 的 GET */
export const getT = (url, data = {}, options = {}) => {
	return get(url, data, options);
};

export default {
	get,
	post,
	postJson,
	putJson,
	del,
	postT,
	getT,
	request,
	resolveBusinessResponse,
	isBusinessSuccess
};
