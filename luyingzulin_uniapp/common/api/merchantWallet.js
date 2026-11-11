import { get, postJson, resolveBusinessResponse } from '@/common/httpRequest';
import {
	DEFAULT_WITHDRAW_CONFIG,
	mapMerchantSecurityDepositLogVoToUi,
	mapMerchantSecurityDepositSummaryVoToUi,
	mapMerchantWalletTransactionVoToUi,
	mapMerchantWalletVoToUi,
	mapUiAccountTypeToWithdrawType,
	mapWithdrawalAccountToUi,
	mapWithdrawRecordVoToUi
} from '@/common/api/assetsMappers';

const buildIncomeExpense = (records = []) => {
	const income = records.filter((item) => item.direction === 1).reduce((sum, item) => sum + Number(item.amount || 0), 0);
	const expense = records.filter((item) => item.direction !== 1).reduce((sum, item) => sum + Number(item.amount || 0), 0);
	return {
		totalIncome: Number(income.toFixed(2)),
		totalExpense: Number(expense.toFixed(2))
	};
};

/** 读取系统配置的应缴保证金（兜底） */
async function fetchMerchantSecurityDepositRequiredConfig(options = {}) {
	try {
		const vo = await postJson('app/config/info', { key: 'merchant_security_deposit_amount' }, options).then(resolveBusinessResponse);
		return Number(vo.value || 0);
	} catch {
		return 0;
	}
}

/** 补全保证金摘要（接口缺字段时用系统配置兜底） */
export async function enrichSecurityDepositSummary(summary = {}, options = {}) {
	const mapped = mapMerchantSecurityDepositSummaryVoToUi(summary);
	if (mapped.depositRequired || Number(mapped.requiredAmount || 0) > 0) {
		return mapped;
	}
	const requiredAmount = await fetchMerchantSecurityDepositRequiredConfig(options);
	if (requiredAmount <= 0) {
		return mapped;
	}
	const heldAmount = Number(mapped.heldAmount || 0);
	const needPayAmount = Math.max(0, Number((requiredAmount - heldAmount).toFixed(2)));
	const depositPaid = heldAmount >= requiredAmount;
	return {
		...mapped,
		requiredAmount,
		needPayAmount,
		depositRequired: true,
		depositPaid,
		canRefund: Number(mapped.availableAmount || 0) > 0,
		statusText: depositPaid ? '已缴纳' : (heldAmount > 0 ? '待补缴' : '未缴纳')
	};
}

/** 商户保证金概览（含配置兜底） */
export async function fetchMerchantSecurityDepositSummaryResolved(options = {}) {
	try {
		const vo = await get('app/merchant/wallet/security-deposit/summary', {}, options).then(resolveBusinessResponse);
		return enrichSecurityDepositSummary(vo, options);
	} catch {
		return enrichSecurityDepositSummary({}, options);
	}
}

/** 商户经营钱包概览 */
export async function fetchMerchantWalletSummary(options = {}) {
	const [walletVo, depositVo] = await Promise.all([
		get('app/merchant/wallet/summary', {}, options).then(resolveBusinessResponse),
		get('app/merchant/wallet/security-deposit/summary', {}, options).then(resolveBusinessResponse).catch(() => null)
	]);
	const wallet = mapMerchantWalletVoToUi(walletVo.wallet || {});
	const securityDeposit = await enrichSecurityDepositSummary(depositVo || walletVo.securityDeposit || {}, options);
	const records = (walletVo.recentTransactions || []).map(mapMerchantWalletTransactionVoToUi);
	const withdrawRecords = (walletVo.recentWithdraws || []).map(mapWithdrawRecordVoToUi);
	const { totalIncome, totalExpense } = buildIncomeExpense(records);
	return {
		...wallet,
		securityDeposit,
		records,
		withdrawRecords,
		totalIncome,
		totalExpense,
		withdrawConfig: { ...DEFAULT_WITHDRAW_CONFIG }
	};
}

/** 商户经营资金明细分页 */
export async function fetchMerchantWalletTransactions({ direction = 'all', page = 1, limit = 50 } = {}, options = {}) {
	const pageVo = await postJson('app/merchant/wallet/transaction/page', { page, limit }, options).then(resolveBusinessResponse);
	let records = (pageVo.records || []).map(mapMerchantWalletTransactionVoToUi);
	if (direction === 'plus') records = records.filter((item) => item.direction === 1);
	if (direction === 'minus') records = records.filter((item) => item.direction !== 1);
	const { totalIncome, totalExpense } = buildIncomeExpense(records);
	return { ...pageVo, records, totalIncome, totalExpense };
}

