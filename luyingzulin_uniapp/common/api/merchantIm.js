import { get, postJson, resolveBusinessResponse } from '@/common/httpRequest';
import { resolvePageData } from '@/common/utils/pageData';

/** 打开/创建与用户的 IM 会话 */
export async function openUserConversation(userId) {
	if (!userId) {
		throw new Error('用户ID不能为空');
	}
	return get(`app/merchant/im/conversation/open/${userId}`).then(resolveBusinessResponse);
}

/** 商户 IM 会话列表 */
export async function fetchMerchantImConversations({ page = 1, limit = 50, silentError = false } = {}) {
	return postJson('app/merchant/im/conversation/page', {
		page,
		limit
	}, { silentError }).then(resolveBusinessResponse).then(resolvePageData);
}

/** 商户 IM 未读消息总数 */
export async function fetchMerchantImUnreadCount(options = {}) {
	try {
		const { list } = await fetchMerchantImConversations({
			page: 1,
			limit: 100,
			silentError: options.silentError
		});
		return (list || []).reduce((sum, item) => sum + (Number(item.unreadCount) || 0), 0);
	} catch (error) {
		if (options.silentError) {
			return 0;
		}
		throw error;
	}
}

/** 商户会话消息分页 */
export async function fetchMerchantImMessages(conversationId, { page = 1, limit = 50 } = {}) {
	return postJson('app/merchant/im/message/page', {
		conversationId,
		page,
		limit
	}).then(resolveBusinessResponse).then(resolvePageData);
}

/** 商户发送文本消息 */
export async function sendMerchantImText(conversationId, content) {
	return postJson('app/merchant/im/message/send', {
		conversationId,
		content: String(content || '').trim()
	}).then(resolveBusinessResponse);
}

/** 商户标记会话已读 */
export async function markMerchantImConversationRead(conversationId) {
	return postJson(`app/merchant/im/conversation/read/${conversationId}`, {}).then(resolveBusinessResponse);
}
