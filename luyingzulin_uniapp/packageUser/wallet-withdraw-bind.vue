<template>
	<view class="page">
		<view v-if="pageLoading" class="loading-card"><text>加载收款账户...</text></view>
		<template v-else>
			<view class="hero-card">
				<text class="hero-title">绑定收款账户</text>
				<text class="hero-desc">提现前需绑定支付宝或微信收款方式，请填写与实名认证一致的信息，避免审核后无法到账。</text>
			</view>

			<view class="card">
				<text class="section-title">收款方式</text>
				<view class="tab-wrap">
					<u-tabs :list="accountTypes" keyName="label" :current="currentTabIndex" :scrollable="true" lineColor="#7ed321" :activeStyle="tabActiveStyle" :inactiveStyle="tabInactiveStyle" @change="handleTabChange"></u-tabs>
				</view>

				<view v-if="activeAccountType === 'alipay'" class="account-panel">
					<text class="field-label">收款姓名</text>
					<input v-model="formMap.alipay.accountName" class="form-input" maxlength="20" placeholder="请输入支付宝实名姓名" />
					<text class="field-label">支付宝账号</text>
					<input v-model="formMap.alipay.accountNumber" class="form-input" maxlength="60" placeholder="请输入手机号或邮箱" />
					<text class="field-tip">请填写与支付宝实名认证一致的姓名和账号。</text>
				</view>

				<view v-else class="account-panel">
					<text class="field-label">微信收款码</text>
					<view class="qr-upload-row">
						<view v-if="formMap.wechat.qrCodeUrl" class="qr-preview" @click="previewQrCode">
							<image class="qr-image" :src="formMap.wechat.qrCodeUrl" mode="aspectFill"></image>
							<view class="qr-remove" @click.stop="removeQrCode">×</view>
						</view>
						<view v-else class="qr-upload-add" @click="chooseQrCode">
							<text class="qr-upload-add__plus">+</text>
							<text class="qr-upload-add__text">上传收款码</text>
						</view>
					</view>
					<text class="field-tip">仅需上传微信收款码图片，无需填写微信号。</text>
				</view>
			</view>

			<button class="submit-btn" :loading="savingAccount" :disabled="savingAccount" @click="saveAccountInfo">保存收款账户</button>
		</template>
	</view>
</template>

<script setup>
	import { computed, reactive, ref } from 'vue';
	import { onShow } from '@dcloudio/uni-app';
	import { fetchWalletDetail, saveWithdrawAccount } from '@/common/api/wallet';
	import { validateWithdrawAccount } from '@/common/api/assetsMappers';
	import { chooseAndUploadImages } from '@/common/upload';
	import { ensureLogin } from '@/common/auth';

	const accountTypes = [
		{ key: 'alipay', label: '支付宝' },
		{ key: 'wechat', label: '微信收款' }
	];
	const activeAccountType = ref('alipay');
	const pageLoading = ref(false);
	const savingAccount = ref(false);
	const uploadingQr = ref(false);
	const tabActiveStyle = { color: '#59b50e', fontWeight: '700' };
	const tabInactiveStyle = { color: '#6b7280' };
	const formMap = reactive({
		alipay: { accountName: '', accountNumber: '', qrCodeUrl: '' },
		wechat: { accountName: '', accountNumber: '', qrCodeUrl: '' }
	});

	const syncAccounts = async () => {
		pageLoading.value = true;
		try {
			const detail = await fetchWalletDetail();
			formMap.alipay = { accountName: '', accountNumber: '', qrCodeUrl: '', ...(detail.withdrawAccounts?.alipay || {}) };
			formMap.wechat = { accountName: '', accountNumber: '', qrCodeUrl: '', ...(detail.withdrawAccounts?.wechat || {}) };
		} catch (error) {
			// toast 已由 http 层处理
		} finally {
			pageLoading.value = false;
		}
	};

	onShow(() => {
		if (!ensureLogin('/packageUser/wallet-withdraw-bind')) return;
		syncAccounts();
	});

	const currentTabIndex = computed(() => Math.max(accountTypes.findIndex((item) => item.key === activeAccountType.value), 0));

	const handleTabChange = (item) => {
		activeAccountType.value = item.key;
	};

	const chooseQrCode = async () => {
		if (uploadingQr.value) return;
		uploadingQr.value = true;
		try {
			const urls = await chooseAndUploadImages({ count: 1 });
			if (urls?.[0]) {
				formMap.wechat.qrCodeUrl = urls[0];
			}
		} catch (error) {
			uni.showToast({ title: error?.message || error?.msg || '上传失败', icon: 'none' });
		} finally {
			uploadingQr.value = false;
		}
	};

	const removeQrCode = () => {
		formMap.wechat.qrCodeUrl = '';
	};

	const previewQrCode = () => {
		if (!formMap.wechat.qrCodeUrl) return;
		uni.previewImage({ urls: [formMap.wechat.qrCodeUrl] });
	};

	const saveAccountInfo = async () => {
		const accountError = validateWithdrawAccount(activeAccountType.value, formMap[activeAccountType.value]);
		if (accountError) {
			uni.showToast({ title: accountError, icon: 'none' });
			return;
		}
		if (savingAccount.value) return;
		savingAccount.value = true;
		try {
			await saveWithdrawAccount({
				accountType: activeAccountType.value,
				accountName: formMap.alipay.accountName,
				accountNumber: formMap.alipay.accountNumber,
				qrCodeUrl: formMap.wechat.qrCodeUrl
			});
			await syncAccounts();
			uni.showToast({ title: '收款账户已保存', icon: 'success' });
			setTimeout(() => {
				const pages = getCurrentPages();
				const hasWithdrawPage = pages.some((page) => {
					const route = page.route || '';
					return route.includes('wallet-withdraw')
						&& !route.includes('wallet-withdraw-bind')
						&& !route.includes('wallet-withdraw-guide');
				});
				if (hasWithdrawPage) {
					uni.navigateBack();
					return;
				}
				uni.redirectTo({ url: '/packageUser/wallet-withdraw' });
			}, 400);
		} catch (error) {
			uni.showToast({ title: error?.message || error?.msg || '保存失败', icon: 'none' });
		} finally {
			savingAccount.value = false;
		}
	};
