<template>
	<view v-if="loading" class="page state-page">
		<text class="state-text">加载中...</text>
	</view>
	<view v-else-if="!goods" class="page state-page">
		<text class="state-text">商品不存在或已下架</text>
	</view>
	<view v-else class="page">
		<view class="detail-scroll">
			<view class="hero-shell">
				<swiper class="hero-swiper" circular indicator-dots indicator-color="rgba(255,255,255,0.34)"
					indicator-active-color="#ffffff">
					<swiper-item v-for="(img, index) in displayImages" :key="`${img}-${index}`" @click="preview(index)">
						<image class="hero-image" :src="img" mode="aspectFill"></image>
					</swiper-item>
				</swiper>
				<view class="hero-overlay">
					<text class="hero-badge"
						:class="stockDisabled ? 'hero-badge-off' : 'hero-badge-on'">{{ stockDisabled ? '缺货' : '可租' }}</text>
					<text class="hero-count">{{ displayImages.length }} 张图</text>
				</view>
			</view>

			<view class="content-sheet">
				<view class="summary-head">
					<!-- <view v-if="(goods.tags || []).length" class="summary-tags">
						<text v-for="tag in (goods.tags || []).slice(0, 3)" :key="tag" class="summary-tag">{{ tag }}</text>
					</view> -->
					<text class="goods-name">{{ goods.name }}</text>
					<text class="goods-desc">{{ goods.subtitle }}</text>
					<text v-if="stockDisabled" class="stock-tip">当前暂无库存，暂不可预订</text>
				</view>

				<view class="price-panel">
					<view class="price-main">
						<text class="price-label">租金</text>
						<view class="price-value-row">
							<text class="price-symbol">¥</text>
							<text class="price">{{ currentPriceValue }}</text>
							<text class="price-unit">/{{ currentPriceUnit }}</text>
						</view>
						<text class="price-hint">{{ currentRentModeDesc }}</text>
					</view>
					<view class="price-side">
						<text class="price-side-label">押金</text>
						<text class="price-note">¥{{ currentDeposit }}</text>
					</view>
				</view>

				<!-- <view class="meta-row">
					<view class="meta-pill">
						<up-icon name="checkmark-circle" size="14" color="#6fb322"></up-icon>
						<text class="meta-text">库存 {{ Number(goods.availableStock || goods.totalStock || 0) }}</text>
					</view>
					<view class="meta-pill">
						<up-icon name="clock" size="14" color="#6fb322"></up-icon>
						<text class="meta-text">{{ currentRentModeDesc }}</text>
					</view>
				</view> -->

				<view class="section-block first-block">
					<view class="block-head">
						<text class="block-title">租赁时长</text>
						<text class="block-caption">按当前模式计费</text>
					</view>
					<view class="choice-grid">
						<view v-for="item in rentModes" :key="item.key" class="choice-card"
							:class="{ active: rentMode === item.key }" @click="handleRentModeChange(item)">
							<text class="choice-title">{{ item.label }}</text>
							<text class="choice-desc">{{ item.desc }}</text>
						</view>
					</view>
				</view>

				<view class="section-block detail-block">
					<view class="block-head detail-head">
						<text class="detail-title">详情介绍</text>
						<text class="block-caption">商品参数与图文说明</text>
					</view>
					<view v-if="paramRows.length" class="spec-list">
						<view v-for="param in paramRows" :key="param.name" class="spec-row">
							<text class="spec-name">{{ param.name }}</text>
							<text class="spec-value">{{ param.value }}</text>
						</view>
					</view>
					<text class="detail-text">{{ goods.detail || '暂无详情描述' }}</text>
				</view>
			</view>
		</view>

		<view class="bottom-bar">
			<view class="mini-entry" @click="toggleFavorite">
				<view class="mini-icon"><up-icon :name="isFavorite ? 'heart-fill' : 'heart'" size="20"
						:color="isFavorite ? '#ff4d4f' : '#7ed321'"></up-icon></view>
				<text class="mini-text">{{ isFavorite ? '已收藏' : '收藏' }}</text>
			</view>
			<view class="mini-entry" @click="contactMerchant">
				<view class="mini-icon"><up-icon name="kefu-ermai" size="20" color="#7ed321"></up-icon></view>
				<text class="mini-text">咨询</text>
			</view>
			<button class="cart-btn" :disabled="stockDisabled"
				@click="handleCartClick">{{ stockDisabled ? '暂无库存' : '加入租物车' }}</button>
			<button class="order-btn" :disabled="stockDisabled"
				@click="handleConfirmClick">{{ stockDisabled ? '暂无库存' : '立即预订' }}</button>
		</view>

		<GoodsOptionSheet :visible="optionSheetVisible" :goods="goods" :rent-mode="rentMode" :spec-index="specIndex"
			:package-index="packageIndex" :action="optionSheetAction" @close="optionSheetVisible = false"
			@confirm="handleOptionConfirm" />
	</view>
