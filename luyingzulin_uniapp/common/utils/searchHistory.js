const SEARCH_HISTORY_KEY = 'camp-search-history';
const MAX_HISTORY = 10;

const readList = () => {
	const stored = uni.getStorageSync(SEARCH_HISTORY_KEY);
	return Array.isArray(stored) ? stored : [];
};

/** 读取搜索历史 */
export const getSearchHistory = () => [...readList()];

/** 追加搜索词（去重、置顶） */
export const addSearchHistory = (keyword = '') => {
	const text = String(keyword || '').trim();
	if (!text) return readList();
	const next = [text, ...readList().filter((item) => item !== text)].slice(0, MAX_HISTORY);
	uni.setStorageSync(SEARCH_HISTORY_KEY, next);
	return next;
};

/** 清空搜索历史 */
export const clearSearchHistory = () => {
	uni.removeStorageSync(SEARCH_HISTORY_KEY);
	return [];
};
