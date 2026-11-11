<template>
	<view class="page">
		<view class="tab-row">
			<view
				v-for="item in tabs"
				:key="item.key"
				class="tab-item"
				:class="{ active: activeTab === item.key }"
				@click="switchTab(item.key)"
			>
				<text class="tab-text">{{ item.label }}</text>
				<view v-if="item.key === 'service' && imUnreadCount" class="tab-dot">
					{{ imUnreadCount > 99 ? '99+' : imUnreadCount }}
				</view>
				<view v-else-if="item.key === 'system' && systemUnreadCount" class="tab-dot">
					{{ systemUnreadCount > 99 ? '99+' : systemUnreadCount }}
				</view>
			</view>
		</view>

		<view v-if="activeTab === 'service'" class="panel">
			<view v-if="imLoading" class="empty">加载中...</view>
			<view v-else-if="!imConversations.length" class="tip-card">
				<text class="tip-title">暂无用户咨询</text>
				<text class="tip-desc">用户从商品详情发起咨询后，会话会出现在这里。也可在订单详情中联系用户。</text>
			</view>
			<view
				v-for="item in imConversations"
				:key="item.id"
				class="conv-item"
				:class="{ unread: item.unreadCount > 0 }"
				@click="openImChat(item)"
			>
				<ChatAvatar :src="item.peerAvatar" :size="96" />
				<view class="conv-main">
					<view class="conv-head">
						<text class="conv-name">{{ item.peerName || '用户' }}</text>
						<text class="conv-time">{{ item.lastMsgTime || '' }}</text>
					</view>
					<view class="conv-foot">
						<text class="conv-preview">{{ item.lastMsgPreview || '暂无消息' }}</text>
						<view v-if="item.unreadCount > 0" class="conv-badge">
							{{ item.unreadCount > 99 ? '99+' : item.unreadCount }}
						</view>
					</view>
				</view>
			</view>
		</view>

		<view v-else class="panel">
			<view v-if="loading" class="empty">加载中...</view>
			<view v-else-if="!list.length" class="empty">暂无平台通知</view>
			<view
				v-for="item in list"
				:key="item.id"
				class="card"
				:class="{ unread: !item.isRead }"
				@click="openMessage(item)"
			>
				<view class="head">
					<text class="title">{{ item.title || '系统通知' }}</text>
					<text v-if="!item.isRead" class="badge">未读</text>
				</view>
				<text class="content">{{ item.content }}</text>
				<text class="time">{{ item.createTime }}</text>
			</view>
		</view>
	</view>
</template>

<script setup>
	import { ref } from 'vue';
	import { onLoad, onShow } from '@dcloudio/uni-app';
	import { ensureLogin } from '@/common/auth';
	import { ensureMerchantWorkbenchAccess } from '@/common/api/merchant';
	import { fetchSystemMessagePage, markMessageRead } from '@/common/api/message';
	import { fetchMerchantUnreadSummary } from '@/common/api/merchantMessage';
	import { fetchMerchantImConversations } from '@/common/api/merchantIm';
	import ChatAvatar from '@/components/ChatAvatar.vue';

	const tabs = [
		{ key: 'service', label: '用户咨询' },
		{ key: 'system', label: '平台通知' }
	];

	const activeTab = ref('service');
	const loading = ref(false);
	const imLoading = ref(false);
	const list = ref([]);
	const imConversations = ref([]);
	const systemUnreadCount = ref(0);
	const imUnreadCount = ref(0);

	async function loadUnreadCounts() {
		try {
			const summary = await fetchMerchantUnreadSummary({ silentError: true });
			systemUnreadCount.value = Number(summary?.systemUnread || 0);
			imUnreadCount.value = Number(summary?.imUnread || 0);
		} catch {
			systemUnreadCount.value = 0;
			imUnreadCount.value = 0;
		}
	}

	async function loadSystemMessages() {
		if (!ensureLogin('/packageMerchant/message')) return;
		loading.value = true;
		try {
			const { list: rows } = await fetchSystemMessagePage({ page: 1, limit: 50 });
			list.value = rows || [];
		} catch (error) {
			list.value = [];
			uni.showToast({ title: error.message || '加载失败', icon: 'none' });
		} finally {
			loading.value = false;
		}
	}

	async function loadImConversations() {
		imLoading.value = true;
		try {
			const { list: rows } = await fetchMerchantImConversations({ page: 1, limit: 50, silentError: true });
			imConversations.value = rows || [];
		} catch {
			imConversations.value = [];
		} finally {
			imLoading.value = false;
		}
	}

	function switchTab(key) {
		activeTab.value = key;
		if (key === 'system') {
			loadSystemMessages();
		} else if (key === 'service') {
			loadImConversations();
		}
	}

	function openImChat(item) {
		if (!item?.userId) return;
		const userName = encodeURIComponent(item.peerName || '用户');
		uni.navigateTo({
			url: `/packageMerchant/im-chat?userId=${item.userId}&userName=${userName}`
		});
	}

	async function openMessage(item) {
		if (!item?.id) return;
		if (!item.isRead) {
			try {
				await markMessageRead(item.id);
				item.isRead = 1;
				systemUnreadCount.value = Math.max(0, systemUnreadCount.value - 1);
			} catch {
				// ignore
			}
		}
		if (item.url) {
			if (item.url.startsWith('/')) {
				uni.navigateTo({ url: item.url });
				return;
			}
			uni.navigateTo({ url: `/packageContent/web-view?url=${encodeURIComponent(item.url)}` });
			return;
		}
		if (item.dataId) {
			uni.navigateTo({ url: `/packageMerchant/order-detail?id=${item.dataId}` });
		}
	}

	onLoad((options) => {
		if (options?.tab === 'system') {
			activeTab.value = 'system';
		}
	});

	onShow(async () => {
		if (!ensureLogin('/packageMerchant/message')) return;
		try {
			await ensureMerchantWorkbenchAccess({ redirect: true, toast: true });
		} catch {
			return;
		}
		await loadUnreadCounts();
		if (activeTab.value === 'system') {
			await loadSystemMessages();
		} else {
			await loadImConversations();
		}
	});
