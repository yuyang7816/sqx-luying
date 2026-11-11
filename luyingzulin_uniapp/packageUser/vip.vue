<template>
	<view class="page">
		<view v-if="loading" class="loading-text">加载中...</view>
		<template v-else>
			<view class="vip-card">
				<text class="vip-level">{{ vipLevel }}</text>
				<text class="vip-desc">{{ vipDesc }}</text>
				<view class="vip-stats">
					<view v-for="item in stats" :key="item.label" class="vip-stat">
						<text class="vip-num">{{ item.value }}</text>
						<text class="vip-label">{{ item.label }}</text>
					</view>
				</view>
			</view>

			<view class="card">
				<text class="section-title">会员权益</text>
				<view v-for="item in benefits" :key="item.title" class="benefit-row">
					<view class="benefit-icon"><up-icon :name="item.icon || 'checkmark-circle-fill'" size="22" color="#7ed321"></up-icon></view>
					<view class="benefit-main">
						<text class="benefit-title">{{ item.title }}</text>
						<text class="benefit-desc">{{ item.content || item.desc }}</text>
					</view>
				</view>
			</view>

			<view class="card">
				<text class="section-title">会员套餐</text>
				<view v-if="!plans.length" class="empty-tip">暂无可用套餐</view>
				<view
					v-for="item in plans"
					:key="item.id"
					class="plan-row"
					:class="{ selected: selectedPlanId === item.id }"
					@click="selectedPlanId = item.id"
				>
					<view>
						<text class="plan-title">{{ item.name }}</text>
						<text class="plan-desc">{{ item.month }} 个月 · 原价 ¥{{ item.originalAmount || item.amount }}</text>
					</view>
					<text class="plan-price">¥{{ item.amount }}</text>
				</view>
			</view>

			<view v-if="plans.length && Number(userInfo?.isVip) !== 1" class="card payment-card">
				<text class="section-title">支付方式</text>
				<view
					v-for="item in paymentMethods"
					:key="item.key"
					class="payment-row"
					:class="{ active: payMethod === item.key }"
					@click="payMethod = item.key"
				>
					<text class="payment-title">{{ item.label }}</text>
					<up-icon :name="payMethod === item.key ? 'checkmark-circle-fill' : 'checkmark-circle'" size="18" :color="payMethod === item.key ? '#59b50e' : '#9aa2b1'"></up-icon>
				</view>
			</view>

			<button
				v-if="plans.length && Number(userInfo?.isVip) !== 1"
				class="buy-btn"
				:loading="submitting"
				:disabled="submitting || !selectedPlan"
				@click="handlePurchase"
			>
				{{ selectedPlan ? `立即开通 ¥${selectedPlan.amount}` : '请选择套餐' }}
			</button>
		</template>
	</view>
</template>

<script setup>
	import { computed, ref } from 'vue';
	import { onShow } from '@dcloudio/uni-app';
	import { runAction } from '@/common/actionFeedback';
	import { ensureLogin, syncSessionFromAppUserInfo } from '@/common/auth';
	import { fetchUserInfo } from '@/common/api/user';
	import { createVipPurchase, fetchVipPlans, fetchVipRights } from '@/common/api/vip';
	import {
		getAvailablePaymentMethods,
		launchPayment,
		navigateAfterPayment
	} from '@/common/payment';

	const loading = ref(true);
	const submitting = ref(false);
	const plans = ref([]);
	const benefits = ref([]);
	const userInfo = ref(null);
	const selectedPlanId = ref(null);
	const paymentMethods = ref([]);
	const payMethod = ref('');

	const loadPaymentMethods = async () => {
		const methods = await getAvailablePaymentMethods();
		paymentMethods.value = methods;
		payMethod.value = methods[0]?.key || 'alipay-h5';
	};

	const selectedPlan = computed(() => plans.value.find((item) => item.id === selectedPlanId.value) || null);
	const vipLevel = computed(() => (Number(userInfo.value?.isVip) === 1 ? 'VIP会员' : '普通会员'));
	const vipDesc = computed(() => (
		Number(userInfo.value?.isVip) === 1
			? '当前已开通会员，下单可享受配置折扣与专属权益。'
			: '开通会员后可享受租金折扣、优先预订与快速核销等权益。'
	));
	const stats = computed(() => ([
		{ label: '会员状态', value: Number(userInfo.value?.isVip) === 1 ? '已开通' : '未开通' },
		{ label: '可选套餐', value: `${plans.value.length}个` },
		{ label: '权益项', value: `${benefits.value.length}项` }
	]));

	const loadPage = async () => {
		if (!ensureLogin('/packageUser/vip')) return;
		loading.value = true;
		try {
			const [planList, rightList, profile] = await Promise.all([
				fetchVipPlans(),
				fetchVipRights(),
				fetchUserInfo()
			]);
			plans.value = planList || [];
			benefits.value = (rightList || []).map((item) => ({
				title: item.title || item.name,
				content: item.content || item.desc,
				icon: item.icon || 'checkmark-circle-fill'
			}));
			userInfo.value = profile;
			syncSessionFromAppUserInfo(profile);
			if (!selectedPlanId.value && plans.value.length) {
				selectedPlanId.value = plans.value[0].id;
			}
			await loadPaymentMethods();
		} catch (error) {
			uni.showToast({ title: error.message || '加载失败', icon: 'none' });
		} finally {
			loading.value = false;
		}
	};

	const handlePurchase = async () => {
		if (submitting.value || !selectedPlan.value) return;
		submitting.value = true;
		await runAction({
			action: async () => {
				const orderResult = await createVipPurchase(selectedPlan.value.id);
				const payRes = await launchPayment({
					amount: orderResult.payAmount,
					orderNo: orderResult.orderNo,
					methodKey: payMethod.value,
					businessType: 'vip'
				});
				return { orderResult, payRes };
			},
			errorText: '开通失败，请稍后重试',
			onSuccess: ({ payRes }) => {
				navigateAfterPayment({ payRes, businessType: 'vip', scene: 'vip' });
			}
		});
		submitting.value = false;
	};

	onShow(loadPage);
