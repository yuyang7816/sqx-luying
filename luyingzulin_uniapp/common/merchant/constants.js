import {
	formatOrderDateTime,
	formatRentRangeText,
	getReturnMethodLabel,
	getRentTypeLabel,
	mapPickupTypeFromApi
} from '@/common/rental/constants';

/** 入驻流程步骤（静态 UI） */
export const MERCHANT_STEPS = [
	{ key: 'submit', label: '提交申请', desc: '填写商家信息、门店信息和证照资料' },
	{ key: 'review', label: '平台审核', desc: '平台核验资质、经营范围和联系人信息' },
	{ key: 'open', label: '开通经营', desc: '审核通过后可上架商品并处理订单' }
];

/** 商家工作台快捷入口 */
export const MERCHANT_MENUS = [
	{ label: '商品管理', icon: 'grid', url: '/packageMerchant/goods' },
	{ label: '订单处理', icon: 'order', url: '/packageMerchant/orders' },
	{ label: '商户钱包', icon: 'red-packet', url: '/packageMerchant/wallet' },
	{ label: '门店设置', icon: 'setting', url: '/packageMerchant/store' },
	{ label: '入驻进度', icon: 'hourglass', url: '/packageMerchant/progress' }
];

/** 商户商品 Tab */
export const MERCHANT_GOODS_TABS = [
	{ key: 'all', label: '全部' },
	{ key: 'on', label: '上架中' },
	{ key: 'off', label: '已下架' }
];

/** 商户订单 Tab */
export const MERCHANT_ORDER_TABS = [
	{ key: 'all', label: '全部' },
	{ key: 'pending', label: '待备货' },
	{ key: 'running', label: '租赁中' },
	{ key: 'review', label: '待验收' },
	{ key: 'completed', label: '已完成' }
];

export const MERCHANT_APPLY_STATUS = {
	PENDING: 'pending',
	REVIEWING: 'reviewing',
	APPROVED: 'approved',
	REJECTED: 'rejected'
};

const APPLY_STATUS_TAG = {
	pending: '待提交',
	reviewing: '资料复核中',
	approved: '已开通',
	rejected: '审核驳回'
};

/** 是否已通过入驻审核 */
export function isMerchantApproved(status) {
	return status === MERCHANT_APPLY_STATUS.APPROVED;
}

/** 是否可进入商户工作台 */
export function canAccessMerchantWorkbench(status) {
	return isMerchantApproved(status);
}

/** 是否为启用中的核销员工 */
export function isMerchantStaffActive(profile = {}) {
	return Boolean(profile?.active);
}

/** 是否应展示审核状态页（审核中） */
export function shouldShowApplyStatusPage(status) {
	return status === MERCHANT_APPLY_STATUS.REVIEWING;
}

/** 是否可重新提交入驻申请 */
export function canResubmitMerchantApply(status) {
	return status === MERCHANT_APPLY_STATUS.REJECTED;
}

const GOODS_STATUS_MAP = {
	1: { status: '在架', statusType: 'online', shelfStatus: true },
	0: { status: '已下架', statusType: 'offline', shelfStatus: false }
};

/** 商品配送方式选项（可多选） */
export const GOODS_DELIVERY_OPTIONS = [
	{ key: 'store', label: '门店自提', desc: '用户到店取货核销' },
	{ key: 'delivery', label: '同城配送', desc: '按门店配送规则送达' }
];

export function createDefaultGoodsDeliveryModes() {
	return { store: true, delivery: true };
}

/** 配送方式 → 展示文案 */
export function formatGoodsDeliveryLabel(deliveryModes = {}) {
	const labels = GOODS_DELIVERY_OPTIONS
		.filter((item) => deliveryModes[item.key])
		.map((item) => item.label);
	return labels.length ? labels.join(' / ') : '请选择配送方式';
}

