<template>
	<view class="page">
		<view v-if="pageLoading" class="loading-card"><text>加载资金明细...</text></view>
		<template v-else>
			<view class="summary-card">
				<view class="summary-item">
					<text class="summary-label">累计收入</text>
					<text class="summary-value plus">+¥{{ summary.totalIncome.toFixed(2) }}</text>
				</view>
				<view class="summary-item">
					<text class="summary-label">累计支出</text>
					<text class="summary-value minus">-¥{{ summary.totalExpense.toFixed(2) }}</text>
				</view>
			</view>

			<view class="filter-wrap">
				<u-tabs :list="filters" keyName="label" :current="currentTabIndex" :scrollable="true" lineColor="#7ed321" :activeStyle="tabActiveStyle" :inactiveStyle="tabInactiveStyle" @change="handleTabChange"></u-tabs>
			</view>

			<view class="card">
				<view v-if="!currentRecords.length" class="empty-tip">暂无资金明细</view>
				<view v-for="item in currentRecords" :key="item.id" class="record-row">
					<view class="record-icon" :class="item.direction === 1 ? 'plus' : 'minus'"><up-icon :name="item.icon" size="18" :color="item.direction === 1 ? '#7ed321' : '#ff7a45'"></up-icon></view>
					<view class="record-main">
						<text class="record-title">{{ item.title }}</text>
						<text class="record-time">{{ item.time }}</text>
						<text class="record-desc">{{ item.desc }}</text>
					</view>
					<text class="record-amount" :class="item.direction === 1 ? 'plus' : 'minus'">{{ item.direction === 1 ? '+' : '-' }}¥{{ Number(item.amount).toFixed(2) }}</text>
				</view>
			</view>
		</template>
	</view>
</template>

<script setup>
	import { computed, ref } from 'vue';
	import { onShow } from '@dcloudio/uni-app';
	import { ensureMerchantApproved } from '@/common/api/merchant';
	import { fetchMerchantWalletTransactions } from '@/common/api/merchantWallet';
	import { ensureLogin } from '@/common/auth';

	const filters = [
		{ key: 'all', label: '全部' },
		{ key: 'plus', label: '收入' },
		{ key: 'minus', label: '支出' }
	];

	const activeFilter = ref('all');
	const summary = ref({ totalIncome: 0, totalExpense: 0, records: [] });
	const pageLoading = ref(false);
	const tabActiveStyle = { color: '#59b50e', fontWeight: '700' };
	const tabInactiveStyle = { color: '#6b7280' };
	const currentTabIndex = computed(() => Math.max(filters.findIndex((item) => item.key === activeFilter.value), 0));

	const syncRecords = async () => {
		pageLoading.value = true;
		try {
			const pageVo = await fetchMerchantWalletTransactions({ direction: activeFilter.value });
			summary.value = {
				totalIncome: pageVo.totalIncome || 0,
				totalExpense: pageVo.totalExpense || 0,
				records: pageVo.records || []
			};
		} catch {
			summary.value = { totalIncome: 0, totalExpense: 0, records: [] };
		} finally {
			pageLoading.value = false;
		}
	};

	onShow(async () => {
		if (!ensureLogin('/packageMerchant/wallet-record')) return;
		try {
			await ensureMerchantApproved({ redirect: true, toast: true });
			await syncRecords();
		} catch {
			// ignore
		}
	});

	const handleTabChange = (item) => {
		activeFilter.value = item.key;
		syncRecords();
	};

	const currentRecords = computed(() => summary.value.records || []);
</script>

<style scoped>
	.page { box-sizing: border-box; min-height: 100vh; padding: 24rpx; background: #f6f8f3; overflow-x: hidden; }
	.loading-card, .empty-tip { padding: 48rpx 32rpx; text-align: center; color: #9aa2b1; font-size: 24rpx; }
	.summary-card, .card { box-sizing: border-box; width: 100%; padding: 28rpx; border-radius: 28rpx; background: #ffffff; overflow: hidden; }
	.summary-card { display: flex; justify-content: space-between; flex-wrap: wrap; gap: 16rpx; }
	.summary-item { box-sizing: border-box; width: calc((100% - 16rpx) / 2); min-width: 0; padding: 24rpx; border-radius: 22rpx; background: #f8fafc; }
	.summary-label, .summary-value, .record-title, .record-time, .record-desc, .record-amount { display: block; }
	.summary-label { font-size: 22rpx; color: #9aa2b1; }
	.summary-value { margin-top: 12rpx; font-size: 34rpx; font-weight: 700; }
	.summary-value.plus, .record-amount.plus { color: #7ed321; }
	.summary-value.minus, .record-amount.minus { color: #ff7a45; }
	.filter-wrap { margin: 24rpx 0; }
	.filter-wrap :deep(.u-tabs__wrapper__nav__item) { padding: 0 28rpx; }
	.filter-wrap :deep(.u-tabs__wrapper__nav__item__text) { font-size: 24rpx; white-space: nowrap; }
	.card { margin-top: 0; }
	.record-row { display: flex; align-items: flex-start; }
	.record-row + .record-row { margin-top: 24rpx; padding-top: 24rpx; border-top: 1rpx solid #eef2f7; }
	.record-icon { display: flex; align-items: center; justify-content: center; width: 60rpx; height: 60rpx; border-radius: 50%; }
	.record-icon.plus { color: #7ed321; background: #eefadb; }
	.record-icon.minus { color: #ff7a45; background: #fff0e6; }
	.record-main { flex: 1; min-width: 0; margin-left: 16rpx; }
	.record-title { font-size: 26rpx; font-weight: 700; color: #1f2937; }
	.record-time { margin-top: 6rpx; font-size: 20rpx; color: #9aa2b1; }
	.record-desc { margin-top: 8rpx; font-size: 22rpx; color: #6b7280; }
	.record-amount { margin-left: 16rpx; flex-shrink: 0; text-align: right; font-size: 26rpx; font-weight: 700; }
</style>
