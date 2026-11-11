<template>
	<view class="page">
		<view v-if="loading" class="empty-card">
			<text class="empty-title">加载地址中...</text>
		</view>
		<view v-else-if="!addressList.length" class="empty-card">
			<up-icon name="map" size="34" color="#a7b19f"></up-icon>
			<text class="empty-title">还没有收货地址</text>
			<text class="empty-desc">新增常用地址后，确认订单时可以更快完成配送选择。</text>
		</view>
		<view v-for="item in addressList" :key="item.id" class="address-card" :class="{ selected: selectMode && selectedAddressId === item.id }" @click="handleCardClick(item)">
			<view class="card-head">
				<text class="name">{{ item.name }}</text>
				<text class="phone">{{ item.phone }}</text>
				<text v-if="item.default" class="default-tag">默认</text>
			</view>
			<text class="address">{{ item.address }}</text>
			<view v-if="!selectMode" class="card-actions">
				<view class="action" @click.stop="goEditAddress(item.id)"><up-icon name="edit-pen" size="18" color="#8a9a7a"></up-icon></view>
				<view class="action" :class="{ disabled: actionLoadingId === `delete-${item.id}` }" @click.stop="removeAddress(item.id)"><up-icon name="trash" size="18" color="#8a9a7a"></up-icon></view>
				<text v-if="!item.default" class="set-default" :class="{ disabled: actionLoadingId === `default-${item.id}` }" @click.stop="setDefault(item.id)">设为默认</text>
			</view>
		</view>
		<button class="add-btn" @click="goAddAddress">新增地址</button>
	</view>
</template>

<script setup>
	import { ref } from 'vue';
	import { onLoad, onShow } from '@dcloudio/uni-app';
	import { runAction } from '@/common/actionFeedback';
	import { deleteAddress, fetchAddressList, getSelectedAddress, setDefaultAddress, setSelectedAddress } from '@/common/api/user';
	import { ensureLogin } from '@/common/auth';

	const addressList = ref([]);
	const selectMode = ref(false);
	const selectedAddressId = ref(0);
	const actionLoadingId = ref('');
	const loading = ref(true);

	const syncAddressList = async () => {
		loading.value = true;
		try {
			addressList.value = await fetchAddressList();
			selectedAddressId.value = Number(getSelectedAddress()?.id || 0);
		} catch (error) {
			addressList.value = [];
			uni.showToast({ title: error.message || '地址加载失败', icon: 'none' });
		} finally {
			loading.value = false;
		}
	};

	onLoad((options) => {
		selectMode.value = options.select === '1';
	});

	onShow(() => {
		if (!ensureLogin(`/packageUser/address-list${selectMode.value ? '?select=1' : ''}`)) return;
		syncAddressList();
	});

	const goAddAddress = () => uni.navigateTo({ url: '/packageUser/address-edit' });
	const goEditAddress = (id) => uni.navigateTo({ url: `/packageUser/address-edit?id=${id}` });

	const setDefault = async (id) => {
		if (actionLoadingId.value) return;
		actionLoadingId.value = `default-${id}`;
		const { ok } = await runAction({
			action: () => setDefaultAddress(id),
			successText: '默认地址已更新',
			errorText: '默认地址更新失败，请稍后重试'
		});
		actionLoadingId.value = '';
		if (!ok) return;
		syncAddressList();
	};

	const removeAddress = async (id) => {
		if (actionLoadingId.value) return;
		if (addressList.value.length <= 1) {
			uni.showToast({ title: '请至少保留一个地址', icon: 'none' });
			return;
		}
		actionLoadingId.value = `delete-${id}`;
		const { ok } = await runAction({
			action: () => deleteAddress(id),
			successText: '地址已删除',
			errorText: '地址删除失败，请稍后重试'
		});
		actionLoadingId.value = '';
		if (!ok) return;
		syncAddressList();
	};

	const handleCardClick = (item) => {
		if (!selectMode.value) return;
		setSelectedAddress(item);
		selectedAddressId.value = item.id;
		uni.navigateBack();
	};
</script>

<style scoped>
	.page {
		box-sizing: border-box;
		min-height: 100vh;
		padding: 28rpx 28rpx 160rpx;
		background: #f6f8f3;
		overflow-x: hidden;
	}

	.empty-card {
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		padding: 120rpx 40rpx;
		margin-bottom: 28rpx;
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

	.address-card {
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

	.address-card.selected {
		background: #f6ffea;
		box-shadow: 0 8rpx 28rpx rgba(126, 211, 33, 0.14);
		border: 2rpx solid rgba(126, 211, 33, 0.25);
	}

	.card-head,
	.card-actions {
		display: flex;
		align-items: center;
	}

	.name,
	.phone,
	.default-tag,
	.address,
	.action,
	.set-default {
		display: block;
	}

	.action.disabled,
	.set-default.disabled {
		opacity: 0.45;
	}

	.card-head {
		flex-wrap: wrap;
		align-items: center;
	}

	.name {
		font-size: 32rpx;
		font-weight: 800;
		color: #1a2114;
	}

	.phone {
		margin-left: 16rpx;
		font-size: 26rpx;
		color: #8a9a7a;
		font-weight: 500;
	}

	.default-tag {
		margin-left: 16rpx;
		padding: 6rpx 16rpx;
		border-radius: 16rpx;
		font-size: 20rpx;
		color: #ffffff;
		background: linear-gradient(135deg, #7ed321, #6bc01a);
		font-weight: 600;
	}

	.address {
		margin-top: 16rpx;
		font-size: 24rpx;
		line-height: 1.7;
		color: #4a5540;
		font-weight: 500;
	}

	.card-actions {
		justify-content: flex-end;
		flex-wrap: wrap;
		margin-top: 24rpx;
		padding-top: 20rpx;
		border-top: 1rpx solid #f1f4ec;
	}

	.action {
		display: flex;
		align-items: center;
		justify-content: center;
		width: 56rpx;
		height: 56rpx;
		margin-left: 16rpx;
		border-radius: 50%;
		background: #f6f8f3;
	}

	.set-default {
		margin-left: 20rpx;
		padding: 10rpx 20rpx;
		border-radius: 20rpx;
		font-size: 24rpx;
		color: #5cb816;
		background: #f6ffea;
		font-weight: 600;
	}

	.add-btn {
		box-sizing: border-box;
		position: fixed;
		left: 28rpx;
		right: 28rpx;
		bottom: calc(28rpx + env(safe-area-inset-bottom));
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
