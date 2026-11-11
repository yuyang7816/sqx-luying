<template>
	<view class="page">
		<view class="hero-shell">
			<view class="top-bar">
				<view class="city-pill" @click="goCitySelect">
					<up-icon name="map" size="16" color="#6f7e63"></up-icon>
					<text class="city-text">{{ cityName }}</text>
					<up-icon name="arrow-down" size="12" color="#8aa076"></up-icon>
				</view>
				<view class="search-box" @click="goSearch">
					<up-icon name="search" size="16" color="#aab592"></up-icon>
					<text class="search-text">搜索帐篷、天幕、营地车</text>
				</view>
			</view>

			<view v-if="bannerList.length" class="banner-wrap">
				<swiper class="banner-swiper" circular autoplay interval="4000" indicator-dots indicator-color="rgba(255,255,255,0.35)" indicator-active-color="#ffffff">
					<swiper-item v-for="item in bannerList" :key="item.id" @click="handleBannerTap(item)">
						<image class="banner-image" :src="item.imageUrl" mode="aspectFill"></image>
					</swiper-item>
				</swiper>
			</view>
			<view v-else class="banner-wrap fallback-banner" @click="handleBannerTap(heroBanner)">
				<image v-if="heroBanner?.imageUrl" class="banner-image" :src="heroBanner.imageUrl" mode="aspectFill"></image>
			</view>
		</view>

		<view class="content-wrap">
			<view class="quick-panel">
				<view class="quick-grid">
					<view v-for="item in quickItems" :key="item.label" class="quick-item" @click="handleQuick(item)">
						<view class="quick-icon" :style="{ background: item.bg }">
							<up-icon :name="item.icon" size="22" :color="item.color"></up-icon>
						</view>
						<text class="quick-label">{{ item.label }}</text>
					</view>
				</view>
			</view>

			<view class="section-tabs">
				<u-tabs :list="sectionTabList" :current="currentTabIndex" :scrollable="true" lineColor="#7ed321" :activeStyle="tabActiveStyle" :inactiveStyle="tabInactiveStyle" @change="handleTabChange"></u-tabs>
			</view>

			<view v-if="loading && !goodsList.length" class="state-box"><text class="state-text">加载中...</text></view>
			<view v-else-if="!goodsList.length" class="state-box"><text class="state-text">暂无商品，请稍后再试</text></view>
			<view v-else class="goods-grid">
				<view v-for="item in goodsList" :key="item.id" class="goods-card" @click="goDetail(item.id)">
					<view class="goods-media">
						<image class="goods-image" :src="item.coverImage" mode="aspectFill"></image>
						<!-- <text class="stock-tag" :class="isGoodsAvailableForCart(item) ? 'in-stock' : 'out-stock'">{{ isGoodsAvailableForCart(item) ? '可租' : '缺货' }}</text> -->
						<!-- <view class="sales-chip">
							<up-icon name="clock" size="12" color="#f7f2e8"></up-icon>
							<text class="sales-chip-text">已租 {{ item.salesCount }}</text>
						</view> -->
					</view>
					<view class="goods-body">
						<!-- <view class="goods-tag-row">
							<text v-for="tag in (item.tags || []).slice(0, 2)" :key="tag" class="goods-tag">{{ tag }}</text>
						</view> -->
						<text class="goods-name">{{ item.name }}</text>
						<!-- <text class="goods-desc">{{ item.subtitle || '营地常用装备，支持按需租期选择。' }}</text> -->
						<view class="price-row">
							<view>
								<text class="goods-price">¥{{ formatPrice(item).price }}</text>
								<text class="goods-unit">/{{ formatPrice(item).unit }}</text>
							</view>
							<text class="deposit-text">押金 ¥{{ Number(item.depositAmount || 0).toFixed(0) }}</text>
						</view>
						<!-- <view class="card-bottom">
							<text class="rule-btn">查看教程</text>
							<view class="plus-btn" @click.stop="handleAddCart(item)">
								<up-icon name="plus" size="18" color="#ffffff"></up-icon>
							</view>
						</view> -->
					</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script setup>
	import { computed, ref } from 'vue';
	import { onPullDownRefresh, onShow } from '@dcloudio/uni-app';
	import { fetchBannerPage } from '@/common/api/banner';
	import { resolveAppCity } from '@/common/api/content';
	import { getAppCity, getLocatedCity, getSelectedCity, locateCurrentCity } from '@/common/city';
	import { fetchGoodsPage } from '@/common/api/rental';
	import { quickAddToCart } from '@/common/rental/cartActions';
	import { ensureLogin } from '@/common/auth';
	import {
		HOME_RENT_TABS,
		buildHomeGoodsQuery,
		formatGoodsRentPrice,
		isGoodsAvailableForCart,
		mapApiRentTypeToDetailMode,
		resolveHomeRentTabIndex
	} from '@/common/rental/constants';

	const cityName = ref(getAppCity() || '定位中...');
	const activeCity = ref(getAppCity());
	const loading = ref(false);
	const bannerList = ref([]);
	const goodsList = ref([]);
	const activeTabIndex = ref(0);
	const cartLoading = ref(false);

	const sectionTabList = HOME_RENT_TABS.map((item) => ({ name: item.name }));
	const tabActiveStyle = { color: '#24351e', fontWeight: '700' };
	const tabInactiveStyle = { color: '#71806a' };
	const currentTabIndex = computed(() => activeTabIndex.value);
	const activeTabConfig = computed(() => HOME_RENT_TABS[activeTabIndex.value] || HOME_RENT_TABS[0]);
	const heroBanner = computed(() => bannerList.value[0] || null);
	const formatPrice = (item) => formatGoodsRentPrice(item, activeTabConfig.value.rentType);

	const quickItems = [{
		label: '领券中心',
		desc: '先领券再下单更划算',
		icon: 'coupon',
		color: '#cc6d22',
		bg: 'linear-gradient(180deg, #fff2e7 0%, #ffe3cb 100%)',
		url: '/packageUser/coupon',
		needLogin: true
	}, {
		label: '租赁指南',
		desc: '新手下单与取还流程',
		icon: 'file-text',
		color: '#4e73b2',
		bg: 'linear-gradient(180deg, #edf4ff 0%, #dce9ff 100%)',
		url: '/packageContent/help'
	}, {
		label: '账户充值',
		desc: '充值余额支付更方便',
		icon: 'rmb-circle',
		color: '#4c8a23',
		bg: 'linear-gradient(180deg, #f1f8df 0%, #deefb8 100%)',
		url: '/packageUser/wallet-recharge',
		needLogin: true
	}, {
		label: '我的收藏',
		desc: '把常租装备先存起来',
		icon: 'heart',
		color: '#c45462',
		bg: 'linear-gradient(180deg, #fff0f2 0%, #ffdfe4 100%)',
		url: '/packageUser/favorite',
		needLogin: true
	}];

	const loadBanners = async () => {
		const { list } = await fetchBannerPage({ page: 1, limit: 6 });
		bannerList.value = list || [];
	};

	const loadGoods = async () => {
		loading.value = true;
		try {
			const query = buildHomeGoodsQuery(activeTabIndex.value);
			const { list } = await fetchGoodsPage({
				page: 1,
				limit: 8,
				rentType: query.rentType,
				sortBy: query.sortBy,
				city: activeCity.value || undefined
			});
			goodsList.value = list || [];
		} catch (error) {
			goodsList.value = [];
		} finally {
			loading.value = false;
		}
	};

	const refreshPage = async () => {
		try {
			// 手动选择的城市优先，避免返回首页时再次 GPS 定位覆盖
			const selected = getSelectedCity();
			let city = selected || getLocatedCity();
			if (!selected) {
				try {
					city = await resolveAppCity(getSelectedCity, locateCurrentCity);
				} catch (error) {
					city = getLocatedCity();
				}
			}
			activeCity.value = city;
			cityName.value = city || '定位失败';
			await Promise.all([loadBanners(), loadGoods()]);
		} catch (error) {
			uni.showToast({ title: '刷新失败，请稍后重试', icon: 'none' });
		} finally {
			uni.stopPullDownRefresh();
		}
	};

	onShow(() => {
		refreshPage();
	});

	onPullDownRefresh(() => {
		refreshPage();
	});

	const goSearch = () => uni.navigateTo({ url: '/packageSearch/index' });
	const goCitySelect = () => uni.navigateTo({ url: '/packageContent/city-select' });
	const handleTabChange = (item) => {
		activeTabIndex.value = resolveHomeRentTabIndex(item.name);
		loadGoods();
	};
	const handleQuick = (item) => {
		if (item.needLogin && !ensureLogin(item.url)) return;
		uni.navigateTo({ url: item.url });
	};
	const goDetail = (id) => uni.navigateTo({ url: `/packageGoods/detail?id=${id}` });
	const handleAddCart = (item) => quickAddToCart({
		goods: item,
		rentModeKey: mapApiRentTypeToDetailMode(activeTabConfig.value.rentType),
		loadingRef: cartLoading,
		loginRedirect: '/pages/cart/index'
	});
	const handleBannerTap = (item) => {
		if (!item?.url) return;
		if (item.url.startsWith('/pages/')) {
			uni.switchTab({ url: item.url });
			return;
		}
		if (item.url.startsWith('/')) {
			uni.navigateTo({ url: item.url });
		}
	};
