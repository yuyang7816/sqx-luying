import { get, postJson, resolveBusinessResponse } from '@/common/httpRequest';

/** 热门搜索词（公开接口） */
export async function fetchHotKeywords() {
	return get('app/rental/search/hot', {}, { hasToken: false }).then(resolveBusinessResponse);
}

/** 商品详情（公开接口） */
export async function fetchGoodsDetail(id) {
	if (!id) {
		throw new Error('商品ID不能为空');
	}
	return get(`app/rental/goods/detail/${id}`, {}, { hasToken: false }).then(resolveBusinessResponse);
}

/** 时段库存校验（公开接口，下单前试算） */
export async function checkStock(payload = {}) {
	const body = {
		goodsId: payload.goodsId,
		specId: payload.specId ?? 0,
		startTime: payload.startTime,
		endTime: payload.endTime,
		quantity: payload.quantity ?? 1
	};
	return postJson('app/rental/stock/check', body, { hasToken: false }).then(resolveBusinessResponse);
}