/** 商户钱包提现记录 */
export async function fetchMerchantWithdrawRecords({ page = 1, limit = 50 } = {}, options = {}) {
	const pageVo = await postJson('app/merchant/wallet/withdraw/page', { page, limit }, options).then(resolveBusinessResponse);
	return { ...pageVo, records: (pageVo.records || []).map(mapWithdrawRecordVoToUi) };
}

/** 商户钱包发起提现 */
export async function submitMerchantWithdraw({ amount, accountType } = {}, options = {}) {
	return postJson('app/merchant/wallet/withdraw/save', {
		amount: Number(amount),
		accountType: mapUiAccountTypeToWithdrawType(accountType),
		sourceType: 3
	}, options).then(resolveBusinessResponse);
}

/** 商户保证金概览 */
export async function fetchMerchantSecurityDepositSummary(options = {}) {
	return fetchMerchantSecurityDepositSummaryResolved(options);
}

/** 商户保证金流水 */
export async function fetchMerchantSecurityDepositLogPage({ page = 1, limit = 20 } = {}, options = {}) {
	const pageVo = await postJson('app/merchant/wallet/security-deposit/log/page', { page, limit }, options).then(resolveBusinessResponse);
	const visibleRecords = (pageVo.records || []).filter((vo) => {
		if (vo.logType !== 'pay') return true;
		return String(vo.status || '').toLowerCase() === 'completed';
	});
	return { ...pageVo, records: visibleRecords.map(mapMerchantSecurityDepositLogVoToUi) };
}

/** 商户缴纳保证金（金额以系统配置为准，无需传 amount） */
export async function applyMerchantSecurityDepositPay({ remark = '' } = {}, options = {}) {
	return postJson('app/merchant/wallet/security-deposit/pay', {
		remark: remark || undefined
	}, options).then(resolveBusinessResponse);
}

/** 商户保证金支付状态 */
export async function fetchMerchantSecurityDepositPayStatus(payNo, options = {}) {
	return get(`app/merchant/wallet/security-deposit/pay-status/${encodeURIComponent(payNo)}`, {}, options).then(resolveBusinessResponse);
}

/** 取消商户保证金待支付单 */
export async function cancelMerchantSecurityDepositPay(payNo, options = {}) {
	return postJson(`app/merchant/wallet/security-deposit/pay/cancel/${encodeURIComponent(payNo)}`, {}, options).then(resolveBusinessResponse);
}

/** 商户申请退还保证金 */
export async function applyMerchantSecurityDepositRefund({ amount, remark = '' } = {}, options = {}) {
	return postJson('app/merchant/wallet/security-deposit/refund/apply', {
		amount: Number(amount),
		remark: remark || undefined
	}, options).then(resolveBusinessResponse);
}

/** 商户提现页：经营余额 + 收款账户 */
export async function fetchMerchantWalletWithdrawContext(options = {}) {
	const summary = await fetchMerchantWalletSummary(options);
	const [alipayAccount, wechatAccount] = await Promise.all([
		get('app/withdrawalAccount/getUserWithdrawalAccount', { type: 1 }, options).then(resolveBusinessResponse).catch(() => null),
		get('app/withdrawalAccount/getUserWithdrawalAccount', { type: 2 }, options).then(resolveBusinessResponse).catch(() => null)
	]);
	return {
		...summary,
		withdrawAccounts: {
			alipay: mapWithdrawalAccountToUi(alipayAccount, 'alipay'),
			wechat: mapWithdrawalAccountToUi(wechatAccount, 'wechat')
		}
	};
}

export {
	calcWithdrawActualAmount,
	calcWithdrawServiceFee,
	formatWithdrawFeeRate,
	validateWithdrawAmount,
	DEFAULT_WITHDRAW_CONFIG
} from '@/common/api/assetsMappers';

// 兼容旧命名，避免遗漏引用
export const fetchMerchantDepositSummary = fetchMerchantSecurityDepositSummary;
export const fetchMerchantDepositLogPage = fetchMerchantSecurityDepositLogPage;
export const applyMerchantDepositPay = applyMerchantSecurityDepositPay;
export const fetchMerchantDepositPayStatus = fetchMerchantSecurityDepositPayStatus;
export const applyMerchantDepositRefund = applyMerchantSecurityDepositRefund;
