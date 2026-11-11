<template>
	<view v-if="order" class="page">
		<view class="section-card goods-card">
			<view class="section-head">
				<text class="section-title">租赁商品</text>
				<text v-if="!showPickupVoucher" class="rent-mode-tag">{{ order.rentModeLabel || '租赁' }}</text>
			</view>
			<view v-for="item in order.goodsItems" :key="item.id" class="goods-block">
				<view class="goods-row">
					<image class="goods-image" :src="item.image" mode="aspectFill"></image>
					<view class="goods-main">
						<text class="goods-name">{{ item.name }}</text>
						<text class="goods-meta">{{ item.specLabel }}<text v-if="item.packageLabel"> · {{ item.packageLabel }}</text></text>
						<text v-if="item.packageDesc" class="goods-package-desc">{{ item.packageDesc }}</text>
						<text v-if="!showPickupVoucher && itemBillingFormula(item)" class="goods-billing">{{ itemBillingFormula(item) }}</text>
					</view>
				</view>
			</view>
		</view>

		<PickupVoucherSection
			v-if="showPickupVoucher"
			:order-id="orderId"
			:initial-view="order.pickupView"
			:create-time="order.createTime"
			@verified="onPickupVerified"
		/>

		<view v-if="hasStoreInfo" class="section-card goods-card">
			<text class="section-title">商家信息</text>
			<view class="goods-row merchant-row">
				<image
					class="goods-image"
					:src="order.store.facadeImage || '/static/logo.png'"
					mode="aspectFill"
				></image>
				<view class="goods-main">
					<view class="merchant-head">
						<text class="goods-name">{{ order.store.name }}</text>
						<view v-if="order.store.contact || order.store.lat" class="store-actions">
							<view v-if="order.store.lat" class="store-action" @click="openLocation">
								<up-icon name="map" size="16" color="#4d9e16"></up-icon>
							</view>
							<view v-if="order.store.contact" class="store-action" @click="callStore">
								<up-icon name="phone" size="16" color="#4d9e16"></up-icon>
							</view>
						</view>
					</view>
					<text v-if="order.store.branchName" class="goods-meta">{{ order.store.branchName }}</text>
					<text class="goods-meta">{{ order.store.address || '--' }}</text>
					<text v-if="order.store.businessHours" class="goods-meta">营业时间 {{ order.store.businessHours }}</text>
					<text
						v-if="order.store.contact"
						class="goods-meta phone-link"
						@click="callStore"
					>门店电话 {{ maskedStorePhone }}</text>
				</view>
			</view>
		</view>

		<view v-if="!showPickupVoucher" class="section-card">
			<text class="section-title">履约信息</text>
			<view class="schedule-grid">
				<view class="schedule-item">
					<text class="schedule-label">{{ pickupLabel }}</text>
					<text class="schedule-time">{{ pickupTime }}</text>
				</view>
				<view class="schedule-divider"></view>
				<view class="schedule-item">
					<text class="schedule-label">应还时间</text>
					<text class="schedule-time emphasis">{{ returnTime }}</text>
				</view>
			</view>
			<view class="fulfillment-type-row">
				<text class="fulfillment-type-label">履约方式</text>
				<text class="fulfillment-type-value">{{ pickupTypeLabel }}</text>
			</view>
		</view>

		<view class="section-card fee-card">
			<text class="section-title">费用明细</text>
			<view class="price-line"><text>租金小计</text><text>¥{{ formatPrice(order.rentAmount) }}</text></view>
			<view class="price-line"><text>押金</text><text>¥{{ formatPrice(order.depositAmount) }}</text></view>
			<view v-if="Number(order.deliveryAmount) > 0" class="price-line"><text>配送费</text><text>¥{{ formatPrice(order.deliveryAmount) }}</text></view>
			<view v-if="Number(order.couponAmount) > 0" class="price-line discount"><text>优惠抵扣</text><text>-¥{{ formatPrice(order.couponAmount) }}</text></view>
			<view class="price-line total"><text>实付金额</text><text>¥{{ formatPrice(order.payAmount) }}</text></view>
		</view>

		<view class="section-card fee-card">
			<text class="section-title">订单信息</text>
			<view class="price-line">
				<text>订单编号</text>
				<view class="order-no-wrap">
					<text>{{ order.orderNo || order.id }}</text>
					<text class="copy-btn" @click="copyOrderNo">复制</text>
				</view>
			</view>
			<view class="price-line"><text>下单时间</text><text>{{ order.createTime || '--' }}</text></view>
			<view v-if="order.contactName || order.contactPhone" class="price-line">
				<text>联系人</text>
				<text>{{ order.contactName || '--' }} {{ order.contactPhone || '' }}</text>
			</view>
		</view>

		<view v-if="hasLifecycleInfo" class="section-card">
			<text class="section-title">进度信息</text>

			<view v-if="hasRenewInfo" class="progress-block">
				<text class="progress-block-title">续租</text>
				<text class="progress-summary">已续租，补差 ¥{{ formatPrice(order.renewAmount) }}，当前应还 {{ returnTime }}</text>
			</view>

			<view v-if="hasReturnInfo" class="progress-block">
				<text class="progress-block-title">归还申请</text>
				<view v-if="order.returnDate" class="progress-row">
					<text class="progress-row-label">归还方式</text>
					<text class="progress-row-value">{{ order.returnDate }} · {{ order.returnMethodLabel || '到店归还' }}</text>
				</view>
				<view v-if="order.returnRemark" class="progress-note">
					<text class="progress-note-label">备注说明</text>
					<text class="progress-note-text">{{ order.returnRemark }}</text>
				</view>
				<view v-if="order.returnImages?.length" class="progress-images">
					<text class="progress-images-label">归还凭证</text>
					<view class="image-row">
						<image
							v-for="(url, index) in order.returnImages"
							:key="`return-${url}-${index}`"
							class="progress-image"
							:src="url"
							mode="aspectFill"
							@click="previewReturnImage(index)"
						></image>
					</view>
				</view>
			</view>

			<view v-if="hasInspectionInfo" class="progress-block">
				<text class="progress-block-title">商家验收</text>
				<view v-if="order.inspectionRemark" class="progress-note">
					<text class="progress-note-label">验收说明</text>
					<text class="progress-note-text">{{ order.inspectionRemark }}</text>
				</view>
				<view v-if="order.inspectionImages?.length" class="progress-images">
					<text class="progress-images-label">验收凭证</text>
					<view class="image-row">
						<image
							v-for="(url, index) in order.inspectionImages"
							:key="`inspection-${url}-${index}`"
							class="progress-image"
							:src="url"
							mode="aspectFill"
							@click="previewInspectionImage(index)"
						></image>
					</view>
				</view>
				<view v-if="hasDepositSettlement" class="deposit-settle">
					<text class="deposit-settle-title">押金结算</text>
					<view class="deposit-settle-row">
						<text>扣款金额</text>
						<text class="deposit-deduct">¥{{ formatPrice(order.inspectionDeductAmount) }}</text>
					</view>
					<view class="deposit-settle-row highlight">
						<text>押金退款</text>
						<text class="deposit-refund">¥{{ formatPrice(order.inspectionRefundAmount) }}</text>
					</view>
				</view>
			</view>
		</view>

		<view class="bottom-space"></view>
	</view>

	<view v-else-if="pageLoading" class="state-wrap">
		<text class="state-text">加载中...</text>
	</view>
	<view v-else class="state-wrap">
		<text class="state-text">订单加载失败，请返回重试</text>
		<view class="state-btn" @click="syncOrder">重新加载</view>
	</view>

	<view v-if="order" class="bottom-bar">
		<view v-if="canCancelOrder" class="plain-btn" @click="cancelOrder">取消订单</view>
		<view v-if="order.status === 'pending_payment'" class="primary-btn" @click="openPayPopup">去付款</view>
		<view v-if="order.status === 'renting'" class="ghost-btn" @click="goRenew">申请续租</view>
		<view v-if="order.status === 'renting'" class="ghost-btn" @click="goReturn">申请归还</view>
		<view class="service-btn" @click="goService">联系客服</view>
	</view>

	<PaymentMethodPopup
		v-model:show="paymentPopupVisible"
		:amount="order?.payAmount || 0"
		:loading="actionLoading"
		@confirm="confirmPay"
	/>
