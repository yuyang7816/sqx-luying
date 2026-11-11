<template>
	<view class="page">
		<view v-if="pageLoading && !hasLoaded" class="loading-card"><text>加载商户保证金...</text></view>
		<template v-else-if="!redirecting">
			<view class="deposit-card">
				<text class="label">应缴保证金</text>
				<text class="amount">¥{{ summary.requiredAmount.toFixed(2) }}</text>
				<text class="status" :class="{ warn: summary.refundPending || summary.needPayAmount > 0 }">{{ summary.statusText }}</text>
				<text class="sub-line">已缴 ¥{{ summary.heldAmount.toFixed(2) }} · 可用 ¥{{ summary.availableAmount.toFixed(2) }} · 冻结 ¥{{ summary.frozenAmount.toFixed(2) }}</text>
				<text v-if="summary.needPayAmount > 0" class="need-pay">待缴 ¥{{ summary.needPayAmount.toFixed(2) }}</text>
				<text v-if="summary.refundPending" class="need-pay">退保审核中 ¥{{ summary.pendingRefundAmount.toFixed(2) }}</text>
				<text class="hint-line">保证金金额由平台统一配置，用于履约担保，与用户租赁押金、商品押金无关</text>
			</view>
			<view class="panel">
				<text class="title">保证金操作</text>
				<view v-if="summary.needPayAmount > 0" class="btn-row">
					<button class="solid-btn" :loading="paying" :disabled="paying || refunding" @click="openPayPopup">缴纳保证金 ¥{{ summary.needPayAmount.toFixed(2) }}</button>
				</view>
				<view v-else-if="summary.depositPaid && !summary.refundPending" class="done-tip">保证金已缴足，无需重复缴纳</view>
				<view v-else-if="summary.refundPending" class="pending-tip">退保申请审核中，请等待平台处理</view>
				<view v-if="summary.canRefund" class="btn-row">
					<button class="outline-btn" :loading="refunding" :disabled="paying || refunding" @click="handleRefund">申请退还 ¥{{ summary.availableAmount.toFixed(2) }}</button>
				</view>
				<text class="tip-text">商户保证金用于平台经营履约担保，与用户端「租赁押金」及商品「商品押金」为不同账户。</text>
			</view>
			<view class="panel">
				<text class="title">保证金流水</text>
				<view v-if="!records.length" class="empty-tip">暂无保证金流水</view>
				<view v-for="item in records" :key="item.id || item.logNo" class="record-row">
					<view class="record-icon" :class="item.type">
						<up-icon :name="item.icon" size="18" :color="recordIconColor(item.type)"></up-icon>
					</view>
					<view class="record-main"><text class="record-title">{{ item.title }}</text><text class="record-time">{{ item.time }}</text></view>
					<text class="record-amount" :class="item.type">{{ item.amount }}</text>
				</view>
			</view>
		</template>

		<PaymentMethodPopup
			v-model:show="payPopupVisible"
			:amount="payAmount"
			:loading="paying"
			@confirm="handlePayConfirm"
		/>
	</view>
</template>

