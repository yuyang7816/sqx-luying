<template>
	<view class="page">
		<view v-if="pageLoading" class="loading-card"><text>加载钱包信息...</text></view>
		<template v-else>
			<view class="balance-card">
				<text class="label">账户余额</text>
				<text class="amount">¥{{ formatAmount(summary.balance) }}</text>
				<text class="sub">冻结金额 ¥{{ formatAmount(summary.frozenAmount) }}</text>
				<view class="badge-row">
					<text class="badge">可提现</text>
					<text class="badge">手续费 {{ feeRateText }}</text>
				</view>
				<view class="action-row">
					<button class="action-btn primary" @click="goRecharge">充值</button>
					<button class="action-btn secondary" @click="goWithdraw">提现</button>
					<button class="action-btn secondary" @click="goWalletRecords">明细</button>
				</view>
			</view>

			<view class="summary-panel">
				<view class="summary-item">
					<text class="summary-label">累计收入</text>
					<text class="summary-value plus">+¥{{ formatAmount(summary.totalIncome) }}</text>
				</view>
				<view class="summary-divider" />
				<view class="summary-item">
					<text class="summary-label">最近提现</text>
					<view v-if="latestWithdraw" class="withdraw-brief">
						<text class="withdraw-status" :class="latestWithdraw.status">{{ latestWithdraw.statusText }}</text>
						<text class="withdraw-amount">¥{{ formatAmount(latestWithdraw.amount) }}</text>
					</view>
					<text v-else class="summary-empty">暂无</text>
				</view>
			</view>

			<view class="link-card" @click="goWithdrawRecords">
				<view class="link-main">
					<text class="link-title">提现明细</text>
					<text class="link-desc">查看提现申请与审核进度</text>
				</view>
				<up-icon name="arrow-right" color="#b8c4b0" size="16" />
			</view>

			<view class="panel">
				<view class="panel-head">
					<text class="panel-title">最近资金明细</text>
					<text v-if="recentRecords.length" class="panel-more" @click="goWalletRecords">全部</text>
				</view>
				<view v-if="!recentRecords.length" class="empty-tip">暂无资金明细</view>
				<view v-else class="record-list">
					<view
						v-for="item in recentRecords"
						:key="item.id"
						class="record-row"
						@click="goWalletRecords"
					>
						<view class="record-main">
							<text class="record-title">{{ item.title }}</text>
							<text class="record-time">{{ item.time }}</text>
						</view>
						<text class="record-amount" :class="item.direction === 1 ? 'plus' : 'minus'">
							{{ item.direction === 1 ? '+' : '-' }}¥{{ formatAmount(item.amount) }}
						</text>
					</view>
				</view>
			</view>
		</template>
	</view>
</template>

