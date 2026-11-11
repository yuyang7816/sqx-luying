<template>
	<view class="page">
		<view class="search-panel">
			<input v-model="keyword" class="search-input" placeholder="搜索装备名称或卖点" confirm-type="search" @confirm="applyFilter" />
			<text class="search-btn" @click="applyFilter">搜索</text>
		</view>

		<scroll-view scroll-x class="category-scroll" show-scrollbar="false">
			<view class="category-row">
				<text v-for="item in categoryTabs" :key="item.id" class="category-pill" :class="{ active: activeCategoryId === item.id }" @click="changeCategory(item.id)">{{ item.name }}</text>
			</view>
		</scroll-view>

		<view class="sort-row">
			<u-tabs :list="sortTabs" keyName="label" :current="currentSortIndex" :scrollable="true" lineColor="#7ed321" :activeStyle="tabActiveStyle" :inactiveStyle="tabInactiveStyle" @change="handleSortChange"></u-tabs>
			<text class="filter-btn" @click="openRentFilter">{{ rentFilterLabel }}</text>
		</view>

		<view class="list-body">
			<view v-if="loading && !goodsList.length" class="empty-wrap">
				<text class="empty-title">加载中...</text>
			</view>
			<view v-else-if="goodsList.length" class="goods-list">
				<view v-for="item in goodsList" :key="item.id" class="goods-card" @click="goDetail(item.id)">
					<image class="goods-image" :src="item.coverImage" mode="aspectFill"></image>
					<view class="goods-body">
						<text class="goods-name">{{ item.name }}</text>
						<text class="goods-desc">{{ item.subtitle }}</text>
						<view class="tag-row">
							<text v-for="tag in (item.tags || []).slice(0, 3)" :key="tag" class="goods-tag">{{ tag }}</text>
							<text v-if="item.availableStock <= 0" class="goods-tag stock-out">缺货</text>
						</view>
						<view class="bottom-row">
							<view>
								<text class="goods-price">¥{{ formatPrice(item).price }}</text>
								<text class="goods-unit">/{{ formatPrice(item).unit }}</text>
							</view>
							<button class="reserve-btn" :disabled="item.availableStock <= 0" @click.stop="goDetail(item.id)">{{ item.availableStock > 0 ? '立即预订' : '暂无库存' }}</button>
						</view>
					</view>
				</view>
				<view v-if="loadingMore" class="load-more"><text class="load-more-text">加载中...</text></view>
				<view v-else-if="finished" class="load-more"><text class="load-more-text">没有更多了</text></view>
			</view>
			<view v-else class="empty-wrap">
				<text class="empty-title">没有找到对应装备</text>
				<text class="empty-desc">换个关键词，或者切换到其他分类试试。</text>
			</view>
		</view>
	</view>
</template>

