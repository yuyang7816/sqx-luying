<template>
	<view v-if="order" class="page">
		<view class="section-card goods-card">
			<text class="section-title">续租商品</text>
			<view v-for="item in order.goodsItems" :key="item.id" class="goods-row">
				<image class="goods-image" :src="item.image" mode="aspectFill"></image>
				<view class="goods-main">
					<text class="goods-name">{{ item.name }}</text>
					<text class="goods-meta">{{ item.specLabel }} · {{ item.packageLabel || '默认配置' }}</text>
					<text class="goods-meta">原租期：{{ order.rangeText }}</text>
					<text class="goods-meta">订单状态：{{ statusLabel }}</text>
				</view>
			</view>
		</view>

		<view class="section-card pickup-card">
			<view class="rent-mode-block">
				<text class="rent-mode-label">续租方式</text>
				<view class="rent-mode-segment">
					<view v-for="item in rentModeOptions" :key="item.key" class="mode-btn"
						:class="{ active: rentMode === item.key }" @click="handleRentModeChange(item.key)">{{ item.label }}</view>
				</view>
			</view>

			<view class="pickup-time-row readonly">
				<view class="pickup-row-icon">
					<up-icon name="clock" size="18" color="#59b50e"></up-icon>
				</view>
				<text class="pickup-row-label">续租起始</text>
				<view class="pickup-row-pickers">
					<text class="pickup-row-value">{{ renewStartDisplay }}</text>
				</view>
			</view>

			<view class="pickup-time-row" :class="{ readonly: rentMode === 'night' }">
				<view class="pickup-row-icon">
					<up-icon name="checkmark-circle" size="18" color="#59b50e"></up-icon>
				</view>
				<text class="pickup-row-label">续租结束</text>
				<view class="pickup-row-pickers">
					<picker v-if="rentMode !== 'night'" mode="date" :value="rentPeriod.endDate" @change="onEndDateChange">
						<text class="pickup-row-value picker-value">{{ returnDateDisplay }}</text>
					</picker>
					<picker v-if="rentMode !== 'night'" mode="time" :value="rentPeriod.endClock" @change="onEndClockChange">
						<text class="pickup-row-value picker-value">{{ returnClockDisplay }}</text>
					</picker>
					<picker v-if="rentMode === 'night'" mode="date" :value="rentPeriod.endDate" @change="onEndDateChange">
						<text class="pickup-row-value picker-value">{{ returnDateDisplay }}</text>
					</picker>
					<text v-if="rentMode === 'night'" class="pickup-row-value">{{ returnClockDisplay }}</text>
				</view>
			</view>

			<text class="billing-rule-tip">{{ billingRuleText }}</text>
		</view>

		<view class="section-card">
			<text class="section-title">费用试算</text>
			<view class="line"><text>时租单价</text><text>¥{{ hourUnitPrice }}</text></view>
			<view class="line"><text>续租时长</text><text>{{ extraHoursText }}</text></view>
			<view class="line"><text>续租后结束</text><text>{{ preview?.newEndTime || rentPeriod.endTime || '--' }}</text></view>
			<view class="line total"><text>续租应付</text><text>¥{{ renewAmount }}</text></view>
		</view>

		<button class="submit-btn" :loading="submitting" :disabled="submitting || previewLoading || !renewPeriodValid" @click="openPayPopup">提交续租</button>
	</view>
	<view v-else-if="pageLoading" class="loading-wrap">加载中...</view>

	<PaymentMethodPopup
		v-model:show="paymentPopupVisible"
		:amount="renewAmount"
		:loading="submitting"
		@confirm="confirmRenewPay"
	/>
</template>

