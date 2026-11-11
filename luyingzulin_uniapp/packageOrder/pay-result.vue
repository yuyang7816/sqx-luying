<template>
	<view class="page">
		<view class="hero-card" :class="result">
			<view class="status-icon"><up-icon :name="statusIcon" size="32" color="#ffffff"></up-icon></view>
			<text class="result-title">{{ title }}</text>
			<text class="result-desc">{{ desc }}</text>
			<text class="result-amount">实付 ¥{{ amount }}</text>
		</view>

		<view class="card">
			<view class="line"><text class="line-label">{{ scene === 'deposit' || scene === 'merchant-deposit' || scene === 'wallet' ? '支付单号' : '订单编号' }}</text><text class="line-value">{{ orderId }}</text></view>
			<view class="line"><text class="line-label">支付状态</text><text class="line-value" :class="result">{{ title }}</text></view>
			<view class="line"><text class="line-label">订单状态</text><text class="line-value">{{ detailStatus }}</text></view>
			<view class="line"><text class="line-label">支付方式</text><text class="line-value">{{ methodLabel }}</text></view>
			<view class="line"><text class="line-label">支付场景</text><text class="line-value">{{ platformLabel }}</text></view>
			<view class="line"><text class="line-label">处理说明</text><text class="line-value">{{ statusNote }}</text></view>
		</view>

		<view class="card tip-card">
			<text class="tip-title">后续安排</text>
			<text class="tip-text">{{ followText }}</text>
			<text class="tip-text">{{ followTipText }}</text>
		</view>

		<view class="action-card">
			<button class="primary-btn" @click="goOrderDetail">{{ primaryActionText }}</button>
			<button v-if="result === 'success'" class="ghost-btn" @click="goPickup">{{ secondaryActionText }}</button>
			<button v-else-if="result === 'pending'" class="ghost-btn" :loading="polling" @click="pollPayStatus">刷新支付状态</button>
			<button v-else class="ghost-btn" @click="goHome">返回首页</button>
		</view>
	</view>
</template>

