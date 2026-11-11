<template>
	<view v-if="order" class="page">
		<view class="section-card goods-card">
			<text class="section-title">租赁商品</text>
			<view v-for="item in order.goodsItems" :key="item.id" class="goods-block">
				<view class="goods-row">
					<image class="goods-image" :src="item.image" mode="aspectFill"></image>
					<view class="goods-main">
						<text class="goods-name">{{ item.name }}</text>
						<text class="goods-meta">{{ item.specLabel }}<text v-if="item.packageLabel"> · {{ item.packageLabel }}</text></text>
						<text v-if="item.packageDesc" class="goods-package-desc">{{ item.packageDesc }}</text>
					</view>
				</view>
			</view>
		</view>

		<view class="section-card goods-card">
			<text class="section-title">订单信息</text>
			<view class="info-line">
				<text class="info-label">订单编号</text>
				<text class="info-value">{{ order.orderNo || order.id }}</text>
			</view>
			<view class="info-line">
				<text class="info-label">联系人</text>
				<text class="info-value">{{ order.user }} {{ order.phone }}</text>
			</view>
			<view v-if="order.rangeText" class="info-line">
				<text class="info-label">租赁时段</text>
				<text class="info-value">{{ order.rangeText }}</text>
			</view>
			<view class="info-line">
				<text class="info-label">订单押金</text>
				<text class="info-value highlight">¥{{ depositText }}</text>
			</view>
		</view>

		<view v-if="hasReturnInfo" class="section-card goods-card">
			<text class="section-title">用户归还信息</text>
			<view v-if="order.returnDate" class="info-line">
				<text class="info-label">归还日期</text>
				<text class="info-value">{{ order.returnDate }}</text>
			</view>
			<view v-if="order.returnMethodLabel" class="info-line">
				<text class="info-label">归还方式</text>
				<text class="info-value">{{ order.returnMethodLabel }}</text>
			</view>
			<view v-if="order.returnRemark" class="return-remark-box">
				<text class="info-label block">用户备注</text>
				<text class="return-remark-text">{{ order.returnRemark }}</text>
			</view>
			<view v-if="order.returnImages?.length" class="upload-row">
				<image
					v-for="(url, index) in order.returnImages"
					:key="`${url}-${index}`"
					class="upload-image"
					:src="url"
					mode="aspectFill"
					@click="previewReturnImage(index)"
				></image>
			</view>
		</view>

		<view class="section-card form-card">
			<text class="section-title">设备状况</text>
			<view class="method-row">
				<view
					v-for="item in conditionOptions"
					:key="item"
					class="method-chip"
					:class="{ active: condition === item }"
					@click="selectCondition(item)"
				>
					{{ item }}
				</view>
			</view>
			<text class="method-hint">{{ conditionHint }}</text>
		</view>

		<view class="section-card form-card">
			<text class="section-title">押金结算</text>
			<view class="settle-line">
				<text class="settle-label">扣款金额</text>
				<view class="settle-input-wrap">
					<text class="settle-prefix">¥</text>
					<input
						class="settle-input"
						type="digit"
						:value="deductInput"
						:disabled="depositAmount <= 0"
						placeholder="0"
						@input="onDeductInput"
						@blur="normalizeDeductInput"
					/>
				</view>
			</view>
			<view class="settle-summary">
				<view class="settle-row">
					<text>订单押金</text>
					<text>¥{{ depositText }}</text>
				</view>
				<view class="settle-row deduct">
					<text>验收扣款</text>
					<text>-¥{{ deductText }}</text>
				</view>
				<view class="settle-row total">
					<text>应退押金</text>
					<text>¥{{ refundText }}</text>
				</view>
			</view>
		</view>

		<view class="section-card form-card">
			<text class="section-title">验收说明<text class="required">*</text></text>
			<textarea
				v-model="remark"
				class="remark-input"
				maxlength="160"
				placeholder="请说明设备状况、污损缺件或扣款依据"
			></textarea>
			<text class="section-title upload-title">验收图片<text class="required">*</text></text>
			<view class="upload-row">
				<view v-for="(item, index) in images" :key="`${item}-${index}`" class="upload-item">
					<image class="upload-image" :src="displayImage(item)" mode="aspectFill" @click="previewImage(index)"></image>
					<view class="upload-remove" @click.stop="removeImage(index)">×</view>
				</view>
				<view v-if="images.length < 3" class="upload-add" @click="chooseImage">+</view>
			</view>
			<text class="upload-hint">至少上传 1 张，最多 3 张</text>
		</view>

		<button class="submit-btn" :loading="submitting" :disabled="submitting" @click="submit">确认验收并结算押金</button>
	</view>
	<view v-else-if="pageLoading" class="loading-wrap">加载中...</view>
	<view v-else class="loading-wrap">订单加载失败，请返回重试</view>
