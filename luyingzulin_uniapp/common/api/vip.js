import { get, postJson, resolveBusinessResponse } from '@/common/httpRequest';

/** 会员套餐列表 */
export async function fetchVipPlans() {
	return postJson('app/vipPlan/list', {}).then(resolveBusinessResponse);
}

/** 会员权益列表 */
export async function fetchVipRights() {
	return postJson('app/vipRights/list', {}).then(resolveBusinessResponse);
}

/** 创建会员购买订单 */
export async function createVipPurchase(planId) {
	return postJson('app/vipOrder/purchase', { planId: Number(planId) }).then(resolveBusinessResponse);
}

/** 会员订单支付状态 */
export async function fetchVipPayStatus(orderNo) {
	return get(`app/vipOrder/pay-status/${encodeURIComponent(orderNo)}`).then(resolveBusinessResponse);
}
