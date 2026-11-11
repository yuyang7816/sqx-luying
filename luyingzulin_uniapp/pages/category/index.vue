<template>
	<view class="page">
		<view class="top-bar">
			<view class="city-pill" @click="goCitySelect">
				<up-icon name="map" size="16" color="#6f7e63"></up-icon>
				<text class="city-text">{{ cityName }}</text>
				<up-icon name="arrow-down" size="12" color="#8aa076"></up-icon>
			</view>
			<view class="search-box" @click="goSearch">
				<up-icon name="search" size="16" color="#aab592"></up-icon>
				<text class="search-text">搜索分类商品...</text>
			</view>
		</view>

		<view class="content">
			<scroll-view scroll-y class="side-scroll">
				<view v-for="item in sideCategories" :key="item.id" class="side-item" :class="{ active: activeCategoryId === item.id }" @click="selectCategory(item.id)">
					<text class="side-text">{{ item.name }}</text>
				</view>
			</scroll-view>

			<view class="main-panel">
				<view class="sort-row">
					<text v-for="item in sorts" :key="item.label" class="sort-item" :class="{ active: activeSortBy === item.sortBy }" @click="changeSort(item.sortBy)">{{ item.label }}</text>
					<text class="filter-btn" @click="openFilter">筛选</text>
				</view>

				<scroll-view scroll-y class="goods-scroll">
					<view v-if="loading && !goodsList.length" class="state-box"><text class="state-text">加载中...</text></view>
					<view v-else-if="!goodsList.length" class="state-box"><text class="state-text">该分类暂无商品</text></view>
					<view v-for="item in goodsList" v-else :key="item.id" class="goods-row" @click="goDetail(item.id)">
						<image class="goods-image" :src="item.coverImage" mode="aspectFill"></image>
						<view class="goods-main">
							<text class="goods-name">{{ item.name }}</text>
							<!-- <view class="tag-wrap">
								<text v-for="tag in (item.tags || []).slice(0, 2)" :key="tag" class="tag green">{{ tag }}</text>
							</view> -->
							<text class="goods-price">日租价 <text class="price-value">¥{{ formatPrice(item).price }}</text></text>
						</view>
						<!-- <view class="plus-btn" @click.stop="handleAddCart(item)"><up-icon name="plus" size="20" color="#ffffff"></up-icon></view> -->
					</view>
				</scroll-view>
			</view>
		</view>
	</view>
</template>

