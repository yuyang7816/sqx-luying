<template>
	<view class="page">
		<view class="hero-card">
			<text class="hero-title">门店与履约设置</text>
			<text class="hero-desc">维护营业状态、自提与配送能力、配送费用规则。</text>
		</view>

		<view class="card">
			<text class="section-title">门店设置</text>
			<view class="field"><text class="label">门店名称</text><input v-model="form.name" class="input" placeholder="请输入门店名称" /></view>
			<view class="field"><text class="label">联系电话</text><input v-model="form.contact" class="input" type="number" placeholder="请输入门店联系电话" /></view>
			<view class="field"><text class="label">营业时间</text><input v-model="form.businessHours" class="input" placeholder="例如 09:00-21:00" /></view>
			<view class="field"><text class="label">门店地址</text><textarea v-model="form.address" class="textarea" maxlength="120" placeholder="请输入门店地址"></textarea></view>
			<view class="switch-row"><text class="switch-label">营业中</text><switch color="#7ed321" :checked="form.businessStatus" @change="form.businessStatus = $event.detail.value" /></view>
		</view>

		<view class="card">
			<text class="section-title">履约方式</text>
			<view class="switch-row"><text class="switch-label">支持门店自提</text><switch color="#7ed321" :checked="form.selfPickupEnabled" @change="form.selfPickupEnabled = $event.detail.value" /></view>
			<view class="switch-row"><text class="switch-label">支持同城配送</text><switch color="#7ed321" :checked="form.deliveryEnabled" @change="form.deliveryEnabled = $event.detail.value" /></view>
			<view class="field"><text class="label">配送范围</text><textarea v-model="form.serviceArea" class="textarea short" maxlength="80" placeholder="请输入支持配送的区域"></textarea></view>
			<text class="tip-text">{{ form.deliveryDesc }}</text>
		</view>

		<view class="card">
			<text class="section-title">配送规则</text>
			<view class="field"><text class="label">基础配送费</text><input v-model="form.baseDeliveryFee" class="input" type="digit" placeholder="请输入基础配送费" /></view>
			<view class="field"><text class="label">满额免配送</text><input v-model="form.freeDeliveryThreshold" class="input" type="digit" placeholder="仅按租金计，不含押金" /></view>
			<text class="tip-text">满额免配送仅按租金金额计算，不含押金。建议根据门店履约半径和同城配送成本定期调整费用规则。</text>
		</view>

		<button class="submit-btn" :loading="saving" :disabled="saving" @click="save">保存门店设置</button>
	</view>
</template>

<script setup>
	import { onMounted, reactive, ref } from 'vue';
	import { ensureLogin } from '@/common/auth';
	import { ensureMerchantOperational, fetchStoreDetail, saveStore } from '@/common/api/merchant';

	const form = reactive({
		id: undefined,
		name: '',
		address: '',
		contact: '',
		businessHours: '09:00-21:00',
		serviceArea: '',
		businessStatus: true,
		deliveryEnabled: false,
		selfPickupEnabled: true,
		baseDeliveryFee: '',
		freeDeliveryThreshold: '',
		deliveryDesc: '门店周边支持同城配送，超出区域需人工确认'
	});
	const saving = ref(false);

	const loadStore = async () => {
		if (!ensureLogin('/packageMerchant/store')) return;
		try {
			await ensureMerchantOperational({ redirect: true, toast: true });
			const data = await fetchStoreDetail();
			Object.assign(form, data);
		} catch (error) {
			uni.showToast({ title: error?.msg || error?.message || '加载门店失败', icon: 'none' });
		}
	};

	onMounted(loadStore);

	const save = async () => {
		if (!form.name?.trim()) {
			uni.showToast({ title: '请输入门店名称', icon: 'none' });
			return;
		}
		saving.value = true;
		try {
			await saveStore(form);
			uni.showToast({ title: '门店设置已保存', icon: 'success' });
		} catch (error) {
			uni.showToast({ title: error?.msg || error?.message || '保存失败', icon: 'none' });
		} finally {
			saving.value = false;
		}
	};
</script>