</template>

<script setup>
	import {
		computed,
		ref
	} from 'vue';
	import {
		onLoad
	} from '@dcloudio/uni-app';
	import {
		fetchGoodsDetail
	} from '@/common/api/goods';
	import GoodsOptionSheet from '@/components/GoodsOptionSheet.vue';
	import {
		quickAddToCart
	} from '@/common/rental/cartActions';
	import {
		checkFavorite,
		toggleFavorite as toggleFavoriteApi
	} from '@/common/api/user';
	import {
		ensureLogin,
		isLoggedIn
	} from '@/common/auth';
	import {
		buildDetailRentModes,
		buildGoodsParamRows,
		buildRentSpecOptions,
		calculateDetailTotalPrice,
		getRentPriceUnit,
		goodsRequiresSpecSelection,
		isGoodsAvailableForCart,
		resolveDefaultDetailRentMode,
		resolveSelectedRentSpec
	} from '@/common/rental/constants';

	const goods = ref(null);
	const loading = ref(true);
	const rentMode = ref('day');
	const specIndex = ref(0);
	const packageIndex = ref(0);
	const optionSheetVisible = ref(false);
	const optionSheetAction = ref('cart');
	const isFavorite = ref(false);
	const goodsId = ref('');

	const rentModes = computed(() => buildDetailRentModes(goods.value));

	const specOptions = computed(() => buildRentSpecOptions(goods.value));

	const paramRows = computed(() => buildGoodsParamRows(goods.value));

	const packageOptions = computed(() => {
		const list = goods.value?.packages || [];
		return list.map((item, index) => ({
			index,
			id: item.id,
			label: item.packageName,
			desc: item.packageDesc,
			price: item.price
		}));
	});

	const hasSelectableOptions = computed(() => specOptions.value.length > 0 || packageOptions.value.length > 0);

	const selectedSpec = computed(() => resolveSelectedRentSpec(goods.value, specIndex.value));
	const currentRentModeDesc = computed(() => rentModes.value.find((item) => item.key === rentMode.value)?.desc || '24小时计费');
	const selectedPackage = computed(() => packageOptions.value[packageIndex.value] || null);
	const displayImages = computed(() => {
		if (!goods.value) return [];
		if (goods.value.images?.length) return goods.value.images;
		return goods.value.coverImage ? [goods.value.coverImage] : [];
	});
	const stockDisabled = computed(() => !isGoodsAvailableForCart(goods.value));

	const currentPriceUnit = computed(() => {
		const mode = rentModes.value.find((item) => item.key === rentMode.value);
		return getRentPriceUnit(mode?.apiRentType || 'day');
	});
	const currentPriceValue = computed(() => {
		if (!goods.value) return '0';
		const specItem = hasSelectableOptions.value ? null : selectedSpec.value;
		const packageItem = hasSelectableOptions.value ? null : selectedPackage.value;
		return calculateDetailTotalPrice(goods.value, rentMode.value, packageItem, specItem);
	});
	const currentDeposit = computed(() => {
		if (!goods.value) return '0';
		return Number(goods.value.depositAmount || 0).toFixed(2);
	});

	const loadDetail = async (id) => {
		loading.value = true;
		try {
			const detail = await fetchGoodsDetail(id);
			goods.value = detail;
			rentMode.value = resolveDefaultDetailRentMode(detail);
			specIndex.value = 0;
			packageIndex.value = 0;
			optionSheetVisible.value = false;
			if (isLoggedIn()) {
				try {
					isFavorite.value = Boolean(await checkFavorite(detail.id, {
						silentError: true
					}));
				} catch (error) {
					isFavorite.value = false;
				}
			} else {
				isFavorite.value = false;
			}
		} catch (error) {
			goods.value = null;
			uni.showToast({
				title: '商品加载失败',
				icon: 'none'
			});
		} finally {
			loading.value = false;
		}
	};

	onLoad((options) => {
		goodsId.value = options.id || '';
		if (!goodsId.value) {
			loading.value = false;
			return;
		}
		loadDetail(goodsId.value);
	});

	const preview = (index) => {
		if (!displayImages.value.length) return;
		uni.previewImage({
			current: index,
			urls: displayImages.value
		});
	};

	const goPage = (url) => uni.navigateTo({
		url
	});

	const contactMerchant = () => {
		if (!goods.value?.merchantId) {
			uni.showToast({
				title: '暂无商家信息',
				icon: 'none'
			});
			return;
		}
		const merchantName = encodeURIComponent(goods.value.merchantName || goods.value.name || '商家');
		const redirect = `/packageGoods/detail?id=${goods.value.id}`;
		if (!ensureLogin(redirect)) return;
		uni.navigateTo({
			url: `/packageContent/im-chat?merchantId=${goods.value.merchantId}&merchantName=${merchantName}`
		});
	};

	const handleRentModeChange = (item) => {
		rentMode.value = item.key;
	};

	const openOptionSheet = (action) => {
		if (!goods.value || stockDisabled.value) return;
		optionSheetAction.value = action;
		optionSheetVisible.value = true;
	};

	const handleCartClick = () => {
		if (!goods.value || stockDisabled.value) return;
		if (hasSelectableOptions.value) {
			openOptionSheet('cart');
			return;
		}
		goCart();
	};

	const handleConfirmClick = () => {
		if (!goods.value || stockDisabled.value) return;
		if (hasSelectableOptions.value) {
			openOptionSheet('buy');
			return;
		}
		goConfirm();
	};

	const handleOptionConfirm = ({
		specIndex: nextSpecIndex,
		packageIndex: nextPackageIndex,
		spec,
		packageItem
	}) => {
		specIndex.value = nextSpecIndex;
		packageIndex.value = nextPackageIndex;
		optionSheetVisible.value = false;
		const resolvedPackageId = packageItem?.id
			?? packageOptions.value[nextPackageIndex]?.id
			?? 0;
		if (optionSheetAction.value === 'buy') {
			goConfirm();
			return;
		}
		goCart({
			specId: spec?.id || 0,
			packageId: resolvedPackageId
		});
	};

	const toggleFavorite = async () => {
		if (!goods.value) return;
		if (!ensureLogin(`/packageGoods/detail?id=${goods.value.id}`)) return;
		try {
			const result = await toggleFavoriteApi(goods.value.id);
			isFavorite.value = Boolean(result?.favorite);
			uni.showToast({
				title: result?.favorite ? '已加入收藏' : '已取消收藏',
				icon: 'none'
			});
		} catch (error) {
			uni.showToast({
				title: error.message || '收藏操作失败',
				icon: 'none'
			});
		}
	};

	const goCart = (selection = {}) => {
		if (!goods.value || stockDisabled.value) return;
		const specId = selection.specId ?? selectedSpec.value?.id ?? 0;
		const packageId = selection.packageId ?? selectedPackage.value?.id ?? 0;
		if (goodsRequiresSpecSelection(goods.value) && !specId) {
			openOptionSheet('cart');
			return;
		}
		if (packageOptions.value.length && !packageId) {
			openOptionSheet('cart');
			return;
		}
		quickAddToCart({
			goods: goods.value,
			rentModeKey: rentMode.value,
			specId,
			packageId,
			loginRedirect: `/packageGoods/detail?id=${goods.value.id}`
		});
	};

	const goConfirm = () => {
		if (!goods.value || stockDisabled.value) return;
		if (goodsRequiresSpecSelection(goods.value) && !selectedSpec.value?.id) {
			openOptionSheet('buy');
			return;
		}
		if (!ensureLogin(`/packageGoods/detail?id=${goods.value.id}`)) return;
		const specLabel = selectedSpec.value?.label || '默认规格';
		const packageLabel = selectedPackage.value?.label || '标准套餐';
		const packagePrice = Number(selectedPackage.value?.price || 0);
		const specPrice = Number(selectedSpec.value?.price || 0);
		const query = [
			`goodsId=${goods.value.id}`,
			`rentMode=${rentMode.value}`,
			`specId=${selectedSpec.value?.id || 0}`,
			`specLabel=${encodeURIComponent(specLabel)}`,
			`specPrice=${specPrice}`,
			`packageId=${selectedPackage.value?.id || ''}`,
			`packagePrice=${packagePrice}`,
			`packageLabel=${encodeURIComponent(packageLabel)}`,
			`depositAmount=${currentDeposit.value}`
		].join('&');
		uni.navigateTo({
			url: `/packageOrder/confirm?${query}`
		});
	};
