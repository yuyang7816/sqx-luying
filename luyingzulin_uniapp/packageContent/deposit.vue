<template>
	<view class="page">
		<view v-if="pageLoading" class="loading-card"><text>加载押金信息...</text></view>
		<template v-else>
			<view class="deposit-card">
				<text class="label">当前租赁押金</text>
				<text class="amount">¥{{ summary.heldAmount.toFixed(2) }}</text>
				<text class="status">{{ summary.statusText }}</text>
				<text class="sub-line">可用 ¥{{ summary.availableAmount.toFixed(2) }} · 冻结 ¥{{ summary.frozenAmount.toFixed(2) }}</text>
				<text v-if="minDepositRequired" class="min-line">最低可用押金 ¥{{ minDepositBalanceText }}</text>
				<text v-if="needPayAmount > 0" class="need-pay">待补缴 ¥{{ needPayAmountText }}</text>
			</view>

			<view class="panel entry-panel" @click="goDepositDetail">
				<view class="entry-main">
					<text class="entry-title">押金明细</text>
					<text class="entry-desc">查看缴纳、冻结、退还等流水记录</text>
				</view>
				<up-icon name="arrow-right" size="14" color="#b0b8c4"></up-icon>
			</view>

			<view class="panel">
				<text class="title">租赁押金操作</text>
				<text v-if="minDepositRequired" class="config-tip">
					平台要求账户可用押金不低于 ¥{{ minDepositBalanceText }}，下单时会从可用余额中冻结商品押金。
				</text>
				<view class="field-row">
					<view class="field-box">
						<input
							v-model="actionAmount"
							class="field-input"
							type="digit"
							:placeholder="amountPlaceholder"
						/>
						<text class="field-unit">元</text>
					</view>
				</view>
				<view v-if="needPayAmount > 0" class="quick-row">
					<text class="quick-chip" @click="fillNeedPayAmount">一键填入待补缴 ¥{{ needPayAmountText }}</text>
				</view>
				<view class="btn-row">
					<button class="solid-btn" :loading="paying" :disabled="paying || refunding" @click="openPayPopup">缴纳租赁押金</button>
					<button class="outline-btn" :loading="refunding" :disabled="paying || refunding" @click="handleRefund">申请退还</button>
				</view>
				<text class="rule-link" @click="openRule">查看押金规则</text>
			</view>
		</template>

		<PaymentMethodPopup
			v-model:show="paymentPopupVisible"
			:amount="payAmountText"
			:loading="paying"
			@confirm="confirmPay"
		/>
	</view>
</template>

