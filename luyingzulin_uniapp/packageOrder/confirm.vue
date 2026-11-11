<template>
	<view class="page">
		<view v-if="pageLoading" class="page-loading"><text>加载订单信息...</text></view>
		<template v-else>
			<view class="section-card goods-card">
				<text class="section-title">租赁商品</text>
				<view v-for="item in draft.goodsItems" :key="`${item.id}-${item.cartId || 0}`" class="goods-row">
					<image class="goods-image" :src="item.image" mode="aspectFill"></image>
					<view class="goods-main">
						<text class="goods-name">{{ item.name }}</text>
						<text class="goods-meta">{{ item.specLabel }}</text>
						<text class="goods-meta">{{ item.rentModeLabel }} · {{ item.rangeText }}</text>
						<text class="goods-price">{{ formatGoodsPriceText(item) }}</text>
					</view>
				</view>
			</view>

			<view class="section-card pickup-card">
				<view class="pickup-segment">
					<view v-for="item in pickupTypes" :key="item.key" class="segment-btn"
						:class="{ active: pickupType === item.key }" @click="handlePickupChange({ key: item.key })">{{ item.label }}
					</view>
				</view>

				<view class="pickup-location-row" @click="openPickupSelector">
					<view class="pickup-row-icon">
						<up-icon :name="pickupType === 'delivery' ? 'map' : 'home'" size="20" color="#59b50e"></up-icon>
					</view>
					<view class="pickup-row-main">
						<text class="pickup-row-title">{{ pickupLocationTitle }}</text>
						<text class="pickup-row-desc">{{ pickupLocationDesc }}</text>
					</view>
					<up-icon name="arrow-right" size="14" color="#b0b8c4"></up-icon>
				</view>

				<view v-if="source === 'cart'" class="rent-mode-block">
					<text class="rent-mode-label">租赁方式</text>
					<view class="rent-mode-segment">
						<view v-for="item in rentModeOptions" :key="item.key" class="mode-btn"
							:class="{ active: rentMode === item.key }" @click="handleRentModeChange(item.key)">{{ item.label }}</view>
					</view>
				</view>

				<view class="pickup-time-row">
					<view class="pickup-row-icon">
						<up-icon name="clock" size="18" color="#59b50e"></up-icon>
					</view>
					<text class="pickup-row-label">取货时间</text>
					<view class="pickup-row-pickers">
						<picker mode="date" :value="rentPeriod.startDate" @change="onStartDateChange">
							<text class="pickup-row-value picker-value">{{ pickupDateDisplay }}</text>
						</picker>
						<picker v-if="rentMode !== 'night'" mode="time" :value="rentPeriod.startClock" @change="onStartClockChange">
							<text class="pickup-row-value picker-value">{{ pickupClockDisplay }}</text>
						</picker>
					</view>
				</view>

				<view class="pickup-time-row" :class="{ readonly: rentMode === 'night' }">
					<view class="pickup-row-icon">
						<up-icon name="checkmark-circle" size="18" color="#59b50e"></up-icon>
					</view>
					<text class="pickup-row-label">预计归还</text>
					<view class="pickup-row-pickers">
						<picker v-if="rentMode !== 'night'" mode="date" :value="rentPeriod.endDate" @change="onEndDateChange">
							<text class="pickup-row-value picker-value">{{ returnDateDisplay }}</text>
						</picker>
						<picker v-if="rentMode !== 'night'" mode="time" :value="rentPeriod.endClock" @change="onEndClockChange">
							<text class="pickup-row-value picker-value">{{ returnClockDisplay }}</text>
						</picker>
						<text v-else class="pickup-row-value">{{ returnTimeDisplay }}</text>
					</view>
				</view>

				<text class="billing-rule-tip">{{ billingRuleText }}</text>
			</view>

			<view class="section-card coupon-entry-card">
				<view class="coupon-entry" @click="openCouponSelector">
					<text class="entry-label">优惠券</text>
					<view class="entry-right">
						<text class="entry-value" :class="{ active: selectedCouponId }">{{ couponEntryLabel }}</text>
						<text v-if="selectedCouponId && couponAmount !== '0.00'" class="entry-discount">-¥{{ couponAmount }}</text>
						<up-icon name="arrow-right" size="14" color="#9aa2b1"></up-icon>
					</view>
				</view>
				<text v-if="selectedCouponId" class="coupon-clear" @click.stop="clearCoupon">取消使用优惠券</text>
			</view>

			<view class="section-card form-card">
				<text class="section-title">联系人与备注</text>
				<view class="field-row">
					<text class="field-label">联系人</text>
					<input v-model="contactName" class="field-input" placeholder="请输入联系人姓名" />
				</view>
				<view class="field-row">
					<text class="field-label">手机号</text>
					<input v-model="contactPhone" class="field-input" type="number" maxlength="11" placeholder="请输入手机号" />
				</view>
				<view class="remark-box">
					<textarea v-model="remark" class="remark-input" maxlength="120" placeholder="备注搭建时间、停车需求、配送说明等"></textarea>
				</view>
			</view>

			<view class="section-card fee-card">
				<text class="section-title">费用明细</text>
				<view class="price-line"><text>租金小计</text><text>¥{{ rentAmount }}</text></view>
				<view class="price-line"><text>押金（账户冻结）</text><text>¥{{ depositAmount }}</text></view>
				<view v-if="depositSummaryLoaded" class="price-line deposit-account-line">
					<text>押金账户</text>
					<text>可用 ¥{{ depositAvailableAmount }}，本单冻结 ¥{{ depositAmount }}</text>
				</view>
				<view v-if="pickupType === 'delivery' || Number(draft.deliveryAmount) > 0" class="price-line">
					<text>配送费</text><text>¥{{ deliveryAmount }}</text>
				</view>
				<text v-if="pickupType === 'delivery' && deliveryFeeRemark" class="delivery-tip">{{ deliveryFeeRemark }}</text>
				<view class="price-line discount"><text>优惠抵扣</text><text>-¥{{ couponAmount }}</text></view>
				<view class="price-line total"><text>应付金额</text><text>¥{{ totalAmount }}</text></view>
			</view>

			<view class="section-card_ agreement-card">
				<view class="agree-icon" @click="agreed = !agreed"><up-icon
						:name="agreed ? 'checkmark-circle-fill' : 'checkmark-circle'" size="16"
						:color="agreed ? '#59b50e' : '#9aa2b1'"></up-icon></view>
				<text class="agree-text">
					我已阅读并同意
					<text class="agree-link" @click="openAgreement('rental')">《租赁协议》</text>
					<text class="agree-link" @click="openAgreement('deposit')">《押金规则》</text>
					<text class="agree-link" @click="openAgreement('damage')">《损坏赔付说明》</text>
				</text>
			</view>
		</template>
	</view>

	<view class="bottom-bar">
		<view>
			<text class="bottom-label">应付</text>
			<text class="bottom-price">¥{{ totalAmount }}</text>
		</view>
		<button class="submit-btn" :loading="submitting" :disabled="submitting || pageLoading"
			@click="submitOrder">提交订单</button>
	</view>

	<PaymentMethodPopup v-model:show="paymentPopupVisible" :amount="totalAmount" :loading="submitting"
		@confirm="confirmPayAndSubmit" />
