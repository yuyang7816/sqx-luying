import { del, get, postJson, putJson, resolveBusinessResponse } from '@/common/httpRequest';
import {
	mapAddressUiToSaveBo,
	mapAddressVoToUi,
	mapAppUserInfoToUi,
	mapFavoriteVoToUi,
	mapProfileUiToUpdateBo
} from '@/common/api/userMappers';

export {
	mapAddressUiToSaveBo,
	mapAddressVoToUi,
	mapAppUserInfoToUi,
	mapFavoriteVoToUi,
	mapProfileUiToUpdateBo
} from '@/common/api/userMappers';

const SELECTED_ADDRESS_SNAPSHOT_KEY = 'selectedAddressSnapshot';

/** 获取当前用户资料 */
export async function fetchUserInfo(options = {}) {
	const vo = await get('app/user/info', {}, options).then(resolveBusinessResponse);
	return mapAppUserInfoToUi(vo);
}

/** 更新个人资料并返回最新 VO */
export async function updateUserProfile(form = {}, options = {}) {
	const vo = await putJson('app/user/profile', mapProfileUiToUpdateBo(form), options).then(resolveBusinessResponse);
	return mapAppUserInfoToUi(vo);
}

/** 地址列表 */
export async function fetchAddressList(options = {}) {
	const list = await get('app/user/address/list', {}, options).then(resolveBusinessResponse);
	return (list || []).map(mapAddressVoToUi);
}

/** 地址详情 */
export async function fetchAddressDetail(id, options = {}) {
	const vo = await get(`app/user/address/detail/${id}`, {}, options).then(resolveBusinessResponse);
	return mapAddressVoToUi(vo);
}

/** 新增/更新地址 */
export async function saveAddress(form = {}, options = {}) {
	const vo = await postJson('app/user/address/save', mapAddressUiToSaveBo(form), options).then(resolveBusinessResponse);
	return mapAddressVoToUi(vo);
}

/** 删除地址 */
export async function deleteAddress(id, options = {}) {
	return del(`app/user/address/delete/${id}`, {}, options).then(resolveBusinessResponse);
}

/** 设为默认地址 */
export async function setDefaultAddress(id, options = {}) {
	return putJson(`app/user/address/default/${id}`, {}, options).then(resolveBusinessResponse);
}

/** 记录订单页选中的配送地址 */
export function setSelectedAddress(address) {
	if (!address?.id) return;
	uni.setStorageSync('selectedAddressId', Number(address.id));
	uni.setStorageSync(SELECTED_ADDRESS_SNAPSHOT_KEY, address);
}

/** 读取订单页选中的配送地址 */
export function getSelectedAddress() {
	const snapshot = uni.getStorageSync(SELECTED_ADDRESS_SNAPSHOT_KEY);
	return snapshot && typeof snapshot === 'object' ? snapshot : null;
}

/** 收藏列表 */
export async function fetchFavoriteList(options = {}) {
	const list = await get('app/user/favorite/list', {}, options).then(resolveBusinessResponse);
	return (list || []).map(mapFavoriteVoToUi);
}

/** 是否已收藏 */
export async function checkFavorite(goodsId, options = {}) {
	return get('app/user/favorite/check', { goodsId }, options).then(resolveBusinessResponse);
}

/** 切换收藏 */
export async function toggleFavorite(goodsId, options = {}) {
	return postJson('app/user/favorite/toggle', { goodsId: Number(goodsId) }, options).then(resolveBusinessResponse);
}

/** 清除应用缓存（保留登录态） */
export async function clearAppCache() {
	const keepKeys = ['camp-auth-session', 'token', 'userId', 'phone', 'openId', 'selectedAddressId', SELECTED_ADDRESS_SNAPSHOT_KEY];
	const info = uni.getStorageInfoSync();
	(info.keys || []).forEach((key) => {
		if (!keepKeys.includes(key)) {
			uni.removeStorageSync(key);
		}
	});
	return true;
}

/** 估算本地缓存体积文案 */
export function formatCacheSizeText() {
	try {
		const info = uni.getStorageInfoSync();
		const kb = Number(info.currentSize || 0);
		if (kb >= 1024) {
			return `${(kb / 1024).toFixed(1)}MB`;
		}
		return `${kb}KB`;
	} catch (error) {
		return '0KB';
	}
}

export const userApi = {
	fetchUserInfo: 'app/user/info',
	updateUserProfile: 'app/user/profile',
	fetchAddressList: 'app/user/address/list',
	saveAddress: 'app/user/address/save',
	fetchFavoriteList: 'app/user/favorite/list',
	toggleFavorite: 'app/user/favorite/toggle'
};
