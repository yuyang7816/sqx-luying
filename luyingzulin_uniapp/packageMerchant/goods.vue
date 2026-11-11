<template>
	<view class="page">
		<view class="sticky-top">
			<view class="toolbar">
				<view class="tabs-wrap">
					<u-tabs
						:list="merchantGoodsTabs"
						keyName="label"
						:current="currentTabIndex"
						:scrollable="true"
						lineColor="#7ed321"
						:activeStyle="tabActiveStyle"
						:inactiveStyle="tabInactiveStyle"
						@change="handleTabChange"
					/>
				</view>
				<button class="add-btn" @click="goCreate">新增商品</button>
			</view>
		</view>

		<view class="page-body">
			<view v-if="loading" class="card empty-card"><text class="empty-desc">加载中...</text></view>

			<view v-for="item in merchantGoods" :key="item.id" class="card goods-card">
				<view class="goods-main">
					<image
						class="goods-cover"
						:src="resolveImageUrl(item.coverImage || item.images?.[0])"
						mode="aspectFill"
					/>
					<view class="goods-info">
						<view class="goods-head">
							<text class="goods-name">{{ item.name }}</text>
							<text class="goods-tag" :class="item.statusType">{{ item.status }}</text>
						</view>
						<text class="goods-meta">¥{{ item.price }}/天 · 库存 {{ item.stock }}</text>
						<text class="goods-meta">{{ item.category }}</text>
					</view>
				</view>
				<view class="action-row">
					<text class="action-link" @click="goEdit(item.id)">编辑</text>
					<text class="action-link" @click="openStockPopup(item)">调库存</text>
					<text class="action-link" @click="toggleShelf(item)">{{ item.shelfStatus ? '下架' : '上架' }}</text>
					<text class="action-link danger" @click="confirmDelete(item)">删除</text>
				</view>
			</view>

			<view v-if="!loading && !merchantGoods.length" class="card empty-card">
				<up-icon name="grid" size="36" color="#b0b8a8" />
				<text class="empty-title">暂无商品</text>
				<text class="empty-desc">{{ emptyDesc }}</text>
				<button class="empty-btn" @click="goCreate">新增商品</button>
			</view>
		</view>

		<u-popup :show="stockPopupVisible" mode="bottom" round="16" safeAreaInsetBottom @close="closeStockPopup">
			<view class="stock-popup" v-if="stockTarget">
				<view class="popup-head">
					<text class="popup-title">调整库存</text>
					<view class="popup-close" @click="closeStockPopup">
						<up-icon name="close" size="18" color="#8a9a7a"></up-icon>
					</view>
				</view>

				<view class="popup-goods">
					<text class="popup-goods-name">{{ stockTarget.name }}</text>
					<text class="popup-goods-stock">当前库存 {{ stockTarget.stock }}</text>
				</view>

				<view class="tab-wrap">
					<u-tabs
						:list="stockActions"
						keyName="label"
						:current="stockActionIndex"
						:scrollable="false"
						lineColor="#7ed321"
						:activeStyle="tabActiveStyle"
						:inactiveStyle="tabInactiveStyle"
						@change="handleStockActionChange"
					/>
				</view>

				<view class="stepper-row">
					<view class="step-btn" @click="changeStockCount(-1)">
						<up-icon name="minus" size="18" color="#1a2114"></up-icon>
					</view>
					<text class="step-value">{{ stockCount }}</text>
					<view class="step-btn" @click="changeStockCount(1)">
						<up-icon name="plus" size="18" color="#1a2114"></up-icon>
					</view>
				</view>

				<view class="result-row">
					<text class="result-label">调整后库存</text>
					<text class="result-value">{{ nextStock }}</text>
				</view>

				<textarea
					v-model="stockRemark"
					class="textarea"
					maxlength="100"
					placeholder="调整原因（选填），例如补货、损耗"
				></textarea>

				<button class="popup-submit-btn" :loading="stockSubmitting" :disabled="stockSubmitting" @click="submitStockAdjust">
					确认调整
				</button>
			</view>
		</u-popup>
	</view>
</template>

