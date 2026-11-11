import { get, postJson, resolveBusinessResponse } from '@/common/httpRequest';
import { resolvePageData } from '@/common/utils/pageData';

/** 租赁分类树（公开接口） */
export async function fetchCategoryTree() {
	return get('app/rental/category/tree', {}, { hasToken: false }).then(resolveBusinessResponse);
}

/** 租赁商品分页（公开接口） */
export async function fetchGoodsPage(params = {}) {
	const payload = {
		page: params.page || 1,
		limit: params.limit || 10,
		categoryId: params.categoryId || undefined,
		keyword: params.keyword || undefined,
		rentType: params.rentType || undefined,
		sortBy: params.sortBy || 'default',
		status: params.status ?? 1,
		city: params.city || undefined
	};
	const data = await postJson('app/rental/goods/page', payload, { hasToken: false }).then(resolveBusinessResponse);
	return resolvePageData(data);
}

/** 我的页订单角标（需登录） */
export async function fetchOrderStatusCounts(options = {}) {
	return get('app/rental/order/status-counts', {}, options).then(resolveBusinessResponse);
}
