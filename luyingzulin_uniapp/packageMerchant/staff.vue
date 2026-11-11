<template>
	<view class="page">
		<view class="hero-card">
			<text class="hero-title">员工管理</text>
			<text class="hero-desc">维护核销联系人，用于取货凭证展示与门店核销。</text>
		</view>

		<view class="card">
			<view class="section-head">
				<text class="section-title">核销员工</text>
				<text class="section-link" @click="openStaffForm()">新增员工</text>
			</view>

			<view v-if="!staffList.length" class="empty-box">
				<up-icon name="account" size="36" color="#b0b8a8"></up-icon>
				<text class="empty-text">暂无员工，请添加核销联系人</text>
			</view>

			<view v-for="item in staffList" :key="item.id || item.name + item.role" class="staff-row">
				<view class="staff-main">
					<text class="staff-name">{{ item.name }}</text>
					<text class="staff-meta">{{ item.role }} · {{ item.phone }}</text>
					<text v-if="item.status === 0" class="staff-status">已停用</text>
				</view>
				<view class="staff-actions">
					<text class="staff-action" @click="editStaff(item)">编辑</text>
					<text class="staff-action danger" @click="removeStaff(item)">删除</text>
				</view>
			</view>
		</view>

		<view v-if="staffModal.visible" class="staff-modal-mask" @click="closeStaffForm">
			<view class="staff-modal" @click.stop>
				<text class="modal-title">{{ staffModal.form.id ? '编辑员工' : '新增员工' }}</text>
				<view class="field"><text class="label">姓名</text><input v-model="staffModal.form.name" class="input" placeholder="请输入姓名" /></view>
				<view class="field"><text class="label">岗位</text><input v-model="staffModal.form.role" class="input" placeholder="如：核销员" /></view>
				<view class="field"><text class="label">手机号</text><input v-model="staffModal.form.phone" class="input" type="number" placeholder="请输入手机号" /></view>
				<view class="switch-row"><text class="switch-label">启用</text><switch color="#7ed321" :checked="staffModal.form.status === 1" @change="staffModal.form.status = $event.detail.value ? 1 : 0" /></view>
				<view class="modal-actions">
					<button class="modal-btn cancel" @click="closeStaffForm">取消</button>
					<button class="modal-btn confirm" :loading="staffModal.saving" @click="submitStaff">保存</button>
				</view>
			</view>
		</view>
	</view>
</template>

