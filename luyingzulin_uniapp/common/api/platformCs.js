import { get, postJson, resolveBusinessResponse } from '@/common/httpRequest';
import { resolvePageData } from '@/common/utils/pageData';

/** 打开/创建平台官方客服会话 */
export async function openPlatformCsConversation() {
	return get('app/platform-cs/conversation/open').then(resolveBusinessResponse);
}

/** 平台官方客服消息分页 */
export async function fetchPlatformCsMessages(conversationId, { page = 1, limit = 100 } = {}) {
	return postJson('app/platform-cs/message/page', {
		conversationId,
		page,
		limit
	}).then(resolveBusinessResponse).then(resolvePageData);
}

/** 用户向官方客服发送文本 */
export async function sendPlatformCsText(conversationId, content) {
	return postJson('app/platform-cs/message/send', {
		conversationId,
		content: String(content || '').trim()
	}).then(resolveBusinessResponse);
}

/** 用户官方客服未读数 */
export async function fetchPlatformCsUnreadCount(options = {}) {
	return get('app/platform-cs/unread-count', {}, options).then(resolveBusinessResponse);
}

/** 标记官方客服会话已读 */
export async function markPlatformCsConversationRead(conversationId) {
	return postJson(`app/platform-cs/conversation/read/${conversationId}`, {}).then(resolveBusinessResponse);
}