<script setup>
	import { computed, ref } from 'vue';
	import { onLoad, onPullDownRefresh, onReachBottom } from '@dcloudio/uni-app';
	import { fetchCategoryTree, fetchGoodsPage } from '@/common/api/rental';
	import { addSearchHistory } from '@/common/utils/searchHistory';
	import { GOODS_LIST_SORT_OPTIONS, formatGoodsRentPrice } from '@/common/rental/constants';

	const ALL_CATEGORY = { id: 0, name: '全部' };
	const PAGE_SIZE = 10;

	const keyword = ref('');
	const searchedKeyword = ref('');
	const categoryTabs = ref([ALL_CATEGORY]);
	const activeCategoryId = ref(0);
	const activeSortBy = ref('default');
	const activeRentType = ref('');
	const goodsList = ref([]);
	const page = ref(1);
	const total = ref(0);
	const loading = ref(false);
	const loadingMore = ref(false);
	const finished = ref(false);
	const pendingCategoryId = ref(null);
	const pendingCategoryName = ref('');

	const sortTabs = GOODS_LIST_SORT_OPTIONS;
	const tabActiveStyle = { color: '#59b50e', fontWeight: '700' };
	const tabInactiveStyle = { color: '#6b7280' };
	const currentSortIndex = computed(() => Math.max(sortTabs.findIndex((item) => item.sortBy === activeSortBy.value), 0));
	const rentFilterLabel = computed(() => {
		if (activeRentType.value === 'hour') return '小时租';
		if (activeRentType.value === 'day') return '整日租';
		if (activeRentType.value === 'overnight') return '过夜租';
		return '筛选';
	});
	const formatPrice = (item) => formatGoodsRentPrice(item, activeRentType.value);

	const resolveCategoryIdByName = (name) => {
		const target = String(name || '').trim();
		if (!target || target === '全部') return 0;
		const matched = categoryTabs.value.find((item) => item.name === target);
		return matched ? matched.id : null;
	};

	const loadCategories = async () => {
		const tree = await fetchCategoryTree();
		const topList = (tree || []).filter((item) => !item.parentId || Number(item.parentId) === 0);
		categoryTabs.value = [ALL_CATEGORY, ...topList];
		if (pendingCategoryId.value != null) {
			const matched = categoryTabs.value.find((item) => Number(item.id) === Number(pendingCategoryId.value));
			activeCategoryId.value = matched ? matched.id : 0;
			pendingCategoryId.value = null;
		} else if (pendingCategoryName.value) {
			const resolved = resolveCategoryIdByName(pendingCategoryName.value);
			activeCategoryId.value = resolved != null ? resolved : 0;
			pendingCategoryName.value = '';
		}
	};

	const fetchList = async ({ reset = false } = {}) => {
		if (reset) {
			page.value = 1;
			finished.value = false;
			goodsList.value = [];
		}
		if (finished.value && !reset) return;
		const isFirstPage = page.value === 1;
		if (isFirstPage) {
			loading.value = true;
		} else {
			loadingMore.value = true;
		}
		try {
			const { list, total: nextTotal } = await fetchGoodsPage({
				page: page.value,
				limit: PAGE_SIZE,
				categoryId: activeCategoryId.value || undefined,
				keyword: searchedKeyword.value || undefined,
				sortBy: activeSortBy.value,
				rentType: activeRentType.value || undefined
			});
			const records = list || [];
			total.value = Number(nextTotal || 0);
			goodsList.value = reset ? records : goodsList.value.concat(records);
			finished.value = goodsList.value.length >= total.value || records.length < PAGE_SIZE;
			if (!finished.value) {
				page.value += 1;
			}
		} catch (error) {
			if (reset) goodsList.value = [];
			uni.showToast({ title: '加载失败，请稍后重试', icon: 'none' });
		} finally {
			loading.value = false;
			loadingMore.value = false;
			uni.stopPullDownRefresh();
		}
	};

	const refreshPage = async () => {
		await loadCategories();
		await fetchList({ reset: true });
	};

	onLoad((options) => {
		if (options.categoryId) {
			pendingCategoryId.value = Number(options.categoryId);
		}
		if (options.cate) {
			pendingCategoryName.value = decodeURIComponent(options.cate);
		}
		if (options.keyword) {
			keyword.value = decodeURIComponent(options.keyword);
			searchedKeyword.value = keyword.value.trim();
		}
		refreshPage();
	});

	onPullDownRefresh(() => {
		refreshPage();
	});

	const loadMore = () => {
		if (loading.value || loadingMore.value || finished.value) return;
		fetchList();
	};

	onReachBottom(loadMore);

	const changeCategory = (categoryId) => {
		activeCategoryId.value = categoryId;
		fetchList({ reset: true });
	};

	const handleSortChange = (item) => {
		activeSortBy.value = item.sortBy;
		fetchList({ reset: true });
	};

	const openRentFilter = () => {
		uni.showActionSheet({
			itemList: ['全部租赁模式', '小时租', '整日租', '过夜租'],
			success: ({ tapIndex }) => {
				const rentTypes = ['', 'hour', 'day', 'overnight'];
				activeRentType.value = rentTypes[tapIndex] || '';
				fetchList({ reset: true });
			}
		});
	};

	const applyFilter = () => {
		const next = keyword.value.trim();
		searchedKeyword.value = next;
		if (next) {
			addSearchHistory(next);
		}
		fetchList({ reset: true });
	};

	const goDetail = (id) => {
		uni.navigateTo({
			url: `/packageGoods/detail?id=${id}`
		});
	};
</script>

