<template>
	<view class="page">
		<view v-if="pageLoading" class="loading-card"><text>加载提现信息...</text></view>
		<template v-else>
			<view class="hero-card">
				<text class="hero-title">余额提现</text>
				<text class="hero-amount">¥{{ summary.balance.toFixed(2) }}</text>
				<text class="hero-desc">可提现余额，提交申请后进入审核，审核通过后按收款方式到账。</text>
			</view>

			<view class="card">
				<view class="section-head">
					<text class="section-title">提现方式</text>
					<text class="manage-link" @click="goBind">管理收款账户</text>
				</view>
				<view class="tab-wrap">
					<u-tabs :list="boundAccountTypes" keyName="label" :current="currentTabIndex" :scrollable="true" lineColor="#7ed321" :activeStyle="tabActiveStyle" :inactiveStyle="tabInactiveStyle" @change="handleTabChange"></u-tabs>
				</view>
				<text class="account-display">{{ boundAccountDisplay }}</text>
			</view>

			<view class="card">
				<text class="section-title">提现金额</text>
				<input v-model="amountInput" class="amount-input" type="digit" placeholder="请输入提现金额" />
				<view class="amount-row">
					<text class="amount-tip">单次提现 ¥{{ withdrawConfig.minAmount }} ~ ¥{{ withdrawConfig.maxAmount }}</text>
					<text class="all-btn" @click="fillMaxAmount">全部提现</text>
				</view>
				<text class="rule-text">手续费 {{ feeRateText }}，实际到账 ¥{{ actualAmountText }}</text>
				<text class="rule-text">{{ validateMessage || '提现申请提交后将进入审核，审核通过后到账。' }}</text>
			</view>

			<button class="submit-btn" :loading="submitting" :disabled="submitting" @click="submit">提交提现申请</button>
		</template>
	</view>
</template>

<script setup>
	import { computed, ref } from 'vue';
	import { onShow } from '@dcloudio/uni-app';
	import {
		calcWithdrawActualAmount,
		fetchWalletDetail,
		formatWithdrawFeeRate,
		submitWithdraw,
		validateWithdrawAmount
	} from '@/common/api/wallet';
	import {
		formatWithdrawAccountDisplay,
		getBoundWithdrawAccountTypes,
		hasAnyWithdrawAccountBound
	} from '@/common/api/assetsMappers';
	import { ensureLogin } from '@/common/auth';

	const accountTypeLabels = {
		alipay: '支付宝',
		wechat: '微信收款'
	};

	const summary = ref({
		balance: 0,
		withdrawConfig: { minAmount: 1, maxAmount: 5000, serviceFeeRate: 0.006 },
		withdrawAccounts: { alipay: {}, wechat: {} }
	});
	const amountInput = ref('');
	const pageLoading = ref(false);
	const submitting = ref(false);
	const activeAccountType = ref('alipay');
	const tabActiveStyle = { color: '#59b50e', fontWeight: '700' };
	const tabInactiveStyle = { color: '#6b7280' };

	const syncWallet = async () => {
		pageLoading.value = true;
		try {
			const detail = await fetchWalletDetail();
			if (!hasAnyWithdrawAccountBound(detail.withdrawAccounts || {})) {
				uni.redirectTo({ url: '/packageUser/wallet-withdraw-guide' });
				return;
			}
			summary.value = detail;
			const boundTypes = getBoundWithdrawAccountTypes(detail.withdrawAccounts || {});
			if (boundTypes.length && !boundTypes.includes(activeAccountType.value)) {
				activeAccountType.value = boundTypes[0];
			}
		} catch (error) {
			// toast 已由 http 层处理
		} finally {
			pageLoading.value = false;
		}
	};

	onShow(() => {
		if (!ensureLogin('/packageUser/wallet-withdraw')) return;
		syncWallet();
	});

	const boundTypeKeys = computed(() => getBoundWithdrawAccountTypes(summary.value.withdrawAccounts || {}));
	const boundAccountTypes = computed(() => boundTypeKeys.value.map((key) => ({ key, label: accountTypeLabels[key] || key })));
	const currentTabIndex = computed(() => Math.max(boundAccountTypes.value.findIndex((item) => item.key === activeAccountType.value), 0));
	const withdrawConfig = computed(() => summary.value.withdrawConfig || {});
	const previewAmount = computed(() => Number(amountInput.value || 0));
	const feeRateText = computed(() => formatWithdrawFeeRate(withdrawConfig.value.serviceFeeRate));
	const actualAmountText = computed(() => calcWithdrawActualAmount(previewAmount.value, withdrawConfig.value).toFixed(2));
	const validateMessage = computed(() => amountInput.value ? validateWithdrawAmount(previewAmount.value, summary.value.balance, withdrawConfig.value) : '');
	const boundAccountDisplay = computed(() => formatWithdrawAccountDisplay(
		activeAccountType.value,
		summary.value.withdrawAccounts?.[activeAccountType.value] || {}
	));

	const handleTabChange = (item) => {
		activeAccountType.value = item.key;
	};

	const goBind = () => {
		uni.navigateTo({ url: '/packageUser/wallet-withdraw-bind' });
	};

	const fillMaxAmount = () => {
		const maxValue = Math.min(Number(summary.value.balance || 0), Number(withdrawConfig.value.maxAmount || 0) || Number(summary.value.balance || 0));
		if (maxValue <= 0) return;
		amountInput.value = maxValue.toFixed(2);
	};

	const submit = async () => {
		const amountError = validateWithdrawAmount(previewAmount.value, summary.value.balance, withdrawConfig.value);
		if (amountError) {
			uni.showToast({ title: amountError, icon: 'none' });
			return;
		}
		if (submitting.value) return;
		submitting.value = true;
		try {
			await submitWithdraw({ amount: amountInput.value, accountType: activeAccountType.value });
			await syncWallet();
			uni.showToast({ title: '提现申请已提交', icon: 'success' });
			setTimeout(() => {
				uni.redirectTo({ url: '/packageUser/wallet-withdraw-records' });
			}, 400);
		} catch (error) {
			uni.showToast({ title: error?.message || error?.msg || '提现失败', icon: 'none' });
		} finally {
			submitting.value = false;
		}
	};
