import { get, postJson, putJson, del, resolveBusinessResponse } from '@/common/httpRequest';
import { fetchMerchantSecurityDepositSummary } from '@/common/api/merchantWallet';
import {
	mapApplyProgressTimeline,
	mapApplyProgressToSummary,
	mapDashboardRevenue,
	mapDashboardStats,
	mapGoodsDetailToForm,
	mapGoodsListVoToView,
	mapOrderDetailVoToView,
	mapOrderListVoToView,
	mapStoreDetailToForm,
	mapStoreFormToSaveBo,
	mapGoodsFormToSaveBo,
	mapDeviceConditionToApi,
	calcInspectionDeduct,
	resolvePageRecords,
	parseLinkRentOrderId,
	resolveMerchantActionLabel,
	normalizePickupCode,
	isMerchantApproved,
	canAccessMerchantWorkbench,
	shouldShowApplyStatusPage,
	canResubmitMerchantApply,
	isMerchantStaffActive,
	MERCHANT_APPLY_STATUS,
	MERCHANT_MENUS,
	MERCHANT_STEPS,
	MERCHANT_ORDER_TABS
} from '@/common/merchant/constants';

export {
	MERCHANT_MENUS,
	MERCHANT_STEPS,
	MERCHANT_ORDER_TABS,
	resolveMerchantActionLabel,
	normalizePickupCode,
	isMerchantApproved,
	canAccessMerchantWorkbench,
	shouldShowApplyStatusPage,
	canResubmitMerchantApply,
	isMerchantStaffActive,
	MERCHANT_APPLY_STATUS
};

const encodeId = (id) => encodeURIComponent(String(id || ''));

/** 入驻进度 */
export async function fetchApplyProgress() {
	const vo = await get('app/merchant/apply/progress').then(resolveBusinessResponse);
	return {
		...vo,
		summary: mapApplyProgressToSummary(vo),
		timeline: mapApplyProgressTimeline(vo)
	};
}

/** 校验商户是否已通过审核，未通过则跳转入驻状态页 */
export async function ensureMerchantApproved({ redirect = true, toast = true } = {}) {
	const progress = await fetchApplyProgress();
	const status = progress.summary?.status || progress.status;
	if (canAccessMerchantWorkbench(status)) {
		return progress;
	}
	if (redirect) {
		if (toast) {
			uni.showToast({ title: '请先完成入驻审核', icon: 'none' });
		}
		setTimeout(() => {
			uni.redirectTo({ url: '/packageMerchant/apply' });
		}, toast ? 400 : 0);
	}
	const error = new Error('NOT_APPROVED');
	error.status = status;
	throw error;
}

/** 当前登录用户员工身份 */
export async function fetchMerchantStaffProfile(options = {}) {
	const vo = await get('app/merchant/staff/me', {}, options).then(resolveBusinessResponse);
	return {
		active: Boolean(vo?.active),
		staffId: vo?.staffId,
		name: vo?.name || '',
		role: vo?.role || '',
		phone: vo?.phone || '',
		merchantId: vo?.merchantId,
		storeId: vo?.storeId,
		merchantName: vo?.merchantName || '',
		storeName: vo?.storeName || ''
	};
}

/** 商户主账号或核销员工均可进入经营端 */
export async function ensureMerchantWorkbenchAccess({ redirect = true, toast = true } = {}) {
	const staff = await fetchMerchantStaffProfile({ silentError: true });
	if (isMerchantStaffActive(staff)) {
		return { role: 'staff', staff };
	}
	const progress = await ensureMerchantApproved({ redirect, toast });
	return { role: 'owner', progress };
}

/** 仅核销员工可进入 */
export async function ensureStaffWorkbenchAccess({ redirect = true, toast = true } = {}) {
	const staff = await fetchMerchantStaffProfile({ silentError: true });
	if (isMerchantStaffActive(staff)) {
		return staff;
	}
	if (redirect && toast) {
		uni.showToast({ title: '您暂无员工权限', icon: 'none' });
	}
	const error = new Error('NOT_STAFF');
	throw error;
}

