<template>
	<view class="page">
		<view class="summary-card">
			<view class="summary-item"><text class="summary-num">{{ stats.totalInvite }}</text><text class="summary-label">累计邀请</text></view>
			<view class="summary-item"><text class="summary-num">{{ stats.convertedCount }}</text><text class="summary-label">已成交</text></view>
			<view class="summary-item"><text class="summary-num">{{ stats.totalReward }}</text><text class="summary-label">累计奖励</text></view>
		</view>

		<view v-if="!records.length" class="empty-card">
			<up-icon name="gift" size="34" color="#a7b19f"></up-icon>
			<text class="empty-title">还没有邀请记录</text>
			<text class="empty-desc">分享海报或小程序给好友后，好友注册成功会在这里生成下级记录。</text>
		</view>

		<view v-else class="card">
			<view v-for="item in records" :key="item.id" class="record-row">
				<image class="avatar" :src="item.avatar" mode="aspectFill"></image>
				<view class="record-main">
					<text class="record-name">{{ item.name }}</text>
					<text class="record-time">{{ item.time }}</text>
					<text class="record-desc">{{ item.desc }}</text>
					<text class="record-desc">手机号：{{ item.inviteeMobile }}</text>
				</view>
				<text class="record-status" :class="item.statusType">{{ item.status }}</text>
			</view>
		</view>
	</view>
</template>

<script setup>
	import { ref } from 'vue';
	import { onShow } from '@dcloudio/uni-app';
	import { ensureLogin } from '@/common/auth';
	import { fetchInviteRecords, fetchInviteStats } from '@/common/api/invite';
	import { DEFAULT_AVATAR_URL } from '@/common/constants/avatar';

	const records = ref([]);
	const stats = ref({ totalInvite: 0, convertedCount: 0, totalReward: 0 });

	onShow(async () => {
		if (!ensureLogin('/packageUser/invite-record')) return;
		try {
			const [statData, rows] = await Promise.all([
				fetchInviteStats(),
				fetchInviteRecords()
			]);
			stats.value = statData;
			records.value = (rows || []).map((item) => ({
				...item,
				avatar: item.avatar || DEFAULT_AVATAR_URL
			}));
		} catch (error) {
			uni.showToast({ title: error.message || '加载失败', icon: 'none' });
		}
	});
</script>

<style scoped>
	.page { box-sizing: border-box; min-height: 100vh; padding: 24rpx; background: #f5f7ff; overflow-x: hidden; }
	.summary-card, .card, .empty-card { box-sizing: border-box; width: 100%; padding: 28rpx; border-radius: 28rpx; background: #ffffff; overflow: hidden; }
	.summary-card { display: flex; justify-content: space-between; gap: 16rpx; }
	.summary-item { flex: 1; min-width: 0; padding: 22rpx 0; border-radius: 20rpx; text-align: center; background: #f7fbf2; }
	.summary-num, .summary-label, .empty-title, .empty-desc, .record-name, .record-time, .record-desc, .record-status { display: block; }
	.summary-num { font-size: 34rpx; font-weight: 700; color: #1f2937; }
	.summary-label { margin-top: 8rpx; font-size: 22rpx; color: #8a94a6; }
	.empty-card { display: flex; flex-direction: column; align-items: center; justify-content: center; margin-top: 24rpx; padding: 100rpx 40rpx; }
	.empty-title { margin-top: 24rpx; font-size: 30rpx; font-weight: 700; color: #1a2114; }
	.empty-desc { margin-top: 14rpx; font-size: 24rpx; line-height: 1.7; text-align: center; color: #8a9a7a; }
	.card { margin-top: 24rpx; }
	.record-row { display: flex; align-items: center; }
	.record-row + .record-row { margin-top: 24rpx; padding-top: 24rpx; border-top: 1rpx solid #eef2f7; }
	.avatar { width: 80rpx; height: 80rpx; border-radius: 24rpx; flex-shrink: 0; }
	.record-main { flex: 1; min-width: 0; margin-left: 16rpx; }
	.record-name { font-size: 28rpx; font-weight: 700; color: #1f2937; }
	.record-time { margin-top: 8rpx; font-size: 20rpx; color: #9aa2b1; }
	.record-desc { margin-top: 8rpx; font-size: 22rpx; line-height: 1.6; color: #6b7280; }
	.record-status { margin-left: 16rpx; flex-shrink: 0; text-align: right; font-size: 24rpx; font-weight: 700; }
	.record-status.done { color: #7ed321; }
	.record-status.pending { color: #ff9f1c; }
</style>
