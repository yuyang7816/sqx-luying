<template>
	<view class="page">
		<view class="tab-panel">
			<u-tabs :list="orderTabs" keyName="label" :current="currentTabIndex" :scrollable="true" lineColor="#7ed321"
				:activeStyle="tabActiveStyle" :inactiveStyle="tabInactiveStyle" @change="handleTabChange"></u-tabs>
		</view>

		<view class="list-body">
			<view v-if="pageLoading && !orderList.length" class="empty-card">
				<text class="empty-title">加载中...</text>
			</view>
			<view v-else-if="!orderList.length" class="empty-card">
				<up-icon name="file-text" size="34" color="#a7b19f"></up-icon>
				<text class="empty-title">当前没有订单</text>
				<text class="empty-desc">去首页挑选露营装备后，这里会展示你的下单记录。</text>
				<view class="empty-btn" @click="goHome">去逛逛</view>
			</view>
			<view v-for="order in orderList" :key="order.id" class="order-card">
				<view class="order-head">
					<text class="order-no">订单编号：{{ order.orderNo || order.id }}</text>
					<text class="order-status" :class="`status-${order.status}`">{{ getStatusInfo(order.status).label }}</text>
				</view>
				<view v-for="item in order.goodsItems" :key="`${order.id}-${item.id}`" class="goods-block">
					<view class="goods-row" @click="goDetail(order.id)">
						<image class="goods-image" :src="item.image" mode="aspectFill"></image>
						<view class="goods-main">
							<text class="goods-name">{{ item.name }}</text>
							<text class="goods-meta">{{ item.specLabel }}<text v-if="item.packageLabel"> ·
									{{ item.packageLabel }}</text></text>
							<text v-if="order.returnDate" class="goods-meta accent-meta">{{ order.returnMethodLabel || '到店归还' }} ·
								{{ order.returnDate }}</text>
						</view>
					</view>
					<view class="schedule-grid">
						<view class="schedule-item">
							<text class="schedule-label">取货时间</text>
							<text class="schedule-time">{{ formatPickupTime(order) }}</text>
						</view>
						<view class="schedule-divider"></view>
						<view class="schedule-item">
							<text class="schedule-label">应还时间</text>
							<text class="schedule-time emphasis">{{ formatReturnTime(order) }}</text>
						</view>
					</view>
				</view>
				<view class="summary-row">
					<text class="summary-text">{{ order.payAmount }}</text>
					<text class="summary-time">{{ order.createTime }}</text>
				</view>
				<view class="action-row">
					<view v-if="order.status === 'pending_payment' || isPickupStatus(order.status)" class="plain-btn"
						@click="handleCancel(order)">取消订单</view>
					<view v-if="order.status === 'pending_payment'" class="primary-btn" @click="goPay(order)">去付款</view>
					<view v-if="isPickupStatus(order.status)" class="ghost-btn" @click="goPickup(order)">取货凭证</view>
					<view v-if="order.status === 'renting'" class="plain-btn" @click="goRenew(order)">申请续租</view>
					<view v-if="order.status === 'renting'" class="primary-btn" @click="goReturn(order)">申请归还</view>
					<view v-if="order.status === 'returning' || order.status === 'inspecting'" class="plain-btn"
						@click="goDetail(order.id)">查看详情</view>
					<view v-if="order.status === 'completed' || order.status === 'cancelled'" class="plain-btn"
						@click="goDetail(order.id)">查看详情</view>
				</view>
			</view>
			<view v-if="loadingMore" class="load-more">加载更多...</view>
		</view>
	</view>

	<PaymentMethodPopup v-model:show="paymentPopupVisible" :amount="payingOrder?.payAmount || 0"
		:loading="!!actionLoadingId" @confirm="confirmPay" />
</template>

