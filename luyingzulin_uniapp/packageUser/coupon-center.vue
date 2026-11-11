<template>
	<view class="page">
		<view class="page-head">
			<text class="head-title">领券中心</text>
			<text class="head-desc">领取后可到「我的优惠券」查看并使用</text>
			<view class="head-link" @click="goMyCoupons">
				<text>我的优惠券</text>
				<up-icon name="arrow-right" size="12" color="#59b50e"></up-icon>
			</view>
		</view>

		<view v-if="pageLoading" class="loading-card"><text>加载可领优惠券...</text></view>
		<view v-else-if="!couponList.length" class="empty-card">
			<up-icon name="coupon" size="30" color="#a7b19f"></up-icon>
			<text class="empty-title">暂无可领优惠券</text>
			<text class="empty-desc">活动上线后会在这里展示，请稍后再来看看。</text>
		</view>

		<view v-else v-for="item in couponList" :key="item.templateId" class="coupon-card" :class="item.type">
			<view class="coupon-left">
				<text class="coupon-value">¥{{ item.amount }}</text>
				<text class="coupon-type">{{ item.label }}</text>
			</view>
			<view class="coupon-main">
				<text class="coupon-title">{{ item.name }}</text>
				<text class="coupon-sub">{{ item.condition }}</text>
				<text class="coupon-date">有效期至 {{ item.expire || '长期有效' }}</text>
			</view>
			<text
				class="claim-btn"
				:class="{ claimed: item.claimed, loading: claimingId === item.templateId }"
				@click="handleClaim(item)"
			>{{ item.claimed ? '已领取' : (claimingId === item.templateId ? '领取中' : '立即领取') }}</text>
		</view>
	</view>
</template>

<script setup>
	import { ref } from 'vue';
	import { onShow } from '@dcloudio/uni-app';
	import { claimCoupon, fetchClaimableCoupons } from '@/common/api/coupon';
	import { ensureLogin } from '@/common/auth';
	import { runAction } from '@/common/actionFeedback';

	const couponList = ref([]);
	const pageLoading = ref(false);
	const claimingId = ref(0);

	const syncCoupons = async () => {
		pageLoading.value = true;
		try {
			couponList.value = await fetchClaimableCoupons({ silentError: true });
		} catch (error) {
			couponList.value = [];
		} finally {
			pageLoading.value = false;
		}
	};

	onShow(() => {
		if (!ensureLogin('/packageUser/coupon-center')) return;
		syncCoupons();
	});

	const goMyCoupons = () => {
		uni.navigateTo({ url: '/packageUser/coupon' });
	};

	const handleClaim = async (item) => {
		if (!item || item.claimed || claimingId.value) return;
		claimingId.value = item.templateId;
		await runAction({
			action: () => claimCoupon(item.templateId),
			successText: '领取成功',
			errorText: '领取失败，请稍后重试',
			onSuccess: async () => {
				item.claimed = true;
				await syncCoupons();
			}
		});
		claimingId.value = 0;
	};
</script>

<style scoped>
	.page {
		box-sizing: border-box;
		min-height: 100vh;
		padding: 24rpx 24rpx calc(24rpx + env(safe-area-inset-bottom));
		background: #f5f7ff;
		overflow-x: hidden;
	}

	.page-head {
		margin-bottom: 20rpx;
		padding: 24rpx;
		border-radius: 24rpx;
		background: linear-gradient(135deg, #f8fdea 0%, #eef8d5 100%);
		border: 1rpx solid rgba(212, 229, 180, 0.88);
	}

	.head-title,
	.head-desc,
	.head-link text {
		display: block;
	}

	.head-title {
		font-size: 28rpx;
		font-weight: 600;
		color: #1f2937;
	}

	.head-desc {
		margin-top: 8rpx;
		font-size: 20rpx;
		line-height: 1.5;
		color: #6b7280;
	}

	.head-link {
		display: flex;
		align-items: center;
		justify-content: flex-end;
		gap: 4rpx;
		margin-top: 12rpx;
		font-size: 22rpx;
		color: #59b50e;
	}

	.loading-card,
	.empty-card {
		box-sizing: border-box;
		padding: 40rpx 28rpx;
		background: #fff;
		border-radius: 24rpx;
		text-align: center;
	}

	.empty-title,
	.empty-desc {
		display: block;
	}

	.empty-title {
		margin-top: 16rpx;
		font-size: 24rpx;
		font-weight: 600;
		color: #1f2937;
	}

	.empty-desc {
		margin-top: 10rpx;
		font-size: 20rpx;
		line-height: 1.55;
		color: #9aa2b1;
	}

	.coupon-card {
		box-sizing: border-box;
		display: flex;
		align-items: center;
		margin-bottom: 20rpx;
		padding: 24rpx 0;
		background: #fff;
		border-radius: 24rpx;
		overflow: hidden;
	}

	.coupon-card.green {
		border-left: 6rpx solid #7ed321;
	}

	.coupon-card.orange {
		border-left: 6rpx solid var(--theme-primary);
	}

	.coupon-left,
	.coupon-main {
		padding: 0 20rpx;
	}

	.coupon-left {
		width: 128rpx;
		flex-shrink: 0;
	}

	.coupon-value,
	.coupon-type,
	.coupon-title,
	.coupon-sub,
	.coupon-date,
	.claim-btn {
		display: block;
	}

	.coupon-value {
		font-size: 40rpx;
		font-weight: 700;
		color: #7ed321;
	}

	.orange .coupon-value {
		color: var(--theme-primary);
	}

	.coupon-type {
		font-size: 20rpx;
		color: #9aa2b1;
	}

	.coupon-main {
		flex: 1;
		min-width: 0;
	}

	.coupon-title {
		font-size: 24rpx;
		font-weight: 600;
		color: #4b5563;
	}

	.coupon-sub {
		margin-top: 6rpx;
		font-size: 20rpx;
		color: #9aa2b1;
	}

	.coupon-date {
		margin-top: 8rpx;
		font-size: 18rpx;
		color: #c0c6d4;
	}

	.claim-btn {
		margin-right: 20rpx;
		flex-shrink: 0;
		padding: 12rpx 20rpx;
		border-radius: 22rpx;
		font-size: 20rpx;
		color: #fff;
		background: #7ed321;
	}

	.orange .claim-btn {
		background: var(--theme-primary);
	}

	.claim-btn.claimed {
		color: #9aa2b1;
		background: #eef1f6;
	}

	.claim-btn.loading {
		opacity: 0.72;
	}
</style>
