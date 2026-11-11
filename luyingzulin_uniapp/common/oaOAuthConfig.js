import { postJson, resolveBusinessResponse } from '@/common/httpRequest';

const STORAGE_KEY = 'sys_oa_oauth_enabled';

const normalizeEnabled = (value) => ['1', 'true', 'yes', 'on'].includes(String(value || '').trim().toLowerCase());

export const isOaOAuthEnabled = () => normalizeEnabled(uni.getStorageSync(STORAGE_KEY));

export const loadOaOAuthConfig = async () => {
	try {
		const vo = await postJson('app/config/info', { key: 'sys_oa_oauth_enabled' }).then(resolveBusinessResponse);
		const value = vo?.value ?? '0';
		uni.setStorageSync(STORAGE_KEY, String(value || '0'));
		return normalizeEnabled(value);
	} catch (error) {
		return isOaOAuthEnabled();
	}
};
