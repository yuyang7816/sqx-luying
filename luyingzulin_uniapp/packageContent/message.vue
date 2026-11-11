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
				<view
					v-if="item.key === 'service' && imUnreadCount"
					class="tab-dot"
				>{{ imUnreadCount > 99 ? '99+' : imUnreadCount }}</view>
				<view
					v-else-if="item.key === 'system' && systemUnreadCount"
					class="tab-dot"
				>{{ systemUnreadCount > 99 ? '99+' : systemUnreadCount }}</view>
			</view>
		</view>

		<view v-if="activeTab === 'service'" class="panel">
			<view v-if="serviceLoading" class="empty">加载中...</view>
			<view v-else-if="!serviceItems.length" class="tip-card">
				<text class="tip-title">暂无客服会话</text>
				<text class="tip-desc">可在下方进入官方客服中心，或在商品详情页联系商家咨询。</text>
			</view>
			<view
				v-for="item in serviceItems"
				:key="item.key"
				class="conv-item"
				:class="{ unread: item.unread > 0 }"
				@click="openServiceItem(item)"
			>
				<ChatAvatar :src="item.avatarSrc" :variant="item.avatarVariant" :size="96" />
				<view class="conv-main">
					<view class="conv-head">
						<text class="conv-name">{{ item.name }}</text>
						<text class="conv-time">{{ item.time }}</text>
					</view>
					<view class="conv-foot">
						<text class="conv-preview">{{ item.preview }}</text>
						<view v-if="item.unread > 0" class="conv-badge">
							{{ item.unread > 99 ? '99+' : item.unread }}
						</view>
					</view>
				</view>
			</view>

			<view class="service-link" @click="goService">
				<up-icon name="kefu-ermai" size="18" color="#61b70f"></up-icon>
				<text class="service-link-text">官方客服中心（电话 / 微信 / 在线 IM）</text>
				<up-icon name="arrow-right" size="14" color="#a8afb7"></up-icon>
			</view>
		</view>

		<view v-else class="panel">
			<view v-if="loading" class="empty">加载中...</view>
			<view v-else-if="!list.length" class="empty">暂无系统通知</view>
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
	import { computed, ref } from 'vue';
	import { onLoad, onShow } from '@dcloudio/uni-app';
	import { ensureLogin, isLoggedIn } from '@/common/auth';
	import {
		fetchSystemMessagePage,
		fetchUnreadSummary,
		markMessageRead
	} from '@/common/api/message';
	import { fetchImConversations } from '@/common/api/im';
	import { openPlatformCsConversation } from '@/common/api/platformCs';
	import ChatAvatar from '@/components/ChatAvatar.vue';
	import { PLATFORM_CS_VARIANT } from '@/common/constants/serviceAvatar';

	const tabs = [
		{ key: 'service', label: '客服消息' },
		{ key: 'system', label: '系统通知' }
	];

	const activeTab = ref('service');
	const loading = ref(false);
	const imLoading = ref(false);
	const platformLoading = ref(false);
	const list = ref([]);
	const imConversations = ref([]);
	const platformConversation = ref(null);
	const systemUnreadCount = ref(0);
	const imUnreadCount = ref(0);

	const serviceLoading = computed(() => imLoading.value || platformLoading.value);

	const serviceItems = computed(() => {
		const items = [];
		if (platformConversation.value) {
			items.push({
				key: `platform-${platformConversation.value.id || 0}`,
				kind: 'platform',
				name: platformConversation.value.peerName || '官方客服',
				preview: platformConversation.value.lastMsgPreview || '点击开始咨询平台官方客服',
				time: platformConversation.value.lastMsgTime || '',
				unread: Number(platformConversation.value.unreadCount) || 0,
				avatarVariant: PLATFORM_CS_VARIANT,
				avatarSrc: ''
			});
		}
		for (const item of imConversations.value) {
			items.push({
				key: `merchant-${item.id}`,
				kind: 'merchant',
				raw: item,
				name: item.peerName || '商家',
				preview: item.lastMsgPreview || '暂无消息',
				time: item.lastMsgTime || '',
				unread: Number(item.unreadCount) || 0,
				avatarVariant: 'merchant',
				avatarSrc: item.peerAvatar || ''
			});
		}
		return items.sort((a, b) => String(b.time || '').localeCompare(String(a.time || '')));
	});

	async function loadUnreadCounts() {
		if (!isLoggedIn()) {
			systemUnreadCount.value = 0;
			imUnreadCount.value = 0;
			return;
		}
		try {
			const summary = await fetchUnreadSummary({ silentError: true });
			systemUnreadCount.value = Number(summary?.systemUnread) || 0;
			imUnreadCount.value = Number(summary?.imUnread) || 0;
		} catch (error) {
			systemUnreadCount.value = 0;
			imUnreadCount.value = 0;
		}
	}

	async function loadSystemMessages() {
		if (!ensureLogin('/packageContent/message')) return;
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

	async function loadPlatformConversation() {
		if (!isLoggedIn()) {
			platformConversation.value = null;
			return;
		}
		platformLoading.value = true;
		try {
			platformConversation.value = await openPlatformCsConversation();
		} catch (error) {
			platformConversation.value = null;
		} finally {
			platformLoading.value = false;
		}
	}

	async function loadImConversations() {
		if (!isLoggedIn()) {
			imConversations.value = [];
			return;
		}
		imLoading.value = true;
		try {
			const { list: rows } = await fetchImConversations({ page: 1, limit: 50, silentError: true });
			imConversations.value = rows || [];
		} catch (error) {
			imConversations.value = [];
		} finally {
			imLoading.value = false;
		}
	}

	async function switchTab(key) {
		activeTab.value = key;
		if (key === 'system') {
			await loadSystemMessages();
		} else if (key === 'service') {
			await Promise.all([loadPlatformConversation(), loadImConversations()]);
		}
	}

	function goService() {
		uni.navigateTo({ url: '/packageUser/service' });
	}

	function openServiceItem(item) {
		if (item.kind === 'platform') {
			if (!ensureLogin('/packageContent/message?tab=service')) return;
			uni.navigateTo({ url: '/packageUser/platform-im-chat' });
			return;
		}
		const merchant = item.raw;
		if (!merchant?.merchantId) return;
		const merchantName = encodeURIComponent(merchant.peerName || '商家');
		uni.navigateTo({
			url: `/packageContent/im-chat?merchantId=${merchant.merchantId}&merchantName=${merchantName}`
		});
	}

	async function openMessage(item) {
		if (!item?.id) return;
		if (!item.isRead) {
			try {
				await markMessageRead(item.id);
				item.isRead = 1;
				systemUnreadCount.value = Math.max(0, systemUnreadCount.value - 1);
			} catch (error) {
				// 标记已读失败不阻断查看
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
			uni.navigateTo({ url: `/packageOrder/detail?id=${item.dataId}` });
		}
	}

	onLoad((options) => {
		if (options?.tab === 'system') {
			activeTab.value = 'system';
		} else if (options?.tab === 'service') {
			activeTab.value = 'service';
		}
	});

	onShow(async () => {
		await loadUnreadCounts();
		if (activeTab.value === 'system') {
			await loadSystemMessages();
		} else if (activeTab.value === 'service') {
			await Promise.all([loadPlatformConversation(), loadImConversations()]);
		}
	});
</script>

<style scoped>
	.page {
		min-height: 100vh;
		background: #f5f7ff;
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
		background: #f3f6fb;
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
	.empty,
	.service-link-text {
		display: block;
	}

	.conv-name {
		flex: 1;
		min-width: 0;
		font-size: 30rpx;
		font-weight: 700;
		color: #1f2937;
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

	.service-link {
		display: flex;
		align-items: center;
		gap: 12rpx;
		margin-top: 8rpx;
		padding: 24rpx 28rpx;
		border-radius: 24rpx;
		background: #ffffff;
	}

	.service-link-text {
		flex: 1;
		min-width: 0;
		font-size: 24rpx;
		color: #6b7280;
	}

	.tip-card {
		padding: 28rpx;
		margin-bottom: 16rpx;
	}

	.tip-title {
		font-size: 26rpx;
		font-weight: 700;
		color: #1f2937;
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
		color: #1f2937;
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