/** 兼容旧文案 → 配送方式对象 */
export function parseGoodsDeliveryModes(value) {
	const defaults = createDefaultGoodsDeliveryModes();
	if (!value) return defaults;
	if (typeof value === 'object' && !Array.isArray(value)) {
		return {
			store: value.store !== false,
			delivery: value.delivery !== false
		};
	}
	const text = String(value);
	return {
		store: text.includes('自提') || text.includes('门店'),
		delivery: text.includes('配送') || text.includes('送达')
	};
}

export const resolvePageRecords = (page = {}) => ({
	list: page.records || page.list || [],
	total: Number(page.total || 0)
});

export const formatMoney = (value) => {
	const num = Number(value);
	if (Number.isNaN(num)) return '0.0';
	return num.toFixed(1);
};

export const maskPhone = (phone = '') => {
	const text = String(phone || '');
	if (text.length < 7) return text || '--';
	return `${text.slice(0, 3)}****${text.slice(-4)}`;
};

export const parseMerchantOrderId = (order = {}) => order.merchantOrderNo || order.id;

export const parseRentOrderId = (order = {}) => order.rentOrderId || order.originOrderId;

/** 入驻进度 → 首页/进度页摘要 */
export function mapApplyProgressToSummary(vo = {}) {
	const status = vo.status || 'pending';
	return {
		status,
		statusLabel: vo.statusLabel || APPLY_STATUS_TAG[status] || '未提交',
		tag: APPLY_STATUS_TAG[status] || '待处理',
		storeName: vo.storeName || vo.merchantName || '—',
		contactName: vo.contactName || '—',
		contactPhone: maskPhone(vo.contactPhone),
		contactPhoneRaw: vo.contactPhone || '',
		category: (vo.businessScopes || [])[0] || '露营装备租赁',
		applyTime: vo.timeline?.[0]?.time || '—',
		serviceArea: vo.serviceArea || '—',
		businessHours: vo.businessHours || '09:00-21:00',
		rejectReason: vo.rejectReason || '',
		merchantId: vo.merchantId,
		storeId: vo.storeId
	};
}

/** 入驻进度时间轴 */
export function mapApplyProgressTimeline(vo = {}) {
	return (vo.timeline || []).map((item) => ({
		title: item.title,
		time: item.time,
		desc: item.desc || ''
	}));
}

/** 商品列表 VO → 页面模型 */
export function mapGoodsListVoToView(vo = {}, shelfHint) {
	const shelf = GOODS_STATUS_MAP[vo.status] || shelfHint || {
		status: vo.availableStock > 0 ? '在架' : '已下架',
		statusType: vo.availableStock > 0 ? 'online' : 'offline',
		shelfStatus: vo.availableStock > 0
	};
	return {
		id: vo.id,
		goodsNo: vo.goodsNo,
		name: vo.name,
		coverImage: vo.coverImage || '',
		price: formatMoney(vo.rentPriceDay ?? vo.rentPrice),
		stock: String(vo.availableStock ?? 0),
		status: shelf.status,
		statusType: shelf.statusType,
		category: vo.categoryName || '—',
		categoryId: vo.categoryId,
		deposit: formatMoney(vo.depositAmount),
		deliveryModes: createDefaultGoodsDeliveryModes(),
		pickup: formatGoodsDeliveryLabel(createDefaultGoodsDeliveryModes()),
		desc: vo.subtitle || vo.detail || '',
		images: vo.images || (vo.coverImage ? [vo.coverImage] : []),
		statusReason: shelf.shelfStatus ? '商品已上架，用户端可见。' : '当前未上架，可在商品列表重新上架。',
		shelfStatus: shelf.shelfStatus,
		rawStatus: vo.status
	};
}

