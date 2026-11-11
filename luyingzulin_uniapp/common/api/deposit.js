import { get, postJson, resolveBusinessResponse } from '@/common/httpRequest';
import { mapDepositLogVoToUi, mapDepositSummaryVoToUi } from '@/common/api/assetsMappers';

/** 押金概览 */
export async function fetchDepositSummary(options = {}) {
	const vo = await get('app/user/deposit/summary', {}, options).then(resolveBusinessResponse);
	return mapDepositSummaryVoToUi(vo);
}

/** 押金流水分页 */
export async function fetchDepositLogPage({ page = 1, limit = 20 } = {}, options = {}) {
	const pageVo = await postJson('app/user/deposit/log/page', { page, limit }, options).then(resolveBusinessResponse);
	return {
		...pageVo,
		records: (pageVo.records || []).map(mapDepositLogVoToUi)
	};
}

/** 缴纳押金（返回 payNo） */
export async function applyDepositPay({ amount, remark = '' } = {}, options = {}) {
	return postJson('app/user/deposit/pay', { amount: Number(amount), remark: remark || undefined }, options)
		.then(resolveBusinessResponse);
}

/** 押金缴纳支付状态 */
export async function fetchDepositPayStatus(payNo) {
	return get(`app/user/deposit/pay-status/${encodeURIComponent(payNo)}`).then(resolveBusinessResponse);
}

/** 申请退押 */
export async function applyDepositRefund({ amount, remark = '' } = {}, options = {}) {
	return postJson('app/user/deposit/refund/apply', { amount: Number(amount), remark: remark || undefined }, options)
		.then(resolveBusinessResponse);
}

export const depositApi = {
	summary: 'app/user/deposit/summary',
	logPage: 'app/user/deposit/log/page',
	pay: 'app/user/deposit/pay',
	payStatus: 'app/user/deposit/pay-status',
	refundApply: 'app/user/deposit/refund/apply'
};
