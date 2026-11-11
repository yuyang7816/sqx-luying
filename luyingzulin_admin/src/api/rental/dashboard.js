import { get, resolveBusinessResponse } from '@/utils/request'
import { getRentCategoryTree } from '@/api/rental/category'
import { getRentGoodsPage } from '@/api/rental/goods'
import { getRentOrderPage } from '@/api/rental/order'
import { getRentInspectionPage } from '@/api/rental/inspection'

/** 统计分类树节点总数（含子级） */
function countCategoryNodes(nodes = []) {
  return nodes.reduce((sum, node) => {
    const children = node.children || node.list || []
    return sum + 1 + countCategoryNodes(children)
  }, 0)
}

function todayRange() {
  const start = new Date()
  start.setHours(0, 0, 0, 0)
  const end = new Date()
  end.setHours(23, 59, 59, 999)
  const pad = (n) => String(n).padStart(2, '0')
  const fmt = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  return { startTime: fmt(start), endTime: fmt(end) }
}

async function safeOrderCount(status) {
  try {
    const page = await getRentOrderPage({ page: 1, limit: 1, status })
    return page.total
  } catch {
    return 0
  }
}

async function safePendingInspectionCount() {
  try {
    const page = await getRentInspectionPage({ page: 1, limit: 1, auditStatus: 'pending' })
    return page.total
  } catch {
    return 0
  }
}

/** 租赁看板指标：优先专用接口，未就绪时降级统计 */
export async function getRentalDashboardStats() {
  try {
    const stats = await get('admin/rental/dashboard/stats').then(resolveBusinessResponse)
    return {
      rentalCategoryTotal: Number(stats?.rentalCategoryTotal ?? stats?.categoryTotal ?? 0),
      rentalGoodsTotal: Number(stats?.rentalGoodsTotal ?? stats?.goodsTotal ?? 0),
      todayOrderCount: Number(stats?.todayOrderCount ?? stats?.todayOrders ?? 0),
      todayPayAmount: Number(stats?.todayPayAmount ?? stats?.todayAmount ?? 0),
      rentingOrderCount: Number(stats?.rentingOrderCount ?? stats?.rentingCount ?? 0),
      pendingCount: Number(stats?.pendingCount ?? stats?.pendingTotal ?? 0),
      pendingReturningCount: Number(stats?.pendingReturningCount ?? 0),
      pendingInspectingCount: Number(stats?.pendingInspectingCount ?? 0),
      pendingInspectionCount: Number(stats?.pendingInspectionCount ?? 0),
      pendingFeedbackCount: Number(stats?.pendingFeedbackCount ?? 0),
      pendingMerchantApplyCount: Number(stats?.pendingMerchantApplyCount ?? 0),
      pendingWithdrawCount: Number(stats?.pendingWithdrawCount ?? 0),
      totalMerchantCount: Number(stats?.totalMerchantCount ?? 0),
      totalReferralCommission: Number(stats?.totalReferralCommission ?? 0),
      todayNewUserCount: Number(stats?.todayNewUserCount ?? 0),
      todayInviteRegisterCount: Number(stats?.todayInviteRegisterCount ?? 0),
      totalAppUserCount: Number(stats?.totalAppUserCount ?? 0),
      weekOrderCount: Number(stats?.weekOrderCount ?? 0),
      weekPayAmount: Number(stats?.weekPayAmount ?? 0),
      orderTrend: Array.isArray(stats?.orderTrend) ? stats.orderTrend : [],
      orderStatusDistribution: Array.isArray(stats?.orderStatusDistribution) ? stats.orderStatusDistribution : [],
    }
  } catch {
    // 专用看板接口未就绪，继续降级统计
  }

  let rentalCategoryTotal = 0
  let rentalGoodsTotal = 0
  let todayOrderCount = 0
  let todayPayAmount = 0

  try {
    const tree = await getRentCategoryTree()
    rentalCategoryTotal = countCategoryNodes(Array.isArray(tree) ? tree : [])
  } catch {
    rentalCategoryTotal = 0
  }

  try {
    const page = await getRentGoodsPage({ page: 1, limit: 1 })
    rentalGoodsTotal = page.total
  } catch {
    rentalGoodsTotal = 0
  }

  try {
    const { startTime, endTime } = todayRange()
    const page = await getRentOrderPage({ page: 1, limit: 1, startTime, endTime })
    todayOrderCount = page.total
    todayPayAmount = Number(page.summary?.payAmount ?? page.summary?.totalAmount ?? 0)
  } catch {
    todayOrderCount = 0
    todayPayAmount = 0
  }

  const [rentingOrderCount, returningCount, inspectingCount, pendingInspectionCount] = await Promise.all([
    safeOrderCount('renting'),
    safeOrderCount('returning'),
    safeOrderCount('inspecting'),
    safePendingInspectionCount(),
  ])

  return {
    rentalCategoryTotal,
    rentalGoodsTotal,
    todayOrderCount,
    todayPayAmount,
    rentingOrderCount,
    pendingCount: returningCount + inspectingCount + pendingInspectionCount,
    pendingReturningCount: returningCount,
    pendingInspectingCount: inspectingCount,
    pendingInspectionCount,
    pendingFeedbackCount: 0,
    pendingMerchantApplyCount: 0,
    pendingWithdrawCount: 0,
    totalMerchantCount: 0,
    totalReferralCommission: 0,
    todayNewUserCount: 0,
    todayInviteRegisterCount: 0,
    totalAppUserCount: 0,
    weekOrderCount: 0,
    weekPayAmount: 0,
    orderTrend: [],
    orderStatusDistribution: [],
  }
}
