<template>
	<view class="page">
		<view class="search-card">
			<input v-model="keyword" class="search-input" placeholder="搜索帐篷、天幕、桌椅、炉具" confirm-type="search" @confirm="toList(keyword)" />
			<text class="search-btn" @click="toList(keyword)">搜索</text>
		</view>

		<view class="panel">
			<text class="panel-title">热门搜索</text>
			<view v-if="loadingHot" class="empty-text">加载中...</view>
			<view v-else-if="hotKeywords.length" class="tag-row">
				<text v-for="item in hotKeywords" :key="item" class="tag-pill" @click="toList(item)">{{ item }}</text>
			</view>
			<text v-else class="empty-text">暂无热门搜索</text>
		</view>

		<view class="panel">
			<view class="panel-head">
				<text class="panel-title">历史搜索</text>
				<text v-if="historyKeywords.length" class="clear-btn" @click="clearHistory">清空</text>
			</view>
			<view v-if="historyKeywords.length" class="tag-row">
				<text v-for="item in historyKeywords" :key="item" class="tag-pill tag-pill-gray" @click="toList(item)">{{ item }}</text>
			</view>
			<text v-else class="empty-text">暂无搜索记录</text>
		</view>
	</view>
</template>

<script setup>
	import { ref } from 'vue';
	import { onShow } from '@dcloudio/uni-app';
	import { fetchHotKeywords } from '@/common/api/goods';
	import { addSearchHistory, clearSearchHistory, getSearchHistory } from '@/common/utils/searchHistory';

	const keyword = ref('');
	const hotKeywords = ref([]);
	const historyKeywords = ref([]);
	const loadingHot = ref(false);

	const syncHistory = () => {
		historyKeywords.value = getSearchHistory();
	};

	const loadHotKeywords = async () => {
		loadingHot.value = true;
		try {
			const list = await fetchHotKeywords();
			hotKeywords.value = (list || []).map((item) => item.keyword).filter(Boolean);
		} catch (error) {
			hotKeywords.value = [];
		} finally {
			loadingHot.value = false;
		}
	};

	onShow(() => {
		syncHistory();
		loadHotKeywords();
	});

	const toList = (value) => {
		const next = (value || '').trim();
		if (!next) {
			uni.showToast({ title: '请输入搜索关键词', icon: 'none' });
			return;
		}
		addSearchHistory(next);
		syncHistory();
		uni.navigateTo({
			url: `/packageGoods/list?keyword=${encodeURIComponent(next)}`
		});
	};

	const clearHistory = () => {
		clearSearchHistory();
		syncHistory();
		uni.showToast({ title: '已清空历史搜索', icon: 'none' });
	};
</script>

<style scoped>
	.page {
		min-height: 100vh;
		padding: 24rpx;
		background: #f5f7ff;
	}

	.search-card,
	.panel {
		background: #ffffff;
		border-radius: 28rpx;
		padding: 24rpx;
		margin-bottom: 24rpx;
	}

	.search-card {
		display: flex;
	}

	.search-input {
		flex: 1;
		margin-right: 16rpx;
		height: 76rpx;
		padding: 0 24rpx;
		border-radius: 38rpx;
		background: #f8fafc;
		font-size: 26rpx;
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

	.panel-head {
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	.panel-title,
	.tag-pill,
	.empty-text,
	.clear-btn {
		display: block;
	}

	.panel-title {
		font-size: 30rpx;
		font-weight: 700;
		color: #1f2937;
	}

	.clear-btn {
		font-size: 24rpx;
		color: #9aa2b1;
	}

	.empty-text {
		margin-top: 24rpx;
		font-size: 24rpx;
		color: #9aa2b1;
	}

	.tag-row {
		display: flex;
		flex-wrap: wrap;
		margin-top: 24rpx;
	}

	.tag-pill {
		margin-right: 16rpx;
		margin-bottom: 16rpx;
		padding: 14rpx 24rpx;
		border-radius: 28rpx;
		font-size: 24rpx;
		color: #59b50e;
		background: #f2fde5;
	}

	.tag-pill-gray {
		color: #6b7280;
		background: #f3f4f6;
	}
</style>