</script>

<style scoped>
	.page { box-sizing: border-box; min-height: 100vh; padding: 24rpx; background: #f5f7ff; overflow-x: hidden; }
	.loading-card { margin-bottom: 24rpx; padding: 48rpx 32rpx; border-radius: 28rpx; background: #fff; text-align: center; color: #8a94a6; }
	.hero-card, .card { box-sizing: border-box; width: 100%; padding: 32rpx 28rpx; border-radius: 28rpx; background: #ffffff; overflow: hidden; }
	.hero-card { background: linear-gradient(160deg, #7ed321 0%, #6bc01a 50%, #5aad15 100%); }
	.hero-title, .hero-desc, .section-title, .field-label, .field-tip { display: block; }
	.hero-title { font-size: 30rpx; font-weight: 700; color: #ffffff; }
	.hero-desc { margin-top: 12rpx; font-size: 22rpx; line-height: 1.7; color: rgba(255,255,255,0.88); }
	.card { margin-top: 24rpx; }
	.section-title { font-size: 30rpx; font-weight: 700; color: #1f2937; }
	.tab-wrap { margin-top: 18rpx; }
	.tab-wrap :deep(.u-tabs__wrapper__nav__item) { padding: 0 28rpx; }
	.tab-wrap :deep(.u-tabs__wrapper__nav__item__text) { font-size: 24rpx; white-space: nowrap; }
	.account-panel { margin-top: 8rpx; }
	.field-label { margin-top: 20rpx; font-size: 24rpx; font-weight: 600; color: #374151; }
	.form-input { width: 100%; height: 88rpx; margin-top: 12rpx; padding: 0 24rpx; border-radius: 22rpx; background: #f8fafc; font-size: 26rpx; color: #1f2937; }
	.field-tip { margin-top: 12rpx; font-size: 22rpx; line-height: 1.7; color: #8a94a6; }
	.qr-upload-row { display: flex; margin-top: 12rpx; }
	.qr-preview, .qr-upload-add { width: 220rpx; height: 220rpx; border-radius: 24rpx; overflow: hidden; background: #f8fafc; }
	.qr-preview { position: relative; }
	.qr-image { width: 100%; height: 100%; }
	.qr-remove { position: absolute; top: 10rpx; right: 10rpx; width: 44rpx; height: 44rpx; line-height: 40rpx; text-align: center; border-radius: 50%; font-size: 30rpx; color: #fff; background: rgba(0,0,0,0.45); }
	.qr-upload-add { display: flex; flex-direction: column; align-items: center; justify-content: center; border: 2rpx dashed #d1d5db; }
	.qr-upload-add__plus { font-size: 52rpx; line-height: 1; color: #59b50e; }
	.qr-upload-add__text { margin-top: 10rpx; font-size: 22rpx; color: #6b7280; }
	.submit-btn { box-sizing: border-box; width: 100%; margin-top: 28rpx; height: 88rpx; line-height: 88rpx; border-radius: 44rpx; font-size: 28rpx; font-weight: 700; color: #ffffff; background: linear-gradient(135deg, #7ed321, #6bc01a); border: none; }
</style>
