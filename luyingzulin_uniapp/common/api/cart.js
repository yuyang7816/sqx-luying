import { del, get, postJson, putJson, resolveBusinessResponse } from '@/common/httpRequest';
import { mapPickupTypeToApi } from '@/common/rental/constants';

/** 购物车列表（需登录） */
export async function fetchCartList() {
	return get('app/rental/cart/list').then(resolveBusinessResponse);
}

/** 加入租物车 */
export async function addToCart(payload = {}, options = {}) {
	const body = {
		goodsId: payload.goodsId,
		specId: payload.specId ?? 0,
		packageId: payload.packageId ?? 0,
		rentType: payload.rentType || 'day',
		startTime: payload.startTime,
		endTime: payload.endTime,
		qty: payload.qty ?? 1
	};
	return postJson('app/rental/cart/add', body, options).then(resolveBusinessResponse);
}

/** 更新数量/规格/租期 */
export async function updateCartItem(payload = {}) {
	const body = {
		id: payload.id,
		specId: payload.specId,
		packageId: payload.packageId,
		rentType: payload.rentType,
		startTime: payload.startTime,
		endTime: payload.endTime,
		qty: payload.qty
	};
	return putJson('app/rental/cart/update', body).then(resolveBusinessResponse);
}

/** 删除单条 */
export async function deleteCartItem(id) {
	return del(`app/rental/cart/${id}`).then(resolveBusinessResponse);
}

/** 批量删除 */
export async function deleteCartBatch(ids = []) {
	return del('app/rental/cart/batch', { ids }).then(resolveBusinessResponse);
}

/** 勾选/全选 */
export async function selectCartItems(payload = {}) {
	const body = {
		id: payload.id,
		selected: payload.selected,
		selectAll: payload.selectAll,
		ids: payload.ids
	};
	return putJson('app/rental/cart/select', body).then(resolveBusinessResponse);
}

/** 结算预览，返回 confirmPayload 供创建订单 */
export async function checkoutPreview(payload = {}) {
	const body = {
		cartIds: payload.cartIds,
		pickupType: mapPickupTypeToApi(payload.pickupType || 'store'),
		pickupStoreId: payload.pickupStoreId,
		addressId: payload.addressId,
		couponId: payload.couponId
	};
	return postJson('app/rental/cart/checkout-preview', body).then(resolveBusinessResponse);
}
