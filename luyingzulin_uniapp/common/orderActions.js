import { runAction } from '@/common/actionFeedback';
import { cancelOrder } from '@/common/api/order';
import {
	getAvailablePaymentMethods,
	launchPayment,
	navigateAfterPayment
} from '@/common/payment';
import { requestOrderSubscribeMessage } from '@/common/subscribeMessage';

/** 取消订单前二次确认 */
export const confirmCancelOrder = async (
	orderId,
	{ paid = false, successText = '订单已取消', errorText = '取消订单失败，请稍后重试' } = {}
) => {
	const content = paid
		? '确定取消订单吗？款项将原路退回，库存预占将释放。'
		: '确定要取消该订单吗？取消后库存预占将释放。';
	const confirmed = await new Promise((resolve) => {
		uni.showModal({
			title: '取消订单',
			content,
			confirmText: '确认取消',
			cancelText: '再想想',
			success: (res) => resolve(Boolean(res.confirm)),
			fail: () => resolve(false)
		});
	});
	if (!confirmed) {
		return { ok: false, cancelled: true };
	}
	return runAction({
		action: () => cancelOrder(orderId),
		successText,
		errorText
	});
};

/** 选择支付方式（多种方式时弹出 ActionSheet） */
export const pickPaymentMethod = async () => {
	const methods = await getAvailablePaymentMethods();
	if (!methods.length) {
		uni.showToast({ title: '暂无可用支付方式', icon: 'none' });
		return null;
	}
	if (methods.length === 1) {
		return methods[0].key;
	}
	return new Promise((resolve) => {
		uni.showActionSheet({
			itemList: methods.map((item) => item.label),
			success: (res) => resolve(methods[res.tapIndex]?.key || null),
			fail: () => resolve(null)
		});
	});
};

/** 指定支付方式发起支付并跳转目标页 */
export const executeOrderPayment = async ({ orderId, orderNo, payAmount, methodKey, scene = 'pay' }) => {
	if (!methodKey) {
		return { ok: false, cancelled: true };
	}

	const { ok, result: payRes } = await runAction({
		action: async () => {
			await requestOrderSubscribeMessage();
			return launchPayment({
				amount: payAmount,
				orderNo: orderNo || String(orderId),
				methodKey,
				businessType: scene === 'renew' ? 'rent_renew' : 'rent_order'
			});
		},
		errorText: '支付发起失败，请稍后重试'
	});

	if (!ok || !payRes) {
		return { ok: false };
	}

	navigateAfterPayment({
		payRes,
		scene,
		businessType: scene === 'renew' ? 'rent_renew' : 'rent_order'
	});
	return { ok: true, payRes, methodKey };
};

/** 待支付订单：选支付方式 → 拉起支付 → 跳转订单列表（单方式时直接支付） */
export const payPendingOrder = async ({ orderId, orderNo, payAmount, scene = 'pay', methodKey } = {}) => {
	const resolvedMethodKey = methodKey || await pickPaymentMethod();
	return executeOrderPayment({
		orderId,
		orderNo,
		payAmount,
		methodKey: resolvedMethodKey,
		scene
	});
};
