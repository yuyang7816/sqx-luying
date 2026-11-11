<template>
	<view class="page">
		<view v-if="loading" class="state-card">
			<text class="state-text">加载资料中...</text>
		</view>
		<template v-else>
			<view class="hero-card">
				<view class="hero-top">
					<view class="avatar-wrap" @click="chooseAvatar">
						<image class="avatar" :src="form.avatar" mode="aspectFill"></image>
						<view class="avatar-mask">
							<up-icon name="camera-fill" size="22" color="#ffffff"></up-icon>
							<text class="avatar-tip">{{ uploadingAvatar ? '上传中' : '更换头像' }}</text>
						</view>
					</view>
					<view class="hero-main">
						<text class="hero-caption">个人资料</text>
						<text class="name">{{ form.userName || '露营用户' }}</text>
						<text class="sub">完善昵称、性别与年龄信息后，推荐装备和售后核验会更准确。</text>
					</view>
				</view>
				<view class="hero-stats">
					<view class="hero-stat">
						<text class="hero-stat-label">绑定手机</text>
						<text class="hero-stat-value">{{ form.phoneMasked || '未绑定' }}</text>
					</view>
					<view class="hero-stat">
						<text class="hero-stat-label">邀请码</text>
						<text class="hero-stat-value">{{ form.invitationCode || '暂无' }}</text>
					</view>
				</view>
			</view>

			<view class="form-card">
				<view class="section-head">
					<text class="section-title">基础信息</text>
					<text class="section-desc">以下资料仅用于账号展示与服务匹配</text>
				</view>
				<view class="field-card">
					<view class="field-head">
						<text class="label">昵称</text>
						<text class="field-tip">最多 20 个字</text>
					</view>
					<input v-model="form.userName" class="input" maxlength="20" placeholder="请输入昵称" />
				</view>
				<view class="field-card readonly-card">
					<view class="field-head">
						<text class="label">手机号</text>
					</view>
					<view class="input readonly">{{ form.phoneMasked || '未绑定' }}</view>
				</view>
				<view class="field-card">
					<view class="field-head">
						<text class="label">性别</text>
					</view>
					<view class="sex-row">
						<text v-for="item in sexOptions" :key="item.value" class="sex-tag" :class="{ active: form.sex === item.value }" @click="form.sex = item.value">{{ item.label }}</text>
					</view>
				</view>
				<view class="field-card">
					<view class="field-head">
						<text class="label">年龄</text>
						<text class="field-tip">选填</text>
					</view>
					<input v-model="form.age" class="input" type="number" maxlength="3" placeholder="填写年龄便于推荐合适装备" />
				</view>
			</view>

			<button class="submit-btn" :loading="saving" :disabled="saving" @click="save">保存资料</button>
		</template>
	</view>
</template>

<script setup>
	import { reactive, ref } from 'vue';
	import { onShow } from '@dcloudio/uni-app';
	import { fetchUserInfo, updateUserProfile } from '@/common/api/user';
	import { ensureLogin, syncSessionFromAppUserInfo } from '@/common/auth';
	import { chooseAndUploadImages } from '@/common/upload';
	import { DEFAULT_AVATAR_URL } from '@/common/constants/avatar';

	const sexOptions = [
		{ label: '保密', value: 0 },
		{ label: '男', value: 1 },
		{ label: '女', value: 2 }
	];
	const form = reactive({
		userName: '',
		avatar: DEFAULT_AVATAR_URL,
		phoneMasked: '',
		sex: 0,
		age: '',
		invitationCode: ''
	});
	const loading = ref(true);
	const saving = ref(false);
	const uploadingAvatar = ref(false);

	const loadProfile = async () => {
		loading.value = true;
		try {
			const profile = await fetchUserInfo();
			form.userName = profile.userName;
			form.avatar = profile.avatar;
			form.phoneMasked = profile.phoneMasked;
			form.sex = profile.sex || 0;
			form.age = profile.age;
			form.invitationCode = profile.invitationCode;
		} catch (error) {
			uni.showToast({ title: error.message || '资料加载失败', icon: 'none' });
		} finally {
			loading.value = false;
		}
	};

	onShow(() => {
		if (!ensureLogin('/packageUser/profile')) return;
		loadProfile();
	});

	const chooseAvatar = async () => {
		if (uploadingAvatar.value || saving.value) return;
		uploadingAvatar.value = true;
		try {
			const urls = await chooseAndUploadImages({ count: 1 });
			if (urls?.[0]) {
				form.avatar = urls[0];
			}
		} catch (error) {
			uni.showToast({ title: error?.message || error?.msg || '头像上传失败', icon: 'none' });
		} finally {
			uploadingAvatar.value = false;
		}
	};

	const save = async () => {
		if (saving.value) return;
		if (!String(form.userName || '').trim()) {
			uni.showToast({ title: '请输入昵称', icon: 'none' });
			return;
		}
		saving.value = true;
		try {
			const profile = await updateUserProfile(form);
			syncSessionFromAppUserInfo(profile);
			form.userName = profile.userName;
			form.avatar = profile.avatar;
			uni.showToast({ title: '资料已保存', icon: 'success' });
		} catch (error) {
			uni.showToast({ title: error.message || '保存失败', icon: 'none' });
		} finally {
			saving.value = false;
		}
	};
</script>

