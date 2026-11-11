/** 首页租赁模式 Tab 与接口 rentType / sortBy 映射 */
export const HOME_RENT_TABS = [{
		name: '租赁套餐',
		rentType: '',
		sortBy: 'default'
	},
	{
		name: '小时租',
		rentType: 'hour',
		sortBy: 'default'
	},
	{
		name: '整日租',
		rentType: 'day',
		sortBy: 'default'
	},
	{
		name: '过夜租',
		rentType: 'overnight',
		sortBy: 'default'
	},
	{
		name: '特惠爆款',
		rentType: '',
		sortBy: 'sales'
	}
];

/** 分类页排序项与接口 sortBy 映射 */
export const CATEGORY_SORT_OPTIONS = [{
		label: '综合',
		sortBy: 'default'
	},
	{
		label: '价格',
		sortBy: 'price'
	},
	{
		label: '热度',
		sortBy: 'sales'
	}
];

/** 商品列表页排序（与分类页一致，对接 sortBy） */
export const GOODS_LIST_SORT_OPTIONS = CATEGORY_SORT_OPTIONS;

export const DEFAULT_OVERNIGHT_START = '18:00';
export const DEFAULT_OVERNIGHT_END = '10:00';

/** 详情页租赁时长 Tab（key 用于下单页，apiRentType 对接接口 rentType） */
export const DETAIL_RENT_MODES = [{
		key: 'day',
		apiRentType: 'day',
		label: '整日租',
		desc: '24小时计费'
	},
	{
		key: 'hour',
		apiRentType: 'hour',
		label: '小时租',
		desc: '4小时起租'
	},
	{
		key: 'night',
		apiRentType: 'overnight',
		label: '过夜租',
		desc: `${DEFAULT_OVERNIGHT_START}-次日${DEFAULT_OVERNIGHT_END}`
	}
];

export const parseOvernightClock = (value, fallback) => {
	const text = String(value || '').trim();
	return /^\d{1,2}:\d{2}$/.test(text) ? text : fallback;
};

export const getGoodsOvernightRange = (goods = {}) => ({
	start: parseOvernightClock(goods.overnightStartTime, DEFAULT_OVERNIGHT_START),
	end: parseOvernightClock(goods.overnightEndTime, DEFAULT_OVERNIGHT_END)
});

/** 格式化过夜租时段文案（优先商品接口字段） */
export const formatOvernightTimeLabel = (start, end) =>
	`${parseOvernightClock(start, DEFAULT_OVERNIGHT_START)}-次日${parseOvernightClock(end, DEFAULT_OVERNIGHT_END)}`;

/** 商品是否开启某租赁模式（按接口价格判断） */
export const isRentModeEnabled = (goods, apiRentType = 'day') => {
	if (!goods) return false;
	if (apiRentType === 'hour') return Number(goods.rentPriceHour ?? 0) > 0;
	if (apiRentType === 'overnight') return Number(goods.rentPriceOvernight ?? 0) > 0;
	return Number(goods.rentPriceDay ?? goods.rentPrice ?? 0) > 0;
};

/** 详情页可用租赁模式（过滤未配置价格的模式，过夜时段取接口） */
export const buildDetailRentModes = (goods) => {
	if (!goods) return [];
	return DETAIL_RENT_MODES
		.filter((item) => isRentModeEnabled(goods, item.apiRentType))
		.map((item) => {
			if (item.apiRentType !== 'overnight') return { ...item };
			return {
				...item,
				desc: formatOvernightTimeLabel(goods.overnightStartTime, goods.overnightEndTime)
			};
		});
};

/** 详情页默认租赁模式（优先整日租，否则取首个可用模式） */
export const resolveDefaultDetailRentMode = (goods, preferredKey = 'day') => {
	const modes = buildDetailRentModes(goods);
	if (modes.some((item) => item.key === preferredKey)) return preferredKey;
	return modes[0]?.key || preferredKey;
};

/** 按租赁模式取商品单价 */
export const getGoodsRentPriceByMode = (goods, rentModeKey = 'day') => {
	if (!goods) return 0;
	const mode = DETAIL_RENT_MODES.find((item) => item.key === rentModeKey);
	const apiType = mode?.apiRentType || 'day';
	if (apiType === 'hour') return Number(goods.rentPriceHour ?? 0);
	if (apiType === 'overnight') return Number(goods.rentPriceOvernight ?? 0);
	return Number(goods.rentPriceDay ?? goods.rentPrice ?? 0);
};

/** 租赁规格 / 商品参数类型（与后端 RentGoodsSpecType 对齐） */
export const SPEC_TYPE_SKU = 1;
export const SPEC_TYPE_PARAM = 2;

const isNumericSpecValue = (value) => /^\d+(\.\d+)?$/.test(String(value ?? '').trim());

/** 详情页租赁规格选项（用户可选 SKU） */
export const buildRentSpecOptions = (goods) => {
	const list = goods?.specs || [];
	return list
		.filter((item) => item?.name || item?.specName)
		.map((item, index) => ({
			index,
			id: item.id,
			label: item.name || item.specName || '',
			price: Number(item.price ?? item.specPrice ?? 0)
		}));
};

/** 详情页商品参数（仅展示） */
export const buildGoodsParamRows = (goods) => {
	if (Array.isArray(goods?.params) && goods.params.length) {
		return goods.params.map((item) => ({
			name: item.name || item.paramName || '',
			value: item.value || item.paramValue || ''
		}));
	}
	// 兼容旧接口：非数字 spec_value 视为参数
	return (goods?.specs || [])
		.filter((item) => {
			const value = item.value ?? item.specValue;
			return (item.name || item.specName) && value && !isNumericSpecValue(value);
		})
		.map((item) => ({
			name: item.name || item.specName || '',
			value: item.value || item.specValue || ''
		}));
};

export const goodsRequiresSpecSelection = (goods) => buildRentSpecOptions(goods).length > 0;

