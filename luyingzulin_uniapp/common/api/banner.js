import { get, postJson, resolveBusinessResponse } from '@/common/httpRequest';
import { resolvePageData } from '@/common/utils/pageData';

/** 首页轮播 Banner 分页（公开接口） */
export async function fetchBannerPage(params = {}) {
	const data = await get('app/banner/page', {
		page: params.page || 1,
		limit: params.limit || 10,
		classify: params.classify ?? 1
	}, { hasToken: false }).then(resolveBusinessResponse);
	return resolvePageData(data);
}
