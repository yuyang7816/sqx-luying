<template>
	<view class="page">
		<view v-if="!selectMode" class="tab-wrap">
			<u-tabs :list="tabList" :current="currentTabIndex" :scrollable="true" lineColor="#7ed321" :activeStyle="tabActiveStyle" :inactiveStyle="tabInactiveStyle" @change="handleTabChange"></u-tabs>
		</view>

		<view v-if="selectMode" class="select-head">
			<text class="select-title">选择优惠券</text>
			<text class="select-desc">仅展示当前订单可用优惠券</text>
			<text class="skip-btn" @click="skipCoupon">不使用优惠券</text>
		</view>

		<view v-if="pageLoading" class="loading-card"><text>加载优惠券...</text></view>
		<view v-else-if="!couponList.length" class="empty-card">
			<up-icon name="coupon" size="30" color="#a7b19f"></up-icon>
			<text class="empty-title">{{ selectMode ? '暂无可用优惠券' : `暂无${activeTab}优惠券` }}</text>
			<text class="empty-desc">{{ selectMode ? '当前订单金额暂不满足任何优惠券使用条件。' : '下单或参与活动后，可用优惠券会集中展示在这里。' }}</text>
		</view>

		<view v-else v-for="item in couponList" :key="item.id" class="coupon-card" :class="[item.type, { selected: selectMode && selectedCouponId === item.id }]" @click="handleCouponClick(item)">
			<view class="coupon-left">
				<text class="coupon-value">¥{{ item.amount }}</text>
				<text class="coupon-type">{{ item.label }}</text>
			</view>
			<view class="coupon-main">
				<text class="coupon-title">{{ item.name }}</text>
				<text class="coupon-sub">{{ item.condition }}</text>
				<text class="coupon-date">{{ item.status === 'used' ? `使用时间 ${item.usedTime}` : `有效期至 ${item.expire}` }}</text>
			</view>
			<text v-if="selectMode" class="use-btn">{{ selectedCouponId === item.id ? '已选' : '使用' }}</text>
			<text v-else-if="item.status === 'unused'" class="use-btn" @click.stop="useCoupon(item)">立即使用</text>
			<text v-else class="status-tag">{{ item.status === 'used' ? '已使用' : '已过期' }}</text>
		</view>
	</view>
</template>

<script setup>
	import { computed, ref } from 'vue';
	import { onLoad, onShow } from '@dcloudio/uni-app';
	import { fetchCouponPage, fetchUsableCoupons } from '@/common/api/coupon';
	import { ensureLogin } from '@/common/auth';
	import {
		clearConfirmCouponContext,
		getConfirmCouponContext,
		setConfirmCouponSelection
	} from '@/common/rental/confirmStorage';

	const tabs = ['未使用', '已使用', '已过期'];
	const activeTab = ref('未使用');
	const couponList = ref([]);
	const pageLoading = ref(false);
	const selectMode = ref(false);
	const selectedCouponId = ref(0);
	const tabList = tabs.map((item) => ({ name: item }));
	const tabActiveStyle = { color: '#59b50e', fontWeight: '600' };
	const tabInactiveStyle = { color: '#9aa2b1' };
	const currentTabIndex = computed(() => Math.max(tabs.findIndex((item) => item === activeTab.value), 0));

	const syncCoupons = async () => {
		pageLoading.value = true;
		try {
			if (selectMode.value) {
				const context = getConfirmCouponContext() || {};
				selectedCouponId.value = Number(context.selectedCouponId || 0);
				couponList.value = await fetchUsableCoupons({
					rentAmount: Number(context.rentAmount || 0),
					deliveryAmount: Number(context.deliveryAmount || 0),
					depositAmount: Number(context.depositAmount || 0)
				});
				return;
			}
			const pageVo = await fetchCouponPage({ status: activeTab.value });
			couponList.value = pageVo.records || [];
		} catch (error) {
			couponList.value = [];
		} finally {
			pageLoading.value = false;
		}
	};

	onLoad((options) => {
		selectMode.value = options.select === '1';
		if (selectMode.value) {
			uni.setNavigationBarTitle({ title: '选择优惠券' });
		}
	});

	onShow(() => {
		if (!ensureLogin('/packageUser/coupon')) return;
		syncCoupons();
	});

	const handleTabChange = (item) => {
		activeTab.value = item.name;
		syncCoupons();
	};

	const selectCouponForOrder = (item) => {
		setConfirmCouponSelection(item);
		clearConfirmCouponContext();
		uni.navigateBack();
	};

	const skipCoupon = () => {
		setConfirmCouponSelection(null);
		clearConfirmCouponContext();
		uni.navigateBack();
	};

	const handleCouponClick = (item) => {
		if (!selectMode.value) return;
		if (Number(selectedCouponId.value) === Number(item.id)) {
			skipCoupon();
			return;
		}
		selectCouponForOrder(item);
	};

	const useCoupon = () => {
		uni.switchTab({ url: '/pages/index/index' });
	};

	const goCouponCenter = () => {
		uni.navigateTo({ url: '/packageUser/coupon-center' });
	};
