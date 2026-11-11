<script setup>
	import {
		ref
	} from 'vue';
	import {
		onShow
	} from '@dcloudio/uni-app';
	import {
		ensureLogin
	} from '@/common/auth';
	import {
		ensureMerchantApproved,
		executeMerchantOrderAction,
		fetchDashboardStats,
		guardMerchantOperational,
		updateStoreBusinessStatus
	} from '@/common/api/merchant';
	import {
		fetchMerchantUnreadSummary
	} from '@/common/api/merchantMessage';
	import {
		formatMoney
	} from '@/common/merchant/constants';

	const loading = ref(false);
	const accessGranted = ref(false);
	const togglingBusiness = ref(false);
	const actionLoadingId = ref('');
	const unreadCount = ref(0);

	const storeName = ref('商家工作台');
	const businessOpen = ref(true);
	const goodsStats = ref({
		total: 0,
		onShelf: 0
	});
	const orderStats = ref({
		total: 0,
		pendingVerify: 0,
		renting: 0,
		refunded: 0
	});
	const revenueStats = ref({
		today: 0,
		month: 0,
		total: 0
	});
	const todayOrders = ref([]);

	const formatAmount = (value) => `¥${formatMoney(value)}`;

	const guardOperational = async () => {
		const allowed = await guardMerchantOperational({
			toast: true
		});
		return allowed;
	};

	const goPage = (url) => {
		if (!ensureLogin(url)) return;
		uni.navigateTo({
			url
		});
	};

	const goPageGuarded = async (url) => {
		if (!ensureLogin(url)) return;
		if (!(await guardOperational())) return;
		uni.navigateTo({
			url
		});
	};

	const goStore = () => goPageGuarded('/packageMerchant/store');

	const goAddGoods = () => goPageGuarded('/packageMerchant/goods-edit?mode=create');

	const goManageGoods = () => goPageGuarded('/packageMerchant/goods');

	const goRevenue = () => goPageGuarded('/packageMerchant/revenue');

	const goWallet = () => goPage('/packageMerchant/wallet');

	const goMessages = () => {
		if (!ensureLogin('/packageMerchant/message')) return;
		uni.navigateTo({
			url: '/packageMerchant/message?tab=service'
		});
	};

	const goOrders = () => goPageGuarded('/packageMerchant/orders');

	const goStaff = () => goPageGuarded('/packageMerchant/staff');

	const goOrderDetail = (id) => goPageGuarded(`/packageMerchant/order-detail?id=${encodeURIComponent(id)}`);

	const loadData = async () => {
		if (!ensureLogin('/packageMerchant/dashboard')) return;
		loading.value = true;
		accessGranted.value = false;
		try {
			await ensureMerchantApproved({
				redirect: true,
				toast: true
			});
			const stats = await fetchDashboardStats();
			accessGranted.value = true;
			storeName.value = stats.storeName || '商家工作台';
			businessOpen.value = stats.businessStatus !== false;
			goodsStats.value = stats.goods || goodsStats.value;
			orderStats.value = stats.orders || orderStats.value;
			revenueStats.value = stats.revenue || revenueStats.value;
			todayOrders.value = stats.todayPickupOrders || [];
			try {
				const summary = await fetchMerchantUnreadSummary({ silentError: true });
				unreadCount.value = Number(summary?.totalUnread || 0);
			} catch {
				unreadCount.value = 0;
			}
		} catch {
			accessGranted.value = false;
			unreadCount.value = 0;
		} finally {
			loading.value = false;
		}
	};

	const handleBusinessToggle = async (event) => {
		const next = Boolean(event.detail.value);
		if (togglingBusiness.value) return;
		if (!(await guardOperational())) {
			businessOpen.value = !next;
			return;
		}
		togglingBusiness.value = true;
		try {
			await updateStoreBusinessStatus(next);
			businessOpen.value = next;
			uni.showToast({
				title: next ? '已开始营业' : '已切换为休息',
				icon: 'none'
			});
		} catch (error) {
			businessOpen.value = !next;
			uni.showToast({
				title: error?.msg || error?.message || '营业状态更新失败',
				icon: 'none'
			});
		} finally {
			togglingBusiness.value = false;
		}
	};

	const handleOrderAction = async (item) => {
		if (!item?.nextAction || item.nextAction === 'none' || actionLoadingId.value) return;
		if (!(await guardOperational())) return;
		if (item.nextAction === 'pickup') {
			goOrderDetail(item.id);
			return;
		}
		actionLoadingId.value = item.id;
		try {
			await executeMerchantOrderAction({
				id: item.id,
				nextAction: item.nextAction
			});
			uni.showToast({
				title: '操作成功',
				icon: 'success'
			});
			await loadData();
		} catch (error) {
			uni.showToast({
				title: error?.msg || error?.message || '操作失败',
				icon: 'none'
			});
		} finally {
			actionLoadingId.value = '';
		}
	};

	onShow(loadData);