<script setup>
	import { computed, ref } from 'vue';
	import { onShow } from '@dcloudio/uni-app';
	import PaymentMethodPopup from '@/components/PaymentMethodPopup.vue';
	import { ensureMerchantApproved } from '@/common/api/merchant';
	import {
		applyMerchantSecurityDepositPay,
		applyMerchantSecurityDepositRefund,
		cancelMerchantSecurityDepositPay,
		fetchMerchantSecurityDepositLogPage,
		fetchMerchantSecurityDepositPayStatus,
		fetchMerchantSecurityDepositSummary
	} from '@/common/api/merchantWallet';
	import { ensureLogin } from '@/common/auth';
	import { runAction } from '@/common/actionFeedback';
	import { launchPayment } from '@/common/payment';

	const emptySummary = () => ({
		totalAmount: 0,
		frozenAmount: 0,
		availableAmount: 0,
		heldAmount: 0,
		requiredAmount: 0,
		needPayAmount: 0,
		pendingRefundAmount: 0,
		depositRequired: false,
		depositPaid: false,
		refundPending: false,
		canRefund: false,
		status: 1,
		statusText: '未缴纳'
	});

	const summary = ref(emptySummary());
	const records = ref([]);
	const pageLoading = ref(false);
	const hasLoaded = ref(false);
	const redirecting = ref(false);
	const paying = ref(false);
	const refunding = ref(false);
	const payPopupVisible = ref(false);

	const payAmount = computed(() => Number(summary.value.needPayAmount || 0));

	const recordIconColor = (type) => {
		if (type === 'plus') return '#7ed321';
		if (type === 'pending') return '#f0ad4e';
		if (type === 'muted') return '#9aa596';
		return '#ff8a8a';
	};

	const leaveWhenNotRequired = () => {
		if (redirecting.value) return;
		redirecting.value = true;
		uni.showToast({ title: '当前无需缴纳保证金', icon: 'none' });
		setTimeout(() => {
			uni.navigateBack({
				fail: () => {
					uni.redirectTo({ url: '/packageMerchant/wallet' });
				}
			});
		}, 300);
	};

	const syncDeposit = async () => {
		if (!hasLoaded.value) {
			pageLoading.value = true;
		}
		try {
			const [summaryVo, logPage] = await Promise.all([
				fetchMerchantSecurityDepositSummary(),
				fetchMerchantSecurityDepositLogPage({ page: 1, limit: 20 })
			]);
			if (!summaryVo.depositRequired && Number(summaryVo.requiredAmount || 0) <= 0) {
				leaveWhenNotRequired();
				return;
			}
			summary.value = summaryVo;
			records.value = logPage.records || [];
			hasLoaded.value = true;
		} catch {
			if (!hasLoaded.value) {
				summary.value = emptySummary();
				records.value = [];
			}
		} finally {
			pageLoading.value = false;
		}
	};

	onShow(async () => {
		if (!ensureLogin('/packageMerchant/wallet-deposit')) return;
		try {
			await ensureMerchantApproved({ redirect: true, toast: true });
			await syncDeposit();
		} catch {
			// ignore
		}
	});

	const openPayPopup = () => {
		if (paying.value || refunding.value || payAmount.value <= 0) return;
		payPopupVisible.value = true;
	};

	const waitDepositPayCompleted = async (payNo, { maxAttempts = 8, intervalMs = 1500 } = {}) => {
		for (let i = 0; i < maxAttempts; i += 1) {
			const statusVo = await fetchMerchantSecurityDepositPayStatus(payNo, { silentError: true }).catch(() => null);
			if (statusVo?.paid) return true;
			if (statusVo?.status === 'cancelled') return false;
			await new Promise((resolve) => setTimeout(resolve, intervalMs));
		}
		return false;
	};

	const handlePayConfirm = async (methodKey) => {
		if (paying.value || payAmount.value <= 0) return;
		paying.value = true;
		const amount = payAmount.value;
		let payNo = '';
		const { ok } = await runAction({
			action: async () => {
				const payOrder = await applyMerchantSecurityDepositPay({});
				payNo = payOrder.payNo;
				const payRes = await launchPayment({
					amount,
					orderNo: payOrder.payNo,
					methodKey,
					businessType: 'merchant_deposit_pay'
				});
				return { payOrder, payRes };
			},
			errorText: '缴纳失败，请稍后重试',
			onSuccess: async ({ payOrder, payRes }) => {
				payPopupVisible.value = false;
				if (payRes.result === 'pending') {
					uni.showToast({ title: '支付确认中', icon: 'none' });
					const paid = await waitDepositPayCompleted(payOrder.payNo);
					uni.showToast({
						title: paid ? '保证金缴纳成功' : '支付结果确认中，请稍后刷新',
						icon: paid ? 'success' : 'none'
					});
				} else {
					uni.showToast({ title: '保证金缴纳成功', icon: 'success' });
				}
				await syncDeposit();
			},
			onError: async () => {
				if (payNo) {
					await cancelMerchantSecurityDepositPay(payNo, { silentError: true }).catch(() => {});
					await syncDeposit();
				}
			}
		});
		paying.value = false;
		if (!ok) return;
	};

	const handleRefund = async () => {
		if (refunding.value || !summary.value.canRefund) return;
		const amount = Number(summary.value.availableAmount || 0);
		if (amount <= 0) return;
		refunding.value = true;
		const { ok } = await runAction({
			action: () => applyMerchantSecurityDepositRefund({ amount }),
			errorText: '退保失败，请稍后重试',
			onSuccess: async () => {
				uni.showToast({ title: '退保申请已提交', icon: 'success' });
				await syncDeposit();
			}
		});
		refunding.value = false;
		if (!ok) return;
	};
</script>

