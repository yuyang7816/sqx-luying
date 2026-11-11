<template>
	<view class="page">
		<view v-if="isAuthed && (validItems.length || invalidItems.length)" class="action-bar">
			<text class="manage" @click="toggleManage">{{ manageMode ? '完成' : '管理' }}</text>
		</view>

		<view v-if="!isAuthed" class="empty-panel">
			<up-icon name="shopping-cart" size="56" color="#a7b19f"></up-icon>
			<text class="empty-title">登录后查看租物车</text>
			<text class="empty-desc">加购、结算需先登录账号</text>
			<button class="login-btn" @click="goLogin">立即登录</button>
		</view>

		<view v-else class="cart-body-wrap">
			<view v-if="loading" class="loading-panel"><text class="loading-text">加载中...</text></view>
			<view v-else class="list-body" :class="{ 'has-bottom-bar': validItems.length }">
				<view v-if="!validItems.length && !invalidItems.length" class="empty-panel in-scroll">
					<up-icon name="shopping-cart" size="56" color="#a7b19f"></up-icon>
					<text class="empty-title">租物车还是空的</text>
					<text class="empty-desc">去首页挑选露营装备吧</text>
					<button class="login-btn" @click="goHome">去逛逛</button>
				</view>

				<view v-for="item in validItems" :key="item.id" class="cart-card">
					<view class="selector" :class="{ active: item.selected }" @click="toggleSelect(item)">
						<up-icon :name="item.selected ? 'checkmark-circle-fill' : 'checkmark-circle'" size="18" :color="item.selected ? '#7ed321' : '#c2c8d3'"></up-icon>
					</view>
					<image class="cart-image" :src="item.image" mode="aspectFill"></image>
					<view class="cart-body">
						<view class="cart-info">
							<text class="cart-name">{{ item.name }}</text>
							<text v-if="item.spec" class="cart-spec">{{ item.spec }}</text>
							<view class="cart-price-row">
								<text class="price-symbol">¥</text>
								<text class="price-value">{{ formatItemPrice(item.price) }}</text>
								<text class="price-unit">/{{ rentUnit(item.rentType) }}</text>
							</view>
						</view>
						<view v-if="!manageMode" class="stepper">
							<view class="step-btn" @click="changeCount(item, -1)"><up-icon name="minus" size="16" color="#5f6775"></up-icon></view>
							<text class="step-value">{{ item.count }}</text>
							<view class="step-btn" @click="changeCount(item, 1)"><up-icon name="plus" size="16" color="#5f6775"></up-icon></view>
						</view>
						<view v-else class="delete-btn" @click="removeItem(item)">删除</view>
					</view>
				</view>

				<view v-if="invalidItems.length" class="invalid-section">
					<text class="invalid-title">失效商品</text>
					<view v-for="item in invalidItems" :key="`invalid-${item.id}`" class="cart-card invalid">
						<view class="selector disabled"><up-icon name="close-circle" size="18" color="#d0d5dd"></up-icon></view>
						<image class="cart-image" :src="item.image" mode="aspectFill"></image>
						<view class="cart-body">
							<view class="cart-info">
								<text class="cart-name">{{ item.name }}</text>
								<text v-if="item.spec" class="cart-spec">{{ item.spec }}</text>
								<text class="invalid-reason">{{ item.invalidReason || '商品已失效' }}</text>
							</view>
							<view class="delete-btn" @click="removeItem(item)">删除</view>
						</view>
					</view>
				</view>
			</view>

			<view v-if="validItems.length" class="bottom-bar">
				<view class="left-row" @click="toggleSelectAll">
					<view class="selector small" :class="{ active: allSelected }">
						<up-icon :name="allSelected ? 'checkmark-circle-fill' : 'checkmark-circle'" size="18" :color="allSelected ? '#7ed321' : '#c2c8d3'"></up-icon>
					</view>
					<text class="all-label">全选</text>
				</view>
				<view class="sum-box">
					<view class="sum-main">
						<text class="sum-label">合计</text>
						<text class="sum-price">¥{{ totalPrice }}</text>
					</view>
					<text class="sum-hint">不含押金，租期以结算页为准</text>
				</view>
				<button v-if="manageMode" class="submit-btn danger" :disabled="!selectedIds.length" @click="removeSelected">删除</button>
				<button v-else class="submit-btn" :disabled="checkoutLoading || !selectedIds.length" :loading="checkoutLoading" @click="goConfirm">立即结算</button>
			</view>
		</view>
	</view>
