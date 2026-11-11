<template>
	<view class="page">
		<view class="section-card">
			<view class="section-head">
				<text class="section-title">账号安全</text>
				<text class="section-desc">与登录、密码和账号状态相关</text>
			</view>
			<view class="cell-list">
				<view class="cell" @click="goForgotPassword">
					<view class="cell-main">
						<view class="cell-icon password-icon"><up-icon name="lock" size="18" color="#7c6214"></up-icon></view>
						<view class="cell-copy"><text class="cell-title">修改密码</text><text class="cell-sub">更新当前登录密码</text></view>
					</view>
					<up-icon name="arrow-right" size="16" color="#b0b8a6"></up-icon>
				</view>
				<view class="cell" @click="showDestroyTip">
					<view class="cell-main">
						<view class="cell-icon destroy-icon"><up-icon name="close-circle" size="18" color="#b55257"></up-icon></view>
						<view class="cell-copy"><text class="cell-title">注销账号</text><text class="cell-sub">需联系平台客服人工处理</text></view>
					</view>
					<up-icon name="arrow-right" size="16" color="#b0b8a6"></up-icon>
				</view>
			</view>
		</view>

		<view class="section-card">
			<view class="section-head">
				<text class="section-title">应用设置</text>
				<text class="section-desc">本地使用偏好与帮助入口</text>
			</view>
			<view class="cell-list">
				<view class="cell" @click="goPage('/packageContent/help')">
					<view class="cell-main">
						<view class="cell-icon guide-icon"><up-icon name="file-text" size="18" color="#4e6a42"></up-icon></view>
						<view class="cell-copy"><text class="cell-title">租赁指南</text><text class="cell-sub">新手下单与取还流程</text></view>
					</view>
					<up-icon name="arrow-right" size="16" color="#b0b8a6"></up-icon>
				</view>
				<view class="cell" @click="goPage('/packageUser/feedback')">
					<view class="cell-main">
						<view class="cell-icon feedback-icon"><up-icon name="email" size="18" color="#7c5ca1"></up-icon></view>
						<view class="cell-copy"><text class="cell-title">意见反馈</text><text class="cell-sub">提交建议与使用问题</text></view>
					</view>
					<up-icon name="arrow-right" size="16" color="#b0b8a6"></up-icon>
				</view>
			</view>
		</view>

		<view class="section-card">
			<view class="section-head">
				<text class="section-title">关于</text>
				<text class="section-desc">平台协议与版本信息</text>
			</view>
			<view class="cell-list">
				<view class="cell" @click="openAgreement('rental')">
					<view class="cell-main">
						<view class="cell-icon rule-icon"><up-icon name="list-dot" size="18" color="#8d6116"></up-icon></view>
						<view class="cell-copy"><text class="cell-title">平台规则</text><text class="cell-sub">查看租赁与服务规范</text></view>
					</view>
					<up-icon name="arrow-right" size="16" color="#b0b8a6"></up-icon>
				</view>
				<view class="cell" @click="openAgreement('privacy')">
					<view class="cell-main">
						<view class="cell-icon privacy-icon"><up-icon name="lock" size="18" color="#46689b"></up-icon></view>
						<view class="cell-copy"><text class="cell-title">隐私协议</text><text class="cell-sub">查看数据与隐私说明</text></view>
					</view>
					<up-icon name="arrow-right" size="16" color="#b0b8a6"></up-icon>
				</view>
			</view>
		</view>
		<button class="logout-btn" @click="logoutAction">退出登录</button>
	</view>
</template>

