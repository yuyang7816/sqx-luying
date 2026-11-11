<template>
	<view class="page">
		<view class="hero-card">
			<view class="hero-badge">收益概览</view>
			<text class="hero-desc">清晰区分团队贡献结构，便于快速查看返佣来源</text>

			<view class="hero-metrics">
				<view class="metric-item">
					<text class="metric-num">{{ formatMoney(overview.totalCommission) }}</text>
					<text class="metric-label">总收益</text>
				</view>
				<view class="metric-divider"></view>
				<view class="metric-item">
					<text class="metric-num">{{ formatMoney(overview.level1Commission) }}</text>
					<text class="metric-label">一级收益</text>
				</view>
				<view class="metric-divider"></view>
				<view class="metric-item">
					<text class="metric-num">{{ formatMoney(overview.level2Commission) }}</text>
					<text class="metric-label">二级收益</text>
				</view>
			</view>
		</view>

		<view class="level-tabs">
			<view class="level-tab" :class="{ active: activeLevel === 1 }" @click="switchLevel(1)">
				一级 ( {{ level1Count }} )
			</view>
			<view class="level-tab" :class="{ active: activeLevel === 2 }" @click="switchLevel(2)">
				二级 ( {{ level2Count }} )
			</view>
		</view>

		<view class="list-card">
			<view class="list-head">
				<text class="list-title">收益明细</text>
				<text class="list-desc">按邀请层级查看单笔奖励记录</text>
			</view>

			<view class="table-head">
				<text class="col-index">编号</text>
				<text class="col-avatar">头像</text>
				<text class="col-name">昵称</text>
				<text class="col-reward">奖励</text>
			</view>

			<view v-if="!filteredRecords.length" class="empty-box">
				<up-icon name="file-text" size="40" color="#b0b8a8"></up-icon>
				<text class="empty-text">暂无数据</text>
			</view>

			<view v-for="(item, index) in filteredRecords" :key="item.id" class="table-row">
				<text class="col-index">{{ index + 1 }}</text>
				<view class="col-avatar">
					<image class="avatar-image" :src="item.avatar" mode="aspectFill"></image>
				</view>
				<view class="col-name-wrap">
					<text class="col-name">{{ item.name }}</text>
					<text class="col-time">{{ item.time }}</text>
				</view>
				<text class="col-reward">¥{{ formatMoney(item.commissionAmount) }}</text>
			</view>
		</view>
	</view>
</template>

<script setup>
	import { computed, ref } from 'vue';
	import { onShow } from '@dcloudio/uni-app';
	import { ensureLogin } from '@/common/auth';
	import { fetchCommissionRecords, fetchTeamOverview } from '@/common/api/invite';

	const activeLevel = ref(1);
	const overview = ref({
		totalCommission: 0,
		level1Commission: 0,
		level2Commission: 0
	});
	const allRecords = ref([]);

	const level1Count = computed(() => allRecords.value.filter((item) => item.level === 1).length);
	const level2Count = computed(() => allRecords.value.filter((item) => item.level === 2).length);
	const filteredRecords = computed(() => allRecords.value.filter((item) => item.level === activeLevel.value));

	const formatMoney = (value) => Number(value || 0).toFixed(2);

	const loadData = async () => {
		if (!ensureLogin('/packageUser/invite-commission')) return;
		try {
			const [teamOverview, rows] = await Promise.all([
				fetchTeamOverview(),
				fetchCommissionRecords()
			]);
			overview.value = teamOverview;
			allRecords.value = rows || [];
		} catch (error) {
			uni.showToast({ title: error.message || '加载失败', icon: 'none' });
		}
	};

	const switchLevel = (level) => {
		activeLevel.value = level;
	};

	onShow(loadData);
</script>

