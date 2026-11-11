<script setup>
	import { computed, ref } from 'vue';
	import { onShow } from '@dcloudio/uni-app';
	import { ensureLogin } from '@/common/auth';
	import { ensureMerchantOperational, fetchDashboardRevenue } from '@/common/api/merchant';
	import { formatMoney } from '@/common/merchant/constants';

	const initialLoading = ref(true);
	const periodLoading = ref(false);
	const pageReady = ref(false);
	const skipNextShow = ref(false);
	const now = new Date();
	const selectedYear = ref(now.getFullYear());
	const selectedMonth = ref(now.getMonth() + 1);
	const revenue = ref({
		amount: 0,
		orderCount: 0,
		todayAmount: 0,
		monthAmount: 0,
		totalAmount: 0
	});

	const monthPickerValue = computed(() => {
		const month = String(selectedMonth.value).padStart(2, '0');
		return `${selectedYear.value}-${month}`;
	});

	const pickerStart = `${now.getFullYear() - 4}-01`;
	const pickerEnd = (() => {
		const month = String(now.getMonth() + 1).padStart(2, '0');
		return `${now.getFullYear()}-${month}`;
	})();

	const periodLabel = computed(() => `${selectedYear.value}年${selectedMonth.value}月`);

	const isCurrentMonth = computed(
		() => selectedYear.value === now.getFullYear() && selectedMonth.value === now.getMonth() + 1
	);

	const avgOrderAmount = computed(() => {
		const count = Number(revenue.value.orderCount || 0);
		if (count <= 0) return 0;
		return Number(revenue.value.amount || 0) / count;
	});

	const formatAmount = (value) => `¥${formatMoney(value)}`;

	const fetchRevenueData = async () => {
		revenue.value = await fetchDashboardRevenue({
			year: selectedYear.value,
			month: selectedMonth.value
		});
	};

	const loadPage = async ({ silent = false } = {}) => {
		if (!ensureLogin('/packageMerchant/revenue')) return;
		if (!silent && !pageReady.value) {
			initialLoading.value = true;
		}
		try {
			await ensureMerchantOperational({ redirect: true, toast: true });
			await fetchRevenueData();
			pageReady.value = true;
		} catch (error) {
			uni.showToast({ title: error?.msg || error?.message || '加载失败', icon: 'none' });
		} finally {
			initialLoading.value = false;
		}
	};

	const loadPeriodRevenue = async () => {
		if (!pageReady.value) return;
		periodLoading.value = true;
		try {
			await fetchRevenueData();
		} catch (error) {
			uni.showToast({ title: error?.msg || error?.message || '加载失败', icon: 'none' });
		} finally {
			periodLoading.value = false;
		}
	};

	const onMonthPickerChange = (event) => {
		const value = String(event.detail.value || '');
		const matched = value.match(/^(\d{4})-(\d{1,2})/);
		if (!matched) return;
		const nextYear = Number(matched[1]);
		const nextMonth = Number(matched[2]);
		if (nextYear === selectedYear.value && nextMonth === selectedMonth.value) return;
		selectedYear.value = nextYear;
		selectedMonth.value = nextMonth;
		// 系统 picker 关闭后会触发 onShow，避免整页重复加载
		skipNextShow.value = true;
		loadPeriodRevenue();
	};

	onShow(() => {
		if (skipNextShow.value) {
			skipNextShow.value = false;
			return;
		}
		loadPage({ silent: pageReady.value });
	});
</script>

<template>
	<view class="page">
		<view v-if="initialLoading && !pageReady" class="loading-wrap">
			<text class="loading-text">加载中...</text>
		</view>

		<template v-else>
			<!-- 一眼看清：今日 / 本月 / 累计 -->
			<view class="overview-card">
				<view class="overview-head">
					<text class="overview-title">营业概览</text>
					<text class="overview-date">{{ now.getFullYear() }}年{{ now.getMonth() + 1 }}月{{ now.getDate() }}日</text>
				</view>
				<view class="overview-grid">
					<view class="overview-item highlight">
						<text class="overview-value">{{ formatAmount(revenue.todayAmount) }}</text>
						<text class="overview-label">今日</text>
					</view>
					<view class="overview-item">
						<text class="overview-value">{{ formatAmount(revenue.monthAmount) }}</text>
						<text class="overview-label">本月</text>
					</view>
					<view class="overview-item">
						<text class="overview-value">{{ formatAmount(revenue.totalAmount) }}</text>
						<text class="overview-label">累计</text>
					</view>
				</view>
			</view>

			<!-- 按月明细 -->
			<view class="period-card" :class="{ 'period-card--loading': periodLoading }">
				<view class="period-head">
					<view class="period-title-wrap">
						<view class="period-mark"></view>
						<text class="period-title">按月明细</text>
					</view>
					<picker
						mode="date"
						fields="month"
						:start="pickerStart"
						:end="pickerEnd"
						:value="monthPickerValue"
						@change="onMonthPickerChange"
					>
						<view class="period-picker">
							<text class="period-picker-text">{{ periodLabel }}</text>
							<up-icon name="arrow-down" size="12" color="#47952d"></up-icon>
						</view>
					</picker>
				</view>

				<view class="period-hero">
					<text class="period-amount">{{ formatAmount(revenue.amount) }}</text>
					<view v-if="isCurrentMonth" class="period-badge">当前月</view>
				</view>

				<view class="period-stats">
					<view class="period-stat">
						<text class="period-stat-value">{{ revenue.orderCount }}</text>
						<text class="period-stat-label">成交单</text>
					</view>
					<view class="period-stat-divider"></view>
					<view class="period-stat">
						<text class="period-stat-value">{{ formatAmount(avgOrderAmount) }}</text>
						<text class="period-stat-label">客单价</text>
					</view>
				</view>
			</view>
		</template>
	</view>
