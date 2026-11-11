import { postJson, resolveBusinessResponse } from '@/common/httpRequest';

/** 商户端未读汇总（系统通知 + IM） */
export async function fetchMerchantUnreadSummary(options = {}) {
	return postJson('app/merchant/messages/unread-summary', {}, { silentError: !!options.silentError })
		.then(resolveBusinessResponse);
}