</script>

<style scoped>
	.page {
		min-height: 100vh;
		background: linear-gradient(180deg, #f4fbe9 0%, #f9fdf3 22%, #ffffff 52%, #ffffff 100%);
		overflow-x: hidden;
	}

	.state-page {
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.state-text {
		font-size: 28rpx;
		color: #90a17b;
	}

	.detail-scroll {
		padding-bottom: 44rpx;
	}

	.hero-shell {
		position: relative;
		/* padding-top: calc(12rpx + env(safe-area-inset-top)); */
	}

	.hero-shell::after {
		content: '';
		position: absolute;
		left: 0;
		right: 0;
		bottom: 0;
		height: 160rpx;
		background: linear-gradient(180deg, rgba(15, 20, 12, 0) 0%, rgba(15, 20, 12, 0.08) 52%, rgba(15, 20, 12, 0.22) 100%);
		pointer-events: none;
	}

	.hero-swiper {
		height: 560rpx;
		border-radius: 0 0 40rpx 40rpx;
		overflow: hidden;
	}

	.hero-image {
		width: 100%;
		height: 100%;
		display: block;
	}

	.hero-overlay {
		position: absolute;
		left: 28rpx;
		right: 28rpx;
		bottom: 42rpx;
		display: flex;
		align-items: center;
		justify-content: space-between;
		pointer-events: none;
		z-index: 2;
	}

	.hero-badge,
	.hero-count,
	.summary-tag,
	.meta-pill,
	.summary-tags,
	.price-panel,
	.price-value-row,
	.meta-row,
	.block-head,
	.choice-grid,
	.bottom-bar,
	.mini-entry,
	.mini-icon {
		display: flex;
		align-items: center;
	}

	.hero-badge,
	.hero-count {
		padding: 10rpx 18rpx;
		border-radius: 999rpx;
		font-size: 22rpx;
		font-weight: 700;
		letter-spacing: 0;
		backdrop-filter: blur(12rpx);
		box-shadow: 0 6rpx 16rpx rgba(12, 18, 10, 0.16);
	}

	.hero-badge-on {
		background: rgba(39, 66, 25, 0.9);
		color: #ffffff;
	}

	.hero-badge-off {
		background: rgba(88, 92, 82, 0.88);
		color: #ffffff;
	}

	.hero-count {
		justify-content: center;
		color: #ffffff;
		background: rgba(18, 24, 17, 0.62);
	}

	.content-sheet {
		position: relative;
		margin-top: -28rpx;
		padding: 26rpx 24rpx calc(180rpx + env(safe-area-inset-bottom));
		background: linear-gradient(180deg, #ffffff 0%, #fcfff8 100%);
		border-radius: 34rpx 34rpx 0 0;
		border-top: 1rpx solid rgba(223, 236, 201, 0.78);
		box-shadow: 0 -8rpx 20rpx rgba(35, 48, 27, 0.03);
		z-index: 2;
	}

	.content-sheet::before {
		content: '';
		display: block;
		width: 92rpx;
		height: 8rpx;
		margin: 0 auto 18rpx;
		border-radius: 999rpx;
		background: rgba(205, 226, 165, 0.9);
	}

	.summary-head {
		padding: 2rpx 0 20rpx;
	}

	.summary-tags {
		flex-wrap: wrap;
		gap: 10rpx;
	}

	.goods-name,
	.goods-desc,
	.stock-tip,
	.price-label,
	.price-unit,
	.price-hint,
	.price-note,
	.price-side-label,
	.meta-text,
	.block-title,
	.block-caption,
	.choice-title,
	.choice-desc,
	.choice-extra,
	.option-sub,
	.detail-title,
	.spec-name,
	.spec-value,
	.detail-text,
	.mini-text,
	.price-symbol {
		display: block;
	}

	.summary-tag {
		justify-content: center;
		padding: 8rpx 16rpx;
		border-radius: 999rpx;
		font-size: 20rpx;
		color: #6e9123;
		background: linear-gradient(180deg, #f4fbe8 0%, #eaf7d0 100%);
	}

	.goods-name {
		margin-top: 14rpx;
		font-size: 40rpx;
		font-weight: 800;
		line-height: 1.28;
		color: #1a2114;
	}

	.goods-desc {
		margin-top: 10rpx;
		font-size: 25rpx;
		line-height: 1.5;
		color: #819274;
	}

	.stock-tip {
		margin-top: 12rpx;
		font-size: 22rpx;
		color: #ef4444;
	}

	.price-panel {
		position: relative;
		overflow: hidden;
		align-items: flex-end;
		justify-content: space-between;
		gap: 14rpx;
		margin-top: 20rpx;
		padding: 26rpx 24rpx 20rpx;
		border-radius: 26rpx;
		background: linear-gradient(135deg, #fcfff3 0%, #f2fbdd 52%, #f8fde9 100%);
		border: 1rpx solid rgba(220, 236, 188, 0.95);
		box-shadow: inset 0 1rpx 0 rgba(255, 255, 255, 0.84), 0 10rpx 22rpx rgba(88, 117, 43, 0.06);
	}

	.price-panel::before {
		content: '';
		position: absolute;
		top: -42rpx;
		right: -18rpx;
		width: 180rpx;
		height: 180rpx;
		border-radius: 50%;
		background: radial-gradient(circle, rgba(255, 255, 255, 0.72) 0%, rgba(255, 255, 255, 0) 72%);
		pointer-events: none;
	}

	.price-panel::after {
		content: '';
		position: absolute;
		left: 24rpx;
		right: 24rpx;
		bottom: 0;
		height: 8rpx;
		border-radius: 999rpx;
		background: linear-gradient(90deg, rgba(126, 211, 33, 0) 0%, rgba(126, 211, 33, 0.18) 22%, rgba(126, 211, 33, 0.26) 50%, rgba(126, 211, 33, 0.08) 100%);
	}

	.price-main,
	.price-side {
		position: relative;
		z-index: 1;
	}

	.price-main {
		flex: 1;
		min-width: 0;
	}

	.price-label,
	.price-side-label,
	.block-caption {
		font-size: 20rpx;
		color: #96a586;
	}

	.price-value-row {
		align-items: flex-end;
		gap: 4rpx;
		margin-top: 6rpx;
	}

	.price-symbol {
		font-size: 30rpx;
		font-weight: 800;
		line-height: 1;
		color: #5fa317;
	}

	.price {
		display: block;
		font-size: 64rpx;
		font-weight: 800;
		line-height: 1;
		color: #63aa17;
	}

	.price-unit {
		font-size: 24rpx;
		line-height: 1.2;
		color: #76925c;
	}

	.price-hint {
		margin-top: 10rpx;
		font-size: 22rpx;
		color: #8ea073;
	}

	.price-side {
		width: 178rpx;
		flex-shrink: 0;
		padding-left: 18rpx;
		border-left: 1rpx solid rgba(150, 165, 134, 0.18);
		text-align: right;
	}

	.price-note {
		margin-top: 8rpx;
		font-size: 36rpx;
		font-weight: 700;
		color: #26331c;
	}

	.meta-row {
		flex-wrap: wrap;
		gap: 12rpx;
		margin-top: 16rpx;
	}

	.meta-pill {
		gap: 8rpx;
		padding: 10rpx 16rpx;
		border-radius: 999rpx;
		background: linear-gradient(180deg, #f6fbe9 0%, #eef8da 100%);
		box-shadow: inset 0 1rpx 0 rgba(255, 255, 255, 0.76);
	}

	.meta-text {
		font-size: 20rpx;
		color: #73865d;
	}

	.section-block {
		margin-top: 28rpx;
		padding-top: 28rpx;
		border-top: 1rpx solid #f1f4ec;
	}

	.first-block {
		margin-top: 0;
		padding-top: 0;
		border-top: none;
	}

	.block-title {
		font-size: 28rpx;
		font-weight: 600;
		color: #1a2114;
	}

	.block-head {
		margin-top: 25rpx;
		margin-bottom: 25rpx;
		align-items: baseline;
		justify-content: space-between;
		gap: 18rpx;
		padding-left: 4rpx;
		border-left: 6rpx solid #d7ecab;
	}

	.choice-grid {
		flex-wrap: wrap;
		align-items: stretch;
		gap: 14rpx;
		margin-top: 18rpx;
	}

	.package-scroll {
		width: 100%;
		margin-top: 18rpx;
		white-space: nowrap;
	}

	.package-scroll-inner {
		display: inline-flex;
		align-items: stretch;
		gap: 12rpx;
		padding: 2rpx 4rpx 6rpx;
	}

	.package-card {
		width: 280rpx;
		flex-shrink: 0;
		white-space: normal;
	}

	.choice-card {
		box-sizing: border-box;
		width: calc((100% - 28rpx) / 3);
		min-width: 0;
		min-height: 124rpx;
		padding: 18rpx 14rpx 16rpx;
		border-radius: 22rpx;
		background: linear-gradient(180deg, #ffffff 0%, #fbfef4 100%);
		border: 1rpx solid rgba(226, 235, 209, 0.92);
		box-shadow: 0 6rpx 14rpx rgba(32, 43, 24, 0.03);
		transition: border-color 0.2s ease, box-shadow 0.2s ease, background 0.2s ease;
	}

	.choice-card.active {
		background: linear-gradient(180deg, #f6fce7 0%, #ecf8d1 100%);
		border-color: rgba(126, 211, 33, 0.5);
		box-shadow: 0 10rpx 20rpx rgba(126, 211, 33, 0.14), inset 0 1rpx 0 rgba(255, 255, 255, 0.72);
	}

	.choice-title {
		font-size: 24rpx;
		font-weight: 700;
		line-height: 1.25;
		color: #22301a;
	}

	.choice-desc {
		margin-top: 8rpx;
		font-size: 20rpx;
		line-height: 1.35;
		color: #8d9d7f;
	}

	.choice-extra {
		margin-top: 10rpx;
		font-size: 20rpx;
		font-weight: 700;
		color: #5da316;
	}

	.option-sub {
		margin-top: 14rpx;
		padding: 14rpx 18rpx;
		border-radius: 18rpx;
		background: linear-gradient(180deg, #f8fceb 0%, #f1f9de 100%);
		border: 1rpx solid rgba(220, 234, 193, 0.72);
		font-size: 22rpx;
		line-height: 1.5;
		color: #6f8552;
	}

	.detail-block {
		padding-bottom: 8rpx;
	}

	.detail-head {
		margin-bottom: 16rpx;
	}

	.detail-title {
		font-size: 28rpx;
		font-weight: 600;
		color: #1a2114;
	}

	.spec-list {
		border: 1rpx solid rgba(233, 238, 224, 0.92);
		border-radius: 20rpx;
		overflow: hidden;
		background: linear-gradient(180deg, #ffffff 0%, #fbfef5 100%);
		box-shadow: 0 6rpx 18rpx rgba(38, 51, 28, 0.03);
	}

	.spec-row {
		display: flex;
		justify-content: space-between;
		align-items: flex-start;
		padding: 22rpx 18rpx;
		border-bottom: 1rpx solid #f1f4ec;
	}

	.spec-name {
		font-size: 24rpx;
		color: #8fa07c;
		flex-shrink: 0;
		margin-right: 24rpx;
	}

	.spec-value {
		flex: 1;
		min-width: 0;
		font-size: 24rpx;
		font-weight: 500;
		text-align: right;
		word-break: break-all;
		color: #21301a;
	}

	.detail-text {
		margin-top: 24rpx;
		padding: 24rpx 22rpx;
		border-radius: 22rpx;
		background: linear-gradient(180deg, #fbfef5 0%, #f7fbe9 100%);
		border: 1rpx solid rgba(230, 238, 214, 0.92);
		box-shadow: inset 0 1rpx 0 rgba(255, 255, 255, 0.8);
		font-size: 26rpx;
		line-height: 1.78;
		color: #4d5c3f;
	}

	.bottom-bar {
		position: fixed;
		left: 24rpx;
		right: 24rpx;
		bottom: calc(18rpx + env(safe-area-inset-bottom));
		padding: 16rpx 18rpx;
		background: rgba(255, 255, 255, 0.94);
		border: 1rpx solid rgba(223, 236, 201, 0.92);
		border-radius: 30rpx;
		box-shadow: 0 10rpx 24rpx rgba(27, 35, 23, 0.08);
		backdrop-filter: blur(18rpx);
		z-index: 100;
	}

	.mini-entry {
		width: 88rpx;
		margin-right: 12rpx;
		flex-direction: column;
		justify-content: center;
		padding: 6rpx 0;
		border-radius: 18rpx;
	}

	.mini-icon {
		justify-content: center;
		width: 52rpx;
		height: 52rpx;
		border-radius: 50%;
		background: linear-gradient(180deg, #f5fbe9 0%, #edf7d8 100%);
	}

	.mini-text {
		margin-top: 6rpx;
		font-size: 20rpx;
		font-weight: 500;
		color: #8b9e73;
	}

	.cart-btn,
	.order-btn {
		flex: 1;
		min-width: 0;
		height: 80rpx;
		line-height: 80rpx;
		margin: 0;
		border-radius: 40rpx;
		font-size: 28rpx;
		font-weight: 700;
	}

	.cart-btn {
		margin-right: 14rpx;
		color: #67a91c;
		background: linear-gradient(180deg, #f7fdeb 0%, #eff9dd 100%);
		border: 2rpx solid rgba(126, 211, 33, 0.22);
		box-shadow: inset 0 1rpx 0 rgba(255, 255, 255, 0.8);
	}

	.order-btn {
		color: #ffffff;
		background: linear-gradient(135deg, #7ed321, #6bc01a);
		box-shadow: 0 8rpx 18rpx rgba(126, 211, 33, 0.28);
		border: none;
	}

	.cart-btn[disabled],
	.order-btn[disabled] {
		color: #9aa2b1;
		background: #e5e7eb;
		box-shadow: none;
		border-color: #e5e7eb;
	}
</style>