<script setup>
	import { ref } from 'vue';
	import { onShow } from '@dcloudio/uni-app';
	import { ensureLogin, getStoredSession, logout } from '@/common/auth';
	import { DEFAULT_AVATAR_URL } from '@/common/constants/avatar';

	const guestSession = {
		userInfo: {
			nickname: '游客',
			avatar: DEFAULT_AVATAR_URL,
			memberLabel: '未登录',
			phone: ''
		}
	};
	const session = ref(guestSession);

	const syncSession = () => {
		session.value = getStoredSession() || guestSession;
	};

	onShow(() => {
		if (!ensureLogin('/packageUser/setting')) return;
		syncSession();
	});

	const goProfile = () => {
		uni.navigateTo({ url: '/packageUser/profile' });
	};

	const goPage = (url) => {
		uni.navigateTo({ url });
	};

	const openAgreement = (type) => {
		uni.navigateTo({ url: `/packageContent/agreement?type=${type}` });
	};

	const goForgotPassword = () => {
		uni.navigateTo({ url: '/packageUser/forgot-password' });
	};

	const showDestroyTip = () => {
		uni.showModal({
			title: '注销账号',
			content: '账号注销需联系平台客服人工处理，确认后将退出当前登录。',
			confirmText: '联系客服',
			cancelText: '取消',
			success: ({ confirm }) => {
				if (confirm) {
					uni.navigateTo({ url: '/packageUser/service' });
				}
			}
		});
	};

	const logoutAction = () => {
		uni.showModal({
			title: '退出登录',
			content: '确定退出当前账号吗？',
			success: async ({ confirm }) => {
				if (!confirm) return;
				await logout();
				uni.showToast({ title: '已退出登录', icon: 'success' });
				setTimeout(() => {
					uni.reLaunch({ url: '/pages/my/my' });
				}, 300);
			}
		});
	};
</script>

<style scoped>
	.page {
		min-height: 100vh;
		padding: 24rpx;
		background: linear-gradient(180deg, #edf6df 0%, #f7faf1 18%, #f7f8f5 100%);
	}

	.section-card {
		margin-bottom: 24rpx;
		padding: 28rpx;
		border-radius: 30rpx;
	}

	.section-card {
		background: #ffffff;
		box-shadow: 0 12rpx 30rpx rgba(38, 52, 29, 0.05);
	}

	.section-head,
	.cell,
	.cell-main {
		display: flex;
	}

	.section-title,
	.section-desc,
	.cell-title,
	.cell-sub,
	.version-text {
		display: block;
	}

	.section-head {
		align-items: flex-start;
		justify-content: space-between;
		gap: 16rpx;
		margin-bottom: 24rpx;
	}

	.section-title {
		font-size: 30rpx;
		font-weight: 800;
		color: #1a2114;
	}

	.section-desc {
		margin-top: 10rpx;
		font-size: 22rpx;
		line-height: 1.5;
		color: #87947c;
	}

	.cell-list {
		border-radius: 24rpx;
		overflow: hidden;
		background: #f8faf5;
	}

	.cell {
		align-items: center;
		justify-content: space-between;
		min-height: 102rpx;
		padding: 22rpx 24rpx;
	}

	.cell + .cell {
		border-top: 1rpx solid #e8eee0;
	}

	.cell-main {
		align-items: center;
		flex: 1;
		min-width: 0;
	}

	.cell-icon {
		display: flex;
		align-items: center;
		justify-content: center;
		width: 64rpx;
		height: 64rpx;
		border-radius: 20rpx;
		margin-right: 18rpx;
		flex-shrink: 0;
	}

	.password-icon { background: linear-gradient(180deg, #fff5db 0%, #fee8be 100%); }
	.destroy-icon { background: linear-gradient(180deg, #fff0f1 0%, #ffdfe3 100%); }
	.guide-icon { background: linear-gradient(180deg, #f3f6ef 0%, #e7ecdf 100%); }
	.feedback-icon { background: linear-gradient(180deg, #f5efff 0%, #eadfff 100%); }
	.rule-icon { background: linear-gradient(180deg, #fff5e6 0%, #ffe8c7 100%); }
	.privacy-icon { background: linear-gradient(180deg, #edf4ff 0%, #dfe9ff 100%); }

	.cell-copy {
		flex: 1;
		min-width: 0;
	}

	.cell-title {
		font-size: 27rpx;
		font-weight: 700;
		line-height: 1.35;
		color: #233019;
	}

	.cell-sub {
		margin-top: 8rpx;
		font-size: 22rpx;
		line-height: 1.45;
		color: #87947d;
	}

	.logout-btn {
		width: 100%;
		height: 92rpx;
		line-height: 92rpx;
		border-radius: 24rpx;
		font-size: 30rpx;
		font-weight: 700;
		color: #d95258;
		background: #ffffff;
		border: 2rpx solid #ffd6d9;
		box-shadow: 0 10rpx 24rpx rgba(217, 82, 88, 0.08);
	}

	.logout-btn::after {
		border: 0;
	}
</style>
