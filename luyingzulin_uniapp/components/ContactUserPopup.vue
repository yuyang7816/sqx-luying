<template>
	<up-popup
		:show="show"
		mode="bottom"
		round="16"
		:closeOnClickOverlay="true"
		safeAreaInsetBottom
		@close="handleClose"
	>
		<view class="contact-popup">
			<view class="popup-head">
				<text class="popup-title">联系用户</text>
				<text v-if="displayName" class="popup-sub">{{ displayName }}</text>
			</view>

			<view class="action-row" @click="handleIm">
				<view class="action-icon im">
					<up-icon name="chat" size="22" color="#5f8c00"></up-icon>
				</view>
				<view class="action-info">
					<text class="action-title">在线 IM 联系</text>
					<text class="action-desc">实时文字沟通，快速回复用户</text>
				</view>
				<up-icon name="arrow-right" size="14" color="#b0b8a8"></up-icon>
			</view>

			<view v-if="displayPhone" class="action-row" @click="handleCall">
				<view class="action-icon phone">
					<up-icon name="phone" size="22" color="#5f8c00"></up-icon>
				</view>
				<view class="action-info">
					<text class="action-title">拨打电话</text>
					<text class="action-desc">{{ displayPhone }}</text>
				</view>
				<up-icon name="arrow-right" size="14" color="#b0b8a8"></up-icon>
			</view>

			<view class="cancel-btn" @click="handleClose">取消</view>
		</view>
	</up-popup>
</template>

<script setup>
	import { computed, ref, watch } from 'vue';
	import { fetchOrderContact } from '@/common/api/merchant';

	const props = defineProps({
		show: { type: Boolean, default: false },
		orderId: { type: [String, Number], default: '' },
		fallback: {
			type: Object,
			default: () => ({})
		}
	});

	const emit = defineEmits(['update:show']);

	const contact = ref({});

	const displayName = computed(() => contact.value.userName || props.fallback.userName || props.fallback.user || '');
	const displayPhone = computed(() => contact.value.contactPhone || props.fallback.contactPhone || props.fallback.phone || '');
	const displayUserId = computed(() => contact.value.userId || props.fallback.userId || '');

	const loadContact = async () => {
		if (!props.orderId) {
			contact.value = { ...props.fallback };
			return;
		}
		try {
			contact.value = await fetchOrderContact(props.orderId);
		} catch {
			contact.value = { ...props.fallback };
		}
	};

	watch(
		() => props.show,
		(visible) => {
			if (visible) {
				loadContact();
			}
		}
	);

	const handleClose = () => {
		emit('update:show', false);
	};

	const openImChat = () => {
		const userId = displayUserId.value;
		if (!userId) {
			uni.showToast({ title: '暂无法发起在线联系', icon: 'none' });
			return;
		}
		const userName = encodeURIComponent(displayName.value || '用户');
		handleClose();
		uni.navigateTo({
			url: `/packageMerchant/im-chat?userId=${userId}&userName=${userName}`
		});
	};

	const handleIm = () => {
		openImChat();
	};

	const handleCall = () => {
		const phone = displayPhone.value;
		if (!phone) {
			uni.showToast({ title: '暂无用户电话', icon: 'none' });
			return;
		}
		handleClose();
		uni.makePhoneCall({ phoneNumber: String(phone) });
	};
</script>

<style scoped>
	.contact-popup {
		padding: 28rpx 28rpx calc(16rpx + env(safe-area-inset-bottom));
		background: #ffffff;
	}

	.popup-head {
		margin-bottom: 20rpx;
	}

	.popup-title,
	.popup-sub,
	.action-title,
	.action-desc {
		display: block;
	}

	.popup-title {
		font-size: 30rpx;
		font-weight: 700;
		color: #1a2114;
	}

	.popup-sub {
		margin-top: 6rpx;
		font-size: 22rpx;
		color: #8a9a7a;
	}

	.action-row {
		display: flex;
		align-items: center;
		gap: 16rpx;
		margin-top: 12rpx;
		padding: 22rpx 20rpx;
		border-radius: 20rpx;
		background: linear-gradient(180deg, #fbfef5 0%, #f6fae8 100%);
		border: 1rpx solid rgba(228, 235, 216, 0.92);
	}

	.action-row:active {
		opacity: 0.88;
	}

	.action-icon {
		display: flex;
		align-items: center;
		justify-content: center;
		width: 56rpx;
		height: 56rpx;
		border-radius: 16rpx;
		flex-shrink: 0;
	}

	.action-icon.im {
		background: linear-gradient(180deg, #eef8d5 0%, #e3f4c0 100%);
	}

	.action-icon.phone {
		background: linear-gradient(180deg, #f0f8e8 0%, #e6f2d8 100%);
	}

	.action-info {
		flex: 1;
		min-width: 0;
	}

	.action-title {
		font-size: 26rpx;
		font-weight: 600;
		color: #1a2114;
	}

	.action-desc {
		margin-top: 4rpx;
		font-size: 20rpx;
		line-height: 1.45;
		color: #8a9a7a;
		word-break: break-all;
	}

	.cancel-btn {
		display: flex;
		align-items: center;
		justify-content: center;
		height: 84rpx;
		margin-top: 20rpx;
		border-radius: 42rpx;
		font-size: 26rpx;
		font-weight: 600;
		color: #66775b;
		background: #f3f6f0;
	}

	.cancel-btn:active {
		opacity: 0.88;
	}
</style>