/** 商品详情 → 编辑表单 */
export function mapGoodsDetailToForm(vo = {}) {
	const shelf = GOODS_STATUS_MAP[vo.status] || { shelfStatus: false };
	const specs = (vo.specs || []).map((item) => ({
		specName: item.name || item.specName || '',
		specPrice: parseSpecPriceFromVo(item)
	}));
	const params = (vo.params || []).map((item) => ({
		paramName: item.name || item.paramName || '',
		paramValue: item.value || item.paramValue || ''
	}));
	const packages = (vo.packages || []).map((item) => ({
		packageName: item.packageName || '',
		packageDesc: item.packageDesc || '',
		price: item.price != null ? String(item.price) : '0'
	}));
	return {
		name: vo.name || '',
		category: vo.categoryName || '',
		categoryId: vo.categoryId,
		desc: vo.detail || vo.subtitle || '',
		price: vo.rentPriceDay != null ? String(vo.rentPriceDay) : '',
		priceHour: vo.rentPriceHour != null ? String(vo.rentPriceHour) : '',
		priceOvernight: vo.rentPriceOvernight != null ? String(vo.rentPriceOvernight) : '',
		overnightStartTime: vo.overnightStartTime || DEFAULT_OVERNIGHT_START,
		overnightEndTime: vo.overnightEndTime || DEFAULT_OVERNIGHT_END,
		deposit: vo.depositAmount != null ? String(vo.depositAmount) : '',
		stock: String(vo.availableStock ?? vo.totalStock ?? 0),
		deliveryModes: createDefaultGoodsDeliveryModes(),
		pickup: formatGoodsDeliveryLabel(createDefaultGoodsDeliveryModes()),
		shelfStatus: shelf.shelfStatus,
		coverImage: vo.coverImage || '',
		tutorialVideoUrl: vo.tutorialVideoUrl || '',
		images: vo.images || [],
		rentModes: {
			day: true,
			hour: Number(vo.rentPriceHour) > 0,
			overnight: Number(vo.rentPriceOvernight) > 0
		},
		specs: specs.length ? specs : [{ specName: '', specPrice: '' }],
		params: params.length ? params : [{ paramName: '', paramValue: '' }],
		packages: packages.length ? packages : [{ packageName: '基础配套', packageDesc: '', price: '0' }]
	};
}

export const DEFAULT_OVERNIGHT_START = '18:00';
export const DEFAULT_OVERNIGHT_END = '10:00';

export function formatOvernightTimeLabel(start, end) {
	return `${start || DEFAULT_OVERNIGHT_START} - 次日${end || DEFAULT_OVERNIGHT_END}`;
}

export function parseSpecPriceFromVo(item = {}) {
	if (item.price != null && item.price !== '') return String(item.price);
	const raw = item.value ?? item.specValue ?? '';
	if (/^\d+(\.\d+)?$/.test(String(raw).trim())) return String(raw).trim();
	return '';
}

export function normalizeGoodsSpecs(rows = []) {
	return (rows || [])
		.filter((item) => item.specName?.trim() && String(item.specPrice ?? item.specValue ?? '').trim())
		.map((item, index) => {
			const price = Number(item.specPrice ?? item.specValue);
			return {
				specName: item.specName.trim(),
				specPrice: Number.isFinite(price) ? price : 0,
				specType: 1,
				sort: index + 1
			};
		});
}

export function normalizeGoodsParams(rows = []) {
	return (rows || [])
		.filter((item) => item.paramName?.trim() && item.paramValue?.trim())
		.map((item, index) => ({
			specName: item.paramName.trim(),
			specValue: item.paramValue.trim(),
			specType: 2,
			sort: index + 1
		}));
}

export function normalizeGoodsPackages(rows = []) {
	return (rows || [])
		.filter((item) => item.packageName?.trim())
		.map((item, index) => ({
			packageName: item.packageName.trim(),
			packageDesc: item.packageDesc?.trim() || '',
			price: Number(item.price) || 0,
			sort: index + 1
		}));
}

