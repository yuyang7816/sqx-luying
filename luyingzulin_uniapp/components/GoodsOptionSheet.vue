<template>
	<u-popup :show="visible" mode="bottom" round="16" safeAreaInsetBottom @close="handleClose">
		<view class="sheet">
			<view class="sheet-head">
				<text class="sheet-title">选择规格与配件</text>
				<text class="sheet-close" @click="handleClose">关闭</text>
			</view>

			<view class="sheet-goods">
				<image v-if="coverImage" class="sheet-cover" :src="coverImage" mode="aspectFill" />
				<view class="sheet-goods-info">
					<text class="sheet-goods-name">{{ goods?.name || '租赁商品' }}</text>
					<view class="sheet-price-row">
						<text class="sheet-price-symbol">¥</text>
						<text class="sheet-price">{{ previewPrice }}</text>
						<text class="sheet-price-unit">/{{ priceUnit }}</text>
					</view>
					<text v-if="selectedSummary" class="sheet-selected">{{ selectedSummary }}</text>
				</view>
			</view>

			<scroll-view scroll-y class="sheet-body" :show-scrollbar="false">
				<view v-if="specOptions.length" class="sheet-section">
					<text class="section-label">租赁规格</text>
					<view class="choice-grid">
						<view
							v-for="item in specOptions"
							:key="item.id || item.index"
							class="choice-card"
							:class="{ active: draftSpecIndex === item.index }"
							@click="draftSpecIndex = item.index"
						>
							<text class="choice-title">{{ item.label }}</text>
							<text v-if="Number(item.price || 0) > 0" class="choice-extra">+¥{{ Number(item.price).toFixed(2) }}</text>
							<text v-else class="choice-desc">标准规格</text>
						</view>
					</view>
				</view>

				<view v-if="packageOptions.length" class="sheet-section">
					<text class="section-label">配件套餐</text>
					<view class="choice-grid package-grid">
						<view
							v-for="item in packageOptions"
							:key="item.id || item.index"
							class="choice-card package-card"
							:class="{ active: draftPackageIndex === item.index, disabled: specRequired && !draftSpec?.id }"
							@click="handlePackagePick(item)"
						>
							<view v-if="draftPackageIndex === item.index" class="package-check">
								<up-icon name="checkmark" size="11" color="#ffffff"></up-icon>
							</view>
							<text class="choice-title">{{ item.label }}</text>
							<text class="choice-desc">{{ item.desc || '标准搭配' }}</text>
							<text v-if="Number(item.price || 0) > 0" class="choice-extra">+¥{{ Number(item.price).toFixed(2) }}</text>
						</view>
					</view>
					<text v-if="draftPackageDesc" class="package-tip">{{ draftPackageDesc }}</text>
				</view>
			</scroll-view>

			<button class="confirm-btn" :disabled="!canConfirm" @click="handleConfirm">
				{{ actionLabel }}
			</button>
		</view>
	</u-popup>
</template>

