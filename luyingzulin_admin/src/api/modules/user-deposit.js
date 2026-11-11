import { post, resolveBusinessResponse } from '@/api/http'
import { resolvePageData } from '@/api/admin'

/** 押金账户分页 */
export function getUserDepositPage(params = {}) {
  return post('admin/user/deposit/page', params)
    .then(resolveBusinessResponse)
    .then(resolvePageData)
}

/** 押金流水分页（按用户） */
export function getUserDepositLogPage(params = {}) {
  return post('admin/user/deposit/log/page', params)
    .then(resolveBusinessResponse)
    .then(resolvePageData)
}

/** 人工退押 */
export function refundUserDeposit(data) {
  return post('admin/user/deposit/refund', data).then(resolveBusinessResponse)
}

/** 人工调押 */
export function adjustUserDeposit(data) {
  return post('admin/user/deposit/adjust', data).then(resolveBusinessResponse)
}

/** 用户退押申请分页 */
export function getUserDepositRefundPage(params = {}) {
  return post('admin/user/deposit/refund/page', params)
    .then(resolveBusinessResponse)
    .then(resolvePageData)
}

/** 审核用户退押申请 */
export function auditUserDepositRefund(data) {
  return post('admin/user/deposit/refund/audit', data).then(resolveBusinessResponse)
}
