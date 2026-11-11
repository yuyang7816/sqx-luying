import { postJson, resolveBusinessResponse } from '@/common/httpRequest';
import { mapFeedbackUiToSubmitBo } from '@/common/api/assetsMappers';

/** 提交意见反馈 */
export async function submitFeedback(form = {}, options = {}) {
	const body = mapFeedbackUiToSubmitBo(form);
	return postJson('app/user/feedback/submit', body, options).then(resolveBusinessResponse);
}

export const feedbackApi = {
	submit: 'app/user/feedback/submit'
};