</template>

<script setup>
	import { computed, ref } from 'vue';
	import { onLoad, onShow } from '@dcloudio/uni-app';
	import PickupVoucherSection from '@/components/PickupVoucherSection.vue';
	import PaymentMethodPopup from '@/components/PaymentMethodPopup.vue';
	import { fetchOrderDetail } from '@/common/api/order';
	import { ensureLogin } from '@/common/auth';
	import { confirmCancelOrder, executeOrderPayment } from '@/common/orderActions';
	import {
		buildRentPriceBreakdown,
		CONFIRM_PICKUP_TYPES,
		formatRentPeriodDisplay,
		isPickupStatus
	} from '@/common/rental/constants';
	import { maskPhone } from '@/common/merchant/constants';

	const order = ref(null);
	const orderId = ref('');
	const pageLoading = ref(false);
	const actionLoading = ref(false);
	const paymentPopupVisible = ref(false);
	let syncSeq = 0;
	let skipNextShowRefresh = false;

	const syncOrder = async ({ keepOnError = false, silent = false } = {}) => {
		if (!orderId.value) return;
		const seq = ++syncSeq;
		if (!silent) {
			pageLoading.value = true;
		}
		try {
			const data = await fetchOrderDetail(orderId.value);
			if (seq !== syncSeq) return;
			order.value = data;
		} catch (error) {
			if (seq !== syncSeq) return;
			if (!keepOnError || !order.value) {
				order.value = null;
			}
		} finally {
			if (seq === syncSeq && !silent) {
				pageLoading.value = false;
			}
		}
	};

	onLoad((options) => {
		if (!ensureLogin(`/packageOrder/detail?id=${options.id || ''}`)) return;
		orderId.value = options.id || '';
		skipNextShowRefresh = true;
		syncOrder();
	});

	onShow(() => {
		if (!orderId.value) return;
		if (skipNextShowRefresh) {
			skipNextShowRefresh = false;
			return;
		}
		syncOrder({ keepOnError: true });
	});

	const showPickupVoucher = computed(() => isPickupStatus(order.value?.status) && order.value?.pickupView);
	const hasStoreInfo = computed(() => {
		const store = order.value?.store;
		if (!store) return false;
		return Boolean(
			(store.name && store.name !== '取货门店')
			|| store.address
			|| store.contact
			|| store.facadeImage
		);
	});
	const maskedStorePhone = computed(() => maskPhone(order.value?.store?.contact || ''));
	const canCancelOrder = computed(() =>
		order.value?.status === 'pending_payment' || isPickupStatus(order.value?.status)
	);
	const hasRenewInfo = computed(() => Boolean(order.value?.renewAmount));
	const hasReturnInfo = computed(() =>
		Boolean(
			order.value?.returnDate
			|| order.value?.returnRemark
			|| order.value?.returnImages?.length
		)
	);
	const hasInspectionInfo = computed(() =>
		Boolean(
			order.value?.inspectionRemark
			|| order.value?.inspectionImages?.length
			|| order.value?.inspectionDeductAmount != null
			|| order.value?.inspectionRefundAmount != null
		)
	);
	const hasDepositSettlement = computed(() =>
		order.value?.inspectionDeductAmount != null || order.value?.inspectionRefundAmount != null
	);
	const hasLifecycleInfo = computed(() =>
		hasRenewInfo.value || hasReturnInfo.value || hasInspectionInfo.value
	);

	const pickupTypeLabel = computed(() =>
		CONFIRM_PICKUP_TYPES.find((item) => item.key === order.value?.pickupType)?.label || '门店自提'
	);
	const pickupTime = computed(() => formatRentPeriodDisplay(order.value?.startTime));
	const returnTime = computed(() => formatRentPeriodDisplay(order.value?.endTime));
	const pickupLabel = computed(() => order.value?.pickupType === 'delivery' ? '送达时间' : '取货时间');

	const itemBillingFormula = (item) => {
		const chips = buildRentPriceBreakdown(item, order.value || {}).chips || [];
		if (!chips.length) return '';
		return chips.map((chip) => chip.value).join(' × ');
	};

	const formatPrice = (value) => Number(value || 0).toFixed(2);

	const copyOrderNo = () => {
		const orderNo = order.value?.orderNo || order.value?.id;
		if (!orderNo) return;
		uni.setClipboardData({ data: String(orderNo) });
	};

	const openPayPopup = () => {
		if (actionLoading.value || !order.value) return;
		paymentPopupVisible.value = true;
	};

	const confirmPay = async (methodKey) => {
		if (actionLoading.value || !order.value) return;
		actionLoading.value = true;
		const { ok } = await executeOrderPayment({
			orderId: order.value.id,
			orderNo: order.value.orderNo,
			payAmount: order.value.payAmount,
			methodKey
		});
		actionLoading.value = false;
		if (ok) {
			paymentPopupVisible.value = false;
		}
	};

	const cancelOrder = async () => {
		if (actionLoading.value || !order.value) return;
		actionLoading.value = true;
		const paid = isPickupStatus(order.value.status);
		const { ok } = await confirmCancelOrder(order.value.id, { paid });
		actionLoading.value = false;
		if (ok) {
			syncOrder();
		}
	};

	const onPickupVerified = () => {
		syncOrder({ keepOnError: true, silent: true });
	};
	const goRenew = () => uni.navigateTo({ url: `/packageOrder/renew?id=${order.value.id}` });
	const goReturn = () => uni.navigateTo({ url: `/packageOrder/return-apply?id=${order.value.id}` });
	const previewReturnImage = (index) => {
		if (!order.value?.returnImages?.length) return;
		uni.previewImage({
			current: order.value.returnImages[index],
			urls: order.value.returnImages
		});
	};
	const previewInspectionImage = (index) => {
		if (!order.value?.inspectionImages?.length) return;
		uni.previewImage({
			current: order.value.inspectionImages[index],
			urls: order.value.inspectionImages
		});
	};
	const goService = () => uni.navigateTo({ url: '/packageUser/service' });
	const callStore = () => {
		if (!order.value?.store?.contact) {
			uni.showToast({ title: '暂无门店电话', icon: 'none' });
			return;
		}
		uni.makePhoneCall({ phoneNumber: order.value.store.contact });
	};
	const openLocation = () => {
		const { lat, lng, name, address } = order.value?.store || {};
		if (!lat || !lng) {
			uni.showToast({ title: '暂无门店坐标', icon: 'none' });
			return;
		}
		uni.openLocation({ latitude: lat, longitude: lng, name, address });
	};