</template>

<script setup>
	import { computed, ref } from 'vue';
	import { onLoad } from '@dcloudio/uni-app';
	import { runAction } from '@/common/actionFeedback';
	import { ensureLogin } from '@/common/auth';
	import { ensureMerchantOperational, fetchOrderDetail, submitInspection } from '@/common/api/merchant';
	import { chooseAndUploadImages, resolveDisplayImageUrl } from '@/common/upload';
	import {
		INSPECTION_CONDITION_OPTIONS,
		calcInspectionRefund,
		clampInspectionDeduct,
		formatMoney,
		suggestInspectionDeduct,
		validateInspectionSubmit
	} from '@/common/merchant/constants';

	const order = ref(null);
	const pageLoading = ref(false);
	const conditionOptions = INSPECTION_CONDITION_OPTIONS;
	const condition = ref('完好');
	const deductInput = ref('0');
	const remark = ref('');
	const images = ref([]);
	const submitting = ref(false);

	const depositAmount = computed(() => Number(order.value?.depositAmount || 0));
	const deductAmount = computed(() => clampInspectionDeduct(deductInput.value, depositAmount.value));
	const refundAmount = computed(() => calcInspectionRefund(depositAmount.value, deductAmount.value));
	const depositText = computed(() => formatMoney(depositAmount.value));
	const deductText = computed(() => formatMoney(deductAmount.value));
	const refundText = computed(() => formatMoney(refundAmount.value));

	const hasReturnInfo = computed(() => Boolean(
		order.value?.returnDate
		|| order.value?.returnMethodLabel
		|| order.value?.returnRemark
		|| order.value?.returnImages?.length
	));

	const conditionHint = computed(() => {
		if (condition.value === '完好') return '设备完好，押金全退';
		if (condition.value === '轻微损坏') return '已填入建议扣款，可按实际污损调整';
		return '严重损坏/缺件，已填入建议扣款，请确认金额';
	});

	const displayImage = (url) => resolveDisplayImageUrl(url);

	onLoad(async (options) => {
		if (!ensureLogin('/packageMerchant/inspection')) return;
		try {
			await ensureMerchantOperational({ redirect: true, toast: true });
		} catch {
			return;
		}
		const orderId = options.id || '';
		pageLoading.value = true;
		try {
			order.value = await fetchOrderDetail(orderId);
			applySuggestedDeduct(condition.value);
		} catch (error) {
			order.value = null;
			uni.showToast({ title: error?.msg || error?.message || '加载订单失败', icon: 'none' });
		} finally {
			pageLoading.value = false;
		}
	});

	const applySuggestedDeduct = (nextCondition) => {
		const suggested = suggestInspectionDeduct(nextCondition, depositAmount.value);
		deductInput.value = String(suggested);
	};

	const selectCondition = (nextCondition) => {
		condition.value = nextCondition;
		applySuggestedDeduct(nextCondition);
	};

	const onDeductInput = (event) => {
		deductInput.value = String(event.detail.value ?? '');
	};

	const normalizeDeductInput = () => {
		deductInput.value = String(deductAmount.value);
	};

	const previewReturnImage = (index) => {
		if (!order.value?.returnImages?.length) return;
		uni.previewImage({
			current: order.value.returnImages[index],
			urls: order.value.returnImages
		});
	};

	const chooseImage = async () => {
		try {
			uni.showLoading({ title: '上传中' });
			const urls = await chooseAndUploadImages({ count: 3 - images.value.length });
			images.value = [...images.value, ...urls].slice(0, 3);
		} catch (error) {
			uni.showToast({ title: error?.message || error?.msg || '图片上传失败', icon: 'none' });
		} finally {
			uni.hideLoading();
		}
	};

	const removeImage = (index) => {
		images.value = images.value.filter((_, idx) => idx !== index);
	};

	const previewImage = (index) => {
		if (!images.value.length) return;
		uni.previewImage({
			current: displayImage(images.value[index]),
			urls: images.value.map((item) => displayImage(item))
		});
	};

	const submit = async () => {
		if (submitting.value || !order.value) return;
		const validation = validateInspectionSubmit({
			condition: condition.value,
			deductAmount: deductAmount.value,
			depositAmount: depositAmount.value,
			remark: remark.value,
			imageUrls: images.value
		});
		if (!validation.ok) {
			uni.showToast({ title: validation.message, icon: 'none' });
			return;
		}

		const confirmContent = validation.deductAmount > 0
			? `将从押金 ¥${depositText.value} 中扣款 ¥${formatMoney(validation.deductAmount)}，退押 ¥${formatMoney(validation.refundAmount)}。确认提交？`
			: `设备完好，将全额退押 ¥${depositText.value}。确认提交？`;

		const confirmed = await new Promise((resolve) => {
			uni.showModal({
				title: '确认验收',
				content: confirmContent,
				success: (res) => resolve(Boolean(res.confirm))
			});
		});
		if (!confirmed) return;

		submitting.value = true;
		const { ok } = await runAction({
			action: () => submitInspection({
				orderId: order.value.id,
				condition: condition.value,
				deductAmount: validation.deductAmount,
				remark: remark.value,
				imageUrls: validation.imageUrls
			}),
			successText: '验收完成，押金已结算',
			errorText: '验收提交失败，请稍后重试'
		});
		submitting.value = false;
		if (!ok) return;
		setTimeout(() => {
			uni.navigateBack();
		}, 400);
	};
