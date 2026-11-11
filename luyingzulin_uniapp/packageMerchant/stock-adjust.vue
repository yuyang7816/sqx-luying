<template>
	<view v-if="goods" class="page">
		<view class="hero-card">
			<text class="hero-title">库存调整</text>
			<text class="hero-desc">用于补货、锁定库存、下架前清仓或同步门店实际可租库存。</text>
		</view>

		<view class="card">
			<text class="section-title">商品信息</text>
			<view class="line"><text class="line-label">商品名称</text><text class="line-value">{{ goods.name }}</text></view>
			<view class="line"><text class="line-label">商品编号</text><text class="line-value">{{ goods.id }}</text></view>
			<view class="line"><text class="line-label">当前库存</text><text class="line-value">{{ goods.stock }}</text></view>
		</view>

		<view class="card">
			<text class="section-title">调整方式</text>
			<view class="tab-wrap">
				<u-tabs :list="actions" keyName="label" :current="currentTabIndex" :scrollable="true" lineColor="#7ed321" :activeStyle="tabActiveStyle" :inactiveStyle="tabInactiveStyle" @change="handleActionChange"></u-tabs>
			</view>
			<view class="stepper-row">
				<view class="step-btn" @click="changeCount(-1)"><up-icon name="minus" size="18" color="#1a2114"></up-icon></view>
				<text class="step-value">{{ count }}</text>
				<view class="step-btn" @click="changeCount(1)"><up-icon name="plus" size="18" color="#1a2114"></up-icon></view>
			</view>
			<textarea v-model="remark" class="textarea" maxlength="100" placeholder="请输入调整原因，例如补货、损耗、预留库存"></textarea>
		</view>

		<view class="card">
			<text class="section-title">调整结果</text>
			<view class="line"><text class="line-label">调整方向</text><text class="line-value">{{ currentActionLabel }}</text></view>
			<view class="line"><text class="line-label">调整数量</text><text class="line-value">{{ count }}</text></view>
			<view class="line"><text class="line-label">调整后库存</text><text class="line-value">{{ nextStock }}</text></view>
		</view>

		<button class="submit-btn" :loading="submitting" :disabled="submitting" @click="submit">确认调整</button>
	</view>
</template>

<script setup>
	import { computed, ref } from 'vue';
	import { onLoad } from '@dcloudio/uni-app';
	import { ensureLogin } from '@/common/auth';
	import { ensureMerchantOperational, adjustGoodsStock, fetchGoodsDetail } from '@/common/api/merchant';

	const goods = ref(null);
	const goodsId = ref('');
	const actions = [{ key: 'add', label: '补充库存' }, { key: 'reduce', label: '扣减库存' }];
	const actionType = ref('add');
	const tabActiveStyle = { color: '#59b50e', fontWeight: '700' };
	const tabInactiveStyle = { color: '#6b7280' };
	const currentTabIndex = computed(() => Math.max(actions.findIndex((item) => item.key === actionType.value), 0));
	const count = ref(1);
	const remark = ref('');
	const submitting = ref(false);

	onLoad(async (options) => {
		if (!ensureLogin('/packageMerchant/stock-adjust')) return;
		try {
			await ensureMerchantOperational({ redirect: true, toast: true });
		} catch {
			return;
		}
		goodsId.value = options.id || '';
		try {
			const { view } = await fetchGoodsDetail(goodsId.value);
			goods.value = view;
		} catch (error) {
			uni.showToast({ title: error?.msg || error?.message || '加载商品失败', icon: 'none' });
		}
	});

	const currentActionLabel = computed(() => actions.find((item) => item.key === actionType.value)?.label || '补充库存');
	const nextStock = computed(() => {
		if (!goods.value) return 0;
		const current = Number(goods.value.stock);
		if (actionType.value === 'add') return current + count.value;
		return Math.max(0, current - count.value);
	});

	const changeCount = (step) => {
		count.value = Math.max(1, count.value + step);
	};

	const handleActionChange = (item) => {
		actionType.value = item.key;
	};

	const submit = async () => {
		if (!goods.value) return;
		submitting.value = true;
		try {
			await adjustGoodsStock({
				goodsId: goodsId.value,
				adjustType: actionType.value === 'add' ? 'in' : 'out',
				quantity: count.value,
				remark: remark.value
			});
			uni.showToast({ title: '库存调整已记录', icon: 'success' });
			setTimeout(() => uni.navigateBack(), 400);
		} catch (error) {
			uni.showToast({ title: error?.msg || error?.message || '调整失败', icon: 'none' });
		} finally {
			submitting.value = false;
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
		width: 100%;
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
	.section-title {
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
		margin-top: 28rpx;
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

	/* ========== 信息行 ========== */
	.line {
		display: flex;
		justify-content: space-between;
		align-items: flex-start;
		margin-top: 20rpx;
		font-size: 26rpx;
		line-height: 1.6;
	}

	.line-label,
	.line-value {
		display: block;
	}

	.line-label {
		width: 140rpx;
		flex-shrink: 0;
		color: #8a9a7a;
		font-weight: 500;
	}

	.line-value {
		flex: 1;
		min-width: 0;
		text-align: right;
		word-break: break-all;
		color: #1a2114;
		font-weight: 600;
	}

	/* ========== 调整方式标签 ========== */
	.tab-wrap {
		margin-top: 20rpx;
	}

	.tab-wrap :deep(.u-tabs__wrapper__nav__item) {
		padding: 0 24rpx;
	}

	.tab-wrap :deep(.u-tabs__wrapper__nav__item__text) {
		font-size: 26rpx;
		white-space: nowrap;
	}

	/* ========== 步进器 ========== */
	.stepper-row {
		display: flex;
		align-items: center;
		justify-content: center;
		gap: 20rpx;
		margin-top: 28rpx;
	}

	.step-btn {
		display: flex;
		align-items: center;
		justify-content: center;
		width: 88rpx;
		height: 88rpx;
		border-radius: 50%;
		background: #f1f4ec;
		transition: all 0.15s ease;
	}

	.step-btn:active {
		transform: scale(0.9);
		background: #e4e9dd;
	}

	.step-value {
		min-width: 0;
		width: 220rpx;
		height: 88rpx;
		line-height: 88rpx;
		text-align: center;
		font-size: 36rpx;
		font-weight: 800;
		color: #1a2114;
		background: #fff8e6;
		border-radius: 24rpx;
		letter-spacing: -1rpx;
	}

	/* ========== 备注文本域 ========== */
	.textarea {
		box-sizing: border-box;
		display: block;
		height: 160rpx;
		margin-top: 28rpx;
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

	/* ========== 提交按钮 ========== */
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
		transition: all 0.2s ease;
	}

	.submit-btn:active {
		transform: scale(0.97);
		box-shadow: 0 4rpx 16rpx rgba(126, 211, 33, 0.25);
	}
</style>