</template>

<script setup>
	import { computed, ref } from 'vue';
	import { onShow, onPullDownRefresh } from '@dcloudio/uni-app';
	import {
		checkoutPreview,
		deleteCartBatch,
		deleteCartItem,
		fetchCartList,
		selectCartItems,
		updateCartItem
	} from '@/common/api/cart';
	import { ensureLogin, isLoggedIn } from '@/common/auth';
	import { getRentPriceUnit, mapCartItemToView, calcCartSelectedTotal } from '@/common/rental/constants';
	import { setPendingConfirmPayload } from '@/common/rental/confirmStorage';

	const isAuthed = ref(false);
	const loading = ref(false);
	const checkoutLoading = ref(false);
	const manageMode = ref(false);
	const cartItems = ref([]);

	const validItems = computed(() => cartItems.value.filter((item) => !item.invalid));
	const invalidItems = computed(() => cartItems.value.filter((item) => item.invalid));
	const selectedIds = computed(() => validItems.value.filter((item) => item.selected).map((item) => item.id));
	const allSelected = computed(() => validItems.value.length > 0 && validItems.value.every((item) => item.selected));
	const totalPrice = computed(() => calcCartSelectedTotal(validItems.value));

	const rentUnit = (rentType) => getRentPriceUnit(rentType);
	const formatItemPrice = (price) => {
		const num = Number(price || 0);
		return num % 1 === 0 ? num.toFixed(0) : num.toFixed(1);
	};

	const loadCart = async () => {
		if (!isAuthed.value) {
			cartItems.value = [];
			return;
		}
		loading.value = true;
		try {
			const list = await fetchCartList();
			cartItems.value = (list || []).map(mapCartItemToView);
		} catch (error) {
			cartItems.value = [];
		} finally {
			loading.value = false;
		}
	};

	const syncPage = async () => {
		isAuthed.value = isLoggedIn();
		await loadCart();
	};

	onShow(() => {
		syncPage().finally(() => uni.stopPullDownRefresh());
	});

	onPullDownRefresh(() => {
		syncPage();
	});

	const toggleManage = () => {
		manageMode.value = !manageMode.value;
	};

	const toggleSelect = async (item) => {
		if (item.invalid) return;
		try {
			await selectCartItems({ id: item.id, selected: !item.selected });
			item.selected = !item.selected;
		} catch (error) {
			// toast 已由 http 层处理
		}
	};

	const toggleSelectAll = async () => {
		if (!validItems.value.length) return;
		const next = !allSelected.value;
		try {
			await selectCartItems({ selectAll: true, selected: next });
			cartItems.value = cartItems.value.map((item) => item.invalid ? item : { ...item, selected: next });
		} catch (error) {
			// ignore
		}
	};

	const changeCount = async (item, step) => {
		const nextQty = Math.max(1, item.count + step);
		if (nextQty === item.count) return;
		try {
			await updateCartItem({ id: item.id, qty: nextQty });
			item.count = nextQty;
			item.itemRentAmount = Number((item.price * nextQty).toFixed(2));
		} catch (error) {
			await loadCart();
		}
	};

	const removeItem = async (item) => {
		try {
			await deleteCartItem(item.id);
			cartItems.value = cartItems.value.filter((row) => row.id !== item.id);
		} catch (error) {
			await loadCart();
		}
	};

	const removeSelected = async () => {
		if (!selectedIds.value.length) {
			uni.showToast({ title: '请先选择商品', icon: 'none' });
			return;
		}
		try {
			await deleteCartBatch(selectedIds.value);
			cartItems.value = cartItems.value.filter((item) => !selectedIds.value.includes(item.id));
			manageMode.value = false;
		} catch (error) {
			await loadCart();
		}
	};

	const goLogin = () => {
		uni.navigateTo({ url: '/packageUser/login?redirect=' + encodeURIComponent('/pages/cart/index') });
	};

	const goHome = () => {
		uni.switchTab({ url: '/pages/index/index' });
	};

	const goConfirm = async () => {
		if (!ensureLogin('/pages/cart/index')) return;
		if (!selectedIds.value.length) {
			uni.showToast({ title: '请先选择商品', icon: 'none' });
			return;
		}
		if (checkoutLoading.value) return;
		checkoutLoading.value = true;
		try {
			const preview = await checkoutPreview({
				cartIds: selectedIds.value,
				pickupType: 'store',
				pickupStoreId: 1
			});
			setPendingConfirmPayload({
				source: 'cart',
				preview,
				cartItems: validItems.value.filter((item) => selectedIds.value.includes(item.id))
			});
			uni.navigateTo({ url: '/packageOrder/confirm?source=cart' });
		} catch (error) {
			// http 层已 toast
		} finally {
			checkoutLoading.value = false;
		}
	};