<style scoped>
	/* ========== 页面基底 ========== */
	.page {
		box-sizing: border-box;
		min-height: 100vh;
		padding: 28rpx;
		background: #f6f8f3;
		overflow-x: hidden;
	}

	/* ========== 顶部信息卡片 ========== */
	.hero-card {
		box-sizing: border-box;
		position: relative;
		padding: 44rpx 36rpx;
		border-radius: 36rpx;
		background: linear-gradient(160deg, #7ed321 0%, #6bc01a 50%, #5aad15 100%);
		overflow: hidden;
		box-shadow: 0 12rpx 40rpx rgba(126, 211, 33, 0.22);
	}

	/* 装饰性光晕 */
	.hero-card::before {
		content: '';
		position: absolute;
		top: -60rpx;
		right: -40rpx;
		width: 220rpx;
		height: 220rpx;
		border-radius: 50%;
		background: rgba(255, 255, 255, 0.1);
	}

	.hero-card::after {
		content: '';
		position: absolute;
		bottom: -40rpx;
		left: -30rpx;
		width: 160rpx;
		height: 160rpx;
		border-radius: 50%;
		background: rgba(255, 255, 255, 0.06);
	}

	.hero-title,
	.hero-desc,
	.section-title,
	.tip-text,
	.label,
	.switch-label {
		display: block;
		position: relative;
		z-index: 1;
	}

	.hero-title {
		font-size: 44rpx;
		font-weight: 800;
		color: #ffffff;
		letter-spacing: 2rpx;
		text-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.08);
	}

	.hero-desc {
		margin-top: 16rpx;
		font-size: 26rpx;
		line-height: 1.7;
		color: rgba(255, 255, 255, 0.95);
	}

	/* ========== 通用卡片 ========== */
	.card {
		box-sizing: border-box;
		width: 100%;
		margin-bottom: 28rpx;
		padding: 32rpx;
		border-radius: 32rpx;
		background: #ffffff;
		box-shadow: 0 6rpx 24rpx rgba(0, 0, 0, 0.04);
		overflow: hidden;
	}

	.section-title {
		font-size: 32rpx;
		font-weight: 800;
		color: #1a2114;
		letter-spacing: 1rpx;
		margin-bottom: 8rpx;
	}

	/* ========== 表单字段 ========== */
	.field + .field {
		margin-top: 28rpx;
	}

	.label {
		font-size: 24rpx;
		color: #6b7a5e;
		font-weight: 600;
		margin-bottom: 12rpx;
	}

	.input {
		box-sizing: border-box;
		height: 88rpx;
		padding: 0 28rpx;
		border-radius: 20rpx;
		font-size: 28rpx;
		color: #1a2114;
		background: #f6f8f3;
		font-weight: 500;
		border: 2rpx solid transparent;
		transition: all 0.2s ease;
	}

	.input:focus {
		background: #ffffff;
		border-color: rgba(126, 211, 33, 0.4);
		box-shadow: 0 0 0 4rpx rgba(126, 211, 33, 0.1);
	}

	.textarea {
		box-sizing: border-box;
		display: block;
		height: 180rpx;
		margin-top: 12rpx;
		padding: 24rpx 28rpx;
		border-radius: 20rpx;
		font-size: 26rpx;
		line-height: 1.7;
		color: #1a2114;
		background: #f6f8f3;
		font-weight: 500;
		border: 2rpx solid transparent;
		transition: all 0.2s ease;
	}

	.textarea:focus {
		background: #ffffff;
		border-color: rgba(126, 211, 33, 0.4);
		box-shadow: 0 0 0 4rpx rgba(126, 211, 33, 0.1);
	}

	.textarea.short {
		height: 140rpx;
	}

	/* ========== 开关行 ========== */
	.switch-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-top: 24rpx;
		padding-top: 20rpx;
		border-top: 1rpx solid #f1f4ec;
	}

	.switch-label {
		font-size: 26rpx;
		color: #1a2114;
		font-weight: 600;
	}

	/* ========== 提示文字 ========== */
	.tip-text {
		margin-top: 20rpx;
		font-size: 22rpx;
		line-height: 1.7;
		color: #8a9a7a;
		padding: 20rpx 24rpx;
		background: #f9fbf6;
		border-radius: 16rpx;
	}

	/* ========== 提交按钮 ========== */
	.submit-btn {
		box-sizing: border-box;
		width: 100%;
		margin-top: 12rpx;
		margin-bottom: 40rpx;
		height: 96rpx;
		line-height: 96rpx;
		border-radius: 48rpx;
		font-size: 32rpx;
		font-weight: 600;
		color: #ffffff;
		background: linear-gradient(135deg, #7ed321, #6bc01a);
		box-shadow: 0 8rpx 28rpx rgba(126, 211, 33, 0.35);
		border: none;
		transition: all 0.2s ease;
	}

	.submit-btn:active {
		transform: scale(0.97);
		box-shadow: 0 4rpx 16rpx rgba(126, 211, 33, 0.25);
	}
</style>
