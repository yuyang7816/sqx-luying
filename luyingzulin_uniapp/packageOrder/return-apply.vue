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

		<view v-if="hasStoreInfo" class="section-card goods-card">
			<view class="section-head">
				<text class="section-title">商家信息</text>
				<text class="copy-btn section-copy-btn" @click="copyAllStoreInfo">复制</text>
			</view>
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
					<text v-if="order.store.address" class="goods-meta">{{ order.store.address }}</text>
					<text v-if="order.store.businessHours" class="goods-meta">营业时间 {{ order.store.businessHours }}</text>
					<text
						v-if="order.store.contact"
						class="goods-meta phone-link"
						@click="callStore"
					>门店电话 {{ order.store.contact }}</text>
				</view>
			</view>
		</view>

		<view class="section-card form-card">
			<text class="section-title">归还方式</text>
			<view class="method-row">
				<view
					v-for="item in methods"
					:key="item.key"
					class="method-chip"
					:class="{ active: method === item.key }"
					@click="method = item.key"
				>
					{{ item.label }}
				</view>
			</view>
			<text v-if="methodDesc" class="method-hint">{{ methodDesc }}</text>
		</view>

		<view class="section-card form-card">
			<text class="section-title">归还日期</text>
			<picker mode="date" :value="returnDate" @change="onDateChange">
				<view class="picker-box">{{ returnDate || '请选择日期' }}</view>
			</picker>
		</view>

		<view class="section-card form-card">
			<text class="section-title">备注（选填）</text>
			<textarea
				v-model="reason"
				class="remark-input"
				maxlength="200"
				placeholder="如有损坏、缺件等情况可说明"
			></textarea>
			<view class="upload-row">
				<view v-for="(item, index) in images" :key="`${item}-${index}`" class="upload-item">
					<image class="upload-image" :src="item" mode="aspectFill" @click="previewImage(index)"></image>
					<view class="upload-remove" @click.stop="removeImage(index)">×</view>
				</view>
				<view v-if="images.length < 3" class="upload-add" @click="chooseImage">+</view>
			</view>
		</view>

		<button class="submit-btn" :loading="submitting" :disabled="submitting" @click="submitReturn">提交申请</button>
	</view>
	<view v-else-if="pageLoading" class="loading-wrap">加载中...</view>
</template>

<script setup>
	import { computed, ref } from 'vue';
	import { onLoad } from '@dcloudio/uni-app';
	import { runAction } from '@/common/actionFeedback';
	import { applyReturn, fetchOrderDetail } from '@/common/api/order';
	import { ensureLogin } from '@/common/auth';
	import { RETURN_METHOD_OPTIONS, validateReturnSubmit } from '@/common/rental/constants';
	import { chooseAndUploadImages } from '@/common/upload';

	const methods = RETURN_METHOD_OPTIONS.map((item) => ({ key: item.key, label: item.label }));
	const order = ref(null);
	const orderId = ref('');
	const method = ref('store');
	const pageLoading = ref(false);
	const submitting = ref(false);
	const returnDate = ref('');
	const reason = ref('');
	const images = ref([]);

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

	const formatDefaultDate = () => {
		const now = new Date();
		const pad = (n) => String(n).padStart(2, '0');
		return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
	};

	onLoad(async (options) => {
		if (!ensureLogin(`/packageOrder/return-apply?id=${options.id || ''}`)) return;
		orderId.value = options.id || '';
		returnDate.value = formatDefaultDate();
		pageLoading.value = true;
		try {
			order.value = await fetchOrderDetail(orderId.value);
		} catch (error) {
			order.value = null;
		} finally {
			pageLoading.value = false;
		}
	});

	const methodDesc = computed(() => {
		if (method.value === 'delivery') return '请自行叫同城配送，将设备送至商户门店。';
		return '按预约时间到店归还即可。';
	});

	const copyText = (text, emptyTip = '暂无内容') => {
		if (!text) {
			uni.showToast({ title: emptyTip, icon: 'none' });
			return;
		}
		uni.setClipboardData({
			data: String(text),
			success: () => uni.showToast({ title: '已复制', icon: 'none' })
		});
	};

	const buildStoreInfoText = () => {
		const store = order.value?.store || {};
		const lines = [];
		if (store.name) lines.push(`商家：${store.name}`);
		if (store.branchName) lines.push(`门店：${store.branchName}`);
		if (store.address) lines.push(`地址：${store.address}`);
		if (store.businessHours) lines.push(`营业时间：${store.businessHours}`);
		if (store.contact) lines.push(`门店电话：${store.contact}`);
		return lines.join('\n');
	};

	const copyAllStoreInfo = () => copyText(buildStoreInfoText(), '暂无商家信息');

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

	const onDateChange = (event) => {
		returnDate.value = event.detail.value;
	};

	const chooseImage = async () => {
		try {
			uni.showLoading({ title: '上传中' });
			const urls = await chooseAndUploadImages({ count: 3 - images.value.length });
			images.value = [...images.value, ...urls].slice(0, 3);
		} catch (error) {
			uni.showToast({ title: error.message || '图片上传失败', icon: 'none' });
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
			current: images.value[index],
			urls: images.value
		});
	};

	const submitReturn = async () => {
		if (submitting.value) return;
		const validation = validateReturnSubmit({ returnDate: returnDate.value });
		if (!validation.ok) {
			uni.showToast({ title: validation.message, icon: 'none' });
			return;
		}
		submitting.value = true;
		const { ok } = await runAction({
			action: () => applyReturn({
				orderId: orderId.value,
				returnMethod: method.value,
				returnDate: returnDate.value,
				remark: reason.value,
				images: images.value
			}),
			successText: '归还申请已提交',
			errorText: '提交失败，请稍后重试'
		});
		submitting.value = false;
		if (!ok) return;
		setTimeout(() => {
			const pages = getCurrentPages();
			const prevPage = pages.length > 1 ? pages[pages.length - 2] : null;
			const prevRoute = prevPage?.route || '';
			if (prevRoute.includes('packageOrder/detail')) {
				uni.navigateBack();
				return;
			}
			uni.redirectTo({ url: `/packageOrder/detail?id=${orderId.value}` });
		}, 400);
	};