<script setup>
	import { computed, ref } from 'vue';
	import { onShow } from '@dcloudio/uni-app';
	import { fetchWalletDetail, formatWithdrawFeeRate } from '@/common/api/wallet';
	import { ensureLogin } from '@/common/auth';

	const emptySummary = () => ({
		balance: 0,
		frozenAmount: 0,
		totalIncome: 0,
		totalExpense: 0,
		records: [],
		withdrawRecords: [],
		withdrawConfig: { serviceFeeRate: 0 }
	});

	const summary = ref(emptySummary());
	const pageLoading = ref(false);

	const formatAmount = (value) => Number(value || 0).toFixed(2);

	const syncWallet = async () => {
		pageLoading.value = true;
		try {
			summary.value = await fetchWalletDetail();
		} catch (error) {
			summary.value = emptySummary();
		} finally {
			pageLoading.value = false;
		}
	};

	onShow(() => {
		if (!ensureLogin('/packageUser/wallet')) return;
		syncWallet();
	});

	const feeRateText = computed(() => formatWithdrawFeeRate(summary.value.withdrawConfig?.serviceFeeRate));
	const recentRecords = computed(() => (summary.value.records || []).slice(0, 4));
	const latestWithdraw = computed(() => summary.value.withdrawRecords?.[0] || null);

	const goRecharge = () => uni.navigateTo({ url: '/packageUser/wallet-recharge' });
	const goWithdraw = () => uni.navigateTo({ url: '/packageUser/wallet-withdraw' });
	const goWalletRecords = () => uni.navigateTo({ url: '/packageUser/wallet-record' });
	const goWithdrawRecords = () => uni.navigateTo({ url: '/packageUser/wallet-withdraw-records' });
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

	.label {
		display: block;
		font-size: 24rpx;
		color: rgba(255, 255, 255, 0.92);
	}

	.amount {
		display: block;
		margin-top: 12rpx;
		font-size: 64rpx;
		font-weight: 800;
		line-height: 1.1;
		color: #ffffff;
		white-space: nowrap;
	}

	.sub {
		display: block;
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

	.summary-panel {
		display: flex;
		align-items: stretch;
		margin-top: 24rpx;
		padding: 24rpx 16rpx;
		border-radius: 28rpx;
		background: #ffffff;
		box-shadow: 0 6rpx 24rpx rgba(0, 0, 0, 0.04);
	}

	.summary-item {
		flex: 1;
		min-width: 0;
		padding: 0 8rpx;
		text-align: center;
	}

	.summary-divider {
		flex-shrink: 0;
		width: 1rpx;
		margin: 8rpx 0;
		background: #eef2f6;
	}

	.summary-label {
		display: block;
		font-size: 22rpx;
		color: #9aa2b1;
		white-space: nowrap;
	}

	.summary-value,
	.summary-empty,
	.withdraw-amount {
		display: block;
		margin-top: 10rpx;
		font-size: 30rpx;
		font-weight: 700;
		line-height: 1.2;
		white-space: nowrap;
	}

	.summary-value.plus,
	.record-amount.plus {
		color: #6bc01a;
	}

	.record-amount.minus {
		color: #ff7a45;
	}

	.summary-empty {
		color: #c0c8d4;
	}

	.withdraw-brief {
		margin-top: 10rpx;
	}

	.withdraw-status {
		display: block;
		font-size: 22rpx;
		font-weight: 600;
		line-height: 1.3;
		white-space: nowrap;
	}

	.withdraw-status.pending {
		color: #c07b00;
	}

	.withdraw-status.success {
		color: #6bc01a;
	}

	.withdraw-status.reject {
		color: #d63b3b;
	}

	.withdraw-amount {
		margin-top: 4rpx;
		color: #1f2937;
	}

	.link-card {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 16rpx;
		margin-top: 24rpx;
		padding: 24rpx 28rpx;
		border-radius: 28rpx;
		background: #ffffff;
		box-shadow: 0 6rpx 24rpx rgba(0, 0, 0, 0.04);
	}

	.link-main {
		flex: 1;
		min-width: 0;
	}

	.link-title {
		display: block;
		font-size: 28rpx;
		font-weight: 700;
		color: #1f2937;
	}

	.link-desc {
		display: block;
		margin-top: 6rpx;
		font-size: 22rpx;
		line-height: 1.4;
		color: #8a94a6;
	}

	.panel {
		margin-top: 24rpx;
		padding: 28rpx 24rpx;
		border-radius: 28rpx;
		background: #ffffff;
		box-shadow: 0 6rpx 24rpx rgba(0, 0, 0, 0.04);
	}

	.panel-head {
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	.panel-title {
		font-size: 30rpx;
		font-weight: 800;
		color: #1a2114;
	}

	.panel-more {
		font-size: 24rpx;
		color: #6bc01a;
	}

	.empty-tip {
		margin-top: 24rpx;
		padding: 32rpx 0;
		font-size: 24rpx;
		color: #9aa2b1;
		text-align: center;
	}

	.record-list {
		margin-top: 8rpx;
	}

	.record-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 20rpx;
		padding: 22rpx 0;
		border-bottom: 1rpx solid #f0f3f7;
	}

	.record-row:last-child {
		border-bottom: none;
		padding-bottom: 0;
	}

	.record-main {
		flex: 1;
		min-width: 0;
	}

	.record-title {
		display: block;
		font-size: 26rpx;
		font-weight: 600;
		color: #1f2937;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.record-time {
		display: block;
		margin-top: 6rpx;
		font-size: 20rpx;
		color: #9aa2b1;
		white-space: nowrap;
	}

	.record-amount {
		flex-shrink: 0;
		font-size: 26rpx;
		font-weight: 700;
		white-space: nowrap;
	}
</style>
