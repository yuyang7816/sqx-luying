<template>
	<view class="page">
		<!-- 顶部引导 -->
		<view class="hero">
			<view class="hero-top">
				<text class="hero-badge">{{ isCreate ? '新建商品' : '编辑商品' }}</text>
				<text v-if="!isCreate && goods.id" class="hero-id">#{{ goods.id }}</text>
			</view>
			<text class="hero-title">{{ isCreate ? '发布租赁装备' : form.name || '编辑商品信息' }}</text>
			<text class="hero-sub">{{ isCreate ? '完善信息后提交，商品将自动上架展示' : '保存修改后立即对用户生效' }}</text>
		</view>

		<!-- 进度指示 -->
		<view class="step-track">
			<view class="step-item active">
				<view class="step-dot">1</view>
				<text class="step-label">基础</text>
			</view>
			<view class="step-line" />
			<view class="step-item active">
				<view class="step-dot">2</view>
				<text class="step-label">定价</text>
			</view>
			<view class="step-line" />
			<view class="step-item">
				<view class="step-dot">3</view>
				<text class="step-label">扩展</text>
			</view>
		</view>

		<!-- 01 基础信息 -->
		<view class="card">
			<view class="card-head">
				<view class="card-index">01</view>
				<view class="card-head-text">
					<text class="card-title">基础信息</text>
					<text class="card-desc">名称、分类与商品展示图</text>
				</view>
			</view>

			<view class="field">
				<text class="field-label required">商品名称</text>
				<view class="field-box">
					<input v-model="form.name" class="field-input" placeholder="如：双人自动充气帐篷" maxlength="40" />
				</view>
			</view>

			<view class="field-row" :class="{ single: !showSubCategory }">
				<view class="field half">
					<text class="field-label required">装备大类</text>
					<picker mode="selector" :range="rootCategoryLabels" :value="rootIndex" @change="onRootCategoryChange">
						<view class="field-box picker" :class="{ empty: !form.rootCategoryId }">
							<text class="picker-val">{{ form.rootCategoryName || '请选择大类' }}</text>
							<up-icon name="arrow-down" size="14" color="#b0b8a8" />
						</view>
					</picker>
				</view>
				<view v-if="showSubCategory" class="field half">
					<text class="field-label required">细分品类</text>
					<picker mode="selector" :range="subCategoryLabels" :value="subIndex" @change="onSubCategoryChange">
						<view class="field-box picker" :class="{ empty: !form.subCategoryId }">
							<text class="picker-val">{{ form.subCategoryName || '请选择品类' }}</text>
							<up-icon name="arrow-down" size="14" color="#b0b8a8" />
						</view>
					</picker>
				</view>
			</view>

			<view class="field">
				<text class="field-label">商品描述</text>
				<view class="field-box textarea-box">
					<textarea
						v-model="form.desc"
						class="field-textarea"
						maxlength="500"
						placeholder="补充卖点、配件清单、使用注意事项等"
					/>
				</view>
			</view>

			<view class="field">
				<view class="field-label-row">
					<text class="field-label required">商品图片</text>
					<text class="field-tip">1-6 张 · 首张为封面</text>
				</view>
				<view class="img-grid">
					<view v-for="(item, index) in images" :key="item + index" class="img-cell">
						<image class="img-thumb" :src="resolveImageUrl(item)" mode="aspectFill" />
						<view class="img-del" @click.stop="removeImage(index)">
							<up-icon name="close" size="12" color="#fff" />
						</view>
						<text v-if="index === 0" class="img-cover-mini">封面</text>
					</view>
					<view v-if="images.length < 6" class="img-cell add" @click="addImage">
						<up-icon name="camera" size="24" color="#59b50e" />
						<text class="add-label">上传</text>
					</view>
				</view>
			</view>

			<view class="field">
				<view class="field-label-row">
					<text class="field-label">使用教程视频</text>
					<text class="field-tip">选填 · 核销后 IM 发送</text>
				</view>
				<view v-if="form.tutorialVideoUrl" class="video-preview">
					<video
						class="video-player"
						:src="resolveImageUrl(form.tutorialVideoUrl)"
						controls
						object-fit="contain"
					/>
					<view class="video-actions">
						<text class="video-action" @click="uploadTutorialVideo">重新上传</text>
						<text class="video-action danger" @click="removeTutorialVideo">移除</text>
					</view>
				</view>
				<view v-else class="video-upload" @click="uploadTutorialVideo">
					<up-icon name="play-circle" size="32" color="#59b50e" />
					<text class="video-upload-label">上传教程视频</text>
					<text class="video-upload-tip">mp4 等格式，不超过 50MB</text>
				</view>
			</view>
		</view>

		<!-- 02 租赁定价 -->
		<view class="card">
			<view class="card-head">
				<view class="card-index">02</view>
				<view class="card-head-text">
					<text class="card-title">租赁定价</text>
					<text class="card-desc">整日租为必填，可按需开启其他租期</text>
				</view>
			</view>

			<view class="rent-card main">
				<view class="rent-head">
					<view>
						<text class="rent-name required">整日租</text>
						<text class="rent-tag">24 小时</text>
					</view>
				</view>
				<view class="rent-price-row">
					<text class="rent-currency">¥</text>
					<input v-model="form.price" class="rent-price-input" type="digit" placeholder="0.00" />
					<text class="rent-unit">/ 天</text>
				</view>
			</view>

			<view class="rent-card" :class="{ dim: !form.rentModes.hour }">
				<view class="rent-head">
					<view>
						<text class="rent-name">小时租</text>
						<text class="rent-tag">4h 起租</text>
					</view>
					<switch color="#59b50e" :checked="form.rentModes.hour" @change="toggleRentMode('hour', $event)" />
				</view>
				<view class="rent-price-row">
					<text class="rent-currency">¥</text>
					<input
						v-model="form.priceHour"
						class="rent-price-input"
						type="digit"
						:disabled="!form.rentModes.hour"
						placeholder="0.00"
					/>
					<text class="rent-unit">/ 时</text>
				</view>
			</view>

			<view class="rent-card" :class="{ dim: !form.rentModes.overnight }">
				<view class="rent-head">
					<view>
						<text class="rent-name">过夜租</text>
						<text class="rent-tag">{{ overnightTimeLabel }}</text>
					</view>
					<switch color="#59b50e" :checked="form.rentModes.overnight" @change="toggleRentMode('overnight', $event)" />
				</view>
				<view v-if="form.rentModes.overnight" class="overnight-time-row">
					<view class="overnight-time-item">
						<text class="mini-label">开始时间</text>
						<picker mode="time" :value="form.overnightStartTime" @change="onOvernightStartChange">
							<view class="field-box compact picker">
								<text class="picker-val">{{ form.overnightStartTime || '18:00' }}</text>
								<up-icon name="clock" size="14" color="#b0b8a8" />
							</view>
						</picker>
					</view>
					<view class="overnight-time-item">
						<text class="mini-label">结束时间</text>
						<picker mode="time" :value="form.overnightEndTime" @change="onOvernightEndChange">
							<view class="field-box compact picker">
								<text class="picker-val">次日 {{ form.overnightEndTime || '10:00' }}</text>
								<up-icon name="clock" size="14" color="#b0b8a8" />
							</view>
						</picker>
					</view>
				</view>
				<view class="rent-price-row">
					<text class="rent-currency">¥</text>
					<input
						v-model="form.priceOvernight"
						class="rent-price-input"
						type="digit"
						:disabled="!form.rentModes.overnight"
						placeholder="0.00"
					/>
					<text class="rent-unit">/ 晚</text>
				</view>
			</view>

			<view class="split-line" />

			<view class="field-row">
				<view class="field half">
					<text class="field-label required">商品押金</text>
					<view class="field-box unit">
						<input v-model="form.deposit" class="field-input align-right" type="digit" placeholder="0" />
						<text class="unit-suffix">元</text>
					</view>
				</view>
				<view class="field half">
					<text class="field-label required">可租库存</text>
					<view class="field-box unit">
						<input v-model="form.stock" class="field-input align-right" type="number" placeholder="0" />
						<text class="unit-suffix">件</text>
					</view>
				</view>
			</view>

			<view class="field">
				<text class="field-label required">配送方式</text>
				<text class="field-tip block">可多选，至少保留一种</text>
				<view class="delivery-grid">
					<view
						v-for="item in deliveryOptions"
						:key="item.key"
						class="delivery-chip"
						:class="{ active: form.deliveryModes[item.key] }"
						@click="toggleDeliveryMode(item.key)"
					>
						<view class="delivery-chip-head">
							<up-icon
								:name="item.key === 'delivery' ? 'car' : 'home'"
								size="18"
								:color="form.deliveryModes[item.key] ? '#59b50e' : '#b0b8a8'"
							/>
							<text class="delivery-chip-label">{{ item.label }}</text>
							<view class="delivery-check" :class="{ on: form.deliveryModes[item.key] }">
								<up-icon v-if="form.deliveryModes[item.key]" name="checkmark" size="12" color="#fff" />
							</view>
						</view>
						<text class="delivery-chip-desc">{{ item.desc }}</text>
					</view>
				</view>
			</view>
		</view>

		<!-- 03 扩展信息 -->
		<view class="card">
			<view class="card-head">
				<view class="card-index">03</view>
				<view class="card-head-text">
					<text class="card-title">配套设备</text>
					<text class="card-desc">可选，添加额外配件或组合设备</text>
				</view>
				<view class="card-action" @click="addPackage">
					<up-icon name="plus" size="14" color="#59b50e" />
					<text>添加</text>
				</view>
			</view>

			<view v-if="!form.packages.length" class="empty-block" @click="addPackage">
				<up-icon name="plus-circle" size="32" color="#c8d4bc" />
				<text class="empty-text">点击添加配套设备</text>
			</view>

			<view v-for="(item, index) in form.packages" :key="'pkg-' + index" class="addon-block">
				<view class="addon-head">
					<text class="addon-title">配套 {{ index + 1 }}</text>
					<text v-if="form.packages.length > 1" class="addon-del" @click="removePackage(index)">移除</text>
				</view>
				<view class="field-row">
					<view class="field grow">
						<text class="mini-label">名称</text>
						<view class="field-box compact">
							<input v-model="item.packageName" class="field-input" placeholder="配套名称" />
						</view>
					</view>
					<view class="field shrink">
						<text class="mini-label">加价</text>
						<view class="field-box compact unit">
							<input v-model="item.price" class="field-input align-right" type="digit" placeholder="0" />
							<text class="unit-suffix">元</text>
						</view>
					</view>
				</view>
				<view class="field compact">
					<text class="mini-label">说明</text>
					<view class="field-box compact textarea-box">
						<textarea
							v-model="item.packageDesc"
							class="field-textarea compact"
							maxlength="120"
							placeholder="包含的设备或配件"
						/>
					</view>
				</view>
			</view>
		</view>

		<view class="card">
			<view class="card-head">
				<view class="card-index muted">—</view>
				<view class="card-head-text">
					<text class="card-title">租赁规格</text>
					<text class="card-desc">选填，用户下单前需选择的型号与加价</text>
				</view>
				<view class="card-action" @click="addSpec">
					<up-icon name="plus" size="14" color="#59b50e" />
					<text>添加</text>
				</view>
			</view>

			<view v-if="!form.specs.length" class="empty-block" @click="addSpec">
				<up-icon name="list" size="32" color="#c8d4bc" />
				<text class="empty-text">点击添加租赁规格</text>
			</view>

			<view v-for="(item, index) in form.specs" :key="'spec-' + index" class="spec-row">
				<view class="field-box compact spec-key">
					<input v-model="item.specName" class="field-input" placeholder="规格名称，如 4人帐" />
				</view>
				<view class="field-box compact spec-price unit">
					<input v-model="item.specPrice" class="field-input align-right" type="digit" placeholder="加价" />
					<text class="unit-suffix">元</text>
				</view>
				<view v-if="form.specs.length > 1" class="spec-del" @click="removeSpec(index)">
					<up-icon name="trash" size="18" color="#c8a0a0" />
				</view>
			</view>
		</view>

		<view class="card">
			<view class="card-head">
				<view class="card-index muted">—</view>
				<view class="card-head-text">
					<text class="card-title">商品参数</text>
					<text class="card-desc">选填，尺寸材质等，仅详情展示</text>
				</view>
				<view class="card-action" @click="addParam">
					<up-icon name="plus" size="14" color="#59b50e" />
					<text>添加</text>
				</view>
			</view>

			<view v-if="!form.params.length" class="empty-block" @click="addParam">
				<up-icon name="file-text" size="32" color="#c8d4bc" />
				<text class="empty-text">点击添加商品参数</text>
			</view>

			<view v-for="(item, index) in form.params" :key="'param-' + index" class="spec-row">
				<view class="field-box compact spec-key">
					<input v-model="item.paramName" class="field-input" placeholder="参数名，如 尺寸" />
				</view>
				<view class="field-box compact spec-key">
					<input v-model="item.paramValue" class="field-input" placeholder="参数值" />
				</view>
				<view v-if="form.params.length > 1" class="spec-del" @click="removeParam(index)">
					<up-icon name="trash" size="18" color="#c8a0a0" />
				</view>
			</view>
		</view>

		<!-- 编辑态元信息 -->
		<view v-if="!isCreate && goods.id" class="meta-card">
			<view class="meta-row">
				<text class="meta-label">展示状态</text>
				<text class="meta-value" :class="form.shelfStatus ? 'on' : 'off'">
					{{ form.shelfStatus ? '展示中' : '未展示' }}
				</text>
			</view>
		</view>

		<view class="footer-gap" />
		<view class="footer">
			<button class="submit-btn" :loading="saving" :disabled="saving" @click="save">
				{{ isCreate ? '提交并上架' : '保存修改' }}
			</button>
		</view>
	</view>
