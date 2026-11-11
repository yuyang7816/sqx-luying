<template>
	<view class="page">
		<view class="locate-card">
			<text class="section-label">定位城市</text>
			<view class="locate-row" @click="handleRelocate">
				<view class="locate-main">
					<up-icon name="map-fill" size="18" color="#7ed321"></up-icon>
					<text class="locate-city">{{ displayCity }}</text>
				</view>
				<text class="locate-action">重新定位</text>
			</view>
		</view>

		<view class="scroll-shell">
			<scroll-view
				class="city-scroll"
				scroll-y
				:scroll-into-view="scrollIntoView"
				scroll-with-animation
				:show-scrollbar="false"
			>
				<view id="anchor-hot" class="section-card">
					<text class="section-title">热门城市</text>
					<view class="hot-grid">
						<view
							v-for="name in hotCityNames"
							:key="name"
							class="hot-item"
							@click="finishSelect(name)"
						>
							<text class="hot-item-text">{{ name }}</text>
						</view>
					</view>
				</view>

				<view
					v-for="group in letterGroups"
					:key="group.letter"
					:id="'anchor-' + group.letter"
					class="section-card letter-section"
				>
					<text class="letter-title">{{ group.letter }}</text>
					<view
						v-for="city in group.cities"
						:key="city"
						class="city-row"
						@click="finishSelect(city)"
					>
						<text class="city-name">{{ city }}</text>
					</view>
				</view>
				<view class="scroll-bottom"></view>
			</scroll-view>

			<view class="index-bar">
				<view class="index-item hot-index" @click="scrollTo('hot')">🔥</view>
				<view
					v-for="letter in letters"
					:key="letter"
					class="index-item"
					@click="scrollTo(letter)"
				>
					{{ letter }}
				</view>
			</view>
		</view>
	</view>
</template>

<script setup>
	import { computed, nextTick, ref } from 'vue';
	import { onShow } from '@dcloudio/uni-app';
	import {
		CITY_ENTRIES,
		HOT_CITY_NAMES,
		getLocatedCity,
		getSelectedCity,
		locateCurrentCity,
		setSelectedCity
	} from '@/common/city';

	const hotCityNames = HOT_CITY_NAMES;
	const letters = [...new Set(CITY_ENTRIES.map((item) => item.letter))].sort();
	const letterGroups = letters.map((letter) => ({
		letter,
		cities: CITY_ENTRIES.filter((item) => item.letter === letter).map((item) => item.name)
	}));

	const currentCity = ref('');
	const locating = ref(false);
	const scrollIntoView = ref('');

	const displayCity = computed(() => {
		if (locating.value) return '定位中...';
		return currentCity.value || '点击重新定位';
	});

	const syncCurrentCity = async () => {
		const stored = getSelectedCity();
		if (stored) {
			currentCity.value = stored;
			return;
		}

		const cachedLocated = getLocatedCity();
		if (cachedLocated) {
			currentCity.value = cachedLocated;
			return;
		}

		try {
			currentCity.value = await locateCurrentCity();
		} catch (error) {
			currentCity.value = '定位失败';
		}
	};

	onShow(() => {
		syncCurrentCity();
	});

	const finishSelect = (cityName) => {
		const normalized = setSelectedCity(cityName);
		if (!normalized) return;
		uni.navigateBack({
			fail: () => {
				uni.switchTab({ url: '/pages/index/index' });
			}
		});
	};

	const scrollTo = (key) => {
		scrollIntoView.value = '';
		nextTick(() => {
			scrollIntoView.value = `anchor-${key}`;
		});
	};

	const handleRelocate = async () => {
		if (locating.value) return;
		locating.value = true;
		try {
			const cityName = await locateCurrentCity({ force: true, useCache: false });
			currentCity.value = cityName;
			finishSelect(cityName);
		} catch (error) {
			uni.showToast({ title: '定位失败，请手动选择', icon: 'none' });
		} finally {
			locating.value = false;
		}
	};
</script>

<style scoped>
	.page {
		box-sizing: border-box;
		display: flex;
		flex-direction: column;
		height: 100vh;
		background: #f5f7ff;
		overflow: hidden;
	}

	.locate-card {
		box-sizing: border-box;
		margin: 24rpx 24rpx 0;
		padding: 28rpx;
		background: #ffffff;
		border-radius: 28rpx;
		box-shadow: 0 8rpx 24rpx rgba(36, 53, 30, 0.04);
	}

	.section-label {
		display: block;
		font-size: 24rpx;
		color: #71806a;
	}

	.locate-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-top: 20rpx;
		padding: 22rpx 24rpx;
		border-radius: 20rpx;
		background: linear-gradient(135deg, #f7fbe8 0%, #eef8dc 100%);
		border: 1rpx solid #dfeecf;
	}

	.locate-main {
		display: flex;
		align-items: center;
		min-width: 0;
		flex: 1;
	}

	.locate-city {
		margin-left: 12rpx;
		font-size: 32rpx;
		font-weight: 700;
		color: #24351e;
	}

	.locate-action {
		flex-shrink: 0;
		margin-left: 16rpx;
		font-size: 24rpx;
		color: #7ed321;
		font-weight: 600;
	}

	.scroll-shell {
		position: relative;
		flex: 1;
		min-height: 0;
		margin-top: 24rpx;
	}

	.city-scroll {
		box-sizing: border-box;
		height: 100%;
		padding: 0 56rpx 0 24rpx;
	}

	.section-card {
		box-sizing: border-box;
		margin-bottom: 24rpx;
		padding: 28rpx;
		background: #ffffff;
		border-radius: 28rpx;
		box-shadow: 0 8rpx 24rpx rgba(36, 53, 30, 0.04);
	}

	.section-title,
	.letter-title,
	.city-name,
	.hot-item-text {
		display: block;
	}

	.section-title {
		font-size: 30rpx;
		font-weight: 700;
		color: #24351e;
	}

	.hot-grid {
		display: flex;
		flex-wrap: wrap;
		margin-top: 20rpx;
	}

	.hot-item {
		box-sizing: border-box;
		width: 25%;
		padding: 10rpx 8rpx;
	}

	.hot-item-text {
		padding: 18rpx 0;
		text-align: center;
		font-size: 26rpx;
		color: #24351e;
		background: #f4f8ef;
		border-radius: 16rpx;
		border: 1rpx solid #e3ecd8;
	}

	.letter-section {
		padding-top: 16rpx;
		padding-bottom: 8rpx;
	}

	.letter-title {
		padding-bottom: 8rpx;
		font-size: 28rpx;
		font-weight: 700;
		color: #7ed321;
	}

	.city-row {
		padding: 24rpx 0;
		border-bottom: 1rpx solid #eef2ea;
	}

	.city-row:last-child {
		border-bottom: none;
	}

	.city-name {
		font-size: 28rpx;
		color: #24351e;
	}

	.scroll-bottom {
		height: 40rpx;
	}

	.index-bar {
		position: absolute;
		top: 50%;
		right: 8rpx;
		z-index: 10;
		display: flex;
		flex-direction: column;
		align-items: center;
		transform: translateY(-50%);
		padding: 8rpx 0;
		border-radius: 999rpx;
		background: rgba(255, 255, 255, 0.92);
		box-shadow: 0 4rpx 16rpx rgba(36, 53, 30, 0.08);
	}

	.index-item {
		width: 34rpx;
		line-height: 28rpx;
		text-align: center;
		font-size: 18rpx;
		color: #71806a;
	}

	.hot-index {
		font-size: 16rpx;
		line-height: 24rpx;
	}
</style>