<script setup>
	import { computed, ref, watch } from 'vue';
	import { onLoad } from '@dcloudio/uni-app';
	import PaymentMethodPopup from '@/components/PaymentMethodPopup.vue';
	import { runAction } from '@/common/actionFeedback';
	import { createRenew, fetchOrderDetail, previewRenew } from '@/common/api/order';
	import { ensureLogin } from '@/common/auth';
	import {
		buildDefaultRenewEndTime,
		calcRenewBillingHours,
		composeRentDateTime,
		createRentPeriodFromTimes,
		DETAIL_RENT_MODES,
		formatDatePart,
		formatDisplayPrice,
		formatRentPeriodDisplay,
		formatTimePart,
		getGoodsOvernightRange,
		getOrderStatusInfo,
		mapApiRentTypeToDetailMode,
		parseDateTime,
		syncRentPeriodTimes
	} from '@/common/rental/constants';
	import {
		launchPayment,
		navigateAfterPayment
	} from '@/common/payment';

	const order = ref(null);
	const orderId = ref('');
	const rentMode = ref('day');
	const rentPeriod = ref(createRentPeriodFromTimes('', '', 'day'));
	const preview = ref(null);
	const pageLoading = ref(false);
	const previewLoading = ref(false);
	const submitting = ref(false);
	const paymentPopupVisible = ref(false);

	const rentModeOptions = DETAIL_RENT_MODES.map((item) => ({
		key: item.key,
		label: item.label
	}));

	const apiRentType = computed(() => {
		if (rentMode.value === 'hour') return 'hour';
		if (rentMode.value === 'night') return 'overnight';
		return 'day';
	});

	const renewBilling = computed(() => {
		if (!order.value?.endTime || !rentPeriod.value.endTime) {
			return { valid: false, message: '请选择续租结束时间', hours: 0 };
		}
		return calcRenewBillingHours(order.value.endTime, rentPeriod.value.endTime);
	});

	const renewPeriodValid = computed(() => renewBilling.value.valid && renewBilling.value.hours >= 1);

	const renewStartDisplay = computed(() => formatRentPeriodDisplay(rentPeriod.value.startTime));
	const returnDateDisplay = computed(() => {
		if (!rentPeriod.value.endDate) return '选择日期';
		const date = rentPeriod.value.endDate.split('-');
		return date.length >= 3 ? `${Number(date[1])}月${Number(date[2])}日` : rentPeriod.value.endDate;
	});
	const returnClockDisplay = computed(() => rentPeriod.value.endClock || '选择时间');
	const billingRuleText = computed(() => {
		if (!order.value?.endTime) return '加载订单信息中...';
		return renewBilling.value.valid ? renewBilling.value.ruleText : renewBilling.value.message || '请选择有效续租时间';
	});

	const applyRentPeriod = (mode, endTime) => {
		if (!order.value?.endTime) return;
		if (mode === 'night') {
			const start = parseDateTime(order.value.endTime);
			const end = parseDateTime(endTime);
			rentPeriod.value = {
				rentMode: mode,
				startDate: formatDatePart(start),
				startClock: formatTimePart(start),
				endDate: formatDatePart(end),
				endClock: formatTimePart(end),
				startTime: order.value.endTime,
				endTime
			};
			return;
		}
		let period = createRentPeriodFromTimes(order.value.endTime, endTime, mode);
		if (mode === 'day') {
			period = syncRentPeriodTimes({ ...period, endClock: period.startClock }, mode);
		}
		rentPeriod.value = period;
	};

	const initRentPeriod = (orderView, modeKey) => {
		const mode = modeKey || mapApiRentTypeToDetailMode(orderView.rentType);
		rentMode.value = mode;
		const defaultEnd = buildDefaultRenewEndTime(orderView.endTime, mode);
		applyRentPeriod(mode, defaultEnd);
	};

	const loadPreview = async () => {
		if (!orderId.value || !renewPeriodValid.value) {
			preview.value = null;
			return;
		}
		previewLoading.value = true;
		try {
			preview.value = await previewRenew(orderId.value, rentPeriod.value.endTime);
		} catch (error) {
			preview.value = null;
		} finally {
			previewLoading.value = false;
		}
	};

	onLoad(async (options) => {
		if (!ensureLogin(`/packageOrder/renew?id=${options.id || ''}`)) return;
		orderId.value = options.id || '';
		pageLoading.value = true;
		try {
			order.value = await fetchOrderDetail(orderId.value);
			if (order.value) {
				initRentPeriod(order.value);
				await loadPreview();
			}
		} catch (error) {
			order.value = null;
		} finally {
			pageLoading.value = false;
		}
	});

	watch(() => [rentPeriod.value.endTime, rentPeriod.value.endDate, rentPeriod.value.endClock], () => {
		loadPreview();
	});

	const renewAmount = computed(() => formatDisplayPrice(preview.value?.renewAmount || 0));
	const hourUnitPrice = computed(() => formatDisplayPrice(preview.value?.hourUnitPrice || 0));
	const extraHoursText = computed(() => {
		if (!renewPeriodValid.value) return '--';
		const hours = preview.value?.extraHours ?? renewBilling.value.hours;
		return `${hours} 小时`;
	});
	const statusLabel = computed(() => getOrderStatusInfo(order.value?.status).label);

	const handleRentModeChange = (key) => {
		if (rentMode.value === key || !order.value) return;
		initRentPeriod(order.value, key);
	};

	const commitEndPeriodChange = () => {
		loadPreview();
	};

	const onEndDateChange = (event) => {
		if (rentMode.value === 'night') {
			const endDate = event.detail.value;
			const overnight = getGoodsOvernightRange({});
			const endTime = composeRentDateTime(endDate, overnight.end);
			rentPeriod.value = {
				...rentPeriod.value,
				endDate,
				endClock: overnight.end,
				endTime
			};
			commitEndPeriodChange();
			return;
		}
		let next = { ...rentPeriod.value, endDate: event.detail.value };
		if (rentMode.value === 'day') {
			next.endClock = rentPeriod.value.startClock;
		}
		rentPeriod.value = syncRentPeriodTimes(next, rentMode.value);
		commitEndPeriodChange();
	};

	const onEndClockChange = (event) => {
		rentPeriod.value = syncRentPeriodTimes({ ...rentPeriod.value, endClock: event.detail.value }, rentMode.value);
		commitEndPeriodChange();
	};

	const openPayPopup = () => {
		if (submitting.value || !renewPeriodValid.value) {
			uni.showToast({ title: renewBilling.value.message || '请先选择续租时间', icon: 'none' });
			return;
		}
		paymentPopupVisible.value = true;
	};

	const confirmRenewPay = async (methodKey) => {
		if (submitting.value || !renewPeriodValid.value) return;
		submitting.value = true;
		await runAction({
			action: async () => {
				const renewResult = await createRenew(orderId.value, rentPeriod.value.endTime, apiRentType.value);
				const payRes = await launchPayment({
					amount: renewResult.renewAmount,
					orderNo: renewResult.renewNo,
					methodKey,
					businessType: 'rent_renew'
				});
				return { renewResult, payRes, methodKey };
			},
			errorText: '续租提交失败，请稍后重试',
			onSuccess: ({ payRes }) => {
				paymentPopupVisible.value = false;
				navigateAfterPayment({ payRes, businessType: 'rent_renew', scene: 'renew' });
			}
		});
		submitting.value = false;
	};