</script>

<style scoped>
	.page { box-sizing: border-box; min-height: 100vh; padding: 24rpx; background: #f5f7ff; overflow-x: hidden; }
	.loading-card { margin-bottom: 24rpx; padding: 48rpx 32rpx; border-radius: 28rpx; background: #fff; text-align: center; color: #8a94a6; }
	.hero-card, .card { box-sizing: border-box; width: 100%; padding: 32rpx 28rpx; border-radius: 28rpx; background: #ffffff; overflow: hidden; }
	.hero-card { background: linear-gradient(160deg, #7ed321 0%, #6bc01a 50%, #5aad15 100%); }
	.hero-title, .hero-amount, .hero-desc, .section-title, .amount-tip, .all-btn, .rule-text, .account-display { display: block; }
	.hero-title { font-size: 30rpx; color: rgba(255,255,255,0.92); }
	.hero-amount { margin-top: 18rpx; font-size: 64rpx; font-weight: 800; color: #ffffff; }
	.hero-desc { margin-top: 12rpx; font-size: 22rpx; line-height: 1.7; color: rgba(255,255,255,0.88); }
	.card { margin-top: 24rpx; }
	.section-head { display: flex; align-items: center; justify-content: space-between; gap: 16rpx; }
	.section-title { font-size: 30rpx; font-weight: 700; color: #1f2937; }
	.manage-link { flex-shrink: 0; font-size: 24rpx; color: #59b50e; }
	.tab-wrap { margin-top: 18rpx; }
	.tab-wrap :deep(.u-tabs__wrapper__nav__item) { padding: 0 28rpx; }
	.tab-wrap :deep(.u-tabs__wrapper__nav__item__text) { font-size: 24rpx; white-space: nowrap; }
	.account-display { margin-top: 16rpx; font-size: 24rpx; line-height: 1.6; color: #6b7280; }
	.amount-input { width: 100%; height: 88rpx; margin-top: 12rpx; padding: 0 24rpx; border-radius: 22rpx; background: #f8fafc; font-size: 26rpx; color: #1f2937; }
	.amount-row { display: flex; align-items: center; justify-content: space-between; gap: 16rpx; margin-top: 18rpx; }
	.amount-tip { flex: 1; min-width: 0; font-size: 22rpx; color: #8a94a6; }
	.all-btn { flex-shrink: 0; font-size: 24rpx; color: #59b50e; }
	.rule-text { margin-top: 10rpx; font-size: 22rpx; line-height: 1.7; color: #6b7280; }
	.submit-btn { box-sizing: border-box; width: 100%; margin-top: 28rpx; height: 88rpx; line-height: 88rpx; border-radius: 44rpx; font-size: 28rpx; font-weight: 700; color: #ffffff; background: linear-gradient(135deg, #7ed321, #6bc01a); border: none; }
</style>
