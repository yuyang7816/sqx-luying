<template>
	<view class="page">
		<view class="hero-card">
			<text class="hero-badge">员工中心</text>
			<text class="hero-title">{{ profile.storeName || profile.merchantName || '门店核销' }}</text>
			<text class="hero-desc">{{ profile.name }} · {{ profile.role || '核销员' }}</text>
		</view>

		<view class="card scan-card">
			<text class="scan-title">快速核销</text>
			<text class="scan-desc">扫描用户取货凭证，或输入提货码完成核销。</text>
			<view class="scan-actions">
				<button class="scan-btn ghost" @click="scanPickupCode">扫码核销</button>
				<button class="scan-btn primary" @click="openPickupInput">输入提货码</button>
			</view>
		</view>

		<view class="section-head">
			<view class="section-title-wrap">
				<view class="section-mark"></view>
				<text class="section-title">待核销订单</text>
			</view>
			<text class="section-count">{{ pendingOrders.length }} 单</text>
		</view>

		<view v-for="item in pendingOrders" :key="item.id">
			<MerchantOrderCard :order="item" @click="goDetail(item.id)">
				<template #actions>
					<view class="ghost-btn" @click.stop="contactUser(item)">联系用户</view>
					<view class="primary-btn" @click.stop="goDetail(item.id)">扫码/输入核销</view>
				</template>
			</MerchantOrderCard>
		</view>

		<view v-if="!loading && !pendingOrders.length" class="card empty-card">
			<up-icon name="order" size="34" color="#a7b19f"></up-icon>
			<text class="empty-title">暂无待核销订单</text>
			<text class="empty-desc">用户取货凭证核销后会从此列表移除。</text>
		</view>
	</view>

	<view class="orders-fab" @click="goAllOrders">
		<up-icon name="list" size="22" color="#ffffff"></up-icon>
		<text class="fab-text">全部</text>
	</view>

	<ContactUserPopup
		v-model:show="contactPopupShow"
		:order-id="contactOrderId"
		:fallback="contactFallback"
	/>
</template>

<script setup>
	import { ref } from 'vue';
	import { onShow } from '@dcloudio/uni-app';
	import MerchantOrderCard from '@/components/MerchantOrderCard.vue';
	import ContactUserPopup from '@/components/ContactUserPopup.vue';
	import { runAction } from '@/common/actionFeedback';
	import { ensureLogin } from '@/common/auth';
	import {
		ensureMerchantOperational,
		ensureStaffWorkbenchAccess,
		fetchMerchantStaffProfile,
		fetchOrderPage,
		normalizePickupCode,
		pickupOrderByCode
	} from '@/common/api/merchant';

	const profile = ref({
		name: '',
		role: '',
		storeName: '',
		merchantName: ''
	});
	const pendingOrders = ref([]);
	const loading = ref(false);
	const contactPopupShow = ref(false);
	const contactOrderId = ref('');
	const contactFallback = ref({});

	const isPendingVerifyOrder = (item) => item.rawStatus === 'prepared' || item.nextAction === 'pickup';

	const loadPendingOrders = async () => {
		loading.value = true;
		try {
			await ensureMerchantOperational({ redirect: true, toast: true });
			const page = await fetchOrderPage({ tab: 'pending', page: 1, limit: 50 });
			pendingOrders.value = (page.list || []).filter(isPendingVerifyOrder);
		} catch (error) {
			pendingOrders.value = [];
			uni.showToast({ title: error?.msg || error?.message || '加载订单失败', icon: 'none' });
		} finally {
			loading.value = false;
		}
	};

	const loadPage = async () => {
		if (!ensureLogin('/packageMerchant/staff-center')) return;
		try {
			await ensureStaffWorkbenchAccess({ redirect: true, toast: true });
			profile.value = await fetchMerchantStaffProfile();
			await loadPendingOrders();
		} catch {
			setTimeout(() => {
				uni.navigateBack({ fail: () => uni.switchTab({ url: '/pages/my/my' }) });
			}, 400);
		}
	};

	onShow(loadPage);

	const goAllOrders = () => {
		uni.navigateTo({ url: '/packageMerchant/orders' });
	};

	const goDetail = (id) => {
		uni.navigateTo({ url: `/packageMerchant/order-detail?id=${encodeURIComponent(id)}` });
	};

	const verifyPickupCode = async (pickupCode) => {
		const code = normalizePickupCode(pickupCode);
		if (!code) {
			uni.showToast({ title: '请输入提货码', icon: 'none' });
			return;
		}
		const { ok, result } = await runAction({
			action: () => pickupOrderByCode(code),
			successText: '核销成功',
			errorText: '核销失败，请核对提货码后重试'
		});
		if (!ok) return;
		await loadPendingOrders();
		if (result?.id) {
			goDetail(result.id);
		}
	};

	const openPickupInput = () => {
		uni.showModal({
			title: '输入提货码',
			editable: true,
			placeholderText: '请输入 6 位提货码',
			success: (res) => {
				if (res.confirm) {
					verifyPickupCode(res.content);
				}
			}
		});
	};

	const scanPickupCode = () => {
		uni.scanCode({
			onlyFromCamera: false,
			success: (res) => verifyPickupCode(res.result),
			fail: () => uni.showToast({ title: '扫码已取消', icon: 'none' })
		});
	};

	const contactUser = (item) => {
		if (!item) return;
		contactOrderId.value = item.id;
		contactFallback.value = {
			userName: item.user,
			contactPhone: item.phone,
			userId: item.userId
		};
		contactPopupShow.value = true;
	};