<script setup>
	import { ref } from 'vue';
	import { onLoad, onShow, onPullDownRefresh } from '@dcloudio/uni-app';
	import { resolveAppCity } from '@/common/api/content';
	import { getAppCity, getLocatedCity, getSelectedCity, locateCurrentCity } from '@/common/city';
	import { fetchCategoryTree, fetchGoodsPage } from '@/common/api/rental';
	import { quickAddToCart } from '@/common/rental/cartActions';
	import {
		CATEGORY_SORT_OPTIONS,
		formatGoodsRentPrice,
		mapApiRentTypeToDetailMode
	} from '@/common/rental/constants';

	const cityName = ref(getAppCity() || '定位中...');
	const activeCity = ref(getAppCity());
	const loading = ref(false);
	const sideCategories = ref([]);
	const goodsList = ref([]);
	const activeCategoryId = ref(null);
	const activeSortBy = ref('default');
	const activeRentType = ref('');
	const pendingCategoryId = ref(null);
	const cartLoading = ref(false);

	const sorts = CATEGORY_SORT_OPTIONS;
	const formatPrice = (item) => formatGoodsRentPrice(item, activeRentType.value);

	const loadCategories = async () => {
		const tree = await fetchCategoryTree();
		sideCategories.value = (tree || []).filter((item) => !item.parentId || Number(item.parentId) === 0);
		if (!sideCategories.value.length) return;
		const targetId = pendingCategoryId.value || activeCategoryId.value || sideCategories.value[0].id;
		const matched = sideCategories.value.find((item) => Number(item.id) === Number(targetId));
		activeCategoryId.value = matched ? matched.id : sideCategories.value[0].id;
		pendingCategoryId.value = null;
	};

	const loadGoods = async () => {
		if (!activeCategoryId.value) return;
		loading.value = true;
		try {
			const { list } = await fetchGoodsPage({
				page: 1,
				limit: 20,
				categoryId: activeCategoryId.value,
				sortBy: activeSortBy.value,
				rentType: activeRentType.value || undefined,
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
			await loadCategories();
			await loadGoods();
		} catch (error) {
			uni.showToast({ title: '刷新失败，请稍后重试', icon: 'none' });
		} finally {
			uni.stopPullDownRefresh();
		}
	};

	onLoad((query) => {
		if (query?.categoryId) {
			pendingCategoryId.value = Number(query.categoryId);
		}
	});

	onShow(() => {
		const storedCategoryId = uni.getStorageSync('pendingCategoryId');
		if (storedCategoryId) {
			pendingCategoryId.value = Number(storedCategoryId);
			uni.removeStorageSync('pendingCategoryId');
		}
		refreshPage();
	});

	onPullDownRefresh(() => {
		refreshPage();
	});

	const selectCategory = (categoryId) => {
		activeCategoryId.value = categoryId;
		loadGoods();
	};

	const changeSort = (sortBy) => {
		activeSortBy.value = sortBy;
		loadGoods();
	};

	const openFilter = () => {
		uni.showActionSheet({
			itemList: ['全部租赁模式', '小时租', '整日租', '过夜租'],
			success: ({ tapIndex }) => {
				const rentTypes = ['', 'hour', 'day', 'overnight'];
				activeRentType.value = rentTypes[tapIndex] || '';
				loadGoods();
			}
		});
	};

	const goSearch = () => uni.navigateTo({ url: '/packageSearch/index' });
	const goCitySelect = () => uni.navigateTo({ url: '/packageContent/city-select' });
	const goDetail = (id) => uni.navigateTo({ url: `/packageGoods/detail?id=${id}` });
	const handleAddCart = (item) => quickAddToCart({
		goods: item,
		rentModeKey: mapApiRentTypeToDetailMode(activeRentType.value),
		loadingRef: cartLoading,
		loginRedirect: '/pages/cart/index'
	});
</script>

<style scoped>
	.page {
		box-sizing: border-box;
		height: 100vh;
		background: #f6f8f3;
		overflow-x: hidden;
	}

	.top-bar {
		display: flex;
		align-items: center;
		gap: 16rpx;
		padding: 24rpx 28rpx;
		background: #ffffff;
		box-shadow: 0 2rpx 16rpx rgba(126, 211, 33, 0.06);
		position: relative;
		z-index: 10;
	}

	.city-pill,
	.search-box {
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
	.search-text {
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
		box-sizing: border-box;
		flex: 1;
		min-width: 0;
		height: 72rpx;
		padding: 0 24rpx;
		border-radius: 36rpx;
		background: #f1f4ec;
	}

	.search-text {
		flex: 1;
		min-width: 0;
		font-size: 24rpx;
		color: #9aa58a;
	}

	.content {
		display: flex;
		height: calc(100vh - 120rpx - var(--native-tabbar-offset));
	}

	.side-scroll {
		width: 180rpx;
		height: 100%;
		background: #eef2e8;
	}

	.side-item {
		position: relative;
		margin: 8rpx 12rpx 8rpx 0;
		padding: 28rpx 16rpx;
		text-align: center;
		border-radius: 0 20rpx 20rpx 0;
	}

	.side-item.active {
		background: #ffffff;
		box-shadow: 2rpx 2rpx 12rpx rgba(0, 0, 0, 0.05);
	}

	.side-item.active::before {
		content: '';
		position: absolute;
		left: 0;
		top: 50%;
		transform: translateY(-50%);
		width: 5rpx;
		height: 28rpx;
		border-radius: 0 4rpx 4rpx 0;
		background: linear-gradient(180deg, #7ed321, #5cb816);
	}

	.side-text {
		display: block;
		font-size: 26rpx;
		line-height: 1.4;
		color: #6b7a5e;
		font-weight: 500;
	}

	.side-item.active .side-text {
		color: #2d3a1e;
		font-weight: 700;
	}

	.main-panel {
		box-sizing: border-box;
		flex: 1;
		min-width: 0;
		padding: 20rpx 24rpx 0;
		overflow: hidden;
		background: #f6f8f3;
		display: flex;
		flex-direction: column;
	}

	.sort-row {
		display: flex;
		align-items: center;
		margin-bottom: 20rpx;
		padding: 4rpx 0;
		flex-shrink: 0;
	}

	.sort-item {
		margin-right: 16rpx;
		padding: 10rpx 24rpx;
		border-radius: 28rpx;
		font-size: 24rpx;
		color: #6b7a5e;
		background: #ffffff;
		font-weight: 500;
	}

	.sort-item.active {
		color: #ffffff;
		background: linear-gradient(135deg, #7ed321, #6bc01a);
		font-weight: 600;
	}

	.filter-btn {
		margin-left: auto;
		padding: 10rpx 24rpx;
		border-radius: 28rpx;
		font-size: 24rpx;
		color: #4a5540;
		background: #ffffff;
		font-weight: 500;
	}

	.goods-scroll {
		flex: 1;
		height: auto;
	}

	.state-box {
		padding: 80rpx 0;
		text-align: center;
	}

	.state-text {
		display: block;
		font-size: 26rpx;
		color: #8a9a7a;
	}

	.goods-row {
		box-sizing: border-box;
		display: flex;
		align-items: center;
		margin-bottom: 24rpx;
		padding: 20rpx;
		background: #ffffff;
		border-radius: 24rpx;
		box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
	}

	.goods-image {
		width: 180rpx;
		height: 180rpx;
		border-radius: 20rpx;
		background: #edf2f7;
		flex-shrink: 0;
	}

	.goods-main {
		flex: 1;
		min-width: 0;
		height: 180rpx;
		display: flex;
		flex-direction: column;
		justify-content: space-between;
		margin-left: 20rpx;
		margin-right: 16rpx;
	}

	.goods-name,
	.goods-price,
	.tag {
		display: block;
	}

	.goods-name {
		font-size: 28rpx;
		font-weight: 700;
		line-height: 1.4;
		color: #1a2114;
		display: -webkit-box;
		-webkit-line-clamp: 2;
		-webkit-box-orient: vertical;
		overflow: hidden;
	}

	.tag-wrap {
		display: flex;
		flex-wrap: wrap;
		margin-top: 12rpx;
		gap: 8rpx;
	}

	.tag {
		padding: 6rpx 14rpx;
		border-radius: 12rpx;
		font-size: 20rpx;
		font-weight: 500;
	}

	.tag.green {
		color: #5cb816;
		background: #eefadb;
	}

	.goods-price {
		font-size: 22rpx;
		color: #8a9a7a;
	}

	.price-value {
		font-size: 36rpx;
		font-weight: 800;
		color: #7ed321;
		margin-left: 8rpx;
	}

	.plus-btn {
		display: flex;
		align-items: center;
		justify-content: center;
		width: 60rpx;
		height: 60rpx;
		border-radius: 50%;
		background: linear-gradient(135deg, #7ed321, #6bc01a);
		flex-shrink: 0;
	}
</style>