</script>

<style scoped>
	.page {
		box-sizing: border-box;
		min-height: 100vh;
		padding: 24rpx 24rpx calc(180rpx + env(safe-area-inset-bottom));
		background: linear-gradient(180deg, #f3fae9 0%, #f8fcf2 20%, #ffffff 44%, #ffffff 100%);
		overflow-x: hidden;
	}

	.state-wrap {
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		min-height: 100vh;
		padding: 80rpx 40rpx;
		background: #f6f8f3;
	}

	.state-text {
		font-size: 28rpx;
		color: #8a9a7a;
		text-align: center;
		line-height: 1.7;
	}

	.state-btn {
		display: flex;
		align-items: center;
		justify-content: center;
		box-sizing: border-box;
		min-width: 240rpx;
		height: 76rpx;
		line-height: normal;
		margin-top: 32rpx;
		padding: 0 40rpx;
		border-radius: 38rpx;
		font-size: 26rpx;
		font-weight: 600;
		color: #ffffff;
		background: linear-gradient(135deg, #7ed321, #6bc01a);
		border: none;
	}

	.section-title,
	.goods-name,
	.goods-meta,
	.goods-price,
	.schedule-label,
	.schedule-time {
		display: block;
	}

	.section-card {
		box-sizing: border-box;
		width: 100%;
		margin-bottom: 20rpx;
		padding: 24rpx 22rpx;
		background: linear-gradient(180deg, #ffffff 0%, #fcfff8 100%);
		border: 1rpx solid rgba(226, 235, 211, 0.92);
		border-radius: 24rpx;
		box-shadow: 0 8rpx 22rpx rgba(34, 45, 25, 0.04);
		overflow: hidden;
	}

	.section-card::before {
		content: '';
		display: block;
		width: 56rpx;
		height: 5rpx;
		margin-bottom: 14rpx;
		border-radius: 999rpx;
		background: linear-gradient(90deg, #cfe9a0, #e9f6cf);
	}

	.compact-fee::before {
		display: none;
	}

	.fulfillment-type-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-top: 16rpx;
		padding-top: 16rpx;
		border-top: 1rpx solid #edf0e9;
		font-size: 22rpx;
	}

	.fulfillment-type-label {
		color: #8a9a7a;
	}

	.fulfillment-type-value {
		color: #1a2114;
		font-weight: 600;
	}

	.section-title {
		font-size: 28rpx;
		font-weight: 600;
		color: #1a2114;
	}

	.section-head {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 16rpx;
	}

	.rent-mode-tag {
		flex-shrink: 0;
		padding: 4rpx 14rpx;
		border-radius: 999rpx;
		font-size: 20rpx;
		font-weight: 600;
		color: #4a8f0f;
		background: rgba(126, 211, 33, 0.14);
	}

	.goods-row {
		display: flex;
		align-items: flex-start;
		margin-top: 10rpx;
		padding: 18rpx;
		background: linear-gradient(180deg, #ffffff 0%, #fbfef5 100%);
		border: 1rpx solid rgba(231, 237, 220, 0.9);
		border-radius: 20rpx;
	}

	.merchant-row {
		margin-top: 14rpx;
	}

	.merchant-head {
		display: flex;
		align-items: flex-start;
		justify-content: space-between;
		gap: 12rpx;
	}

	.phone-link {
		color: #59b50e;
	}

	.goods-block + .goods-block {
		margin-top: 16rpx;
	}

	.goods-image {
		width: 160rpx;
		height: 160rpx;
		border-radius: 16rpx;
		background: #edf2f7;
		flex-shrink: 0;
	}

	.goods-main {
		flex: 1;
		min-width: 0;
		min-height: 160rpx;
		margin-left: 16rpx;
		display: flex;
		flex-direction: column;
	}

	.goods-name {
		font-size: 26rpx;
		font-weight: 600;
		line-height: 1.45;
		color: #1a2114;
	}

	.goods-billing {
		display: block;
		margin-top: auto;
		padding-top: 8rpx;
		font-size: 22rpx;
		font-weight: 600;
		line-height: 1.45;
		color: #1a2114;
		text-align: right;
		word-break: break-all;
	}

	.goods-meta {
		margin-top: 6rpx;
		font-size: 20rpx;
		color: #8a9a7a;
	}

	.goods-package-desc {
		display: block;
		margin-top: 8rpx;
		font-size: 20rpx;
		line-height: 1.55;
		color: #66775b;
	}

	.goods-price {
		margin-top: 12rpx;
		font-size: 26rpx;
		font-weight: 600;
		color: #69b21b;
	}

	.store-actions {
		display: flex;
		gap: 10rpx;
	}

	.store-action {
		display: flex;
		align-items: center;
		justify-content: center;
		width: 52rpx;
		height: 52rpx;
		border-radius: 12rpx;
		background: #f2f8ec;
		flex-shrink: 0;
	}

	.schedule-grid {
		display: flex;
		align-items: stretch;
		margin-top: 16rpx;
		background: #f7f9f5;
		border-radius: 16rpx;
	}

	.schedule-item {
		flex: 1;
		min-width: 0;
		padding: 20rpx;
	}

	.schedule-divider {
		width: 1rpx;
		margin: 20rpx 0;
		background: #dce5d4;
	}

	.schedule-label {
		font-size: 21rpx;
		color: #7b8971;
	}

	.schedule-time {
		margin-top: 10rpx;
		font-size: 24rpx;
		font-weight: 700;
		line-height: 1.45;
		color: #24351e;
		word-break: break-word;
	}

	.schedule-time.emphasis {
		color: #4a8f0f;
	}

	.price-line {
		display: flex;
		justify-content: space-between;
		align-items: center;
		margin-top: 14rpx;
		font-size: 22rpx;
		color: #4a5540;
	}

	.price-line text:last-child {
		font-weight: 500;
	}

	.price-line.discount {
		color: #ff7a45;
	}

	.price-line.total {
		margin-top: 18rpx;
		padding: 18rpx 20rpx;
		border-radius: 18rpx;
		background: linear-gradient(135deg, #fbfef4 0%, #eef8d5 100%);
		font-size: 24rpx;
		font-weight: 600;
		color: #1a2114;
	}

	.price-line.total text:last-child {
		font-size: 30rpx;
		font-weight: 700;
		color: #69b21b;
	}

	.progress-block {
		margin-top: 20rpx;
		padding: 20rpx;
		border-radius: 18rpx;
		background: #f7f9f5;
		border: 1rpx solid rgba(231, 237, 220, 0.9);
	}

	.progress-block:first-of-type {
		margin-top: 14rpx;
	}

	.progress-block + .progress-block {
		margin-top: 16rpx;
	}

	.progress-block-title {
		display: block;
		font-size: 24rpx;
		font-weight: 600;
		color: #1a2114;
	}

	.progress-summary {
		display: block;
		margin-top: 10rpx;
		font-size: 22rpx;
		line-height: 1.6;
		color: #4a5540;
	}

	.progress-row {
		display: flex;
		justify-content: space-between;
		align-items: flex-start;
		gap: 16rpx;
		margin-top: 12rpx;
		font-size: 22rpx;
	}

	.progress-row-label {
		flex-shrink: 0;
		color: #8a9a7a;
	}

	.progress-row-value {
		flex: 1;
		min-width: 0;
		text-align: right;
		color: #1a2114;
		word-break: break-all;
	}

	.progress-note {
		margin-top: 14rpx;
		padding-top: 14rpx;
		border-top: 1rpx solid rgba(220, 229, 212, 0.9);
	}

	.progress-note-label,
	.progress-images-label {
		display: block;
		font-size: 20rpx;
		color: #8a9a7a;
	}

	.progress-note-text {
		display: block;
		margin-top: 8rpx;
		font-size: 22rpx;
		line-height: 1.65;
		color: #1a2114;
		word-break: break-all;
	}

	.progress-images {
		margin-top: 14rpx;
		padding-top: 14rpx;
		border-top: 1rpx solid rgba(220, 229, 212, 0.9);
	}

	.progress-images-label {
		margin-bottom: 12rpx;
	}

	.deposit-settle {
		margin-top: 14rpx;
		padding: 16rpx 18rpx;
		border-radius: 14rpx;
		background: linear-gradient(135deg, #fbfef4 0%, #eef8d5 100%);
		border: 1rpx solid rgba(207, 232, 184, 0.8);
	}

	.deposit-settle-title {
		display: block;
		margin-bottom: 10rpx;
		font-size: 20rpx;
		font-weight: 600;
		color: #4a8f0f;
	}

	.deposit-settle-row {
		display: flex;
		justify-content: space-between;
		align-items: center;
		font-size: 22rpx;
		color: #4a5540;
	}

	.deposit-settle-row + .deposit-settle-row {
		margin-top: 10rpx;
	}

	.deposit-settle-row.highlight {
		margin-top: 12rpx;
		padding-top: 12rpx;
		border-top: 1rpx dashed rgba(126, 211, 33, 0.35);
		font-weight: 600;
		color: #1a2114;
	}

	.deposit-deduct {
		color: #ff7a45;
		font-weight: 600;
	}

	.deposit-refund {
		color: #69b21b;
		font-size: 26rpx;
		font-weight: 700;
	}

	.progress-image {
		width: 148rpx;
		height: 148rpx;
		border-radius: 14rpx;
		background: #f8fafc;
	}

	.order-no-wrap {
		display: flex;
		align-items: center;
		justify-content: flex-end;
		flex: 1;
		min-width: 0;
		gap: 12rpx;
	}

	.order-no-wrap text:first-child {
		flex: 1;
		min-width: 0;
		text-align: right;
		word-break: break-all;
	}

	.copy-btn {
		flex-shrink: 0;
		margin-left: 12rpx;
		padding: 2rpx 12rpx;
		border: 1rpx solid #cfe8b8;
		border-radius: 8rpx;
		font-size: 18rpx;
		color: #5aa51b;
	}

	.image-row {
		display: flex;
		flex-wrap: wrap;
		gap: 12rpx;
	}

	.bottom-space {
		height: 24rpx;
	}

	.bottom-bar {
		box-sizing: border-box;
		position: fixed;
		left: 0;
		right: 0;
		bottom: 0;
		display: flex;
		flex-wrap: wrap;
		justify-content: flex-end;
		gap: 12rpx;
		padding: 16rpx 28rpx calc(16rpx + env(safe-area-inset-bottom));
		background: rgba(255, 255, 255, 0.96);
		border-top: 1rpx solid rgba(222, 234, 202, 0.92);
		box-shadow: 0 -4rpx 16rpx rgba(27, 41, 21, 0.08);
		z-index: 100;
	}

	.primary-btn,
	.ghost-btn,
	.plain-btn,
	.service-btn {
		display: flex;
		align-items: center;
		justify-content: center;
		box-sizing: border-box;
		min-width: 0;
		height: 76rpx;
		line-height: normal;
		margin: 0;
		padding: 0 32rpx;
		border-radius: 38rpx;
		font-size: 26rpx;
		font-weight: 600;
		text-align: center;
	}

	.primary-btn {
		color: #ffffff;
		background: linear-gradient(135deg, #7ed321, #6bc01a);
		box-shadow: 0 4rpx 14rpx rgba(126, 211, 33, 0.3);
		border: none;
	}

	.ghost-btn {
		color: #5cb816;
		background: #f6ffea;
		border: 2rpx solid rgba(126, 211, 33, 0.25);
	}

	.plain-btn {
		color: #6b7a5e;
		background: #f6f8f3;
		border: 2rpx solid #e4e9dd;
	}

	.service-btn {
		color: #ffffff;
		background: linear-gradient(135deg, #8a9a7a, #6b7a5e);
		border: none;
	}
</style>
