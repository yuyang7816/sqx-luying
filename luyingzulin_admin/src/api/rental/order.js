import { get, post, resolveBusinessResponse } from '@/utils/request'
import { resolvePageData } from '@/api/admin'

const PREFIX = 'admin/rental/order'

/**
 * 租赁订单分页
 * @param {Object} params page/limit、status、keyword（订单ID/手机号/商品名）、startTime、endTime
 */
export function getRentOrderPage(params = {}) {
  return post(`${PREFIX}/page`, params)
    .then(resolveBusinessResponse)
    .then(resolvePageData)
}

/** 租赁订单详情（含用户、商品明细、费用、时间轴） */
export function getRentOrderDetail(id) {
  return get(`${PREFIX}/detail/${id}`).then(resolveBusinessResponse)
}

/** 管理端代取消待支付订单 */
export function cancelRentOrder(id) {
  return post(`${PREFIX}/cancel/${id}`).then(resolveBusinessResponse)
}