<script setup>
	import { computed, ref } from 'vue';
	import { onShow } from '@dcloudio/uni-app';
	import { ensureLogin } from '@/common/auth';
	import {
		adjustGoodsStock,
		ensureMerchantOperational,
		deleteGoods,
		fetchGoodsPage,
		guardMerchantOperational,
		toggleGoodsShelf
	} from '@/common/api/merchant';
	import { MERCHANT_GOODS_TABS } from '@/common/merchant/constants';
	import { resolveDisplayImageUrl } from '@/common/upload';

	const merchantGoodsTabs = MERCHANT_GOODS_TABS;
	const stockActions = [
		{ key: 'add', label: '补充库存' },
		{ key: 'reduce', label: '扣减库存' }
	];
	const activeTab = ref('all');
	const merchantGoods = ref([]);
	const loading = ref(false);
	const deletingId = ref('');
	const stockPopupVisible = ref(false);
	const stockTarget = ref(null);
	const stockActionType = ref('add');
	const stockCount = ref(1);
	const stockRemark = ref('');
	const stockSubmitting = ref(false);
	const tabActiveStyle = { color: '#59b50e', fontWeight: '700' };
	const tabInactiveStyle = { color: '#6b7280' };
	const currentTabIndex = computed(() =>
		Math.max(merchantGoodsTabs.findIndex((item) => item.key === activeTab.value), 0)
	);
	const stockActionIndex = computed(() =>
		Math.max(stockActions.findIndex((item) => item.key === stockActionType.value), 0)
	);
	const nextStock = computed(() => {
		if (!stockTarget.value) return 0;
		const current = Number(stockTarget.value.stock) || 0;
		if (stockActionType.value === 'add') return current + stockCount.value;
		return Math.max(0, current - stockCount.value);
	});
	const emptyDesc = computed(() => {
		if (activeTab.value === 'on') return '暂无上架中的商品，可新增或上架已有商品';
		if (activeTab.value === 'off') return '暂无已下架商品';
		return '点击新增商品，提交后将自动上架';
	});

	const resolveImageUrl = (src) => resolveDisplayImageUrl(src) || '/static/logo.png';

	const resolveStatusFilter = () => {
		if (activeTab.value === 'on') return 1;
		if (activeTab.value === 'off') return 0;
		return undefined;
	};

	const resetStockForm = () => {
		stockActionType.value = 'add';
		stockCount.value = 1;
		stockRemark.value = '';
	};

	const openStockPopup = async (item) => {
		if (!(await guardMerchantOperational({ toast: true }))) return;
		stockTarget.value = item;
		resetStockForm();
		stockPopupVisible.value = true;
	};

	const closeStockPopup = () => {
		stockPopupVisible.value = false;
		stockTarget.value = null;
		resetStockForm();
	};

	const changeStockCount = (step) => {
		stockCount.value = Math.max(1, stockCount.value + step);
	};

	const handleStockActionChange = (item) => {
		stockActionType.value = item.key;
	};

	const submitStockAdjust = async () => {
		if (!stockTarget.value || stockSubmitting.value) return;
		stockSubmitting.value = true;
		try {
			await adjustGoodsStock({
				goodsId: stockTarget.value.id,
				adjustType: stockActionType.value === 'add' ? 'in' : 'out',
				quantity: stockCount.value,
				remark: stockRemark.value
			});
			uni.showToast({ title: '库存调整已记录', icon: 'success' });
			closeStockPopup();
			loadGoods();
		} catch (error) {
			uni.showToast({ title: error?.msg || error?.message || '调整失败', icon: 'none' });
		} finally {
			stockSubmitting.value = false;
		}
	};

	const goCreate = async () => {
		if (!ensureLogin('/packageMerchant/goods-edit?mode=create')) return;
		if (!(await guardMerchantOperational({ toast: true }))) return;
		uni.navigateTo({ url: '/packageMerchant/goods-edit?mode=create' });
	};

	const goEdit = (id) => uni.navigateTo({ url: `/packageMerchant/goods-edit?id=${id}` });

	const loadGoods = async () => {
		if (!ensureLogin('/packageMerchant/goods')) return;
		loading.value = true;
		try {
			await ensureMerchantOperational({ redirect: true, toast: true });
			const page = await fetchGoodsPage({
				page: 1,
				limit: 50,
				status: resolveStatusFilter()
			});
			merchantGoods.value = page.list || [];
		} catch (error) {
			uni.showToast({ title: error?.msg || error?.message || '加载商品失败', icon: 'none' });
		} finally {
			loading.value = false;
		}
	};

	const handleTabChange = (item) => {
		activeTab.value = item.key;
		loadGoods();
	};

	const toggleShelf = async (item) => {
		try {
			await toggleGoodsShelf(item.id, !item.shelfStatus);
			uni.showToast({ title: item.shelfStatus ? '已下架' : '已上架', icon: 'success' });
			loadGoods();
		} catch (error) {
			uni.showToast({ title: error?.msg || error?.message || '操作失败', icon: 'none' });
		}
	};

	const confirmDelete = (item) => {
		if (deletingId.value) return;
		uni.showModal({
			title: '删除商品',
			content: `确定删除「${item.name}」吗？删除后不可恢复。`,
			confirmText: '删除',
			confirmColor: '#e05c5c',
			success: async (res) => {
				if (!res.confirm) return;
				deletingId.value = item.id;
				try {
					await deleteGoods(item.id);
					uni.showToast({ title: '已删除', icon: 'success' });
					loadGoods();
				} catch (error) {
					uni.showToast({ title: error?.msg || error?.message || '删除失败', icon: 'none' });
				} finally {
					deletingId.value = '';
				}
			}
		});
	};

	onShow(loadGoods);