export function createEmptyGoodsEditForm() {
	return {
		name: '',
		category: '',
		desc: '',
		price: '',
		priceHour: '',
		priceOvernight: '',
		overnightStartTime: DEFAULT_OVERNIGHT_START,
		overnightEndTime: DEFAULT_OVERNIGHT_END,
		deposit: '',
		stock: '',
		deliveryModes: createDefaultGoodsDeliveryModes(),
		pickup: formatGoodsDeliveryLabel(createDefaultGoodsDeliveryModes()),
		shelfStatus: true,
		rootCategoryId: null,
		rootCategoryName: '',
		subCategoryId: null,
		subCategoryName: '',
		categoryId: null,
		tutorialVideoUrl: '',
		rentModes: { day: true, hour: false, overnight: false },
		specs: [{ specName: '', specPrice: '' }],
		params: [{ paramName: '', paramValue: '' }],
		packages: [{ packageName: '基础配套', packageDesc: '标准出厂配件', price: '0' }]
	};
}

/** 编辑表单 → 保存 BO */
export function mapGoodsFormToSaveBo(form = {}, options = {}) {
	const categoryId = form.categoryId || options.defaultCategoryId;
	const images = form.images || [];
	const specs = normalizeGoodsSpecs(form.specs);
	const params = normalizeGoodsParams(form.params);
	const packages = normalizeGoodsPackages(form.packages);
	const body = {
		name: form.name?.trim(),
		categoryId,
		subtitle: form.desc?.slice(0, 60) || '',
		detail: form.desc || '',
		coverImage: form.coverImage || images[0] || '',
		tutorialVideoUrl: form.tutorialVideoUrl?.trim() || '',
		rentPriceDay: Number(form.price) || 0,
		rentPriceHour: form.rentModes?.hour ? Number(form.priceHour) || 0 : 0,
		rentPriceOvernight: form.rentModes?.overnight ? Number(form.priceOvernight) || 0 : 0,
		overnightStartTime: form.rentModes?.overnight ? (form.overnightStartTime || DEFAULT_OVERNIGHT_START) : undefined,
		overnightEndTime: form.rentModes?.overnight ? (form.overnightEndTime || DEFAULT_OVERNIGHT_END) : undefined,
		depositAmount: Number(form.deposit) || 0,
		status: options.autoShelf ? 1 : (form.shelfStatus ? 1 : 0),
		images,
		totalStock: Number(form.stock) || 0,
		specs: specs.length ? specs : undefined,
		params: params.length ? params : undefined,
		packages: packages.length ? packages : undefined
	};
	if (options.id) {
		return { id: options.id, ...body };
	}
	return body;
}

/** 商家商品表单校验 */
export function validateGoodsForm(form = {}, images = [], categoryTree = []) {
	const name = form.name?.trim();
	if (!name) return '请输入商品名称';
	const categoryMessage = validateGoodsCategorySelection(form, categoryTree);
	if (categoryMessage) return categoryMessage;
	if (!images?.length) return '请至少上传 1 张商品图片';
	const price = Number(form.price);
	if (!Number.isFinite(price) || price <= 0) return '请填写整日租价格';
	if (form.rentModes?.hour) {
		const hourPrice = Number(form.priceHour);
		if (!Number.isFinite(hourPrice) || hourPrice <= 0) return '请填写小时租价格';
	}
	if (form.rentModes?.overnight) {
		const overnightPrice = Number(form.priceOvernight);
		if (!Number.isFinite(overnightPrice) || overnightPrice <= 0) return '请填写过夜租价格';
		if (!form.overnightStartTime || !form.overnightEndTime) return '请设置过夜租时段';
		if (form.overnightStartTime === form.overnightEndTime) return '过夜租开始与结束时间不能相同';
	}
	const deposit = Number(form.deposit);
	if (!Number.isFinite(deposit) || deposit < 0) return '请输入有效的押金金额';
	const stock = Number(form.stock);
	if (!Number.isInteger(stock) || stock <= 0) return '请输入有效的可租库存（正整数）';
	const deliveryModes = parseGoodsDeliveryModes(form.deliveryModes ?? form.pickup);
	if (!deliveryModes.store && !deliveryModes.delivery) return '请至少选择一种配送方式';
	const packages = normalizeGoodsPackages(form.packages);
	if (!packages.length) return '请至少添加 1 项配套设备';
	const invalidPackage = (form.packages || []).find((item) => item.packageName?.trim() && !item.packageDesc?.trim());
	if (invalidPackage) return '请填写配套设备说明';
	const partialSpec = (form.specs || []).find((item) => {
		const hasName = !!item.specName?.trim();
		const priceText = String(item.specPrice ?? item.specValue ?? '').trim();
		const hasPrice = priceText !== '';
		return (hasName && !hasPrice) || (!hasName && hasPrice);
	});
	if (partialSpec) return '租赁规格请填写名称和价格，或删除空白项';
	const partialParam = (form.params || []).find((item) => {
		const hasName = !!item.paramName?.trim();
		const hasValue = !!item.paramValue?.trim();
		return (hasName && !hasValue) || (!hasName && hasValue);
	});
	if (partialParam) return '商品参数请填写名称和参数值，或删除空白项';
	return '';
}