<script setup>
	import { computed, ref } from 'vue';
	import { onShow } from '@dcloudio/uni-app';
	import PaymentMethodPopup from '@/components/PaymentMethodPopup.vue';
	import { applyDepositPay, applyDepositRefund, fetchDepositSummary } from '@/common/api/deposit';
	import { ensureLogin } from '@/common/auth';
	import { runAction } from '@/common/actionFeedback';
	import { formatMoney } from '@/common/rental/constants';
	import {
		launchPayment,
		navigateAfterPayment
	} from '@/common/payment';

	const emptySummary = () => ({
		totalAmount: 0,
		frozenAmount: 0,
		availableAmount: 0,
		heldAmount: 0,
		minDepositBalance: 0,
		status: 1,
		statusText: '未缴纳'
	});

	const summary = ref(emptySummary());
	const actionAmount = ref('');
	const pageLoading = ref(false);
	const paying = ref(false);
	const refunding = ref(false);
	const paymentPopupVisible = ref(false);

	const minDepositRequired = computed(() => Number(summary.value.minDepositBalance || 0) > 0);
	const minDepositBalanceText = computed(() => Number(summary.value.minDepositBalance || 0).toFixed(2));
	const needPayAmount = computed(() => {
		if (!minDepositRequired.value) return 0;
		const gap = Number(summary.value.minDepositBalance || 0) - Number(summary.value.availableAmount || 0);
		return Math.max(Number(gap.toFixed(2)), 0);
	});
	const needPayAmountText = computed(() => needPayAmount.value.toFixed(2));
	const amountPlaceholder = computed(() => {
		if (needPayAmount.value > 0) {
			return `建议至少缴纳 ¥${needPayAmountText.value}`;
		}
		if (minDepositRequired.value) {
			return `最低可用押金 ¥${minDepositBalanceText.value}`;
		}
		return '请输入操作金额';
	});

	const payAmountText = computed(() => {
		const value = Number(actionAmount.value || 0);
		return Number.isFinite(value) && value > 0 ? formatMoney(value) : '0.00';
	});

	const syncDeposit = async () => {
		pageLoading.value = true;
		try {
			summary.value = await fetchDepositSummary();
		} catch (error) {
			summary.value = emptySummary();
		} finally {
			pageLoading.value = false;
		}
	};

	onShow(async () => {
		if (!ensureLogin('/packageContent/deposit')) return;
		await syncDeposit();
	});

	const resolveAmount = () => {
		const value = Number(actionAmount.value || 0);
		if (!Number.isFinite(value) || value <= 0) {
			throw new Error('请输入有效金额');
		}
		return value;
	};

	const openPayPopup = () => {
		if (paying.value || refunding.value) return;
		try {
			resolveAmount();
		} catch (error) {
			uni.showToast({ title: error.message || '请输入有效金额', icon: 'none' });
			return;
		}
		paymentPopupVisible.value = true;
	};

	const fillNeedPayAmount = () => {
		actionAmount.value = needPayAmountText.value;
	};

	const confirmPay = async (methodKey) => {
		if (paying.value) return;
		paying.value = true;
		let amount = 0;
		try {
			amount = resolveAmount();
		} catch (error) {
			uni.showToast({ title: error.message || '请输入有效金额', icon: 'none' });
			paying.value = false;
			return;
		}
		await runAction({
			action: async () => {
				const payOrder = await applyDepositPay({ amount });
				const payRes = await launchPayment({
					amount,
					orderNo: payOrder.payNo,
					methodKey,
					businessType: 'deposit_pay'
				});
				return { payOrder, payRes, amount };
			},
			errorText: '缴纳失败，请稍后重试',
			onSuccess: ({ payRes }) => {
				paymentPopupVisible.value = false;
				navigateAfterPayment({ payRes, businessType: 'deposit_pay', scene: 'deposit' });
			}
		});
		paying.value = false;
	};

	const handleRefund = async () => {
		if (refunding.value) return;
		refunding.value = true;
		const { ok } = await runAction({
			action: () => applyDepositRefund({ amount: resolveAmount() }),
			successText: '退押申请已提交',
			errorText: '退押失败，请稍后重试'
		});
		refunding.value = false;
		if (!ok) return;
		actionAmount.value = '';
		syncDeposit();
	};

	const goDepositDetail = () => {
		uni.navigateTo({ url: '/packageContent/deposit-detail' });
	};

	const openRule = () => uni.navigateTo({ url: '/packageContent/agreement?type=deposit' });
</script>

