<template>
	<view v-if="order" class="page">
		<view class="section-card stock-card">
			<view class="section-head">
				<text class="section-title">备货内容</text>
				<text v-if="order.rentModeLabel" class="rent-mode-tag">{{ order.rentModeLabel }}</text>
			</view>
			<view v-for="item in order.goodsItems" :key="item.id" class="stock-block">
				<view class="goods-row">
					<image class="goods-image" :src="item.image" mode="aspectFill"></image>
					<view class="goods-main">
						<view class="goods-head">
							<text class="goods-name">{{ item.name }}</text>
							<text v-if="formatItemBilling(item)" class="goods-billing">{{ formatItemBilling(item) }}</text>
						</view>
						<text class="goods-meta">{{ item.specLabel }}<text v-if="item.packageLabel"> · {{ item.packageLabel }}</text></text>
						<text v-if="item.packageDesc" class="goods-package-desc">{{ item.packageDesc }}</text>
						<text class="goods-qty">数量 {{ item.count || 1 }} 件</text>
					</view>
				</view>
			</view>
		</view>

		<view class="section-card">
			<text class="section-title">履约信息</text>
			<view class="info-box">
				<view class="info-head">
					<text class="info-title">{{ pickupTypeLabel }}</text>
					<view v-if="order.phone" class="store-actions">
						<view class="store-action" @click="callUser">
							<up-icon name="phone" size="16" color="#4d9e16"></up-icon>
						</view>
					</view>
				</view>
				<text class="info-desc">{{ order.address || '--' }}</text>
				<text v-if="order.remark" class="info-desc">备注：{{ order.remark }}</text>
			</view>
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
			<view class="contact-row">
				<text class="contact-label">用户</text>
				<text class="contact-value">{{ order.user }} {{ order.phone }}</text>
			</view>
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
			<view class="price-line">
				<text>订单状态</text>
				<text>{{ currentStatusLabel }}</text>
			</view>
			<view class="price-line">
				<text>下单时间</text>
				<text>{{ order.createTime || order.time || '--' }}</text>
			</view>
		</view>

		<view class="section-card fee-card">
			<text class="section-title">费用明细</text>
			<view class="price-line total">
				<text>实付金额</text>
				<text>¥{{ formatPrice(order.payAmount) }}</text>
			</view>
		</view>

		<view v-if="hasLifecycleInfo" class="section-card">
			<text class="section-title">进度信息</text>
			<view v-if="order.returnDate" class="meta-line">
				<text class="meta-label">归还预约</text>
				<text class="meta-value">{{ order.returnDate }} / {{ order.returnMethodLabel || '到店归还' }}</text>
			</view>
			<view v-if="order.returnRemark" class="meta-line">
				<text class="meta-label">归还备注</text>
				<text class="meta-value">{{ order.returnRemark }}</text>
			</view>
			<view v-if="order.returnImages?.length" class="return-images">
				<text class="meta-label block">归还图片</text>
				<view class="image-row">
					<image
						v-for="(url, index) in order.returnImages"
						:key="`${url}-${index}`"
						class="return-image"
						:src="url"
						mode="aspectFill"
						@click="previewReturnImage(index)"
					></image>
				</view>
			</view>
			<view v-if="order.inspectionRemark" class="meta-line">
				<text class="meta-label">验收说明</text>
				<text class="meta-value">{{ order.inspectionRemark }}</text>
			</view>
			<view v-if="order.inspectionImages?.length" class="return-images">
				<text class="meta-label block">验收图片</text>
				<view class="image-row">
					<image
						v-for="(url, index) in order.inspectionImages"
						:key="`${url}-${index}`"
						class="return-image"
						:src="url"
						mode="aspectFill"
						@click="previewInspectionImage(index)"
					></image>
				</view>
			</view>
			<view v-if="order.inspectionDeductAmount != null" class="meta-line">
				<text class="meta-label">扣款金额</text>
				<text class="meta-value">¥{{ formatPrice(order.inspectionDeductAmount) }}</text>
			</view>
			<view v-if="order.inspectionRefundAmount != null" class="meta-line">
				<text class="meta-label">押金退款</text>
				<text class="meta-value">¥{{ formatPrice(order.inspectionRefundAmount) }}</text>
			</view>
			<view v-for="(item, index) in timeline" :key="item.title + item.time" class="timeline-row" :class="{ last: index === timeline.length - 1 }">
				<view class="timeline-dot" :class="{ active: index === 0 }"></view>
				<view class="timeline-main">
					<text class="timeline-title">{{ item.title }}</text>
					<text class="timeline-time">{{ item.time }}</text>
				</view>
			</view>
		</view>

		<view v-if="showPickupVerify" class="section-card verify-card">
			<text class="section-title">核销取货</text>
			<text class="verify-desc">请扫描用户取货凭证二维码，或手动输入 6 位提货码完成核销。</text>
			<input
				v-model="pickupCodeInput"
				class="verify-input"
				type="number"
				maxlength="12"
				placeholder="请输入提货码"
				:disabled="processing"
			/>
			<view class="verify-actions">
				<view class="ghost-btn" :class="{ disabled: processing }" @click="scanPickupCode">扫码核销</view>
				<view class="primary-btn" :class="{ disabled: processing }" @click="submitPickupVerify">
					{{ processing ? '核销中...' : '确认核销' }}
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
		<view class="ghost-btn" @click="openContactPopup">联系用户</view>
		<view v-if="showPrepareButton" class="primary-btn" :class="{ disabled: processing }" @click="processPrepare">
			{{ processing ? '处理中...' : '确认备货' }}
		</view>
		<view v-else-if="showInspectionButton" class="primary-btn" @click="goInspection">录入验收结果</view>
	</view>

	<ContactUserPopup
		v-model:show="contactPopupShow"
		:order-id="orderId"
		:fallback="contactFallback"
	/>
