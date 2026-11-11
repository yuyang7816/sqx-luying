<template>
	<view class="page">
		<view class="hero-panel">
			<view class="hero-main" @click="goProfile">
				<view class="hero-actions">
					<view class="message-entry" @click.stop="goMessages">
						<up-icon name="bell" size="28" color="#ffffff"></up-icon>
						<view v-if="unreadCount" class="message-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</view>
					</view>
				</view>
				<view class="user-row">
					<image class="avatar" :src="session.userInfo.avatar" mode="aspectFill"></image>
					<view class="user-main-info">
						<text class="nickname">{{ session.userInfo.nickname }}</text>
					</view>
				</view>
			</view>

			<view class="asset-panel">
				<view class="asset-item" @click="handleAsset('/packageUser/wallet')">
					<text class="asset-value">{{ walletBalance }}</text>
					<text class="asset-label">余额 (元)</text>
				</view>
				<view class="asset-item" @click="handleAsset('/packageContent/deposit')">
					<text class="asset-value">{{ depositAmount }}</text>
					<text class="asset-label">押金 (元)</text>
				</view>
				<view class="asset-item" @click="handleAsset('/packageUser/coupon')">
					<text class="asset-value">{{ couponCount }}</text>
					<text class="asset-label">优惠券 (张)</text>
				</view>
			</view>
		</view>

		<view class="content-wrap">
			<view class="content-card order-section">
				<view class="section-head">
					<text class="section-title">我的订单</text>
					<view class="order-link" @click="goOrderList">
						<text class="section-link-text">全部订单</text>
						<up-icon name="arrow-right" size="14" color="#a8afb7"></up-icon>
					</view>
				</view>
				<view class="status-grid">
					<view v-for="item in orderStates" :key="item.label" class="status-item" @click="goOrder(item.key)">
						<view class="status-icon">
							<up-icon :name="item.icon" size="26" color="#646c78"></up-icon>
						</view>
						<text class="status-label">{{ item.label }}</text>
					</view>
				</view>
			</view>

			<view class="content-card tool-section">
				<view class="section-head service-head">
					<text class="section-title">常用工具</text>
				</view>
				<view class="service-grid">
					<view v-for="item in serviceItems" :key="item.label" class="service-item" @click="handleService(item)">
						<view class="service-icon" :style="{ background: item.iconBg }">
							<up-icon :name="item.icon" size="24" :color="item.iconColor"></up-icon>
						</view>
						<text class="service-label">{{ item.label }}</text>
					</view>
				</view>
			</view>

			<view class="recommend-section">
				<view class="recommend-head">
					<view class="recommend-mark"></view>
					<text class="recommend-title">为你推荐</text>
				</view>
				<view class="recommend-grid">
					<view v-for="item in recommendGoods" :key="item.id" class="recommend-item" @click="goDetail(item.id)">
						<image class="recommend-image" :src="item.coverImage" mode="aspectFill"></image>
						<text class="recommend-name">{{ item.name }}</text>
						<view class="recommend-price-row">
							<text class="recommend-price">¥{{ formatPrice(item).price }}</text>
							<text class="recommend-unit">/{{ formatPrice(item).unit }}</text>
						</view>
					</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script setup>
	import {
		computed,
		ref
	} from 'vue';
	import {
		onShow,
		onPullDownRefresh
	} from '@dcloudio/uni-app';
	import {
		getStoredSession,
		isLoggedIn,
		ensureLogin
	} from '@/common/auth';
	import {
		fetchGoodsPage
	} from '@/common/api/rental';
	import {
		fetchCouponSummary
	} from '@/common/api/coupon';
	import {
		fetchDepositSummary
	} from '@/common/api/deposit';
	import {
		fetchWalletSummary
	} from '@/common/api/wallet';
	import {
		fetchUnreadSummary
	} from '@/common/api/message';
	import {
		fetchApplyProgress,
		fetchMerchantStaffProfile,
		canAccessMerchantWorkbench,
		isMerchantStaffActive
	} from '@/common/api/merchant';
	import { fetchServiceConfigRemote } from '@/common/api/content';
	import {
		buildOrderStates,
		formatGoodsRentPrice
	} from '@/common/rental/constants';
	import {
		resolveServiceCenterAction
	} from '@/common/serviceDefaults';
	import { DEFAULT_AVATAR_URL } from '@/common/constants/avatar';

	const orderStates = ref(buildOrderStates());
	const recommendGoods = ref([]);
	const merchantApproved = ref(false);
	const merchantStaff = ref(false);
	const baseServiceItems = [{
			label: '我的收藏',
			icon: 'heart-fill',
			iconColor: '#55a833',
			iconBg: '#eef8e7',
			url: '/packageUser/favorite',
			needLogin: true
		},
		{
			label: '地址管理',
			icon: 'map-fill',
			iconColor: '#55a833',
			iconBg: '#eef8e7',
			url: '/packageUser/address-list',
			needLogin: true
		},
		{
			label: '领券中心',
			icon: 'coupon-fill',
			iconColor: '#55a833',
			iconBg: '#eef8e7',
			url: '/packageUser/coupon-center',
			needLogin: true
		},
		{
			label: '客服中心',
			icon: 'server-fill',
			iconColor: '#55a833',
			iconBg: '#eef8e7',
			serviceCenter: true,
			url: '/packageUser/service'
		},
		{
			label: '邀请有礼',
			icon: 'gift-fill',
			iconColor: '#55a833',
			iconBg: '#eef8e7',
			url: '/packageUser/invite',
			needLogin: true
		},
		{
			label: '我的团队',
			icon: 'plus-people-fill',
			iconColor: '#55a833',
			iconBg: '#eef8e7',
			url: '/packageUser/invite-users',
			needLogin: true
		},
		{
			label: '设置中心',
			icon: 'setting-fill',
			iconColor: '#55a833',
			iconBg: '#eef8e7',
			url: '/packageUser/setting'
		}
	];
	const merchantEntryItem = computed(() => {
		if (merchantStaff.value) {
			return {
				label: '员工中心',
				icon: 'account-fill',
				iconColor: '#55a833',
				iconBg: '#eef8e7',
				url: '/packageMerchant/staff-center',
				needLogin: true
			};
		}
		if (merchantApproved.value) {
			return {
				label: '商家中心',
				icon: 'home-fill',
				iconColor: '#55a833',
				iconBg: '#eef8e7',
				url: '/packageMerchant/dashboard',
				needLogin: true
			};
		}
		return {
			label: '商家入驻',
			icon: 'home-fill',
			iconColor: '#55a833',
			iconBg: '#eef8e7',
			url: '/packageMerchant/apply'
		};
	});
	const serviceItems = computed(() => [
		...baseServiceItems.slice(0, 4),
		merchantEntryItem.value,
		...baseServiceItems.slice(4)
	]);

	const guestSession = {
		platformLabel: '游客态',
		loginModeLabel: '未登录',
		userInfo: {
			nickname: '立即登录',
			avatar: DEFAULT_AVATAR_URL
		}
	};

	const session = ref(guestSession);
	const walletBalance = ref('0.00');
	const depositAmount = ref('0.00');
	const couponCount = ref(0);
	const unreadCount = ref(0);
	const serviceCenterAction = ref({ type: 'navigate', url: '/packageUser/service', needLogin: false });
	const formatPrice = (item) => formatGoodsRentPrice(item, 'day');

	const syncSession = () => {
		session.value = getStoredSession() || guestSession;
	};

	const loadAssets = async () => {
		if (!isLoggedIn()) {
			merchantApproved.value = false;
			merchantStaff.value = false;
			walletBalance.value = '0.00';
			depositAmount.value = '0.00';
			couponCount.value = 0;
			unreadCount.value = 0;
			orderStates.value = buildOrderStates();
			return;
		}
		try {
			const wallet = await fetchWalletSummary({
				silentError: true
			});
			walletBalance.value = Number(wallet?.balance ?? wallet?.amount ?? 0).toFixed(2);
		} catch (error) {
			walletBalance.value = '0.00';
		}
		try {
			const deposit = await fetchDepositSummary({
				silentError: true
			});
			depositAmount.value = Number(deposit?.availableAmount || 0).toFixed(2);
		} catch (error) {
			depositAmount.value = '0.00';
		}
		try {
			const couponSummary = await fetchCouponSummary({
				silentError: true
			});
			couponCount.value = Number(couponSummary?.unused || 0);
		} catch (error) {
			couponCount.value = 0;
		}
		try {
			const summary = await fetchUnreadSummary({ silentError: true });
			unreadCount.value = Number(summary?.totalUnread) || 0;
		} catch (error) {
			unreadCount.value = 0;
		}
		try {
			const {
				list
			} = await fetchGoodsPage({
				page: 1,
				limit: 4,
				sortBy: 'sales'
			});
			recommendGoods.value = list || [];
		} catch (error) {
			recommendGoods.value = [];
		}
		try {
			const staffProfile = await fetchMerchantStaffProfile({ silentError: true });
			merchantStaff.value = isMerchantStaffActive(staffProfile);
		} catch {
			merchantStaff.value = false;
		}
		try {
			const data = await fetchApplyProgress();
			merchantApproved.value = canAccessMerchantWorkbench(data.summary?.status || data.status);
		} catch (error) {
			merchantApproved.value = false;
		}
	};

	const loadServiceCenterRoute = async () => {
		try {
			const config = await fetchServiceConfigRemote();
			serviceCenterAction.value = resolveServiceCenterAction(config);
		} catch (error) {
			serviceCenterAction.value = { type: 'navigate', url: '/packageUser/service', needLogin: false };
		}
	};

	const refreshPage = async () => {
		syncSession();
		await Promise.all([loadAssets(), loadServiceCenterRoute()]);
		uni.stopPullDownRefresh();
	};

	onShow(() => {
		refreshPage();
	});

	onPullDownRefresh(() => {
		refreshPage();
	});

	const goMessages = () => {
		if (!ensureLogin('/packageContent/message')) return;
		uni.navigateTo({
			url: '/packageContent/message?tab=service'
		});
	};

	const goProfile = () => {
		if (!ensureLogin('/pages/my/my')) return;
		uni.navigateTo({
			url: '/packageUser/profile'
		});
	};

	const handleAsset = (url) => {
		if (!ensureLogin(url)) return;
		uni.navigateTo({
			url
		});
	};

	const goOrderList = () => {
		if (!ensureLogin('/packageOrder/list')) return;
		uni.navigateTo({
			url: '/packageOrder/list'
		});
	};

	const goOrder = (status) => {
		if (!ensureLogin(`/packageOrder/list?status=${status}`)) return;
		uni.navigateTo({
			url: `/packageOrder/list?status=${status}`
		});
	};

	const handleService = (item) => {
		if (item.serviceCenter) {
			const action = serviceCenterAction.value;
			if (action.type === 'phone') {
				uni.makePhoneCall({ phoneNumber: action.phoneNumber });
				return;
			}
			if (action.needLogin && !ensureLogin(action.url)) return;
			uni.navigateTo({ url: action.url });
			return;
		}
		if (item.needLogin && !ensureLogin(item.url)) return;
		uni.navigateTo({
			url: item.url
		});
	};

	const goDetail = (id) => {
		uni.navigateTo({
			url: `/packageGoods/detail?id=${id}`
		});
	};