<style scoped>
	.page {
		box-sizing: border-box;
		min-height: 100vh;
		padding: 32rpx 24rpx 48rpx;
		background: #f7f8f6;
		overflow-x: hidden;
	}

	.loading-card {
		padding: 56rpx 28rpx;
		border-radius: 16rpx;
		background: #fff;
		text-align: center;
		font-size: 26rpx;
		color: #8a9485;
	}

	.deposit-card {
		box-sizing: border-box;
		position: relative;
		width: 100%;
		margin-bottom: 28rpx;
		padding: 38rpx 32rpx 36rpx;
		background: linear-gradient(135deg, #78cc19 0%, #56b50f 100%);
		border-radius: 16rpx;
		overflow: hidden;
		box-shadow: 0 14rpx 30rpx rgba(79, 168, 18, 0.18);
	}

	.label,
	.amount,
	.status,
	.sub-line,
	.min-line,
	.need-pay,
	.title,
	.config-tip,
	.entry-title,
	.entry-desc,
	.rule-link {
		display: block;
		position: relative;
		z-index: 1;
	}

	.label {
		font-size: 24rpx;
		font-weight: 500;
		color: rgba(255, 255, 255, 0.88);
		line-height: 1.4;
	}

	.amount {
		margin-top: 10rpx;
		font-size: 68rpx;
		line-height: 1.2;
		font-weight: 800;
		color: #ffffff;
	}

	.status {
		margin-top: 16rpx;
		width: auto;
		display: inline-block;
		padding: 6rpx 18rpx;
		border-radius: 6rpx;
		font-size: 22rpx;
		color: #4a7a0a;
		background: rgba(255, 255, 255, 0.95);
		font-weight: 600;
	}

	.sub-line {
		margin-top: 18rpx;
		font-size: 24rpx;
		line-height: 1.5;
		color: rgba(255, 255, 255, 0.82);
	}

	.min-line {
		margin-top: 8rpx;
		font-size: 22rpx;
		line-height: 1.5;
		color: rgba(255, 255, 255, 0.92);
	}

	.need-pay {
		margin-top: 12rpx;
		display: inline-block;
		padding: 6rpx 16rpx;
		border-radius: 6rpx;
		font-size: 22rpx;
		font-weight: 600;
		color: #fff7e6;
		background: rgba(255, 140, 0, 0.28);
	}

	.panel {
		box-sizing: border-box;
		width: 100%;
		margin-bottom: 28rpx;
		padding: 32rpx;
		background: #ffffff;
		border: 2rpx solid #eef1ea;
		border-radius: 16rpx;
		box-shadow: 0 8rpx 20rpx rgba(31, 45, 20, 0.035);
		overflow: hidden;
	}

	.entry-panel {
		padding-top: 30rpx;
		padding-bottom: 30rpx;
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	.entry-main {
		flex: 1;
		min-width: 0;
	}

	.entry-title {
		font-size: 28rpx;
		font-weight: 700;
		color: #182116;
	}

	.entry-desc {
		margin-top: 8rpx;
		font-size: 24rpx;
		line-height: 1.5;
		color: #7f8a7a;
	}

	.title {
		font-size: 30rpx;
		font-weight: 700;
		color: #182116;
	}

	.config-tip {
		margin-top: 14rpx;
		font-size: 24rpx;
		line-height: 1.6;
		color: #667064;
	}

	.field-row {
		margin-top: 24rpx;
	}

	.field-box {
		display: flex;
		align-items: center;
		height: 96rpx;
		padding: 0 26rpx;
		border-radius: 10rpx;
		background: #fafbf9;
		border: 2rpx solid #e4e9df;
	}

	.field-input {
		flex: 1;
		height: 96rpx;
		font-size: 28rpx;
		font-weight: 600;
		color: #1f2920;
	}

	.field-unit {
		flex-shrink: 0;
		font-size: 26rpx;
		color: #7d877b;
	}

	.quick-row {
		margin-top: 18rpx;
	}

	.quick-chip {
		display: inline-block;
		padding: 10rpx 18rpx;
		border-radius: 8rpx;
		font-size: 22rpx;
		color: #4a9d0c;
		background: #f3fbe9;
		border: 2rpx solid #d6edbd;
	}

	.btn-row {
		display: flex;
		justify-content: space-between;
		margin-top: 28rpx;
		gap: 16rpx;
	}

	.solid-btn,
	.outline-btn {
		box-sizing: border-box;
		min-width: 0;
		flex: 1;
		height: 92rpx;
		line-height: 92rpx;
		margin: 0;
		border-radius: 10rpx;
		font-size: 28rpx;
		font-weight: 600;
	}

	.solid-btn {
		color: #ffffff;
		background: #61bd13;
		border: none;
		box-shadow: 0 8rpx 16rpx rgba(97, 189, 19, 0.18);
	}

	.outline-btn {
		color: #4ca40e;
		background: #f8fdf3;
		border: 2rpx solid #cfe9b6;
	}

	.rule-link {
		margin-top: 24rpx;
		text-align: center;
		font-size: 24rpx;
		color: #6f806b;
		padding: 8rpx 12rpx;
	}
</style>
