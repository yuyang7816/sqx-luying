<template>
	<view class="page">
		<view v-if="pageLoading" class="loading-card"><text>加载钱包信息...</text></view>
		<template v-else>
			<view class="hero-card">
				<text class="hero-title">账户充值</text>
				<text class="hero-amount">¥{{ balanceText }}</text>
				<text class="hero-desc">充值后可用于租赁订单余额支付，实时到账。</text>
			</view>

			<view class="card">
				<text class="section-title">充值金额</text>
				<input v-model="amountInput" class="amount-input" type="digit" placeholder="请输入充值金额" />
				<view class="quick-row">
					<text v-for="item in quickAmounts" :key="item" class="quick-chip"
						:class="{ active: Number(amountInput) === item }" @click="amountInput = String(item)">
						¥{{ item }}
					</text>
				</view>
				<text class="rule-text">单次充值 ¥{{ minAmount }} ~ ¥{{ maxAmount }}</text>
			</view>

			<view class="card">
				<text class="section-title">支付方式</text>
				<view v-for="item in paymentMethods" :key="item.key" class="payment-row"
					:class="{ active: payMethod === item.key }" @click="payMethod = item.key">
					<view class="payment-main">
						<text class="payment-title">{{ item.label }}</text>
						<text class="payment-desc">{{ item.desc }}</text>
					</view>
					<up-icon :name="payMethod === item.key ? 'checkmark-circle-fill' : 'checkmark-circle'" size="18"
						:color="payMethod === item.key ? '#59b50e' : '#9aa2b1'"></up-icon>
				</view>
				<text v-if="!paymentMethods.length" class="empty-tip">当前环境暂无可用在线支付方式</text>
			</view>

			<view class="submit-btn" :loading="paying" :disabled="paying || !paymentMethods.length" @click="handleRecharge">
				确认充值</view>
		</template>
	</view>
</template>

<script setup>
	import {
		computed,
		ref
	} from 'vue';
	import {
		onShow
	} from '@dcloudio/uni-app';
	import {
		applyWalletRecharge,
		fetchWalletSummary
	} from '@/common/api/wallet';
	import {
		ensureLogin
	} from '@/common/auth';
	import {
		runAction
	} from '@/common/actionFeedback';
	import {
		consumePendingPayment,
		getOnlinePaymentMethods,
		launchPayment,
		navigateAfterPayment,
		pollPaymentStatus
	} from '@/common/payment';

	const minAmount = 1;
	const maxAmount = 5000;
	const quickAmounts = [50, 100, 200, 500];

	const balance = ref(0);
	const amountInput = ref('');
	const pageLoading = ref(false);
	const paying = ref(false);
	const paymentMethods = ref([]);
	const payMethod = ref('');

	const balanceText = computed(() => Number(balance.value || 0).toFixed(2));

	const loadPaymentMethods = () => {
		const methods = getOnlinePaymentMethods();
		paymentMethods.value = methods;
		payMethod.value = methods[0]?.key || '';
	};

	const syncWallet = async () => {
		pageLoading.value = true;
		try {
			const wallet = await fetchWalletSummary();
			balance.value = wallet.balance || wallet.amount || 0;
		} catch (error) {
			balance.value = 0;
		} finally {
			pageLoading.value = false;
		}
	};

	const syncPendingRecharge = async () => {
		const pending = consumePendingPayment();
		if (!pending || pending.businessType !== 'wallet_recharge') {
			return;
		}
		const result = await pollPaymentStatus({
			businessType: 'wallet_recharge',
			orderNo: pending.orderNo,
			maxAttempts: 5,
			intervalMs: 1200
		});
		if (result === 'success') {
			uni.showToast({
				title: '充值成功',
				icon: 'success'
			});
			await syncWallet();
		}
	};

	onShow(async () => {
		if (!ensureLogin('/packageUser/wallet-recharge')) return;
		loadPaymentMethods();
		await syncWallet();
		await syncPendingRecharge();
	});

	const resolveAmount = () => {
		const value = Number(amountInput.value || 0);
		if (!Number.isFinite(value) || value < minAmount) {
			throw new Error(`充值金额不能少于 ¥${minAmount}`);
		}
		if (value > maxAmount) {
			throw new Error(`充值金额不能超过 ¥${maxAmount}`);
		}
		return value;
	};

	const handleRecharge = async () => {
		if (paying.value || !paymentMethods.value.length) return;
		paying.value = true;
		let amount = 0;
		try {
			amount = resolveAmount();
		} catch (error) {
			uni.showToast({
				title: error.message || '请输入有效金额',
				icon: 'none'
			});
			paying.value = false;
			return;
		}
		await runAction({
			action: async () => {
				const payOrder = await applyWalletRecharge({
					amount
				});
				const payNo = payOrder?.payNo || payOrder;
				const payRes = await launchPayment({
					amount,
					orderNo: payNo,
					methodKey: payMethod.value,
					businessType: 'wallet_recharge'
				});
				return {
					payOrder: {
						payNo
					},
					payRes,
					amount
				};
			},
			errorText: '充值失败，请稍后重试',
			onSuccess: ({
				payOrder,
				payRes
			}) => {
				navigateAfterPayment({
					payRes,
					businessType: 'wallet_recharge',
					scene: 'wallet',
					orderNo: payOrder?.payNo
				});
			}
		});
		paying.value = false;
	};
