<template>
	<view class="page">
		<view class="hero-section">
			<view class="hero-copy">
				<text class="hero-title">夜幕营地已点亮</text>
				<text class="hero-desc">登录后继续下单，订单与钱包直达。</text>
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

		<view class="login-shell" :class="{ 'login-shell--mp': isMpWeixin }">
			<view v-if="showLoginTabs" class="tabs-wrap">
				<view class="login-tabs">
					<view
						v-for="item in tabs"
						:key="item.key"
						class="login-tab"
						:class="{ 'login-tab--active': mode === item.key }"
						@click="handleTabChange(item)"
					>
						<text class="login-tab__text">{{ item.label }}</text>
					</view>
				</view>
			</view>

			<view class="form-area">
				<view class="intro-strip" :class="{ 'intro-strip--mp': isMpWeixin && !showWxBind }">
					<text class="intro-title">{{ currentModeLabel }}</text>
					<text class="intro-note">{{ currentModeNote }}</text>
				</view>

				<view v-if="mode === 'password'" class="field-list">
					<view class="field-item">
						<text class="field-label">账号或手机号</text>
						<view class="field-box">
							<input v-model="account" class="field-input" maxlength="20" placeholder="请输入账号或手机号" />
						</view>
					</view>
					<view class="field-item">
						<text class="field-label">登录密码</text>
						<view class="field-box">
							<input v-model="password" class="field-input" password placeholder="请输入密码" />
						</view>
					</view>
				</view>

				<template v-else-if="mode === 'code'">
					<view class="field-list">
						<view class="field-item">
							<text class="field-label">手机号</text>
							<view class="field-box">
								<input v-model="mobile" class="field-input" type="number" maxlength="11" placeholder="请输入手机号" />
							</view>
						</view>
						<view class="field-item">
							<view class="field-row-head">
								<text class="field-label">短信验证码</text>
								<text class="field-hint">发送到当前手机号</text>
							</view>
							<view class="code-row">
								<view class="field-box code-box">
									<input v-model="code" class="field-input" type="number" maxlength="6" placeholder="请输入验证码" />
								</view>
								<text class="code-btn" :class="{ disabled: countdown > 0 }" @click="sendCode">{{ countdown > 0 ? `${countdown}s` : '发送验证码' }}</text>
							</view>
						</view>
					</view>
				</template>

				<template v-else-if="showWxBind">
					<view class="bind-tip">
						<text class="bind-title">绑定手机号</text>
						<text class="bind-desc">{{ isMpWeixin ? '微信授权成功，请点击下方按钮授权手机号完成登录。' : '微信授权成功，请补充手机号完成登录。' }}</text>
					</view>
					<view v-if="!isMpWeixin" class="field-item">
						<text class="field-label">手机号</text>
						<view class="field-box">
							<input v-model="mobile" class="field-input" type="number" maxlength="11" placeholder="请输入手机号" />
						</view>
					</view>
					<view v-else class="mp-phone-panel">
						<view class="mp-phone-icon">
							<up-icon name="phone-fill" size="40" color="#ffffff"></up-icon>
						</view>
						<text class="mp-phone-title">微信官方授权</text>
						<text class="mp-phone-desc">使用微信绑定的手机号，安全快捷，无需手动输入。</text>
					</view>
				</template>

				<view v-else-if="isMpWeixin" class="mp-wechat-panel">
					<view class="mp-wechat-badge">
						<view class="mp-wechat-icon">
							<up-icon name="weixin-fill" size="44" color="#ffffff"></up-icon>
						</view>
					</view>
					<text class="mp-wechat-title">微信一键登录</text>
					<text class="mp-wechat-desc">授权微信账号即可安全登录，订单进度与钱包余额自动同步。</text>
					<view class="mp-wechat-features">
						<view class="mp-feature-chip">
							<up-icon name="checkmark-circle-fill" size="14" color="#07c160"></up-icon>
							<text>无需记密码</text>
						</view>
						<view class="mp-feature-chip">
							<up-icon name="checkmark-circle-fill" size="14" color="#07c160"></up-icon>
							<text>订单实时同步</text>
						</view>
						<view class="mp-feature-chip">
							<up-icon name="checkmark-circle-fill" size="14" color="#07c160"></up-icon>
							<text>官方安全授权</text>
						</view>
					</view>
				</view>

				<view v-else class="wechat-panel">
					<text class="wechat-title">微信快捷登录</text>
					<text class="wechat-desc">授权后可快速进入账号。</text>
				</view>

				<view v-if="!isMpWeixin" class="link-row">
					<text class="link-text" @click="goRegister">注册账号</text>
					<text v-if="mode !== 'wechat'" class="link-text" @click="goForgotPassword">忘记密码</text>
				</view>

				<view class="agree-row" @click="agreed = !agreed">
					<view class="agree-icon"><up-icon :name="agreed ? 'checkmark-circle-fill' : 'checkmark-circle'" size="20" :color="agreed ? '#59b50e' : '#c1c7d2'"></up-icon></view>
					<text class="agree-text">我已阅读并同意《用户协议》《隐私政策》</text>
				</view>

				<button
					v-if="showWxBind && isMpWeixin"
					class="submit-btn submit-btn--mp-wechat"
					open-type="getPhoneNumber"
					:loading="submitting"
					:disabled="submitting"
					@getphonenumber="handleWxPhoneBind"
				>
					<up-icon name="phone-fill" size="22" color="#ffffff"></up-icon>
					<text>微信授权手机号</text>
				</button>
				<button
					v-else
					class="submit-btn"
					:class="{ 'submit-btn--mp-wechat': isMpWeixin && !showWxBind }"
					:loading="submitting"
					:disabled="submitting"
					@click="submit"
				>
					<up-icon v-if="isMpWeixin && !showWxBind" name="weixin-fill" size="22" color="#ffffff"></up-icon>
					<text>{{ submitButtonText }}</text>
				</button>
			</view>
		</view>
	</view>