</script>

<style scoped>
	.page {
		box-sizing: border-box;
		min-height: 100vh;
		background: #f6f8f3;
		padding-bottom: calc(132rpx + var(--native-tabbar-offset));
	}

	.action-bar {
		display: flex;
		justify-content: flex-end;
		align-items: center;
		flex-shrink: 0;
		padding: 16rpx 28rpx;
		background: #ffffff;
		border-bottom: 1rpx solid #eef2e8;
	}

	.cart-body-wrap {
		min-height: calc(100vh - var(--native-tabbar-offset));
	}

	.manage, .cart-name, .cart-spec, .all-label, .sum-label, .sum-hint, .sum-price, .empty-title, .empty-desc, .invalid-title, .invalid-reason, .loading-text {
		display: block;
	}

	.manage {
		font-size: 28rpx;
		color: #7ed321;
		font-weight: 600;
	}

	.loading-panel {
		flex: 1;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.loading-text {
		font-size: 26rpx;
		color: #8a9a7a;
	}

	.empty-panel {
		flex: 1;
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		padding: 80rpx 48rpx;
	}

	.empty-panel.in-scroll {
		min-height: 100%;
	}

	.empty-title {
		margin-top: 28rpx;
		font-size: 32rpx;
		font-weight: 700;
		color: #1a2114;
	}

	.empty-desc {
		margin-top: 12rpx;
		font-size: 24rpx;
		color: #8a9a7a;
	}

	.login-btn {
		margin-top: 36rpx;
		padding: 0 48rpx;
		height: 80rpx;
		line-height: 80rpx;
		border-radius: 40rpx;
		font-size: 28rpx;
		color: #ffffff;
		background: linear-gradient(135deg, #7ed321, #6bc01a);
		border: none;
	}

	.list-body {
		box-sizing: border-box;
		width: 100%;
		padding: 24rpx 28rpx;
	}

	.list-body.has-bottom-bar {
		padding-bottom: 24rpx;
	}

	.invalid-section {
		margin-top: 12rpx;
	}

	.invalid-title {
		margin-bottom: 16rpx;
		font-size: 26rpx;
		font-weight: 700;
		color: #8a9a7a;
	}

	.cart-card {
		box-sizing: border-box;
		display: flex;
		align-items: center;
		margin-bottom: 20rpx;
		padding: 24rpx;
		background: #ffffff;
		border-radius: 24rpx;
		box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.04);
	}

	.cart-card.invalid {
		opacity: 0.72;
	}

	.selector {
		display: flex;
		align-items: center;
		justify-content: center;
		width: 44rpx;
		height: 44rpx;
		margin-right: 16rpx;
		flex-shrink: 0;
		align-self: center;
	}

	.selector.small {
		margin-right: 0;
	}

	.selector.disabled {
		opacity: 0.5;
	}

	.cart-image {
		width: 160rpx;
		height: 160rpx;
		border-radius: 16rpx;
		background: #f1f4ec;
		flex-shrink: 0;
	}

	.cart-body {
		flex: 1;
		min-width: 0;
		margin-left: 20rpx;
		display: flex;
		align-items: flex-end;
		justify-content: space-between;
		gap: 12rpx;
		min-height: 160rpx;
	}

	.cart-info {
		flex: 1;
		min-width: 0;
		display: flex;
		flex-direction: column;
		justify-content: space-between;
		min-height: 160rpx;
	}

	.cart-name {
		font-size: 28rpx;
		font-weight: 600;
		line-height: 1.45;
		color: #1a2114;
		overflow: hidden;
		text-overflow: ellipsis;
		display: -webkit-box;
		-webkit-line-clamp: 2;
		-webkit-box-orient: vertical;
	}

	.cart-spec {
		margin-top: 8rpx;
		font-size: 22rpx;
		line-height: 1.4;
		color: #9aa58a;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.cart-price-row {
		display: flex;
		align-items: baseline;
		margin-top: auto;
		padding-top: 16rpx;
	}

	.price-symbol {
		font-size: 22rpx;
		font-weight: 600;
		color: #59b50e;
	}

	.price-value {
		font-size: 34rpx;
		font-weight: 700;
		line-height: 1;
		color: #59b50e;
	}

	.price-unit {
		margin-left: 4rpx;
		font-size: 22rpx;
		color: #9aa58a;
	}

	.invalid-reason {
		margin-top: auto;
		padding-top: 16rpx;
		font-size: 22rpx;
		line-height: 1.4;
		color: #ff7a45;
	}

	.stepper {
		display: flex;
		align-items: center;
		padding: 6rpx;
		border-radius: 32rpx;
		background: #f1f4ec;
		flex-shrink: 0;
	}

	.step-btn {
		display: flex;
		align-items: center;
		justify-content: center;
		width: 48rpx;
		height: 48rpx;
		border-radius: 50%;
		background: #ffffff;
	}

	.step-value {
		min-width: 56rpx;
		height: 48rpx;
		line-height: 48rpx;
		text-align: center;
		font-size: 28rpx;
		font-weight: 600;
		color: #2d3a1e;
	}

	.delete-btn {
		flex-shrink: 0;
		padding: 0 20rpx;
		height: 56rpx;
		line-height: 56rpx;
		border-radius: 28rpx;
		font-size: 24rpx;
		color: #ff6b57;
		background: #fff1ee;
	}

	.bottom-bar {
		box-sizing: border-box;
		display: flex;
		align-items: center;
		position: fixed;
		left: 0;
		right: 0;
		bottom: 0;
		z-index: 20;
		padding: 20rpx 28rpx;
		background: #ffffff;
		border-top: 1rpx solid #eef2e8;
		box-shadow: 0 -4rpx 24rpx rgba(0, 0, 0, 0.04);
	}

	/* #ifdef H5 */
	.bottom-bar {
		bottom: var(--native-tabbar-offset);
	}
	/* #endif */

	.left-row {
		display: flex;
		align-items: center;
		flex-shrink: 0;
		margin-right: 20rpx;
	}

	.all-label {
		margin-left: 8rpx;
		font-size: 26rpx;
		color: #4a5540;
		white-space: nowrap;
	}

	.sum-box {
		flex: 1;
		min-width: 0;
		overflow: hidden;
	}

	.sum-main {
		display: flex;
		align-items: baseline;
		flex-wrap: wrap;
		gap: 8rpx;
	}

	.sum-label {
		font-size: 24rpx;
		color: #4a5540;
		line-height: 1.2;
		white-space: nowrap;
	}

	.sum-hint {
		margin-top: 4rpx;
		font-size: 20rpx;
		color: #b0b8a8;
		line-height: 1.3;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.sum-price {
		font-size: 36rpx;
		font-weight: 700;
		line-height: 1.1;
		color: #59b50e;
		white-space: nowrap;
	}

	.submit-btn {
		box-sizing: border-box;
		flex-shrink: 0;
		padding: 0 32rpx;
		height: 80rpx;
		line-height: 80rpx;
		margin-left: 16rpx;
		border-radius: 40rpx;
		font-size: 28rpx;
		font-weight: 600;
		color: #ffffff;
		background: linear-gradient(135deg, #7ed321, #6bc01a);
		border: none;
	}

	.submit-btn.danger {
		background: linear-gradient(135deg, #ff8f7a, #ff6b57);
	}
</style>
