import { get, post, resolveBusinessResponse } from '@/api/http'
import { resolvePageData } from '@/api/admin'
import { getRentOrderPage } from '@/api/rental/order'

/** M05 管理端 C 端用户分页（多维筛选） */
export function getAppUserPage(params = {}) {
  return post('admin/user/list', params)
    .then(resolveBusinessResponse)
    .then(resolvePageData)
}

/** M05 用户详情（管理端含明文手机号） */
export function getAppUserInfo(userId) {
  return get('admin/user/info', { userId }).then(resolveBusinessResponse)
}

export function updateAppUserApi(data) {
  return post('admin/user/update', data).then(resolveBusinessResponse)
}

export function deleteAppUserApi(data) {
  return post('admin/user/delete', data).then(resolveBusinessResponse)
}

/**
 * 通过订单关键词聚合用户订单统计（订单数、押金合计）
 * keyword 优先使用用户名，与订单列表 keyword 筛选一致
 */
export async function fetchUserOrderStats(keyword) {
  const text = String(keyword || '').trim()
  if (!text) {
    return { orderCount: 0, depositAmount: 0 }
  }
  try {
    const data = await getRentOrderPage({ page: 1, limit: 200, keyword: text })
    const list = data.list || []
    const depositAmount = list.reduce((sum, item) => sum + Number(item.depositAmount || 0), 0)
    return {
      orderCount: Number(data.total || 0),
      depositAmount,
    }
  } catch {
    return { orderCount: 0, depositAmount: 0 }
  }
}
