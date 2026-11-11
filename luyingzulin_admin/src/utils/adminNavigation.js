/** 管理后台跨页面导航：用户详情、订单详情 */

export function buildUserDetailQuery(userId) {
  const id = Number(userId)
  if (!id) return null
  return { path: '/users', query: { userId: id } }
}

export function buildOrderDetailRoute(orderId) {
  const id = Number(orderId)
  if (id) {
    return { name: 'rental-order-detail', params: { id } }
  }
  return null
}
