import { postJson, resolveBusinessResponse } from '@/common/httpRequest';
import { resolvePageData } from '@/common/utils/pageData';

/** 消息分页 */
export async function fetchMessagePage(params = {}) {
	const body = {
		page: params.page || 1,
		limit: params.limit || 20,
		isRead: params.isRead
	};
	if (params.type != null) {
		body.type = params.type;
	}
	if (params.systemNoticeOnly) {
		body.systemNoticeOnly = true;
	}
	return postJson('app/messages/page', body, { silentError: !!params.silentError })
		.then(resolveBusinessResponse)
		.then(resolvePageData);
}

/** 系统通知分页（排除 IM/官方客服类消息） */
export async function fetchSystemMessagePage(params = {}) {
	return fetchMessagePage({
		...params,
		systemNoticeOnly: true
	});
}

/** 未读系统通知数量（排除 IM/官方客服类消息） */
export async function fetchMessageUnreadCount(options = {}) {
	const { total } = await fetchSystemMessagePage({
		page: 1,
		limit: 1,
		isRead: 0,
		silentError: options.silentError
	});
	return total;
}

/** 未读汇总（系统通知 + IM） */
export async function fetchUnreadSummary(options = {}) {
	return postJson('app/messages/unread-summary', {}, { silentError: !!options.silentError })
		.then(resolveBusinessResponse);
}

/** 小程序订阅消息模板 */
export async function fetchSubscribeTemplates(options = {}) {
	return postJson('app/messages/subscribe-templates', {}, { silentError: !!options.silentError })
		.then(resolveBusinessResponse);
}

/** 消息详情 */
export async function fetchMessageInfo(id) {
	return postJson('app/messages/info', { id }).then(resolveBusinessResponse);
}

/** 标记已读 */
export async function markMessageRead(id) {
	return postJson('app/messages/read', { id }).then(resolveBusinessResponse);
}