/** 校验商户保证金是否已缴足（系统配置>0 时），未缴足则跳转钱包页 */
export async function ensureMerchantSecurityDeposit({ redirect = true, toast = true } = {}) {
	const summary = await fetchMerchantSecurityDepositSummary({ silentError: true });
	if (!summary.depositRequired || summary.depositPaid) {
		return summary;
	}
	if (redirect) {
		if (toast) {
			uni.showToast({ title: '请先缴纳商户保证金', icon: 'none' });
		}
		setTimeout(() => {
			uni.navigateTo({ url: '/packageMerchant/wallet' });
		}, toast ? 400 : 0);
	}
	const error = new Error('DEPOSIT_REQUIRED');
	error.summary = summary;
	throw error;
}

/** 经营操作前校验保证金，未缴足弹窗引导 */
export async function guardMerchantOperational({ toast = true } = {}) {
	const summary = await fetchMerchantSecurityDepositSummary({ silentError: true });
	if (!summary.depositRequired || summary.depositPaid) {
		return true;
	}
	if (!toast) {
		return false;
	}
	const needPay = Number(summary.needPayAmount || summary.requiredAmount || 0).toFixed(2);
	return new Promise((resolve) => {
		uni.showModal({
			title: '需缴纳保证金',
			content: `请先缴纳商户保证金（待缴 ¥${needPay}）后再操作商品和订单`,
			confirmText: '去缴纳',
			cancelText: '取消',
			success: (res) => {
				if (res.confirm) {
					uni.navigateTo({ url: '/packageMerchant/wallet-deposit' });
				}
				resolve(false);
			},
			fail: () => resolve(false)
		});
	});
}

/** 校验商户可经营：主账号/员工身份 + 保证金已缴足（若配置要求） */
export async function ensureMerchantOperational({ redirect = true, toast = true } = {}) {
	await ensureMerchantWorkbenchAccess({ redirect, toast });
	await ensureMerchantSecurityDeposit({ redirect, toast });
}

/** 提交入驻申请 */
export async function submitApply(payload = {}) {
	const storeName = String(payload.storeName || payload.merchantName || '').trim();
	const body = {
		storeName,
		merchantName: storeName,
		contactName: payload.contactName,
		contactPhone: payload.contactPhone,
		storeAddress: payload.address || payload.storeAddress,
		province: payload.province,
		city: payload.city,
		district: payload.district,
		latitude: payload.latitude,
		longitude: payload.longitude,
		businessHours: payload.businessHours,
		serviceArea: payload.serviceArea,
		baseDeliveryFee: payload.baseDeliveryFee ? Number(payload.baseDeliveryFee) : undefined,
		freeDeliveryThreshold: payload.freeDeliveryThreshold ? Number(payload.freeDeliveryThreshold) : undefined,
		businessScopes: payload.businessScopes || ['露营装备租赁'],
		licenseUrls: payload.licenseUrls || [],
		extraUrls: payload.extraUrls || []
	};
	return postJson('app/merchant/apply/submit', body).then(resolveBusinessResponse);
}

/** 商家中心首页数据 */
export async function fetchMerchantHome() {
	const progress = await fetchApplyProgress();
	let storeName = progress.summary.storeName;
	try {
		const store = await fetchStoreDetail();
		storeName = store.name || storeName;
	} catch {
		// 未开通商户时忽略
	}
	return {
		summary: { ...progress.summary, storeName },
		menus: MERCHANT_MENUS,
		steps: MERCHANT_STEPS
	};
}