</script>

<style scoped>
	.page {
		box-sizing: border-box;
		min-height: 100vh;
		background: #f6f8f3;
		overflow-x: hidden;
	}

	.sticky-top {
		position: sticky;
		top: 0;
		z-index: 100;
		padding: 20rpx 28rpx 12rpx;
		background: #f6f8f3;
	}

	.toolbar {
		display: flex;
		align-items: center;
		gap: 16rpx;
		padding: 12rpx 12rpx 12rpx 8rpx;
		border-radius: 28rpx;
		background: #ffffff;
		box-shadow: 0 6rpx 24rpx rgba(0, 0, 0, 0.04);
	}

	.tabs-wrap {
		flex: 1;
		min-width: 0;
	}

	.tabs-wrap :deep(.u-tabs__wrapper__nav__item) {
		padding: 0 16rpx;
	}

	.tabs-wrap :deep(.u-tabs__wrapper__nav__item__text) {
		font-size: 26rpx;
		white-space: nowrap;
	}

	.add-btn {
		flex-shrink: 0;
		min-width: 0;
		width: auto;
		padding: 0 24rpx;
		height: 68rpx;
		line-height: 68rpx;
		margin: 0;
		border-radius: 34rpx;
		font-size: 24rpx;
		font-weight: 600;
		color: #ffffff;
		background: linear-gradient(135deg, #7ed321, #6bc01a);
		box-shadow: 0 4rpx 16rpx rgba(126, 211, 33, 0.3);
		border: none;
	}

	.page-body {
		padding: 0 28rpx 28rpx;
	}

	.card {
		box-sizing: border-box;
		width: 100%;
		margin-bottom: 28rpx;
		padding: 32rpx;
		border-radius: 32rpx;
		background: #ffffff;
		box-shadow: 0 6rpx 24rpx rgba(0, 0, 0, 0.04);
		overflow: hidden;
	}

	.goods-main {
		display: flex;
		gap: 20rpx;
	}

	.goods-cover {
		flex-shrink: 0;
		width: 144rpx;
		height: 144rpx;
		border-radius: 20rpx;
		background: #f4f6f2;
	}

	.goods-info {
		flex: 1;
		min-width: 0;
	}

	.goods-head {
		display: flex;
		justify-content: space-between;
		align-items: flex-start;
		gap: 16rpx;
	}

	.goods-name,
	.goods-tag,
	.goods-meta,
	.empty-title,
	.empty-desc {
		display: block;
	}

	.goods-name {
		font-size: 30rpx;
		font-weight: 700;
		color: #1a2114;
		line-height: 1.4;
		flex: 1;
		min-width: 0;
		display: -webkit-box;
		-webkit-line-clamp: 2;
		-webkit-box-orient: vertical;
		overflow: hidden;
	}

	.goods-meta {
		margin-top: 10rpx;
		font-size: 24rpx;
		color: #8a9a7a;
		line-height: 1.5;
	}

	.goods-tag {
		flex-shrink: 0;
		padding: 8rpx 18rpx;
		border-radius: 20rpx;
		font-size: 20rpx;
		font-weight: 600;
	}

	.goods-tag.online {
		color: #5cb816;
		background: #f6ffea;
		border: 1rpx solid rgba(126, 211, 33, 0.15);
	}

	.goods-tag.offline {
		color: #8a9a7a;
		background: #f1f4ec;
		border: 1rpx solid #e4e9dd;
	}

	.action-row {
		display: flex;
		flex-wrap: wrap;
		gap: 16rpx 24rpx;
		margin-top: 24rpx;
		padding-top: 20rpx;
		border-top: 1rpx solid #f1f4ec;
	}

	.action-link {
		padding: 10rpx 20rpx;
		border-radius: 20rpx;
		font-size: 24rpx;
		color: #5cb816;
		background: #f6ffea;
		font-weight: 600;
	}

	.action-link.danger {
		color: #e05c5c;
		background: #fff0f0;
	}

	.empty-card {
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		padding: 80rpx 40rpx;
	}

	.empty-title {
		margin-top: 16rpx;
		font-size: 30rpx;
		font-weight: 700;
		color: #1a2114;
		text-align: center;
	}

	.empty-desc {
		margin-top: 14rpx;
		font-size: 24rpx;
		line-height: 1.7;
		color: #8a9a7a;
		text-align: center;
	}

	.empty-btn {
		margin-top: 28rpx;
		padding: 0 40rpx;
		height: 76rpx;
		line-height: 76rpx;
		border-radius: 38rpx;
		font-size: 26rpx;
		font-weight: 600;
		color: #ffffff;
		background: linear-gradient(135deg, #7ed321, #6bc01a);
		border: none;
	}

	.stock-popup {
		box-sizing: border-box;
		padding: 32rpx 32rpx calc(32rpx + env(safe-area-inset-bottom));
		background: #ffffff;
	}

	.popup-head {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 20rpx;
	}

	.popup-title {
		font-size: 32rpx;
		font-weight: 700;
		color: #1a2114;
	}

	.popup-close {
		display: flex;
		align-items: center;
		justify-content: center;
		width: 56rpx;
		height: 56rpx;
		border-radius: 50%;
		background: #f4f6f2;
	}

	.popup-goods {
		margin-top: 24rpx;
		padding: 24rpx;
		border-radius: 20rpx;
		background: #f6f8f3;
	}

	.popup-goods-name,
	.popup-goods-stock {
		display: block;
	}

	.popup-goods-name {
		font-size: 28rpx;
		font-weight: 700;
		color: #1a2114;
		line-height: 1.5;
	}

	.popup-goods-stock {
		margin-top: 8rpx;
		font-size: 24rpx;
		color: #8a9a7a;
	}

	.tab-wrap {
		margin-top: 24rpx;
	}

	.stepper-row {
		display: flex;
		align-items: center;
		justify-content: center;
		gap: 20rpx;
		margin-top: 28rpx;
	}

	.step-btn {
		display: flex;
		align-items: center;
		justify-content: center;
		width: 88rpx;
		height: 88rpx;
		border-radius: 50%;
		background: #f1f4ec;
	}

	.step-value {
		min-width: 220rpx;
		height: 88rpx;
		line-height: 88rpx;
		text-align: center;
		font-size: 36rpx;
		font-weight: 800;
		color: #1a2114;
		background: #fff8e6;
		border-radius: 24rpx;
	}

	.result-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-top: 24rpx;
		padding: 0 8rpx;
		font-size: 26rpx;
	}

	.result-label {
		color: #8a9a7a;
	}

	.result-value {
		font-weight: 700;
		color: #59b50e;
	}

	.textarea {
		box-sizing: border-box;
		display: block;
		width: 100%;
		height: 140rpx;
		margin-top: 24rpx;
		padding: 20rpx 24rpx;
		border-radius: 20rpx;
		font-size: 26rpx;
		line-height: 1.6;
		color: #1a2114;
		background: #f6f8f3;
	}

	.popup-submit-btn {
		box-sizing: border-box;
		width: 100%;
		margin-top: 28rpx;
		height: 92rpx;
		line-height: 92rpx;
		border-radius: 46rpx;
		font-size: 30rpx;
		font-weight: 600;
		color: #ffffff;
		background: linear-gradient(135deg, #7ed321, #6bc01a);
		box-shadow: 0 8rpx 28rpx rgba(126, 211, 33, 0.35);
		border: none;
	}
</style>
