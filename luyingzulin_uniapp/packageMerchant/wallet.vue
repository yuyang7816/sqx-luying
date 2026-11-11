<template>
	<view class="page">
		<view v-if="pageLoading && !hasLoaded" class="loading-card"><text>加载钱包信息...</text></view>
		<template v-else>
			<view class="balance-card">
				<text class="label">商户经营余额</text>
				<text class="amount">¥{{ formatAmount(summary.balance) }}</text>
				<text class="sub">冻结金额 ¥{{ formatAmount(summary.frozenAmount) }} · 与用户个人钱包独立</text>
				<view class="badge-row">
					<text class="badge">可提现</text>
					<text class="badge">手续费 {{ feeRateText }}</text>
				</view>
				<view class="action-row">
					<button class="action-btn primary" @click="goWithdraw">提现</button>
					<button class="action-btn secondary" @click="goWalletRecords">经营明细</button>
					<button class="action-btn secondary" @click="goWithdrawRecords">提现记录</button>
				</view>
			</view>

			<view v-if="showSecurityDeposit" class="deposit-card" @click="goDeposit">
				<view class="deposit-main">
					<text class="deposit-label">商户保证金</text>
					<text class="deposit-amount">{{ depositAmountText }}</text>
					<text class="deposit-sub">{{ depositSubText }}</text>
				</view>
				<view class="deposit-side">
					<text class="deposit-status" :class="{ warn: !securityDeposit.depositPaid || securityDeposit.refundPending }">{{ securityDeposit.statusText || '未缴纳' }}</text>
					<view class="deposit-arrow">
						<up-icon name="arrow-right" color="rgba(255,255,255,0.88)" size="14"></up-icon>
					</view>
				</view>
			</view>
		</template>
	</view>
</template>

