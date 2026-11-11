import { get, post, resolveBusinessResponse } from '@/utils/request'
import { resolvePageData } from '@/api/admin'

const PREFIX = 'admin/merchant'

/** 商户分页 */
export function getMerchantPage(data = {}) {
  return post(`${PREFIX}/page`, data).then(resolveBusinessResponse).then(resolvePageData)
}

/** 商户详情 */
export function getMerchantDetail(id) {
  return get(`${PREFIX}/detail/${id}`).then(resolveBusinessResponse)
}

/** 启用/冻结商户 */
export function updateMerchantStatus(data) {
  return post(`${PREFIX}/updateStatus`, data).then(resolveBusinessResponse)
}

/** 调整商户保证金 */
export function adjustMerchantSecurityDeposit(data) {
  return post(`${PREFIX}/deposit/adjust`, data).then(resolveBusinessResponse)
}

/** 商户保证金退还申请分页 */
export function getMerchantDepositRefundPage(data = {}) {
  return post(`${PREFIX}/deposit/refund/page`, data).then(resolveBusinessResponse).then(resolvePageData)
}

/** 审核商户保证金退还 */
export function auditMerchantDepositRefund(data) {
  return post(`${PREFIX}/deposit/refund/audit`, data).then(resolveBusinessResponse)
}

/** 商户下拉选项（最多 200 条） */
export async function getMerchantOptions(keyword = '') {
  const data = await getMerchantPage({ page: 1, limit: 200, keyword: keyword || undefined })
  return (data.list || []).map((item) => ({
    label: `${item.name} (#${item.id})`,
    value: item.id,
  }))
}