</template>

<script setup>
	import { computed, reactive, ref } from 'vue';
	import { onLoad } from '@dcloudio/uni-app';
	import { ensureLogin } from '@/common/auth';
	import { ensureMerchantOperational, createGoods, fetchGoodsDetail, updateGoods } from '@/common/api/merchant';
	import { fetchCategoryTree } from '@/common/api/rental';
	import {
		createEmptyGoodsEditForm,
		DEFAULT_OVERNIGHT_END,
		DEFAULT_OVERNIGHT_START,
		findRentCategoryPath,
		formatGoodsDeliveryLabel,
		formatOvernightTimeLabel,
		getRentCategoryRoots,
		GOODS_DELIVERY_OPTIONS,
		parseGoodsDeliveryModes,
		validateGoodsForm
	} from '@/common/merchant/constants';
	import { chooseAndUploadImages, chooseAndUploadVideo, resolveDisplayImageUrl } from '@/common/upload';

	const resolveImageUrl = (src) => resolveDisplayImageUrl(src);

	const goods = ref({ id: '', shelfStatus: false });
	const form = reactive(createEmptyGoodsEditForm());
	const images = ref([]);
	const categoryTree = ref([]);
	const rootIndex = ref(0);
	const subIndex = ref(0);
	const isCreate = ref(false);
	const goodsId = ref('');
	const saving = ref(false);

	const rootCategories = computed(() => getRentCategoryRoots(categoryTree.value));
	const rootCategoryLabels = computed(() => rootCategories.value.map((item) => item.name));
	const subCategories = computed(() => {
		const root = rootCategories.value.find((item) => Number(item.id) === Number(form.rootCategoryId));
		return root?.children || [];
	});
	const subCategoryLabels = computed(() => subCategories.value.map((item) => item.name));
	const showSubCategory = computed(() => subCategories.value.length > 0);
	const deliveryOptions = GOODS_DELIVERY_OPTIONS;
	const overnightTimeLabel = computed(() =>
		formatOvernightTimeLabel(form.overnightStartTime, form.overnightEndTime)
	);

	const syncCategorySelection = () => {
		if (!form.categoryId || !categoryTree.value.length) return;
		const path = findRentCategoryPath(categoryTree.value, form.categoryId);
		if (!path) return;
		form.rootCategoryId = path.rootId;
		form.rootCategoryName = path.rootName;
		form.subCategoryId = path.childId;
		form.subCategoryName = path.childName;
		form.category = path.label;
		const rootIdx = rootCategories.value.findIndex((item) => Number(item.id) === Number(path.rootId));
		rootIndex.value = rootIdx >= 0 ? rootIdx : 0;
		if (path.childId) {
			const subIdx = subCategories.value.findIndex((item) => Number(item.id) === Number(path.childId));
			subIndex.value = subIdx >= 0 ? subIdx : 0;
		} else {
			subIndex.value = 0;
		}
	};

	const loadCategories = async () => {
		try {
			const tree = await fetchCategoryTree();
			categoryTree.value = Array.isArray(tree) ? tree : [];
			syncCategorySelection();
		} catch (error) {
			categoryTree.value = [];
		}
	};

	onLoad(async (options) => {
		if (!ensureLogin('/packageMerchant/goods-edit')) return;
		try {
			await ensureMerchantOperational({ redirect: true, toast: true });
		} catch {
			return;
		}
		isCreate.value = options.mode === 'create';
		uni.setNavigationBarTitle({ title: isCreate.value ? '新增商品' : '编辑商品' });
		await loadCategories();
		if (isCreate.value) {
			images.value = [];
			Object.assign(form, createEmptyGoodsEditForm());
			syncDeliveryLabel();
			return;
		}
		goodsId.value = options.id || '';
		try {
			const { view, form: editForm } = await fetchGoodsDetail(goodsId.value);
			goods.value = view;
			Object.assign(form, createEmptyGoodsEditForm(), editForm);
			form.deliveryModes = parseGoodsDeliveryModes(editForm.deliveryModes ?? editForm.pickup);
			syncDeliveryLabel();
			images.value = editForm.images?.length ? editForm.images : (view.images || []);
			syncCategorySelection();
		} catch (error) {
			uni.showToast({ title: error?.msg || error?.message || '加载商品失败', icon: 'none' });
		}
	});

	const applyRootCategory = (root) => {
		if (!root) return;
		form.rootCategoryId = root.id;
		form.rootCategoryName = root.name;
		form.subCategoryId = null;
		form.subCategoryName = '';
		subIndex.value = 0;
		const children = root.children || [];
		if (children.length) {
			form.categoryId = null;
			form.category = '';
			return;
		}
		form.categoryId = root.id;
		form.category = root.name;
	};

	const onRootCategoryChange = (event) => {
		const index = Number(event.detail.value || 0);
		rootIndex.value = index;
		applyRootCategory(rootCategories.value[index]);
	};

	const onSubCategoryChange = (event) => {
		const index = Number(event.detail.value || 0);
		subIndex.value = index;
		const selected = subCategories.value[index];
		if (!selected) return;
		form.subCategoryId = selected.id;
		form.subCategoryName = selected.name;
		form.categoryId = selected.id;
		form.category = `${form.rootCategoryName} / ${selected.name}`;
	};

	const toggleRentMode = (key, event) => {
		form.rentModes[key] = !!event.detail.value;
		if (!form.rentModes[key]) {
			if (key === 'hour') form.priceHour = '';
			if (key === 'overnight') form.priceOvernight = '';
			return;
		}
		if (key === 'overnight') {
			form.overnightStartTime = form.overnightStartTime || DEFAULT_OVERNIGHT_START;
			form.overnightEndTime = form.overnightEndTime || DEFAULT_OVERNIGHT_END;
		}
	};

	const onOvernightStartChange = (event) => {
		form.overnightStartTime = event.detail.value || DEFAULT_OVERNIGHT_START;
	};

	const onOvernightEndChange = (event) => {
		form.overnightEndTime = event.detail.value || DEFAULT_OVERNIGHT_END;
	};

	const syncDeliveryLabel = () => {
		form.pickup = formatGoodsDeliveryLabel(form.deliveryModes);
	};

	const toggleDeliveryMode = (key) => {
		const next = !form.deliveryModes[key];
		const enabledCount = Object.values(form.deliveryModes).filter(Boolean).length;
		if (!next && enabledCount <= 1) {
			uni.showToast({ title: '至少保留一种配送方式', icon: 'none' });
			return;
		}
		form.deliveryModes[key] = next;
		syncDeliveryLabel();
	};

	const addPackage = () => {
		form.packages.push({ packageName: '', packageDesc: '', price: '0' });
	};

	const removePackage = (index) => {
		form.packages.splice(index, 1);
	};

	const addSpec = () => {
		form.specs.push({ specName: '', specPrice: '' });
	};

	const removeSpec = (index) => {
		form.specs.splice(index, 1);
	};

	const addParam = () => {
		form.params.push({ paramName: '', paramValue: '' });
	};

	const removeParam = (index) => {
		form.params.splice(index, 1);
	};

	const addImage = async () => {
		if (images.value.length >= 6) {
			uni.showToast({ title: '最多上传 6 张图片', icon: 'none' });
			return;
		}
		try {
			uni.showLoading({ title: '上传中' });
			const urls = await chooseAndUploadImages({ count: 6 - images.value.length });
			images.value = [...images.value, ...urls].slice(0, 6);
		} catch (error) {
			uni.showToast({ title: '图片上传失败', icon: 'none' });
		} finally {
			uni.hideLoading();
		}
	};

	const removeImage = (index) => {
		images.value = images.value.filter((_, idx) => idx !== index);
	};

	const uploadTutorialVideo = async () => {
		try {
			uni.showLoading({ title: '上传中' });
			const url = await chooseAndUploadVideo();
			form.tutorialVideoUrl = url;
		} catch (error) {
			if (error?.errMsg?.includes('cancel') || error?.message === 'video too large') return;
			uni.showToast({ title: '视频上传失败', icon: 'none' });
		} finally {
			uni.hideLoading();
		}
	};

	const removeTutorialVideo = () => {
		form.tutorialVideoUrl = '';
	};

	const save = async () => {
		const message = validateGoodsForm(form, images.value, categoryTree.value);
		if (message) {
			uni.showToast({ title: message, icon: 'none' });
			return;
		}
		saving.value = true;
		try {
			const payload = {
				...form,
				coverImage: images.value[0],
				images: images.value
			};
			if (isCreate.value) {
				await createGoods(payload);
			} else {
				await updateGoods(goodsId.value, payload);
			}
			uni.showToast({ title: isCreate.value ? '商品已上架' : '商品信息已保存', icon: 'success' });
			setTimeout(() => uni.navigateBack(), 400);
		} catch (error) {
			uni.showToast({ title: error?.msg || error?.message || '保存失败', icon: 'none' });
		} finally {
			saving.value = false;
		}
	};
