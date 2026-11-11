/** 所有待办类统计字段（工作台总角标） */
export const PENDING_OPS_BADGE_KEYS = [
  'pendingReturningCount',
  'pendingInspectingCount',
  'pendingInspectionCount',
  'pendingFeedbackCount',
  'pendingMerchantApplyCount',
  'pendingWithdrawCount',
  'pendingUserDepositRefundCount',
]

/** 菜单路径 -> 看板统计字段（多字段取合计） */
export const MENU_BADGE_BY_PATH = {
  '/dashboard': PENDING_OPS_BADGE_KEYS,
  '/rental/orders': ['pendingReturningCount', 'pendingInspectingCount'],
  '/rental/inspections': 'pendingInspectionCount',
  '/merchant/applies': 'pendingMerchantApplyCount',
  '/business/withdraw-records': 'pendingWithdrawCount',
  '/business/feedbacks': 'pendingFeedbackCount',
  '/business/deposits': 'pendingUserDepositRefundCount',
  '/business/platform-cs': 'platformCsUnreadMessages',
}

/** 父级菜单 -> 聚合统计字段（未配置则回退为子菜单角标合计） */
export const MENU_GROUP_BADGE_BY_ID = {
  'rental-order': 'pendingCount',
  merchant: 'pendingMerchantApplyCount',
  operations: ['pendingFeedbackCount', 'platformCsUnreadMessages'],
}

/** 订单列表 Tab -> 订单状态 code */
export const ORDER_TAB_BADGE_STATUS = {
  returning: 'returning',
  inspecting: 'inspecting',
  pending_payment: 'pending_payment',
  waiting_pickup: 'waiting_pickup',
}

export function formatBadgeValue(value, max = 99) {
  const num = Number(value || 0)
  if (num <= 0) return 0
  if (num > max) return `${max}+`
  return num
}

export function resolveBadgeCount(stats = {}, spec) {
  if (!spec) return 0
  const keys = Array.isArray(spec) ? spec : [spec]
  return keys.reduce((sum, key) => sum + Number(stats?.[key] || 0), 0)
}

export function resolveGroupBadge(stats = {}, menuId, childPaths = []) {
  const spec = MENU_GROUP_BADGE_BY_ID[menuId]
  if (spec !== undefined) {
    return resolveBadgeCount(stats, spec)
  }
  if (!childPaths.length) {
    return 0
  }
  return childPaths.reduce((sum, path) => sum + resolveBadgeCount(stats, MENU_BADGE_BY_PATH[path]), 0)
}

export function resolveOrderTabBadge(stats = {}, tabName) {
  const status = ORDER_TAB_BADGE_STATUS[tabName]
  if (!status) return 0
  if (status === 'returning') return Number(stats.pendingReturningCount || 0)
  if (status === 'inspecting') return Number(stats.pendingInspectingCount || 0)
  const distribution = stats.orderStatusDistribution || []
  const hit = distribution.find((item) => item.status === status)
  return Number(hit?.count || 0)
}
