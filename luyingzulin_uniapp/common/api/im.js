import { get, postJson, resolveBusinessResponse } from '@/common/httpRequest';
import { resolvePageData } from '@/common/utils/pageData';

/** 打开/创建与商户的 IM 会话 */
export async function openMerchantConversation(merchantId) {
	if (!merchantId) {
		throw new Error('商户ID不能为空');
	}
	return get(`app/im/conversation/open/${merchantId}`).then(resolveBusinessResponse);
}

/** IM 会话列表 */
export async function fetchImConversations({ page = 1, limit = 50, silentError = false } = {}) {
	return postJson('app/im/conversation/page', {
		page,
		limit
	}, { silentError }).then(resolveBusinessResponse).then(resolvePageData);
}

/** IM 未读消息总数（各会话未读之和） */
export async function fetchImUnreadCount(options = {}) {
	try {
		const { list } = await fetchImConversations({
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

/** 会话消息分页 */
export async function fetchImMessages(conversationId, { page = 1, limit = 50 } = {}) {
	return postJson('app/im/message/page', {
		conversationId,
		page,
		limit
	}).then(resolveBusinessResponse).then(resolvePageData);
}

/** 发送文本消息 */
export async function sendImText(conversationId, content) {
	return postJson('app/im/message/send', {
		conversationId,
		content: String(content || '').trim()
	}).then(resolveBusinessResponse);
}

/** 标记会话已读 */
export async function markImConversationRead(conversationId) {
	return postJson(`app/im/conversation/read/${conversationId}`, {}).then(resolveBusinessResponse);
}