</script>

<style scoped>
	.page {
		min-height: 100vh;
		background: #f5f8f0;
		box-sizing: border-box;
	}

	.tab-row {
		display: flex;
		padding: 24rpx;
		gap: 16rpx;
		background: #ffffff;
	}

	.tab-item {
		position: relative;
		flex: 1;
		height: 72rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		border-radius: 999rpx;
		background: #f3f6f0;
	}

	.tab-item.active {
		background: #eefadb;
	}

	.tab-text {
		font-size: 26rpx;
		font-weight: 600;
		color: #626a73;
	}

	.tab-item.active .tab-text {
		color: #4a7a0a;
	}

	.tab-dot {
		position: absolute;
		top: 8rpx;
		right: 28rpx;
		min-width: 28rpx;
		height: 28rpx;
		padding: 0 8rpx;
		line-height: 28rpx;
		border-radius: 14rpx;
		text-align: center;
		font-size: 18rpx;
		font-weight: 700;
		color: #ffffff;
		background: #ff5d57;
	}

	.panel {
		padding: 24rpx;
	}

	.tip-card,
	.card {
		background: #ffffff;
		border-radius: 24rpx;
	}

	.conv-item {
		display: flex;
		align-items: center;
		gap: 20rpx;
		padding: 24rpx;
		margin-bottom: 16rpx;
		border-radius: 24rpx;
		background: #ffffff;
	}

	.conv-item.unread {
		border: 1rpx solid #d8f0b8;
	}

	.conv-main {
		flex: 1;
		min-width: 0;
	}

	.conv-head,
	.conv-foot {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 16rpx;
	}

	.conv-name,
	.conv-preview,
	.conv-time,
	.tip-title,
	.tip-desc,
	.title,
	.content,
	.time,
	.empty {
		display: block;
	}

	.conv-name {
		flex: 1;
		min-width: 0;
		font-size: 30rpx;
		font-weight: 700;
		color: #1a2114;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.conv-time {
		flex-shrink: 0;
		font-size: 22rpx;
		color: #9ca3af;
	}

	.conv-foot {
		margin-top: 10rpx;
	}

	.conv-preview {
		flex: 1;
		min-width: 0;
		font-size: 24rpx;
		line-height: 1.5;
		color: #6b7280;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.conv-badge {
		flex-shrink: 0;
		min-width: 32rpx;
		height: 32rpx;
		padding: 0 10rpx;
		line-height: 32rpx;
		border-radius: 16rpx;
		text-align: center;
		font-size: 20rpx;
		font-weight: 700;
		color: #ffffff;
		background: #ff5d57;
	}

	.tip-card {
		padding: 28rpx;
		margin-bottom: 16rpx;
	}

	.tip-title {
		font-size: 26rpx;
		font-weight: 700;
		color: #1a2114;
	}

	.tip-desc {
		margin-top: 12rpx;
		font-size: 22rpx;
		line-height: 1.7;
		color: #6b7280;
	}

	.card {
		margin-bottom: 20rpx;
		padding: 28rpx;
	}

	.card.unread {
		border: 1rpx solid #d8f0b8;
	}

	.head {
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	.title {
		font-size: 28rpx;
		font-weight: 700;
		color: #1a2114;
	}

	.badge {
		font-size: 20rpx;
		color: #59b50e;
		background: #eef8e0;
		padding: 4rpx 12rpx;
		border-radius: 999rpx;
	}

	.content {
		margin-top: 12rpx;
		font-size: 24rpx;
		line-height: 1.7;
		color: #6b7280;
	}

	.time {
		margin-top: 16rpx;
		font-size: 22rpx;
		color: #9ca3af;
	}

	.empty {
		padding: 80rpx 0;
		text-align: center;
		color: #6b7280;
		font-size: 26rpx;
	}
</style>