<script setup>
	import { computed, ref } from 'vue';
	import { onShow } from '@dcloudio/uni-app';
	import { ensureMerchantApproved } from '@/common/api/merchant';
	import { fetchMerchantWalletSummary, formatWithdrawFeeRate } from '@/common/api/merchantWallet';
	import { ensureLogin } from '@/common/auth';

	const emptySummary = () => ({
		balance: 0,
		frozenAmount: 0,
		securityDeposit: {
			heldAmount: 0,
			availableAmount: 0,
			frozenAmount: 0,
			depositRequired: false,
			depositPaid: true,
			statusText: '无需缴纳'
		},
		withdrawConfig: { serviceFeeRate: 0 }
	});

	const summary = ref(emptySummary());
	const pageLoading = ref(false);
	const hasLoaded = ref(false);

	const formatAmount = (value) => Number(value || 0).toFixed(2);

	const securityDeposit = computed(() => summary.value.securityDeposit || {});

	const syncWallet = async () => {
		if (!hasLoaded.value) {
			pageLoading.value = true;
		}
		try {
			summary.value = await fetchMerchantWalletSummary();
			hasLoaded.value = true;
		} catch {
			if (!hasLoaded.value) {
				summary.value = emptySummary();
			}
		} finally {
			pageLoading.value = false;
		}
	};

	onShow(async () => {
		if (!ensureLogin('/packageMerchant/wallet')) return;
		try {
			await ensureMerchantApproved({ redirect: true, toast: true });
			await syncWallet();
		} catch {
			// 未通过审核会跳转
		}
	});

	const feeRateText = computed(() => formatWithdrawFeeRate(summary.value.withdrawConfig?.serviceFeeRate));
	const showSecurityDeposit = computed(() => {
		const deposit = securityDeposit.value;
		return Boolean(
			deposit.depositRequired
			|| Number(deposit.requiredAmount || 0) > 0
			|| Number(deposit.needPayAmount || 0) > 0
		);
	});
	const depositAmountText = computed(() => {
		const deposit = securityDeposit.value;
		if (!deposit.depositPaid && Number(deposit.needPayAmount || 0) > 0) {
			return `待缴 ¥${formatAmount(deposit.needPayAmount)}`;
		}
		if (!deposit.depositPaid && Number(deposit.requiredAmount || 0) > 0) {
			return `应缴 ¥${formatAmount(deposit.requiredAmount)}`;
		}
		return `¥${formatAmount(deposit.heldAmount)}`;
	});
	const depositSubText = computed(() => {
		const deposit = securityDeposit.value;
		if (deposit.refundPending) {
			return `退保审核中 ¥${formatAmount(deposit.pendingRefundAmount)} · 已缴 ¥${formatAmount(deposit.heldAmount)}`;
		}
		if (!deposit.depositPaid) {
			return `已缴 ¥${formatAmount(deposit.heldAmount)} · 应缴 ¥${formatAmount(deposit.requiredAmount)}`;
		}
		return `可用 ¥${formatAmount(deposit.availableAmount)} · 冻结 ¥${formatAmount(deposit.frozenAmount)}`;
	});

	const goWithdraw = () => uni.navigateTo({ url: '/packageMerchant/wallet-withdraw' });
	const goWalletRecords = () => uni.navigateTo({ url: '/packageMerchant/wallet-record' });
	const goWithdrawRecords = () => uni.navigateTo({ url: '/packageMerchant/wallet-withdraw-records' });
	const goDeposit = () => uni.navigateTo({ url: '/packageMerchant/wallet-deposit' });
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
	}

	.balance-card {
		box-sizing: border-box;
		padding: 40rpx 32rpx 32rpx;
		border-radius: 32rpx;
		background: linear-gradient(160deg, #7ed321 0%, #6bc01a 50%, #5aad15 100%);
		box-shadow: 0 12rpx 40rpx rgba(126, 211, 33, 0.22);
	}

	.label,
	.amount,
	.sub,
	.deposit-label,
	.deposit-amount,
	.deposit-sub,
	.deposit-status {
		display: block;
	}

	.label {
		font-size: 24rpx;
		color: rgba(255, 255, 255, 0.92);
	}

	.amount {
		margin-top: 12rpx;
		font-size: 64rpx;
		font-weight: 800;
		line-height: 1.1;
		color: #ffffff;
		white-space: nowrap;
	}

	.sub {
		margin-top: 10rpx;
		font-size: 22rpx;
		color: rgba(255, 255, 255, 0.88);
	}

	.badge-row {
		display: flex;
		flex-wrap: nowrap;
		gap: 12rpx;
		margin-top: 20rpx;
	}

	.badge {
		padding: 6rpx 16rpx;
		border-radius: 999rpx;
		font-size: 20rpx;
		font-weight: 600;
		color: #4a7a0a;
		background: rgba(255, 255, 255, 0.92);
		white-space: nowrap;
	}

	.action-row {
		display: flex;
		gap: 12rpx;
		margin-top: 28rpx;
	}

	.action-btn {
		flex: 1;
		height: 76rpx;
		line-height: 76rpx;
		padding: 0;
		border-radius: 38rpx;
		font-size: 24rpx;
		font-weight: 700;
		border: none;
	}

	.action-btn.primary {
		color: #5b8f00;
		background: #ffffff;
	}

	.action-btn.secondary {
		color: #ffffff;
		background: rgba(255, 255, 255, 0.18);
		border: 2rpx solid rgba(255, 255, 255, 0.24);
	}

	.deposit-card {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 20rpx;
		margin-top: 24rpx;
		padding: 28rpx 32rpx;
		border-radius: 28rpx;
		background: linear-gradient(135deg, #5aad15, #7ed321);
		box-shadow: 0 8rpx 28rpx rgba(90, 173, 21, 0.18);
	}

	.deposit-main {
		flex: 1;
		min-width: 0;
	}

	.deposit-label {
		font-size: 22rpx;
		color: rgba(255, 255, 255, 0.9);
	}

	.deposit-amount {
		margin-top: 8rpx;
		font-size: 40rpx;
		font-weight: 800;
		color: #ffffff;
	}

	.deposit-sub {
		margin-top: 8rpx;
		font-size: 20rpx;
		color: rgba(255, 255, 255, 0.85);
	}

	.deposit-side {
		display: flex;
		flex-direction: row;
		align-items: center;
		gap: 10rpx;
		flex-shrink: 0;
	}

	.deposit-status {
		display: inline-flex;
		align-items: center;
		justify-content: center;
		height: 44rpx;
		padding: 0 16rpx;
		border-radius: 999rpx;
		font-size: 20rpx;
		font-weight: 600;
		line-height: 1;
		color: #4a7a0a;
		background: rgba(255, 255, 255, 0.92);
		white-space: nowrap;
	}

	.deposit-status.warn {
		color: #b45309;
		background: #fff7ed;
	}

	.deposit-arrow {
		display: flex;
		align-items: center;
		justify-content: center;
		width: 32rpx;
		height: 32rpx;
		flex-shrink: 0;
	}
</style>
