import { get, post, resolveBusinessResponse } from '@/utils/request'
import { resolvePageData } from '@/api/admin'

const PREFIX = 'admin/rental/inspection'

/**
 * 验收记录分页
 * @param {Object} params page/limit、keyword（订单ID/验收ID）、auditStatus、deviceCondition
 */
export function getRentInspectionPage(params = {}) {
  return post(`${PREFIX}/page`, params)
    .then(resolveBusinessResponse)
    .then(resolvePageData)
}

/** 验收详情（含图片、扣款、退押、复核信息） */
export function getRentInspectionDetail(id) {
  return get(`${PREFIX}/detail/${id}`).then(resolveBusinessResponse)
}

/**
 * 验收复核
 * @param {Object} data id、auditStatus（approved/rejected）、auditRemark
 */
export function auditRentInspection(data) {
  return post(`${PREFIX}/audit`, data).then(resolveBusinessResponse)
}

/** 按订单 ID 查询验收记录（详情页补充） */
export async function getRentInspectionByOrderId(orderId) {
  if (!orderId) return null
  const page = await getRentInspectionPage({ page: 1, limit: 1, keyword: String(orderId) })
  return page.list?.[0] || null
}