/** 工作台概览（服务端聚合） */
export async function fetchDashboardStats() {
	try {
		const vo = await get('app/merchant/dashboard/stats').then(resolveBusinessResponse);
		return mapDashboardStats(vo);
	} catch {
		const [pendingPage, reviewPage, goodsTotalPage, goodsOnShelfPage, allOrdersPage] = await Promise.all([
			postJson('app/merchant/order/page', { page: 1, limit: 1, tab: 'pending' }).then(resolveBusinessResponse).then(resolvePageRecords),
			postJson('app/merchant/order/page', { page: 1, limit: 1, tab: 'review' }).then(resolveBusinessResponse).then(resolvePageRecords),
			postJson('app/merchant/goods/page', { page: 1, limit: 1 }).then(resolveBusinessResponse).then(resolvePageRecords),
			postJson('app/merchant/goods/page', { page: 1, limit: 1, status: 1 }).then(resolveBusinessResponse).then(resolvePageRecords),
			postJson('app/merchant/order/page', { page: 1, limit: 50, tab: 'all' }).then(resolveBusinessResponse).then(resolvePageRecords)
		]);
		const today = new Date().toISOString().slice(0, 10);
		const monthPrefix = today.slice(0, 7);
		const orderList = allOrdersPage.list || [];
		const todayAmount = orderList.reduce((sum, item) => {
			const day = String(item.createTime || '').slice(0, 10);
			return day === today ? sum + Number(item.payAmount || 0) : sum;
		}, 0);
		const monthAmount = orderList.reduce((sum, item) => {
			const day = String(item.createTime || '').slice(0, 7);
			return day === monthPrefix ? sum + Number(item.payAmount || 0) : sum;
		}, 0);
		const totalAmount = orderList.reduce((sum, item) => sum + Number(item.payAmount || 0), 0);
		return mapDashboardStats({
			goodsTotalCount: goodsTotalPage.total,
			goodsOnShelfCount: goodsOnShelfPage.total,
			orderTotalCount: allOrdersPage.total,
			pendingVerifyCount: pendingPage.total,
			rentingCount: 0,
			refundedCount: 0,
			todayAmount,
			monthAmount,
			totalAmount,
			todayPickupOrders: []
		});
	}
}

/** 营业额按年月筛选 */
export async function fetchDashboardRevenue({ year, month } = {}) {
	const params = {};
	if (year) params.year = year;
	if (month) params.month = month;
	const vo = await get('app/merchant/dashboard/revenue', params).then(resolveBusinessResponse);
	return mapDashboardRevenue(vo);
}

/** 切换门店营业状态 */
export async function updateStoreBusinessStatus(open) {
	const store = await fetchStoreDetail();
	await saveStore({ ...store, businessStatus: Boolean(open) });
	return Boolean(open);
}

/** 员工列表 */
export async function fetchStaffList() {
	const list = await get('app/merchant/staff/list').then(resolveBusinessResponse);
	return Array.isArray(list) ? list : [];
}

/** 保存员工（新增/编辑） */
export async function saveStaff(payload = {}) {
	const body = {
		id: payload.id,
		storeId: payload.storeId,
		name: payload.name,
		role: payload.role,
		phone: payload.phone,
		status: payload.status ?? 1
	};
	if (body.id) {
		return putJson('app/merchant/staff/save', body).then(resolveBusinessResponse);
	}
	return postJson('app/merchant/staff/save', body).then(resolveBusinessResponse);
}

/** 删除员工 */
export async function deleteStaff(id) {
	return del(`app/merchant/staff/${encodeURIComponent(id)}`).then(resolveBusinessResponse);
}

/** 商品分页 */
export async function fetchGoodsPage({ page = 1, limit = 20, keyword, status } = {}) {
	const pageVo = await postJson('app/merchant/goods/page', {
		page,
		limit,
		keyword,
		status
	}).then(resolveBusinessResponse);
	return {
		...resolvePageRecords(pageVo),
		list: (pageVo.records || []).map((item) => mapGoodsListVoToView(item))
	};
}

/** 商品详情（编辑用） */
export async function fetchGoodsDetail(id) {
	if (!id) throw new Error('商品ID不能为空');
	const vo = await get(`app/merchant/goods/detail/${id}`).then(resolveBusinessResponse);
	const view = mapGoodsListVoToView(vo);
	return { view, form: mapGoodsDetailToForm({ ...vo, ...view }) };
}

/** 新增商品（提交后自动上架） */
export async function createGoods(form = {}) {
	const body = mapGoodsFormToSaveBo(form, { autoShelf: true });
	return postJson('app/merchant/goods/save', body).then(resolveBusinessResponse);
}

/** 更新商品 */
export async function updateGoods(id, form = {}) {
	const body = mapGoodsFormToSaveBo(form, { id });
	return postJson('app/merchant/goods/save', body).then(resolveBusinessResponse);
}

/** 上下架 */
export async function toggleGoodsShelf(goodsId, shelfStatus) {
	const body = { goodsId, status: shelfStatus ? 1 : 0 };
	return putJson('app/merchant/goods/shelf', body).then(resolveBusinessResponse);
}