</script>

<style scoped>
	.page {
		box-sizing: border-box;
		min-height: 100vh;
		padding: 30rpx 24rpx 48rpx;
		background: #f7f9f5;
		overflow-x: hidden;
	}

	.loading-wrap {
		padding: 120rpx 0;
		text-align: center;
		font-size: 28rpx;
		color: #7d8978;
	}

	.section-card {
		box-sizing: border-box;
		width: 100%;
		margin-bottom: 24rpx;
		padding: 28rpx 24rpx;
		background: #ffffff;
		border: 2rpx solid #e7eedf;
		border-radius: 16rpx;
		box-shadow: 0 8rpx 24rpx rgba(42, 65, 28, 0.045);
		overflow: hidden;
	}

	.section-card::before {
		content: '';
		display: block;
		width: 44rpx;
		height: 4rpx;
		margin-bottom: 18rpx;
		border-radius: 4rpx;
		background: #cbe99c;
	}

	.form-card {
		padding-top: 26rpx;
		padding-bottom: 28rpx;
	}

	.section-title,
	.goods-name,
	.goods-meta,
	.goods-package-desc,
	.info-label,
	.info-value,
	.return-remark-text,
	.method-hint,
	.settle-label,
	.settle-row text {
		display: block;
	}

	.section-title {
		font-size: 30rpx;
		font-weight: 700;
		line-height: 1.4;
		color: #1c2818;
	}

	.required {
		display: inline;
		margin-left: 4rpx;
		color: #ef4444;
	}

	.goods-row {
		display: flex;
		align-items: flex-start;
		margin-top: 18rpx;
		padding: 18rpx;
		background: #fbfdf9;
		border: 2rpx solid #e8efe0;
		border-radius: 12rpx;
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
		margin-left: 16rpx;
	}

	.goods-name {
		font-size: 26rpx;
		font-weight: 600;
		line-height: 1.45;
		color: #1a2114;
	}

	.goods-meta {
		margin-top: 6rpx;
		font-size: 20rpx;
		color: #8a9a7a;
	}

	.goods-package-desc {
		margin-top: 8rpx;
		font-size: 20rpx;
		line-height: 1.55;
		color: #66775b;
	}

	.info-line {
		display: flex;
		align-items: flex-start;
		justify-content: space-between;
		gap: 16rpx;
		margin-top: 18rpx;
		font-size: 24rpx;
		line-height: 1.6;
	}

	.info-label {
		width: 140rpx;
		flex-shrink: 0;
		color: #84917f;
	}

	.info-label.block {
		width: auto;
		margin-bottom: 10rpx;
	}

	.info-value {
		flex: 1;
		min-width: 0;
		text-align: right;
		color: #243021;
		word-break: break-all;
	}

	.info-value.highlight {
		font-weight: 700;
		color: #3f6212;
	}

	.return-remark-box {
		margin-top: 16rpx;
		padding: 20rpx 24rpx;
		border: 2rpx solid #e8efe0;
		border-radius: 12rpx;
		background: #fbfdf9;
	}

	.return-remark-text {
		font-size: 24rpx;
		line-height: 1.7;
		color: #1f2937;
		white-space: pre-wrap;
		word-break: break-all;
	}

	.upload-row {
		display: flex;
		flex-wrap: wrap;
		gap: 16rpx;
		margin-top: 16rpx;
	}

	.upload-image {
		width: 140rpx;
		height: 140rpx;
		border-radius: 18rpx;
		background: #f8fafc;
	}

	.method-row {
		display: flex;
		flex-wrap: wrap;
		gap: 14rpx;
		margin-top: 20rpx;
	}

	.method-chip {
		box-sizing: border-box;
		flex: 1;
		padding: 20rpx 12rpx;
		border: 2rpx solid #e7eee0;
		border-radius: 12rpx;
		text-align: center;
		font-size: 24rpx;
		color: #788474;
		background: #fbfdf9;
	}

	.method-chip.active {
		color: #3f6212;
		font-weight: 700;
		border-color: #cdeaa0;
		background: #effbd7;
	}

	.method-hint {
		margin-top: 16rpx;
		font-size: 22rpx;
		line-height: 1.5;
		color: #8a9a7a;
	}

	.settle-line {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-top: 22rpx;
	}

	.settle-label {
		font-size: 24rpx;
		font-weight: 600;
		color: #33402f;
	}

	.settle-input-wrap {
		display: flex;
		align-items: center;
		width: 240rpx;
		padding: 14rpx 20rpx;
		border: 2rpx solid #e2ebda;
		border-radius: 12rpx;
		background: #fbfdf9;
	}

	.settle-prefix {
		font-size: 24rpx;
		color: #84917f;
		margin-right: 8rpx;
	}

	.settle-input {
		flex: 1;
		font-size: 28rpx;
		font-weight: 600;
		color: #1a2114;
		text-align: right;
	}

	.settle-summary {
		margin-top: 20rpx;
		padding: 20rpx 24rpx;
		border: 2rpx solid #e8efe0;
		border-radius: 12rpx;
		background: #fbfdf9;
	}

	.settle-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
		font-size: 24rpx;
		color: #7d8978;
	}

	.settle-row + .settle-row {
		margin-top: 12rpx;
	}

	.settle-row.deduct text:last-child {
		color: #ef4444;
	}

	.settle-row.total {
		margin-top: 16rpx;
		padding-top: 16rpx;
		border-top: 2rpx solid #e6edde;
		font-weight: 700;
		color: #1a2114;
	}

	.settle-row.total text:last-child {
		color: #3f6212;
		font-size: 30rpx;
	}

	.remark-input {
		display: block;
		height: 160rpx;
		margin-top: 20rpx;
		padding: 20rpx 24rpx;
		border: 2rpx solid #e2ebda;
		border-radius: 12rpx;
		font-size: 24rpx;
		line-height: 1.6;
		color: #243021;
		background: #fbfdf9;
	}

	.submit-btn {
		box-sizing: border-box;
		width: 100%;
		height: 92rpx;
		line-height: 92rpx;
		margin-top: 12rpx;
		border-radius: 12rpx;
		font-size: 28rpx;
		color: #fff;
		background: #62bd16;
		box-shadow: 0 8rpx 18rpx rgba(98, 189, 22, 0.2);
	}

	.upload-title {
		margin-top: 24rpx;
	}

	.upload-hint {
		display: block;
		margin-top: 8rpx;
		font-size: 22rpx;
		line-height: 1.5;
		color: #8a9a7a;
	}

	.upload-item,
	.upload-add {
		width: 140rpx;
		height: 140rpx;
		margin-right: 16rpx;
		margin-bottom: 16rpx;
		border-radius: 12rpx;
		overflow: hidden;
		background: #fbfdf9;
	}

	.upload-item {
		position: relative;
		border: 2rpx solid #e2ebda;
	}

	.upload-item .upload-image {
		width: 100%;
		height: 100%;
	}

	.upload-add {
		display: flex;
		align-items: center;
		justify-content: center;
		border: 2rpx dashed #cfe8a9;
		font-size: 48rpx;
		color: #8a9a7a;
	}

	.upload-remove {
		position: absolute;
		top: 8rpx;
		right: 8rpx;
		width: 36rpx;
		height: 36rpx;
		line-height: 32rpx;
		text-align: center;
		border-radius: 50%;
		font-size: 24rpx;
		color: #fff;
		background: rgba(0, 0, 0, 0.45);
	}
</style>