</script>

<style scoped>
	.page {
		box-sizing: border-box;
		min-height: 100vh;
		padding: 20rpx 24rpx 44rpx;
		background: #f3f6f1;
		overflow-x: hidden;
	}

	.hero-card {
		box-sizing: border-box;
		padding: 28rpx;
		border-radius: 20rpx;
		background: linear-gradient(145deg, #337547 0%, #4e963d 100%);
		box-shadow: 0 10rpx 28rpx rgba(42, 98, 55, 0.18);
	}

	.hero-badge,
	.hero-title,
	.hero-desc,
	.scan-title,
	.scan-desc,
	.section-title,
	.section-count,
	.order-id,
	.order-user,
	.order-time,
	.order-tag,
	.empty-title,
	.empty-desc {
		display: block;
	}

	.hero-badge {
		display: inline-block;
		padding: 6rpx 16rpx;
		border-radius: 999rpx;
		font-size: 20rpx;
		font-weight: 600;
		color: #4a8f00;
		background: rgba(255, 255, 255, 0.92);
	}

	.hero-title {
		margin-top: 16rpx;
		font-size: 36rpx;
		font-weight: 700;
		line-height: 1.35;
		color: #ffffff;
	}

	.hero-desc {
		margin-top: 10rpx;
		font-size: 22rpx;
		line-height: 1.6;
		color: rgba(255, 255, 255, 0.88);
	}

	.card {
		box-sizing: border-box;
		margin-top: 20rpx;
		padding: 26rpx 24rpx;
		border-radius: 20rpx;
		background: #ffffff;
		box-shadow: 0 5rpx 20rpx rgba(31, 53, 35, 0.045);
	}

	.scan-card {
		background: #ffffff;
		border: none;
	}

	.scan-title {
		font-size: 30rpx;
		font-weight: 700;
		color: #1a2114;
	}

	.scan-desc {
		margin-top: 10rpx;
		font-size: 22rpx;
		line-height: 1.6;
		color: #8a9a7a;
	}

	.scan-actions {
		display: flex;
		gap: 16rpx;
		margin-top: 24rpx;
	}

	.scan-btn {
		flex: 1;
		height: 60rpx;
		line-height: 60rpx;
		border-radius: 12rpx;
		font-size: 23rpx;
		margin: 0;
	}

	.scan-btn.ghost {
		color: #59b50e;
		background: #f1f8df;
	}

	.scan-btn.primary {
		color: #ffffff;
		background: #4d9a34;
	}

	.section-head {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-top: 20rpx;
		padding: 0;
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

	.section-title {
		font-size: 27rpx;
		font-weight: 600;
		color: #1a2114;
		line-height: 1.2;
	}

	.section-count {
		font-size: 22rpx;
		font-weight: 500;
		color: #59b50e;
	}

	.empty-card {
		display: flex;
		flex-direction: column;
		align-items: center;
		padding: 48rpx 24rpx 24rpx;
	}

	.empty-title {
		margin-top: 16rpx;
		font-size: 28rpx;
		font-weight: 700;
		color: #1a2114;
	}

	.empty-desc {
		margin-top: 10rpx;
		font-size: 22rpx;
		color: #8a9a7a;
		text-align: center;
		line-height: 1.6;
	}

	.orders-fab {
		position: fixed;
		right: 28rpx;
		bottom: calc(48rpx + env(safe-area-inset-bottom));
		z-index: 100;
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		width: 96rpx;
		height: 96rpx;
		border-radius: 20rpx;
		background: #4d9a34;
		box-shadow: 0 10rpx 24rpx rgba(42, 98, 55, 0.24);
	}

	.fab-text {
		margin-top: 4rpx;
		font-size: 20rpx;
		font-weight: 600;
		color: #ffffff;
		line-height: 1;
	}
</style>