</script>

<style scoped>
	.page { box-sizing: border-box; min-height: 100vh; padding: 24rpx 24rpx 40rpx; background: #f5f7ff; overflow-x: hidden; }
	.loading-wrap { padding: 120rpx 0; text-align: center; font-size: 28rpx; color: #6b7280; }
	.section-card { box-sizing: border-box; width: 100%; margin-bottom: 24rpx; padding: 28rpx; background: #fff; border-radius: 28rpx; overflow: hidden; }
	.section-title { display: block; font-size: 30rpx; font-weight: 700; color: #1f2937; }
	.goods-row { display: flex; gap: 20rpx; margin-top: 20rpx; }
	.goods-image { flex-shrink: 0; width: 140rpx; height: 140rpx; border-radius: 16rpx; background: #f3f4f6; }
	.goods-main { flex: 1; min-width: 0; }
	.goods-name, .goods-meta { display: block; }
	.goods-name { font-size: 28rpx; font-weight: 700; color: #1f2937; line-height: 1.4; }
	.goods-meta { margin-top: 8rpx; font-size: 22rpx; color: #6b7280; line-height: 1.5; }
	.rent-mode-block { margin-bottom: 8rpx; }
	.rent-mode-label { display: block; font-size: 24rpx; color: #53634a; margin-bottom: 16rpx; }
	.rent-mode-segment { display: flex; gap: 12rpx; }
	.mode-btn { flex: 1; height: 64rpx; line-height: 64rpx; text-align: center; border-radius: 16rpx; font-size: 24rpx; color: #53634a; background: #f3f6ef; }
	.mode-btn.active { color: #fff; font-weight: 600; background: linear-gradient(135deg, #7ed321, #59b50e); }
	.pickup-time-row { display: flex; align-items: center; gap: 16rpx; margin-top: 20rpx; padding: 8rpx 0 4rpx; }
	.pickup-time-row:last-of-type { margin-bottom: 4rpx; }
	.pickup-row-icon { flex-shrink: 0; width: 44rpx; height: 44rpx; display: flex; align-items: center; justify-content: center; border-radius: 12rpx; background: rgba(89, 181, 14, 0.1); }
	.pickup-row-label { flex-shrink: 0; font-size: 24rpx; color: #53634a; }
	.pickup-row-pickers { flex: 1; min-width: 0; display: flex; align-items: center; justify-content: flex-end; gap: 12rpx; }
	.pickup-row-value { flex: 1; min-width: 0; font-size: 24rpx; font-weight: 500; color: #1a2114; text-align: right; }
	.pickup-row-value.picker-value { flex: none; padding: 0; border: none; background: transparent; color: #4a8f0f; font-weight: 600; }
	.pickup-time-row.readonly .pickup-row-value { padding: 0; border: none; background: transparent; color: #1a2114; font-weight: 500; }
	.billing-rule-tip { display: block; margin-top: 16rpx; padding: 14rpx 18rpx; border-radius: 14rpx; font-size: 20rpx; line-height: 1.6; color: #7a8b6e; background: rgba(247, 251, 240, 0.9); }
	.line { display: flex; justify-content: space-between; align-items: flex-start; margin-top: 20rpx; font-size: 24rpx; color: #4b5563; }
	.line text:first-child { width: 132rpx; flex-shrink: 0; color: #8f97a5; }
	.line text:last-child { flex: 1; min-width: 0; text-align: right; word-break: break-all; color: #1f2937; }
	.line.total { font-size: 28rpx; font-weight: 700; color: #1f2937; }
	.submit-btn { box-sizing: border-box; width: 100%; height: 88rpx; line-height: 88rpx; border-radius: 44rpx; font-size: 28rpx; color: #fff; background: linear-gradient(135deg, #7ed321, #59b50e); }
	.submit-btn[disabled] { opacity: 0.55; }
</style>
