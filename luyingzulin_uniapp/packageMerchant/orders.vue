<template>
	<view class="page">
		<view class="tab-panel">
			<u-tabs :list="merchantOrderTabs" keyName="label" :current="currentTabIndex" :scrollable="true" lineColor="#7ed321"
				:activeStyle="tabActiveStyle" :inactiveStyle="tabInactiveStyle" @change="handleTabChange"></u-tabs>
		</view>

		<view class="list-body">
			<view v-if="loading && !currentOrders.length" class="empty-card">
				<text class="empty-title">加载中...</text>
			</view>
			<view v-else-if="!currentOrders.length" class="empty-card">
				<up-icon name="order" size="34" color="#a7b19f"></up-icon>
				<text class="empty-title">当前没有相关订单</text>
				<text class="empty-desc">切换其它筛选查看，或等待用户下单与归还申请同步到商家端。</text>
			</view>

			<MerchantOrderCard
				v-for="item in currentOrders"
				:key="item.id"
				:order="item"
				@click="goDetail(item.id)"
			>
				<template #actions>
					<view class="ghost-btn" @click.stop="contactUser(item)">联系用户</view>
					<view
						v-if="item.nextAction === 'prepare'"
						class="primary-btn"
						:class="{ disabled: actionLoadingId === item.id }"
						@click.stop="handlePrepare(item)"
					>
						{{ actionLoadingId === item.id ? '处理中...' : '确认备货' }}
					</view>
					<view
						v-else-if="item.nextAction === 'pickup'"
						class="primary-btn"
						@click.stop="goDetail(item.id)"
					>
						扫码/输入核销
					</view>
					<view
						v-else-if="item.nextAction === 'inspection'"
						class="primary-btn"
						@click.stop="goInspection(item.id)"
					>
						录入验收结果
					</view>
					<view v-else class="plain-btn" @click.stop="goDetail(item.id)">查看详情</view>
				</template>
			</MerchantOrderCard>
		</view>

		<ContactUserPopup
			v-model:show="contactPopupShow"
			:order-id="contactOrderId"
			:fallback="contactFallback"
		/>
	</view>
</template>

<script setup>
	import { computed, ref } from 'vue';
	import { onLoad, onShow } from '@dcloudio/uni-app';
	import { runAction } from '@/common/actionFeedback';
	import MerchantOrderCard from '@/components/MerchantOrderCard.vue';
	import ContactUserPopup from '@/components/ContactUserPopup.vue';
	import { ensureLogin } from '@/common/auth';
	import {
		ensureMerchantOperational,
		executeMerchantOrderAction,
		fetchOrderPage,
		MERCHANT_ORDER_TABS
	} from '@/common/api/merchant';

	const merchantOrderTabs = MERCHANT_ORDER_TABS;
	const activeTab = ref('all');
	const currentOrders = ref([]);
	const loading = ref(false);
	const actionLoadingId = ref('');
	const contactPopupShow = ref(false);
	const contactOrderId = ref('');
	const contactFallback = ref({});
	const tabActiveStyle = { color: '#59b50e', fontWeight: '700' };
	const tabInactiveStyle = { color: '#6b7280' };
	const currentTabIndex = computed(() => Math.max(merchantOrderTabs.findIndex((item) => item.key === activeTab.value), 0));

	onLoad((options) => {
		const tab = String(options?.tab || '');
		if (tab && merchantOrderTabs.some((item) => item.key === tab)) {
			activeTab.value = tab;
		}
	});

	const loadOrders = async () => {
		if (!ensureLogin('/packageMerchant/orders')) return;
		loading.value = true;
		try {
			await ensureMerchantOperational({ redirect: true, toast: true });
			const page = await fetchOrderPage({ tab: activeTab.value, page: 1, limit: 50 });
			currentOrders.value = page.list || [];
		} catch (error) {
			currentOrders.value = [];
			uni.showToast({ title: error?.msg || error?.message || '加载订单失败', icon: 'none' });
		} finally {
			loading.value = false;
		}
	};

	onShow(loadOrders);

	const handleTabChange = (item) => {
		activeTab.value = item.key;
		loadOrders();
	};

	const goDetail = (id) => uni.navigateTo({ url: `/packageMerchant/order-detail?id=${encodeURIComponent(id)}` });

	const goInspection = (id) => {
		uni.navigateTo({ url: `/packageMerchant/inspection?id=${encodeURIComponent(id)}` });
	};

	const handlePrepare = async (item) => {
		if (actionLoadingId.value || !item) return;
		actionLoadingId.value = item.id;
		const { ok } = await runAction({
			action: () => executeMerchantOrderAction(item),
			successText: '备货完成',
			errorText: '订单处理失败，请稍后重试'
		});
		actionLoadingId.value = '';
		if (ok) {
			loadOrders();
		}
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
		background: #f6f8f3;
		overflow-x: hidden;
	}

	.tab-panel {
		padding: 20rpx 28rpx 12rpx;
		background: #ffffff;
		box-shadow: 0 2rpx 16rpx rgba(126, 211, 33, 0.06);
	}

	.tab-panel :deep(.u-tabs__wrapper__nav__item) {
		padding: 0 24rpx;
	}

	.tab-panel :deep(.u-tabs__wrapper__nav__item__text) {
		font-size: 26rpx;
		white-space: nowrap;
	}

	.list-body {
		box-sizing: border-box;
		padding: 28rpx 28rpx 40rpx;
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
</style>
