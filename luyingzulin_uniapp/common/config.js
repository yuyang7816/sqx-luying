import { API_BASE_URL } from './env';

/** @deprecated 请优先使用 common/env.js 的 API_BASE_URL */
const config = {
	APIHOST: API_BASE_URL.replace(/\/$/, ''),
	WSHOST1: API_BASE_URL.replace(/^http/, 'ws')
};

export default config;