</script>

<style scoped>
	.page {
		min-height: 100vh;
		background: linear-gradient(180deg, #f3fbeb 0%, #f6fceb 12%, #fbfef5 26%, #ffffff 42%, #ffffff 100%);
	}

	.hero-shell {
		padding: calc(12rpx + env(safe-area-inset-top)) 24rpx 10rpx;
		background: linear-gradient(180deg, rgba(243, 251, 235, 0.98) 0%, rgba(247, 253, 238, 0.88) 42%, rgba(252, 254, 245, 0.48) 72%, rgba(255, 255, 255, 0) 100%);
	}

	.top-bar {
		display: flex;
		align-items: center;
		gap: 16rpx;
		position: sticky;
		top: env(safe-area-inset-top);
		z-index: 20;
		padding: 0 0 14rpx;
		background: rgba(250, 253, 244, 0.94);
		backdrop-filter: blur(14rpx);
	}

	.city-pill,
	.search-box,
	.quick-grid,
	.goods-tag-row,
	.price-row,
	.card-bottom,
	.sales-chip {
		display: flex;
	}

	.city-pill {
		align-items: center;
		gap: 8rpx;
		padding: 0 18rpx;
		height: 72rpx;
		border-radius: 36rpx;
		background: linear-gradient(180deg, #f1fadf 0%, #e7f5cb 100%);
		border: 1rpx solid rgba(204, 226, 168, 0.95);
		flex-shrink: 0;
	}

	.city-text,
	.search-text,
	.quick-label,
	.state-text,
	.stock-tag,
	.sales-chip-text,
	.goods-tag,
	.goods-name,
	.goods-desc,
	.goods-price,
	.goods-unit,
	.deposit-text,
	.rule-btn {
		display: block;
	}

	.city-text {
		font-size: 26rpx;
		font-weight: 600;
		color: #3e4f37;
	}

	.search-box {
		align-items: center;
		gap: 12rpx;
		flex: 1;
		min-width: 0;
		height: 72rpx;
		padding: 0 24rpx;
		border-radius: 36rpx;
		background: #ffffff;
		box-shadow: 0 8rpx 18rpx rgba(36, 46, 34, 0.06);
	}

	.search-text {
		font-size: 24rpx;
		color: #9aa48d;
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}

	.banner-wrap {
		margin-top: 2rpx;
		border-radius: 28rpx;
		overflow: hidden;
		background: #eef8dd;
		box-shadow: 0 18rpx 36rpx rgba(36, 46, 34, 0.08);
	}

	.banner-swiper,
	.banner-image,
	.fallback-banner {
		height: 264rpx;
	}

	.banner-image {
		width: 100%;
		display: block;
	}

	.content-wrap {
		margin-top: -18rpx;
		padding: 0 24rpx calc(24rpx + var(--native-tabbar-offset));
	}

	.content-wrap::before {
		content: '';
		display: block;
		height: 14rpx;
		border-radius: 999rpx;
		margin: 0 auto 14rpx;
		width: 120rpx;
		background: rgba(206, 230, 161, 0.95);
	}

	.quick-panel {
		margin-top: 8rpx;
		padding: 8rpx;
		background: linear-gradient(180deg, rgba(255, 255, 255, 0.92) 0%, rgba(251, 254, 244, 0.82) 100%);
		border-radius: 28rpx;
		border: 1rpx solid rgba(220, 236, 194, 0.72);
		box-shadow: 0 6rpx 14rpx rgba(31, 40, 28, 0.035);
	}

	.quick-grid {
		display: grid;
		grid-template-columns: repeat(4, minmax(0, 1fr));
		gap: 12rpx;
	}

	.quick-item {
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		gap: 8rpx;
		padding: 12rpx 6rpx 10rpx;
		border-radius: 22rpx;
		background: linear-gradient(180deg, rgba(255, 255, 255, 0.96) 0%, #f8fdeb 100%);
		box-shadow: inset 0 1rpx 0 rgba(255, 255, 255, 0.72);
	}

	.quick-icon {
		display: flex;
		align-items: center;
		justify-content: center;
		width: 62rpx;
		height: 62rpx;
		border-radius: 22rpx;
		box-shadow: inset 0 1rpx 0 rgba(255, 255, 255, 0.7);
		flex-shrink: 0;
	}

	.quick-label {
		font-size: 21rpx;
		font-weight: 700;
		line-height: 1.2;
		color: #273222;
		text-align: center;
	}

	.section-tabs {
		margin-top: 12rpx;
		padding: 0 6rpx;
	}

	.section-tabs :deep(.u-tabs) {
		background: linear-gradient(180deg, rgba(243, 250, 229, 0.82) 0%, rgba(233, 245, 205, 0.72) 100%);
		border-radius: 999rpx;
		padding: 5rpx 8rpx 0;
		box-shadow: inset 0 1rpx 0 rgba(255, 255, 255, 0.68);
	}

	.section-tabs :deep(.u-tabs__wrapper__nav) {
		align-items: center;
		min-height: 68rpx;
	}

	.section-tabs :deep(.u-tabs__wrapper__nav__item) {
		display: flex;
		align-items: center;
		justify-content: center;
		height: 60rpx;
		padding: 0 22rpx;
	}

	.section-tabs :deep(.u-tabs__wrapper__nav__line) {
		height: 6rpx !important;
		border-radius: 999rpx;
	}

	.section-tabs :deep(.u-tabs__wrapper__nav__item__text) {
		font-size: 26rpx;
		line-height: 1;
		white-space: nowrap;
	}

	.state-box {
		padding: 80rpx 28rpx 56rpx;
		text-align: center;
	}

	.state-text {
		font-size: 24rpx;
		color: #8b9782;
	}

	.goods-grid {
		display: grid;
		grid-template-columns: repeat(2, minmax(0, 1fr));
		gap: 14rpx;
		padding: 14rpx 0 calc(24rpx + var(--native-tabbar-offset));
	}

	.goods-card {
		position: relative;
		overflow: hidden;
		border-radius: 24rpx;
		background: linear-gradient(180deg, #ffffff 0%, #fcfffd 100%);
		border: 1rpx solid rgba(229, 236, 232, 0.9);
		box-shadow: 0 8rpx 16rpx rgba(24, 34, 24, 0.045);
	}

	.goods-card::after {
		content: '';
		position: absolute;
		left: 18rpx;
		right: 18rpx;
		bottom: 0;
		height: 20rpx;
		border-radius: 0 0 28rpx 28rpx;
		background: linear-gradient(180deg, rgba(255, 255, 255, 0) 0%, rgba(251, 253, 248, 0.55) 100%);
		pointer-events: none;
	}

	.goods-media {
		position: relative;
		padding: 8rpx 8rpx 0;
	}

	.goods-image {
		width: 100%;
		height: 228rpx;
		display: block;
		background: #edf5f1;
		border-radius: 16rpx;
	}

	.stock-tag,
	.sales-chip {
		position: absolute;
		align-items: center;
		gap: 6rpx;
		padding: 5rpx 10rpx;
		border-radius: 999rpx;
		font-size: 16rpx;
		font-weight: 700;
	}

	.stock-tag {
		left: 20rpx;
		top: 20rpx;
	}

	.in-stock {
		background: rgba(53, 93, 40, 0.88);
		color: #f6fbef;
	}

	.out-stock {
		background: rgba(99, 105, 93, 0.88);
		color: #f8f6ef;
	}

	.sales-chip {
		right: 20rpx;
		bottom: 14rpx;
		background: rgba(18, 28, 18, 0.56);
	}

	.sales-chip-text {
		font-size: 16rpx;
		color: #f7f2e8;
	}

	.goods-body {
		padding: 10rpx 12rpx 14rpx;
	}

	.goods-tag-row {
		flex-wrap: wrap;
		gap: 6rpx;
		min-height: 24rpx;
	}

	.goods-tag {
		padding: 3rpx 8rpx;
		border-radius: 999rpx;
		background: linear-gradient(180deg, #f4fbe8 0%, #ebf7d4 100%);
		font-size: 16rpx;
		color: #6f9127;
	}

	.goods-name {
		margin-top: 6rpx;
		font-size: 28rpx;
		font-weight: 600;
		line-height: 1.34;
		min-height: 60rpx;
		color: #1f291c;
		display: -webkit-box;
		-webkit-line-clamp: 2;
		-webkit-box-orient: vertical;
		overflow: hidden;
	}

	.goods-desc {
		margin-top: 2rpx;
		min-height: 28rpx;
		font-size: 18rpx;
		line-height: 1.4;
		color: #97a286;
		display: -webkit-box;
		-webkit-line-clamp: 1;
		-webkit-box-orient: vertical;
		overflow: hidden;
	}

	.price-row {
		align-items: flex-end;
		justify-content: space-between;
		gap: 10rpx;
		margin-top: 6rpx;
	}

	.goods-price {
		display: inline-block;
		font-size: 28rpx;
		font-weight: 600;
		color: #59b50e;
	}

	.goods-unit {
		display: inline-block;
		margin-left: 4rpx;
		font-size: 16rpx;
		color: #728163;
	}

	.deposit-text {
		font-size: 15rpx;
		color: #b5beaa;
		text-align: right;
	}

	.card-bottom {
		align-items: center;
		justify-content: space-between;
		margin-top: 6rpx;
	}

	.rule-btn {
		padding: 4rpx 8rpx;
		border-radius: 999rpx;
		background: rgba(244, 251, 232, 0.7);
		border: 1rpx solid rgba(211, 231, 171, 0.9);
		font-size: 15rpx;
		color: #92a86a;
	}

	.plus-btn {
		display: flex;
		align-items: center;
		justify-content: center;
		width: 40rpx;
		height: 40rpx;
		border-radius: 50%;
		background: linear-gradient(135deg, #679d2a 0%, #88c13d 100%);
		box-shadow: 0 6rpx 12rpx rgba(95, 145, 39, 0.18);
	}

	@media screen and (max-width: 360px) {
		.banner-swiper,
		.banner-image,
		.fallback-banner {
			height: 232rpx;
		}

		.quick-grid {
			grid-template-columns: repeat(2, minmax(0, 1fr));
		}

		.goods-grid {
			gap: 16rpx;
			grid-template-columns: minmax(0, 1fr);
		}

		.search-text {
			white-space: normal;
		}
	}
</style>
