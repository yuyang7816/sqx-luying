/** 确认订单页：待结算数据本地暂存（购物车 → 确认页） */
const CONFIRM_STORAGE_KEY = 'pendingConfirmPayload';
const CONFIRM_COUPON_CONTEXT_KEY = 'confirmCouponContext';
const CONFIRM_COUPON_SELECTION_KEY = 'confirmCouponSelection';

export const setPendingConfirmPayload = (payload) => {
	uni.setStorageSync(CONFIRM_STORAGE_KEY, payload);
};

export const getPendingConfirmPayload = () => uni.getStorageSync(CONFIRM_STORAGE_KEY) || null;

export const clearPendingConfirmPayload = () => {
	uni.removeStorageSync(CONFIRM_STORAGE_KEY);
};

/** 跳转选券页时写入订单金额上下文 */
export const setConfirmCouponContext = (context = {}) => {
	uni.setStorageSync(CONFIRM_COUPON_CONTEXT_KEY, context);
};

export const getConfirmCouponContext = () => uni.getStorageSync(CONFIRM_COUPON_CONTEXT_KEY) || null;

export const clearConfirmCouponContext = () => {
	uni.removeStorageSync(CONFIRM_COUPON_CONTEXT_KEY);
};

/** 选券页回写：coupon 为 null 表示不使用优惠券 */
export const setConfirmCouponSelection = (coupon) => {
	uni.setStorageSync(CONFIRM_COUPON_SELECTION_KEY, coupon ? {
		id: coupon.id,
		name: coupon.name,
		amount: coupon.amount,
		condition: coupon.condition
	} : { id: 0 });
};

export const consumeConfirmCouponSelection = () => {
	const selection = uni.getStorageSync(CONFIRM_COUPON_SELECTION_KEY);
	if (selection === '' || selection === undefined || selection === null) {
		return undefined;
	}
	uni.removeStorageSync(CONFIRM_COUPON_SELECTION_KEY);
	return selection;
};

export const createEmptyConfirmDraft = () => ({
	source: '',
	goodsItems: [],
	rentAmount: 0,
	depositAmount: 0,
	deliveryAmount: 0,
	couponAmount: 0,
	payAmount: 0,
	pickupType: 'store',
	store: null,
	address: null,
	contactName: '',
	contactPhone: '',
	remark: ''
});