<script setup>
	import { reactive, ref } from 'vue';
	import { onShow } from '@dcloudio/uni-app';
	import { ensureLogin } from '@/common/auth';
	import {
		deleteStaff,
		ensureMerchantOperational,
		fetchStaffList,
		fetchStoreDetail,
		guardMerchantOperational,
		saveStaff
	} from '@/common/api/merchant';

	const storeId = ref(undefined);
	const staffList = ref([]);
	const staffModal = reactive({
		visible: false,
		saving: false,
		form: { id: undefined, storeId: undefined, name: '', role: '核销员', phone: '', status: 1 }
	});

	const guardOperational = async () => {
		return guardMerchantOperational({ toast: true });
	};

	const loadStaffList = async () => {
		try {
			staffList.value = await fetchStaffList();
		} catch {
			staffList.value = [];
		}
	};

	const loadPage = async () => {
		if (!ensureLogin('/packageMerchant/staff')) return;
		try {
			await ensureMerchantOperational({ redirect: true, toast: true });
			const store = await fetchStoreDetail();
			storeId.value = store?.id;
			await loadStaffList();
		} catch (error) {
			uni.showToast({ title: error?.msg || error?.message || '加载失败', icon: 'none' });
		}
	};

	onShow(loadPage);

	const openStaffForm = (item) => {
		staffModal.form = {
			id: item?.id,
			storeId: storeId.value,
			name: item?.name || '',
			role: item?.role || '核销员',
			phone: item?.phone || '',
			status: item?.status ?? 1
		};
		staffModal.visible = true;
	};

	const closeStaffForm = () => {
		staffModal.visible = false;
	};

	const editStaff = (item) => openStaffForm(item);

	const submitStaff = async () => {
		if (!(await guardOperational())) return;
		if (!staffModal.form.name?.trim()) {
			uni.showToast({ title: '请输入员工姓名', icon: 'none' });
			return;
		}
		if (!staffModal.form.role?.trim()) {
			uni.showToast({ title: '请输入岗位', icon: 'none' });
			return;
		}
		if (!staffModal.form.phone?.trim()) {
			uni.showToast({ title: '请输入手机号', icon: 'none' });
			return;
		}
		staffModal.saving = true;
		try {
			await saveStaff({ ...staffModal.form, storeId: storeId.value });
			uni.showToast({ title: '员工已保存', icon: 'success' });
			closeStaffForm();
			await loadStaffList();
		} catch (error) {
			uni.showToast({ title: error?.msg || error?.message || '保存失败', icon: 'none' });
		} finally {
			staffModal.saving = false;
		}
	};

	const removeStaff = (item) => {
		if (!item?.id) return;
		uni.showModal({
			title: '删除员工',
			content: `确认删除 ${item.name}？删除后该员工将无法参与核销。`,
			success: async (res) => {
				if (!res.confirm) return;
				if (!(await guardOperational())) return;
				try {
					await deleteStaff(item.id);
					uni.showToast({ title: '已删除', icon: 'success' });
					await loadStaffList();
				} catch (error) {
					uni.showToast({ title: error?.msg || error?.message || '删除失败', icon: 'none' });
				}
			}
		});
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

	.hero-card {
		padding: 36rpx 32rpx;
		border-radius: 32rpx;
		background: linear-gradient(160deg, #7ed321 0%, #6bc01a 50%, #5aad15 100%);
		box-shadow: 0 12rpx 40rpx rgba(126, 211, 33, 0.22);
	}

	.hero-title,
	.hero-desc,
	.section-title,
	.empty-text,
	.label,
	.switch-label,
	.staff-name,
	.staff-meta {
		display: block;
	}

	.hero-title {
		font-size: 40rpx;
		font-weight: 800;
		color: #ffffff;
	}

	.hero-desc {
		margin-top: 14rpx;
		font-size: 24rpx;
		line-height: 1.7;
		color: rgba(255, 255, 255, 0.94);
	}

	.card {
		margin-top: 24rpx;
		padding: 28rpx;
		border-radius: 28rpx;
		background: #ffffff;
		box-shadow: 0 6rpx 24rpx rgba(0, 0, 0, 0.04);
	}

	.section-head {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-bottom: 8rpx;
	}

	.section-title {
		font-size: 30rpx;
		font-weight: 700;
		color: #1a2114;
	}

	.section-link {
		font-size: 24rpx;
		color: #59b50e;
	}

	.empty-box {
		display: flex;
		flex-direction: column;
		align-items: center;
		padding: 48rpx 0 24rpx;
	}

	.empty-text {
		margin-top: 16rpx;
		font-size: 24rpx;
		color: #8a9a7a;
	}

	.staff-row {
		display: flex;
		justify-content: space-between;
		align-items: center;
		margin-top: 20rpx;
		padding: 24rpx;
		border-radius: 20rpx;
		background: #f9fbf6;
	}

	.staff-main {
		flex: 1;
		min-width: 0;
	}

	.staff-name {
		font-size: 28rpx;
		font-weight: 700;
		color: #1a2114;
	}

	.staff-meta {
		margin-top: 8rpx;
		font-size: 22rpx;
		color: #8a9a7a;
	}

	.staff-status {
		display: inline-block;
		margin-top: 8rpx;
		padding: 4rpx 12rpx;
		border-radius: 12rpx;
		font-size: 20rpx;
		color: #b45309;
		background: #fff4d6;
	}

	.staff-actions {
		display: flex;
		align-items: center;
		flex-shrink: 0;
	}

	.staff-action {
		padding: 10rpx 24rpx;
		border-radius: 20rpx;
		font-size: 24rpx;
		color: #5cb816;
		background: #f6ffea;
		font-weight: 600;
		margin-left: 16rpx;
	}

	.staff-action.danger {
		color: #d94b4b;
		background: #fff1f1;
	}

	.field + .field,
	.switch-row {
		margin-top: 24rpx;
	}

	.label {
		margin-bottom: 12rpx;
		font-size: 24rpx;
		color: #6b7a5e;
		font-weight: 600;
	}

	.input {
		box-sizing: border-box;
		width: 100%;
		height: 88rpx;
		padding: 0 28rpx;
		border-radius: 20rpx;
		font-size: 28rpx;
		color: #1a2114;
		background: #f6f8f3;
	}

	.switch-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	.switch-label {
		font-size: 26rpx;
		color: #1a2114;
		font-weight: 600;
	}

	.staff-modal-mask {
		position: fixed;
		inset: 0;
		background: rgba(0, 0, 0, 0.45);
		display: flex;
		align-items: center;
		justify-content: center;
		z-index: 1000;
		padding: 40rpx;
	}

	.staff-modal {
		width: 100%;
		max-width: 640rpx;
		padding: 36rpx;
		border-radius: 28rpx;
		background: #ffffff;
	}

	.modal-title {
		display: block;
		margin-bottom: 24rpx;
		font-size: 34rpx;
		font-weight: 700;
		color: #1a2114;
	}

	.modal-actions {
		display: flex;
		gap: 20rpx;
		margin-top: 28rpx;
	}

	.modal-btn {
		flex: 1;
		height: 80rpx;
		line-height: 80rpx;
		border-radius: 20rpx;
		font-size: 28rpx;
	}

	.modal-btn.cancel {
		color: #71806a;
		background: #f3f6ef;
	}

	.modal-btn.confirm {
		color: #ffffff;
		background: linear-gradient(135deg, #7ed321, #6bc01a);
	}
</style>