</template>

<style scoped>
	.page {
		box-sizing: border-box;
		min-height: 100vh;
		padding: 24rpx;
		background: #f3f6f1;
	}

	.loading-wrap {
		display: flex;
		align-items: center;
		justify-content: center;
		min-height: 60vh;
	}

	.loading-text {
		font-size: 28rpx;
		color: #8a9a7a;
	}

	.overview-card {
		padding: 28rpx 24rpx 32rpx;
		border-radius: 24rpx;
		background: linear-gradient(145deg, #337547 0%, #4e963d 100%);
		box-shadow: 0 10rpx 28rpx rgba(42, 98, 55, 0.18);
	}

	.overview-head {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-bottom: 28rpx;
	}

	.overview-title {
		font-size: 30rpx;
		font-weight: 700;
		color: #ffffff;
	}

	.overview-date {
		font-size: 22rpx;
		color: rgba(255, 255, 255, 0.78);
	}

	.overview-grid {
		display: flex;
		gap: 0;
	}

	.overview-item {
		flex: 1;
		min-width: 0;
		text-align: center;
		padding: 8rpx 4rpx;
	}

	.overview-item + .overview-item {
		border-left: 1rpx solid rgba(255, 255, 255, 0.22);
	}

	.overview-item.highlight .overview-value {
		color: #ffffff;
		font-size: 36rpx;
	}

	.overview-value {
		display: block;
		font-size: 32rpx;
		font-weight: 800;
		color: rgba(255, 255, 255, 0.95);
		line-height: 1.2;
		word-break: break-all;
	}

	.overview-label {
		display: block;
		margin-top: 10rpx;
		font-size: 22rpx;
		color: rgba(255, 255, 255, 0.72);
	}

	.period-card {
		margin-top: 20rpx;
		padding: 28rpx 24rpx 32rpx;
		border-radius: 24rpx;
		background: #ffffff;
		box-shadow: 0 5rpx 20rpx rgba(31, 53, 35, 0.045);
		transition: opacity 0.2s ease;
	}

	.period-card--loading {
		opacity: 0.72;
	}

	.period-head {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-bottom: 24rpx;
	}

	.period-title-wrap {
		display: flex;
		align-items: center;
		gap: 10rpx;
	}

	.period-mark {
		width: 5rpx;
		height: 24rpx;
		border-radius: 3rpx;
		background: #55a833;
	}

	.period-title {
		font-size: 27rpx;
		font-weight: 600;
		color: #1a2114;
	}

	.period-picker {
		display: flex;
		align-items: center;
		gap: 6rpx;
		padding: 10rpx 18rpx;
		border-radius: 999rpx;
		background: #f1f8df;
	}

	.period-picker-text {
		font-size: 24rpx;
		font-weight: 600;
		color: #47952d;
	}

	.period-hero {
		display: flex;
		align-items: center;
		justify-content: center;
		gap: 12rpx;
		padding: 12rpx 0 8rpx;
	}

	.period-amount {
		font-size: 56rpx;
		font-weight: 800;
		color: #59b50e;
		line-height: 1.15;
		letter-spacing: -1rpx;
	}

	.period-badge {
		padding: 4rpx 14rpx;
		border-radius: 999rpx;
		font-size: 20rpx;
		font-weight: 500;
		color: #47952d;
		background: #f1f8df;
	}

	.period-stats {
		display: flex;
		align-items: center;
		margin-top: 20rpx;
		padding-top: 24rpx;
		border-top: 1rpx solid #edf1e9;
	}

	.period-stat {
		flex: 1;
		text-align: center;
	}

	.period-stat-divider {
		width: 1rpx;
		height: 56rpx;
		background: #edf1e9;
		flex-shrink: 0;
	}

	.period-stat-value {
		display: block;
		font-size: 34rpx;
		font-weight: 800;
		color: #1a2114;
		line-height: 1.2;
	}

	.period-stat-label {
		display: block;
		margin-top: 8rpx;
		font-size: 22rpx;
		color: #8a9a7a;
	}
</style>
