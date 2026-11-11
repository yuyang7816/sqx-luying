<template>
	<view class="page">
		<view v-if="errorMessage" class="state-card">
			<text class="state-title">页面无法打开</text>
			<text class="state-desc">{{ errorMessage }}</text>
			<button class="retry-btn" @click="reloadPage">重新加载</button>
		</view>
		<view v-else-if="loading" class="state-card">
			<text class="state-title">页面加载中</text>
			<text class="state-desc">{{ pageTitle }}</text>
		</view>
		<web-view v-if="safeUrl && !errorMessage" :src="safeUrl" @load="handleLoaded" @error="handleError"></web-view>
	</view>
</template>

<script setup>
	import { ref } from 'vue';
	import { onLoad } from '@dcloudio/uni-app';

	const DEFAULT_ALLOW_HOSTS = ['uniapp.dcloud.net.cn', 'mp.weixin.qq.com'];
	const safeUrl = ref('');
	const pageTitle = ref('外部页面');
	const loading = ref(true);
	const errorMessage = ref('');
	let pendingUrl = '';

	const isAllowedUrl = (rawUrl) => {
		try {
			const parsed = new URL(rawUrl);
			if (!['http:', 'https:'].includes(parsed.protocol)) {
				return false;
			}
			return DEFAULT_ALLOW_HOSTS.some((host) => parsed.hostname === host || parsed.hostname.endsWith(`.${host}`));
		} catch (error) {
			return false;
		}
	};

	const applyPage = (rawUrl, title) => {
		pendingUrl = rawUrl;
		pageTitle.value = title || '外部页面';
		uni.setNavigationBarTitle({ title: pageTitle.value });
		if (!rawUrl) {
			errorMessage.value = '缺少跳转链接';
			loading.value = false;
			return;
		}
		if (!isAllowedUrl(rawUrl)) {
			errorMessage.value = '当前链接不在安全域名白名单内，请联系运营配置后再访问。';
			loading.value = false;
			return;
		}
		errorMessage.value = '';
		loading.value = true;
		safeUrl.value = rawUrl;
	};

	const handleLoaded = () => {
		loading.value = false;
	};

	const handleError = () => {
		loading.value = false;
		errorMessage.value = '页面加载失败，请检查网络后重试。';
	};

	const reloadPage = () => {
		if (!pendingUrl) return;
		applyPage(pendingUrl, pageTitle.value);
	};

	onLoad((options) => {
		const rawUrl = options.url ? decodeURIComponent(options.url) : '';
		const title = options.title ? decodeURIComponent(options.title) : '外部页面';
		applyPage(rawUrl, title);
	});
</script>

<style scoped>
	.page { min-height: 100vh; background: #f5f7ff; }
	.state-card { margin: 24rpx; padding: 40rpx 32rpx; border-radius: 28rpx; background: #ffffff; }
	.state-title, .state-desc { display: block; }
	.state-title { font-size: 32rpx; font-weight: 700; color: #1f2937; }
	.state-desc { margin-top: 16rpx; font-size: 24rpx; line-height: 1.7; color: #6b7280; }
	.retry-btn { margin-top: 28rpx; height: 80rpx; line-height: 80rpx; border-radius: 40rpx; font-size: 26rpx; color: #ffffff; background: linear-gradient(135deg, #7ed321, #6bc01a); border: none; }
</style>