</template>

<script setup>
	import { computed, ref } from 'vue';
	import { onLoad, onUnload } from '@dcloudio/uni-app';
	import { getDefaultLoginMode, getLoginModes, loginByCode, loginByPassword, loginByWechat, registerByWechat, sendAuthCode, tryCompleteMpOAuthLogin } from '@/common/auth';
	import { buildRegisterPageUrl, navigateAfterAuth, parseLoginRedirect, validateLoginSubmit } from '@/common/userValidators';
	import { resolveErrorMessage } from '@/common/errorCode';

	const tabs = getLoginModes();
	const isMpWeixin = ref(false);
	// #ifdef MP-WEIXIN
	isMpWeixin.value = true;
	// #endif
	const mode = ref(getDefaultLoginMode());
	const account = ref('');
	const mobile = ref('');
	const password = ref('');
	const code = ref('');
	const agreed = ref(true);
	const redirect = ref('');
	const countdown = ref(0);
	const submitting = ref(false);
	const showWxBind = ref(false);
	const showLoginTabs = computed(() => !isMpWeixin.value && tabs.length > 1);
	const currentModeLabel = computed(() => {
		if (showWxBind.value) {
			return '绑定手机号';
		}
		if (isMpWeixin.value) {
			return '欢迎使用露营租赁';
		}
		const current = tabs.find((item) => item.key === mode.value);
		return current ? current.label : '登录';
	});
	const currentModeNote = computed(() => {
		if (showWxBind.value) {
			return '完成绑定后即可继续登录';
		}
		if (isMpWeixin.value) {
			return '使用微信授权，快速开始你的露营之旅';
		}
		if (mode.value === 'password') {
			return '适合常用账号快速进入';
		}
		if (mode.value === 'code') {
			return '短信校验更适合临时登录';
		}
		return '授权后直接进入账号';
	});
	const submitButtonText = computed(() => {
		if (showWxBind.value) {
			return isMpWeixin.value ? '微信授权手机号' : '绑定并登录';
		}
		if (isMpWeixin.value || mode.value === 'wechat') {
			return '微信一键登录';
		}
		return '立即登录';
	});
	let countdownTimer = null;

	onLoad(async (options) => {
		redirect.value = parseLoginRedirect(options);
		try {
			const session = await tryCompleteMpOAuthLogin();
			if (session) {
				afterLogin();
			}
		} catch (error) {
			if (error.message === 'NEED_BIND_PHONE') {
				showWxBind.value = true;
				mode.value = 'wechat';
				return;
			}
			uni.showToast({ title: error.message || '微信登录失败', icon: 'none' });
		}
	});

	onUnload(() => {
		if (countdownTimer) {
			clearInterval(countdownTimer);
			countdownTimer = null;
		}
	});

	const startCountdown = () => {
		countdown.value = 60;
		if (countdownTimer) {
			clearInterval(countdownTimer);
		}
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
			await sendAuthCode({ mobile: mobile.value, scene: 'login' });
			startCountdown();
			uni.showToast({ title: '验证码已发送', icon: 'none' });
		} catch (error) {
			uni.showToast({ title: error.msg || error.message || '验证码发送失败', icon: 'none' });
		}
	};

	const handleTabChange = (item) => {
		mode.value = item.key;
	};

	const afterLogin = () => {
		uni.showToast({ title: '登录成功', icon: 'success' });
		setTimeout(() => {
			navigateAfterAuth(redirect.value);
		}, 400);
	};

	const goRegister = () => {
		uni.navigateTo({ url: buildRegisterPageUrl(redirect.value) });
	};

	const goForgotPassword = () => {
		uni.navigateTo({ url: '/packageUser/forgot-password' });
	};

	const handleWxPhoneBind = async (event) => {
		if (submitting.value) return;
		if (!agreed.value) {
			uni.showToast({ title: '请先阅读并同意用户协议', icon: 'none' });
			return;
		}
		const detail = event?.detail || {};
		if (detail.errMsg && detail.errMsg !== 'getPhoneNumber:ok') {
			uni.showToast({ title: '已取消授权', icon: 'none' });
			return;
		}
		const phoneCode = detail.code;
		if (!phoneCode) {
			uni.showToast({ title: '获取手机号失败，请重试', icon: 'none' });
			return;
		}
		submitting.value = true;
		try {
			await registerByWechat({ phoneCode });
			afterLogin();
		} catch (error) {
			const message = resolveErrorMessage(error);
			if (message && !message.includes('redirect')) {
				uni.showToast({ title: message, icon: 'none' });
			}
		} finally {
			submitting.value = false;
		}
	};

	const submit = async () => {
		if (submitting.value) return;
		const validationMessage = validateLoginSubmit({
			mode: mode.value,
			agreed: agreed.value,
			account: account.value,
			password: password.value,
			mobile: mobile.value,
			code: code.value
		});
		if (validationMessage) {
			uni.showToast({ title: validationMessage, icon: 'none' });
			return;
		}

		submitting.value = true;
		try {
			if (showWxBind.value) {
				if (!mobile.value) {
					throw new Error('请填写手机号');
				}
				await registerByWechat({ phone: mobile.value });
			} else if (mode.value === 'password') {
				await loginByPassword({ account: account.value, password: password.value });
			} else if (mode.value === 'code') {
				await loginByCode({ mobile: mobile.value, code: code.value });
			} else {
				await loginByWechat();
			}
			afterLogin();
		} catch (error) {
			if (error.message === 'NEED_BIND_PHONE') {
				showWxBind.value = true;
				uni.showToast({ title: '请绑定手机号', icon: 'none' });
			} else {
				const message = resolveErrorMessage(error);
				if (message && !message.includes('redirect')) {
					uni.showToast({ title: message, icon: 'none' });
				}
			}
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
	.scene-text {
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

	.hero-badge,
	.hero-title,
	.hero-desc,
	.field-label,
	.field-hint,
	.bind-title,
	.bind-desc,
	.wechat-title,
	.wechat-desc,
	.mp-wechat-title,
	.mp-wechat-desc,
	.link-text,
	.agree-text {
		display: block;
	}

	.hero-badge {
		position: relative;
		z-index: 1;
		display: inline-block;
		padding: 12rpx 22rpx;
		border-radius: 999rpx;
		font-size: 22rpx;
		font-weight: 700;
		letter-spacing: 2rpx;
		color: #f4f2d8;
		background: rgba(255, 255, 255, 0.1);
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

	.login-shell--mp {
		margin-top: -18rpx;
	}

	.login-shell--mp .form-area {
		padding-top: 36rpx;
	}

	.tabs-wrap,
	.form-area {
		background: #fffdf7;
	}

	.tabs-wrap {
		border-radius: 32rpx 32rpx 0 0;
		padding: 18rpx 18rpx 0;
	}

	.login-tabs {
		display: flex;
		align-items: stretch;
		background: #f5f0e1;
		border-radius: 999rpx;
		padding: 8rpx 8rpx 0;
	}

	.login-tab {
		position: relative;
		flex: 1;
		display: flex;
		align-items: center;
		justify-content: center;
		min-width: 0;
		padding: 0 30rpx;
		min-height: 72rpx;
	}

	.login-tab__text {
		font-size: 30rpx;
		line-height: 1.2;
		white-space: nowrap;
		color: #6b7280;
	}

	.login-tab--active .login-tab__text {
		color: #59b50e;
		font-weight: 700;
	}

	.login-tab--active::after {
		content: '';
		position: absolute;
		left: 50%;
		bottom: 0;
		width: 48rpx;
		height: 6rpx;
		border-radius: 999rpx;
		background: #7ed321;
		transform: translateX(-50%);
	}

	.form-area {
		padding: 28rpx 28rpx 42rpx;
		border-radius: 0 0 32rpx 32rpx;
	}

	.intro-strip {
		margin-bottom: 24rpx;
		padding: 4rpx 6rpx 0;
	}

	.intro-strip--mp {
		text-align: center;
		margin-bottom: 12rpx;
	}

	.intro-title,
	.intro-note {
		display: block;
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
	.code-row,
	.agree-row {
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

	.bind-tip,
	.wechat-panel {
		margin: 8rpx 0 24rpx;
		padding: 32rpx 28rpx;
		border-radius: 24rpx;
		background: linear-gradient(180deg, #f8f1df 0%, #f5ead5 100%);
		border: 2rpx solid #efe0c2;
	}

	.bind-title,
	.wechat-title {
		font-size: 30rpx;
		font-weight: 700;
		color: #493823;
	}

	.bind-desc,
	.wechat-desc {
		margin-top: 10rpx;
		font-size: 24rpx;
		line-height: 1.65;
		color: #86735a;
	}

	.mp-wechat-panel {
		display: flex;
		flex-direction: column;
		align-items: center;
		margin: 12rpx 0 28rpx;
		padding: 40rpx 32rpx 36rpx;
		border-radius: 28rpx;
		background: linear-gradient(180deg, #f4fbf4 0%, #eef8ee 100%);
		border: 2rpx solid rgba(7, 193, 96, 0.16);
		box-shadow: inset 0 1rpx 0 rgba(255, 255, 255, 0.92);
	}

	.mp-wechat-badge {
		display: flex;
		align-items: center;
		justify-content: center;
		width: 120rpx;
		height: 120rpx;
		border-radius: 50%;
		background: radial-gradient(circle, rgba(7, 193, 96, 0.14) 0%, rgba(7, 193, 96, 0.04) 72%);
	}

	.mp-wechat-icon {
		display: flex;
		align-items: center;
		justify-content: center;
		width: 88rpx;
		height: 88rpx;
		border-radius: 50%;
		background: linear-gradient(135deg, #09d66d 0%, #07c160 100%);
		box-shadow: 0 12rpx 28rpx rgba(7, 193, 96, 0.28);
	}

	.mp-wechat-title {
		margin-top: 24rpx;
		font-size: 34rpx;
		font-weight: 800;
		color: #1f2937;
	}

	.mp-wechat-desc {
		margin-top: 12rpx;
		font-size: 24rpx;
		line-height: 1.7;
		color: #667085;
		text-align: center;
	}

	.mp-wechat-features {
		display: flex;
		flex-wrap: wrap;
		justify-content: center;
		gap: 12rpx;
		margin-top: 28rpx;
		width: 100%;
	}

	.mp-feature-chip {
		display: flex;
		align-items: center;
		gap: 8rpx;
		padding: 10rpx 18rpx;
		border-radius: 999rpx;
		background: rgba(255, 255, 255, 0.88);
		border: 1rpx solid rgba(7, 193, 96, 0.12);
	}

	.mp-feature-chip text {
		font-size: 22rpx;
		color: #3f5f47;
	}

	.mp-phone-panel {
		display: flex;
		flex-direction: column;
		align-items: center;
		margin-top: 8rpx;
		padding: 36rpx 28rpx;
		border-radius: 24rpx;
		background: linear-gradient(180deg, #f4fbf4 0%, #eef8ee 100%);
		border: 2rpx solid rgba(7, 193, 96, 0.16);
	}

	.mp-phone-icon {
		display: flex;
		align-items: center;
		justify-content: center;
		width: 88rpx;
		height: 88rpx;
		border-radius: 50%;
		background: linear-gradient(135deg, #09d66d 0%, #07c160 100%);
		box-shadow: 0 12rpx 28rpx rgba(7, 193, 96, 0.24);
	}

	.mp-phone-title,
	.mp-phone-desc {
		display: block;
		text-align: center;
	}

	.mp-phone-title {
		margin-top: 20rpx;
		font-size: 28rpx;
		font-weight: 700;
		color: #1f2937;
	}

	.mp-phone-desc {
		margin-top: 10rpx;
		font-size: 22rpx;
		line-height: 1.65;
		color: #667085;
	}

	.link-row {
		margin: 8rpx 4rpx 28rpx;
	}

	.link-text {
		font-size: 24rpx;
		font-weight: 700;
		color: #6e7f2e;
	}

	.agree-row {
		align-items: flex-start;
		gap: 12rpx;
		margin: 0 4rpx 32rpx;
	}

	.agree-icon,
	.submit-btn {
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.agree-icon {
		width: 36rpx;
		height: 36rpx;
		margin-top: 2rpx;
		flex-shrink: 0;
	}

	.agree-text {
		flex: 1;
		min-width: 0;
		font-size: 22rpx;
		line-height: 1.6;
		color: #837d6e;
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
		gap: 12rpx;
	}

	.submit-btn--mp-wechat {
		background: linear-gradient(135deg, #09d66d 0%, #07c160 52%, #06ad56 100%);
		box-shadow: 0 18rpx 34rpx rgba(7, 193, 96, 0.28);
	}

	.submit-btn::after {
		border: none;
	}

	.submit-btn[disabled] {
		background: #c9d0b6;
		box-shadow: none;
		color: #f4f1e6;
	}

	@media screen and (max-width: 360px) {
		.hero-section {
			padding: 94rpx 34rpx 40rpx;
		}

		.hero-copy {
			padding-right: 0;
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