<script setup>
	import { computed, onUnmounted, ref } from 'vue';
	import { onLoad } from '@dcloudio/uni-app';
	import { fetchDepositPayStatus } from '@/common/api/deposit';
	import { cancelMerchantSecurityDepositPay, fetchMerchantSecurityDepositPayStatus } from '@/common/api/merchantWallet';
	import { fetchOrderDetail, fetchPayStatus } from '@/common/api/order';
	import { fetchVipPayStatus } from '@/common/api/vip';
	import { fetchWalletRechargePayStatus } from '@/common/api/wallet';
	import { fetchUserInfo } from '@/common/api/user';
	import { syncSessionFromAppUserInfo } from '@/common/auth';
	import { getOrderStatusInfo } from '@/common/rental/constants';

	const result = ref('success');
	const amount = ref('0.00');
	const orderId = ref('');
	const methodLabel = ref('微信支付');
	const platformLabel = ref('当前平台');
	const scene = ref('pay');
	const orderDetailId = ref('');
	const payStatus = ref(null);
	const polling = ref(false);
	let pollTimer = null;

	const statusLabelMap = {
		pending_payment: '待支付',
		waiting_pickup: '待取货',
		delivering: '配送中',
		renting: '租赁中',
		returning: '待归还',
		inspecting: '验收中',
		completed: '已完成',
		cancelled: '已取消',
		cancelled: '已取消',
		paid: '已支付',
		pending: '待支付'
	};

	const shouldPollPayStatus = () => ['vip', 'deposit', 'merchant-deposit', 'wallet', 'pay'].includes(scene.value);

	const cancelMerchantDepositPayIfFailed = async () => {
		if (scene.value !== 'merchant-deposit' || !orderId.value || result.value !== 'fail') return;
		await cancelMerchantSecurityDepositPay(orderId.value, { silentError: true }).catch(() => {});
	};

	const loadPayStatus = async () => {
		if (!orderId.value) return;
		polling.value = true;
		try {
			if (scene.value === 'vip') {
				const statusVo = await fetchVipPayStatus(orderId.value);
				payStatus.value = statusVo;
				if (statusVo?.paid) {
					result.value = 'success';
					const profile = await fetchUserInfo();
					syncSessionFromAppUserInfo(profile);
				} else if (result.value === 'success' && !statusVo?.paid) {
					result.value = 'pending';
				}
				return;
			}
			if (scene.value === 'deposit') {
				const statusVo = await fetchDepositPayStatus(orderId.value);
				payStatus.value = statusVo;
				if (statusVo?.paid) {
					result.value = 'success';
				} else if (result.value === 'success' && !statusVo?.paid) {
					result.value = 'pending';
				}
				return;
			}
			if (scene.value === 'merchant-deposit') {
				const statusVo = await fetchMerchantSecurityDepositPayStatus(orderId.value);
				payStatus.value = statusVo;
				if (statusVo?.paid) {
					result.value = 'success';
				} else if (result.value === 'success' && !statusVo?.paid) {
					result.value = 'pending';
				}
				return;
			}
			if (scene.value === 'wallet') {
				const statusVo = await fetchWalletRechargePayStatus(orderId.value);
				payStatus.value = statusVo;
				if (statusVo?.paid) {
					result.value = 'success';
				} else if (result.value === 'success' && !statusVo?.paid) {
					result.value = 'pending';
				}
				return;
			}
			if (scene.value === 'renew') return;
			const statusVo = await fetchPayStatus(orderId.value);
			payStatus.value = statusVo;
			if (statusVo?.paid) {
				result.value = 'success';
			} else if (result.value === 'success' && !statusVo?.paid) {
				result.value = 'pending';
			}
		} catch (error) {
			// ignore
		} finally {
			polling.value = false;
		}
	};

	const pollPayStatus = async () => {
		await loadPayStatus();
	};

	const loadOrderDetail = async () => {
		if (!orderDetailId.value) return;
		try {
			const detail = await fetchOrderDetail(orderDetailId.value);
			payStatus.value = { status: detail.status, paid: detail.status !== 'pending_payment' };
		} catch (error) {
			// ignore
		}
	};

	onLoad(async (options) => {
		result.value = options.result || 'success';
		amount.value = options.amount || '0.00';
		orderId.value = options.id || '';
		orderDetailId.value = options.orderId || '';
		methodLabel.value = options.method ? decodeURIComponent(options.method) : '微信支付';
		platformLabel.value = options.platform ? decodeURIComponent(options.platform) : '当前平台';
		scene.value = options.scene || 'pay';
		await cancelMerchantDepositPayIfFailed();
		if (scene.value === 'renew') {
			loadOrderDetail();
		} else if (shouldPollPayStatus()) {
			loadPayStatus();
			if (result.value === 'pending') {
				pollTimer = setInterval(loadPayStatus, 4000);
			}
		}
	});

	onUnmounted(() => {
		if (pollTimer) clearInterval(pollTimer);
	});

	const title = computed(() => {
		if (result.value === 'pending') return '支付确认中';
		if (result.value === 'fail') return '支付失败';
		if (scene.value === 'vip') return '会员开通成功';
		if (scene.value === 'deposit') return '租赁押金缴纳成功';
		if (scene.value === 'merchant-deposit') return '保证金缴纳成功';
		if (scene.value === 'wallet') return '充值成功';
		return scene.value === 'renew' ? '续租提交成功' : '支付成功';
	});
	const desc = computed(() => {
		if (result.value === 'pending') return '支付结果同步中，请稍后查看订单状态。';
		if (result.value === 'fail') return '本次支付未完成，请重新发起支付。';
		if (scene.value === 'vip') return '会员权益已生效，可在会员中心查看详情。';
		if (scene.value === 'deposit') return '租赁押金已入账，可在租赁押金账户查看余额与流水。';
		if (scene.value === 'merchant-deposit') return '保证金已入账，可在商户钱包查看余额与流水。';
		if (scene.value === 'wallet') return '充值金额已入账，可在我的钱包查看余额与明细。';
		return scene.value === 'renew' ? '续租费用已支付，订单租期已更新。' : '订单已生成，门店正在为您备货。';
	});
	const statusIcon = computed(() => result.value === 'success' ? 'checkmark' : result.value === 'pending' ? 'clock' : 'close');
	const statusNote = computed(() => {
		if (result.value === 'pending') return '支付通道回调中，系统会自动更新订单状态。';
		if (result.value === 'fail') return '未扣款成功，可返回重新发起支付。';
		if (scene.value === 'vip') return '会员身份已同步到账户，下单时可享受会员权益。';
		if (scene.value === 'deposit') return '押金余额已更新，可用于租赁订单免押或冻结抵扣。';
		if (scene.value === 'merchant-deposit') return '商户保证金余额已更新，可在钱包管理中查看。';
		if (scene.value === 'wallet') return '账户余额已更新，可用于租赁订单余额支付。';
		return scene.value === 'renew' ? '续租订单已追加到当前租赁单，请留意新的归还安排。' : '已锁定库存，请按预约时间取货。';
	});
	const followText = computed(() => {
		if (result.value === 'pending') return '如长时间未更新，可进入订单列表刷新状态或联系客服处理。';
		if (result.value === 'fail') return '库存仍可能被释放，建议尽快重新提交订单。';
		if (scene.value === 'vip') return '返回会员中心可查看权益说明与套餐记录。';
		if (scene.value === 'deposit') return '返回押金账户可查看可用余额、冻结金额与流水记录。';
		if (scene.value === 'merchant-deposit') return '返回商户钱包可查看保证金余额与流水记录。';
		if (scene.value === 'wallet') return '返回我的钱包可查看余额、明细与提现记录。';
		return scene.value === 'renew' ? '系统已记录续租成功，新的租期与订单时间线可在详情页查看。' : '系统已生成订单与提货码，门店会根据预约时间完成备货。';
	});
	const followTipText = computed(() => {
		if (scene.value === 'renew') return '续租完成后可在订单详情中继续查看归还安排和时间线。';
		if (scene.value === 'deposit') return '押金缴纳成功后可在押金账户查看余额变化与流水明细。';
		if (scene.value === 'merchant-deposit') return '保证金缴纳成功后可在商户钱包查看余额变化与流水明细。';
		if (scene.value === 'wallet') return '充值成功后可在我的钱包查看余额变化与资金明细。';
		if (scene.value === 'vip') return '会员开通后可在会员中心查看权益说明。';
		return '支付成功后可在订单详情中查看取货凭证、续租或归还入口。';
	});
	const primaryActionText = computed(() => {
		if (scene.value === 'vip') return '返回会员中心';
		if (scene.value === 'deposit') return '查看租赁押金';
		if (scene.value === 'merchant-deposit') return '查看商户钱包';
		if (scene.value === 'wallet') return '查看我的钱包';
		return '查看订单';
	});
	const secondaryActionText = computed(() => {
		if (scene.value === 'vip') return '查看权益';
		if (scene.value === 'deposit') return '查看流水';
		if (scene.value === 'merchant-deposit') return '查看流水';
		if (scene.value === 'wallet') return '查看明细';
		if (scene.value === 'renew') return '查看租期';
		return '取货凭证';
	});
	const detailStatus = computed(() => {
		if (scene.value === 'vip' && payStatus.value?.paid) {
			return '已开通';
		}
		if (scene.value === 'deposit' && payStatus.value?.paid) {
			return '已入账';
		}
		if (scene.value === 'merchant-deposit' && payStatus.value?.paid) {
			return '已入账';
		}
		if (scene.value === 'wallet' && payStatus.value?.paid) {
			return '已入账';
		}
		if (payStatus.value?.status) {
			return statusLabelMap[payStatus.value.status] || getOrderStatusInfo(payStatus.value.status).label;
		}
		return '--';
	});

	const resolveDetailId = () => orderDetailId.value || orderId.value;

	const goOrderDetail = () => {
		if (scene.value === 'vip') {
			uni.navigateTo({ url: '/packageUser/vip' });
			return;
		}
		if (scene.value === 'deposit') {
			uni.navigateTo({ url: '/packageContent/deposit' });
			return;
		}
		if (scene.value === 'merchant-deposit') {
			uni.navigateTo({ url: '/packageMerchant/wallet-deposit' });
			return;
		}
		if (scene.value === 'wallet') {
			uni.navigateTo({ url: '/packageUser/wallet' });
			return;
		}
		uni.navigateTo({ url: `/packageOrder/detail?id=${resolveDetailId()}` });
	};

	const goPickup = () => {
		if (scene.value === 'vip') {
			uni.navigateTo({ url: '/packageUser/vip' });
			return;
		}
		if (scene.value === 'deposit') {
			goOrderDetail();
			return;
		}
		if (scene.value === 'merchant-deposit') {
			uni.navigateTo({ url: '/packageMerchant/wallet-deposit' });
			return;
		}
		if (scene.value === 'wallet') {
			uni.navigateTo({ url: '/packageUser/wallet-record' });
			return;
		}
		if (scene.value === 'renew') {
			goOrderDetail();
			return;
		}
		goOrderDetail();
	};

	const goHome = () => {
		uni.switchTab({ url: '/pages/index/index' });
	};