/** 获取一级分类 */
export function getRentCategoryRoots(tree = []) {
	return (tree || []).filter((item) => !item.parentId || Number(item.parentId) === 0);
}

/** 根据 categoryId 解析一二级分类 */
export function findRentCategoryPath(tree = [], categoryId) {
	if (!categoryId) return null;
	const roots = getRentCategoryRoots(tree);
	for (const root of roots) {
		if (Number(root.id) === Number(categoryId)) {
			return {
				rootId: root.id,
				rootName: root.name,
				childId: null,
				childName: '',
				categoryId: root.id,
				label: root.name
			};
		}
		for (const child of root.children || []) {
			if (Number(child.id) === Number(categoryId)) {
				return {
					rootId: root.id,
					rootName: root.name,
					childId: child.id,
					childName: child.name,
					categoryId: child.id,
					label: `${root.name} / ${child.name}`
				};
			}
		}
	}
	return null;
}

/** 校验商品分类是否选完整 */
export function validateGoodsCategorySelection(form = {}, categoryTree = []) {
	if (!form.rootCategoryId) return '请选择装备大类';
	const root = getRentCategoryRoots(categoryTree).find((item) => Number(item.id) === Number(form.rootCategoryId));
	if (!root) return '请选择商品分类';
	const children = root.children || [];
	if (children.length && !form.categoryId) return '请选择细分品类';
	if (!form.categoryId) return '请选择商品分类';
	return '';
}

/** 分类树扁平化（兼容旧逻辑） */
export function flattenRentCategories(nodes = [], bucket = []) {
	(nodes || []).forEach((node) => {
		bucket.push({ id: node.id, name: node.name });
		if (node.children?.length) {
			flattenRentCategories(node.children, bucket);
		}
	});
	return bucket;
}

/** 商户订单商品明细 → 列表卡片结构 */
export function buildMerchantGoodsItems(vo = {}) {
	const items = (vo.items || []).map((item) => ({
		id: item.goodsId,
		name: item.goodsName || '租赁商品',
		image: item.coverImage || '/static/logo.png',
		specLabel: item.specInfo || '默认规格',
		packageLabel: item.packageName || '',
		packageDesc: item.packageDesc || '',
		count: Number(item.qty || 1),
		unitPrice: Number(item.rentPrice || 0)
	}));
	if (items.length) return items;
	return [{
		id: vo.rentOrderId || vo.id,
		name: vo.goodsName || '租赁商品',
		image: vo.coverImage || '/static/logo.png',
		specLabel: getRentTypeLabel(vo.rentType),
		packageLabel: '',
		count: 1,
		unitPrice: 0
	}];
}

