/** 提现默认配置（服务端 sys_config 未暴露给移动端时使用） */
export const DEFAULT_WITHDRAW_CONFIG = {
	minAmount: 1,
	maxAmount: 5000,
	serviceFeeRate: 0.006
};

/** 反馈 Tab 文案 → 服务端 feedbackType */
export const FEEDBACK_TYPE_UI_TO_API = {
	页面问题: 'other',
	订单问题: 'order',
	设备问题: 'product',
	配送问题: 'order_service',
	功能建议: 'suggestion'
};

const COUPON_CARD_TYPE = {
	delivery_free: 'orange',
	deposit_free: 'orange',
	discount: 'orange'
};

const resolveCouponCardType = (couponType) => COUPON_CARD_TYPE[couponType] || 'green';

const formatDatePart = (value) => {
	if (!value) return '';
	return String(value).slice(0, 10);
};

const resolveTransactionIcon = (direction) => (Number(direction) === 1 ? 'plus' : 'minus');

/** 优惠券 VO → 列表卡片 */
export const mapCouponVoToUi = (vo = {}) => ({
	id: vo.id,
	couponNo: vo.couponNo || '',
	name: vo.name || '',
	amount: Number(vo.amount || 0),
	condition: vo.conditionText || (vo.minAmount ? `满 ${vo.minAmount} 可用` : '无门槛'),
	label: vo.couponTypeLabel || vo.couponType || '优惠券',
	expire: formatDatePart(vo.expireTime),
	status: vo.status || 'unused',
	type: resolveCouponCardType(vo.couponType),
	usedTime: vo.usedTime || '',
	orderNo: vo.orderNo || ''
});

/** 下单可用券 → 确认页选项 */
export const mapUsableCouponVoToUi = (vo = {}) => ({
	id: vo.id,
	name: vo.name || '',
	amount: Number(vo.amount || 0),
	condition: vo.conditionText || (vo.minAmount ? `满 ${vo.minAmount} 可用` : '无门槛')
});

/** 可领取券模板 → 领券中心卡片 */
export const mapClaimableCouponVoToUi = (vo = {}) => ({
	templateId: vo.templateId,
	name: vo.name || '',
	amount: Number(vo.amount || 0),
	condition: vo.conditionText || (vo.minAmount ? `满 ${vo.minAmount} 可用` : '无门槛'),
	label: vo.couponTypeLabel || vo.couponType || '优惠券',
	expire: formatDatePart(vo.validEnd),
	claimed: Boolean(vo.claimed),
	type: resolveCouponCardType(vo.couponType)
});

/** 钱包 VO → 余额摘要 */
export const mapWalletVoToUi = (vo = {}) => ({
	balance: Number(vo.amount || 0),
	frozenAmount: Number(vo.frozenAmount || 0),
	userId: vo.userId || 0
});

/** 钱包流水 VO → 明细行 */
export const mapWalletTransactionVoToUi = (vo = {}) => ({
	id: vo.id,
	title: vo.title || '钱包变动',
	time: vo.createTime || '',
	desc: vo.description || '',
	amount: Number(vo.amount || 0),
	direction: Number(vo.direction) === 1 ? 1 : -1,
	type: vo.transactionType || vo.bizType || '',
	icon: resolveTransactionIcon(vo.direction)
});

/** 提现记录 VO → 明细卡片 */
export const mapWithdrawRecordVoToUi = (vo = {}) => {
	const accountType = Number(vo.accountType);
	const accountLabel = accountType === 2 ? '支付宝' : accountType === 1 ? '微信收款' : '提现账户';
	const statusMeta = resolveWithdrawStatusMeta(vo.status);
	const withdrawalAccount = vo.withdrawalAccount || {};
	const uiAccountType = accountType === 2 ? 'alipay' : 'wechat';
	const accountNumber = formatWithdrawAccountDisplay(uiAccountType, mapWithdrawalAccountToUi(withdrawalAccount, uiAccountType));
	return {
		id: vo.id,
		time: vo.createTime || '',
		accountType: uiAccountType,
		accountLabel,
		accountNumber,
		qrCodeUrl: uiAccountType === 'wechat' ? (withdrawalAccount.accountNumber || '') : '',
		amount: Number(vo.amount || 0),
		serviceFee: Number(vo.serviceFee || 0),
		actualAmount: Number(vo.actualAmount || 0),
		status: statusMeta.key,
		statusText: statusMeta.label,
		reason: vo.reason || ''
	};
};

