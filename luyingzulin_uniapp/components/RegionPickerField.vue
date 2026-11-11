<template>
	<picker v-if="useNativePicker" mode="region" :value="nativeValue" @change="onNativeChange">
		<view class="picker-box" :class="{ placeholder: !displayText }">
			<text class="picker-text">{{ displayText || placeholder }}</text>
			<up-icon name="arrow-right" size="14" color="#aab592"></up-icon>
		</view>
	</picker>
	<view v-else class="region-picker-light" data-up-theme="light">
		<view class="picker-box" :class="{ placeholder: !displayText }" @click="openPicker">
			<text class="picker-text">{{ displayText || placeholder }}</text>
			<up-icon name="arrow-right" size="14" color="#aab592"></up-icon>
		</view>

		<up-picker
			ref="pickerRef"
			:show="pickerVisible"
			title="选择地区"
			keyName="name"
			:columns="pickerColumns"
			bgColor="#ffffff"
			:round="16"
			cancelColor="#6b7280"
			confirmColor="#7ed321"
			:maskStyle="pickerMaskStyle"
			@change="onPickerChange"
			@confirm="onPickerConfirm"
			@cancel="closePicker"
			@close="closePicker"
		/>
	</view>
</template>

<script setup>
	import { computed, nextTick, ref } from 'vue';
	import { buildRegionColumnsByChange, buildRegionPickerColumns, formatRegionText } from '@/common/regionPicker';

	const props = defineProps({
		province: { type: String, default: '' },
		city: { type: String, default: '' },
		district: { type: String, default: '' },
		placeholder: { type: String, default: '请选择省市区' }
	});

	const emit = defineEmits(['update:province', 'update:city', 'update:district', 'change']);

	// H5 原生 region 无数据，小程序/App 仍可用官方 picker
	// #ifdef H5
	const useNativePicker = false;
	// #endif
	// #ifndef H5
	const useNativePicker = true;
	// #endif

	const pickerMaskStyle =
		'background-image: linear-gradient(180deg, rgba(255,255,255,0.95), rgba(255,255,255,0.6)), linear-gradient(0deg, rgba(255,255,255,0.95), rgba(255,255,255,0.6));';

	const pickerRef = ref(null);
	const pickerVisible = ref(false);
	const pickerColumns = ref([[], [], []]);
	const pcasData = ref(null);

	const displayText = computed(() => formatRegionText(props.province, props.city, props.district));
	const nativeValue = computed(() => [props.province || '', props.city || '', props.district || '']);

	const emitRegion = (province, city, district) => {
		emit('update:province', province || '');
		emit('update:city', city || '');
		emit('update:district', district || '');
		emit('change', { province: province || '', city: city || '', district: district || '' });
	};

	const onNativeChange = (event) => {
		const [province, city, district] = event.detail?.value || [];
		emitRegion(province, city, district);
	};

	const openPicker = async () => {
		const state = await buildRegionPickerColumns({
			province: props.province,
			city: props.city,
			district: props.district
		});
		pcasData.value = state.data;
		pickerColumns.value = state.columns;
		pickerVisible.value = true;
		await nextTick();
		pickerRef.value?.setColumns(state.columns);
		pickerRef.value?.setIndexs(state.indexes, true);
	};

	const closePicker = () => {
		pickerVisible.value = false;
	};

	const onPickerChange = async (event) => {
		if (!pcasData.value || !pickerRef.value) return;
		const { columnIndex, indexs } = event;
		const result = await buildRegionColumnsByChange(pcasData.value, pickerColumns.value, columnIndex, indexs);
		if (columnIndex === 0 && result.cities) {
			pickerRef.value.setColumnValues(1, result.cities);
			pickerRef.value.setColumnValues(2, result.districts || []);
			return;
		}
		if (columnIndex === 1 && result.districts) {
			pickerRef.value.setColumnValues(2, result.districts);
		}
	};

	const onPickerConfirm = (event) => {
		const [province, city, district] = event.value || [];
		emitRegion(province?.name, city?.name, district?.name);
		closePicker();
	};
</script>

<style scoped>
	.picker-box {
		display: flex;
		align-items: center;
		justify-content: space-between;
		box-sizing: border-box;
		height: 88rpx;
		margin-top: 12rpx;
		padding: 0 28rpx;
		border-radius: 20rpx;
		background: #f6f8f3;
	}

	.picker-text {
		flex: 1;
		min-width: 0;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
		font-size: 28rpx;
		color: #1a2114;
		font-weight: 500;
	}

	.picker-box.placeholder .picker-text {
		color: #9aa38f;
		font-weight: 400;
	}

	.region-picker-light :deep(.u-popup__content) {
		background-color: #ffffff !important;
	}

	.region-picker-light :deep(.u-picker__view) {
		background-color: #ffffff;
	}

	.region-picker-light :deep(.u-picker__view__column__item),
	.region-picker-light :deep(.u-toolbar__title) {
		color: #1a2114;
	}

	/* #ifdef H5 */
	.region-picker-light :deep(.uni-picker-view-wrapper),
	.region-picker-light :deep(uni-picker-view),
	.region-picker-light :deep(.uni-picker-view-group) {
		background-color: #ffffff;
	}
	/* #endif */
</style>