/** 商户订单列表 VO → 页面模型 */
export function mapOrderListVoToView(vo = {}) {
	const orderId = vo.merchantOrderNo || String(vo.id);
	const goodsItems = buildMerchantGoodsItems(vo);
	return {
		id: orderId,
		orderNo: vo.rentOrderNo || orderId,
		originOrderId: vo.rentOrderId,
		user: vo.userName || vo.contactName || '用户',
		goods: vo.goodsName || goodsItems[0]?.name || '租赁商品',
		status: vo.statusLabel || vo.status,
		statusLabel: vo.statusLabel || vo.status,
		statusType: vo.statusType || 'pending',
		time: formatOrderDateTime(vo.createTime) || '—',
		createTime: formatOrderDateTime(vo.createTime) || '—',
		amount: formatMoney(vo.payAmount),
		payAmount: Number(vo.payAmount || 0),
		phone: maskPhone(vo.contactPhone),
		pickupTypeKey: mapPickupTypeFromApi(vo.pickupType),
		pickupType: vo.pickupTypeLabel || vo.pickupType || '门店自提',
		address: vo.address || '—',
		remark: vo.remark || '',
		rawStatus: vo.status,
		nextAction: vo.nextAction,
		goodsItems,
		rentType: vo.rentType || 'day',
		rentModeLabel: getRentTypeLabel(vo.rentType),
		startTime: vo.startTime || '',
		endTime: vo.endTime || '',
		rangeText: formatRentRangeText(vo.startTime, vo.endTime),
		returnDate: vo.returnDate || '',
		returnMethodLabel: vo.returnMethodLabel || ''
	};
}

/** 商户订单详情 VO → 页面模型 */
export function mapOrderDetailVoToView(vo = {}) {
	const base = mapOrderListVoToView(vo);
	return {
		...base,
		contactName: vo.contactName,
		userId: vo.userId,
		timeline: (vo.timeline || []).map((item) => ({
			title: item.title,
			time: item.time
		})),
		nextAction: vo.nextAction || 'none',
		inspectionRemark: vo.inspectionRemark || '',
		inspectionImages: Array.isArray(vo.inspectionImageUrls) ? vo.inspectionImageUrls.filter(Boolean) : [],
		inspectionDeductAmount: vo.deductAmount != null ? Number(vo.deductAmount) : undefined,
		inspectionRefundAmount: vo.refundAmount != null ? Number(vo.refundAmount) : undefined,
		returnDate: vo.returnDate || '',
		returnMethodLabel: vo.returnMethod
			? getReturnMethodLabel(vo.returnMethod)
			: (vo.returnMethodLabel || ''),
		returnRemark: vo.returnRemark || '',
		returnImages: Array.isArray(vo.returnImageUrls) ? vo.returnImageUrls.filter(Boolean) : [],
		depositAmount: Number(vo.depositAmount || 0),
		days: Number(vo.days || 1),
		startTime: vo.startTime || '',
		endTime: vo.endTime || ''
	};
}

/** 提货码归一化（去空格） */
export const normalizePickupCode = (value = '') => String(value || '').replace(/\s+/g, '').trim();

/** nextAction → 按钮文案 */
export function resolveMerchantActionLabel(nextAction) {
	const map = {
		prepare: '确认备货',
		pickup: '扫码/输入核销',
		inspection: '录入验收结果',
		none: '已完成'
	};
	return map[nextAction] || '处理订单';
}

/** 验收设备状况选项 */
export const INSPECTION_CONDITION_OPTIONS = ['完好', '轻微损坏', '严重损坏'];

/** 设备状况 UI → API */
export function mapDeviceConditionToApi(label) {
	if (label === '完好') return 'good';
	if (label === '轻微损坏') return 'minor_damage';
	return 'major_damage';
}

/** 按状况建议扣款（可手动调整，不超过订单押金） */
export function suggestInspectionDeduct(condition, depositAmount = 0) {
	const deposit = Number(depositAmount) || 0;
	if (condition === '完好') return 0;
	if (condition === '轻微损坏') return Math.min(deposit, 20);
	return Math.min(deposit, 50);
}