export const resolveWithdrawStatusMeta = (status) => {
	const value = Number(status);
	if (value === 2) {
		return { key: 'success', label: '已到账' };
	}
	if (value === 3) {
		return { key: 'reject', label: '已拒绝' };
	}
	return { key: 'pending', label: '审核中' };
};

/** 租赁押金概览 VO → 页面摘要（用户端，非商户保证金） */
export const mapDepositSummaryVoToUi = (vo = {}) => {
	const availableAmount = Number(vo.availableAmount || 0);
	const frozenAmount = Number(vo.frozenAmount || 0);
	const totalAmount = Number(vo.totalAmount || 0);
	const pendingRefundAmount = Number(vo.pendingRefundAmount ?? vo.pending_refund_amount ?? 0);
	const minDepositBalance = Number(vo.minDepositBalance ?? vo.min_deposit_balance ?? 0);
	const heldAmount = availableAmount + frozenAmount;
	const refundPendingRaw = vo.refundPending ?? vo.refund_pending;
	const canRefundRaw = vo.canRefund ?? vo.can_refund;
	const refundPending = refundPendingRaw === undefined ? pendingRefundAmount > 0 : Boolean(refundPendingRaw);
	const canRefund = canRefundRaw === undefined ? (!refundPending && availableAmount > 0) : Boolean(canRefundRaw);
	return {
		totalAmount,
		frozenAmount,
		availableAmount,
		heldAmount: heldAmount > 0 ? heldAmount : totalAmount,
		pendingRefundAmount,
		minDepositBalance,
		refundPending,
		canRefund,
		status: Number(vo.status),
		statusText: Number(vo.status) === 1 ? '已缴纳' : '已冻结'
	};
};

/** 商户保证金概览 VO → 页面摘要 */
export const mapMerchantSecurityDepositSummaryVoToUi = (vo = {}) => {
	const availableAmount = Number(vo.availableAmount || 0);
	const frozenAmount = Number(vo.frozenAmount || 0);
	const totalAmount = Number(vo.totalAmount || 0);
	const heldAmount = availableAmount + frozenAmount;
	const requiredAmount = Number(vo.requiredAmount ?? vo.required_amount ?? 0);
	const needPayAmount = Number(vo.needPayAmount ?? vo.need_pay_amount ?? 0);
	const pendingRefundAmount = Number(vo.pendingRefundAmount ?? vo.pending_refund_amount ?? 0);
	const depositRequiredRaw = vo.depositRequired ?? vo.deposit_required;
	const depositPaidRaw = vo.depositPaid ?? vo.deposit_paid;
	const canRefundRaw = vo.canRefund ?? vo.can_refund;
	const refundPendingRaw = vo.refundPending ?? vo.refund_pending;
	const depositRequired = depositRequiredRaw === undefined ? requiredAmount > 0 : Boolean(depositRequiredRaw);
	const depositPaid = depositPaidRaw === undefined ? (!depositRequired || heldAmount >= requiredAmount) : Boolean(depositPaidRaw);
	const refundPending = refundPendingRaw === undefined ? pendingRefundAmount > 0 : Boolean(refundPendingRaw);
	const canRefund = canRefundRaw === undefined ? (!refundPending && availableAmount > 0) : Boolean(canRefundRaw);
	const displayHeld = heldAmount > 0 ? heldAmount : totalAmount;
	let statusText = '未缴纳';
	if (refundPending) {
		statusText = '退保审核中';
	} else if (!depositRequired) {
		statusText = '无需缴纳';
	} else if (depositPaid) {
		statusText = '已缴纳';
	} else if (displayHeld > 0) {
		statusText = '待补缴';
	}
	return {
		totalAmount,
		frozenAmount,
		availableAmount,
		heldAmount: displayHeld,
		requiredAmount,
		needPayAmount,
		pendingRefundAmount,
		depositRequired,
		depositPaid,
		refundPending,
		canRefund,
		status: Number(vo.status),
		statusText
	};
};

/** 租赁押金流水 VO → 明细行 */
export const mapDepositLogVoToUi = (vo = {}) => {
	const isIncome = ['pay', 'unfreeze', 'refund'].includes(vo.logType);
	return {
		id: vo.id,
		logNo: vo.logNo || '',
		title: vo.logTypeLabel || vo.logType || '租赁押金变动',
		time: vo.createTime || '',
		amount: `${isIncome ? '+' : '-'}¥${Number(vo.amount || 0).toFixed(2)}`,
		type: isIncome ? 'plus' : 'minus',
		icon: isIncome ? 'plus' : 'minus',
		orderNo: vo.orderNo || '',
		remark: vo.remark || ''
	};
};

