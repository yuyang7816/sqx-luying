<template>
	<view class="page">
		<view class="hero-section">
			<view class="hero-copy">
				<text class="hero-title">夜幕营地已点亮</text>
				<text class="hero-desc">重置密码后继续下单，订单与钱包直达。</text>
			</view>
			<view class="hero-scene">
				<view class="scene-moon"></view>
				<view class="scene-glow"></view>
				<view class="scene-card">
					<text class="scene-tag">今晚出发</text>
					<text class="scene-text">装备与进度一页直达</text>
				</view>
				<view class="ridge ridge-back"></view>
				<view class="ridge ridge-front"></view>
				<view class="tent-wrap">
					<view class="tent"></view>
					<view class="tent-door"></view>
					<view class="camp-fire"></view>
				</view>
			</view>
		</view>

		<view class="login-shell">
			<view class="tabs-wrap">
				<u-tabs :list="pageTabs" keyName="label" :current="0" :scrollable="false" lineColor="#7ed321" :activeStyle="tabActiveStyle" :inactiveStyle="tabInactiveStyle"></u-tabs>
			</view>

			<view class="form-area">
				<view class="intro-strip">
					<text class="intro-title">找回密码</text>
					<text class="intro-note">{{ resetDesc }}</text>
				</view>

				<view class="field-list">
					<view class="field-item">
						<text class="field-label">手机号</text>
						<view class="field-box">
							<input v-model="form.mobile" class="field-input" type="number" maxlength="11" placeholder="请输入注册手机号" />
						</view>
					</view>

					<view class="field-item">
						<view class="field-row-head">
							<text class="field-label">短信验证码</text>
							<text class="field-hint">发送到当前手机号</text>
						</view>
						<view class="code-row">
							<view class="field-box code-box">
								<input v-model="form.code" class="field-input" type="number" maxlength="6" placeholder="请输入验证码" />
							</view>
							<text class="code-btn" :class="{ disabled: countdown > 0 }" @click="sendCode">{{ countdown > 0 ? `${countdown}s` : '发送验证码' }}</text>
						</view>
					</view>

					<view class="field-item">
						<text class="field-label">新密码</text>
						<view class="field-box">
							<input v-model="form.password" class="field-input" password placeholder="请输入新密码，至少 6 位" />
						</view>
					</view>

					<view class="field-item">
						<text class="field-label">确认密码</text>
						<view class="field-box">
							<input v-model="form.confirmPassword" class="field-input" password placeholder="请再次输入新密码" />
						</view>
					</view>
				</view>

				<button class="submit-btn" :loading="submitting" :disabled="submitting" @click="submit">确认重置</button>
				<text class="footer-link" @click="goLogin">返回登录</text>
			</view>
		</view>
	</view>
</template>

<script setup>
	import { computed, reactive, ref } from 'vue';
	import { onUnload } from '@dcloudio/uni-app';
	import { resetPassword, sendAuthCode } from '@/common/auth';

	const pageTabs = [{ label: '找回密码' }];
	const tabActiveStyle = { color: '#59b50e', fontWeight: '700' };
	const tabInactiveStyle = { color: '#6b7280' };
	const resetDesc = computed(() => '通过手机号验证码重置密码。');

	const form = reactive({
		mobile: '',
		code: '',
		password: '',
		confirmPassword: ''
	});
	const countdown = ref(0);
	const submitting = ref(false);
	let countdownTimer = null;

	onUnload(() => {
		if (countdownTimer) {
			clearInterval(countdownTimer);
			countdownTimer = null;
		}
	});

	const startCountdown = () => {
		countdown.value = 60;
		if (countdownTimer) clearInterval(countdownTimer);
		countdownTimer = setInterval(() => {
			countdown.value -= 1;
			if (countdown.value <= 0) {
				clearInterval(countdownTimer);
				countdownTimer = null;
				countdown.value = 0;
			}
		}, 1000);
	};

	const sendCode = async () => {
		if (countdown.value > 0) return;
		try {
			await sendAuthCode({ mobile: form.mobile, scene: 'reset' });
			startCountdown();
			uni.showToast({ title: '验证码已发送', icon: 'none' });
		} catch (error) {
			uni.showToast({ title: error.msg || error.message || '验证码发送失败', icon: 'none' });
		}
	};

	const goLogin = () => {
		uni.redirectTo({ url: '/packageUser/login' });
	};

	const submit = async () => {
		if (submitting.value) return;
		submitting.value = true;
		try {
			await resetPassword(form);
			uni.showToast({ title: '密码已重置', icon: 'success' });
			setTimeout(() => {
				uni.redirectTo({ url: '/packageUser/login' });
			}, 400);
		} catch (error) {
			uni.showToast({ title: error.message || '重置失败', icon: 'none' });
		} finally {
			submitting.value = false;
		}
	};
