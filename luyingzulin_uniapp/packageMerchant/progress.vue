<script setup>
	import { computed, ref } from 'vue';
	import { onShow } from '@dcloudio/uni-app';
	import { ensureLogin } from '@/common/auth';
	import {
		fetchApplyProgress,
		canAccessMerchantWorkbench,
		canResubmitMerchantApply,
		shouldShowApplyStatusPage
	} from '@/common/api/merchant';

	const merchantSummary = ref({
		status: 'pending',
		statusLabel: '加载中',
		storeName: '—',
		contactName: '—',
		contactPhone: '—',
		applyTime: '—',
		rejectReason: ''
	});
	const merchantProgress = ref([]);

	const isApproved = computed(() => canAccessMerchantWorkbench(merchantSummary.value.status));
	const isRejected = computed(() => canResubmitMerchantApply(merchantSummary.value.status));
	const isReviewing = computed(() => shouldShowApplyStatusPage(merchantSummary.value.status));

	const goDashboard = () => {
		if (!ensureLogin('/packageMerchant/dashboard')) return;
		uni.navigateTo({ url: '/packageMerchant/dashboard' });
	};

	const goResubmit = () => {
		uni.navigateTo({ url: '/packageMerchant/apply?resubmit=1' });
	};

	const loadData = async () => {
		if (!ensureLogin('/packageMerchant/progress')) return;
		try {
			const data = await fetchApplyProgress();
			merchantSummary.value = data.summary;
			merchantProgress.value = data.timeline || [];
			if (canAccessMerchantWorkbench(data.summary?.status)) {
				uni.redirectTo({ url: '/packageMerchant/dashboard' });
			}
		} catch (error) {
			uni.showToast({ title: error?.msg || error?.message || '加载进度失败', icon: 'none' });
		}
	};

	onShow(loadData);
</script>

<template>
	<view class="page">
		<view class="hero-card">
			<text class="hero-title">{{ merchantSummary.statusLabel }}</text>
			<text class="hero-desc">
				{{ isReviewing ? '您的入驻申请正在审核中，审核通过后方可进入商户工作台。' : '申请时间：' + merchantSummary.applyTime + '，可在下方查看审核进度与申请信息。' }}
			</text>
		</view>

		<view v-if="isRejected && merchantSummary.rejectReason" class="card reject-card">
			<text class="section-title">驳回原因</text>
			<text class="reject-reason">{{ merchantSummary.rejectReason }}</text>
		</view>

		<view class="card">
			<text class="section-title">申请信息</text>
			<view class="line"><text>门店名称</text><text>{{ merchantSummary.storeName }}</text></view>
			<view class="line"><text>负责人</text><text>{{ merchantSummary.contactName }}</text></view>
			<view class="line"><text>联系电话</text><text>{{ merchantSummary.contactPhone }}</text></view>
		</view>

		<view class="card">
			<text class="section-title">审核进度</text>
			<view v-for="(item, index) in merchantProgress" :key="item.title + index" class="progress-row">
				<view class="progress-index">{{ index + 1 }}</view>
				<view class="progress-main">
					<text class="progress-title">{{ item.title }}</text>
					<text class="progress-time">{{ item.time }}</text>
					<text class="progress-desc">{{ item.desc }}</text>
				</view>
			</view>
		</view>

		<view v-if="isApproved || isRejected" class="card action-card">
			<button v-if="isApproved" class="primary-btn full-btn" @click="goDashboard">进入商家工作台</button>
			<button v-if="isRejected" class="primary-btn full-btn" @click="goResubmit">重新提交申请</button>
		</view>
	</view>
</template>

<style scoped>
	.page { box-sizing: border-box; min-height: 100vh; padding: 24rpx; background: #f5f7ff; overflow-x: hidden; }
	.hero-card { box-sizing: border-box; padding: 32rpx; border-radius: 32rpx; background: linear-gradient(135deg, var(--theme-hero-start), var(--theme-hero-end)); }
	.hero-title, .hero-desc, .section-title, .progress-title, .progress-time, .progress-desc, .reject-reason { display: block; }
	.hero-title { font-size: 40rpx; font-weight: 700; color: #ffffff; }
	.hero-desc { margin-top: 14rpx; font-size: 24rpx; line-height: 1.7; color: rgba(255,255,255,0.92); }
	.card { box-sizing: border-box; width: 100%; margin-top: 24rpx; padding: 28rpx; border-radius: 28rpx; background: #ffffff; overflow: hidden; }
	.reject-card { background: #fff7f7; }
	.reject-reason { margin-top: 16rpx; font-size: 28rpx; line-height: 1.7; color: #c0392b; font-weight: 600; }
	.section-title { font-size: 30rpx; font-weight: 700; color: #1f2937; }
	.line { display: flex; justify-content: space-between; align-items: flex-start; margin-top: 20rpx; font-size: 24rpx; line-height: 1.6; color: #4b5563; }
	.line text:first-child { width: 132rpx; flex-shrink: 0; color: #8f97a5; }
	.line text:last-child { flex: 1; min-width: 0; text-align: right; word-break: break-all; color: #1f2937; }
	.progress-row { display: flex; margin-top: 24rpx; }
	.progress-index { width: 48rpx; height: 48rpx; line-height: 48rpx; border-radius: 50%; text-align: center; font-size: 24rpx; font-weight: 700; color: #ffffff; background: var(--theme-primary); }
	.progress-main { flex: 1; min-width: 0; margin-left: 16rpx; }
	.progress-title { font-size: 26rpx; font-weight: 700; color: #1f2937; }
	.progress-time { margin-top: 8rpx; font-size: 20rpx; color: #9aa2b1; }
	.progress-desc { margin-top: 8rpx; font-size: 22rpx; line-height: 1.6; color: #6b7280; }
	.action-card { margin-bottom: 40rpx; }
	.primary-btn { box-sizing: border-box; height: 84rpx; line-height: 84rpx; margin: 0; border-radius: 42rpx; font-size: 28rpx; color: #ffffff; background: #7ed321; }
	.full-btn { width: 100%; }
</style>