/** 商户保证金流水 VO → 明细行 */
export const mapMerchantSecurityDepositLogVoToUi = (vo = {}) => {
	const status = String(vo.status || '').toLowerCase();
	const isCompleted = status === 'completed';
	const isPending = status === 'pending';
	const isRejected = status === 'rejected' || status === 'cancelled';
	const isPay = vo.logType === 'pay';
	const isRefund = vo.logType === 'refund';
	const isIncome = isCompleted && ['pay', 'adjust'].includes(vo.logType);
	let title = vo.logTypeLabel || vo.logType || '保证金变动';
	if (isPending && isPay) {
		title = '缴纳保证金（待支付）';
	} else if (isPending && isRefund) {
		title = '退还保证金（审核中）';
	} else if (isRejected && isRefund) {
		title = '退还保证金（已驳回）';
	} else if (isCompleted && isRefund) {
		title = '退还保证金';
	}
	let amountText = '';
	if (isIncome) {
		amountText = `+¥${Number(vo.amount || 0).toFixed(2)}`;
	} else if (isCompleted && isRefund) {
		amountText = `-¥${Number(vo.amount || 0).toFixed(2)}`;
	} else if (isPending || isRejected) {
		amountText = `¥${Number(vo.amount || 0).toFixed(2)}`;
	} else {
		amountText = `-¥${Number(vo.amount || 0).toFixed(2)}`;
	}
	let type = 'minus';
	let icon = 'minus';
	if (isIncome) {
		type = 'plus';
		icon = 'plus';
	} else if (isPending) {
		type = 'pending';
		icon = 'clock';
	} else if (isRejected) {
		type = 'muted';
		icon = 'close';
	}
	return {
		id: vo.id,
		logNo: vo.logNo || '',
		title,
		time: vo.createTime || '',
		amount: amountText,
		type,
		icon,
		orderNo: vo.orderNo || '',
		remark: vo.remark || '',
		status
	};
};

/** 商户经营钱包 VO → 余额摘要 */
export const mapMerchantWalletVoToUi = (vo = {}) => ({
	balance: Number(vo.amount || 0),
	frozenAmount: Number(vo.frozenAmount || 0),
	merchantId: vo.merchantId || 0
});

/** 商户经营钱包流水 VO → 明细行 */
export const mapMerchantWalletTransactionVoToUi = (vo = {}) => ({
	id: vo.id,
	title: vo.title || '经营账户变动',
	time: vo.createTime || '',
	desc: vo.description || '',
	amount: Number(vo.amount || 0),
	direction: Number(vo.direction) === 1 ? 1 : -1,
	type: vo.transactionType || vo.bizType || '',
	icon: Number(vo.direction) === 1 ? 'plus' : 'minus'
});

/** 页面反馈类型 → 提交 BO */
export const mapFeedbackUiToSubmitBo = (form = {}) => ({
	feedbackType: FEEDBACK_TYPE_UI_TO_API[form.type] || 'other',
	content: String(form.content || '').trim(),
	contactPhone: String(form.contact || '').trim(),
	imageUrls: Array.isArray(form.imageUrls) ? form.imageUrls.filter(Boolean) : []
});

/** UI 提现方式 → 提现账号保存 type（1 支付宝 2 微信） */
export const mapUiAccountTypeToSaveType = (accountType) => (accountType === 'wechat' ? 2 : 1);

/** UI 提现方式 → 提现申请 accountType（1 微信 2 支付宝） */
export const mapUiAccountTypeToWithdrawType = (accountType) => (accountType === 'wechat' ? 1 : 2);

/** 提现账号实体 → 页面表单 */
export const mapWithdrawalAccountToUi = (account = {}, accountType = '') => {
	const type = accountType || (Number(account.type) === 2 ? 'wechat' : 'alipay');
	const accountNumber = account.accountNumber || '';
	if (type === 'wechat') {
		return {
			accountName: '',
			accountNumber: '',
			qrCodeUrl: accountNumber
		};
	}
	return {
		accountName: account.accountName || '',
		accountNumber,
		qrCodeUrl: ''
	};
};

/** 指定方式是否已绑定收款账户 */
export const isWithdrawAccountBound = (accountType, account = {}) => !validateWithdrawAccount(accountType, account);

