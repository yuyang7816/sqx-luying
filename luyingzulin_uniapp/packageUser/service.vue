<template>
	<view class="page">
		<view class="hero-card">
			<text class="hero-title">官方客服中心</text>
			<text class="hero-desc">平台官方客服，可咨询租赁、订单、押金、配送等问题；联系商家请从商品详情页发起。</text>
			<text v-if="activeChannel" class="hero-badge">{{ channelLabel }}</text>
		</view>

		<view class="card">
			<text class="section-title">联系渠道</text>

			<view v-if="!activeChannel" class="channel-empty">
				<up-icon name="clock" size="30" color="#a7b19f"></up-icon>
				<text class="channel-empty-title">当前未开放客服渠道</text>
				<text class="channel-empty-desc">请稍后再试，或通过订单详情页查看相关说明。</text>
			</view>

			<view v-else class="channel-panel">
				<view class="channel-head">
					<view class="channel-icon">
						<up-icon :name="iconMap[activeChannel.key]" size="24" color="#61b70f"></up-icon>
					</view>
					<view class="channel-main">
						<text class="channel-title">{{ activeChannel.title }}</text>
						<text class="channel-desc">{{ activeChannel.desc }}</text>
					</view>
				</view>

				<view v-if="activeChannel.key === 'onlineIm'" class="channel-action">
					<view class="primary-btn" :class="{ disabled: openingIm }" @click="openIm">
						{{ openingIm ? '连接中...' : (activeChannel.actionText || '立即会话') }}
					</view>
				</view>

				<view v-else-if="activeChannel.key === 'wechat'" class="wechat-panel">

					<image v-if="activeChannel.qrCode" class="wechat-qr" :src="activeChannel.qrCode" mode="aspectFill"
						@click="previewQr(activeChannel.qrCode)"></image>
					<text v-if="activeChannel.qrCode" class="qr-tip">点击二维码可放大查看</text>
					<view class="wechat-row">
						<text class="field-value" style="text-align: center;margin-top: 30rpx;">{{ activeChannel.wechatId }}</text>
					</view>
					<view class="wechat-actions">
						<view class="secondary-btn" @click="copyWechat(activeChannel.wechatId)">复制微信号</view>
					</view>
				</view>

				<view v-else-if="activeChannel.key === 'phone'" class="channel-action">
					<view class="info-row">
						<text class="field-label">客服电话</text>
						<text class="field-value">{{ activeChannel.phoneNumber }}</text>
					</view>
					<view class="info-row">
						<text class="field-label">服务时间</text>
						<text class="field-meta">{{ activeChannel.serviceHours }}</text>
					</view>
					<view class="primary-btn" @click="callPhone(activeChannel.phoneNumber)">拨打电话</view>
				</view>

				<view v-else-if="activeChannel.key === 'wecom'" class="channel-action">
					<view class="info-row">
						<text class="field-label">企业名称</text>
						<text class="field-value">{{ activeChannel.corpName }}</text>
					</view>
					<view class="primary-btn" @click="openWecom(activeChannel.corpLink)">打开企业微信</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script setup>
	import {
		computed,
		ref
	} from 'vue';
	import {
		onShow
	} from '@dcloudio/uni-app';
	import {
		ensureLogin
	} from '@/common/auth';
	import {
		fetchServiceConfigRemote
	} from '@/common/api/content';
	import {
		getServiceConfig,
		getActiveServiceChannel
	} from '@/common/serviceDefaults';

	const serviceConfig = ref(getServiceConfig());
	const openingIm = ref(false);

	const activeChannel = computed(() => getActiveServiceChannel(serviceConfig.value));

	const channelLabelMap = {
		onlineIm: '在线 IM',
		wechat: '微信客服',
		phone: '电话客服',
		wecom: '企业微信'
	};

	const channelLabel = computed(() => channelLabelMap[activeChannel.value?.key] || '客服');

	const iconMap = {
		onlineIm: 'chat',
		wechat: 'weixin-fill',
		phone: 'phone-fill',
		wecom: 'account-fill'
	};

	const openIm = () => {
		if (openingIm.value) return;
		if (!ensureLogin('/packageUser/service')) return;
		openingIm.value = true;
		uni.navigateTo({
			url: '/packageUser/platform-im-chat',
			complete: () => {
				openingIm.value = false;
			}
		});
	};

	const copyWechat = (wechatId) => {
		uni.setClipboardData({
			data: wechatId
		});
	};

	const previewQr = (url) => {
		uni.previewImage({
			urls: [url]
		});
	};

	const callPhone = (phoneNumber) => {
		uni.makePhoneCall({
			phoneNumber
		});
	};

	const openWecom = (url) => {
		// #ifdef H5
		window.open(url, '_blank');
		// #endif
		// #ifndef H5
		uni.setClipboardData({
			data: url
		});
		// #endif
	};

	onShow(async () => {
		try {
			serviceConfig.value = await fetchServiceConfigRemote();
		} catch (error) {
			serviceConfig.value = getServiceConfig();
		}
	});