</template>

<script setup>
	import {
		computed,
		ref
	} from 'vue';
	import {
		onLoad,
		onShow
	} from '@dcloudio/uni-app';
	import PaymentMethodPopup from '@/components/PaymentMethodPopup.vue';
	import {
		runAction
	} from '@/common/actionFeedback';
	import {
		checkoutPreview,
		updateCartItem
	} from '@/common/api/cart';
	import {
		fetchUsableCoupons
	} from '@/common/api/coupon';
	import {
		checkStock,
		fetchGoodsDetail
	} from '@/common/api/goods';
	import {
		createOrder
	} from '@/common/api/order';
	import {
		fetchDepositSummary
	} from '@/common/api/deposit';
	import {
		fetchPickupStores
	} from '@/common/api/store';
	import {
		fetchAddressList,
		getSelectedAddress
	} from '@/common/api/user';
	import {
		ensureLogin,
		isLoggedIn
	} from '@/common/auth';
	import {
		clearPendingConfirmPayload,
		consumeConfirmCouponSelection,
		createEmptyConfirmDraft,
		getPendingConfirmPayload,
		setConfirmCouponContext,
		setConfirmCouponSelection
	} from '@/common/rental/confirmStorage';
	import {
		buildDeliveryFeeRemark,
		calcDeliveryFeeAmount,
		calcOrderPayableAmount,
		calcRentBilling,
		calcRentItemAmount,
		CONFIRM_PICKUP_TYPES,
		createRentPeriodFromTimes,
		createRentPeriodSelection,
		DETAIL_RENT_MODES,
		formatConfirmGoodsPriceText,
		formatMoney,
		formatRentPeriodDisplay,
		formatRentRangeText,
		getGoodsRentPriceByMode,
		getRentTypeLabel,
		mapApiRentTypeToDetailMode,
		mapCheckoutPreviewItemToView,
		mapPickupTypeFromApi,
		syncRentPeriodTimes,
		validateConfirmSubmit
	} from '@/common/rental/constants';
	import {
		launchPayment,
		navigateAfterPayment
	} from '@/common/payment';
	import {
		requestOrderSubscribeMessage
	} from '@/common/subscribeMessage';

	const draft = ref(createEmptyConfirmDraft());
	const confirmPayload = ref(null);
	const pageLoading = ref(false);
	const previewLoading = ref(false);
	const source = ref('');
	const pickupType = ref('store');
	const storeOptions = ref([]);
	const selectedCouponId = ref(0);
	const selectedCoupon = ref(null);
	const deliveryFeeRemark = ref('');
	const usableCouponCount = ref(0);
	const contactName = ref('');
	const contactPhone = ref('');
	const remark = ref('');
	const agreed = ref(true);
	const payMethod = ref('');
	const submitting = ref(false);
	const paymentPopupVisible = ref(false);
	const rentPeriodConfirmed = ref(false);
	const rentMode = ref('day');
	const rentPeriod = ref(createRentPeriodSelection('day'));
	const directGoods = ref(null);
	const directPackagePrice = ref(0);
	const directSpecPrice = ref(0);
	const storedCartItems = ref([]);
	const depositSummary = ref({
		availableAmount: 0,
		minDepositBalance: 0
	});
	const depositSummaryLoaded = ref(false);
	let previewRequestId = 0;

	const pickupTypes = CONFIRM_PICKUP_TYPES;
	const rentModeOptions = DETAIL_RENT_MODES.map((item) => ({
		key: item.key,
		label: item.label
	}));
	const returnTimeDisplay = computed(() => formatRentPeriodDisplay(rentPeriod.value.endTime));
	const pickupDateDisplay = computed(() => {
		if (!rentPeriod.value.startDate) return '选择日期';
		const date = rentPeriod.value.startDate.split('-');
		return date.length >= 3 ? `${Number(date[1])}月${Number(date[2])}日` : rentPeriod.value.startDate;
	});
	const pickupClockDisplay = computed(() => rentPeriod.value.startClock || '选择时间');
	const returnDateDisplay = computed(() => {
		if (!rentPeriod.value.endDate) return '选择日期';
		const date = rentPeriod.value.endDate.split('-');
		return date.length >= 3 ? `${Number(date[1])}月${Number(date[2])}日` : rentPeriod.value.endDate;
	});
	const returnClockDisplay = computed(() => rentPeriod.value.endClock || '选择时间');
	const pickupLocationTitle = computed(() => {
		if (pickupType.value === 'delivery') {
			return draft.value.address?.name || draft.value.address?.contactName || '请选择配送地址';
		}
		return draft.value.store?.name || '请选择取货门店';
	});
	const pickupLocationDesc = computed(() => {
		if (pickupType.value === 'delivery') {
			return draft.value.address?.address || draft.value.address?.phone || '点击选择同城送达地址';
		}
		return draft.value.store?.address || '点击选择门店';
	});
	const billingRuleText = computed(() => {
		if (!rentPeriodConfirmed.value) return '请先选择取货与归还时间，系统将按租期自动计费';
		const rentType = rentMode.value === 'hour' ? 'hour' : rentMode.value === 'night' ? 'overnight' : 'day';
		const billing = calcRentBilling(rentType, rentPeriod.value.startTime, rentPeriod.value.endTime);
		return billing.valid ? billing.ruleText : '请选择有效租期';
	});
	const formatGoodsPriceText = (item) => formatConfirmGoodsPriceText(item, rentPeriodConfirmed.value);

	const resetRentPeriodDraft = () => {
		rentPeriodConfirmed.value = false;
		draft.value.rentAmount = 0;
		draft.value.rangeText = '待选择';
		draft.value.days = 0;
		if (draft.value.goodsItems?.length) {
			const rentType = rentMode.value === 'hour' ? 'hour' : rentMode.value === 'night' ? 'overnight' : 'day';
			draft.value.goodsItems = draft.value.goodsItems.map((item) => ({
				...item,
				rangeText: '待选择',
				days: 0,
				rentModeLabel: getRentTypeLabel(rentType)
			}));
		}
		if (source.value === 'cart') {
			draft.value.payAmount = calcOrderPayableAmount({
				rentAmount: 0,
				deliveryAmount: 0,
				couponAmount: 0
			});
		}
	};

	const handleRentModeChange = async (key) => {
		if (source.value !== 'cart' || rentMode.value === key) return;
		rentMode.value = key;
		rentPeriod.value = createRentPeriodSelection(key);
		resetRentPeriodDraft();
		await applyDefaultRentPeriod();
		await loadUsableCouponCount();
	};

	const resolvePickupStoreId = () => {
		return draft.value.store?.id ? Number(draft.value.store.id) : undefined;
	};

	const resolveOrderMerchantId = async () => {
		if (directGoods.value?.merchantId) {
			return Number(directGoods.value.merchantId);
		}
		const goodsId = Number(confirmPayload.value?.items?.[0]?.goodsId || 0);
		if (!goodsId) return undefined;
		try {
			const goods = await fetchGoodsDetail(goodsId);
			return goods?.merchantId ? Number(goods.merchantId) : undefined;
		} catch (error) {
			return undefined;
		}
	};

	const loadPickupLocations = async () => {
		try {
			const merchantId = await resolveOrderMerchantId();
			const stores = await fetchPickupStores('store', {
				merchantId,
				silentError: true
			});
			storeOptions.value = stores;
			const currentStoreId = draft.value.store?.id;
			const matchedStore = stores.find((item) => Number(item.id) === Number(currentStoreId));
			if (matchedStore) {
				draft.value.store = matchedStore;
			} else if (stores.length) {
				draft.value.store = stores[0];
			} else {
				draft.value.store = null;
			}
		} catch (error) {
			storeOptions.value = [];
		}
	};

	const buildDirectBuyDraft = async (options = {}) => {
		const goodsId = Number(options.goodsId || 0);
		if (!goodsId) throw new Error('商品信息缺失');
		const goods = await fetchGoodsDetail(goodsId);
		directGoods.value = goods;
		const mode = options.rentMode || 'day';
		rentMode.value = mode;
		const rentType = mode === 'hour' ? 'hour' : mode === 'night' ? 'overnight' : 'day';
		const count = Number(options.count || 1);
		rentPeriod.value = createRentPeriodSelection(mode);
		rentPeriodConfirmed.value = false;
		const packagePrice = Number(options.packagePrice || 0);
		const specPrice = Number(options.specPrice || 0);
		directPackagePrice.value = packagePrice;
		directSpecPrice.value = specPrice;
		const packageItem = packagePrice > 0 ? {
			price: packagePrice
		} : null;
		const specItem = specPrice > 0 ? {
			price: specPrice
		} : null;
		const billing = calcRentItemAmount(goods, mode, rentPeriod.value.startTime, rentPeriod.value.endTime, count,
			packageItem, specItem);
		const unitPrice = getGoodsRentPriceByMode(goods, mode);
		const depositAmount = Number(options.depositAmount || goods.depositAmount || goods.deposit || 0);
		return {
			source: 'direct',
			goodsItems: [{
				id: goodsId,
				name: goods.goodsName || goods.name,
				image: goods.coverImage || goods.image || '/static/logo.png',
				specLabel: options.specLabel || '默认规格',
				packageLabel: options.packageLabel || '默认配置',
				count,
				unitPrice,
				depositAmount,
				days: billing.valid ? billing.units : 0,
				rentModeLabel: getRentTypeLabel(rentType),
				rangeText: '待选择'
			}],
			rentMode: mode,
			rentModeLabel: getRentTypeLabel(rentType),
			rangeText: '待选择',
			days: billing.valid ? billing.units : 0,
			pickupType: 'store',
			store: draft.value.store,
			address: getSelectedAddress(),
			rentAmount: 0,
			depositAmount: depositAmount * count,
			deliveryAmount: 0,
			couponAmount: 0,
			payAmount: 0
		};
	};

	const syncDirectConfirmPayload = () => {
		if (source.value === 'cart' || !confirmPayload.value?.items?.length) return;
		const rentType = rentMode.value === 'hour' ? 'hour' : rentMode.value === 'night' ? 'overnight' : 'day';
		const item = confirmPayload.value.items[0];
		item.rentType = rentType;
		item.startTime = rentPeriod.value.startTime;
		item.endTime = rentPeriod.value.endTime;
		item.days = draft.value.days || 1;
	};

	const syncCartConfirmPayload = (payload = {}) => {
		if (source.value !== 'cart' || !confirmPayload.value?.items?.length) return;
		const rentType = payload.rentType ||
			(rentMode.value === 'hour' ? 'hour' : rentMode.value === 'night' ? 'overnight' : 'day');
		confirmPayload.value.items = confirmPayload.value.items.map((item) => ({
			...item,
			rentType,
			startTime: payload.startTime,
			endTime: payload.endTime,
			days: payload.days
		}));
	};

	const applyRentPeriod = async (payload = {}) => {
		rentMode.value = payload.rentMode || rentMode.value;
		rentPeriod.value = createRentPeriodFromTimes(payload.startTime, payload.endTime, rentMode.value);
		rentPeriodConfirmed.value = true;
		const rentType = rentMode.value === 'hour' ? 'hour' : rentMode.value === 'night' ? 'overnight' : 'day';

		if (source.value === 'cart') {
			const cartIds = confirmPayload.value?.cartIds || [];
			if (cartIds.length) {
				await Promise.all(cartIds.map((id) => updateCartItem({
					id,
					rentType,
					startTime: payload.startTime,
					endTime: payload.endTime
				})));
			}
			syncCartConfirmPayload({
				...payload,
				rentType
			});
			await refreshPreview();
			await loadUsableCouponCount();
			return;
		}

		if (!directGoods.value) return;
		const packageItem = directPackagePrice.value > 0 ? {
			price: directPackagePrice.value
		} : null;
		const specItem = directSpecPrice.value > 0 ? {
			price: directSpecPrice.value
		} : null;
		const count = Number(draft.value.goodsItems?.[0]?.count || 1);
		const billing = calcRentItemAmount(
			directGoods.value,
			rentMode.value,
			rentPeriod.value.startTime,
			rentPeriod.value.endTime,
			count,
			packageItem,
			specItem
		);
		if (!billing.valid) {
			uni.showToast({
				title: billing.message || '租期无效',
				icon: 'none'
			});
			rentPeriodConfirmed.value = false;
			return;
		}
		draft.value.rentAmount = billing.amount;
		draft.value.days = billing.units;
		draft.value.rangeText = formatRentRangeText(rentPeriod.value.startTime, rentPeriod.value.endTime);
		if (draft.value.goodsItems?.[0]) {
			draft.value.goodsItems[0].days = billing.units;
			draft.value.goodsItems[0].rangeText = draft.value.rangeText;
			draft.value.goodsItems[0].rentModeLabel = getRentTypeLabel(billing.rentType);
		}
		syncDirectConfirmPayload();
		await refreshDirectPreview();
		await loadUsableCouponCount();
	};

	const ensureRentPeriodGoods = async () => {
		if (directGoods.value) return directGoods.value;
		const goodsId = source.value === 'cart' ?
			Number(confirmPayload.value?.items?.[0]?.goodsId || 0) :
			Number(draft.value.goodsItems?.[0]?.id || 0);
		if (!goodsId) return null;
		const goods = await fetchGoodsDetail(goodsId);
		directGoods.value = goods;
		return goods;
	};

	const commitRentPeriodChange = async () => {
		const synced = syncRentPeriodTimes(rentPeriod.value, rentMode.value, directGoods.value);
		rentPeriod.value = synced;
		const rentType = rentMode.value === 'hour' ? 'hour' : rentMode.value === 'night' ? 'overnight' : 'day';
		const billing = calcRentBilling(rentType, synced.startTime, synced.endTime);
		if (!billing.valid) {
			rentPeriodConfirmed.value = false;
			resetRentPeriodDraft();
			return;
		}
		try {
			await ensureRentPeriodGoods();
			if (directGoods.value) {
				const stockResult = await checkStock({
					goodsId: directGoods.value.id,
					specId: Number(confirmPayload.value?.items?.[0]?.specId || 0),
					startTime: synced.startTime,
					endTime: synced.endTime,
					quantity: Number(draft.value.goodsItems?.[0]?.count || 1)
				});
				if (!stockResult?.available) {
					uni.showToast({
						title: '所选时段库存不足',
						icon: 'none'
					});
					rentPeriodConfirmed.value = false;
					resetRentPeriodDraft();
					return;
				}
			}
		} catch (error) {
			uni.showToast({
				title: '库存校验失败，请稍后重试',
				icon: 'none'
			});
			return;
		}
		await applyRentPeriod({
			rentMode: rentMode.value,
			rentType,
			startTime: synced.startTime,
			endTime: synced.endTime,
			days: billing.units
		});
	};

	const onStartDateChange = (event) => {
		rentPeriod.value = syncRentPeriodTimes({
			...rentPeriod.value,
			startDate: event.detail.value
		}, rentMode.value, directGoods.value);
		commitRentPeriodChange();
	};

	const onStartClockChange = (event) => {
		rentPeriod.value = syncRentPeriodTimes({
			...rentPeriod.value,
			startClock: event.detail.value
		}, rentMode.value, directGoods.value);
		commitRentPeriodChange();
	};

	const onEndDateChange = (event) => {
		rentPeriod.value = syncRentPeriodTimes({
			...rentPeriod.value,
			endDate: event.detail.value
		}, rentMode.value, directGoods.value);
		commitRentPeriodChange();
	};

	const onEndClockChange = (event) => {
		rentPeriod.value = syncRentPeriodTimes({
			...rentPeriod.value,
			endClock: event.detail.value
		}, rentMode.value, directGoods.value);
		commitRentPeriodChange();
	};

	const applyDefaultRentPeriod = async () => {
		try {
			await ensureRentPeriodGoods();
		} catch (error) {
			return;
		}
		if (!directGoods.value) return;
		await commitRentPeriodChange();
	};

	const resolveAddressId = () => {
		const addressId = draft.value.address?.id;
		return addressId ? Number(addressId) : undefined;
	};

	const buildDraftFromPreview = (preview = {}, options = {}) => {
		const cartMap = new Map((storedCartItems.value || []).map((item) => [Number(item.id), item]));
		const goodsItems = (preview.items || []).map((item) => mapCheckoutPreviewItemToView(
			item,
			cartMap.get(Number(item.cartId)) || {}, {
				rentPeriodConfirmed: options.rentPeriodConfirmed
			}
		));
		return {
			source: 'cart',
			goodsItems,
			rentAmount: Number(preview.rentAmount || 0),
			depositAmount: Number(preview.depositAmount || 0),
			deliveryAmount: Number(preview.deliveryAmount || 0),
			deliveryFeeRemark: preview.deliveryFeeRemark || '',
			couponAmount: Number(preview.couponAmount || 0),
			payAmount: Number(preview.payAmount || 0),
			pickupType: mapPickupTypeFromApi(preview.pickupType),
			store: draft.value.store,
			address: getSelectedAddress()
		};
	};

	const refreshDirectPreview = () => {
		const rentAmount = Number(draft.value.rentAmount || 0);
		const store = draft.value.store;
		const amount = calcDeliveryFeeAmount({
			pickupType: pickupType.value,
			store,
			rentAmount
		});
		draft.value.deliveryAmount = amount;
		const remarkText = buildDeliveryFeeRemark({
			pickupType: pickupType.value,
			store,
			rentAmount
		});
		draft.value.deliveryFeeRemark = remarkText;
		deliveryFeeRemark.value = remarkText;
	};

	const refreshPreview = async () => {
		if (source.value !== 'cart' || !confirmPayload.value) {
			refreshDirectPreview();
			return;
		}
		const requestId = ++previewRequestId;
		previewLoading.value = true;
		try {
			const preview = await checkoutPreview({
				cartIds: confirmPayload.value.cartIds,
				pickupType: pickupType.value,
				pickupStoreId: resolvePickupStoreId(),
				addressId: resolveAddressId(),
				couponId: selectedCouponId.value || undefined
			});
			if (requestId !== previewRequestId) return;
			confirmPayload.value = preview.confirmPayload || preview;
			draft.value = buildDraftFromPreview(preview, {
				rentPeriodConfirmed: rentPeriodConfirmed.value
			});
			deliveryFeeRemark.value = preview.deliveryFeeRemark || '';
		} catch (error) {
			// toast 已处理
		} finally {
			if (requestId === previewRequestId) {
				previewLoading.value = false;
			}
		}
	};

	const loadUsableCouponCount = async () => {
		try {
			const list = await fetchUsableCoupons({
				rentAmount: Number(draft.value.rentAmount || 0),
				deliveryAmount: Number(draft.value.deliveryAmount || 0),
				depositAmount: Number(draft.value.depositAmount || 0)
			});
			usableCouponCount.value = list.length;
			if (selectedCouponId.value && !list.some((item) => Number(item.id) === Number(selectedCouponId.value))) {
				selectedCouponId.value = 0;
				selectedCoupon.value = null;
				await refreshPreview();
			}
		} catch (error) {
			usableCouponCount.value = 0;
		}
	};

	const applyCouponSelectionFromStorage = async () => {
		const selection = consumeConfirmCouponSelection();
		if (selection === undefined) return;
		if (!selection?.id) {
			selectedCouponId.value = 0;
			selectedCoupon.value = null;
		} else {
			selectedCouponId.value = selection.id;
			selectedCoupon.value = selection;
		}
		await refreshPreview();
	};

	const initPage = async (options = {}) => {
		source.value = options.source || '';
		pageLoading.value = true;
		try {
			if (source.value === 'cart') {
				const pending = getPendingConfirmPayload();
				const preview = pending?.preview;
				storedCartItems.value = pending?.cartItems || [];
				if (!preview?.items?.length) {
					uni.showToast({
						title: '请先选择结算商品',
						icon: 'none'
					});
					setTimeout(() => uni.navigateBack(), 600);
					return;
				}
				confirmPayload.value = preview.confirmPayload || preview;
				draft.value = buildDraftFromPreview(preview, {
					rentPeriodConfirmed: false
				});
				pickupType.value = draft.value.pickupType || 'store';
			} else {
				draft.value = await buildDirectBuyDraft(options);
				pickupType.value = draft.value.pickupType || 'store';
				const rentType = rentMode.value === 'hour' ? 'hour' : rentMode.value === 'night' ? 'overnight' : 'day';
				confirmPayload.value = {
					items: [{
						goodsId: Number(options.goodsId || draft.value.goodsItems?.[0]?.id || 0),
						specId: Number(options.specId || 0),
						packageId: options.packageId ? Number(options.packageId) : undefined,
						rentType,
						startTime: '',
						endTime: '',
						days: 0,
						quantity: Number(options.count || 1)
					}],
					cartIds: []
				};
			}
			await loadPickupLocations();
			if (source.value === 'cart') {
				const firstPreviewItem = confirmPayload.value?.items?.[0];
				if (firstPreviewItem?.rentType) {
					rentMode.value = mapApiRentTypeToDetailMode(firstPreviewItem.rentType);
				}
				rentPeriod.value = createRentPeriodSelection(rentMode.value);
				rentPeriodConfirmed.value = false;
				draft.value.rentAmount = 0;
				draft.value.couponAmount = 0;
				draft.value.payAmount = calcOrderPayableAmount({
					rentAmount: 0,
					deliveryAmount: 0,
					couponAmount: 0
				});
			}
			contactName.value = draft.value.contactName || draft.value.address?.name || '';
			contactPhone.value = draft.value.contactPhone || draft.value.address?.phone || '';
			if (source.value !== 'cart') {
				refreshDirectPreview();
			}
			await loadUsableCouponCount();
		} catch (error) {
			uni.showToast({
				title: error.message || '加载订单信息失败',
				icon: 'none'
			});
			setTimeout(() => uni.navigateBack(), 600);
		} finally {
			pageLoading.value = false;
			if (!rentPeriodConfirmed.value) {
				setTimeout(() => {
					applyDefaultRentPeriod();
				}, 320);
			}
		}
	};

	onLoad((options) => {
		initPage(options);
	});

	onShow(async () => {
		await applyCouponSelectionFromStorage();
		await loadUsableCouponCount();
		await loadDepositSummary();
		if (pickupType.value === 'delivery') {
			let selectedAddress = getSelectedAddress();
			if (!selectedAddress) {
				try {
					const list = await fetchAddressList({
						silentError: true
					});
					selectedAddress = list.find((item) => item.default) || list[0] || null;
				} catch (error) {
					selectedAddress = null;
				}
			}
			if (selectedAddress) {
				draft.value.address = selectedAddress;
				contactName.value = selectedAddress.name;
				contactPhone.value = selectedAddress.phone;
			}
			await refreshPreview();
		}
	});

	const couponEntryLabel = computed(() => {
		if (selectedCoupon.value?.name) return selectedCoupon.value.name;
		if (usableCouponCount.value) return `${usableCouponCount.value} 张可用`;
		return '暂无可用优惠券';
	});

	const couponAmount = computed(() => {
		if (source.value === 'cart' && selectedCouponId.value) {
			return formatMoney(draft.value.couponAmount || selectedCoupon.value?.amount || 0);
		}
		return formatMoney(selectedCoupon.value?.amount || 0);
	});

	const rentAmount = computed(() => formatMoney(draft.value.rentAmount));
	const depositAmount = computed(() => formatMoney(draft.value.depositAmount));
	const depositAvailableAmount = computed(() => formatMoney(depositSummary.value.availableAmount || 0));
	const deliveryAmount = computed(() => formatMoney(draft.value.deliveryAmount));

	const loadDepositSummary = async () => {
		if (!isLoggedIn()) {
			depositSummaryLoaded.value = false;
			return;
		}
		try {
			const summary = await fetchDepositSummary({
				silentError: true
			});
			depositSummary.value = {
				availableAmount: Number(summary?.availableAmount || 0),
				minDepositBalance: Number(summary?.minDepositBalance || 0)
			};
			depositSummaryLoaded.value = true;
		} catch (error) {
			depositSummaryLoaded.value = false;
		}
	};

	const ensureDepositSufficient = () => {
		const required = Number(draft.value.depositAmount || 0);
		if (required <= 0) return true;
		const available = Number(depositSummary.value.availableAmount || 0);
		const minBalance = Number(depositSummary.value.minDepositBalance || 0);
		const threshold = Math.max(required, minBalance);
		if (available >= threshold) {
			return true;
		}
		if (minBalance > 0 && available < minBalance) {
			uni.showModal({
				title: '押金不足',
				content: `账户可用押金需不低于 ¥${formatMoney(minBalance)}，请先补充押金后再下单`,
				confirmText: '去缴纳',
				success: (res) => {
					if (res.confirm) {
						uni.navigateTo({
							url: '/packageContent/deposit'
						});
					}
				}
			});
			return false;
		}
		uni.showModal({
			title: '押金不足',
			content: `本单需冻结押金 ¥${formatMoney(required)}，当前可用 ¥${formatMoney(available)}，请先缴纳押金`,
			confirmText: '去缴纳',
			success: (res) => {
				if (res.confirm) {
					uni.navigateTo({
						url: '/packageContent/deposit'
					});
				}
			}
		});
		return false;
	};

	const totalAmount = computed(() => {
		if (source.value === 'cart' && !rentPeriodConfirmed.value) {
			return calcOrderPayableAmount({
				rentAmount: 0,
				deliveryAmount: 0,
				couponAmount: 0
			});
		}
		if (source.value === 'cart' && selectedCouponId.value && draft.value.payAmount !== undefined && draft.value
			.payAmount !== null) {
			return formatMoney(draft.value.payAmount);
		}
		const activeCouponAmount = selectedCouponId.value ?
			(source.value === 'cart' ? draft.value.couponAmount : selectedCoupon.value?.amount) || 0 :
			0;
		return calcOrderPayableAmount({
			rentAmount: draft.value.rentAmount,
			deliveryAmount: draft.value.deliveryAmount,
			couponAmount: activeCouponAmount
		});
	});

	const handlePickupChange = async (item) => {
		pickupType.value = item.key;
		draft.value.pickupType = pickupType.value;
		if (pickupType.value === 'delivery') {
			const selectedAddress = getSelectedAddress();
			draft.value.address = selectedAddress;
			contactName.value = selectedAddress?.name || '';
			contactPhone.value = selectedAddress?.phone || '';
		}
		await refreshPreview();
		await loadUsableCouponCount();
	};

	const openPickupSelector = () => {
		if (pickupType.value === 'delivery') {
			uni.navigateTo({
				url: '/packageUser/address-list?select=1'
			});
			return;
		}
		if (!storeOptions.value.length) {
			uni.showToast({
				title: '暂无可选门店，请稍后再试',
				icon: 'none'
			});
			return;
		}
		uni.showActionSheet({
			itemList: storeOptions.value.map((item) => item.name),
			success: ({
				tapIndex
			}) => {
				const selectedLocation = storeOptions.value[tapIndex];
				if (!selectedLocation) return;
				draft.value.store = selectedLocation;
				refreshPreview();
			}
		});
	};

	const openCouponSelector = async () => {
		await loadUsableCouponCount();
		if (!usableCouponCount.value) {
			uni.showToast({
				title: '暂无可用优惠券',
				icon: 'none'
			});
			return;
		}
		setConfirmCouponContext({
			rentAmount: draft.value.rentAmount,
			depositAmount: draft.value.depositAmount,
			deliveryAmount: draft.value.deliveryAmount,
			selectedCouponId: selectedCouponId.value
		});
		uni.navigateTo({
			url: '/packageUser/coupon?select=1'
		});
	};

	const clearCoupon = async () => {
		selectedCouponId.value = 0;
		selectedCoupon.value = null;
		setConfirmCouponSelection(null);
		if (confirmPayload.value) {
			confirmPayload.value = {
				...confirmPayload.value,
				couponId: undefined
			};
		}
		draft.value = {
			...draft.value,
			couponAmount: 0,
			payAmount: calcOrderPayableAmount({
				rentAmount: draft.value.rentAmount,
				deliveryAmount: draft.value.deliveryAmount,
				couponAmount: 0
			})
		};
		await refreshPreview();
	};

	const buildCreatePayload = () => ({
		items: confirmPayload.value?.items || [],
		confirmPayload: confirmPayload.value,
		pickupType: pickupType.value,
		pickupStoreId: resolvePickupStoreId(),
		addressId: pickupType.value === 'delivery' ? resolveAddressId() : undefined,
		contactName: contactName.value,
		contactPhone: contactPhone.value,
		couponId: selectedCouponId.value || undefined,
		remark: remark.value,
		payChannel: payMethod.value,
		cartIds: confirmPayload.value?.cartIds
	});

	const submitOrder = async () => {
		if (submitting.value) return;
		if (!rentPeriodConfirmed.value) {
			uni.showToast({
				title: '请先选择取还时间',
				icon: 'none'
			});
			return;
		}
		const rentType = rentMode.value === 'hour' ? 'hour' : rentMode.value === 'night' ? 'overnight' : 'day';
		const billing = calcRentBilling(rentType, rentPeriod.value.startTime, rentPeriod.value.endTime);
		if (!billing.valid) {
			uni.showToast({
				title: billing.message || '请选择有效租期',
				icon: 'none'
			});
			return;
		}
		const validation = validateConfirmSubmit({
			contactName: contactName.value,
			contactPhone: contactPhone.value,
			agreed: agreed.value,
			pickupType: pickupType.value,
			address: draft.value.address,
			store: draft.value.store
		});
		if (!validation.ok) {
			uni.showToast({
				title: validation.message,
				icon: 'none'
			});
			return;
		}
		const redirectUrl = source.value === 'cart' ?
			'/packageOrder/confirm?source=cart' :
			`/packageOrder/confirm?goodsId=${draft.value.goodsItems[0]?.id}&rentMode=${draft.value.rentMode || 'day'}`;
		if (!ensureLogin(redirectUrl)) return;
		await loadDepositSummary();
		if (!ensureDepositSufficient()) return;
		paymentPopupVisible.value = true;
	};

	const confirmPayAndSubmit = async (methodKey) => {
		if (submitting.value) return;
		payMethod.value = methodKey || payMethod.value;
		submitting.value = true;
		await runAction({
			action: async () => {
				await requestOrderSubscribeMessage();
				const orderResult = await createOrder(buildCreatePayload());
				const payRes = await launchPayment({
					amount: orderResult.payAmount,
					orderNo: orderResult.orderNo,
					methodKey: payMethod.value
				});
				return {
					orderResult,
					payRes
				};
			},
			errorText: '下单失败，请稍后重试',
			onSuccess: ({
				payRes
			}) => {
				paymentPopupVisible.value = false;
				clearPendingConfirmPayload();
				navigateAfterPayment({
					payRes,
					businessType: 'rent_order'
				});
			}
		});
		submitting.value = false;
	};

	const openAgreement = (type) => {
		uni.navigateTo({
			url: `/packageContent/agreement?type=${type}`
		});
	};
