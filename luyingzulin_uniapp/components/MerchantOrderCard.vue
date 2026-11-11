<template>
	<view class="order-card" @click="emit('click')">
		<view class="order-head">
			<text class="order-no">订单编号：{{ order.orderNo || order.id }}</text>
			<text class="order-status" :class="`status-${order.statusType || 'pending'}`">{{ order.statusLabel || order.status }}</text>
		</view>

		<view v-for="item in order.goodsItems || []" :key="`${order.id}-${item.id}`" class="goods-block">
			<view class="goods-row">
				<image class="goods-image" :src="item.image" mode="aspectFill"></image>
				<view class="goods-main">
					<text class="goods-name">{{ item.name }}</text>
					<text class="goods-meta">{{ item.specLabel }}<text v-if="item.packageLabel"> · {{ item.packageLabel }}</text></text>
					<text v-if="showUser" class="goods-meta user-meta">用户：{{ order.user }} · {{ order.pickupType }}</text>
					<text v-if="order.returnDate" class="goods-meta accent-meta">{{ order.returnMethodLabel || '到店归还' }} · {{ order.returnDate }}</text>
				</view>
			</view>
			<view class="schedule-grid">
				<view class="schedule-item">
					<text class="schedule-label">取货时间</text>
					<text class="schedule-time">{{ pickupText }}</text>
				</view>
				<view class="schedule-divider"></view>
				<view class="schedule-item">
					<text class="schedule-label">应还时间</text>
					<text class="schedule-time emphasis">{{ returnText }}</text>
				</view>
			</view>
		</view>

		<view class="summary-row">
			<text class="summary-text">{{ displayAmount }}</text>
			<text class="summary-time">{{ order.createTime || order.time }}</text>
		</view>

		<view v-if="$slots.actions" class="action-row" @click.stop>
			<slot name="actions" />
		</view>
	</view>
</template>

<script setup>
	import { computed } from 'vue';
	import { formatRentPeriodDisplay } from '@/common/rental/constants';

	const props = defineProps({
		order: { type: Object, required: true },
		showUser: { type: Boolean, default: true }
	});

	const emit = defineEmits(['click']);

	const pickupText = computed(() => formatRentPeriodDisplay(props.order.startTime));
	const returnText = computed(() => formatRentPeriodDisplay(props.order.endTime));

	const displayAmount = computed(() => {
		const amount = props.order.payAmount ?? props.order.amount ?? 0;
		return Number(amount || 0).toFixed(1);
	});
</script>

<style scoped>
	.order-card {
		box-sizing: border-box;
		width: 100%;
		margin-bottom: 28rpx;
		padding: 32rpx;
		background: #ffffff;
		border-radius: 32rpx;
		box-shadow: 0 6rpx 24rpx rgba(0, 0, 0, 0.04);
		overflow: hidden;
	}

	.order-head {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-bottom: 24rpx;
	}

	.order-no,
	.order-status,
	.goods-name,
	.goods-meta,
	.summary-text,
	.schedule-label,
	.schedule-time {
		display: block;
	}

	.order-no {
		flex: 1;
		min-width: 0;
		font-size: 24rpx;
		color: #8a9a7a;
		font-weight: 500;
	}

	.order-status {
		flex-shrink: 0;
		margin-left: 16rpx;
		font-size: 24rpx;
		font-weight: 700;
		color: #7ed321;
	}

	.order-status.status-pending {
		color: #c88a00;
	}

	.order-status.status-running,
	.order-status.status-review {
		color: #7ed321;
	}

	.order-status.status-completed {
		color: #9aa2b1;
	}

	.goods-block + .goods-block {
		margin-top: 16rpx;
	}

	.goods-row {
		display: flex;
		align-items: flex-start;
	}

	.goods-image {
		width: 148rpx;
		height: 148rpx;
		border-radius: 20rpx;
		background: #edf2f7;
		flex-shrink: 0;
	}

	.goods-main {
		flex: 1;
		min-width: 0;
		margin-left: 20rpx;
	}

	.goods-name {
		font-size: 28rpx;
		font-weight: 700;
		line-height: 1.4;
		color: #1a2114;
		display: -webkit-box;
		-webkit-line-clamp: 2;
		-webkit-box-orient: vertical;
		overflow: hidden;
	}

	.goods-meta {
		margin-top: 8rpx;
		font-size: 22rpx;
		color: #8a9a7a;
	}

	.goods-meta.user-meta {
		margin-top: 10rpx;
	}

	.goods-meta.accent-meta {
		margin-top: 6rpx;
		color: #5aad15;
		font-weight: 600;
	}

	.schedule-grid {
		display: flex;
		align-items: stretch;
		margin-top: 14rpx;
		background: #f7f9f5;
		border-radius: 16rpx;
	}

	.schedule-item {
		flex: 1;
		min-width: 0;
		padding: 18rpx 20rpx;
	}

	.schedule-divider {
		width: 1rpx;
		margin: 18rpx 0;
		background: #dce5d4;
	}

	.schedule-label {
		font-size: 21rpx;
		color: #7b8971;
	}

	.schedule-time {
		margin-top: 8rpx;
		font-size: 24rpx;
		font-weight: 700;
		line-height: 1.45;
		color: #24351e;
		word-break: break-word;
	}

	.schedule-time.emphasis {
		color: #4a8f0f;
	}

	.summary-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-top: 20rpx;
		padding-top: 20rpx;
		border-top: 1rpx solid #f1f4ec;
	}

	.summary-text,
	.summary-time {
		display: block;
	}

	.summary-text {
		font-size: 36rpx;
		font-weight: 800;
		color: #1a2114;
		letter-spacing: -1rpx;
	}

	.summary-text::before {
		content: '¥';
		font-size: 26rpx;
		font-weight: 700;
		margin-right: 2rpx;
	}

	.summary-time {
		flex: 1;
		min-width: 0;
		margin-left: 16rpx;
		text-align: right;
		font-size: 22rpx;
		line-height: 1.6;
		color: #9aa58a;
		word-break: break-all;
	}

	.action-row {
		display: flex;
		justify-content: flex-end;
		flex-wrap: wrap;
		margin-top: 24rpx;
		margin-left: -16rpx;
	}

	.action-row :deep(.plain-btn),
	.action-row :deep(.ghost-btn),
	.action-row :deep(.primary-btn) {
		display: flex;
		align-items: center;
		justify-content: center;
		box-sizing: border-box;
		min-width: 0;
		width: auto;
		min-height: 68rpx;
		padding: 0 32rpx;
		line-height: normal;
		margin: 0 0 0 16rpx;
		border-radius: 36rpx;
		font-size: 26rpx;
		font-weight: 600;
		text-align: center;
	}

	.action-row :deep(.primary-btn) {
		color: #ffffff;
		background: linear-gradient(135deg, #7ed321, #6bc01a);
		box-shadow: 0 4rpx 14rpx rgba(126, 211, 33, 0.3);
	}

	.action-row :deep(.ghost-btn) {
		color: #5cb816;
		background: #f6ffea;
		border: 2rpx solid rgba(126, 211, 33, 0.2);
	}

	.action-row :deep(.plain-btn) {
		color: #6b7a5e;
		background: #f6f8f3;
		border: 2rpx solid #e4e9dd;
	}

	.action-row :deep(.primary-btn.disabled),
	.action-row :deep(.ghost-btn.disabled) {
		opacity: 0.55;
		pointer-events: none;
	}
</style>