</template>

<script setup>
	import { computed, ref } from 'vue';
	import { onLoad, onShow } from '@dcloudio/uni-app';
	import { runAction } from '@/common/actionFeedback';
	import ContactUserPopup from '@/components/ContactUserPopup.vue';
	import { ensureLogin } from '@/common/auth';
	import {
		ensureMerchantOperational,
		executeMerchantOrderAction,
		fetchOrderDetail,
		normalizePickupCode
	} from '@/common/api/merchant';
	import {
		CONFIRM_PICKUP_TYPES,
		buildRentPriceBreakdown,
		formatDisplayPrice,
		formatRentPeriodDisplay
	} from '@/common/rental/constants';

	const order = ref(null);
	const orderId = ref('');
	const pageLoading = ref(false);
	const processing = ref(false);
	const pickupCodeInput = ref('');
	const contactPopupShow = ref(false);
	const contactFallback = ref({});
	let skipNextShowRefresh = false;

	const syncOrder = async ({ silent = false } = {}) => {
		if (!orderId.value) return;
		if (!silent) {
			pageLoading.value = true;
		}
		try {
			await ensureMerchantOperational({ redirect: true, toast: true });
			order.value = await fetchOrderDetail(orderId.value);
		} catch (error) {
			if (!silent || !order.value) {
				order.value = null;
			}
			if (!silent) {
				uni.showToast({ title: error?.msg || error?.message || '加载订单失败', icon: 'none' });
			}
		} finally {
			if (!silent) {
				pageLoading.value = false;
			}
		}
	};

	onLoad((options) => {
		if (!ensureLogin('/packageMerchant/order-detail')) return;
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
		syncOrder({ silent: Boolean(order.value) });
	});

	const timeline = computed(() => order.value?.timeline || []);
	const currentStatusLabel = computed(() => order.value?.status || '订单处理中');
	const showInspectionButton = computed(() => order.value?.nextAction === 'inspection');
	const showPrepareButton = computed(() => order.value?.nextAction === 'prepare');
	const showPickupVerify = computed(() => order.value?.nextAction === 'pickup');
	const hasLifecycleInfo = computed(() =>
		Boolean(
			order.value?.returnDate
			|| order.value?.returnRemark
			|| order.value?.returnImages?.length
			|| order.value?.inspectionRemark
			|| order.value?.inspectionImages?.length
			|| order.value?.inspectionDeductAmount != null
			|| order.value?.inspectionRefundAmount != null
			|| timeline.value.length
		)
	);

	const pickupTypeLabel = computed(() =>
		order.value?.pickupType
		|| CONFIRM_PICKUP_TYPES.find((item) => item.key === order.value?.pickupTypeKey)?.label
		|| '门店自提'
	);
	const pickupTime = computed(() => formatRentPeriodDisplay(order.value?.startTime));
	const returnTime = computed(() => formatRentPeriodDisplay(order.value?.endTime));
	const pickupLabel = computed(() => order.value?.pickupTypeKey === 'delivery' ? '送达时间' : '取货时间');

	const formatPrice = (value) => formatDisplayPrice(value);

	const formatItemBilling = (item) => {
		const chips = buildRentPriceBreakdown(item, order.value || {}).chips || [];
		if (!chips.length) return '';
		return chips.map((chip) => chip.value).join(' × ');
	};

	const copyOrderNo = () => {
		const orderNo = order.value?.orderNo || order.value?.id;
		if (!orderNo) return;
		uni.setClipboardData({ data: String(orderNo) });
	};

	const callUser = async () => {
		let phone = order.value?.phone;
		if (!phone && order.value) {
			contactFallback.value = {
				userName: order.value.user,
				contactPhone: order.value.phone,
				userId: order.value.userId
			};
			contactPopupShow.value = true;
			return;
		}
		if (!phone) {
			uni.showToast({ title: '暂无用户电话', icon: 'none' });
			return;
		}
		uni.makePhoneCall({ phoneNumber: String(phone) });
	};

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

	const openContactPopup = () => {
		if (!order.value) return;
		contactFallback.value = {
			userName: order.value.user,
			contactPhone: order.value.phone,
			userId: order.value.userId
		};
		contactPopupShow.value = true;
	};

	const goInspection = () => {
		if (!order.value) return;
		uni.navigateTo({ url: `/packageMerchant/inspection?id=${encodeURIComponent(order.value.id)}` });
	};

	const processPrepare = async () => {
		if (processing.value || !order.value) return;
		processing.value = true;
		const { ok } = await runAction({
			action: () => executeMerchantOrderAction(order.value),
			successText: '备货完成',
			errorText: '订单处理失败，请稍后重试'
		});
		processing.value = false;
		if (!ok) return;
		syncOrder();
	};

	const submitPickupVerify = async () => {
		if (processing.value || !order.value) return;
		const pickupCode = normalizePickupCode(pickupCodeInput.value);
		if (!pickupCode) {
			uni.showToast({ title: '请输入提货码', icon: 'none' });
			return;
		}
		processing.value = true;
		const { ok } = await runAction({
			action: () => executeMerchantOrderAction(order.value, { pickupCode }),
			successText: '核销成功',
			errorText: '核销失败，请核对提货码后重试'
		});
		processing.value = false;
		if (!ok) return;
		pickupCodeInput.value = '';
		syncOrder();
	};

	const scanPickupCode = () => {
		if (processing.value) return;
		uni.scanCode({
			onlyFromCamera: false,
			success: (res) => {
				const code = normalizePickupCode(res.result);
				if (!code) {
					uni.showToast({ title: '未识别到有效提货码', icon: 'none' });
					return;
				}
				pickupCodeInput.value = code;
				submitPickupVerify();
			},
			fail: () => {
				uni.showToast({ title: '扫码已取消', icon: 'none' });
			}
		});
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
	.info-title,
	.info-desc,
	.schedule-label,
	.schedule-time,
	.contact-label,
	.contact-value,
	.meta-label,
	.meta-value,
	.timeline-title,
	.timeline-time,
	.verify-desc {
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

	.stock-block + .stock-block {
		margin-top: 16rpx;
		padding-top: 16rpx;
		border-top: 1rpx solid rgba(231, 237, 220, 0.9);
	}

	.goods-head {
		display: flex;
		align-items: flex-start;
		justify-content: space-between;
		gap: 16rpx;
	}

	.goods-billing {
		flex-shrink: 0;
		max-width: 52%;
		font-size: 22rpx;
		font-weight: 600;
		line-height: 1.45;
		color: #1a2114;
		text-align: right;
		word-break: break-all;
	}

	.goods-qty {
		display: block;
		margin-top: 10rpx;
		font-size: 22rpx;
		font-weight: 600;
		color: #4a8f0f;
	}

	.goods-row {
		display: flex;
		align-items: flex-start;
		margin-top: 14rpx;
		padding: 18rpx;
		background: linear-gradient(180deg, #ffffff 0%, #fbfef5 100%);
		border: 1rpx solid rgba(231, 237, 220, 0.9);
		border-radius: 20rpx;
	}

	.goods-name {
		flex: 1;
		min-width: 0;
		font-size: 26rpx;
		font-weight: 600;
		line-height: 1.45;
		color: #1a2114;
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
		margin-left: 16rpx;
		display: flex;
		flex-direction: column;
		justify-content: center;
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

	.info-box {
		box-sizing: border-box;
		margin-top: 14rpx;
		padding: 20rpx 22rpx;
		border-radius: 20rpx;
		background: linear-gradient(180deg, #fbfef4 0%, #f5fae8 100%);
		border: 1rpx solid rgba(212, 229, 180, 0.88);
	}

	.info-head {
		display: flex;
		align-items: center;
		justify-content: space-between;
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
	}

	.info-title {
		font-size: 24rpx;
		font-weight: 600;
		color: #1a2114;
	}

	.info-desc {
		margin-top: 8rpx;
		font-size: 20rpx;
		line-height: 1.55;
		color: #66775b;
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

	.contact-row {
		display: flex;
		align-items: flex-start;
		justify-content: space-between;
		margin-top: 16rpx;
		padding-top: 16rpx;
		border-top: 1rpx solid #edf0e9;
		font-size: 22rpx;
	}

	.contact-label,
	.meta-label {
		width: 132rpx;
		flex-shrink: 0;
		color: #8a9a7a;
	}

	.contact-value,
	.meta-value {
		flex: 1;
		min-width: 0;
		text-align: right;
		color: #1a2114;
		word-break: break-all;
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

	.meta-line {
		display: flex;
		justify-content: space-between;
		align-items: flex-start;
		margin-top: 16rpx;
		font-size: 22rpx;
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

	.meta-label.block {
		width: auto;
		margin-bottom: 12rpx;
	}

	.return-images {
		margin-top: 16rpx;
	}

	.image-row {
		display: flex;
		flex-wrap: wrap;
		gap: 16rpx;
	}

	.return-image {
		width: 160rpx;
		height: 160rpx;
		border-radius: 16rpx;
		background: #f8fafc;
	}

	.timeline-row {
		display: flex;
		margin-top: 20rpx;
		position: relative;
	}

	.timeline-row:not(.last)::before {
		content: '';
		position: absolute;
		left: 10rpx;
		top: 28rpx;
		width: 2rpx;
		height: calc(100% + 8rpx);
		background: #e4e9dd;
	}

	.timeline-dot {
		width: 20rpx;
		height: 20rpx;
		margin-top: 6rpx;
		border-radius: 50%;
		background: #e4e9dd;
		flex-shrink: 0;
		position: relative;
		z-index: 1;
		border: 3rpx solid #ffffff;
		box-shadow: 0 0 0 2rpx #e4e9dd;
	}

	.timeline-dot.active {
		background: #7ed321;
		box-shadow: 0 0 0 2rpx rgba(126, 211, 33, 0.3);
	}

	.timeline-main {
		flex: 1;
		min-width: 0;
		margin-left: 16rpx;
	}

	.timeline-title {
		font-size: 22rpx;
		font-weight: 600;
		color: #1a2114;
	}

	.timeline-time {
		margin-top: 6rpx;
		font-size: 20rpx;
		color: #8a9a7a;
	}

	.verify-desc {
		margin-top: 12rpx;
		font-size: 22rpx;
		line-height: 1.6;
		color: #66775b;
	}

	.verify-input {
		box-sizing: border-box;
		width: 100%;
		height: 84rpx;
		margin-top: 16rpx;
		padding: 0 24rpx;
		border-radius: 16rpx;
		background: #f7f9f5;
		border: 1rpx solid rgba(212, 229, 180, 0.65);
		font-size: 32rpx;
		font-weight: 700;
		letter-spacing: 4rpx;
		color: #1a2114;
	}

	.verify-actions {
		display: flex;
		gap: 12rpx;
		margin-top: 16rpx;
	}

	.verify-actions .primary-btn,
	.verify-actions .ghost-btn {
		flex: 1;
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
	.ghost-btn {
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

	.primary-btn.disabled,
	.ghost-btn.disabled {
		opacity: 0.55;
		pointer-events: none;
	}
</style>