</script>

<style scoped>
	.page { box-sizing: border-box; min-height: 100vh; padding: 24rpx; background: #f5f7ff; overflow-x: hidden; }
	.hero-card { box-sizing: border-box; width: 100%; padding: 38rpx 28rpx; border-radius: 32rpx; text-align: center; background: linear-gradient(135deg, #8ee226, #9fe943); overflow: hidden; }
	.hero-card.pending { background: linear-gradient(135deg, var(--theme-hero-soft-start), var(--theme-hero-soft-end)); }
	.hero-card.fail { background: linear-gradient(135deg, #ff9a7a, #ffb38c); }
	.card, .action-card { box-sizing: border-box; width: 100%; margin-top: 24rpx; padding: 28rpx; background: #fff; border-radius: 28rpx; overflow: hidden; }
	.action-card { padding-bottom: 8rpx; }
	.status-icon, .result-title, .result-desc, .result-amount, .tip-title, .tip-text { display: block; }
	.status-icon { display: flex; align-items: center; justify-content: center; width: 120rpx; height: 120rpx; margin: 0 auto 24rpx; border-radius: 50%; background: rgba(255,255,255,0.22); }
	.result-title { font-size: 40rpx; font-weight: 700; color: #ffffff; }
	.result-desc { margin-top: 14rpx; font-size: 24rpx; line-height: 1.7; color: rgba(255,255,255,0.94); }
	.result-amount { margin-top: 24rpx; font-size: 46rpx; font-weight: 700; color: #ffffff; }
	.line { display: flex; justify-content: space-between; align-items: flex-start; margin-top: 20rpx; font-size: 24rpx; line-height: 1.6; color: #4b5563; }
	.line:first-child { margin-top: 0; }
	.line-label, .line-value { display: block; }
	.line-label { width: 132rpx; flex-shrink: 0; color: #8f97a5; }
	.line-value { flex: 1; min-width: 0; text-align: right; word-break: break-all; }
	.success { color: #59b50e; }
	.pending { color: #c88a00; }
	.fail { color: #ff6b57; }
	.tip-title { font-size: 30rpx; font-weight: 700; color: #1f2937; }
	.tip-text { margin-top: 12rpx; font-size: 22rpx; line-height: 1.7; color: #6b7280; }
	.primary-btn, .ghost-btn { box-sizing: border-box; width: 100%; height: 88rpx; line-height: 88rpx; margin-bottom: 20rpx; border-radius: 44rpx; font-size: 28rpx; }
	.primary-btn { color: #fff; background: linear-gradient(135deg, #7ed321, #59b50e); }
	.ghost-btn { color: #59b50e; background: #f2fde5; }
</style>