</script>

<style scoped>
	.page { box-sizing: border-box; min-height: 100vh; padding: 24rpx 24rpx calc(40rpx + env(safe-area-inset-bottom)); background: #f5f7ff; overflow-x: hidden; }
	.loading-text, .empty-tip { padding: 40rpx 0; text-align: center; color: #6b7280; font-size: 26rpx; }
	.vip-card { box-sizing: border-box; width: 100%; padding: 32rpx; border-radius: 32rpx; background: linear-gradient(135deg, var(--theme-hero-deep-start), var(--theme-hero-end)); overflow: hidden; }
	.vip-level, .vip-desc, .vip-num, .vip-label, .section-title, .benefit-title, .benefit-desc, .plan-title, .plan-desc, .plan-price, .payment-title { display: block; }
	.vip-level { font-size: 40rpx; font-weight: 700; color: #ffffff; }
	.vip-desc { margin-top: 14rpx; font-size: 24rpx; line-height: 1.7; color: rgba(255,255,255,0.9); }
	.vip-stats { display: flex; flex-wrap: wrap; gap: 16rpx; margin-top: 28rpx; }
	.vip-stat { box-sizing: border-box; width: calc((100% - 32rpx) / 3); min-width: 0; padding: 22rpx 0; border-radius: 22rpx; text-align: center; background: rgba(255,255,255,0.12); }
	.vip-num { font-size: 32rpx; font-weight: 700; color: #ffffff; }
	.vip-label { margin-top: 8rpx; font-size: 20rpx; color: rgba(255,255,255,0.82); }
	.card { box-sizing: border-box; width: 100%; margin-top: 24rpx; padding: 28rpx; border-radius: 28rpx; background: #ffffff; overflow: hidden; }
	.section-title { font-size: 30rpx; font-weight: 700; color: #1f2937; }
	.benefit-row, .plan-row, .payment-row { display: flex; align-items: center; margin-top: 24rpx; }
	.benefit-icon { display: flex; align-items: center; justify-content: center; width: 64rpx; height: 64rpx; border-radius: 18rpx; background: #eefadb; }
	.benefit-main { flex: 1; min-width: 0; margin-left: 16rpx; }
	.benefit-title, .plan-title, .payment-title { font-size: 26rpx; font-weight: 700; color: #1f2937; }
	.benefit-desc, .plan-desc { margin-top: 8rpx; font-size: 22rpx; line-height: 1.6; color: #6b7280; }
	.plan-row, .payment-row { justify-content: space-between; align-items: flex-start; padding: 22rpx 24rpx; border-radius: 24rpx; border: 2rpx solid #eef2e8; }
	.plan-row.selected { border-color: rgba(126, 211, 33, 0.45); background: #f8fdea; }
	.plan-row > view { flex: 1; min-width: 0; }
	.plan-price { margin-left: 16rpx; flex-shrink: 0; font-size: 28rpx; font-weight: 700; color: #7ed321; }
	.payment-row { align-items: center; }
	.buy-btn { width: 100%; height: 88rpx; line-height: 88rpx; margin-top: 24rpx; border-radius: 44rpx; font-size: 30rpx; font-weight: 700; color: #fff; background: linear-gradient(135deg, #7ed321, #59b50e); border: none; }
</style>