</script>

<template>
	<view v-if="loading" class="loading-wrap">
		<text class="loading-text">加载中...</text>
	</view>
	<view v-else-if="accessGranted" class="page">
		<view class="hero-card" :class="businessOpen ? 'hero-open' : 'hero-closed'">
			<view class="hero-head">
				<view class="hero-info" @click="goStore">
					<text class="hero-title">{{ storeName }}</text>
					<text class="hero-desc">门店经营概览</text>
				</view>
				<view class="hero-actions">
					<view class="hero-message" @click="goMessages">
						<view class="hero-message-icon">
							<up-icon name="bell" size="22" color="#59b50e"></up-icon>
							<view v-if="unreadCount" class="hero-message-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</view>
						</view>
						<text class="hero-message-label">消息</text>
					</view>
					<view class="hero-wallet" @click="goWallet">
						<view class="hero-wallet-icon">
							<up-icon name="red-packet" size="22" color="#59b50e"></up-icon>
						</view>
						<text class="hero-wallet-label">商户钱包</text>
					</view>
				</view>
			</view>
			<view class="hero-foot">
				<view class="hero-store-link" @click="goStore">
					<text class="hero-store-text">门店设置</text>
					<up-icon name="arrow-right" size="14" color="rgba(255,255,255,0.88)"></up-icon>
				</view>
				<view class="hero-status" @click.stop>
					<text class="status-badge" :class="businessOpen ? 'open' : 'closed'">{{ businessOpen ? '营业中' : '休息中' }}</text>
					<switch :color="businessOpen ? '#ffffff' : '#f5c842'" :checked="businessOpen" :disabled="togglingBusiness"
						@change="handleBusinessToggle" />
				</view>
			</view>
		</view>

		<view class="overview-card">
			<view class="stats-section">
				<view class="section-head">
					<view class="section-title-wrap">
						<view class="section-mark"></view>
						<text class="section-title">商品</text>
					</view>
				</view>
				<view class="metric-row">
					<view class="metric-item">
						<text class="metric-value">{{ goodsStats.total }}</text>
						<text class="metric-label">全部</text>
					</view>
					<view class="metric-item">
						<text class="metric-value">{{ goodsStats.onShelf }}</text>
						<text class="metric-label">在售</text>
					</view>
					<view class="metric-item action" @click.stop="goAddGoods">
						<view class="metric-action-icon">
							<up-icon name="plus" size="15" color="#ffffff"></up-icon>
						</view>
						<text class="metric-label action">添加商品</text>
					</view>
					<view class="metric-item action" @click.stop="goManageGoods">
						<view class="metric-action-icon">
							<up-icon name="grid" size="18" color="#ffffff"></up-icon>
						</view>
						<text class="metric-label action">管理商品</text>
					</view>
				</view>
			</view>

			<view class="stats-section">
				<view class="section-head">
					<view class="section-title-wrap">
						<view class="section-mark"></view>
						<text class="section-title">订单</text>
					</view>
					<view class="section-link-row" @click.stop="goOrders">
						<text class="section-link">全部订单</text>
						<up-icon name="arrow-right" size="12" color="#59b50e"></up-icon>
					</view>
				</view>
				<view class="metric-row metrics-4">
					<view class="metric-item">
						<text class="metric-value">{{ orderStats.total }}</text>
						<text class="metric-label">全部</text>
					</view>
					<view class="metric-item">
						<text class="metric-value accent">{{ orderStats.pendingVerify }}</text>
						<text class="metric-label">待核销</text>
					</view>
					<view class="metric-item">
						<text class="metric-value">{{ orderStats.renting }}</text>
						<text class="metric-label">租赁中</text>
					</view>
					<view class="metric-item">
						<text class="metric-value warn">{{ orderStats.refunded }}</text>
						<text class="metric-label">已退款</text>
					</view>
				</view>
			</view>

			<view class="stats-section">
				<view class="section-head">
					<view class="section-title-wrap">
						<view class="section-mark"></view>
						<text class="section-title">营收</text>
					</view>
					<view class="section-link-row" @click.stop="goRevenue">
						<text class="section-link">营收明细</text>
						<up-icon name="arrow-right" size="12" color="#59b50e"></up-icon>
					</view>
				</view>
				<view class="metric-row metrics-3">
					<view class="metric-item">
						<text class="metric-value money">{{ formatAmount(revenueStats.today) }}</text>
						<text class="metric-label">今日</text>
					</view>
					<view class="metric-item">
						<text class="metric-value money">{{ formatAmount(revenueStats.month) }}</text>
						<text class="metric-label">当月</text>
					</view>
					<view class="metric-item">
						<text class="metric-value money">{{ formatAmount(revenueStats.total) }}</text>
						<text class="metric-label">总计</text>
					</view>
				</view>
			</view>
		</view>

		<view class="stats-section entry-section" @click="goStaff">
			<view class="entry-row">
				<view class="entry-main">
					<view class="section-title-wrap">
						<view class="section-mark"></view>
						<text class="section-title">员工管理</text>
					</view>
					<text class="section-sub">管理核销员与联系人</text>
				</view>
				<up-icon name="arrow-right" size="16" color="#8a9a7a"></up-icon>
			</view>
		</view>

		<view class="card today-card">
			<view class="section-head">
				<view>
					<view class="section-title-wrap">
						<view class="section-mark dark"></view>
						<text class="section-title dark">今日订单</text>
					</view>
					<text class="section-sub">待取货订单</text>
				</view>
				<view class="section-link-row muted" @click="goPageGuarded('/packageMerchant/orders?tab=pending')">
					<text class="section-link dark">查看全部</text>
					<up-icon name="arrow-right" size="12" color="#6b7a5e"></up-icon>
				</view>
			</view>

			<view v-if="!todayOrders.length" class="empty-box">
				<up-icon name="order" size="36" color="#b0b8a8"></up-icon>
				<text class="empty-text">今日暂无待取货订单</text>
			</view>

			<view v-for="item in todayOrders" :key="item.id" class="order-row">
				<view class="order-main" @click="goOrderDetail(item.id)">
					<view class="order-head">
						<text class="order-goods">{{ item.goods }}</text>
						<text class="order-status">{{ item.status }}</text>
					</view>
					<text class="order-meta">{{ item.contact }} · 取货 {{ item.pickupTime }}</text>
					<text class="order-amount">¥{{ item.amount }}</text>
				</view>
				<view class="order-actions">
					<view v-if="item.nextAction && item.nextAction !== 'none'" class="action-btn primary"
						@click.stop="handleOrderAction(item)">{{ item.actionLabel }}</view>
					<view class="action-btn ghost" @click.stop="goOrderDetail(item.id)">详情</view>
				</view>
			</view>
		</view>
	</view>