export const resolveSelectedRentSpec = (goods, specIndex = 0) => {
	const options = buildRentSpecOptions(goods);
	if (!options.length) return null;
	const normalized = Math.max(Math.min(Number(specIndex) || 0, options.length - 1), 0);
	return options[normalized] || null;
};

/** 详情页费用试算：租赁单价 + 规格加价 + 套餐加价 */
export const calculateDetailTotalPrice = (goods, rentModeKey = 'day', packageItem = null, specItem = null) => {
	const base = getGoodsRentPriceByMode(goods, rentModeKey);
	const specPrice = Number(specItem?.price ?? 0);
	const packagePrice = Number(packageItem?.price ?? 0);
	return (base + specPrice + packagePrice).toFixed(2);
};

/** 构建库存校验默认租期（整日租：当前至次日同刻） */
export const buildDefaultStockCheckRange = (rentModeKey = 'day', goods = null) => {
	const pad = (value) => String(value).padStart(2, '0');
	const now = new Date();
	const start = new Date(now);
	const overnight = getGoodsOvernightRange(goods || {});
	const [overnightStartHour, overnightStartMinute = 0] = overnight.start.split(':').map(Number);
	const [overnightEndHour, overnightEndMinute = 0] = overnight.end.split(':').map(Number);
	if (rentModeKey === 'hour') {
		start.setHours(now.getHours() + 1, 0, 0, 0);
	} else if (rentModeKey === 'night') {
		start.setHours(overnightStartHour, overnightStartMinute, 0, 0);
		if (now.getHours() > overnightStartHour || (now.getHours() === overnightStartHour && now.getMinutes() >= overnightStartMinute)) {
			start.setDate(start.getDate() + 1);
		}
	} else {
		start.setHours(now.getHours() + 1, 0, 0, 0);
	}
	const end = new Date(start);
	if (rentModeKey === 'hour') {
		end.setHours(end.getHours() + 4);
	} else if (rentModeKey === 'night') {
		end.setDate(end.getDate() + 1);
		end.setHours(overnightEndHour, overnightEndMinute, 0, 0);
	} else {
		end.setDate(end.getDate() + 1);
	}
	const format = (date) =>
		`${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
	return {
		startTime: format(start),
		endTime: format(end)
	};
};

/** 加购占位租期（接口必填，真实租期在结算页选择） */
export const buildCartAddPayload = (goodsId, rentModeKey = 'day', qty = 1, goods = null, specId = 0, packageId = 0) => {
	const mode = DETAIL_RENT_MODES.find((item) => item.key === rentModeKey) || DETAIL_RENT_MODES[0];
	const range = buildDefaultStockCheckRange(rentModeKey, goods);
	return {
		goodsId,
		specId: specId ?? 0,
		packageId: packageId ?? 0,
		rentType: mode.apiRentType,
		startTime: range.startTime,
		endTime: range.endTime,
		qty
	};
};

/** 列表/卡片是否可加购（无库存字段时不前端拦截，交由后端校验） */
export const isGoodsAvailableForCart = (goods) => {
	if (!goods) return false;
	if (goods.availableStock != null) return Number(goods.availableStock) > 0;
	if (goods.totalStock != null) return Number(goods.totalStock) > 0;
	return true;
};

const padDatePart = (value) => String(value).padStart(2, '0');

/** 解析 yyyy-MM-dd HH:mm:ss */
export const parseDateTime = (value) => {
	if (!value) return null;
	const normalized = String(value).replace('T', ' ').trim();
	const [datePart, timePart = '00:00:00'] = normalized.split(' ');
	const [year, month, day] = datePart.split('-').map(Number);
	const [hour, minute, second = 0] = timePart.split(':').map(Number);
	if (!year || !month || !day) return null;
	return new Date(year, month - 1, day, hour || 0, minute || 0, second || 0);
};

/** 格式化为 yyyy-MM-dd HH:mm:ss */
export const formatDateTime = (date) => {
	if (!(date instanceof Date) || Number.isNaN(date.getTime())) return '';
	return `${date.getFullYear()}-${padDatePart(date.getMonth() + 1)}-${padDatePart(date.getDate())} ${padDatePart(date.getHours())}:${padDatePart(date.getMinutes())}:${padDatePart(date.getSeconds())}`;
};

export const formatDatePart = (date) => {
	if (!(date instanceof Date) || Number.isNaN(date.getTime())) return '';
	return `${date.getFullYear()}-${padDatePart(date.getMonth() + 1)}-${padDatePart(date.getDate())}`;
};

export const formatTimePart = (date) => `${padDatePart(date.getHours())}:${padDatePart(date.getMinutes())}`;

/**
 * 租期计费规则（与 server RentOrderPriceCalculator 对齐）：
 * - 整日租 day：计费天数 = max(结束日期 − 开始日期 的自然日差, 1)，租金 = 日单价 × 天数 × 数量
 * - 小时租 hour：计费小时 = max(结束时刻 − 开始时刻 的小时数, 1)，租金 = 小时单价 × 小时数 × 数量
 * - 过夜租 overnight：按「晚」计费，固定 1 晚，租金 = 过夜单价 × 数量
 * 套餐加价：每行商品额外 + 套餐价，再乘数量
 */
export const calcRentBilling = (apiRentType, startTime, endTime) => {
	const start = parseDateTime(startTime);
	const end = parseDateTime(endTime);
	if (!start || !end) {
		return {
			valid: false,
			message: '请选择完整的租赁起止时间'
		};
	}
	if (end <= start) {
		return {
			valid: false,
			message: '结束时间须晚于开始时间'
		};
	}
	if (apiRentType === 'hour') {
		const hours = Math.max(Math.ceil((end.getTime() - start.getTime()) / 3600000), 1);
		return {
			valid: true,
			units: hours,
			unitLabel: '小时',
			rentType: 'hour',
			ruleText: `小时租：${hours} 小时 × 小时单价`
		};
	}
	if (apiRentType === 'overnight') {
		return {
			valid: true,
			units: 1,
			unitLabel: '晚',
			rentType: 'overnight',
			ruleText: '过夜租：按 1 晚计费（18:00 至次日 10:00）'
		};
	}
	const startDay = new Date(start.getFullYear(), start.getMonth(), start.getDate());
	const endDay = new Date(end.getFullYear(), end.getMonth(), end.getDate());
	const days = Math.max(Math.round((endDay.getTime() - startDay.getTime()) / 86400000), 1);
	return {
		valid: true,
		units: days,
		unitLabel: '天',
		rentType: 'day',
		ruleText: `整日租：${days} 天 × 日单价`
	};
};

/** 根据用户选择的起止时间计算租金（含规格/套餐加价） */
export const calcRentItemAmount = (goods, rentModeKey, startTime, endTime, qty = 1, packageItem = null, specItem = null) => {
	const mode = DETAIL_RENT_MODES.find((item) => item.key === rentModeKey) || DETAIL_RENT_MODES[0];
	const billing = calcRentBilling(mode.apiRentType, startTime, endTime);
	if (!billing.valid) {
		return {
			...billing,
			amount: 0,
			unitPrice: 0
		};
	}
	const unitPrice = Number(getGoodsRentPriceByMode(goods, rentModeKey));
	const specPrice = Number(specItem?.price ?? 0);
	const packagePrice = Number(packageItem?.price ?? 0);
	const amount = (unitPrice * billing.units + specPrice + packagePrice) * Number(qty || 1);
	return {
		...billing,
		unitPrice,
		specPrice,
		packagePrice,
		amount: Number(amount.toFixed(2))
	};
};

/** 租期选择器初始值 */
export const createRentPeriodSelection = (rentModeKey = 'day', goods = null) => {
	const range = buildDefaultStockCheckRange(rentModeKey, goods);
	const start = parseDateTime(range.startTime);
	const end = parseDateTime(range.endTime);
	return {
		rentMode: rentModeKey,
		startDate: formatDatePart(start),
		startClock: formatTimePart(start),
		endDate: formatDatePart(end),
		endClock: formatTimePart(end),
		startTime: range.startTime,
		endTime: range.endTime
	};
};

export const composeRentDateTime = (datePart, clockPart, fallbackClock = '09:00') => {
	if (!datePart) return '';
	const clock = clockPart || fallbackClock;
	return `${datePart} ${clock.length === 5 ? `${clock}:00` : clock}`;
};

export const syncRentPeriodTimes = (selection = {}, rentModeKey = 'day', goods = null) => {
	if (rentModeKey === 'night') {
		const overnight = getGoodsOvernightRange(goods || {});
		const startDate = selection.startDate || formatDatePart(new Date());
		const start = parseDateTime(`${startDate} ${overnight.start}:00`);
		const end = new Date(start);
		end.setDate(end.getDate() + 1);
		const [endHour, endMinute = 0] = overnight.end.split(':').map(Number);
		end.setHours(endHour, endMinute, 0, 0);
		return {
			...selection,
			rentMode: rentModeKey,
			startDate: formatDatePart(start),
			startClock: overnight.start,
			endDate: formatDatePart(end),
			endClock: overnight.end,
			startTime: formatDateTime(start),
			endTime: formatDateTime(end)
		};
	}
	const startTime = composeRentDateTime(selection.startDate, selection.startClock);
	const endTime = composeRentDateTime(selection.endDate, selection.endClock);
	return {
		...selection,
		rentMode: rentModeKey,
		startTime,
		endTime
	};
};

/** 首页 rentType → 详情 rentMode */
export const mapApiRentTypeToDetailMode = (apiRentType = '') => {
	if (apiRentType === 'hour') return 'hour';
	if (apiRentType === 'overnight') return 'night';
	if (apiRentType === 'day') return 'day';
	return 'day';
};

/** 租赁类型展示单位 */
export const getRentPriceUnit = (rentType) => {
	if (rentType === 'hour') return '小时';
	if (rentType === 'overnight') return '晚';
	return '天';
};

/** 格式化列表展示价 */
export const formatGoodsRentPrice = (item, rentType = '') => {
	const price = item?.rentPrice ?? item?.rentPriceDay ?? 0;
	return {
		price: Number(price || 0).toFixed(2),
		unit: getRentPriceUnit(rentType)
	};
};

/** 首页 Tab 切换：按名称解析索引，未知名称回退首项 */
export const resolveHomeRentTabIndex = (tabName) => {
	const index = HOME_RENT_TABS.findIndex((tab) => tab.name === tabName);
	return index >= 0 ? index : 0;
};

/** 首页商品列表查询参数（rentType / sortBy） */
export const buildHomeGoodsQuery = (tabIndex = 0) => {
	const tab = HOME_RENT_TABS[tabIndex] || HOME_RENT_TABS[0];
	return {
		rentType: tab.rentType || undefined,
		sortBy: tab.sortBy
	};
};

/** 我的页订单角标 UI 结构 */
export const buildOrderStates = (counts = {}) => {
	const mapped = mapOrderStatusCounts(counts);
	return [{
			key: 'pending',
			label: '待付款',
			count: mapped.pending_payment,
			icon: 'rmb-circle-fill'
		},
		{
			key: 'pickup',
			label: '待取货',
			count: mapped.waiting_pickup,
			icon: 'bag-fill'
		},
		{
			key: 'using',
			label: '使用中',
			count: mapped.renting,
			icon: 'clock-fill'
		},
		{
			key: 'return',
			label: '待归还',
			count: mapped.returning,
			icon: 'rewind-left-fill'
		},
		{
			key: 'done',
			label: '售后',
			count: mapped.after_sale,
			icon: 'server-fill'
		}
	];
};

/** 订单角标字段映射（服务端 snake_case / camelCase） */
export const mapOrderStatusCounts = (counts = {}) => ({
	pending_payment: Number(counts.pending_payment ?? counts.pendingPayment ?? 0),
	waiting_pickup: Number(counts.waiting_pickup ?? counts.waitingPickup ?? 0),
	renting: Number(counts.renting ?? 0),
	returning: Number(counts.returning ?? 0),
	after_sale: Number(counts.after_sale ?? counts.afterSale ?? counts.completed ?? 0)
});

/** 确认页取货方式（UI key ↔ 接口 pickupType） */
export const CONFIRM_PICKUP_TYPES = [{
		key: 'store',
		apiType: 'store',
		label: '门店自提'
	},
	{
		key: 'delivery',
		apiType: 'delivery',
		label: '同城送达'
	}
];

/** 租期计费规则说明（展示用，与 server RentOrderPriceCalculator 对齐） */
export const RENT_BILLING_RULE_LINES = [
	'整日租：计费天数 = max(归还日期 − 取货日期, 1)，租金 = 日单价 × 天数 × 数量',
	'小时租：计费小时 = max(归还时刻 − 取货时刻, 1)，租金 = 小时单价 × 小时数 × 数量',
	'过夜租：固定 1 晚（当日 18:00 至次日 10:00），租金 = 过夜单价 × 数量',
	'套餐加价：每行商品可叠加套餐价，再乘以数量'
];

export const mapPickupTypeToApi = (uiKey = 'store') => {
	const byKey = CONFIRM_PICKUP_TYPES.find((item) => item.key === uiKey);
	if (byKey) return byKey.apiType;
	if (uiKey === 'pickup_point' || uiKey === 'station') return 'store';
	const byApi = CONFIRM_PICKUP_TYPES.find((item) => item.apiType === uiKey);
	return byApi?.apiType || 'store';
};

export const mapPickupTypeFromApi = (apiType = 'store') => {
	if (apiType === 'pickup_point' || apiType === 'station') return 'store';
	return CONFIRM_PICKUP_TYPES.find((item) => item.apiType === apiType)?.key || 'store';
};

/** 租赁类型展示文案 */
export const getRentTypeLabel = (rentType = 'day') => {
	if (rentType === 'hour') return '小时租';
	if (rentType === 'overnight') return '过夜租';
	return '整日租';
};

/** 购物车规格展示：规格 · 套餐 */
export const buildCartSpecLabel = (specInfo = '', packageName = '') => {
	const parts = [];
	if (specInfo && specInfo !== '默认规格') parts.push(specInfo);
	if (packageName) parts.push(packageName);
	return parts.length ? parts.join(' · ') : '默认规格';
};

/** 购物车条目：接口 VO → 页面展示结构 */
export const mapCartItemToView = (item = {}) => {
	const rentType = item.rentType || 'day';
	let unitPrice = Number(item.rentPriceDay ?? 0);
	if (rentType === 'hour') unitPrice = Number(item.rentPriceHour ?? 0);
	if (rentType === 'overnight') unitPrice = Number(item.rentPriceOvernight ?? 0);
	return {
		id: item.id,
		cartId: item.id,
		goodsId: item.goodsId,
		name: item.goodsName || '',
		image: item.coverImage || '/static/logo.png',
		spec: buildCartSpecLabel(item.specInfo, item.packageName),
		specInfo: item.specInfo || '',
		packageId: item.packageId ?? 0,
		packageName: item.packageName || '',
		rentType,
		rentTypeLabel: getRentTypeLabel(rentType),
		price: unitPrice,
		unitPrice,
		count: Number(item.qty ?? 1),
		selected: Boolean(item.selected),
		invalid: Boolean(item.invalid),
		invalidReason: item.invalidReason || '',
		startTime: item.startTime || '',
		endTime: item.endTime || '',
		depositAmount: Number(item.depositAmount ?? 0),
		itemRentAmount: Number(item.itemRentAmount ?? 0),
		itemDepositAmount: Number(item.itemDepositAmount ?? 0)
	};
};

/** 格式化租期区间展示 */
export const formatRentRangeText = (startTime, endTime) => {
	if (!startTime || !endTime) return '';
	const formatPart = (value) => {
		const text = String(value).replace('T', ' ');
		const match = text.match(/(\d{2}-\d{2})\s(\d{2}:\d{2})/);
		return match ? `${match[1]} ${match[2]}` : text.slice(5, 16);
	};
	return `${formatPart(startTime)} - ${formatPart(endTime)}`;
};

/** 确认页租期展示：5月30日 10:00 */
export const formatRentPeriodDisplay = (value) => {
	const date = parseDateTime(value);
	if (!date) return '--';
	return `${date.getMonth() + 1}月${date.getDate()}日 ${padDatePart(date.getHours())}:${padDatePart(date.getMinutes())}`;
};

/** 租赁时长单位文案 */
export const getRentDurationUnitLabel = (rentType = 'day') => {
	if (rentType === 'hour') return '小时';
	if (rentType === 'overnight') return '晚';
	return '天';
};

/** 订单租期时长摘要 */
export const buildRentDurationSummary = (order = {}) => {
	const rentType = order.rentType || 'day';
	const unit = getRentDurationUnitLabel(rentType);
	if (rentType === 'hour' && order.startTime && order.endTime) {
		const billing = calcRenewBillingHours(order.startTime, order.endTime);
		const hours = billing.valid ? billing.hours : Number(order.days || 1);
		return { value: hours, unit, text: `${hours} 小时` };
	}
	const days = Number(order.days || 1);
	return { value: days, unit, text: `${days} ${unit}` };
};

/** 续租摘要文案（列表等紧凑场景） */
export const buildRenewSummaryText = (order = {}) => {
	const { renewDays, renewAmount, rentType = 'day' } = order;
	if (!renewAmount && !renewDays) return '';
	const unit = getRentDurationUnitLabel(rentType);
	const amount = formatDisplayPrice(renewAmount || 0);
	if (renewDays) {
		return `已续 ${renewDays} ${unit}，补差 ¥${amount}`;
	}
	return `已续租，补差 ¥${amount}`;
};

/** 租金计费拆解（单价 / 数量 / 时长） */
export const buildRentPriceBreakdown = (item = {}, order = {}) => {
	const rentType = order.rentType || 'day';
	const unit = getRentDurationUnitLabel(rentType);
	const duration = buildRentDurationSummary(order);
	const count = Number(item.count || 1);
	const unitPrice = formatDisplayPrice(item.unitPrice || 0);
	return {
		unitPriceText: `¥${unitPrice}/${unit}`,
		qtyText: `${count} 件`,
		durationText: duration.text,
		chips: [
			{ label: '单价', value: `¥${unitPrice}/${unit}` },
			{ label: '数量', value: `${count} 件` },
			{ label: '时长', value: duration.text }
		]
	};
};

/** 续租默认结束时间：在当前订单结束时间基础上延长 1 个计费单位 */
export const buildDefaultRenewEndTime = (orderEndTime, rentModeKey = 'day') => {
	const start = parseDateTime(orderEndTime);
	if (!start) return '';
	const end = new Date(start);
	if (rentModeKey === 'hour') {
		end.setHours(end.getHours() + 1);
	} else {
		end.setDate(end.getDate() + 1);
	}
	return formatDateTime(end);
};

/** 续租计费小时数（不足 1 小时按 1 小时计，向上取整） */
export const calcRenewBillingHours = (currentEndTime, selectedEndTime) => {
	const start = parseDateTime(currentEndTime);
	const end = parseDateTime(selectedEndTime);
	if (!start || !end) {
		return { valid: false, message: '请选择完整的续租结束时间', hours: 0 };
	}
	if (end <= start) {
		return { valid: false, message: '结束时间须晚于当前租期结束时间', hours: 0 };
	}
	const minutes = Math.max(0, Math.round((end.getTime() - start.getTime()) / 60000));
	const hours = Math.max(Math.ceil(minutes / 60), 1);
	return {
		valid: true,
		hours,
		ruleText: `续租：${hours} 小时 × 时租单价`
	};
};

/** 根据当前结束时间与所选续租结束时间计算 extraDays（小时租时为小时数） */
export const calcRenewExtraUnits = (apiRentType, currentEndTime, selectedEndTime) => {
	if (apiRentType === 'overnight') {
		const start = parseDateTime(currentEndTime);
		const end = parseDateTime(selectedEndTime);
		if (!start || !end) {
			return { valid: false, message: '请选择完整的续租结束时间', units: 0, unitLabel: '晚' };
		}
		if (end <= start) {
			return { valid: false, message: '结束时间须晚于当前租期结束时间', units: 0, unitLabel: '晚' };
		}
		const startDay = new Date(start.getFullYear(), start.getMonth(), start.getDate());
		const endDay = new Date(end.getFullYear(), end.getMonth(), end.getDate());
		const nights = Math.max(Math.round((endDay.getTime() - startDay.getTime()) / 86400000), 1);
		return {
			valid: true,
			units: nights,
			unitLabel: '晚',
			ruleText: `过夜租：续租 ${nights} 晚 × 过夜单价`
		};
	}
	const billing = calcRentBilling(apiRentType, currentEndTime, selectedEndTime);
	if (!billing.valid) {
		return { ...billing, units: 0, unitLabel: billing.unitLabel || getRentPriceUnit(apiRentType) };
	}
	return {
		valid: true,
		units: billing.units,
		unitLabel: billing.unitLabel,
		ruleText: billing.ruleText.replace(/^[^：]+：/, '续租：')
	};
};

/** 由起止时间构建租期选择器初始值 */
export const createRentPeriodFromTimes = (startTime, endTime, rentModeKey = 'day') => {
	const start = parseDateTime(startTime);
	const end = parseDateTime(endTime);
	if (!start || !end) {
		return createRentPeriodSelection(rentModeKey);
	}
	return syncRentPeriodTimes({
		rentMode: rentModeKey,
		startDate: formatDatePart(start),
		startClock: formatTimePart(start),
		endDate: formatDatePart(end),
		endClock: formatTimePart(end),
		startTime,
		endTime
	}, rentModeKey);
};

/** 金额格式化为两位小数 */
export const formatMoney = (value) => Number(value || 0).toFixed(2);

/** 展示用单价（避免 29.900000000002 等浮点噪音） */
export const formatDisplayPrice = (value) => {
	const num = Number(value || 0);
	if (!Number.isFinite(num)) return '0';
	const rounded = Math.round(num * 100) / 100;
	if (Number.isInteger(rounded)) return String(rounded);
	return rounded.toFixed(2).replace(/0+$/, '').replace(/\.$/, '');
};

/** 购物车条目单价（按租赁类型） */
export const getCartLineUnitPrice = (cartItem = {}, rentType = cartItem.rentType || 'day') => {
	if (cartItem.unitPrice != null && cartItem.unitPrice !== '') return Number(cartItem.unitPrice);
	if (cartItem.price != null && cartItem.price !== '') return Number(cartItem.price);
	if (rentType === 'hour') return Number(cartItem.rentPriceHour ?? 0);
	if (rentType === 'overnight') return Number(cartItem.rentPriceOvernight ?? 0);
	return Number(cartItem.rentPriceDay ?? 0);
};

/** 结算预览商品行 → 确认页展示结构 */
export const mapCheckoutPreviewItemToView = (previewItem = {}, cartItem = {}, options = {}) => {
	const rentType = previewItem.rentType || cartItem.rentType || 'day';
	const count = Number(previewItem.quantity || cartItem.count || 1);
	const rentPeriodConfirmed = Boolean(options.rentPeriodConfirmed);
	const lineRentAmount = Number(previewItem.itemRentAmount || 0);
	const specInfo = cartItem.specInfo || '';
	const packageName = cartItem.packageName || '';
	return {
		id: previewItem.goodsId,
		cartId: previewItem.cartId,
		name: previewItem.goodsName || cartItem.name || '',
		image: cartItem.image || '/static/logo.png',
		specLabel: buildCartSpecLabel(specInfo, packageName),
		specInfo,
		packageLabel: packageName,
		packageName,
		count,
		days: rentPeriodConfirmed ? Number(previewItem.days || 0) : 0,
		unitPrice: getCartLineUnitPrice(cartItem, rentType),
		lineRentAmount,
		rentType,
		rentModeLabel: getRentTypeLabel(rentType),
		rangeText: rentPeriodConfirmed ?
			formatRentRangeText(previewItem.startTime, previewItem.endTime) :
			'待选择'
	};
};

/** 确认页商品行价格文案 */
export const formatConfirmGoodsPriceText = (item = {}, rentPeriodConfirmed = false) => {
	if (rentPeriodConfirmed && item.lineRentAmount != null) {
		return `¥${formatMoney(item.lineRentAmount)}`;
	}
	const unit = getRentPriceUnit(item.rentType);
	return `¥${formatDisplayPrice(item.unitPrice)}/${unit} x ${item.count}`;
};

/** 平台默认同城配送费（元），与 server RentalDeliveryFeeSupport 兜底值一致 */
export const PLATFORM_DEFAULT_DELIVERY_FEE = 15;

/** 确认页同城配送费预估（门店配置优先，未配置回落平台默认；达免配送门槛为 0） */
export const calcDeliveryFeeAmount = ({
	pickupType = 'store',
	store,
	rentAmount = 0,
	platformDefaultFee = PLATFORM_DEFAULT_DELIVERY_FEE
} = {}) => {
	if (pickupType !== 'delivery') return 0;
	const rent = Number(rentAmount || 0);
	const threshold = store?.freeDeliveryThreshold != null && store.freeDeliveryThreshold !== '' ?
		Number(store.freeDeliveryThreshold) :
		0;
	if (threshold > 0 && rent >= threshold) return 0;
	if (store?.baseDeliveryFee != null && store.baseDeliveryFee !== '') {
		const fee = Number(store.baseDeliveryFee);
		if (!Number.isNaN(fee) && fee >= 0) return fee;
	}
	return Number(platformDefaultFee ?? PLATFORM_DEFAULT_DELIVERY_FEE);
};

/** 确认页配送费规则说明（与 server buildRuleRemark 语义对齐） */
export const buildDeliveryFeeRemark = ({
	pickupType = 'store',
	store,
	rentAmount = 0,
	platformDefaultFee = PLATFORM_DEFAULT_DELIVERY_FEE
} = {}) => {
	if (pickupType !== 'delivery') return '';
	const fee = calcDeliveryFeeAmount({
		pickupType,
		store,
		rentAmount,
		platformDefaultFee
	});
	const baseFee = store?.baseDeliveryFee != null && store.baseDeliveryFee !== '' ?
		Number(store.baseDeliveryFee) :
		Number(platformDefaultFee ?? PLATFORM_DEFAULT_DELIVERY_FEE);
	let remark = '同城配送费按取货门店规则计算';
	if (store?.name) remark += `（${store.name}）`;
	remark += `，基础配送费 ¥${formatMoney(baseFee)}`;
	const threshold = store?.freeDeliveryThreshold != null && store.freeDeliveryThreshold !== '' ?
		Number(store.freeDeliveryThreshold) :
		0;
	if (threshold > 0) remark += `；租金满 ¥${formatMoney(threshold)} 免配送`;
	if (fee === 0 && baseFee > 0) remark += '。当前订单已满足免配送条件';
	else if (fee > 0) remark += `。本单配送费 ¥${formatMoney(fee)}`;
	return remark;
};

/** 确认页应付 = 租金 + 配送 - 优惠（押金从账户冻结，不计入支付） */
export const calcOrderPayableAmount = ({
	rentAmount = 0,
	deliveryAmount = 0,
	couponAmount = 0
} = {}) => {
	const total = Number(rentAmount || 0) +
		Number(deliveryAmount || 0) -
		Number(couponAmount || 0);
	return formatMoney(Math.max(total, 0));
};

/** 租物车已选商品租金合计（不含押金） */
export const calcCartSelectedTotal = (items = []) =>
	items
	.filter((item) => !item.invalid && item.selected)
	.reduce((sum, item) => sum + Number(item.itemRentAmount ?? item.price * item.count ?? 0), 0)
	.toFixed(2);

/** 订单中心 Tab（对接 status 筛选） */
export const ORDER_TABS = [{
		key: 'all',
		label: '全部'
	},
	{
		key: 'pending_payment',
		label: '待支付'
	},
	{
		key: 'waiting_pickup',
		label: '待取货'
	},
	{
		key: 'renting',
		label: '租赁中'
	},
	{
		key: 'returning',
		label: '待归还'
	},
	{
		key: 'completed',
		label: '已完成'
	}
];

/** 订单状态展示文案 */
export const ORDER_STATUS_MAP = {
	pending_payment: {
		label: '待支付',
		desc: '请尽快完成支付，库存将为您保留 30 分钟。'
	},
	waiting_pickup: {
		label: '待取货',
		desc: '订单已备货完成，请按预约时间到店取货。'
	},
	delivering: {
		label: '配送中',
		desc: '设备正在配送途中，请保持电话畅通。'
	},
	renting: {
		label: '租赁中',
		desc: '设备已取走，如需延长租期可发起续租。'
	},
	returning: {
		label: '待归还',
		desc: '请在约定时间前完成归还，超时将按规则补差。'
	},
	inspecting: {
		label: '验收中',
		desc: '门店正在验收设备，请耐心等待结果。'
	},
	completed: {
		label: '已完成',
		desc: '订单已完成，押金将按原路退回。'
	},
	cancelled: {
		label: '已取消',
		desc: '订单已取消，未支付订单库存将自动释放。'
	}
};

/** 归还方式 UI ↔ API */
export const RETURN_METHOD_OPTIONS = [{
		key: 'store',
		apiType: 'store',
		label: '到店归还'
	},
	{
		key: 'delivery',
		apiType: 'delivery',
		label: '同城送达'
	}
];

export const mapReturnMethodToApi = (uiKey = 'store') => {
	if (uiKey === 'pickup' || uiKey === 'express') return 'delivery';
	const hit = RETURN_METHOD_OPTIONS.find((item) => item.key === uiKey);
	return hit?.apiType || uiKey;
};

export const mapReturnMethodFromApi = (apiType = 'store') => {
	if (apiType === 'pickup_point' || apiType === 'delivery') return 'delivery';
	return RETURN_METHOD_OPTIONS.find((item) => item.apiType === apiType)?.key || 'store';
};

export const getReturnMethodLabel = (apiOrUiKey = 'store') => {
	if (apiOrUiKey === 'pickup_point' || apiOrUiKey === 'pickup' || apiOrUiKey === 'express') {
		return '同城送达';
	}
	const byApi = RETURN_METHOD_OPTIONS.find((item) => item.apiType === apiOrUiKey);
	if (byApi) return byApi.label;
	return RETURN_METHOD_OPTIONS.find((item) => item.key === apiOrUiKey)?.label || '到店归还';
};

export const getOrderStatusInfo = (status = 'pending_payment') =>
	ORDER_STATUS_MAP[status] || ORDER_STATUS_MAP.pending_payment;

export const formatOrderDateTime = (value) => {
	if (!value) return '';
	return String(value).replace('T', ' ').slice(0, 16);
};

const ORDER_LOG_STATUS_LABEL = {
	pending_payment: '待支付',
	waiting_pickup: '待取货',
	delivering: '配送中',
	renting: '租赁中',
	returning: '待归还',
	inspecting: '验收中',
	completed: '已完成',
	cancelled: '已取消'
};

/** 订单日志 → 时间轴（最新在前） */
export const mapOrderLogsToTimeline = (logs = []) => {
	if (!Array.isArray(logs) || !logs.length) return [];
	return [...logs]
		.sort((a, b) => String(b.createTime || '').localeCompare(String(a.createTime || '')))
		.map((log) => ({
			title: log.remark || ORDER_LOG_STATUS_LABEL[log.toStatus] || log.toStatus || '状态更新',
			time: formatOrderDateTime(log.createTime)
		}));
};

const buildStoreFromPickup = (pickupVo) => {
	const vo = pickupVo || {};
	const fullAddress = vo.fullAddress ||
		[vo.province, vo.city, vo.district, vo.storeAddress].filter(Boolean).join(' ') ||
		vo.storeAddress ||
		'';
	const merchantName = vo.merchantName || '';
	const storeName = vo.storeName || '';
	return {
		id: vo.pickupStoreId,
		merchantId: vo.merchantId,
		merchantName,
		name: merchantName || storeName || '取货门店',
		branchName: merchantName && storeName && merchantName !== storeName ? storeName : '',
		address: fullAddress,
		detailAddress: vo.storeAddress || '',
		province: vo.province || '',
		city: vo.city || '',
		district: vo.district || '',
		contact: vo.storePhone || '',
		businessHours: vo.businessHours || '',
		facadeImage: vo.facadeImage || '',
		lat: Number(vo.latitude ?? vo.lat ?? 0),
		lng: Number(vo.longitude ?? vo.lng ?? 0)
	};
};

/** 订单商品明细 VO → 页面展示结构 */
export const mapOrderItemToView = (item = {}, fallbackRentType = 'day') => ({
	id: item.goodsId || item.id,
	name: item.goodsName || '租赁商品',
	image: item.coverImage || '/static/logo.png',
	specLabel: item.specInfo || getRentTypeLabel(item.rentType || fallbackRentType) || '默认规格',
	packageLabel: item.packageName || '',
	packageDesc: item.packageDesc || '',
	count: Number(item.qty || 1),
	unitPrice: Number(item.rentPrice || 0),
	depositAmount: Number(item.depositAmount || 0)
});

/** 列表 VO → 卡片展示结构 */
export const mapOrderListVoToView = (vo = {}) => {
	const renewRecords = vo.renewRecords || [];
	const lastRenew = renewRecords[renewRecords.length - 1];
	const mappedItems = (vo.items || []).map((item) => mapOrderItemToView(item, vo.rentType));
	const fallbackItem = {
		id: vo.id,
		name: vo.goodsSummary || '租赁商品',
		image: vo.coverImage || '/static/logo.png',
		specLabel: getRentTypeLabel(vo.rentType),
		packageLabel: '',
		packageDesc: '',
		count: 1
	};
	return {
		id: vo.id,
		orderNo: vo.orderNo,
		status: vo.status,
		goodsItems: mappedItems.length ? mappedItems : [fallbackItem],
		rentModeLabel: getRentTypeLabel(vo.rentType),
		rentType: vo.rentType || 'day',
		startTime: vo.startTime || '',
		endTime: vo.endTime || '',
		rangeText: formatRentRangeText(vo.startTime, vo.endTime),
		days: Number(vo.days || 1),
		pickupType: mapPickupTypeFromApi(vo.pickupType),
		payAmount: Number(vo.payAmount || 0),
		createTime: formatOrderDateTime(vo.createTime),
		renewDays: lastRenew?.extraDays,
		renewAmount: lastRenew ? Number(lastRenew.renewAmount || 0) : undefined,
		returnDate: vo.returnApply?.returnDate,
		returnMethodLabel: vo.returnApply?.returnMethod ? getReturnMethodLabel(vo.returnApply.returnMethod) : ''
	};
};

/** 详情 VO → 页面展示结构 */
export const mapOrderDetailVoToView = (vo = {}, pickupVo = null) => {
	const items = (vo.items || []).map((item) => mapOrderItemToView(item, vo.rentType));
	const renewRecords = vo.renewRecords || [];
	const lastRenew = renewRecords[renewRecords.length - 1];
	return {
		id: vo.id,
		orderNo: vo.orderNo,
		status: vo.status,
		goodsItems: items.length ? items : [{
			id: vo.id,
			name: '租赁商品',
			image: '/static/logo.png',
			specLabel: getRentTypeLabel(vo.rentType),
			packageLabel: '',
			packageDesc: '',
			count: 1,
			unitPrice: Number(vo.rentAmount || 0),
			depositAmount: Number(vo.depositAmount || 0)
		}],
		rentModeLabel: getRentTypeLabel(vo.rentType),
		rentType: vo.rentType || 'day',
		startTime: vo.startTime || '',
		endTime: vo.endTime || '',
		rangeText: formatRentRangeText(vo.startTime, vo.endTime),
		days: Number(vo.days || 1),
		pickupType: mapPickupTypeFromApi(vo.pickupType),
		store: buildStoreFromPickup(pickupVo),
		contactName: vo.contactName || '',
		contactPhone: vo.contactPhone || '',
		couponAmount: Number(vo.couponAmount || 0),
		deliveryAmount: Number(vo.deliveryAmount || 0),
		rentAmount: Number(vo.rentAmount || 0),
		depositAmount: Number(vo.depositAmount || 0),
		payAmount: Number(vo.payAmount || 0),
		pickupCode: vo.pickupCode || pickupVo?.pickupCodeDisplay || pickupVo?.pickupCode || '',
		createTime: formatOrderDateTime(vo.createTime),
		renewDays: lastRenew?.extraDays,
		renewAmount: lastRenew ? Number(lastRenew.renewAmount || 0) : undefined,
		returnDate: vo.returnApply?.returnDate,
		returnMethodLabel: vo.returnApply?.returnMethod ? getReturnMethodLabel(vo.returnApply.returnMethod) : '',
		returnRemark: vo.returnApply?.remark || '',
		returnImages: Array.isArray(vo.returnApply?.imageUrls) ? vo.returnApply.imageUrls.filter(Boolean) : [],
		inspectionRemark: vo.inspection?.remark || '',
		inspectionImages: Array.isArray(vo.inspection?.imageUrls) ? vo.inspection.imageUrls.filter(Boolean) : [],
		inspectionDeductAmount: vo.inspection ? Number(vo.inspection.deductAmount ?? 0) : undefined,
		inspectionRefundAmount: vo.inspection ? Number(vo.inspection.refundAmount ?? 0) : undefined,
		timeline: mapOrderLogsToTimeline(vo.logs),
		pickupView: pickupVo ? mapPickupCodeVoToView(pickupVo, {
			rangeText: formatRentRangeText(vo.startTime, vo.endTime),
			contactName: vo.contactName,
			contactPhone: vo.contactPhone,
			createTime: formatOrderDateTime(vo.createTime),
			status: vo.status
		}) : null
	};
};

/** 取货凭证 VO → 页面结构 */
export const mapPickupCodeVoToView = (vo = {}, orderView = null) => ({
	orderId: vo.orderId,
	orderNo: vo.orderNo,
	pickupCode: vo.pickupCodeDisplay || vo.pickupCode || '',
	pickupCodeRaw: vo.pickupCode || '',
	used: Boolean(vo.used),
	verified: Boolean(vo.verified ?? vo.used),
	orderStatus: vo.orderStatus || orderView?.status || '',
	rangeText: orderView?.rangeText || '',
	contactName: vo.contactName || orderView?.contactName || '',
	contactPhone: vo.contactPhone || orderView?.contactPhone || '',
	createTime: orderView?.createTime || '',
	store: buildStoreFromPickup(vo)
});

/** 订单列表：待取货/配送中状态 */
export const isPickupStatus = (status = '') =>
	status === 'waiting_pickup' || status === 'delivering';

/** 续租费用试算：续租总额 / 新增天数 → 单价（与 renew 页一致） */
export const calcRenewUnitPrice = (renewAmount, extraDays) => {
	const amount = Number(renewAmount || 0);
	const days = Number(extraDays || 0);
	if (!amount || !days) return '0.0';
	return (amount / days).toFixed(1);
};

/** 订单列表操作按钮：按业务状态返回应展示的 action key */
export const resolveOrderListActions = (status = '') => {
	const actions = [];
	if (status === 'pending_payment') {
		actions.push('cancel', 'pay');
	} else if (isPickupStatus(status)) {
		actions.push('cancel', 'pickup');
	} else if (status === 'renting') {
		actions.push('renew', 'return');
	} else if (status === 'returning' || status === 'inspecting' || status === 'completed' || status === 'cancelled') {
		actions.push('detail');
	}
	return actions;
};

/** 归还申请提交前校验 */
export const validateReturnSubmit = ({
	returnDate = ''
} = {}) => {
	if (!String(returnDate).trim()) {
		return {
			ok: false,
			message: '请选择归还日期'
		};
	}
	return {
		ok: true,
		message: ''
	};
};

/** 确认订单提交前校验：取货方式 / 地址或点位 / 协议 */
export const validateConfirmSubmit = ({
	contactName = '',
	contactPhone = '',
	agreed = false,
	pickupType = 'store',
	address = null,
	store = null
} = {}) => {
	if (!String(contactName).trim() || !String(contactPhone).trim()) {
		return {
			ok: false,
			message: '请完善联系人信息'
		};
	}
	if (!agreed) {
		return {
			ok: false,
			message: '请先同意租赁协议'
		};
	}
	if (pickupType === 'delivery') {
		if (!address?.id && !String(address?.address || '').trim()) {
			return {
				ok: false,
				message: '请选择配送地址'
			};
		}
	} else if (!store?.id) {
		return {
			ok: false,
			message: '请选择取货门店'
		};
	}
	return {
		ok: true,
		message: ''
	};
};