</script>

<style scoped>
	.page {
		box-sizing: border-box;
		min-height: 100vh;
		padding: 24rpx;
		background: #f6f8f3;
	}

	.loading-card,
	.hero-card,
	.card {
		box-sizing: border-box;
		width: 100%;
		border-radius: 28rpx;
		background: #fff;
	}

	.loading-card {
		padding: 48rpx 32rpx;
		text-align: center;
		color: #8a94a6;
	}

	.hero-card {
		padding: 36rpx 32rpx;
		margin-bottom: 24rpx;
		background: linear-gradient(160deg, #7ed321 0%, #6bc01a 100%);
	}

	.hero-title,
	.hero-amount,
	.hero-desc,
	.section-title,
	.rule-text,
	.empty-tip,
	.payment-title,
	.payment-desc {
		display: block;
	}

	.hero-title {
		font-size: 28rpx;
		color: rgba(255, 255, 255, 0.92);
	}

	.hero-amount {
		margin-top: 12rpx;
		font-size: 56rpx;
		font-weight: 800;
		color: #fff;
	}

	.hero-desc {
		margin-top: 12rpx;
		font-size: 22rpx;
		line-height: 1.6;
		color: rgba(255, 255, 255, 0.88);
	}

	.card {
		margin-bottom: 24rpx;
		padding: 28rpx;
	}

	.section-title {
		margin-bottom: 20rpx;
		font-size: 28rpx;
		font-weight: 700;
		color: #1f2937;
	}

	.amount-input {
		box-sizing: border-box;
		width: 100%;
		height: 88rpx;
		padding: 0 24rpx;
		border-radius: 20rpx;
		background: #f8fafc;
		font-size: 36rpx;
		font-weight: 700;
		color: #1f2937;
	}

	.quick-row {
		display: flex;
		flex-wrap: wrap;
		gap: 16rpx;
		margin-top: 20rpx;
	}

	.quick-chip {
		padding: 12rpx 24rpx;
		border-radius: 999rpx;
		background: #f3f6ef;
		font-size: 24rpx;
		color: #4b5563;
	}

	.quick-chip.active {
		background: #eefadb;
		color: #5b8f00;
		font-weight: 700;
	}

	.rule-text {
		margin-top: 16rpx;
		font-size: 22rpx;
		color: #8a94a6;
	}

	.payment-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 16rpx;
		margin-top: 16rpx;
		padding: 20rpx;
		border-radius: 20rpx;
		background: #f8fafc;
	}

	.payment-row.active {
		background: #f3faeb;
	}

	.payment-main {
		flex: 1;
		min-width: 0;
	}

	.payment-title {
		font-size: 26rpx;
		font-weight: 600;
		color: #1f2937;
	}

	.payment-desc {
		margin-top: 6rpx;
		font-size: 22rpx;
		color: #8a94a6;
	}

	.empty-tip {
		margin-top: 12rpx;
		font-size: 22rpx;
		color: #9aa2b1;
	}

	.submit-btn {
		height: 88rpx;
		line-height: 88rpx;
		border-radius: 44rpx;
		background: linear-gradient(135deg, #7ed321, #6bc01a);
		color: #fff;
		font-size: 28rpx;
		font-weight: 700;
		border: none;
		text-align: center;
	}
</style>