</template>

<style scoped>
	.page {
		box-sizing: border-box;
		min-height: 100vh;
		padding: 20rpx 24rpx 44rpx;
		background: #f3f6f1;
		overflow-x: hidden;
	}

	.loading-wrap {
		display: flex;
		align-items: center;
		justify-content: center;
		min-height: 100vh;
		background: #f6f8f3;
	}

	.loading-text {
		font-size: 28rpx;
		color: #8a9a7a;
	}

	.hero-card {
		box-sizing: border-box;
		padding: 28rpx;
		border-radius: 20rpx;
		transition: background 0.25s ease, box-shadow 0.25s ease;
	}

	.hero-open {
		background: linear-gradient(145deg, #337547 0%, #4e963d 100%);
		box-shadow: 0 10rpx 28rpx rgba(42, 98, 55, 0.18);
	}

	.hero-closed {
		background: #6f7d72;
		box-shadow: 0 10rpx 28rpx rgba(67, 81, 70, 0.16);
	}

	.hero-head {
		display: flex;
		align-items: flex-start;
		justify-content: space-between;
		gap: 24rpx;
	}

	.hero-info {
		flex: 1;
		min-width: 0;
	}

	.hero-title,
	.hero-desc,
	.hero-wallet-label,
	.hero-message-label,
	.hero-store-text {
		display: block;
	}

	.hero-actions {
		flex-shrink: 0;
		display: flex;
		align-items: flex-start;
		gap: 16rpx;
	}

	.hero-message,
	.hero-wallet {
		flex-shrink: 0;
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 8rpx;
		padding-top: 4rpx;
	}

	.hero-message-icon,
	.hero-wallet-icon {
		position: relative;
		display: flex;
		align-items: center;
		justify-content: center;
		width: 64rpx;
		height: 64rpx;
		border-radius: 16rpx;
		background: rgba(255, 255, 255, 0.94);
	}

	.hero-message-badge {
		position: absolute;
		top: -6rpx;
		right: -6rpx;
		min-width: 28rpx;
		height: 28rpx;
		padding: 0 8rpx;
		line-height: 28rpx;
		border-radius: 14rpx;
		text-align: center;
		font-size: 18rpx;
		font-weight: 700;
		color: #ffffff;
		background: #ff5d57;
	}

	.hero-message-label,
	.hero-wallet-label {
		font-size: 20rpx;
		font-weight: 600;
		color: rgba(255, 255, 255, 0.95);
		line-height: 1.2;
	}

	.hero-title {
		font-size: 36rpx;
		font-weight: 700;
		color: #ffffff;
		line-height: 1.35;
	}

	.hero-desc {
		margin-top: 10rpx;
		font-size: 22rpx;
		line-height: 1.6;
		color: rgba(255, 255, 255, 0.88);
	}

	.hero-foot {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 20rpx;
		margin-top: 24rpx;
		padding-top: 20rpx;
		border-top: 1rpx solid rgba(255, 255, 255, 0.22);
	}

	.hero-store-link {
		display: flex;
		align-items: center;
		gap: 6rpx;
		min-width: 0;
	}

	.hero-store-text {
		font-size: 24rpx;
		font-weight: 600;
		color: rgba(255, 255, 255, 0.95);
	}

	.hero-status {
		flex-shrink: 0;
		display: flex;
		align-items: center;
		gap: 12rpx;
	}

	.status-badge {
		padding: 6rpx 18rpx;
		border-radius: 999rpx;
		font-size: 22rpx;
		font-weight: 700;
		line-height: 1.2;
		white-space: nowrap;
	}

	.status-badge.open {
		color: #4a8f00;
		background: #ffffff;
		box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.08);
	}

	.status-badge.closed {
		color: #b45309;
		background: #fff4d6;
		box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.06);
	}

	.overview-card,
	.card,
	.entry-section {
		box-sizing: border-box;
		margin-top: 20rpx;
		border-radius: 20rpx;
		background: #ffffff;
		box-shadow: 0 5rpx 20rpx rgba(31, 53, 35, 0.045);
}

	.stats-section {
		padding: 24rpx;
	}

	.stats-section + .stats-section {
		border-top: 1rpx solid #e8eee4;
	}

	.card {
		padding: 26rpx 24rpx;
	}

	.stats-section .section-head {
		margin-bottom: 18rpx;
	}

	.section-head {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-bottom: 16rpx;
	}

	.section-title-wrap {
		display: flex;
		align-items: center;
		gap: 10rpx;
	}

	.section-mark {
		width: 5rpx;
		height: 24rpx;
		border-radius: 3rpx;
		background: #55a833;
		flex-shrink: 0;
	}

	.section-mark.dark {
		background: #59b50e;
	}

	.section-title {
		font-size: 27rpx;
		font-weight: 600;
		color: #1a2114;
		line-height: 1.2;
	}

	.section-title.dark {
		display: block;
	}

	.section-sub {
		display: block;
		margin-top: 8rpx;
		margin-left: 16rpx;
		font-size: 22rpx;
		color: #8a9a7a;
		line-height: 1.4;
	}

	.section-link-row {
		display: flex;
		align-items: center;
		gap: 4rpx;
		flex-shrink: 0;
	}

	.section-link {
		font-size: 22rpx;
		color: #47952d;
		font-weight: 500;
		line-height: 1.2;
	}

	.section-link.dark {
		color: #6b7a5e;
	}

	.add-btn {
		display: flex;
		align-items: center;
		justify-content: center;
		width: 56rpx;
		height: 56rpx;
		border-radius: 16rpx;
		background: #f1f8df;
	}

	.metric-row.metrics-4 .metric-item {
		width: calc((100% - 30rpx) / 4);
	}

	.metric-item.action {
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		gap: 6rpx;
	}

	.metric-action-icon {
		display: flex;
		align-items: center;
		justify-content: center;
		width: 40rpx;
		height: 40rpx;
		border-radius: 12rpx;
		background: #4d9a34;
	}

	.metric-label.action {
		margin-top: 0;
		color: #59b50e;
		font-weight: 400;
	}

	.metric-row {
		display: flex;
		gap: 0;
	}

	.metric-row.metrics-4 .metric-item {
		width: calc((100% - 30rpx) / 4);
	}

	.metric-row.metrics-3 .metric-item {
		width: calc((100% - 20rpx) / 3);
	}

	.metric-item {
		box-sizing: border-box;
		flex: 1;
		min-width: 0;
		padding: 8rpx 6rpx;
		text-align: center;
	}

	.metric-item + .metric-item {
		border-left: 1rpx solid #edf1e9;
	}

	.metric-value {
		display: block;
		font-size: 32rpx;
		font-weight: 800;
		color: #1a2114;
		line-height: 1.15;
	}

	.metric-value.accent {
		color: #59b50e;
	}

	.metric-value.warn {
		color: #ef6b4a;
	}

	.metric-value.money {
		font-size: 25rpx;
		color: #367d32;
	}

	.metric-label {
		display: block;
		margin-top: 8rpx;
		font-size: 20rpx;
		color: #8a9a7a;
	}

	.empty-box {
		display: flex;
		flex-direction: column;
		align-items: center;
		padding: 48rpx 0 24rpx;
	}

	.empty-text {
		margin-top: 16rpx;
		font-size: 24rpx;
		color: #8a9a7a;
	}

	.order-row {
		padding: 22rpx 0;
		border-top: 1rpx solid #eaf0e6;
	}

	.order-row:first-of-type {
		border-top: none;
		padding-top: 8rpx;
	}

	.order-head {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 16rpx;
	}

	.order-goods {
		flex: 1;
		min-width: 0;
		font-size: 28rpx;
		font-weight: 700;
		color: #1a2114;
	}

	.order-status {
		flex-shrink: 0;
		font-size: 22rpx;
		color: #59b50e;
		font-weight: 600;
	}

	.order-meta,
	.order-amount {
		display: block;
		margin-top: 8rpx;
		font-size: 22rpx;
		color: #8a9a7a;
		line-height: 1.5;
	}

	.order-amount {
		margin-top: 6rpx;
		font-size: 26rpx;
		font-weight: 700;
		color: #59b50e;
	}

	.order-actions {
		display: flex;
		justify-content: flex-end;
		gap: 12rpx;
		margin-top: 18rpx;
	}

	.action-btn {
		display: flex;
		align-items: center;
		justify-content: center;
		box-sizing: border-box;
		height: 60rpx;
		margin: 0;
		padding: 0 24rpx;
		border-radius: 12rpx;
		font-size: 23rpx;
		line-height: normal;
		text-align: center;
		border: none;
	}

	.action-btn.primary {
		color: #ffffff;
		background: #4d9a34;
	}

	.entry-section {
		padding: 24rpx;
	}

	.entry-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 16rpx;
	}

	.entry-main {
		flex: 1;
		min-width: 0;
	}

	.entry-section .section-sub {
		margin-left: 14rpx;
		margin-top: 8rpx;
	}

	.action-btn.ghost {
		color: #59b50e;
		background: #f1f8df;
	}
</style>
