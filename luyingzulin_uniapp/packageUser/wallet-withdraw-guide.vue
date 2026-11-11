<template>
	<view class="page">
		<view v-if="pageLoading" class="loading-card"><text>加载提现信息...</text></view>
		<view v-else class="guide-card">
			<view class="guide-icon">
				<up-icon name="rmb-circle-fill" size="44" color="#7ed321"></up-icon>
			</view>
			<text class="guide-title">尚未绑定收款账户</text>
			<text class="guide-desc">提现前需要先绑定支付宝或微信收款方式。绑定完成后即可提交提现申请，审核通过后将按收款方式到账。</text>
			<view class="guide-steps">
				<view class="step-item">
					<text class="step-index">1</text>
					<text class="step-text">选择支付宝或微信收款方式</text>
				</view>
				<view class="step-item">
					<text class="step-index">2</text>
					<text class="step-text">填写收款信息并保存</text>
				</view>
				<view class="step-item">
					<text class="step-index">3</text>
					<text class="step-text">返回提现页提交申请</text>
				</view>
			</view>
			<button class="bind-btn" @click="goBind">去绑定收款账户</button>
		</view>
	</view>
</template>

<script setup>
	import { ref } from 'vue';
	import { onShow } from '@dcloudio/uni-app';
	import { fetchWalletDetail } from '@/common/api/wallet';
	import { hasAnyWithdrawAccountBound } from '@/common/api/assetsMappers';
	import { ensureLogin } from '@/common/auth';

	const pageLoading = ref(false);

	const syncGuideState = async () => {
		pageLoading.value = true;
		try {
			const detail = await fetchWalletDetail();
			if (hasAnyWithdrawAccountBound(detail.withdrawAccounts || {})) {
				uni.redirectTo({ url: '/packageUser/wallet-withdraw' });
			}
		} catch (error) {
			// toast 已由 http 层处理
		} finally {
			pageLoading.value = false;
		}
	};

	onShow(() => {
		if (!ensureLogin('/packageUser/wallet-withdraw-guide')) return;
		syncGuideState();
	});

	const goBind = () => {
		uni.navigateTo({ url: '/packageUser/wallet-withdraw-bind' });
	};
</script>

<style scoped>
	.page { box-sizing: border-box; min-height: 100vh; padding: 24rpx; background: #f5f7ff; overflow-x: hidden; }
	.loading-card, .guide-card { box-sizing: border-box; width: 100%; padding: 48rpx 32rpx; border-radius: 28rpx; background: #fff; text-align: center; }
	.loading-card { color: #8a94a6; }
	.guide-card { padding: 72rpx 40rpx 56rpx; }
	.guide-icon { display: flex; align-items: center; justify-content: center; width: 120rpx; height: 120rpx; margin: 0 auto 28rpx; border-radius: 50%; background: #eefadb; }
	.guide-title, .guide-desc, .bind-btn { display: block; }
	.guide-title { font-size: 34rpx; font-weight: 700; color: #1f2937; }
	.guide-desc { margin-top: 16rpx; font-size: 24rpx; line-height: 1.7; color: #6b7280; text-align: left; }
	.guide-steps { margin-top: 32rpx; padding: 28rpx; border-radius: 24rpx; background: #f8fafc; text-align: left; }
	.step-item { display: flex; align-items: flex-start; gap: 16rpx; }
	.step-item + .step-item { margin-top: 20rpx; }
	.step-index { flex-shrink: 0; width: 40rpx; height: 40rpx; line-height: 40rpx; border-radius: 50%; text-align: center; font-size: 22rpx; font-weight: 700; color: #ffffff; background: #7ed321; }
	.step-text { flex: 1; min-width: 0; font-size: 24rpx; line-height: 1.6; color: #4b5563; }
	.bind-btn { width: 100%; height: 88rpx; margin-top: 40rpx; line-height: 88rpx; border-radius: 44rpx; font-size: 28rpx; font-weight: 700; color: #ffffff; background: linear-gradient(135deg, #7ed321, #6bc01a); border: none; }
</style>
