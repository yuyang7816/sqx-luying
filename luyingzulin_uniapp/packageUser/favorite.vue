<template>
	<view class="page">
		<view v-if="loading" class="empty-card">
			<text class="empty-title">加载收藏中...</text>
		</view>
		<view v-else-if="!favoriteList.length" class="empty-card">
			<up-icon name="heart" size="34" color="#a7b19f"></up-icon>
			<text class="empty-title">还没有收藏装备</text>
			<text class="empty-desc">在商品详情页点亮收藏后，这里会集中展示你关注的露营装备。</text>
			<button class="empty-btn" @click="goGoodsList">去逛逛</button>
		</view>

		<view v-for="item in favoriteList" :key="item.favoriteId || item.id" class="fav-card" @click="goDetail(item.id)">
			<image class="fav-image" :src="item.image" mode="aspectFill"></image>
			<view class="fav-main">
				<text class="fav-name">{{ item.name }}</text>
				<view class="price-row"><text class="fav-price">¥{{ item.price }} <text class="unit">/天</text></text><text class="fav-deposit">押金 ¥{{ item.deposit }}</text></view>
				<view class="btn-row">
					<text class="stock-tag" :class="{ off: item.offShelf }">{{ item.offShelf ? '已下架' : '可租' }}</text>
					<text class="add-btn" @click.stop="goDetail(item.id)">查看详情</text>
				</view>
			</view>
			<view class="heart" @click.stop="removeFavorite(item.id)"><up-icon name="heart-fill" size="20" color="#ff4d4f"></up-icon></view>
		</view>
	</view>
</template>

<script setup>
	import { ref } from 'vue';
	import { onShow } from '@dcloudio/uni-app';
	import { fetchFavoriteList, toggleFavorite as toggleFavoriteApi } from '@/common/api/user';
	import { ensureLogin } from '@/common/auth';

	const favoriteList = ref([]);
	const loading = ref(true);

	const syncFavorites = async () => {
		loading.value = true;
		try {
			favoriteList.value = await fetchFavoriteList();
		} catch (error) {
			favoriteList.value = [];
			uni.showToast({ title: error.message || '收藏加载失败', icon: 'none' });
		} finally {
			loading.value = false;
		}
	};

	onShow(() => {
		if (!ensureLogin('/packageUser/favorite')) return;
		syncFavorites();
	});

	const goDetail = (id) => uni.navigateTo({ url: `/packageGoods/detail?id=${id}` });
	const goGoodsList = () => uni.navigateTo({ url: '/packageGoods/list' });

	const removeFavorite = async (id) => {
		try {
			const result = await toggleFavoriteApi(id);
			await syncFavorites();
			uni.showToast({ title: result?.favorite ? '已加入收藏' : '已取消收藏', icon: 'none' });
		} catch (error) {
			uni.showToast({ title: error.message || '操作失败', icon: 'none' });
		}
	};
</script>

<style scoped>
	.page {
		box-sizing: border-box;
		min-height: 100vh;
		padding: 28rpx;
		background: #f6f8f3;
		overflow-x: hidden;
	}

	.empty-card,
	.fav-card {
		box-sizing: border-box;
		width: 100%;
		background: #ffffff;
		border-radius: 32rpx;
		overflow: hidden;
	}

	.empty-title,
	.empty-desc,
	.fav-name,
	.fav-price,
	.fav-deposit,
	.stock-tag,
	.add-btn {
		display: block;
	}

	.empty-card {
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		padding: 120rpx 40rpx;
		margin-bottom: 28rpx;
		box-shadow: 0 6rpx 24rpx rgba(0, 0, 0, 0.04);
	}

	.empty-title {
		margin-top: 24rpx;
		font-size: 30rpx;
		font-weight: 700;
		color: #1a2114;
	}

	.empty-desc {
		margin-top: 14rpx;
		font-size: 24rpx;
		line-height: 1.7;
		text-align: center;
		color: #8a9a7a;
	}

	.empty-btn {
		box-sizing: border-box;
		width: 240rpx;
		height: 84rpx;
		line-height: 84rpx;
		margin-top: 32rpx;
		border-radius: 42rpx;
		font-size: 28rpx;
		font-weight: 600;
		color: #ffffff;
		background: linear-gradient(135deg, #7ed321, #6bc01a);
		border: none;
	}

	.fav-card {
		display: flex;
		align-items: center;
		margin-bottom: 28rpx;
		padding: 28rpx;
		box-shadow: 0 6rpx 24rpx rgba(0, 0, 0, 0.04);
		position: relative;
	}

	.fav-image {
		width: 160rpx;
		height: 160rpx;
		border-radius: 20rpx;
		background: #edf2f7;
		flex-shrink: 0;
	}

	.fav-main {
		flex: 1;
		min-width: 0;
		margin-left: 20rpx;
		margin-right: 16rpx;
	}

	.fav-name {
		font-size: 28rpx;
		font-weight: 700;
		line-height: 1.4;
		color: #1a2114;
		display: -webkit-box;
		-webkit-line-clamp: 2;
		-webkit-box-orient: vertical;
		overflow: hidden;
	}

	.price-row {
		display: flex;
		align-items: baseline;
		min-width: 0;
		margin-top: 14rpx;
	}

	.fav-price {
		font-size: 36rpx;
		font-weight: 800;
		color: #7ed321;
	}

	.unit {
		font-size: 22rpx;
		color: #8a9a7a;
		font-weight: 500;
	}

	.fav-deposit {
		margin-left: 14rpx;
		white-space: nowrap;
		font-size: 22rpx;
		color: #8a9a7a;
		font-weight: 500;
	}

	.btn-row {
		display: flex;
		align-items: center;
		flex-wrap: wrap;
		gap: 16rpx;
		margin-top: 16rpx;
	}

	.stock-tag {
		padding: 8rpx 18rpx;
		border-radius: 16rpx;
		font-size: 20rpx;
		color: #5cb816;
		background: #f6ffea;
		border: 1rpx solid rgba(126, 211, 33, 0.15);
		font-weight: 600;
	}

	.stock-tag.off {
		color: #9aa2b1;
		background: #f5f7fb;
		border-color: #eef2f7;
	}

	.add-btn {
		padding: 8rpx 24rpx;
		border-radius: 24rpx;
		font-size: 22rpx;
		font-weight: 600;
		color: #ffffff;
		background: linear-gradient(135deg, #7ed321, #6bc01a);
	}

	.heart {
		display: flex;
		align-items: center;
		justify-content: center;
		width: 56rpx;
		height: 56rpx;
		border-radius: 50%;
		background: #fff0f0;
		flex-shrink: 0;
	}
</style>
