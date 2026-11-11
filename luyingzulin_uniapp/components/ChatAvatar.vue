<template>
	<view class="chat-avatar" :style="wrapStyle">
		<view v-if="showCsIcon" class="chat-avatar__icon chat-avatar__icon--cs">
			<up-icon name="kefu-ermai" :size="iconSize" color="#61b70f"></up-icon>
		</view>
		<image
			v-else-if="resolvedSrc"
			class="chat-avatar__image"
			:src="resolvedSrc"
			mode="aspectFill"
		></image>
		<view v-else-if="variant === 'merchant'" class="chat-avatar__icon chat-avatar__icon--shop">
			<up-icon name="home-fill" :size="iconSize" color="#7ed321"></up-icon>
		</view>
		<view v-else class="chat-avatar__icon chat-avatar__icon--user">
			<up-icon name="account-fill" :size="iconSize" color="#7ed321"></up-icon>
		</view>
	</view>
</template>

<script setup>
	import { computed } from 'vue';
	import { resolveAvatarUrl } from '@/common/constants/avatar';
	import { PLATFORM_CS_VARIANT } from '@/common/constants/serviceAvatar';

	const props = defineProps({
		src: {
			type: String,
			default: ''
		},
		variant: {
			type: String,
			default: 'user'
		},
		size: {
			type: [Number, String],
			default: 88
		}
	});

	const resolvedSrc = computed(() => {
		const value = String(props.src || '').trim();
		if (!value) return '';
		return resolveAvatarUrl(value);
	});

	const showCsIcon = computed(() => props.variant === PLATFORM_CS_VARIANT && !String(props.src || '').trim());

	const iconSize = computed(() => {
		const num = Number(props.size) || 88;
		return Math.max(18, Math.round(num * 0.42));
	});

	const wrapStyle = computed(() => {
		const num = Number(props.size) || 88;
		return {
			width: `${num}rpx`,
			height: `${num}rpx`
		};
	});
</script>

<style scoped>
	.chat-avatar {
		flex-shrink: 0;
		border-radius: 50%;
		overflow: hidden;
		background: #eef5e6;
	}

	.chat-avatar__image,
	.chat-avatar__icon {
		width: 100%;
		height: 100%;
	}

	.chat-avatar__icon {
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.chat-avatar__icon--cs {
		background: #eefadb;
	}

	.chat-avatar__icon--shop {
		background: #f3f8ec;
	}

	.chat-avatar__icon--user {
		background: #eef5f0;
	}
</style>