/** @deprecated 兼容旧调用 */
export function calcInspectionDeduct({ condition, allParts, fullRefund, depositAmount } = {}) {
	if (condition === '完好' && allParts !== false && fullRefund !== false) return 0;
	const mapped = condition === '轻微污损' ? '轻微损坏'
		: (condition === '配件缺失' || condition === '需赔付' ? '严重损坏' : condition);
	return suggestInspectionDeduct(mapped, depositAmount);
}

/** 限制扣款在 [0, 押金] */
export function clampInspectionDeduct(deductAmount, depositAmount = 0) {
	const deposit = Math.max(Number(depositAmount) || 0, 0);
	const deduct = Math.max(Number(deductAmount) || 0, 0);
	return Math.min(deduct, deposit);
}

/** 计算应退押金 */
export function calcInspectionRefund(depositAmount = 0, deductAmount = 0) {
	const deposit = Math.max(Number(depositAmount) || 0, 0);
	const deduct = clampInspectionDeduct(deductAmount, deposit);
	return Math.max(deposit - deduct, 0);
}

/** 提交前校验 */
export function validateInspectionSubmit({ condition, deductAmount, depositAmount, remark, imageUrls } = {}) {
	if (!INSPECTION_CONDITION_OPTIONS.includes(condition)) {
		return { ok: false, message: '请选择设备状况' };
	}
	const deposit = Math.max(Number(depositAmount) || 0, 0);
	const deduct = clampInspectionDeduct(deductAmount, deposit);
	if (!String(remark || '').trim()) {
		return { ok: false, message: '请填写验收说明' };
	}
	const images = Array.isArray(imageUrls) ? imageUrls.filter(Boolean) : [];
	if (!images.length) {
		return { ok: false, message: '请上传验收图片' };
	}
	if (deposit <= 0 && deduct > 0) {
		return { ok: false, message: '本单无押金，无法扣款' };
	}
	return {
		ok: true,
		deductAmount: deduct,
		refundAmount: calcInspectionRefund(deposit, deduct),
		imageUrls: images
	};
}

/** 门店详情 → 页面表单 */
export function mapStoreDetailToForm(vo = {}) {
	const staffList = (vo.staffList || []).map((item) => ({
		id: item.id,
		name: item.name,
		role: item.role,
		phone: maskPhone(item.phone)
	}));
	const verifyStaff = staffList[0] || {};
	return {
		id: vo.id,
		name: vo.name || '',
		address: vo.address || '',
		contact: vo.contactPhone || '',
		businessHours: vo.businessHours || '09:00-21:00',
		serviceArea: vo.serviceArea || '',
		businessStatus: vo.status === 1,
		deliveryEnabled: vo.deliveryEnabled === 1,
		selfPickupEnabled: vo.selfPickupEnabled === 1,
		baseDeliveryFee: vo.baseDeliveryFee != null ? String(vo.baseDeliveryFee) : '',
		freeDeliveryThreshold: vo.freeDeliveryThreshold != null ? String(vo.freeDeliveryThreshold) : '',
		verifyContactName: verifyStaff.name || '',
		verifyContactPhone: verifyStaff.phone || '',
		staffList,
		deliveryDesc: vo.deliveryEnabled === 1
			? '门店周边支持同城配送，超出区域需人工确认'
			: '当前未开启配送服务'
	};
}

/** 门店表单 → 保存 BO */
export function mapStoreFormToSaveBo(form = {}) {
	return {
		id: form.id,
		name: form.name,
		address: form.address,
		contactPhone: form.contact,
		businessHours: form.businessHours,
		serviceArea: form.serviceArea,
		deliveryEnabled: form.deliveryEnabled ? 1 : 0,
		selfPickupEnabled: form.selfPickupEnabled ? 1 : 0,
		baseDeliveryFee: form.baseDeliveryFee ? Number(form.baseDeliveryFee) : undefined,
		freeDeliveryThreshold: form.freeDeliveryThreshold ? Number(form.freeDeliveryThreshold) : undefined,
		status: form.businessStatus ? 1 : 0
	};
}

