<script setup>
	import { computed, ref } from 'vue';
	import { onShow } from '@dcloudio/uni-app';
	import { ensureLogin } from '@/common/auth';
	import {
		fetchMerchantHome,
		canAccessMerchantWorkbench,
		MERCHANT_MENUS,
		MERCHANT_STEPS
	} from '@/common/api/merchant';

	const merchantSteps = MERCHANT_STEPS;
	const merchantMenus = MERCHANT_MENUS;
	const merchantSummary = ref({
		status: 'pending',
		statusLabel: '加载中',
		tag: '—',
		storeName: '—'
	});
	const loading = ref(false);

	const isApproved = computed(() => canAccessMerchantWorkbench(merchantSummary.value.status));
	const isReviewing = computed(() => merchantSummary.value.status === 'reviewing');
	const isRejected = computed(() => merchantSummary.value.status === 'rejected');

	const goPage = (url) => {
		if (!ensureLogin(url)) return;
		if (url === '/packageMerchant/dashboard' && !isApproved.value) {
			uni.showToast({ title: '请先完成入驻审核', icon: 'none' });
			uni.navigateTo({ url: '/packageMerchant/apply' });
			return;
		}
		if (['/packageMerchant/goods', '/packageMerchant/orders', '/packageMerchant/store'].includes(url) && !isApproved.value) {
			uni.showToast({ title: '请先完成入驻审核', icon: 'none' });
			uni.navigateTo({ url: '/packageMerchant/apply' });
			return;
		}
		uni.navigateTo({ url });
	};

	const goWorkbench = () => goPage('/packageMerchant/dashboard');

	const loadData = async () => {
		loading.value = true;
		try {
			const data = await fetchMerchantHome();
			merchantSummary.value = data.summary || merchantSummary.value;
		} catch (error) {
			uni.showToast({ title: error?.msg || error?.message || '加载失败', icon: 'none' });
		} finally {
			loading.value = false;
		}
	};

	onShow(loadData);
</script>

<template>
	<view class="page">
		<view class="hero-card">
			<text class="hero-title">商家中心</text>
			<text class="hero-desc">面向多商家经营场景，用户可在这里完成入驻、跟进审核进度，并进入商家工作台处理商品与订单。</text>
		</view>

		<view class="card">
			<text class="section-title">入驻流程</text>
			<view v-for="(item, index) in merchantSteps" :key="item.key" class="step-row">
				<view class="step-index">{{ index + 1 }}</view>
				<view class="step-main">
					<text class="step-title">{{ item.label }}</text>
					<text class="step-desc">{{ item.desc }}</text>
				</view>
			</view>
		</view>

		<view class="card status-card">
			<view class="status-head">
				<view>
					<text class="section-title">当前状态</text>
					<text class="status-name">{{ merchantSummary.statusLabel }}</text>
				</view>
				<text class="status-tag">{{ merchantSummary.tag }}</text>
			</view>
			<text class="status-desc">门店：{{ merchantSummary.storeName }}</text>
			<view class="btn-row">
				<button class="ghost-btn" @click="goPage('/packageMerchant/progress')">查看进度</button>
				<button v-if="isApproved" class="primary-btn" @click="goWorkbench">进入工作台</button>
				<button v-else-if="isRejected" class="primary-btn" @click="goPage('/packageMerchant/apply?resubmit=1')">重新提交</button>
				<button v-else class="primary-btn" @click="goPage('/packageMerchant/apply')">查看审核状态</button>
			</view>
		</view>

		<view class="card">
			<text class="section-title">商家模块</text>
			<view class="menu-grid">
				<view v-for="item in merchantMenus" :key="item.label" class="menu-item" @click="goPage(item.url)">
					<view class="menu-icon"><up-icon :name="item.icon" size="24" color="#7ed321"></up-icon></view>
					<text class="menu-label">{{ item.label }}</text>
				</view>
			</view>
		</view>
	</view>
</template>

<style scoped>
	.page { box-sizing: border-box; min-height: 100vh; padding: 24rpx; background: #f5f7ff; overflow-x: hidden; }
	.hero-card { box-sizing: border-box; padding: 32rpx; border-radius: 32rpx; background: linear-gradient(135deg, var(--theme-hero-start), var(--theme-hero-end)); }
	.hero-title, .hero-desc, .section-title, .step-title, .step-desc, .status-name, .status-desc, .status-tag, .menu-label { display: block; }
	.hero-title { font-size: 40rpx; font-weight: 700; color: #ffffff; }
	.hero-desc { margin-top: 14rpx; font-size: 24rpx; line-height: 1.7; color: rgba(255,255,255,0.9); }
	.card { box-sizing: border-box; width: 100%; margin-top: 24rpx; padding: 28rpx; border-radius: 28rpx; background: #ffffff; overflow: hidden; }
	.section-title { font-size: 30rpx; font-weight: 700; color: #1f2937; }
	.step-row { display: flex; margin-top: 24rpx; }
	.step-index { width: 48rpx; height: 48rpx; line-height: 48rpx; border-radius: 50%; text-align: center; font-size: 24rpx; font-weight: 700; color: #ffffff; background: #7ed321; }
	.step-main { flex: 1; min-width: 0; margin-left: 16rpx; }
	.step-title { font-size: 26rpx; font-weight: 700; color: #1f2937; }
	.step-desc { margin-top: 8rpx; font-size: 22rpx; line-height: 1.6; color: #6b7280; }
	.status-head { display: flex; justify-content: space-between; align-items: flex-start; }
	.status-name { margin-top: 12rpx; font-size: 36rpx; font-weight: 700; color: #1f2937; }
	.status-tag { padding: 8rpx 16rpx; border-radius: 18rpx; font-size: 20rpx; color: #5f8c00; background: #eefadb; }
	.status-desc { margin-top: 14rpx; font-size: 22rpx; line-height: 1.7; color: #6b7280; }
	.btn-row { display: flex; justify-content: space-between; margin-top: 24rpx; }
	.primary-btn, .ghost-btn { box-sizing: border-box; min-width: 0; flex: 1; height: 84rpx; line-height: 84rpx; margin: 0; border-radius: 42rpx; font-size: 28rpx; }
	.ghost-btn { margin-right: 16rpx; }
	.primary-btn { color: #ffffff; background: #7ed321; }
	.ghost-btn { color: #5f8c00; background: #eefadb; }
	.menu-grid { display: flex; flex-wrap: wrap; margin-top: 20rpx; }
	.menu-item { box-sizing: border-box; width: 25%; margin-bottom: 24rpx; padding: 0 8rpx; text-align: center; }
	.menu-icon { display: flex; align-items: center; justify-content: center; width: 72rpx; height: 72rpx; margin: 0 auto; border-radius: 22rpx; background: #eefadb; }
	.menu-label { margin-top: 10rpx; font-size: 22rpx; color: #4b5563; }
</style>