<script setup>
	import { computed, ref, watch } from 'vue';
	import {
		buildRentSpecOptions,
		calculateDetailTotalPrice,
		getRentPriceUnit,
		goodsRequiresSpecSelection,
		resolveSelectedRentSpec
	} from '@/common/rental/constants';

	const props = defineProps({
		visible: { type: Boolean, default: false },
		goods: { type: Object, default: null },
		rentMode: { type: String, default: 'day' },
		specIndex: { type: Number, default: 0 },
		packageIndex: { type: Number, default: 0 },
		action: { type: String, default: 'cart' }
	});

	const emit = defineEmits(['close', 'confirm']);

	const draftSpecIndex = ref(0);
	const draftPackageIndex = ref(0);

	const specOptions = computed(() => buildRentSpecOptions(props.goods));
	const packageOptions = computed(() => {
		const list = props.goods?.packages || [];
		return list.map((item, index) => ({
			index,
			id: item.id,
			label: item.packageName,
			desc: item.packageDesc,
			price: item.price
		}));
	});

	const specRequired = computed(() => goodsRequiresSpecSelection(props.goods));

	const draftSpec = computed(() => resolveSelectedRentSpec(props.goods, draftSpecIndex.value));
	const draftPackage = computed(() => packageOptions.value[draftPackageIndex.value] || null);
	const draftPackageDesc = computed(() => draftPackage.value?.desc || '');

	const coverImage = computed(() => props.goods?.coverImage || props.goods?.images?.[0] || '');

	const priceUnit = computed(() => {
		const map = { day: 'day', hour: 'hour', night: 'overnight' };
		return getRentPriceUnit(map[props.rentMode] || 'day');
	});

	const previewPrice = computed(() => {
		if (!props.goods) return '0';
		return calculateDetailTotalPrice(props.goods, props.rentMode, draftPackage.value, draftSpec.value);
	});

	const selectedSummary = computed(() => {
		const parts = [];
		if (specOptions.value.length && draftSpec.value?.label) {
			parts.push(draftSpec.value.label);
		}
		if (packageOptions.value.length && draftPackage.value?.label) {
			parts.push(draftPackage.value.label);
		}
		return parts.join(' · ');
	});

	const canConfirm = computed(() => {
		if (specRequired.value && !draftSpec.value?.id && specOptions.value.length) return false;
		if (packageOptions.value.length && !draftPackage.value) return false;
		return true;
	});

	const actionLabel = computed(() => (props.action === 'buy' ? '立即预订' : '加入租物车'));

	const syncDraft = () => {
		draftSpecIndex.value = props.specIndex;
		draftPackageIndex.value = props.packageIndex;
	};

	watch(() => props.visible, (value) => {
		if (value) syncDraft();
	});

	const handleClose = () => emit('close');

	const handlePackagePick = (item) => {
		if (specRequired.value && !draftSpec.value?.id) {
			uni.showToast({ title: '请先选择规格', icon: 'none' });
			return;
		}
		draftPackageIndex.value = item.index;
	};

	const handleConfirm = () => {
		if (!canConfirm.value) {
			if (specRequired.value) {
				uni.showToast({ title: '请选择商品规格', icon: 'none' });
			} else if (packageOptions.value.length) {
				uni.showToast({ title: '请选择配件套餐', icon: 'none' });
			}
			return;
		}
		emit('confirm', {
			specIndex: draftSpecIndex.value,
			packageIndex: draftPackageIndex.value,
			spec: draftSpec.value,
			packageItem: draftPackage.value
		});
	};
</script>

