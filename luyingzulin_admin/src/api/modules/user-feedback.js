import { get, post, resolveBusinessResponse } from '@/api/http'
import { resolvePageData } from '@/api/admin'

/** 用户反馈分页 */
export function getUserFeedbackPage(params = {}) {
  return post('admin/user/feedback/page', params)
    .then(resolveBusinessResponse)
    .then(resolvePageData)
}

/** 用户反馈详情 */
export function getUserFeedbackInfo(id) {
  return get('admin/user/feedback/info', { id }).then(resolveBusinessResponse)
}

/** 处理反馈 */
export function processUserFeedback(data) {
  return post('admin/user/feedback/process', data).then(resolveBusinessResponse)
}