</script>

<style scoped>
	.page {
		min-height: 100vh;
		background: linear-gradient(180deg, #67bf0f 0%, #84d626 35%, #eef3e9 35%, #f4f4f4 100%);
	}

	.hero-panel {
		padding: calc(34rpx + env(safe-area-inset-top)) 28rpx 42rpx;
		background: linear-gradient(180deg, #63bc0d 0%, #7fd325 56%, #92dd34 100%);
	}

	.hero-main {
		position: relative;
	}

	.user-row,
	.asset-panel,
	.section-head,
	.order-link,
	.status-grid,
	.recommend-head,
	.recommend-price-row {
		display: flex;
	}

	.nickname,
	.asset-value,
	.asset-label,
	.section-title,
	.section-link-text,
	.status-label,
	.service-label,
	.recommend-title,
	.recommend-name,
	.recommend-price,
	.recommend-unit {
		display: block;
	}

	.hero-actions {
		display: flex;
		flex-direction: row;
		align-items: center;
		gap: 12rpx;
		position: absolute;
		top: 4rpx;
		right: 2rpx;
	}

	.message-entry {
		display: flex;
		position: relative;
		width: 64rpx;
		height: 64rpx;
		align-items: center;
		justify-content: center;
		border-radius: 50%;
		background: rgba(255, 255, 255, 0.12);
	}

	.message-badge {
		position: absolute;
		top: -4rpx;
		right: -4rpx;
		min-width: 28rpx;
		height: 28rpx;
		padding: 0 8rpx;
		line-height: 28rpx;
		border-radius: 14rpx;
		font-size: 18rpx;
		font-weight: 700;
		color: #ffffff;
		text-align: center;
		background: #ff5d57;
	}

	.user-row {
		align-items: center;
		padding-right: 88rpx;
	}

	.avatar {
		width: 110rpx;
		height: 110rpx;
		border-radius: 50%;
		background: rgba(255, 255, 255, 0.28);
		border: 4rpx solid rgba(255, 255, 255, 0.22);
		flex-shrink: 0;
	}

	.user-main-info {
		flex: 1;
		margin-left: 22rpx;
		min-width: 0;
	}

	.nickname {
		font-size: 44rpx;
		font-weight: 700;
		line-height: 1.2;
		color: #ffffff;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
		max-width: 100%;
	}

	.asset-panel {
		justify-content: space-between;
		margin-top: 42rpx;
	}

	.asset-item {
		flex: 1;
		text-align: center;
	}

	.asset-value {
		font-size: 44rpx;
		font-weight: 700;
		line-height: 1.15;
		color: #ffffff;
	}

	.asset-label {
		margin-top: 8rpx;
		font-size: 22rpx;
		line-height: 1.4;
		color: rgba(255, 255, 255, 0.92);
	}

	.content-wrap {
		margin-top: -6rpx;
		padding: 30rpx 22rpx calc(34rpx + var(--native-tabbar-offset));
		border-top-left-radius: 56rpx;
		border-top-right-radius: 56rpx;
		background: #f4f4f4;
	}

	.content-card {
		padding: 26rpx;
		border-radius: 30rpx;
		background: #FFFFFF;
	}

	.tool-section {
		margin-top: 22rpx;
	}

	.section-head {
		align-items: center;
		justify-content: space-between;
		margin-bottom: 20rpx;
	}

	.section-title {
		font-size: 30rpx;
		font-weight: 600;
		color: #1f2329;
	}

	.order-link {
		align-items: center;
		gap: 4rpx;
	}

	.section-link-text {
		font-size: 24rpx;
		color: #a4abb2;
	}

	.status-grid {
		justify-content: space-around;
		gap: 8rpx 0;
	}

	.status-item {
		position: relative;
		width: 20%;
		flex: 0 0 20%;
		text-align: center;
	}

	.status-icon {
		display: flex;
		align-items: center;
		justify-content: center;
		width: 66rpx;
		height: 66rpx;
		margin: 0 auto;
	}

	.status-label {
		margin-top: 8rpx;
		font-size: 22rpx;
		color: #6f7680;
	}

	.service-head {
		margin-bottom: 8rpx;
	}

	.service-grid {
		display: grid;
		grid-template-columns: repeat(4, minmax(0, 1fr));
		gap: 30rpx 8rpx;
	}

	.service-item {
		text-align: center;
	}

	.service-icon {
		display: flex;
		align-items: center;
		justify-content: center;
		width: 72rpx;
		height: 72rpx;
		margin: 0 auto;
		border-radius: 18rpx;
	}

	.service-label {
		margin-top: 12rpx;
		font-size: 22rpx;
		line-height: 1.35;
		color: #626a73;
	}

	.recommend-section {
		margin-top: 26rpx;
	}

	.recommend-head {
		align-items: center;
		margin-bottom: 18rpx;
	}

	.recommend-mark {
		width: 6rpx;
		height: 30rpx;
		margin-right: 12rpx;
		border-radius: 3rpx;
		background: #7ed321;
	}

	.recommend-title {
		font-size: 30rpx;
		font-weight: 800;
		color: #1f2329;
	}

	.recommend-grid {
		display: grid;
		grid-template-columns: repeat(2, minmax(0, 1fr));
		gap: 18rpx;
	}

	.recommend-item {
		overflow: hidden;
		border-radius: 20rpx;
		background: #ffffff;
	}

	.recommend-image {
		width: 100%;
		height: 220rpx;
		background: #e9edf1;
	}

	.recommend-name {
		padding: 14rpx 14rpx 0;
		font-size: 24rpx;
		font-weight: 600;
		line-height: 1.4;
		color: #22262c;
	}

	.recommend-price-row {
		align-items: baseline;
		padding: 10rpx 14rpx 16rpx;
	}

	.recommend-price {
		font-size: 28rpx;
		font-weight: 700;
		color: #59b50e;
	}

	.recommend-unit {
		margin-left: 4rpx;
		font-size: 20rpx;
		color: #9aa1a9;
	}
</style>
