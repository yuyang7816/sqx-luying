<template>
	<view class="page" :class="{ 'is-empty': !records.length && !pageLoading }">
		<view class="section-card">
			<text class="section-title">提现明细</text>
			<text class="section-desc">查看余额提现申请与审核进度</text>
		</view>

		<view v-if="pageLoading" class="loading-state"><text>加载提现记录...</text></view>
		<view v-else-if="!records.length" class="empty-state">
			<up-icon name="file-text" size="34" color="#a7b19f"></up-icon>
			<text class="empty-title">暂无提现记录</text>
			<text class="empty-desc">提现申请提交后，会在这里展示审核进度和到账结果。</text>
		</view>

		<view v-else class="record-list">
			<view v-for="item in records" :key="item.id" class="record-card">
				<view class="record-head">
					<text class="record-title">{{ item.accountLabel }}</text>
					<text class="record-status" :class="item.status">{{ item.statusText }}</text>
				</view>
				<text class="record-amount">¥{{ Number(item.amount).toFixed(2) }}</text>
				<text class="record-line">到账金额：¥{{ Number(item.actualAmount).toFixed(2) }}</text>
				<text class="record-line">手续费：¥{{ Number(item.serviceFee).toFixed(2) }}</text>
				<text class="record-line">收款账户：{{ item.accountNumber }}</text>
				<view v-if="item.status === 'reject'" class="reject-reason">
					<text class="reject-reason__label">拒绝原因</text>
					<text class="reject-reason__text">{{ item.reason || '未填写拒绝原因，如有疑问请联系客服' }}</text>
				</view>
				<text class="record-time">{{ item.time }}</text>
			</view>
		</view>
	</view>
</template>

<script setup>
	import { ref } from 'vue';
	import { onShow } from '@dcloudio/uni-app';
	import { fetchWithdrawRecords } from '@/common/api/wallet';
	import { ensureLogin } from '@/common/auth';

	const records = ref([]);
	const pageLoading = ref(false);

	const syncRecords = async () => {
		pageLoading.value = true;
		try {
			const pageVo = await fetchWithdrawRecords();
			records.value = pageVo.records || [];
		} catch (error) {
			records.value = [];
		} finally {
			pageLoading.value = false;
		}
	};

	onShow(() => {
		if (!ensureLogin('/packageUser/wallet-withdraw-records')) return;
		syncRecords();
	});
</script>

<style scoped>
	.page { box-sizing: border-box; min-height: 100vh; padding: 24rpx; background: #f5f7ff; overflow-x: hidden; }
	.section-card, .record-card, .empty-state, .loading-state { box-sizing: border-box; width: 100%; border-radius: 28rpx; background: #ffffff; overflow: hidden; }
	.section-card { padding: 28rpx; }
	.loading-state { margin-top: 24rpx; padding: 80rpx 40rpx; text-align: center; color: #9aa2b1; }
	.section-title, .section-desc, .empty-title, .empty-desc, .record-title, .record-status, .record-amount, .record-line, .record-time { display: block; }
	.section-title { font-size: 32rpx; font-weight: 800; color: #1a2114; }
	.section-desc { margin-top: 12rpx; font-size: 24rpx; line-height: 1.7; color: #8a94a6; }
	.empty-state { display: flex; flex-direction: column; align-items: center; justify-content: center; margin-top: 24rpx; padding: 120rpx 40rpx; }
	.empty-title { margin-top: 24rpx; font-size: 30rpx; font-weight: 700; color: #1a2114; }
	.empty-desc { margin-top: 14rpx; font-size: 24rpx; line-height: 1.7; text-align: center; color: #8a9a7a; }
	.record-list { margin-top: 24rpx; }
	.record-card { margin-bottom: 24rpx; padding: 28rpx; }
	.record-head { display: flex; align-items: center; justify-content: space-between; gap: 16rpx; }
	.record-title { font-size: 28rpx; font-weight: 700; color: #1f2937; }
	.record-status { padding: 8rpx 16rpx; border-radius: 18rpx; font-size: 20rpx; font-weight: 600; }
	.record-status.pending { color: #c07b00; background: #fff6df; }
	.record-status.success { color: #3e8e0a; background: #eefadb; }
	.record-status.reject { color: #d63b3b; background: #fff0f0; }
	.record-amount { margin-top: 18rpx; font-size: 44rpx; font-weight: 800; color: #1f2937; }
	.record-line { margin-top: 12rpx; font-size: 22rpx; line-height: 1.7; color: #6b7280; word-break: break-all; }
	.record-time { margin-top: 16rpx; font-size: 20rpx; color: #9aa2b1; }
	.reject-reason { margin-top: 16rpx; padding: 18rpx 20rpx; border-radius: 18rpx; background: #fff5f5; }
	.reject-reason__label, .reject-reason__text { display: block; }
	.reject-reason__label { font-size: 22rpx; font-weight: 700; color: #d63b3b; }
	.reject-reason__text { margin-top: 8rpx; font-size: 22rpx; line-height: 1.7; color: #7f1d1d; word-break: break-all; }
</style>