<style scoped>
	.sheet {
		max-height: 82vh;
		padding: 28rpx 28rpx calc(24rpx + env(safe-area-inset-bottom));
		background: #ffffff;
		border-radius: 24rpx 24rpx 0 0;
	}

	.sheet-head {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-bottom: 20rpx;
	}

	.sheet-title {
		font-size: 32rpx;
		font-weight: 700;
		color: #1a2114;
	}

	.sheet-close {
		font-size: 26rpx;
		color: #8a9a7a;
	}

	.sheet-goods {
		display: flex;
		gap: 20rpx;
		padding-bottom: 24rpx;
		border-bottom: 1rpx solid #f1f4ec;
	}

	.sheet-cover {
		width: 160rpx;
		height: 160rpx;
		border-radius: 16rpx;
		flex-shrink: 0;
		background: #f5f7f2;
	}

	.sheet-goods-info {
		flex: 1;
		min-width: 0;
	}

	.sheet-goods-name {
		display: block;
		font-size: 28rpx;
		font-weight: 700;
		color: #1a2114;
		line-height: 1.4;
	}

	.sheet-price-row {
		display: flex;
		align-items: flex-end;
		gap: 4rpx;
		margin-top: 12rpx;
	}

	.sheet-price-symbol {
		font-size: 24rpx;
		font-weight: 700;
		color: #5fa317;
	}

	.sheet-price {
		font-size: 44rpx;
		font-weight: 800;
		line-height: 1;
		color: #63aa17;
	}

	.sheet-price-unit {
		font-size: 22rpx;
		color: #76925c;
		margin-bottom: 4rpx;
	}

	.sheet-selected {
		display: block;
		margin-top: 10rpx;
		font-size: 22rpx;
		color: #8a9a7a;
	}

	.sheet-body {
		max-height: 46vh;
		margin-top: 20rpx;
	}

	.sheet-section {
		margin-bottom: 28rpx;
	}

	.section-label {
		display: block;
		font-size: 28rpx;
		font-weight: 700;
		color: #1a2114;
		margin-bottom: 16rpx;
	}

	.choice-grid {
		display: flex;
		flex-wrap: wrap;
		gap: 14rpx;
	}

	.package-grid .choice-card {
		position: relative;
		width: calc((100% - 14rpx) / 2);
		min-height: 128rpx;
		padding: 20rpx 16rpx 18rpx;
		overflow: hidden;
	}

	.choice-card {
		box-sizing: border-box;
		width: calc((100% - 28rpx) / 3);
		min-height: 112rpx;
		padding: 16rpx 12rpx;
		border-radius: 18rpx;
		background: #fbfef4;
		border: 2rpx solid #e6edd8;
		transition: border-color 0.2s ease, background 0.2s ease, box-shadow 0.2s ease;
	}

	.choice-card.active {
		background: linear-gradient(180deg, #f6fce7 0%, #ecf8d1 100%);
		border-color: #7ed321;
		box-shadow: 0 8rpx 20rpx rgba(126, 211, 33, 0.18), inset 0 0 0 1rpx rgba(126, 211, 33, 0.12);
	}

	.package-card.active {
		background: linear-gradient(180deg, #f3fbe4 0%, #e3f5c4 100%);
		border-color: #6bc01a;
		box-shadow: 0 10rpx 24rpx rgba(107, 192, 26, 0.22), inset 0 0 0 1rpx rgba(107, 192, 26, 0.18);
	}

	.package-card.active .choice-title {
		color: #3d7a0f;
	}

	.package-card.active .choice-desc {
		color: #5a8f2e;
	}

	.package-card.active .choice-extra {
		color: #4a9410;
	}

	.package-check {
		position: absolute;
		top: 0;
		right: 0;
		display: flex;
		align-items: center;
		justify-content: center;
		width: 40rpx;
		height: 36rpx;
		border-radius: 0 16rpx 0 16rpx;
		background: linear-gradient(135deg, #7ed321, #6bc01a);
		box-shadow: 0 4rpx 10rpx rgba(107, 192, 26, 0.28);
	}

	.choice-card.disabled {
		opacity: 0.45;
	}

	.choice-title {
		display: block;
		font-size: 24rpx;
		font-weight: 700;
		color: #22301a;
	}

	.choice-desc {
		display: block;
		margin-top: 6rpx;
		font-size: 20rpx;
		color: #8d9d7f;
		line-height: 1.35;
	}

	.choice-extra {
		display: block;
		margin-top: 8rpx;
		font-size: 20rpx;
		font-weight: 700;
		color: #5da316;
	}

	.package-tip {
		display: block;
		margin-top: 12rpx;
		padding: 12rpx 16rpx;
		border-radius: 14rpx;
		background: #f6fbe9;
		font-size: 22rpx;
		color: #6f8552;
		line-height: 1.5;
	}

	.confirm-btn {
		width: 100%;
		height: 88rpx;
		line-height: 88rpx;
		margin-top: 20rpx;
		border-radius: 44rpx;
		font-size: 30rpx;
		font-weight: 700;
		color: #ffffff;
		background: linear-gradient(135deg, #7ed321, #6bc01a);
		border: none;
	}

	.confirm-btn[disabled] {
		color: #9aa2b1;
		background: #e5e7eb;
	}
</style>
