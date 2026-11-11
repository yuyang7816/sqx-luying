import { fetchSubscribeTemplates } from '@/common/api/message';

/**
 * 引导用户订阅订单状态消息（仅微信小程序）
 */
export async function requestOrderSubscribeMessage() {
	// #ifndef MP-WEIXIN
	return { accepted: false, reason: 'not_mp' };
	// #endif

	// #ifdef MP-WEIXIN
	try {
		const templates = await fetchSubscribeTemplates({ silentError: true });
		const tmplIds = (templates?.orderNotify || []).filter(Boolean);
		if (!tmplIds.length) {
			return { accepted: false, reason: 'no_templates' };
		}
		return await new Promise((resolve) => {
			uni.requestSubscribeMessage({
				tmplIds,
				success: (res) => resolve({ accepted: true, result: res }),
				fail: (error) => resolve({ accepted: false, reason: 'denied', error })
			});
		});
	} catch (error) {
		return { accepted: false, reason: 'error', error };
	}
	// #endif
}