/** 是否已绑定至少一种收款方式 */
export const hasAnyWithdrawAccountBound = (withdrawAccounts = {}) => (
	isWithdrawAccountBound('alipay', withdrawAccounts.alipay || {})
	|| isWithdrawAccountBound('wechat', withdrawAccounts.wechat || {})
);

/** 已绑定的提现方式列表（alipay / wechat） */
export const getBoundWithdrawAccountTypes = (withdrawAccounts = {}) => {
	const types = [];
	if (isWithdrawAccountBound('alipay', withdrawAccounts.alipay || {})) {
		types.push('alipay');
	}
	if (isWithdrawAccountBound('wechat', withdrawAccounts.wechat || {})) {
		types.push('wechat');
	}
	return types;
};

/** 校验收款账户是否已绑定 */
export const validateWithdrawAccount = (accountType, account = {}) => {
	if (accountType === 'alipay') {
		if (!String(account.accountName || '').trim()) {
			return '请先填写收款姓名';
		}
		if (!String(account.accountNumber || '').trim()) {
			return '请先填写支付宝账号';
		}
		return '';
	}
	if (accountType === 'wechat') {
		const qrCodeUrl = String(account.qrCodeUrl || account.accountNumber || '').trim();
		if (!qrCodeUrl) {
			return '请先上传微信收款码';
		}
		return '';
	}
	return '请选择提现方式';
};

export const formatWithdrawAccountDisplay = (accountType, account = {}) => {
	if (accountType === 'wechat') {
		const qrCodeUrl = account.qrCodeUrl || account.accountNumber || '';
		return qrCodeUrl ? '已绑定微信收款码' : '-';
	}
	const name = String(account.accountName || '').trim();
	const number = String(account.accountNumber || '').trim();
	if (name && number) {
		return `${name} / ${number}`;
	}
	return number || name || '-';
};

export const calcWithdrawServiceFee = (amount, config = DEFAULT_WITHDRAW_CONFIG) => {
	const value = Number(amount || 0);
	const rate = Number(config?.serviceFeeRate || 0);
	if (!Number.isFinite(value) || value <= 0 || !Number.isFinite(rate) || rate <= 0) {
		return 0;
	}
	return Number((value * rate).toFixed(2));
};

export const calcWithdrawActualAmount = (amount, config = DEFAULT_WITHDRAW_CONFIG) => {
	const value = Number(amount || 0);
	if (!Number.isFinite(value) || value <= 0) return 0;
	return Number((value - calcWithdrawServiceFee(value, config)).toFixed(2));
};

export const formatWithdrawFeeRate = (rate) => {
	const value = Number(rate || 0);
	if (!Number.isFinite(value) || value <= 0) return '0%';
	return `${Number((value * 100).toFixed(2))}%`;
};

export const validateWithdrawAmount = (amount, balance, config = DEFAULT_WITHDRAW_CONFIG) => {
	const value = Number(amount || 0);
	if (!Number.isFinite(value) || value <= 0) {
		return '请输入有效提现金额';
	}
	const minAmount = Number(config?.minAmount || 0);
	const maxAmount = Number(config?.maxAmount || 0);
	if (minAmount > 0 && value < minAmount) {
		return `最小提现金额为${minAmount}元`;
	}
	if (maxAmount > 0 && value > maxAmount) {
		return `最大提现金额为${maxAmount}元`;
	}
	if (value > Number(balance || 0)) {
		return '提现金额不能超过可提现余额';
	}
	if (calcWithdrawActualAmount(value, config) <= 0) {
		return '提现金额扣除手续费后必须大于0';
	}
	return '';
};

/** 确认页应付金额 = 租金 + 押金 + 配送 - 优惠券抵扣 */
export const calcConfirmPayableAmount = (rentAmount, depositAmount, deliveryAmount, couponAmount) => {
	const rent = Number(rentAmount || 0);
	const deposit = Number(depositAmount || 0);
	const delivery = Number(deliveryAmount || 0);
	const coupon = Number(couponAmount || 0);
	return Number((rent + deposit + delivery - coupon).toFixed(2));
};

/** 提现页「全部提现」填充值 */
export const calcMaxWithdrawFillAmount = (balance, config = DEFAULT_WITHDRAW_CONFIG) => {
	const maxAmount = Number(config?.maxAmount || 0);
	const available = Number(balance || 0);
	if (maxAmount > 0) {
		return Math.min(available, maxAmount);
	}
	return available;
};