</script>

<style scoped>
	.page {
		min-height: 100vh;
		padding: 0 24rpx 48rpx;
		background: linear-gradient(180deg, #f1f3ea 0%, #f8f7f1 38%, #f2f1ea 100%);
		box-sizing: border-box;
	}

	.hero-section {
		position: relative;
		margin: 0 -24rpx;
		padding: 98rpx 42rpx 44rpx;
		border-radius: 0 0 52rpx 52rpx;
		background: linear-gradient(180deg, #17261a 0%, #213a24 36%, #355b2f 72%, #496d36 100%);
		overflow: hidden;
	}

	.hero-section::before {
		content: '';
		position: absolute;
		right: -90rpx;
		top: -20rpx;
		width: 360rpx;
		height: 360rpx;
		border-radius: 50%;
		background: radial-gradient(circle, rgba(124, 204, 72, 0.22) 0%, rgba(124, 204, 72, 0) 72%);
	}

	.hero-section::after {
		content: '';
		position: absolute;
		left: -120rpx;
		bottom: -120rpx;
		width: 420rpx;
		height: 260rpx;
		border-radius: 210rpx;
		background: radial-gradient(circle, rgba(249, 194, 95, 0.14) 0%, rgba(249, 194, 95, 0) 74%);
	}

	.hero-copy {
		position: relative;
		z-index: 2;
		width: 100%;
		max-width: 100%;
		padding-right: 0;
		box-sizing: border-box;
	}

	.hero-scene {
		position: relative;
		margin-top: 30rpx;
		height: 168rpx;
		border-radius: 30rpx;
		background: linear-gradient(180deg, rgba(255, 255, 255, 0.06) 0%, rgba(255, 255, 255, 0.02) 100%);
		border: 1rpx solid rgba(255, 255, 255, 0.08);
		overflow: hidden;
	}

	.scene-moon,
	.scene-glow,
	.ridge,
	.tent-wrap,
	.tent,
	.tent-door,
	.camp-fire {
		position: absolute;
	}

	.scene-moon {
		top: 26rpx;
		right: 40rpx;
		width: 62rpx;
		height: 62rpx;
		border-radius: 50%;
		background: #fff2b3;
		box-shadow: 0 0 28rpx rgba(255, 231, 145, 0.42);
	}

	.scene-glow {
		top: 8rpx;
		right: -8rpx;
		width: 160rpx;
		height: 160rpx;
		border-radius: 50%;
		background: radial-gradient(circle, rgba(255, 240, 174, 0.22) 0%, rgba(255, 240, 174, 0) 72%);
	}

	.scene-card {
		position: absolute;
		left: 26rpx;
		top: 14rpx;
		z-index: 3;
		max-width: 230rpx;
		padding: 14rpx 16rpx;
		border-radius: 20rpx;
		background: rgba(13, 26, 17, 0.34);
		backdrop-filter: blur(8rpx);
	}

	.scene-tag,
	.scene-text,
	.intro-title,
	.intro-note,
	.hero-title,
	.hero-desc,
	.field-label,
	.field-hint,
	.link-text,
	.agree-text {
		display: block;
	}

	.scene-tag {
		font-size: 20rpx;
		font-weight: 700;
		color: #eef7d8;
	}

	.scene-text {
		margin-top: 8rpx;
		font-size: 20rpx;
		line-height: 1.4;
		color: rgba(255, 255, 255, 0.78);
	}

	.ridge-back {
		left: -30rpx;
		right: -30rpx;
		bottom: 40rpx;
		height: 68rpx;
		background: linear-gradient(180deg, #345a37 0%, #26452a 100%);
		border-radius: 50% 50% 0 0;
	}

	.ridge-front {
		left: -40rpx;
		right: -40rpx;
		bottom: 0;
		height: 78rpx;
		background: linear-gradient(180deg, #223b25 0%, #182a1c 100%);
		border-radius: 55% 55% 0 0;
	}

	.tent-wrap {
		left: 54%;
		bottom: 22rpx;
		width: 92rpx;
		height: 72rpx;
		transform: translateX(-50%);
		z-index: 4;
	}

	.tent {
		left: 0;
		bottom: 0;
		width: 0;
		height: 0;
		border-left: 46rpx solid transparent;
		border-right: 46rpx solid transparent;
		border-bottom: 66rpx solid #d8dfb2;
	}

	.tent-door {
		left: 34rpx;
		bottom: 0;
		width: 0;
		height: 0;
		border-left: 12rpx solid transparent;
		border-right: 12rpx solid transparent;
		border-bottom: 24rpx solid #2f3a25;
	}

	.camp-fire {
		left: -10rpx;
		bottom: 8rpx;
		width: 18rpx;
		height: 18rpx;
		border-radius: 50%;
		background: radial-gradient(circle, #ffe38c 0%, #ff9838 58%, rgba(255, 152, 56, 0) 76%);
		box-shadow: 0 0 20rpx rgba(255, 152, 56, 0.55);
	}

	.hero-title {
		position: relative;
		z-index: 1;
		margin-top: 26rpx;
		font-size: 48rpx;
		font-weight: 800;
		line-height: 1.18;
		white-space: nowrap;
		display: inline-block;
		color: #ffffff;
	}

	.hero-desc {
		position: relative;
		z-index: 1;
		margin-top: 16rpx;
		font-size: 22rpx;
		line-height: 1.6;
		white-space: nowrap;
		display: inline-block;
		color: rgba(241, 244, 232, 0.78);
	}

	.login-shell {
		position: relative;
		margin-top: -26rpx;
		z-index: 2;
		border-radius: 36rpx;
		overflow: hidden;
		background: #fffdf7;
		box-shadow: 0 22rpx 56rpx rgba(35, 48, 28, 0.14);
	}

	.tabs-wrap,
	.form-area {
		background: #fffdf7;
	}

	.tabs-wrap {
		border-radius: 32rpx 32rpx 0 0;
		padding: 18rpx 18rpx 0;
	}

	.tabs-wrap :deep(.u-tabs) {
		background: #f5f0e1;
		border-radius: 999rpx;
		padding: 8rpx 8rpx 0;
	}

	.tabs-wrap :deep(.u-tabs__wrapper__nav__item) {
		padding: 0 30rpx;
	}

	.tabs-wrap :deep(.u-tabs__wrapper__nav__line) {
		height: 6rpx !important;
		border-radius: 999rpx;
	}

	.tabs-wrap :deep(.u-tabs__wrapper__nav__item__text) {
		font-size: 30rpx;
		white-space: nowrap;
	}

	.form-area {
		padding: 28rpx 28rpx 42rpx;
		border-radius: 0 0 32rpx 32rpx;
	}

	.intro-strip {
		margin-bottom: 24rpx;
		padding: 4rpx 6rpx 0;
	}

	.intro-title {
		font-size: 34rpx;
		font-weight: 800;
		color: #28311f;
	}

	.intro-note {
		margin-top: 8rpx;
		font-size: 22rpx;
		line-height: 1.6;
		color: #817966;
	}

	.field-list {
		padding-top: 4rpx;
	}

	.field-item {
		width: 100%;
		margin-bottom: 26rpx;
		box-sizing: border-box;
	}

	.field-row-head,
	.link-row,
	.code-row {
		display: flex;
	}

	.field-row-head,
	.link-row {
		justify-content: space-between;
		align-items: center;
		gap: 20rpx;
	}

	.field-label {
		margin-bottom: 14rpx;
		font-size: 22rpx;
		font-weight: 700;
		color: #5f5b49;
		padding-left: 4rpx;
	}

	.field-row-head .field-label {
		margin-bottom: 0;
	}

	.field-hint {
		font-size: 20rpx;
		color: #a49a82;
	}

	.field-box {
		display: flex;
		align-items: center;
		width: 100%;
		height: 92rpx;
		padding: 0 26rpx;
		border-radius: 24rpx;
		background: linear-gradient(180deg, #f7f0df 0%, #fdfaf1 100%);
		border: 2rpx solid #efe6d2;
		box-shadow: inset 0 1rpx 0 rgba(255, 255, 255, 0.88);
		box-sizing: border-box;
	}

	.code-box {
		flex: 1;
		min-width: 0;
	}

	.field-input {
		width: 100%;
		height: 100%;
		margin: 0;
		padding: 0;
		border: 0;
		background: transparent;
		font-size: 28rpx;
		color: #1f2937;
		box-sizing: content-box;
	}

	.field-input::placeholder {
		color: #b1a692;
	}

	.field-box:focus-within {
		background: #ffffff;
		border-color: #d9c08e;
		box-shadow: 0 0 0 8rpx rgba(217, 192, 142, 0.16);
	}

	.code-row {
		align-items: center;
		gap: 14rpx;
		width: 100%;
	}

	.code-btn {
		display: block;
		min-width: 176rpx;
		height: 92rpx;
		line-height: 92rpx;
		text-align: center;
		border-radius: 22rpx;
		font-size: 24rpx;
		font-weight: 700;
		color: #fffef8;
		background: linear-gradient(135deg, #f3b14c 0%, #db7b2f 100%);
		box-shadow: 0 12rpx 24rpx rgba(219, 123, 47, 0.24);
		flex-shrink: 0;
	}

	.code-btn.disabled {
		background: #d8ccb8;
		color: #857967;
		box-shadow: none;
	}

	.submit-btn {
		width: 100%;
		height: 100rpx;
		line-height: 100rpx;
		border-radius: 48rpx;
		border: none;
		box-sizing: border-box;
		font-size: 30rpx;
		font-weight: 700;
		color: #fffdf8;
		background: linear-gradient(135deg, #2f6b24 0%, #5d9e23 52%, #86c62d 100%);
		box-shadow: 0 18rpx 34rpx rgba(56, 103, 34, 0.24);
	}

	.submit-btn::after {
		border: none;
	}

	.submit-btn[disabled] {
		background: #c9d0b6;
		box-shadow: none;
		color: #f4f1e6;
	}

	.footer-link {
		display: block;
		margin-top: 30rpx;
		font-size: 24rpx;
		text-align: center;
		color: #6e7f2e;
		font-weight: 700;
	}

	@media screen and (max-width: 360px) {
		.hero-section {
			padding: 94rpx 34rpx 40rpx;
		}

		.hero-title {
			font-size: 42rpx;
		}

		.hero-desc {
			font-size: 20rpx;
		}

		.hero-scene {
			margin-top: 26rpx;
			height: 160rpx;
		}

		.field-row-head,
		.link-row,
		.code-row {
			flex-wrap: wrap;
		}

		.code-btn {
			width: 100%;
		}
	}
</style>