</script>

<style scoped>
	.page { box-sizing: border-box; min-height: 100vh; background: #f5f7ff; overflow-x: hidden; }
	.page-head {
		margin: 24rpx 24rpx 0;
		padding: 22rpx 24rpx;
		background: #fff;
		border-radius: 20rpx;
	}
	.head-desc { display: block; font-size: 20rpx; line-height: 1.5; color: #6b7280; }
	.head-link {
		display: flex;
		align-items: center;
		justify-content: flex-end;
		gap: 4rpx;
		margin-top: 12rpx;
		font-size: 22rpx;
		color: #59b50e;
	}
	.tab-wrap { box-sizing: border-box; width: 100%; padding: 0 24rpx; background: #fff; }
	.tab-wrap :deep(.u-tabs__wrapper__nav__item) { padding: 0 24rpx; }
	.tab-wrap :deep(.u-tabs__wrapper__nav__item__text) { font-size: 24rpx; white-space: nowrap; }
	.select-head { margin: 24rpx 24rpx 0; padding: 22rpx 24rpx; background: #fff; border-radius: 20rpx; }
	.select-title, .select-desc, .skip-btn { display: block; }
	.select-title { font-size: 26rpx; font-weight: 600; color: #1f2937; }
	.select-desc { margin-top: 8rpx; font-size: 20rpx; color: #9aa2b1; }
	.skip-btn { margin-top: 14rpx; font-size: 22rpx; color: #59b50e; text-align: right; }
	.loading-card, .empty-card { box-sizing: border-box; margin: 24rpx; padding: 40rpx 28rpx; background: #fff; border-radius: 24rpx; text-align: center; }
	.empty-title, .empty-desc { display: block; }
	.empty-title { margin-top: 16rpx; font-size: 24rpx; font-weight: 600; color: #1f2937; }
	.empty-desc { margin-top: 10rpx; font-size: 20rpx; line-height: 1.55; color: #9aa2b1; }
	.coupon-card { box-sizing: border-box; display: flex; align-items: center; margin: 20rpx 24rpx; padding: 24rpx 0; background: #fff; border-radius: 24rpx; overflow: hidden; }
	.coupon-card.green { border-left: 6rpx solid #7ed321; }
	.coupon-card.orange { border-left: 6rpx solid var(--theme-primary); }
	.coupon-card.selected { box-shadow: 0 8rpx 20rpx rgba(126, 211, 33, 0.12); background: linear-gradient(180deg, #fff 0%, #f8fdea 100%); }
	.coupon-left, .coupon-main { padding: 0 20rpx; }
	.coupon-left { width: 128rpx; flex-shrink: 0; }
	.coupon-value, .coupon-type, .coupon-title, .coupon-sub, .coupon-date, .use-btn, .status-tag { display: block; }
	.coupon-value { font-size: 40rpx; font-weight: 700; color: #7ed321; }
	.orange .coupon-value { color: var(--theme-primary); }
	.coupon-type { font-size: 20rpx; color: #9aa2b1; }
	.coupon-main { flex: 1; min-width: 0; }
	.coupon-title { font-size: 24rpx; font-weight: 600; color: #4b5563; }
	.coupon-sub { margin-top: 6rpx; font-size: 20rpx; color: #9aa2b1; }
	.coupon-date { margin-top: 8rpx; font-size: 18rpx; color: #c0c6d4; }
	.coupon-title, .coupon-sub, .coupon-date { word-break: break-all; }
	.use-btn { margin-right: 20rpx; flex-shrink: 0; padding: 12rpx 20rpx; border-radius: 22rpx; font-size: 20rpx; color: #fff; background: #7ed321; }
	.orange .use-btn { background: var(--theme-primary); }
	.status-tag { margin-right: 20rpx; flex-shrink: 0; font-size: 20rpx; color: #9aa2b1; }
</style>