</script>

<style scoped>
	.page {
		box-sizing: border-box;
		min-height: 100vh;
		padding: 24rpx 24rpx 40rpx;
		background: linear-gradient(180deg, #f3fae9 0%, #f8fcf2 20%, #ffffff 44%, #ffffff 100%);
		overflow-x: hidden;
	}

	.loading-wrap {
		padding: 120rpx 0;
		text-align: center;
		font-size: 28rpx;
		color: #6b7280;
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

	.form-card {
		padding: 26rpx 22rpx;
	}

	.section-title,
	.goods-name,
	.goods-meta,
	.method-hint {
		display: block;
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

	.section-head .section-title {
		flex: 1;
		min-width: 0;
	}

	.section-copy-btn {
		margin-top: 0;
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
		display: flex;
		flex-direction: column;
		justify-content: center;
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
		display: block;
		margin-top: 8rpx;
		font-size: 20rpx;
		line-height: 1.55;
		color: #66775b;
	}

	.phone-link {
		color: #59b50e;
	}

	.copy-btn {
		flex-shrink: 0;
		padding: 2rpx 12rpx;
		border: 1rpx solid #cfe8b8;
		border-radius: 8rpx;
		font-size: 18rpx;
		line-height: 1.8;
		color: #5aa51b;
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

	.method-row {
		display: flex;
		gap: 16rpx;
		margin-top: 18rpx;
	}

	.method-chip {
		flex: 1;
		padding: 20rpx 0;
		border: 1rpx solid #e7eddc;
		border-radius: 16rpx;
		text-align: center;
		font-size: 26rpx;
		color: #74806d;
		background: #fbfef7;
	}

	.method-chip.active {
		color: #3f6212;
		font-weight: 700;
		border-color: #cfe8a9;
		background: #effbd8;
	}

	.method-hint {
		margin-top: 14rpx;
		font-size: 22rpx;
		line-height: 1.5;
		color: #8a9a7a;
	}

	.picker-box {
		box-sizing: border-box;
		margin-top: 18rpx;
		padding: 22rpx 24rpx;
		border: 1rpx solid #e7eddc;
		border-radius: 16rpx;
		font-size: 26rpx;
		color: #1a2114;
		background: #fbfef7;
	}

	.remark-input {
		display: block;
		height: 140rpx;
		margin-top: 18rpx;
		padding: 20rpx 24rpx;
		border: 1rpx solid #e7eddc;
		border-radius: 16rpx;
		font-size: 24rpx;
		line-height: 1.6;
		color: #1a2114;
		background: #fbfef7;
	}

	.upload-row {
		display: flex;
		flex-wrap: wrap;
		margin-top: 18rpx;
	}

	.upload-item,
	.upload-add {
		width: 140rpx;
		height: 140rpx;
		margin-right: 16rpx;
		margin-bottom: 16rpx;
		border-radius: 16rpx;
		overflow: hidden;
		background: #fbfef7;
	}

	.upload-item {
		position: relative;
	}

	.upload-image {
		width: 100%;
		height: 100%;
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

	.upload-add {
		display: flex;
		align-items: center;
		justify-content: center;
		border: 1rpx dashed #cfe1bd;
		font-size: 48rpx;
		color: #79a957;
	}

	.submit-btn {
		box-sizing: border-box;
		width: 100%;
		height: 88rpx;
		line-height: 88rpx;
		margin-top: 8rpx;
		border-radius: 44rpx;
		font-size: 28rpx;
		color: #fff;
		background: linear-gradient(135deg, #7ed321, #59b50e);
	}
</style>