<style scoped>
	.page {
		min-height: 100vh;
		padding: 24rpx;
		background: linear-gradient(180deg, #edf6df 0%, #f7faf1 18%, #f7f8f5 100%);
		overflow-x: hidden;
	}

	.state-card,
	.hero-card,
	.form-card {
		width: 91%;
		padding: 30rpx;
		border-radius: 30rpx;
		overflow: hidden;
	}

	.state-card,
	.form-card {
		background: #ffffff;
		box-shadow: 0 12rpx 30rpx rgba(38, 52, 29, 0.05);
	}

	.hero-card {
		background: linear-gradient(180deg, #314c24 0%, #3b5d2b 100%);
		box-shadow: 0 20rpx 40rpx rgba(47, 74, 34, 0.18);
	}

	.hero-top,
	.hero-stats,
	.field-head,
	.sex-row {
		display: flex;
	}

	.hero-caption,
	.name,
	.sub,
	.hero-stat-label,
	.hero-stat-value,
	.section-title,
	.section-desc,
	.label,
	.field-tip,
	.state-text {
		display: block;
	}

	.hero-top {
		align-items: center;
	}

	.avatar-wrap {
		position: relative;
		width: 124rpx;
		height: 124rpx;
		flex-shrink: 0;
	}

	.avatar {
		width: 124rpx;
		height: 124rpx;
		border-radius: 34rpx;
		background: #eef2f7;
		border: 4rpx solid rgba(255, 255, 255, 0.14);
	}

	.avatar-mask {
		position: absolute;
		left: 0;
		right: 0;
		bottom: 0;
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		height: 52rpx;
		border-radius: 0 0 30rpx 30rpx;
		background: rgba(0, 0, 0, 0.42);
	}

	.avatar-tip {
		margin-top: 4rpx;
		font-size: 18rpx;
		line-height: 1.2;
		color: rgba(255, 255, 255, 0.92);
	}

	.hero-main {
		flex: 1;
		min-width: 0;
		margin-left: 22rpx;
	}

	.hero-caption {
		font-size: 22rpx;
		font-weight: 700;
		color: rgba(211, 235, 167, 0.9);
	}

	.name {
		margin-top: 10rpx;
		font-size: 38rpx;
		font-weight: 800;
		line-height: 1.25;
		color: #ffffff;
	}

	.sub {
		margin-top: 10rpx;
		font-size: 22rpx;
		line-height: 1.55;
		color: rgba(233, 240, 224, 0.76);
	}

	.hero-stats {
		gap: 18rpx;
		margin-top: 28rpx;
	}

	.hero-stat {
		flex: 1;
		min-width: 0;
		padding: 22rpx 24rpx;
		border-radius: 24rpx;
		background: rgba(255, 255, 255, 0.08);
		border: 1rpx solid rgba(255, 255, 255, 0.08);
	}

	.hero-stat-label {
		font-size: 20rpx;
		color: rgba(217, 230, 201, 0.68);
	}

	.hero-stat-value {
		margin-top: 12rpx;
		font-size: 24rpx;
		font-weight: 700;
		line-height: 1.35;
		color: #ffffff;
		word-break: break-all;
	}

	.form-card {
		margin-top: 24rpx;
	}

	.section-title {
		font-size: 32rpx;
		font-weight: 800;
		color: #1a2114;
	}

	.section-desc {
		margin-top: 10rpx;
		font-size: 22rpx;
		line-height: 1.5;
		color: #87947c;
	}

	.field-card {
		margin-top: 24rpx;
		padding: 24rpx;
		border-radius: 24rpx;
		background: #f8faf5;
	}

	.readonly-card {
		background: #f4f6f1;
	}

	.field-head {
		align-items: center;
		justify-content: space-between;
		gap: 16rpx;
	}

	.label {
		font-size: 24rpx;
		font-weight: 700;
		color: #233019;
	}

	.field-tip {
		font-size: 20rpx;
		color: #9aa58f;
	}

	.input {
		height: 82rpx;
		margin-top: 16rpx;
		padding: 0 22rpx;
		border-radius: 20rpx;
		font-size: 26rpx;
		color: #1f2937;
		background: #ffffff;
		line-height: 82rpx;
	}

	.input.readonly {
		color: #687160;
		background: rgba(255, 255, 255, 0.7);
	}

	.sex-row {
		flex-wrap: wrap;
		margin-top: 16rpx;
		gap: 16rpx;
	}

	.sex-tag {
		padding: 16rpx 28rpx;
		border-radius: 999rpx;
		font-size: 24rpx;
		font-weight: 600;
		color: #67735f;
		background: #ffffff;
	}

	.sex-tag.active {
		color: #213717;
		background: linear-gradient(180deg, #e9f6c7 0%, #d8eea4 100%);
	}

	.state-text {
		font-size: 26rpx;
		color: #6b7280;
		text-align: center;
	}

	.submit-btn {
		width: 100%;
		margin-top: 30rpx;
		height: 92rpx;
		line-height: 92rpx;
		border-radius: 24rpx;
		font-size: 30rpx;
		font-weight: 700;
		color: #203717;
		background: linear-gradient(180deg, #ebf7ca 0%, #d6eca1 100%);
		border: none;
		box-shadow: 0 16rpx 30rpx rgba(102, 139, 54, 0.18);
	}

	.submit-btn::after {
		border: 0;
	}
</style>