<script setup>
	import {
		computed,
		ref
	} from 'vue';
	import {
		onLoad,
		onShow,
		onReachBottom
	} from '@dcloudio/uni-app';
	import PaymentMethodPopup from '@/components/PaymentMethodPopup.vue';
	import {
		fetchOrderPage
	} from '@/common/api/order';
	import {
		ensureLogin,
		isLoggedIn
	} from '@/common/auth';
	import {
		confirmCancelOrder,
		executeOrderPayment
	} from '@/common/orderActions';
	import {
		ORDER_TABS,
		formatRentPeriodDisplay,
		getOrderStatusInfo,
		isPickupStatus
	} from '@/common/rental/constants';

	const statusAlias = {
		pending: 'pending_payment',
		pickup: 'waiting_pickup',
		using: 'renting',
		return: 'returning',
		done: 'completed'
	};

	const orderTabs = ORDER_TABS;
	const activeTab = ref('all');
	const orderList = ref([]);
	const pageLoading = ref(false);
	const loadingMore = ref(false);
	const pageNo = ref(1);
	const hasMore = ref(true);
	const actionLoadingId = ref('');
	const paymentPopupVisible = ref(false);
	const payingOrder = ref(null);
	const tabActiveStyle = {
		color: '#59b50e',
		fontWeight: '700'
	};
	const tabInactiveStyle = {
		color: '#6b7280'
	};
	const currentTabIndex = computed(() => Math.max(orderTabs.findIndex((item) => item.key === activeTab.value), 0));

	const getStatusInfo = getOrderStatusInfo;
	const formatPickupTime = (order) => formatRentPeriodDisplay(order?.startTime);
	const formatReturnTime = (order) => formatRentPeriodDisplay(order?.endTime);

	const loadOrders = async (reset = false) => {
		if (reset) {
			pageNo.value = 1;
			hasMore.value = true;
			orderList.value = [];
		}
		if (!hasMore.value && !reset) return;
		const loadingRef = reset ? pageLoading : loadingMore;
		if (loadingRef.value) return;
		loadingRef.value = true;
		try {
			const pageVo = await fetchOrderPage({
				status: activeTab.value,
				page: pageNo.value,
				limit: 10
			});
			const records = pageVo.records || [];
			orderList.value = reset ? records : [...orderList.value, ...records];
			hasMore.value = records.length >= 10 && orderList.value.length < Number(pageVo.total || 0);
			if (records.length) pageNo.value += 1;
		} catch (error) {
			if (reset) orderList.value = [];
		} finally {
			loadingRef.value = false;
		}
	};

	onLoad((options) => {
		if (!ensureLogin('/packageOrder/list')) return;
		if (options.status) {
			activeTab.value = statusAlias[options.status] || options.status;
		}
		loadOrders(true);
	});

	onShow(() => {
		if (isLoggedIn()) {
			loadOrders(true);
		}
	});

	const loadMore = () => {
		if (!pageLoading.value && hasMore.value) {
			loadOrders(false);
		}
	};

	onReachBottom(loadMore);

	const handleTabChange = (item) => {
		activeTab.value = item.key;
		loadOrders(true);
	};

	const goDetail = (id) => uni.navigateTo({
		url: `/packageOrder/detail?id=${id}`
	});

	const handleCancel = async (order) => {
		if (actionLoadingId.value) return;
		actionLoadingId.value = `cancel-${order.id}`;
		const paid = isPickupStatus(order.status);
		const {
			ok
		} = await confirmCancelOrder(order.id, {
			paid,
			successText: paid ? '订单已取消，款项将原路退回' : '订单已取消'
		});
		actionLoadingId.value = '';
		if (ok) {
			loadOrders(true);
		}
	};

	const goPay = (order) => {
		if (actionLoadingId.value) return;
		payingOrder.value = order;
		paymentPopupVisible.value = true;
	};

	const confirmPay = async (methodKey) => {
		if (!payingOrder.value || actionLoadingId.value) return;
		const order = payingOrder.value;
		actionLoadingId.value = `pay-${order.id}`;
		const {
			ok
		} = await executeOrderPayment({
			orderId: order.id,
			orderNo: order.orderNo,
			payAmount: order.payAmount,
			methodKey
		});
		actionLoadingId.value = '';
		if (ok) {
			paymentPopupVisible.value = false;
			payingOrder.value = null;
			loadOrders(true);
		}
	};

	const goPickup = (order) => goDetail(order.id);
	const goRenew = (order) => uni.navigateTo({
		url: `/packageOrder/renew?id=${order.id}`
	});
	const goReturn = (order) => uni.navigateTo({
		url: `/packageOrder/return-apply?id=${order.id}`
	});
	const goHome = () => uni.switchTab({
		url: '/pages/index/index'
	});
</script>

