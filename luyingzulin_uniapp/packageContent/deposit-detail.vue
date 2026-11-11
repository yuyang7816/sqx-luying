<template>
	<view class="page">
		<view v-if="pageLoading && !records.length" class="loading-card"><text>加载押金明细...</text></view>
		<template v-else>
			<view class="list-body">
				<view class="card">
					<view v-if="!records.length" class="empty-tip">暂无押金明细</view>
					<view v-for="item in records" :key="item.id || item.logNo" class="record-row">
						<view class="record-icon" :class="item.type">
							<up-icon :name="item.icon" size="18" :color="item.type === 'plus' ? '#7ed321' : '#ff8a8a'"></up-icon>
						</view>
						<view class="record-main">
							<text class="record-title">{{ item.title }}</text>
							<text class="record-time">{{ item.time }}</text>
							<text v-if="item.remark" class="record-desc">{{ item.remark }}</text>
						</view>
						<text class="record-amount" :class="item.type">{{ item.amount }}</text>
					</view>
					<view v-if="records.length && !hasMore" class="footer-tip">没有更多了</view>
					<view v-if="loadingMore" class="footer-tip">加载中...</view>
				</view>
			</view>
		</template>
	</view>
</template>

<script setup>
	import { ref } from 'vue';
	import { onShow, onReachBottom } from '@dcloudio/uni-app';
	import { fetchDepositLogPage } from '@/common/api/deposit';
	import { ensureLogin } from '@/common/auth';

	const records = ref([]);
	const pageLoading = ref(false);
	const loadingMore = ref(false);
	const page = ref(1);
	const hasMore = ref(true);
	const limit = 20;

	const syncRecords = async ({ reset = false } = {}) => {
		if (reset) {
			page.value = 1;
			hasMore.value = true;
			records.value = [];
		}
		if (!hasMore.value && !reset) return;
		const isFirstPage = page.value === 1;
		if (isFirstPage) {
			pageLoading.value = true;
		} else {
			loadingMore.value = true;
		}
		try {
			const logPage = await fetchDepositLogPage({ page: page.value, limit });
			const list = logPage.records || [];
			records.value = reset ? list : records.value.concat(list);
			const total = Number(logPage.total || 0);
			hasMore.value = total ? records.value.length < total : list.length >= limit;
			if (hasMore.value) {
				page.value += 1;
			}
		} catch (error) {
			if (reset) {
				records.value = [];
			}
			hasMore.value = false;
		} finally {
			pageLoading.value = false;
			loadingMore.value = false;
		}
	};

	onShow(() => {
		if (!ensureLogin('/packageContent/deposit-detail')) return;
		syncRecords({ reset: true });
	});

	const loadMore = () => {
		if (pageLoading.value || loadingMore.value || !hasMore.value) return;
		syncRecords();
	};

	onReachBottom(loadMore);
</script>

<style scoped>
	.page {
		box-sizing: border-box;
		min-height: 100vh;
		background: #f6f8f3;
		overflow-x: hidden;
	}

	.list-body {
		padding: 28rpx;
		box-sizing: border-box;
	}

	.loading-card,
	.empty-tip,
	.footer-tip {
		padding: 48rpx 32rpx;
		text-align: center;
		color: #9aa2b1;
		font-size: 24rpx;
	}

	.card {
		box-sizing: border-box;
		width: 100%;
		padding: 32rpx;
		background: #ffffff;
		border-radius: 32rpx;
		box-shadow: 0 6rpx 24rpx rgba(0, 0, 0, 0.04);
		overflow: hidden;
	}

	.record-row {
		display: flex;
		align-items: center;
		margin-top: 24rpx;
		padding: 24rpx;
		background: #f9fbf6;
		border-radius: 24rpx;
	}

	.record-row:first-child {
		margin-top: 0;
	}

	.record-icon {
		display: flex;
		align-items: center;
		justify-content: center;
		width: 64rpx;
		height: 64rpx;
		border-radius: 50%;
		flex-shrink: 0;
	}

	.record-icon.plus {
		background: linear-gradient(135deg, #eefadb, #f6ffea);
	}

	.record-icon.minus {
		background: linear-gradient(135deg, #ffecec, #fff5f5);
	}

	.record-main {
		flex: 1;
		min-width: 0;
		margin-left: 20rpx;
	}

	.record-title,
	.record-time,
	.record-desc,
	.record-amount {
		display: block;
	}

	.record-title {
		font-size: 28rpx;
		font-weight: 700;
		color: #1a2114;
	}

	.record-time {
		margin-top: 8rpx;
		font-size: 22rpx;
		color: #8a9a7a;
	}

	.record-desc {
		margin-top: 8rpx;
		font-size: 22rpx;
		color: #6b7280;
	}

	.record-amount {
		flex-shrink: 0;
		text-align: right;
		font-size: 28rpx;
		font-weight: 800;
	}

	.record-amount.plus {
		color: #7ed321;
	}

	.record-amount.minus {
		color: #ff7a45;
	}
</style>