</script>

<style scoped>
	.page {
		box-sizing: border-box;
		height: calc(100vh - 128rpx);
		padding: 24rpx 24rpx calc(180rpx + env(safe-area-inset-bottom));
		background: linear-gradient(180deg, #f3fae9 0%, #f8fcf2 20%, #ffffff 44%, #ffffff 100%);
		overflow-x: hidden;
	}

	.page-loading {
		padding: 120rpx 0;
		text-align: center;
		font-size: 24rpx;
		color: #8a9a7a;
	}

	.section-card_ {
		box-sizing: border-box;
		width: 100%;
		position: relative;
		margin-bottom: 20rpx;
		padding: 24rpx 22rpx;
		background: linear-gradient(180deg, #ffffff 0%, #fcfff8 100%);
		border: 1rpx solid rgba(226, 235, 211, 0.92);
		border-radius: 24rpx;
		box-shadow: 0 8rpx 22rpx rgba(34, 45, 25, 0.04);
		overflow: hidden;
	}

	.section-card {
		box-sizing: border-box;
		width: 100%;
		position: relative;
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

	.section-title,
	.goods-name,
	.goods-meta,
	.goods-price,
	.info-title,
	.info-desc,
	.entry-label,
	.entry-value,
	.entry-discount,
	.field-label,
	.agree-text,
	.bottom-label,
	.bottom-price {
		display: block;
	}

	.section-title {
		font-size: 28rpx;
		font-weight: 600;
		color: #1a2114;
	}

	.goods-row {
		display: flex;
		align-items: stretch;
		margin-top: 10rpx;
		padding: 18rpx;
		background: linear-gradient(180deg, #ffffff 0%, #fbfef5 100%);
		border: 1rpx solid rgba(231, 237, 220, 0.9);
		border-radius: 20rpx;
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

	.goods-price {
		margin-top: 12rpx;
		font-size: 26rpx;
		font-weight: 600;
		color: #69b21b;
	}

	.tab-wrap {
		margin-top: 8rpx;
		padding: 4rpx 0;
	}

	.tab-wrap :deep(.u-tabs__wrapper__nav__item) {
		padding: 0 16rpx;
	}

	.tab-wrap :deep(.u-tabs__wrapper__nav__item__text) {
		font-size: 22rpx;
		white-space: nowrap;
	}

	.tab-wrap :deep(.u-tabs__wrapper__nav__line) {
		height: 5rpx !important;
		border-radius: 999rpx;
	}

	.pickup-card {
		padding-bottom: 8rpx;
	}

	.pickup-segment {
		display: flex;
		gap: 0;
		padding: 6rpx;
		border-radius: 999rpx;
		background: #f0f4ea;
	}

	.segment-btn {
		flex: 1;
		height: 64rpx;
		line-height: 64rpx;
		text-align: center;
		border-radius: 999rpx;
		font-size: 24rpx;
		color: #71806a;
		transition: all 0.2s ease;
	}

	.segment-btn.active {
		font-weight: 600;
		color: #ffffff;
		background: linear-gradient(135deg, #7ed321 0%, #59b50e 100%);
		box-shadow: 0 6rpx 16rpx rgba(89, 181, 14, 0.28);
	}

	.rent-mode-block {
		margin-top: 20rpx;
		padding: 22rpx 20rpx;
		border-radius: 20rpx;
		background: linear-gradient(180deg, #fbfef4 0%, #f5fae8 100%);
		border: 1rpx solid rgba(212, 229, 180, 0.88);
	}

	.rent-mode-label {
		display: block;
		font-size: 24rpx;
		color: #53634a;
	}

	.rent-mode-segment {
		display: flex;
		gap: 12rpx;
		margin-top: 16rpx;
	}

	.mode-btn {
		flex: 1;
		height: 64rpx;
		line-height: 64rpx;
		text-align: center;
		border-radius: 999rpx;
		font-size: 24rpx;
		color: #71806a;
		background: #f0f4ea;
		transition: all 0.2s ease;
	}

	.mode-btn.active {
		font-weight: 600;
		color: #ffffff;
		background: linear-gradient(135deg, #7ed321 0%, #59b50e 100%);
		box-shadow: 0 6rpx 16rpx rgba(89, 181, 14, 0.28);
	}

	.mode-btn.disabled {
		opacity: 0.72;
	}

	.rent-mode-tip {
		display: block;
		margin-top: 12rpx;
		font-size: 20rpx;
		line-height: 1.5;
		color: #8a9a7a;
	}

	.pickup-location-row {
		display: flex;
		align-items: center;
		gap: 16rpx;
		margin-top: 20rpx;
		padding: 8rpx 0 4rpx;
	}

	.pickup-time-row {
		display: flex;
		align-items: center;
		gap: 16rpx;
		margin-top: 20rpx;
		padding: 8rpx 0 4rpx;
	}

	.pickup-time-row:last-of-type {
		margin-bottom: 4rpx;
	}

	.pickup-row-icon {
		flex-shrink: 0;
		width: 44rpx;
		height: 44rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		border-radius: 12rpx;
		background: rgba(89, 181, 14, 0.1);
	}

	.pickup-row-main {
		flex: 1;
		min-width: 0;
	}

	.pickup-row-title {
		display: block;
		font-size: 26rpx;
		font-weight: 600;
		color: #1a2114;
		line-height: 1.4;
	}

	.pickup-row-desc {
		display: block;
		margin-top: 6rpx;
		font-size: 20rpx;
		color: #66775b;
		line-height: 1.5;
	}

	.pickup-row-label {
		flex-shrink: 0;
		font-size: 24rpx;
		color: #53634a;
	}

	.pickup-row-pickers {
		flex: 1;
		min-width: 0;
		display: flex;
		align-items: center;
		justify-content: flex-end;
		gap: 12rpx;
	}

	.pickup-row-value {
		flex: 1;
		min-width: 0;
		font-size: 24rpx;
		font-weight: 500;
		color: #1a2114;
		text-align: right;
	}

	.pickup-row-value.picker-value {
		flex: none;
		padding: 0;
		border: none;
		background: transparent;
		color: #4a8f0f;
		font-weight: 600;
	}

	.pickup-time-row.readonly .pickup-row-value {
		padding: 0;
		border: none;
		background: transparent;
		color: #1a2114;
		font-weight: 500;
	}

	.billing-rule-tip {
		display: block;
		margin-top: 16rpx;
		padding: 14rpx 18rpx;
		border-radius: 14rpx;
		font-size: 20rpx;
		line-height: 1.6;
		color: #7a8b6e;
		background: rgba(247, 251, 240, 0.9);
	}

	.info-box {
		box-sizing: border-box;
		margin-top: 14rpx;
		padding: 20rpx 22rpx;
		border-radius: 20rpx;
		background: linear-gradient(180deg, #fbfef4 0%, #f5fae8 100%);
		border: 1rpx solid rgba(212, 229, 180, 0.88);
	}

	.info-head {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 12rpx;
	}

	.info-title {
		font-size: 24rpx;
		font-weight: 600;
		color: #1a2114;
	}

	.info-action {
		flex-shrink: 0;
		font-size: 20rpx;
		color: #63aa18;
		font-weight: 500;
	}

	.info-desc {
		margin-top: 8rpx;
		font-size: 20rpx;
		line-height: 1.55;
		color: #66775b;
	}

	.coupon-entry-card::before {
		display: none;
	}

	.coupon-entry {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 16rpx;
	}

	.entry-label {
		font-size: 26rpx;
		font-weight: 600;
		color: #1a2114;
		flex-shrink: 0;
	}

	.entry-right {
		flex: 1;
		min-width: 0;
		display: flex;
		align-items: center;
		justify-content: flex-end;
		gap: 8rpx;
	}

	.entry-value {
		font-size: 22rpx;
		color: #8a9a7a;
		text-align: right;
	}

	.entry-value.active {
		color: #1a2114;
		font-weight: 500;
	}

	.entry-discount {
		font-size: 22rpx;
		color: #ff7a45;
		font-weight: 600;
		flex-shrink: 0;
	}

	.coupon-clear {
		display: block;
		margin-top: 12rpx;
		font-size: 20rpx;
		color: #63aa18;
		text-align: right;
	}

	.field-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-top: 14rpx;
		padding: 18rpx 20rpx;
		border: 1rpx solid rgba(229, 236, 215, 0.9);
		border-radius: 18rpx;
		background: linear-gradient(180deg, #ffffff 0%, #fbfef5 100%);
	}

	.field-label {
		font-size: 26rpx;
		color: #8fa07d;
		margin-bottom: 6rpx;
	}

	.field-input {
		height: 64rpx;
		font-size: 24rpx;
		color: #1a2114;
		font-weight: 500;
		text-align: right;
	}

	.remark-box {
		margin-top: 14rpx;
		padding: 18rpx 20rpx;
		border-radius: 18rpx;
		background: linear-gradient(180deg, #fbfef5 0%, #f7fbe9 100%);
		border: 1rpx solid rgba(229, 236, 215, 0.92);
	}

	.remark-input {
		width: 100%;
		height: 130rpx;
		font-size: 24rpx;
		line-height: 1.55;
		color: #1a2114;
	}

	.price-line {
		display: flex;
		justify-content: space-between;
		align-items: center;
		margin-top: 14rpx;
		font-size: 22rpx;
		color: #4a5540;
	}

	.price-line text:last-child {
		font-weight: 500;
	}

	.price-line.discount {
		color: #ff7a45;
	}

	.deposit-account-line text:last-child {
		font-size: 24rpx;
		color: #6b7280;
		font-weight: 400;
	}

	.delivery-tip {
		display: block;
		margin-top: 12rpx;
		font-size: 20rpx;
		line-height: 1.6;
		color: #8a9a7a;
	}

	.price-line.total {
		margin-top: 18rpx;
		padding: 18rpx 20rpx;
		border-radius: 18rpx;
		background: linear-gradient(135deg, #fbfef4 0%, #eef8d5 100%);
		font-size: 24rpx;
		font-weight: 600;
		color: #1a2114;
	}

	.price-line.total text:last-child {
		font-size: 30rpx;
		font-weight: 700;
		color: #69b21b;
	}

	.agreement-card {
		display: flex;
		align-items: flex-start;
		padding: 20rpx 22rpx;
		background: linear-gradient(180deg, #fbfef4 0%, #f6faea 100%);
	}

	.agree-icon {
		display: flex;
		align-items: center;
		justify-content: center;
		width: 32rpx;
		height: 32rpx;
		flex-shrink: 0;
		margin-top: 2rpx;
	}

	.agree-text {
		flex: 1;
		min-width: 0;
		margin-left: 10rpx;
		font-size: 20rpx;
		line-height: 1.55;
		color: #6b7a5e;
	}

	.agree-link {
		color: #5aad15;
		font-weight: 500;
	}

	.bottom-bar {
		box-sizing: border-box;
		position: fixed;
		left: 24rpx;
		right: 24rpx;
		bottom: calc(20rpx + env(safe-area-inset-bottom));
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 14rpx;
		padding: 16rpx 22rpx;
		background: rgba(255, 255, 255, 0.94);
		border: 1rpx solid rgba(222, 234, 202, 0.92);
		border-radius: 28rpx;
		box-shadow: 0 10rpx 26rpx rgba(30, 40, 24, 0.08);
		backdrop-filter: blur(18rpx);
		z-index: 100;
	}

	.bottom-bar>view {
		flex: 1;
		min-width: 0;
	}

	.bottom-label {
		font-size: 20rpx;
		color: #8ea07c;
	}

	.bottom-price {
		font-size: 36rpx;
		font-weight: 700;
		color: #69b21b;
		margin-top: 2rpx;
	}

	.submit-btn {
		min-width: 0;
		width: auto;
		padding: 0 36rpx;
		flex-shrink: 0;
		height: 76rpx;
		line-height: 76rpx;
		margin: 0;
		border-radius: 38rpx;
		font-size: 26rpx;
		font-weight: 600;
		color: #ffffff;
		background: linear-gradient(135deg, #7ed321, #6bc01a);
		box-shadow: 0 6rpx 16rpx rgba(126, 211, 33, 0.24);
		border: none;
	}
</style>