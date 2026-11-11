/** 统一解包 MyBatis-Plus 分页结构 */
export function resolvePageData(data) {
	const page = data?.records ? data : data?.page || data || {};
	return {
		list: page.records || page.list || [],
		total: Number(page.total || page.totalCount || 0)
	};
}

export default {
	resolvePageData
};