<style scoped>
	.page {
		box-sizing: border-box;
		min-height: 100vh;
		background: #f5f7ff;
		overflow-x: hidden;
	}

	.search-panel {
		display: flex;
		align-items: center;
		padding: 24rpx;
	}

	.search-input {
		flex: 1;
		min-width: 0;
		margin-right: 18rpx;
		height: 76rpx;
		padding: 0 24rpx;
		border-radius: 38rpx;
		background: #ffffff;
		font-size: 26rpx;
		color: #1f2937;
	}

	.search-btn {
		padding: 0 28rpx;
		height: 76rpx;
		line-height: 76rpx;
		border-radius: 38rpx;
		font-size: 26rpx;
		color: #ffffff;
		background: linear-gradient(135deg, #7ed321, #59b50e);
	}

	.category-scroll,
	.sort-row {
		white-space: nowrap;
	}

	.category-row {
		display: inline-flex;
		padding: 0 24rpx 16rpx;
	}

	.category-pill {
		display: inline-block;
		margin-right: 16rpx;
		padding: 14rpx 24rpx;
		border-radius: 32rpx;
		font-size: 24rpx;
		color: #6b7280;
		background: #ffffff;
	}

	.category-pill.active {
		color: #59b50e;
		background: #f2fde5;
	}

	.sort-row {
		display: flex;
		align-items: center;
		padding: 0 24rpx 24rpx;
	}

	.sort-row :deep(.u-tabs) {
		flex: 1;
		min-width: 0;
	}

	.sort-row :deep(.u-tabs__wrapper__nav__item) {
		padding: 0 28rpx;
	}

	.sort-row :deep(.u-tabs__wrapper__nav__item__text) {
		font-size: 24rpx;
		white-space: nowrap;
	}

	.filter-btn {
		flex-shrink: 0;
		margin-left: 12rpx;
		padding: 10rpx 20rpx;
		border-radius: 28rpx;
		font-size: 24rpx;
		color: #4a5540;
		background: #ffffff;
	}

	.list-body {
		padding-bottom: 24rpx;
	}

	.goods-list {
		padding: 0 24rpx 24rpx;
	}

	.goods-card {
		box-sizing: border-box;
		display: flex;
		width: 100%;
		margin-bottom: 24rpx;
		background: #ffffff;
		border-radius: 28rpx;
		padding: 20rpx;
		overflow: hidden;
	}

	.goods-image {
		width: 220rpx;
		height: 220rpx;
		border-radius: 22rpx;
		background: #edf2f7;
	}

	.goods-body {
		flex: 1;
		min-width: 0;
		margin-left: 18rpx;
	}

	.goods-name,
	.goods-desc,
	.goods-tag,
	.goods-price,
	.goods-unit,
	.empty-title,
	.empty-desc,
	.load-more-text {
		display: block;
	}

	.goods-name {
		font-size: 28rpx;
		font-weight: 700;
		line-height: 1.5;
		color: #1f2937;
	}

	.goods-desc {
		margin-top: 10rpx;
		font-size: 22rpx;
		color: #6b7280;
	}

	.tag-row {
		display: flex;
		flex-wrap: wrap;
		margin-top: 16rpx;
	}

	.goods-tag {
		margin-right: 10rpx;
		margin-bottom: 10rpx;
		padding: 6rpx 14rpx;
		border-radius: 18rpx;
		font-size: 20rpx;
		color: #59b50e;
		background: #f2fde5;
	}

	.goods-tag.stock-out {
		color: #ef4444;
		background: #fef2f2;
	}

	.bottom-row {
		display: flex;
		align-items: flex-end;
		justify-content: space-between;
		gap: 16rpx;
		margin-top: 24rpx;
	}

	.bottom-row > view {
		flex: 1;
		min-width: 0;
	}

	.goods-price {
		font-size: 34rpx;
		font-weight: 700;
		color: #59b50e;
	}

	.goods-unit {
		font-size: 22rpx;
		color: #6b7280;
	}

	.reserve-btn {
		margin: 0;
		min-width: 0;
		flex-shrink: 0;
		width: auto;
		padding: 0 24rpx;
		height: 68rpx;
		line-height: 68rpx;
		border-radius: 34rpx;
		font-size: 24rpx;
		color: #ffffff;
		background: linear-gradient(135deg, #7ed321, #59b50e);
	}

	.reserve-btn[disabled] {
		color: #9aa2b1;
		background: #e5e7eb;
	}

	.load-more {
		padding: 16rpx 0 8rpx;
		text-align: center;
	}

	.load-more-text {
		font-size: 24rpx;
		color: #9aa2b1;
	}

	.empty-wrap {
		padding-top: 240rpx;
		text-align: center;
	}

	.empty-title {
		font-size: 32rpx;
		font-weight: 700;
		color: #1f2937;
	}

	.empty-desc {
		margin-top: 14rpx;
		font-size: 24rpx;
		color: #6b7280;
	}
</style>
