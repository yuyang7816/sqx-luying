<template>
	<u-popup :show="show" mode="bottom" round="16" safeAreaInsetBottom @close="handleClose">
		<view class="payment-popup">
			<view class="popup-head">
				<text class="popup-title">选择支付方式</text>
			</view>
			<view
				v-for="item in paymentMethods"
				:key="item.key"
				class="payment-row"
				:class="{ active: payMethod === item.key }"
				@click="payMethod = item.key"
			>
				<view class="payment-main">
					<view class="payment-icon"><up-icon :name="item.icon" size="22" color="#5f8c00"></up-icon></view>
					<view class="payment-info">
						<text class="payment-title">{{ item.label }}</text>
						<text v-if="item.desc" class="payment-desc">{{ item.desc }}</text>
					</view>
				</view>
				<view class="payment-check">
					<up-icon
						:name="payMethod === item.key ? 'checkmark-circle-fill' : 'checkmark-circle'"
						size="18"
						:color="payMethod === item.key ? '#59b50e' : '#9aa2b1'"
					></up-icon>
				</view>
			</view>
			<button class="popup-submit-btn" :loading="loading" :disabled="loading || !payMethod" @click="handleConfirm">
				确认支付 ¥{{ displayAmount }}
			</button>
		</view>
	</u-popup>
</template>

<script setup>
	import { computed, ref, watch } from 'vue';
	import { getAvailablePaymentMethods } from '@/common/payment';

	const props = defineProps({
		show: { type: Boolean, default: false },
		amount: { type: [String, Number], default: '0.00' },
		loading: { type: Boolean, default: false }
	});

	const emit = defineEmits(['update:show', 'confirm']);

	const paymentMethods = ref([]);
	const payMethod = ref('');

	const displayAmount = computed(() => {
		const num = Number(props.amount || 0);
		return Number.isNaN(num) ? String(props.amount || '0.00') : num.toFixed(2);
	});

	const loadMethods = async () => {
		const methods = await getAvailablePaymentMethods();
		paymentMethods.value = methods;
		payMethod.value = methods[0]?.key || '';
	};

	watch(
		() => props.show,
		(visible) => {
			if (visible) {
				loadMethods();
			}
		}
	);

	const handleClose = () => {
		emit('update:show', false);
	};

	const handleConfirm = () => {
		if (!payMethod.value) {
			uni.showToast({ title: '请选择支付方式', icon: 'none' });
			return;
		}
		emit('confirm', payMethod.value);
	};
</script>

<style scoped>
	.payment-popup {
		padding: 28rpx 28rpx calc(24rpx + env(safe-area-inset-bottom));
		background: #fff;
	}

	.popup-head {
		margin-bottom: 16rpx;
	}

	.popup-title,
	.payment-title,
	.payment-desc {
		display: block;
	}

	.popup-title {
		font-size: 28rpx;
		font-weight: 600;
		color: #1a2114;
	}

	.payment-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 14rpx;
		margin-top: 12rpx;
		padding: 18rpx 20rpx;
		border-radius: 20rpx;
		background: #fbfef5;
		border: 1rpx solid rgba(228, 235, 216, 0.9);
	}

	.payment-row.active {
		background: linear-gradient(180deg, #f8fdea 0%, #eef8d5 100%);
		border-color: rgba(126, 211, 33, 0.42);
	}

	.payment-main {
		display: flex;
		align-items: center;
		flex: 1;
		min-width: 0;
	}

	.payment-icon {
		display: flex;
		align-items: center;
		justify-content: center;
		width: 56rpx;
		height: 56rpx;
		border-radius: 16rpx;
		background: linear-gradient(180deg, #f4fbe8 0%, #e9f7cf 100%);
		flex-shrink: 0;
	}

	.payment-info {
		flex: 1;
		min-width: 0;
		margin-left: 14rpx;
	}

	.payment-title {
		font-size: 24rpx;
		font-weight: 600;
		color: #1a2114;
	}

	.payment-desc {
		margin-top: 4rpx;
		font-size: 20rpx;
		line-height: 1.45;
		color: #8a9a7a;
	}

	.payment-check {
		display: flex;
		align-items: center;
		justify-content: center;
		flex-shrink: 0;
	}

	.popup-submit-btn {
		width: 100%;
		height: 80rpx;
		line-height: 80rpx;
		margin-top: 24rpx;
		border-radius: 40rpx;
		font-size: 26rpx;
		font-weight: 600;
		color: #fff;
		background: linear-gradient(135deg, #7ed321, #6bc01a);
		border: none;
	}
</style>