</script>

<style scoped>
	.page {
		box-sizing: border-box;
		min-height: 100vh;
		padding: 24rpx;
		background: #f6f8f3;
		overflow-x: hidden;
	}

	.hero-card,
	.card,
	.channel-panel {
		box-sizing: border-box;
		width: 100%;
		overflow: hidden;
	}

	.hero-card {
		padding: 36rpx 32rpx;
		border-radius: 32rpx;
		background: linear-gradient(135deg, #8ee226, #9ee842);
	}

	.hero-title,
	.hero-desc,
	.hero-badge,
	.section-title,
	.channel-title,
	.channel-desc,
	.field-label,
	.field-value,
	.field-meta,
	.qr-tip,
	.channel-empty-title,
	.channel-empty-desc {
		display: block;
	}

	.hero-title {
		font-size: 40rpx;
		font-weight: 700;
		color: #ffffff;
	}

	.hero-desc {
		margin-top: 14rpx;
		font-size: 24rpx;
		line-height: 1.7;
		color: rgba(255, 255, 255, 0.94);
	}

	.hero-badge {
		margin-top: 20rpx;
		padding: 10rpx 18rpx;
		align-self: flex-start;
		border-radius: 22rpx;
		font-size: 22rpx;
		color: #4a7a0a;
		background: rgba(255, 255, 255, 0.92);
		width: max-content;
	}

	.card {
		margin-top: 24rpx;
		padding: 28rpx;
		border-radius: 28rpx;
		background: #ffffff;
		box-shadow: 0 8rpx 24rpx rgba(126, 211, 33, 0.06);
	}

	.section-title {
		font-size: 30rpx;
		font-weight: 700;
		color: #1f2937;
	}

	.channel-empty {
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		margin-top: 24rpx;
		padding: 60rpx 32rpx;
		border-radius: 24rpx;
		background: #f9fbf6;
		text-align: center;
	}

	.channel-empty-title {
		margin-top: 18rpx;
		font-size: 28rpx;
		font-weight: 700;
		color: #1f2937;
	}

	.channel-empty-desc {
		margin-top: 12rpx;
		font-size: 22rpx;
		line-height: 1.7;
		color: #8a9a7a;
	}

	.channel-panel {
		margin-top: 24rpx;
		padding: 28rpx;
		border-radius: 24rpx;
		background: #f9fbf6;
		border: 1rpx solid #eef5e6;
	}

	.channel-head {
		display: flex;
		align-items: flex-start;
	}

	.channel-icon {
		display: flex;
		align-items: center;
		justify-content: center;
		width: 80rpx;
		height: 80rpx;
		border-radius: 24rpx;
		background: #eefadb;
		flex-shrink: 0;
	}

	.channel-main {
		flex: 1;
		min-width: 0;
		margin-left: 20rpx;
	}

	.channel-title {
		font-size: 30rpx;
		font-weight: 700;
		color: #1f2937;
	}

	.channel-desc {
		margin-top: 10rpx;
		font-size: 24rpx;
		line-height: 1.7;
		color: #8a9a7a;
	}

	.channel-action,
	.wechat-panel {
		margin-top: 28rpx;
		padding-top: 28rpx;
		border-top: 1rpx solid #e8f0df;
	}

	.info-row+.info-row,
	.wechat-row+.wechat-actions {
		margin-top: 20rpx;
	}

	.field-label {
		font-size: 22rpx;
		color: #8a9a7a;
	}

	.field-value {
		margin-top: 8rpx;
		font-size: 32rpx;
		font-weight: 700;
		color: #1f2937;
		word-break: break-all;
	}

	.field-meta {
		margin-top: 8rpx;
		font-size: 26rpx;
		color: #4b5563;
	}

	.primary-btn,
	.secondary-btn {
		display: flex;
		align-items: center;
		justify-content: center;
		box-sizing: border-box;
		width: 100%;
		margin-top: 24rpx;
		height: 84rpx;
		padding: 0;
		border-radius: 42rpx;
		font-size: 28rpx;
		font-weight: 400;
		text-align: center;
		border: none;
	}

	.primary-btn {
		color: #ffffff;
		background: linear-gradient(135deg, #7ed321, #6bc01a);
	}

	.primary-btn.disabled {
		color: #9aa2b1;
		background: #e5e7eb;
	}

	.secondary-btn {
		color: #59b50e;
		background: #eefadb;
	}

	.wechat-actions {
		display: flex;
	}

	.wechat-qr {
		display: block;
		width: 240rpx;
		height: 240rpx;
		margin: 28rpx auto 0;
		border-radius: 20rpx;
		background: #ffffff;
		box-shadow: 0 8rpx 20rpx rgba(0, 0, 0, 0.06);
	}

	.qr-tip {
		margin-top: 16rpx;
		font-size: 22rpx;
		color: #8a9a7a;
		text-align: center;
	}
</style>