<style scoped>
	/* ========== 页面基底 ========== */
	.page {
		box-sizing: border-box;
		min-height: 100vh;
		background: #f6f8f3;
		overflow-x: hidden;
	}

	/* ========== 标签栏 ========== */
	.tab-panel {
		padding: 20rpx 28rpx 12rpx;
		background: #ffffff;
		box-shadow: 0 2rpx 16rpx rgba(126, 211, 33, 0.06);
		position: sticky;
		top: 0;
		z-index: 100;
	}

	.tab-panel :deep(.u-tabs__wrapper__nav__item) {
		padding: 0 24rpx;
	}

	.tab-panel :deep(.u-tabs__wrapper__nav__item__text) {
		font-size: 26rpx;
		white-space: nowrap;
	}

	/* ========== 列表内容区 ========== */
	.list-body {
		box-sizing: border-box;
		padding: 28rpx 28rpx 40rpx;
	}

	.load-more {
		padding: 24rpx 0 8rpx;
		text-align: center;
		font-size: 24rpx;
		color: #9aa58a;
	}

	.empty-card {
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		padding: 120rpx 40rpx;
		background: #ffffff;
		border-radius: 32rpx;
		box-shadow: 0 6rpx 24rpx rgba(0, 0, 0, 0.04);
	}

	.empty-title,
	.empty-desc {
		display: block;
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
		display: flex;
		align-items: center;
		justify-content: center;
		box-sizing: border-box;
		width: 240rpx;
		height: 84rpx;
		line-height: normal;
		margin-top: 32rpx;
		border-radius: 42rpx;
		font-size: 28rpx;
		font-weight: 600;
		color: #ffffff;
		background: linear-gradient(135deg, #7ed321, #6bc01a);
		border: none;
	}

	/* ========== 订单卡片 ========== */
	.order-card {
		box-sizing: border-box;
		width: 100%;
		margin-bottom: 28rpx;
		padding: 32rpx;
		background: #ffffff;
		border-radius: 32rpx;
		box-shadow: 0 6rpx 24rpx rgba(0, 0, 0, 0.04);
		overflow: hidden;
		transition: all 0.2s ease;
	}

	.order-card:active {
		transform: translateY(-2rpx);
		box-shadow: 0 10rpx 32rpx rgba(0, 0, 0, 0.08);
	}

	/* ========== 订单头部 ========== */
	.order-head {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-bottom: 24rpx;
	}

	.order-no,
	.order-status,
	.goods-name,
	.goods-meta,
	.summary-text {
		display: block;
	}

	.order-no {
		font-size: 24rpx;
		color: #8a9a7a;
		font-weight: 500;
	}

	.order-status {
		font-size: 24rpx;
		font-weight: 700;
		color: #7ed321;
	}

	.order-status.status-pending_payment {
		color: #c88a00;
	}

	.order-status.status-cancelled {
		color: #9aa2b1;
	}

	/* ========== 商品行 ========== */
	.goods-block + .goods-block {
		margin-top: 16rpx;
	}

	.goods-row {
		display: flex;
		align-items: flex-start;
	}

	.goods-image {
		width: 148rpx;
		height: 148rpx;
		border-radius: 20rpx;
		background: #edf2f7;
		flex-shrink: 0;
	}

	.goods-main {
		flex: 1;
		min-width: 0;
		margin-left: 20rpx;
		display: flex;
		flex-direction: column;
		justify-content: center;
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

	.goods-meta {
		margin-top: 8rpx;
		font-size: 22rpx;
		color: #8a9a7a;
	}

	.goods-meta.accent-meta {
		color: #5aad15;
		font-weight: 600;
	}

	.schedule-grid {
		display: flex;
		align-items: stretch;
		margin-top: 14rpx;
		background: #f7f9f5;
		border-radius: 16rpx;
	}

	.schedule-item {
		flex: 1;
		min-width: 0;
		padding: 18rpx 20rpx;
	}

	.schedule-divider {
		width: 1rpx;
		margin: 18rpx 0;
		background: #dce5d4;
	}

	.schedule-label,
	.schedule-time {
		display: block;
	}

	.schedule-label {
		font-size: 21rpx;
		color: #7b8971;
	}

	.schedule-time {
		margin-top: 8rpx;
		font-size: 24rpx;
		font-weight: 700;
		line-height: 1.45;
		color: #24351e;
		word-break: break-word;
	}

	.schedule-time.emphasis {
		color: #4a8f0f;
	}

	/* ========== 汇总行 ========== */
	.summary-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding-top: 20rpx;
		border-top: 1rpx solid #f1f4ec;
	}

	.summary-text,
	.summary-time {
		display: block;
	}

	.summary-text {
		font-size: 36rpx;
		font-weight: 800;
		color: #1a2114;
		letter-spacing: -1rpx;
	}

	.summary-text::before {
		content: '¥';
		font-size: 26rpx;
		font-weight: 700;
		margin-right: 2rpx;
	}

	.summary-time {
		flex: 1;
		min-width: 0;
		margin-left: 16rpx;
		text-align: right;
		font-size: 22rpx;
		line-height: 1.6;
		color: #9aa58a;
		word-break: break-all;
	}

	/* ========== 操作按钮行 ========== */
	.action-row {
		display: flex;
		justify-content: flex-end;
		flex-wrap: wrap;
		margin-top: 24rpx;
		margin-left: -16rpx;
	}

	.primary-btn,
	.ghost-btn,
	.plain-btn {
		display: flex;
		align-items: center;
		justify-content: center;
		box-sizing: border-box;
		min-width: 0;
		width: auto;
		min-height: 68rpx;
		padding: 0 32rpx;
		line-height: normal;
		margin: 0 0 0 16rpx;
		border-radius: 36rpx;
		font-size: 26rpx;
		/* font-weight: 500; */
		text-align: center;
		transition: all 0.2s ease;
	}

	.primary-btn {
		color: #ffffff;
		background: linear-gradient(135deg, #7ed321, #6bc01a);
		box-shadow: 0 4rpx 14rpx rgba(126, 211, 33, 0.3);
		border: none;
	}

	.primary-btn:active {
		transform: scale(0.96);
		box-shadow: 0 2rpx 8rpx rgba(126, 211, 33, 0.2);
	}

	.ghost-btn {
		color: #5cb816;
		background: #f6ffea;
		border: 2rpx solid rgba(126, 211, 33, 0.2);
	}

	.ghost-btn:active {
		background: #eefadb;
		transform: scale(0.96);
	}

	.plain-btn {
		color: #6b7a5e;
		background: #f6f8f3;
		border: 2rpx solid #e4e9dd;
	}

	.plain-btn:active {
		background: #eef2e8;
		transform: scale(0.96);
	}
</style>