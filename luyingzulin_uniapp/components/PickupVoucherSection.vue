<template>
	<view v-if="pickupView" class="pickup-voucher">
		<view class="hero-card">
			<text class="hero-title">到店取货凭证</text>
			<text class="hero-desc">{{ statusHint }}</text>
		</view>

		<view class="card qr-card">
			<text class="title">提货码</text>
			<view class="qr-box">
				<u-qrcode
					v-if="pickupView.pickupCodeRaw"
					:val="pickupView.pickupCodeRaw"
					:size="qrSize"
					unit="px"
					:show-loading="true"
					loading-text="生成中"
				/>
			</view>
			<view class="code-row">
				<text class="qr-code">{{ pickupView.pickupCode }}</text>
			</view>
			<text class="desc">{{ pickupView.verified ? '门店已核销，租期已开始' : '请向门店工作人员出示二维码或提货码完成核销' }}</text>
			<text v-if="pollingActive" class="desc polling">正在等待门店核销...</text>
		</view>
	</view>
</template>

<script setup>
	import { computed, onBeforeUnmount, ref, watch } from 'vue';
	import { refreshPickupView } from '@/common/api/order';

	const props = defineProps({
		orderId: { type: [String, Number], required: true },
		initialView: { type: Object, default: null },
		createTime: { type: String, default: '' }
	});
	const emit = defineEmits(['verified']);

	const pickupView = ref(props.initialView);
	const pollingActive = ref(false);
	const pollTimer = ref(null);
	const verifiedNotified = ref(false);
	const qrSize = 148;

	const statusHint = computed(() => {
		if (pickupView.value?.verified) {
			return '门店已核销取货，租期已开始，请妥善使用装备。';
		}
		return '请在预约时间内向门店工作人员出示提货码，待门店核销后即为取货成功。';
	});

	const clearPollTimer = () => {
		if (pollTimer.value) {
			clearInterval(pollTimer.value);
			pollTimer.value = null;
		}
		pollingActive.value = false;
	};

	const handleVerified = () => {
		if (verifiedNotified.value) return;
		verifiedNotified.value = true;
		clearPollTimer();
		uni.showToast({ title: '门店已核销，取货成功', icon: 'success' });
		emit('verified');
	};

	const applyPickupView = (view, { notifyOnVerify = false } = {}) => {
		const wasVerified = Boolean(pickupView.value?.verified);
		pickupView.value = view;
		if (view?.verified && !wasVerified && notifyOnVerify) {
			handleVerified();
		}
		if (view?.verified) {
			clearPollTimer();
		}
	};

	const loadPickup = async ({ silent = false } = {}) => {
		if (!props.orderId) return;
		try {
			const view = await refreshPickupView(props.orderId);
			applyPickupView(view);
		} catch (error) {
			if (!silent && !pickupView.value) {
				pickupView.value = null;
			}
		}
	};

	const pollPickupStatus = async () => {
		if (!props.orderId || pickupView.value?.verified) {
			clearPollTimer();
			return;
		}
		try {
			const view = await refreshPickupView(props.orderId);
			applyPickupView(view, { notifyOnVerify: true });
		} catch (error) {
			// 轮询失败不打断用户操作
		}
	};

	const startPolling = () => {
		clearPollTimer();
		if (pickupView.value?.verified) return;
		pollingActive.value = true;
		pollTimer.value = setInterval(() => {
			pollPickupStatus();
		}, 5000);
	};

	watch(() => props.initialView, (value) => {
		if (value) {
			applyPickupView(value);
			if (!value.verified) startPolling();
		}
	}, { immediate: true });

	watch(() => props.orderId, (value) => {
		if (!value) return;
		if (!pickupView.value) {
			loadPickup().then(() => startPolling());
		}
	}, { immediate: true });

	onBeforeUnmount(() => {
		clearPollTimer();
	});
</script>

<style scoped>
	.pickup-voucher { box-sizing: border-box; width: 100%; margin-bottom: 20rpx; overflow-x: hidden; }
	.hero-card { box-sizing: border-box; padding: 30rpx; border-radius: 30rpx; background: linear-gradient(135deg, #8ee226, #9de943); }
	.hero-title, .hero-desc, .title, .desc, .qr-code { display: block; }
	.hero-title { font-size: 38rpx; font-weight: 700; color: #ffffff; }
	.hero-desc { margin-top: 12rpx; font-size: 24rpx; line-height: 1.7; color: rgba(255,255,255,0.92); }
	.card { box-sizing: border-box; width: 100%; margin-top: 24rpx; padding: 28rpx; background: #fff; border-radius: 28rpx; overflow: hidden; }
	.title { text-align: center; font-size: 34rpx; font-weight: 700; color: #1f2937; }
	.qr-box { display: flex; align-items: center; justify-content: center; min-height: 300rpx; margin: 20rpx auto 12rpx; padding: 18rpx; border-radius: 20rpx; background: linear-gradient(135deg, #f2fde5, #fff8e6); }
	.code-row { display: flex; align-items: center; justify-content: center; gap: 12rpx; margin-top: 8rpx; }
	.qr-code { text-align: center; font-size: 42rpx; font-weight: 700; letter-spacing: 4rpx; color: #1f2937; }
	.desc { margin-top: 16rpx; text-align: center; font-size: 24rpx; line-height: 1.7; color: #6b7280; }
	.desc.polling { color: #59b50e; }
</style>
