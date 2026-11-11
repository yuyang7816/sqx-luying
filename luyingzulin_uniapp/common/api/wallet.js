import { get, postJson, resolveBusinessResponse } from '@/common/httpRequest';
import {
	calcWithdrawActualAmount,
	calcWithdrawServiceFee,
	DEFAULT_WITHDRAW_CONFIG,
	formatWithdrawFeeRate,
	mapUiAccountTypeToSaveType,
	mapUiAccountTypeToWithdrawType,
	mapWalletTransactionVoToUi,
	mapWalletVoToUi,
	mapWithdrawalAccountToUi,
	mapWithdrawRecordVoToUi,
	validateWithdrawAmount
} from '@/common/api/assetsMappers';

export {
	calcWithdrawActualAmount,
	calcWithdrawServiceFee,
	formatWithdrawFeeRate,
	validateWithdrawAmount,
	DEFAULT_WITHDRAW_CONFIG
} from '@/common/api/assetsMappers';

const resolveUserId = () => {
	const userId = uni.getStorageSync('userId');
	return userId ? Number(userId) : 0;
};

const buildIncomeExpense = (records = []) => {
	const income = records.filter((item) => item.direction === 1).reduce((sum, item) => sum + Number(item.amount || 0), 0);
	const expense = records.filter((item) => item.direction !== 1).reduce((sum, item) => sum + Number(item.amount || 0), 0);
	return {
		totalIncome: Number(income.toFixed(2)),
		totalExpense: Number(expense.toFixed(2))
	};
};

/** 我的钱包余额（需登录） */
export async function fetchWalletSummary(options = {}) {
	const vo = await get('app/userWallet/wallet', {}, options).then(resolveBusinessResponse);
	const wallet = mapWalletVoToUi(vo);
	return {
		...wallet,
		amount: wallet.balance,
		userId: resolveUserId()
	};
}

/** 钱包页聚合数据：余额 + 流水 + 提现记录 + 收款账户 */
export async function fetchWalletDetail(options = {}) {
	const [walletVo, txPage, withdrawPage, alipayAccount, wechatAccount] = await Promise.all([
		get('app/userWallet/wallet', {}, options).then(resolveBusinessResponse),
		postJson('app/userWalletTransaction/page', {
			page: 1,
			limit: 20
		}, options).then(resolveBusinessResponse),
		postJson('app/withdrawRecord/page', {
			page: 1,
			limit: 20
		}, options).then(resolveBusinessResponse),
		get('app/withdrawalAccount/getUserWithdrawalAccount', { type: 1 }, options).then(resolveBusinessResponse).catch(() => null),
		get('app/withdrawalAccount/getUserWithdrawalAccount', { type: 2 }, options).then(resolveBusinessResponse).catch(() => null)
	]);
	const records = (txPage.records || []).map(mapWalletTransactionVoToUi);
	const withdrawRecords = (withdrawPage.records || []).map(mapWithdrawRecordVoToUi);
	const { totalIncome, totalExpense } = buildIncomeExpense(records);
	const wallet = mapWalletVoToUi(walletVo);
	return {
		...wallet,
		totalIncome,
		totalExpense,
		records,
		withdrawRecords,
		withdrawConfig: { ...DEFAULT_WITHDRAW_CONFIG },
		withdrawAccounts: {
			alipay: mapWithdrawalAccountToUi(alipayAccount, 'alipay'),
			wechat: mapWithdrawalAccountToUi(wechatAccount, 'wechat')
		}
	};
}

/** 钱包流水分页 */
export async function fetchWalletTransactions({ direction = 'all', page = 1, limit = 50 } = {}, options = {}) {
	const pageVo = await postJson('app/userWalletTransaction/page', {
		page,
		limit
	}, options).then(resolveBusinessResponse);
	let records = (pageVo.records || []).map(mapWalletTransactionVoToUi);
	if (direction === 'plus') {
		records = records.filter((item) => item.direction === 1);
	}
	if (direction === 'minus') {
		records = records.filter((item) => item.direction !== 1);
	}
	const { totalIncome, totalExpense } = buildIncomeExpense(records);
	return {
		...pageVo,
		records,
		totalIncome,
		totalExpense
	};
}

/** 提现记录分页 */
export async function fetchWithdrawRecords({ page = 1, limit = 50 } = {}, options = {}) {
	const pageVo = await postJson('app/withdrawRecord/page', { page, limit }, options).then(resolveBusinessResponse);
	return {
		...pageVo,
		records: (pageVo.records || []).map(mapWithdrawRecordVoToUi)
	};
}

/** 保存收款账户 */
export async function saveWithdrawAccount({ accountType, accountName = '', accountNumber = '', qrCodeUrl = '' } = {}, options = {}) {
	const type = mapUiAccountTypeToSaveType(accountType);
	const payload = { type };
	if (accountType === 'wechat') {
		payload.accountNumber = String(qrCodeUrl || accountNumber || '').trim();
		payload.accountName = '';
	} else {
		payload.accountName = String(accountName || '').trim();
		payload.accountNumber = String(accountNumber || '').trim();
	}
	return postJson('app/withdrawalAccount/save', payload, options).then(resolveBusinessResponse);
}

/** 提交提现申请 */
export async function submitWithdraw({ amount, accountType } = {}, options = {}) {
	return postJson('app/withdrawRecord/save', {
		amount: Number(amount),
		accountType: mapUiAccountTypeToWithdrawType(accountType),
		sourceType: 2
	}, options).then(resolveBusinessResponse);
}

/** 申请余额充值（返回 payNo） */
export async function applyWalletRecharge({ amount, remark = '' } = {}, options = {}) {
	return postJson('app/userWallet/recharge/apply', {
		amount: Number(amount),
		remark: remark || undefined
	}, options).then(resolveBusinessResponse);
}

/** 余额充值支付状态 */
export async function fetchWalletRechargePayStatus(payNo, options = {}) {
	return get(`app/userWallet/recharge/pay-status/${encodeURIComponent(payNo)}`, {}, options).then(resolveBusinessResponse);
}

export const walletApi = {
	wallet: 'app/userWallet/wallet',
	rechargeApply: 'app/userWallet/recharge/apply',
	rechargePayStatus: 'app/userWallet/recharge/pay-status',
	transactionPage: 'app/userWalletTransaction/page',
	withdrawPage: 'app/withdrawRecord/page',
	withdrawSave: 'app/withdrawRecord/save',
	withdrawAccountSave: 'app/withdrawalAccount/save'
};
