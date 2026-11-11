<template>
	<view class="page">
		<view v-if="loading" class="state-box"><text class="state-text">加载会话...</text></view>
		<template v-else>
			<scroll-view scroll-y class="message-scroll" :scroll-into-view="scrollIntoView" scroll-with-animation>
				<view v-if="!messages.length" class="state-box empty">
					<text class="state-text">暂无消息，向商家发送咨询吧</text>
				</view>
				<view
					v-for="item in messages"
					:id="`msg-${item.id}`"
					:key="item.id"
					class="message-row"
					:class="item.senderRole === 'user' ? 'mine' : 'peer'"
				>
					<ChatAvatar
						class="message-avatar"
						:src="item.senderRole === 'user' ? selfAvatar : peerAvatar"
						:variant="item.senderRole === 'user' ? 'user' : 'merchant'"
						:size="72"
					/>
					<view class="message-body">
						<view class="bubble">
							<text class="bubble-text">{{ item.content }}</text>
						</view>
						<text class="message-time">{{ item.createTime }}</text>
					</view>
				</view>
			</scroll-view>

			<view class="input-bar">
				<input
					v-model="draft"
					class="input-field"
					confirm-type="send"
					maxlength="500"
					placeholder="输入消息..."
					@confirm="handleSend"
				/>
				<view class="send-btn" :class="{ disabled: sending || !draft.trim() }" @click="handleSend">发送</view>
			</view>
		</template>
	</view>
</template>

<script setup>
	import { nextTick, ref } from 'vue';
	import { onLoad, onShow } from '@dcloudio/uni-app';
	import { ensureLogin, getStoredSession } from '@/common/auth';
	import { DEFAULT_AVATAR_URL } from '@/common/constants/avatar';
	import {
		fetchImMessages,
		markImConversationRead,
		openMerchantConversation,
		sendImText
	} from '@/common/api/im';
	import ChatAvatar from '@/components/ChatAvatar.vue';

	const loading = ref(true);
	const sending = ref(false);
	const draft = ref('');
	const merchantId = ref('');
	const merchantName = ref('商家');
	const conversationId = ref(0);
	const messages = ref([]);
	const scrollIntoView = ref('');
	const selfAvatar = ref(DEFAULT_AVATAR_URL);
	const peerAvatar = ref('');

	const scrollToBottom = async () => {
		const last = messages.value[messages.value.length - 1];
		if (!last?.id) return;
		scrollIntoView.value = '';
		await nextTick();
		scrollIntoView.value = `msg-${last.id}`;
	};

	const loadMessages = async () => {
		if (!conversationId.value) return;
		const { list } = await fetchImMessages(conversationId.value, { page: 1, limit: 100 });
		messages.value = list || [];
		await scrollToBottom();
	};

	const initConversation = async () => {
		loading.value = true;
		try {
			const session = getStoredSession();
			selfAvatar.value = session?.userInfo?.avatar || DEFAULT_AVATAR_URL;
			const conversation = await openMerchantConversation(merchantId.value);
			conversationId.value = Number(conversation?.id || 0);
			merchantName.value = conversation?.peerName || merchantName.value;
			peerAvatar.value = conversation?.peerAvatar || '';
			uni.setNavigationBarTitle({ title: merchantName.value });
			await loadMessages();
			if (conversationId.value) {
				await markImConversationRead(conversationId.value);
			}
		} catch (error) {
			uni.showToast({ title: error.message || '会话加载失败', icon: 'none' });
		} finally {
			loading.value = false;
		}
	};

	onLoad((options) => {
		merchantId.value = options.merchantId || '';
		merchantName.value = decodeURIComponent(options.merchantName || '商家');
		if (!merchantId.value) {
			loading.value = false;
			uni.showToast({ title: '商户信息缺失', icon: 'none' });
			return;
		}
		const redirect = `/packageContent/im-chat?merchantId=${merchantId.value}&merchantName=${encodeURIComponent(merchantName.value)}`;
		if (!ensureLogin(redirect)) {
			loading.value = false;
			return;
		}
		initConversation();
	});

	onShow(async () => {
		if (!conversationId.value || loading.value) return;
		await loadMessages();
		await markImConversationRead(conversationId.value);
	});

	const handleSend = async () => {
		const content = draft.value.trim();
		if (!content || sending.value || !conversationId.value) return;
		sending.value = true;
		try {
			await sendImText(conversationId.value, content);
			draft.value = '';
			await loadMessages();
		} catch (error) {
			uni.showToast({ title: error.message || '发送失败', icon: 'none' });
		} finally {
			sending.value = false;
		}
	};
</script>

<style scoped>
	.page {
		display: flex;
		flex-direction: column;
		height: 100vh;
		background: #f5f8f0;
	}

	.message-scroll {
		flex: 1;
		min-height: 0;
		padding: 24rpx;
		box-sizing: border-box;
	}

	.state-box {
		flex: 1;
		display: flex;
		align-items: center;
		justify-content: center;
		padding: 80rpx 24rpx;
	}

	.state-box.empty {
		min-height: 320rpx;
	}

	.state-text {
		font-size: 24rpx;
		color: #8a9a7a;
	}

	.message-row {
		display: flex;
		align-items: flex-start;
		gap: 16rpx;
		margin-bottom: 24rpx;
	}

	.message-row.peer {
		justify-content: flex-start;
	}

	.message-row.mine {
		flex-direction: row-reverse;
		justify-content: flex-start;
	}

	.message-avatar {
		flex-shrink: 0;
	}

	.message-body {
		min-width: 0;
		max-width: calc(100% - 96rpx);
		display: flex;
		flex-direction: column;
	}

	.message-row.mine .message-body {
		align-items: flex-end;
	}

	.message-row.peer .message-body {
		align-items: flex-start;
	}

	.bubble {
		max-width: 100%;
		padding: 18rpx 22rpx;
		border-radius: 22rpx;
	}

	.message-row.mine .bubble {
		background: linear-gradient(135deg, #7ed321, #6bc01a);
	}

	.message-row.peer .bubble {
		background: #ffffff;
		border: 1rpx solid rgba(226, 235, 211, 0.92);
	}

	.bubble-text {
		font-size: 26rpx;
		line-height: 1.6;
		color: #1a2114;
		word-break: break-all;
	}

	.message-row.mine .bubble-text {
		color: #ffffff;
	}

	.message-time {
		margin-top: 8rpx;
		font-size: 20rpx;
		color: #b0b8a8;
	}

	.input-bar {
		display: flex;
		align-items: center;
		gap: 16rpx;
		padding: 16rpx 20rpx calc(16rpx + env(safe-area-inset-bottom));
		background: #ffffff;
		border-top: 1rpx solid rgba(226, 235, 211, 0.92);
	}

	.input-field {
		flex: 1;
		min-width: 0;
		height: 72rpx;
		padding: 0 24rpx;
		border-radius: 36rpx;
		background: #f6f8f3;
		font-size: 26rpx;
		color: #1a2114;
	}

	.send-btn {
		display: flex;
		align-items: center;
		justify-content: center;
		flex-shrink: 0;
		height: 72rpx;
		margin: 0;
		padding: 0 28rpx;
		border-radius: 36rpx;
		font-size: 26rpx;
		font-weight: 600;
		line-height: normal;
		text-align: center;
		color: #ffffff;
		background: linear-gradient(135deg, #7ed321, #6bc01a);
		border: none;
	}

	.send-btn.disabled {
		color: #9aa2b1;
		background: #e5e7eb;
	}
</style>
