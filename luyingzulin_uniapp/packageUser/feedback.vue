<template>
	<view class="page">
		<view class="card">
			<text class="section-title">反馈类型</text>
			<view class="type-picker" @click="chooseType">
				<text class="type-value">{{ currentType }}</text>
				<up-icon name="arrow-down" size="16" color="#98a2af"></up-icon>
			</view>
			<text class="section-title content-title">问题描述</text>
			<textarea v-model="content" class="textarea" maxlength="400" placeholder="请描述遇到的问题、页面位置和期望处理方式"></textarea>
			<text class="count">{{ content.length }}/400</text>
		</view>

		<view class="card">
			<text class="section-title">上传截图</text>
			<view class="upload-row">
				<view v-for="(item, index) in images" :key="`${item}-${index}`" class="upload-item">
					<image class="upload-image" :src="item" mode="aspectFill"></image>
				</view>
				<view v-if="images.length < 3" class="upload-add" @click="chooseImage">上传图片</view>
			</view>
			<text class="upload-tip">最多 3 张，便于客服快速定位问题。</text>
		</view>

		<view class="card">
			<text class="section-title">联系方式</text>
			<input v-model="contact" class="input" placeholder="请输入手机号或微信号，便于回访" />
		</view>

		<button class="submit-btn" :loading="submitting" :disabled="submitting" @click="submit">提交反馈</button>
	</view>
</template>

<script setup>
	import {
		ref
	} from 'vue';
	import {
		runAction
	} from '@/common/actionFeedback';
	import {
		submitFeedback
	} from '@/common/api/feedback';
	import {
		chooseAndUploadImages
	} from '@/common/upload';

	const types = ['页面问题', '订单问题', '设备问题', '配送问题', '功能建议'];
	const currentType = ref('页面问题');
	const content = ref('');
	const contact = ref('13812348888');
	const images = ref([]);
	const submitting = ref(false);

	const chooseType = () => {
		uni.showActionSheet({
			itemList: types,
			success: ({
				tapIndex
			}) => {
				currentType.value = types[tapIndex] || currentType.value;
			}
		});
	};

	const chooseImage = async () => {
		try {
			uni.showLoading({
				title: '上传中'
			});
			const urls = await chooseAndUploadImages({
				count: 3 - images.value.length
			});
			images.value = [...images.value, ...urls].slice(0, 3);
		} catch (error) {
			uni.showToast({
				title: '图片上传失败',
				icon: 'none'
			});
		} finally {
			uni.hideLoading();
		}
	};

	const submit = async () => {
		if (submitting.value) return;
		submitting.value = true;
		const {
			ok
		} = await runAction({
			action: () => Promise.resolve(submitFeedback({
				type: currentType.value,
				content: content.value,
				contact: contact.value,
				imageUrls: images.value
			})),
			successText: '反馈已提交',
			errorText: '反馈提交失败，请稍后重试'
		});
		submitting.value = false;
		if (!ok) return;
		content.value = '';
		images.value = [];
	};
</script>

<style scoped>
	.page {
		box-sizing: border-box;
		min-height: 100vh;
		padding: 24rpx;
		background: #f5f7ff;
		overflow-x: hidden;
	}

	.card {
		box-sizing: border-box;
		width: 100%;
		padding: 28rpx;
		border-radius: 28rpx;
		background: #ffffff;
		overflow: hidden;
	}

	.card+.card {
		margin-top: 24rpx;
	}

	.section-title,
	.count,
	.upload-tip {
		display: block;
	}

	.section-title {
		font-size: 30rpx;
		font-weight: 700;
		color: #1f2937;
	}

	.content-title {
		margin-top: 28rpx;
	}

	.type-picker {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-top: 20rpx;
		padding: 0 20rpx;
		height: 76rpx;
		border-radius: 18rpx;
		background: #f5f7fb;
	}

	.type-value {
		display: block;
		font-size: 24rpx;
		color: #1f2937;
	}

	.textarea {
		display: block;
		height: 220rpx;
		margin-top: 20rpx;
		padding: 20rpx;
		border-radius: 18rpx;
		font-size: 24rpx;
		line-height: 1.7;
		color: #1f2937;
		background: #f5f7fb;
	}

	.count {
		margin-top: 10rpx;
		text-align: right;
		font-size: 20rpx;
		color: #a0a7b3;
	}

	.input {
		height: 76rpx;
		margin-top: 18rpx;
		padding: 0 20rpx;
		border-radius: 18rpx;
		font-size: 26rpx;
		color: #1f2937;
		background: #f5f7fb;
	}

	.upload-row {
		display: flex;
		flex-wrap: wrap;
		margin-top: 20rpx;
	}

	.upload-item,
	.upload-add {
		width: 180rpx;
		height: 180rpx;
		margin-right: 16rpx;
		margin-bottom: 16rpx;
		border-radius: 22rpx;
		overflow: hidden;
		background: #f8fafc;
	}

	.upload-image {
		width: 100%;
		height: 100%;
	}

	.upload-add {
		display: flex;
		align-items: center;
		justify-content: center;
		font-size: 24rpx;
		color: #6b7280;
	}

	.upload-tip {
		margin-top: 8rpx;
		font-size: 22rpx;
		color: #8a94a6;
	}

	.submit-btn {
		box-sizing: border-box;
		width: 100%;
		margin-top: 30rpx;
		height: 88rpx;
		line-height: 88rpx;
		border-radius: 18rpx;
		font-size: 30rpx;
		color: #ffffff;
		background: #7ed321;
	}
</style>