import { post, resolveBusinessResponse } from '@/utils/request'
import { resolvePageData } from '@/api/admin'

const PREFIX = 'admin/merchant/apply'

/** 入驻申请分页 */
export function getMerchantApplyPage(data = {}) {
  return post(`${PREFIX}/page`, data).then(resolveBusinessResponse).then(resolvePageData)
}

/** 审核入驻申请 */
export function auditMerchantApply(data) {
  return post(`${PREFIX}/audit`, data).then(resolveBusinessResponse)
}