/** 聚合工作台指标（兼容旧版 fallback） */
export function buildDashboardStats({
	pending = 0,
	review = 0,
	goodsCount = 0,
	todayAmount = 0,
	rentingCount = 0,
	weekAmount = 0
} = {}) {
	const stats = [
		{ label: '待处理订单', value: String(pending) },
		{ label: '在架商品', value: String(goodsCount) },
		{ label: '今日租金', value: `¥${formatMoney(todayAmount)}` },
		{ label: '待验收归还', value: String(review) }
	];
	if (rentingCount > 0) {
		stats.push({ label: '租赁中', value: String(rentingCount) });
	}
	if (weekAmount > 0) {
		stats.push({ label: '本周租金', value: `¥${formatMoney(weekAmount)}` });
	}
	return stats;
}

const formatDateTimeShort = (value) => {
	if (!value) return '—';
	const text = String(value).replace('T', ' ');
	const match = text.match(/(\d{4}-\d{2}-\d{2}) (\d{2}:\d{2})/);
	return match ? `${match[1].slice(5)} ${match[2]}` : text.slice(0, 16);
};

/** 工作台今日待取货订单 */
export function mapDashboardTodayOrder(vo = {}) {
	const nextAction = vo.nextAction || 'none';
	return {
		id: vo.merchantOrderNo || String(vo.id || ''),
		rawId: vo.merchantOrderNo || String(vo.id || ''),
		goods: vo.goodsName || '租赁商品',
		contact: vo.contactName || '用户',
		phone: vo.contactPhone || '',
		pickupTime: formatDateTimeShort(vo.pickupStartTime),
		status: vo.statusLabel || '待取货',
		nextAction,
		amount: formatMoney(vo.payAmount),
		actionLabel: resolveMerchantActionLabel(nextAction)
	};
}

/** 服务端工作台统计 → 页面结构 */
export function mapDashboardStats(vo = {}) {
	const todayPickupOrders = [...(vo.todayPickupOrders || [])].sort((left, right) => {
		const leftTime = String(left?.pickupStartTime || left?.createTime || '');
		const rightTime = String(right?.pickupStartTime || right?.createTime || '');
		return rightTime.localeCompare(leftTime);
	});
	return {
		storeName: vo.storeName || '商家工作台',
		businessStatus: vo.businessStatus !== 0,
		goods: {
			total: Number(vo.goodsTotalCount ?? vo.goodsCount ?? 0),
			onShelf: Number(vo.goodsOnShelfCount ?? vo.goodsCount ?? 0)
		},
		orders: {
			total: Number(vo.orderTotalCount ?? 0),
			pendingVerify: Number(vo.pendingVerifyCount ?? 0),
			renting: Number(vo.rentingCount ?? 0),
			refunded: Number(vo.refundedCount ?? 0)
		},
		revenue: {
			today: Number(vo.todayAmount ?? 0),
			month: Number(vo.monthAmount ?? 0),
			total: Number(vo.totalAmount ?? vo.weekAmount ?? 0)
		},
		todayPickupOrders: todayPickupOrders.map(mapDashboardTodayOrder)
	};
}

/** 营业额筛选结果 */
export function mapDashboardRevenue(vo = {}) {
	return {
		year: Number(vo.year || new Date().getFullYear()),
		month: Number(vo.month || 0),
		amount: Number(vo.amount ?? 0),
		orderCount: Number(vo.orderCount ?? 0),
		todayAmount: Number(vo.todayAmount ?? 0),
		monthAmount: Number(vo.monthAmount ?? 0),
		totalAmount: Number(vo.totalAmount ?? 0)
	};
}

/** 解析 LINK 订单 ID */
export function parseLinkRentOrderId(orderId) {
	const text = String(orderId || '');
	if (text.startsWith('LINK-')) {
		return Number(text.replace('LINK-', ''));
	}
	return null;
}
