import { get, postJson, resolveBusinessResponse } from '@/common/httpRequest';
import { mapClaimableCouponVoToUi, mapCouponVoToUi, mapUsableCouponVoToUi } from '@/common/api/assetsMappers';

const TAB_STATUS_MAP = {
	未使用: 'unused',
	已使用: 'used',
	已过期: 'expired'
};

const resolveStatus = (statusKey = 'unused') => TAB_STATUS_MAP[statusKey] || statusKey;

/** 我的优惠券分页 */
export async function fetchCouponPage({ status = 'unused', keyword = '', page = 1, limit = 50 } = {}, options = {}) {
	const pageVo = await postJson('app/user/coupon/page', {
		page,
		limit,
		status: resolveStatus(status) || undefined,
		keyword: keyword || undefined
	}, options).then(resolveBusinessResponse);
	return {
		...pageVo,
		records: (pageVo.records || []).map(mapCouponVoToUi)
	};
}

/** 下单可用优惠券 */
export async function fetchUsableCoupons({ rentAmount = 0, deliveryAmount = 0, depositAmount = 0 } = {}, options = {}) {
	const list = await get('app/user/coupon/usable', {
		rentAmount,
		deliveryAmount,
		depositAmount
	}, options).then(resolveBusinessResponse);
	return (list || []).map(mapUsableCouponVoToUi);
}

/** 未使用券数量（我的页角标） */
export async function fetchCouponSummary(options = {}) {
	const pageVo = await postJson('app/user/coupon/page', {
		page: 1,
		limit: 1,
		status: 'unused'
	}, options).then(resolveBusinessResponse);
	return {
		unused: Number(pageVo.total || 0),
		used: 0,
		expired: 0,
		total: Number(pageVo.total || 0)
	};
}

/** 可领取优惠券列表（领券中心） */
export async function fetchClaimableCoupons(options = {}) {
	const list = await get('app/user/coupon/claimable', {}, options).then(resolveBusinessResponse);
	return (list || []).map(mapClaimableCouponVoToUi);
}

/** 领取优惠券 */
export async function claimCoupon(templateId, options = {}) {
	await postJson('app/user/coupon/claim', { templateId }, options).then(resolveBusinessResponse);
}

export const couponApi = {
	page: 'app/user/coupon/page',
	usable: 'app/user/coupon/usable',
	claimable: 'app/user/coupon/claimable',
	claim: 'app/user/coupon/claim'
};