<style scoped>
	.page {
		box-sizing: border-box;
		min-height: 100vh;
		padding: 24rpx;
		background: #f6f8f3;
		overflow-x: hidden;
	}

	.hero-card,
	.list-card {
		box-sizing: border-box;
		width: 100%;
		border-radius: 28rpx;
		overflow: hidden;
	}

	.hero-card {
		padding: 32rpx 28rpx;
		background: linear-gradient(160deg, #7ed321 0%, #6bc01a 50%, #5aad15 100%);
		box-shadow: 0 12rpx 36rpx rgba(126, 211, 33, 0.22);
	}

	.hero-badge {
		display: inline-block;
		padding: 8rpx 18rpx;
		border-radius: 999rpx;
		font-size: 22rpx;
		font-weight: 600;
		color: #5aad15;
		background: rgba(255, 255, 255, 0.92);
	}

	.hero-desc,
	.metric-label,
	.metric-num,
	.list-title,
	.list-desc,
	.col-index,
	.col-name,
	.col-time,
	.col-reward,
	.empty-text {
		display: block;
	}

	.hero-desc {
		margin-top: 18rpx;
		font-size: 24rpx;
		line-height: 1.7;
		color: rgba(255, 255, 255, 0.95);
	}

	.hero-metrics {
		display: flex;
		align-items: center;
		margin-top: 28rpx;
		padding: 28rpx 16rpx;
		border-radius: 24rpx;
		background: rgba(255, 255, 255, 0.94);
	}

	.metric-item {
		flex: 1;
		min-width: 0;
		text-align: center;
	}

	.metric-divider {
		width: 1rpx;
		height: 72rpx;
		background: #eef2e8;
		flex-shrink: 0;
	}

	.metric-num {
		font-size: 40rpx;
		font-weight: 800;
		color: #59b50e;
		line-height: 1.1;
	}

	.metric-label {
		margin-top: 10rpx;
		font-size: 22rpx;
		color: #8a9a7a;
	}

	.level-tabs {
		display: flex;
		margin-top: 24rpx;
		padding: 8rpx;
		border-radius: 999rpx;
		background: #ffffff;
		box-shadow: 0 6rpx 20rpx rgba(0, 0, 0, 0.04);
	}

	.level-tab {
		flex: 1;
		height: 72rpx;
		line-height: 72rpx;
		border-radius: 999rpx;
		text-align: center;
		font-size: 26rpx;
		font-weight: 600;
		color: #8a9a7a;
	}

	.level-tab.active {
		color: #ffffff;
		background: linear-gradient(135deg, #7ed321, #6bc01a);
	}

	.list-card {
		margin-top: 24rpx;
		padding: 28rpx 24rpx 12rpx;
		background: #ffffff;
		box-shadow: 0 8rpx 28rpx rgba(0, 0, 0, 0.04);
	}

	.list-head {
		margin-bottom: 24rpx;
	}

	.list-title {
		font-size: 32rpx;
		font-weight: 800;
		color: #1a2114;
	}

	.list-desc {
		margin-top: 8rpx;
		font-size: 22rpx;
		color: #8a9a7a;
	}

	.table-head,
	.table-row {
		display: flex;
		align-items: center;
	}

	.table-head {
		padding: 18rpx 0;
		border-radius: 16rpx;
		background: #f9fbf6;
	}

	.table-row {
		padding: 22rpx 0;
		border-bottom: 1rpx solid #f0f4ea;
	}

	.table-row:last-child {
		border-bottom: none;
	}

	.table-head .col-index,
	.table-head .col-avatar,
	.table-head .col-name {
		font-size: 24rpx;
		font-weight: 400;
		color: #8a9a7a;
	}

	.table-row .col-index,
	.table-row .col-name {
		font-size: 24rpx;
		font-weight: 400;
		color: #8a9a7a;
	}

	.col-index {
		width: 72rpx;
		text-align: center;
		flex-shrink: 0;
	}

	.table-head .col-avatar {
		width: 88rpx;
		text-align: center;
	}

	.col-avatar {
		width: 88rpx;
		display: flex;
		justify-content: center;
		flex-shrink: 0;
	}

	.col-name-wrap {
		flex: 1;
		min-width: 0;
		padding: 0 12rpx;
	}

	.col-name {
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.col-time {
		margin-top: 6rpx;
		font-size: 20rpx;
		color: #8a9a7a;
	}

	.col-reward {
		width: 140rpx;
		text-align: right;
		padding-right: 8rpx;
		font-size: 26rpx;
		font-weight: 700;
		color: #59b50e;
		flex-shrink: 0;
	}

	.avatar-image,
	.avatar-fallback {
		width: 64rpx;
		height: 64rpx;
		border-radius: 50%;
		overflow: hidden;
	}

	.avatar-fallback {
		line-height: 64rpx;
		text-align: center;
		font-size: 24rpx;
		font-weight: 400;
		color: #8a9a7a;
		background: #f9fbf6;
	}

	.empty-box {
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		padding: 80rpx 0 48rpx;
	}

	.empty-text {
		margin-top: 16rpx;
		font-size: 24rpx;
		color: #b0b8a8;
	}
</style>
