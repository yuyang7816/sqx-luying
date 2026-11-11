<template>
	<view class="page">
		<view class="card">
			<view class="field"><text class="label">收货人</text><input v-model="form.name" class="input" placeholder="请输入收货人" /></view>
			<view class="field"><text class="label">手机号</text><input v-model="form.phone" class="input" type="number" maxlength="11" placeholder="请输入手机号" /></view>
			<view class="field">
				<text class="label">所在地区</text>
				<RegionPickerField
					v-model:province="form.province"
					v-model:city="form.city"
					v-model:district="form.district"
				/>
			</view>
			<view class="field"><text class="label">详细地址</text><textarea v-model="form.detailAddress" class="textarea" maxlength="120" placeholder="请输入详细地址，例如街道、楼栋、门牌号"></textarea></view>
			<view class="switch-row"><text class="switch-label">设为默认地址</text><switch color="#7ed321" :checked="form.default" @change="form.default = $event.detail.value" /></view>
		</view>
		<button class="submit-btn" :loading="saving" :disabled="saving" @click="save">保存地址</button>
	</view>
</template>

<script setup>
	import { reactive, ref } from 'vue';
	import { onLoad, onShow } from '@dcloudio/uni-app';
	import RegionPickerField from '@/components/RegionPickerField.vue';
	import { runAction } from '@/common/actionFeedback';
	import { fetchAddressDetail, saveAddress } from '@/common/api/user';
	import { ensureLogin } from '@/common/auth';
	import { validateAddressSave } from '@/common/userValidators';

	const form = reactive({
		id: '',
		name: '',
		phone: '',
		province: '',
		city: '',
		district: '',
		detailAddress: '',
		default: false
	});
	const saving = ref(false);
	const editingId = ref('');

	const loadAddress = async (id) => {
		try {
			const currentAddress = await fetchAddressDetail(id);
			form.id = currentAddress.id;
			form.name = currentAddress.name;
			form.phone = String(currentAddress.phone || '').replace(/\*/g, '');
			form.province = currentAddress.province || '';
			form.city = currentAddress.city || '';
			form.district = currentAddress.district || '';
			form.detailAddress = currentAddress.detailAddress || currentAddress.address || '';
			form.default = currentAddress.default === true;
		} catch (error) {
			uni.showToast({ title: error.message || '地址加载失败', icon: 'none' });
		}
	};

	onLoad((options) => {
		editingId.value = options.id || '';
	});

	onShow(() => {
		const redirectUrl = editingId.value ? `/packageUser/address-edit?id=${editingId.value}` : '/packageUser/address-edit';
		if (!ensureLogin(redirectUrl)) return;
		if (editingId.value) {
			loadAddress(editingId.value);
		}
	});

	const save = async () => {
		if (saving.value) return;
		const validationMessage = validateAddressSave({
			name: form.name,
			phone: form.phone,
			province: form.province,
			city: form.city,
			district: form.district,
			detailAddress: form.detailAddress
		});
		if (validationMessage) {
			uni.showToast({ title: validationMessage, icon: 'none' });
			return;
		}
		saving.value = true;
		const { ok } = await runAction({
			action: () => saveAddress(form),
			successText: '地址已保存',
			errorText: '地址保存失败，请稍后重试'
		});
		saving.value = false;
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
		padding: 28rpx;
		background: #f6f8f3;
		overflow-x: hidden;
	}

	.card {
		box-sizing: border-box;
		width: 100%;
		padding: 32rpx;
		background: #ffffff;
		border-radius: 32rpx;
		box-shadow: 0 6rpx 24rpx rgba(0, 0, 0, 0.04);
		overflow: hidden;
	}

	.field + .field {
		margin-top: 28rpx;
	}

	.label,
	.switch-label {
		display: block;
		font-size: 24rpx;
		color: #6b7a5e;
		font-weight: 600;
		margin-bottom: 12rpx;
	}

	.input {
		height: 88rpx;
		line-height: 88rpx;
		margin-top: 12rpx;
		padding: 0 28rpx;
		border-radius: 20rpx;
		background: #f6f8f3;
		font-size: 28rpx;
		color: #1a2114;
		font-weight: 500;
	}

	.textarea {
		display: block;
		height: 180rpx;
		margin-top: 12rpx;
		padding: 24rpx 28rpx;
		border-radius: 20rpx;
		background: #f6f8f3;
		font-size: 26rpx;
		line-height: 1.7;
		color: #1a2114;
		font-weight: 500;
	}

	.switch-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-top: 28rpx;
		padding-top: 24rpx;
		border-top: 1rpx solid #f1f4ec;
	}

	.submit-btn {
		box-sizing: border-box;
		width: 100%;
		margin-top: 32rpx;
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
	}
</style>