/** 删除商品 */
export async function deleteGoods(id) {
	if (!id) throw new Error('商品ID不能为空');
	return del(`app/merchant/goods/${encodeURIComponent(id)}`).then(resolveBusinessResponse);
}

/** 库存调整 */
export async function adjustGoodsStock({ goodsId, adjustType = 'in', quantity = 1, remark = '' } = {}) {
	const body = { goodsId, adjustType, quantity, remark, specId: 0 };
	return postJson('app/merchant/stock/adjust', body).then(resolveBusinessResponse);
}

/** 订单分页 */
export async function fetchOrderPage({ tab = 'all', page = 1, limit = 20, keyword } = {}) {
	const pageVo = await postJson('app/merchant/order/page', { tab, page, limit, keyword })
		.then(resolveBusinessResponse);
	return {
		list: (pageVo.records || []).map(mapOrderListVoToView),
		total: Number(pageVo.total || 0)
	};
}

/** 订单详情 */
export async function fetchOrderDetail(id) {
	if (!id) throw new Error('订单ID不能为空');
	const vo = await get(`app/merchant/order/detail/${encodeId(id)}`).then(resolveBusinessResponse);
	return mapOrderDetailVoToView(vo);
}

/** 确认备货 */
export async function prepareOrder(id) {
	return postJson(`app/merchant/order/prepare/${encodeId(id)}`, {}).then(resolveBusinessResponse);
}

/** 确认取货/核销（需提货码） */
export async function pickupOrder(id, pickupCode) {
	const code = normalizePickupCode(pickupCode);
	if (!code) throw new Error('请输入提货码');
	return postJson(`app/merchant/order/pickup/${encodeId(id)}`, { pickupCode: code }).then(resolveBusinessResponse);
}

/** 按提货码核销（扫码入口） */
export async function pickupOrderByCode(pickupCode) {
	const code = normalizePickupCode(pickupCode);
	if (!code) throw new Error('请输入提货码');
	const vo = await postJson('app/merchant/order/pickup-by-code', { pickupCode: code }).then(resolveBusinessResponse);
	return mapOrderDetailVoToView(vo);
}

/** 联系用户 */
export async function fetchOrderContact(id) {
	return get(`app/merchant/order/contact/${encodeId(id)}`).then(resolveBusinessResponse);
}

/** 提交验收 */
export async function submitInspection({ orderId, condition, deductAmount, remark, imageUrls }) {
	const rentOrderId = parseLinkRentOrderId(orderId) || Number(orderId);
	const body = {
		orderId: rentOrderId,
		deviceCondition: mapDeviceConditionToApi(condition),
		deductAmount: Number(deductAmount) || 0,
		remark: remark || '',
		imageUrls: Array.isArray(imageUrls) ? imageUrls.filter(Boolean) : []
	};
	return postJson('app/merchant/inspection/submit', body).then(resolveBusinessResponse);
}

/** 门店详情 */
export async function fetchStoreDetail() {
	const vo = await get('app/merchant/store/detail').then(resolveBusinessResponse);
	return mapStoreDetailToForm(vo);
}

/** 保存门店 */
export async function saveStore(form = {}) {
	const body = mapStoreFormToSaveBo(form);
	return putJson('app/merchant/store/save', body).then(resolveBusinessResponse);
}

/** 执行订单下一步操作（备货）；核销需在详情页输入/扫码提货码 */
export async function executeMerchantOrderAction(order, { pickupCode } = {}) {
	const action = order?.nextAction;
	if (action === 'prepare') return prepareOrder(order.id);
	if (action === 'pickup') {
		if (!pickupCode) {
			const error = new Error('请先输入或扫描提货码');
			error.code = 'PICKUP_CODE_REQUIRED';
			throw error;
		}
		return pickupOrder(order.id, pickupCode);
	}
	return Promise.resolve();
}

/** @deprecated 兼容旧引用，请改用 fetchOrderPage */
export async function getMerchantOrdersByStatus(tab = 'all') {
	const { list } = await fetchOrderPage({ tab, page: 1, limit: 100 });
	return list;
}

/** @deprecated 兼容旧引用 */
export async function getMerchantOrderById(id) {
	return fetchOrderDetail(id);
}

/** @deprecated 兼容旧引用 */
export async function getMerchantGoodsById(id) {
	const { view } = await fetchGoodsDetail(id);
	return view;
}