<style scoped>
	.page {
		box-sizing: border-box;
		min-height: 100vh;
		padding: 24rpx;
		background: #f6f8f3;
		overflow-x: hidden;
	}

	.loading-card {
		padding: 48rpx 32rpx;
		border-radius: 28rpx;
		background: #fff;
		text-align: center;
		color: #8a94a6;
		font-size: 28rpx;
	}

	.deposit-card {
		box-sizing: border-box;
		padding: 40rpx 32rpx 32rpx;
		border-radius: 32rpx;
		background: linear-gradient(160deg, #7ed321 0%, #6bc01a 50%, #5aad15 100%);
		box-shadow: 0 12rpx 40rpx rgba(126, 211, 33, 0.22);
		color: #fff;
		margin-bottom: 24rpx;
	}

	.label {
		display: block;
		font-size: 24rpx;
		color: rgba(255, 255, 255, 0.92);
		margin-bottom: 12rpx;
	}

	.amount {
		display: block;
		font-size: 64rpx;
		font-weight: 800;
		line-height: 1.1;
		margin-bottom: 8rpx;
	}

	.status {
		display: block;
		font-size: 24rpx;
		color: rgba(255, 255, 255, 0.92);
		margin-bottom: 16rpx;
	}

	.status.warn {
		color: #fff7d6;
		font-weight: 600;
	}

	.sub-line,
	.need-pay,
	.hint-line {
		display: block;
		font-size: 22rpx;
		line-height: 1.5;
		color: rgba(255, 255, 255, 0.88);
	}

	.need-pay {
		margin-top: 8rpx;
		color: #fff7d6;
		font-weight: 600;
	}

	.hint-line {
		margin-top: 12rpx;
		color: rgba(255, 255, 255, 0.78);
	}

	.panel {
		box-sizing: border-box;
		padding: 28rpx;
		border-radius: 28rpx;
		background: #fff;
		margin-bottom: 24rpx;
		box-shadow: 0 8rpx 28rpx rgba(90, 173, 21, 0.06);
	}

	.title {
		display: block;
		font-size: 30rpx;
		font-weight: 700;
		color: #2f3a2f;
		margin-bottom: 20rpx;
	}

	.btn-row {
		display: flex;
		flex-direction: column;
		gap: 16rpx;
		margin-bottom: 16rpx;
	}

	.solid-btn,
	.outline-btn {
		width: 100%;
		height: 84rpx;
		line-height: 84rpx;
		border-radius: 42rpx;
		font-size: 28rpx;
		font-weight: 700;
		margin: 0;
	}

	.solid-btn {
		background: linear-gradient(135deg, #7ed321, #5aad15);
		color: #fff;
		border: none;
		box-shadow: 0 8rpx 24rpx rgba(126, 211, 33, 0.24);
	}

	.outline-btn {
		background: #fff;
		color: #5b8f00;
		border: 2rpx solid #7ed321;
	}

	.done-tip,
	.pending-tip {
		padding: 20rpx 0;
		font-size: 26rpx;
	}

	.done-tip {
		color: #5b8f00;
	}

	.pending-tip {
		color: #f0ad4e;
	}

	.tip-text {
		display: block;
		font-size: 22rpx;
		line-height: 1.6;
		color: #8a9688;
	}

	.empty-tip {
		padding: 32rpx 0;
		text-align: center;
		color: #9aa596;
		font-size: 26rpx;
	}

	.record-row {
		display: flex;
		align-items: center;
		padding: 20rpx 0;
		border-bottom: 1rpx solid #eef2ea;
	}

	.record-row:last-child {
		border-bottom: none;
	}

	.record-icon {
		width: 56rpx;
		height: 56rpx;
		border-radius: 50%;
		display: flex;
		align-items: center;
		justify-content: center;
		margin-right: 16rpx;
		background: #f6f8f3;
	}

	.record-icon.pending {
		background: #fff8eb;
	}

	.record-icon.muted {
		background: #f3f4f2;
	}

	.record-main {
		flex: 1;
		min-width: 0;
	}

	.record-title {
		display: block;
		font-size: 28rpx;
		color: #2f3a2f;
	}

	.record-time {
		display: block;
		font-size: 22rpx;
		color: #9aa596;
		margin-top: 4rpx;
	}

	.record-amount {
		font-size: 28rpx;
		font-weight: 600;
	}

	.record-amount.plus {
		color: #5b8f00;
	}

	.record-amount.minus {
		color: #d9534f;
	}

	.record-amount.pending {
		color: #f0ad4e;
	}

	.record-amount.muted {
		color: #9aa596;
	}
</style>