</script>

<style scoped>
	.page {
		box-sizing: border-box;
		min-height: 100vh;
		padding: 0 28rpx 0;
		background: #f6f8f3;
		overflow-x: hidden;
	}

	/* ===== Hero ===== */
	.hero {
		margin: 0 -28rpx;
		padding: 48rpx 36rpx 40rpx;
		background: linear-gradient(165deg, #7ed321 0%, #6bc01a 45%, #59b50e 100%);
		border-radius: 0 0 40rpx 40rpx;
		box-shadow: 0 16rpx 48rpx rgba(89, 181, 14, 0.22);
	}

	.hero-top {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-bottom: 16rpx;
	}

	.hero-badge {
		padding: 6rpx 18rpx;
		border-radius: 999rpx;
		font-size: 22rpx;
		font-weight: 600;
		color: #3d6e0a;
		background: rgba(255, 255, 255, 0.92);
	}

	.hero-id {
		font-size: 22rpx;
		color: rgba(255, 255, 255, 0.75);
	}

	.hero-title {
		display: block;
		font-size: 40rpx;
		font-weight: 800;
		color: #ffffff;
		line-height: 1.35;
	}

	.hero-sub {
		display: block;
		margin-top: 10rpx;
		font-size: 24rpx;
		line-height: 1.55;
		color: rgba(255, 255, 255, 0.88);
	}

	/* ===== Step track ===== */
	.step-track {
		display: flex;
		align-items: center;
		justify-content: center;
		gap: 0;
		margin: 32rpx 0 28rpx;
		padding: 20rpx 32rpx;
		border-radius: 24rpx;
		background: #ffffff;
		box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.04);
	}

	.step-item {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 8rpx;
	}

	.step-dot {
		display: flex;
		align-items: center;
		justify-content: center;
		width: 44rpx;
		height: 44rpx;
		border-radius: 50%;
		font-size: 22rpx;
		font-weight: 700;
		color: #b0b8a8;
		background: #eef2ea;
	}

	.step-item.active .step-dot {
		color: #ffffff;
		background: linear-gradient(135deg, #7ed321, #59b50e);
		box-shadow: 0 4rpx 12rpx rgba(89, 181, 14, 0.35);
	}

	.step-label {
		font-size: 20rpx;
		color: #b0b8a8;
	}

	.step-item.active .step-label {
		color: #3d6e0a;
		font-weight: 600;
	}

	.step-line {
		flex: 1;
		height: 2rpx;
		margin: 0 12rpx;
		margin-bottom: 28rpx;
		background: linear-gradient(90deg, #d4e8c0, #eef2ea);
	}

	/* ===== Card ===== */
	.card {
		box-sizing: border-box;
		margin-bottom: 24rpx;
		padding: 32rpx 28rpx;
		border-radius: 32rpx;
		background: #ffffff;
		box-shadow: 0 6rpx 28rpx rgba(0, 0, 0, 0.04);
	}

	.card-head {
		display: flex;
		align-items: flex-start;
		gap: 16rpx;
		margin-bottom: 32rpx;
	}

	.card-index {
		flex-shrink: 0;
		display: flex;
		align-items: center;
		justify-content: center;
		width: 52rpx;
		height: 52rpx;
		border-radius: 16rpx;
		font-size: 22rpx;
		font-weight: 800;
		color: #59b50e;
		background: #edf8e4;
	}

	.card-index.muted {
		color: #b0b8a8;
		background: #f0f2ee;
	}

	.card-head-text {
		flex: 1;
		min-width: 0;
	}

	.card-title {
		display: block;
		font-size: 30rpx;
		font-weight: 700;
		color: #1a2418;
		line-height: 1.35;
	}

	.card-desc {
		display: block;
		margin-top: 4rpx;
		font-size: 22rpx;
		line-height: 1.45;
		color: #8a9a7a;
	}

	.card-action {
		flex-shrink: 0;
		display: flex;
		align-items: center;
		gap: 4rpx;
		padding: 8rpx 16rpx;
		border-radius: 999rpx;
		font-size: 24rpx;
		font-weight: 600;
		color: #59b50e;
		background: #edf8e4;
	}

	/* ===== Fields ===== */
	.field {
		margin-bottom: 28rpx;
	}

	.field:last-child {
		margin-bottom: 0;
	}

	.field.compact {
		margin-bottom: 16rpx;
	}

	.field.half {
		flex: 1;
		min-width: 0;
		margin-bottom: 0;
	}

	.field.grow {
		flex: 1;
		min-width: 0;
		margin-bottom: 0;
	}

	.field.shrink {
		flex-shrink: 0;
		width: 180rpx;
		margin-bottom: 0;
	}

	.field-row {
		display: flex;
		align-items: flex-end;
		gap: 16rpx;
		margin-bottom: 28rpx;
	}

	.field-row.single .field.half {
		flex: 1;
	}

	.field-label {
		display: block;
		margin-bottom: 12rpx;
		font-size: 26rpx;
		font-weight: 600;
		color: #1a2418;
		line-height: 1.35;
	}

	.field-label.required::before {
		content: '*';
		margin-right: 4rpx;
		color: #e05c5c;
	}

	.field-label-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-bottom: 12rpx;
	}

	.field-tip {
		font-size: 22rpx;
		color: #b0b8a8;
	}

	.field-tip.block {
		display: block;
		margin: -4rpx 0 16rpx;
	}

	.mini-label {
		display: block;
		margin-bottom: 8rpx;
		font-size: 22rpx;
		color: #8a9a7a;
	}

	.field-box {
		box-sizing: border-box;
		display: flex;
		align-items: center;
		min-height: 88rpx;
		padding: 0 24rpx;
		border-radius: 20rpx;
		background: #f4f6f2;
		border: 2rpx solid transparent;
		transition: border-color 0.2s;
	}

	.field-box.compact {
		min-height: 76rpx;
		padding: 0 20rpx;
		border-radius: 16rpx;
	}

	.field-box.picker {
		justify-content: space-between;
	}

	.field-box.picker.empty .picker-val {
		color: #b0b8a8;
	}

	.field-box.textarea-box {
		align-items: stretch;
		min-height: 180rpx;
		padding: 20rpx 24rpx;
	}

	.field-box.unit {
		padding-right: 20rpx;
	}

	.field-input {
		flex: 1;
		width: 100%;
		min-width: 0;
		height: 88rpx;
		font-size: 28rpx;
		color: #1a2418;
		background: transparent;
	}

	.field-box.compact .field-input {
		height: 76rpx;
		font-size: 26rpx;
	}

	.field-input.align-right {
		text-align: right;
	}

	.field-textarea {
		width: 100%;
		height: 140rpx;
		font-size: 28rpx;
		line-height: 1.6;
		color: #1a2418;
		background: transparent;
	}

	.field-textarea.compact {
		height: 120rpx;
		font-size: 26rpx;
	}

	.field-box.compact.textarea-box {
		align-items: stretch;
		min-height: 120rpx;
		padding: 16rpx 20rpx;
	}

	.picker-val {
		flex: 1;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
		font-size: 28rpx;
		color: #1a2418;
	}

	.unit-suffix {
		flex-shrink: 0;
		margin-left: 8rpx;
		font-size: 24rpx;
		color: #8a9a7a;
	}

	/* ===== Images ===== */
	.img-grid {
		display: flex;
		flex-wrap: wrap;
		gap: 12rpx;
	}

	.img-cell {
		position: relative;
		width: calc((100% - 36rpx) / 4);
		aspect-ratio: 1;
		border-radius: 16rpx;
		overflow: hidden;
	}

	.img-thumb {
		width: 100%;
		height: 100%;
	}

	.img-del {
		position: absolute;
		top: 8rpx;
		right: 8rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		width: 36rpx;
		height: 36rpx;
		border-radius: 50%;
		background: rgba(0, 0, 0, 0.45);
	}

	.img-cover-mini {
		position: absolute;
		left: 0;
		bottom: 0;
		padding: 4rpx 10rpx;
		font-size: 18rpx;
		color: #fff;
		background: rgba(26, 36, 24, 0.6);
	}

	.img-cell.add {
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		gap: 6rpx;
		border: 2rpx dashed #c8d4bc;
		background: #fafbf8;
	}

	.add-label {
		font-size: 22rpx;
		color: #8a9a7a;
	}

	/* ===== Tutorial video ===== */
	.video-upload {
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		gap: 8rpx;
		padding: 48rpx 24rpx;
		border-radius: 20rpx;
		border: 2rpx dashed #c8d4bc;
		background: #fafbf8;
	}

	.video-upload-label {
		font-size: 26rpx;
		font-weight: 600;
		color: #3d6e0a;
	}

	.video-upload-tip {
		font-size: 22rpx;
		color: #b0b8a8;
	}

	.video-preview {
		display: flex;
		flex-direction: column;
		gap: 12rpx;
	}

	.video-player {
		width: 100%;
		height: 360rpx;
		border-radius: 20rpx;
		background: #1a2418;
	}

	.video-actions {
		display: flex;
		align-items: center;
		gap: 24rpx;
	}

	.video-action {
		font-size: 24rpx;
		font-weight: 600;
		color: #59b50e;
	}

	.video-action.danger {
		color: #e05c5c;
	}

	/* ===== Rent cards ===== */
	.rent-card {
		margin-bottom: 16rpx;
		padding: 24rpx 28rpx;
		border-radius: 24rpx;
		background: #f8faf6;
		border: 2rpx solid #e8efe4;
	}

	.rent-card.main {
		background: linear-gradient(135deg, #f0fae8 0%, #edf8e4 100%);
		border-color: #c8e8a8;
	}

	.rent-card.dim {
		opacity: 0.55;
	}

	.rent-head {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-bottom: 16rpx;
	}

	.rent-name {
		display: block;
		font-size: 28rpx;
		font-weight: 700;
		color: #1a2418;
	}

	.rent-name.required::before {
		content: '*';
		margin-right: 4rpx;
		color: #e05c5c;
	}

	.rent-tag {
		display: block;
		margin-top: 4rpx;
		font-size: 22rpx;
		color: #8a9a7a;
	}

	.rent-price-row {
		display: flex;
		align-items: baseline;
		gap: 4rpx;
	}

	.rent-currency {
		font-size: 32rpx;
		font-weight: 700;
		color: #59b50e;
	}

	.rent-price-input {
		flex: 1;
		min-width: 0;
		height: 72rpx;
		font-size: 44rpx;
		font-weight: 800;
		color: #1a2418;
		background: transparent;
	}

	.rent-unit {
		font-size: 24rpx;
		color: #8a9a7a;
	}

	.overnight-time-row {
		display: flex;
		gap: 16rpx;
		margin-bottom: 16rpx;
	}

	.overnight-time-item {
		flex: 1;
		min-width: 0;
	}

	.split-line {
		height: 1rpx;
		margin: 28rpx 0;
		background: #e8efe4;
	}

	.delivery-grid {
		display: flex;
		flex-direction: column;
		gap: 16rpx;
	}

	.delivery-chip {
		padding: 24rpx;
		border-radius: 20rpx;
		background: #f8faf6;
		border: 2rpx solid #e8efe4;
		transition: all 0.2s ease;
	}

	.delivery-chip.active {
		background: linear-gradient(135deg, #f4fae8 0%, #edf8e4 100%);
		border-color: #b8e08a;
		box-shadow: 0 6rpx 20rpx rgba(89, 181, 14, 0.12);
	}

	.delivery-chip-head {
		display: flex;
		align-items: center;
		gap: 12rpx;
	}

	.delivery-chip-label {
		flex: 1;
		font-size: 28rpx;
		font-weight: 700;
		color: #1a2418;
	}

	.delivery-check {
		display: flex;
		align-items: center;
		justify-content: center;
		width: 36rpx;
		height: 36rpx;
		border-radius: 50%;
		border: 2rpx solid #c8d4bc;
		background: #ffffff;
	}

	.delivery-check.on {
		border-color: #59b50e;
		background: #59b50e;
	}

	.delivery-chip-desc {
		display: block;
		margin-top: 8rpx;
		padding-left: 30rpx;
		font-size: 22rpx;
		line-height: 1.45;
		color: #8a9a7a;
	}

	/* ===== Addon / Spec ===== */
	.empty-block {
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		gap: 12rpx;
		padding: 48rpx 24rpx;
		border-radius: 20rpx;
		border: 2rpx dashed #d8e4d0;
		background: #fafbf8;
	}

	.empty-text {
		font-size: 24rpx;
		color: #b0b8a8;
	}

	.addon-block {
		margin-bottom: 20rpx;
		padding: 24rpx;
		border-radius: 20rpx;
		background: #f8faf6;
		border: 1rpx solid #e8efe4;
	}

	.addon-block:last-child {
		margin-bottom: 0;
	}

	.addon-head {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-bottom: 16rpx;
	}

	.addon-title {
		font-size: 24rpx;
		font-weight: 700;
		color: #3d6e0a;
	}

	.addon-del {
		font-size: 22rpx;
		color: #e05c5c;
	}

	.spec-row {
		display: flex;
		align-items: center;
		gap: 12rpx;
		margin-bottom: 12rpx;
	}

	.spec-row:last-child {
		margin-bottom: 0;
	}

	.spec-key {
		flex: 1;
		min-width: 0;
	}

	.spec-price {
		flex: 0 0 200rpx;
	}

	.spec-del {
		flex-shrink: 0;
		display: flex;
		align-items: center;
		justify-content: center;
		width: 56rpx;
		height: 56rpx;
	}

	/* ===== Meta ===== */
	.meta-card {
		margin-bottom: 24rpx;
		padding: 24rpx 28rpx;
		border-radius: 20rpx;
		background: #ffffff;
		box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.03);
	}

	.meta-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	.meta-label {
		font-size: 26rpx;
		color: #8a9a7a;
	}

	.meta-value {
		font-size: 26rpx;
		font-weight: 700;
	}

	.meta-value.on {
		color: #59b50e;
	}

	.meta-value.off {
		color: #b0b8a8;
	}

	/* ===== Footer ===== */
	.footer-gap {
		height: 160rpx;
	}

	.footer {
		position: fixed;
		left: 0;
		right: 0;
		bottom: 0;
		padding: 16rpx 28rpx calc(16rpx + env(safe-area-inset-bottom));
		background: rgba(246, 248, 243, 0.92);
		backdrop-filter: blur(12px);
		border-top: 1rpx solid rgba(0, 0, 0, 0.05);
	}

	.submit-btn {
		width: 100%;
		height: 96rpx;
		line-height: 96rpx;
		border: none;
		border-radius: 999rpx;
		font-size: 32rpx;
		font-weight: 700;
		color: #ffffff;
		background: linear-gradient(135deg, #7ed321 0%, #59b50e 100%);
		box-shadow: 0 8rpx 28rpx rgba(89, 181, 14, 0.35);
	}

	.submit-btn[disabled] {
		opacity: 0.65;
	}
</style>
