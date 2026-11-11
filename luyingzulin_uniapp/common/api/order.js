import { get, postJson, resolveBusinessResponse } from '@/common/httpRequest';
import {
	mapOrderDetailVoToView,
	mapOrderListVoToView,
	mapPickupCodeVoToView,
	mapPickupTypeToApi,
	mapReturnMethodToApi
} from '@/common/rental/constants';

/**
 * 创建租赁订单
 * @param {Object} payload 含 items/pickupType/contactName 等，或 confirmPayload + 联系人信息
 */
export async function createOrder(payload = {}) {
	const confirm = payload.confirmPayload || {};
	const items = (payload.items || confirm.items || []).map((item) => ({
		goodsId: item.goodsId,
		specId: item.specId ?? 0,
		packageId: item.packageId ?? undefined,
		rentType: item.rentType,
		startTime: item.startTime,
		endTime: item.endTime,
		days: item.days,
		quantity: item.quantity ?? item.qty ?? 1
	}));
	const body = {
		items,
		pickupType: mapPickupTypeToApi(payload.pickupType || confirm.pickupType || 'store'),
		pickupStoreId: payload.pickupStoreId ?? confirm.pickupStoreId,
		addressId: payload.addressId ?? confirm.addressId,
		contactName: payload.contactName,
		contactPhone: payload.contactPhone,
		couponId: payload.couponId ?? confirm.couponId,
		remark: payload.remark || '',
		payChannel: payload.payChannel,
		cartIds: payload.cartIds ?? confirm.cartIds
	};
	return postJson('app/rental/order/create', body).then(resolveBusinessResponse);
}

/** 取消待支付订单 */
export async function cancelOrder(orderId) {
	return postJson(`app/rental/order/cancel/${orderId}`).then(resolveBusinessResponse);
}

/** 支付状态轮询 */
export async function fetchPayStatus(orderNo) {
	return get(`app/rental/order/pay-status/${orderNo}`).then(resolveBusinessResponse);
}

/** 订单分页（Tab status 筛选） */
export async function fetchOrderPage({ status = 'all', page = 1, limit = 20 } = {}) {
	const body = {
		page,
		limit,
		status: status === 'all' ? undefined : status
	};
	const pageVo = await postJson('app/rental/order/page', body).then(resolveBusinessResponse);
	return {
		...pageVo,
		records: (pageVo.records || []).map(mapOrderListVoToView)
	};
}

const PICKUP_STORE_STATUSES = new Set([
	'waiting_pickup',
	'delivering',
	'renting',
	'returning',
	'inspecting',
	'completed'
]);

/** 订单详情（含商品/时间轴/续租/归还/验收） */
export async function fetchOrderDetail(orderId) {
	const detailVo = await get(`app/rental/order/detail/${orderId}`).then(resolveBusinessResponse);
	let pickupVo = null;
	if (PICKUP_STORE_STATUSES.has(detailVo?.status)) {
		try {
			pickupVo = await get(`app/rental/pickup/code/${orderId}`).then(resolveBusinessResponse);
		} catch (error) {
			pickupVo = null;
		}
	}
	return mapOrderDetailVoToView(detailVo, pickupVo);
}

/** 取货凭证 */
export async function fetchPickupCode(orderId) {
	const pickupVo = await get(`app/rental/pickup/code/${orderId}`).then(resolveBusinessResponse);
	return mapPickupCodeVoToView(pickupVo);
}

/** 刷新取货凭证（含订单时段，用于轮询核销状态） */
export async function refreshPickupView(orderId) {
	const [pickupVo, orderVo] = await Promise.all([
		get(`app/rental/pickup/code/${orderId}`).then(resolveBusinessResponse),
		get(`app/rental/order/detail/${orderId}`).then(resolveBusinessResponse)
	]);
	const orderView = mapOrderDetailVoToView(orderVo, pickupVo);
	return mapPickupCodeVoToView(pickupVo, orderView);
}

/** 续租预览 */
export async function previewRenew(orderId, newEndTime) {
	return get(`app/rental/renew/preview/${orderId}`, { newEndTime }).then(resolveBusinessResponse);
}

/** 创建续租单 */
export async function createRenew(orderId, newEndTime, renewRentType) {
	const body = {
		orderId: Number(orderId),
		newEndTime,
		renewRentType: renewRentType || undefined
	};
	return postJson('app/rental/renew/create', body).then(resolveBusinessResponse);
}

/** 提交归还申请 */
export async function applyReturn(payload = {}) {
	const images = Array.isArray(payload.images) ? payload.images.filter(Boolean) : [];
	const body = {
		orderId: Number(payload.orderId),
		returnMethod: mapReturnMethodToApi(payload.returnMethod || payload.method || 'store'),
		returnDate: payload.returnDate || '',
		remark: payload.remark || payload.reason || '',
		imageUrls: images
	};
	return postJson('app/rental/return/apply', body).then(resolveBusinessResponse);
}

/** 归还申请详情 */
export async function fetchReturnDetail(orderId) {
	return get(`app/rental/return/detail/${orderId}`).then(resolveBusinessResponse);
}
