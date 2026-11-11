/** 租赁订单状态文案（与 server OrderStatus 枚举 code 一致） */
export const RENT_ORDER_STATUS_MAP = {
  pending_payment: { label: '待支付', type: 'warning' },
  waiting_pickup: { label: '待取货', type: 'primary' },
  delivering: { label: '配送中', type: 'primary' },
  renting: { label: '租赁中', type: 'success' },
  returning: { label: '待归还', type: 'warning' },
  inspecting: { label: '验收中', type: 'warning' },
  completed: { label: '已完成', type: 'info' },
  cancelled: { label: '已取消', type: 'info' },
  refunding: { label: '退款中', type: 'danger' },
}

/** 列表 Tab：全部 + 业务状态分组 */
export const RENT_ORDER_TABS = [
  { name: 'all', label: '全部', status: undefined },
  { name: 'pending_payment', label: '待支付', status: 'pending_payment' },
  { name: 'waiting_pickup', label: '待取货', status: 'waiting_pickup' },
  { name: 'renting', label: '租赁中', status: 'renting' },
  { name: 'returning', label: '待归还', status: 'returning' },
  { name: 'inspecting', label: '验收中', status: 'inspecting' },
  { name: 'completed', label: '已完成', status: 'completed' },
  { name: 'cancelled', label: '已取消', status: 'cancelled' },
]

/** 租赁类型文案 */
export const RENT_TYPE_MAP = {
  day: '日租',
  hour: '时租',
  overnight: '过夜租',
}

/** 取货方式文案 */
export const PICKUP_TYPE_MAP = {
  store: '门店自提',
  pickup_point: '自提点',
  delivery: '同城配送',
}

export function getRentOrderStatusMeta(status) {
  return RENT_ORDER_STATUS_MAP[status] || { label: status || '-', type: 'info' }
}

export function formatRentType(code) {
  return RENT_TYPE_MAP[code] || code || '-'
}

export function formatPickupType(code) {
  return PICKUP_TYPE_MAP[code] || code || '-'
}

export function formatMoney(value) {
  if (value === null || value === undefined || value === '') return '-'
  return `¥${Number(value).toFixed(2)}`
}

export function formatRentPeriod(row) {
  if (!row?.startTime && !row?.endTime) return '-'
  const start = row.startTime || '-'
  const end = row.endTime || '-'
  const days = row.days != null ? ` · ${row.days}天` : ''
  return `${start} ~ ${end}${days}`
}

/** 列表商品摘要：优先 goodsSummary，否则拼接明细名 */
export function formatGoodsSummary(row) {
  if (row.goodsSummary) return row.goodsSummary
  const items = row.items || []
  if (!items.length) return row.goodsName || '-'
  const names = items.map((item) => item.goodsName).filter(Boolean)
  if (!names.length) return '-'
  if (names.length === 1) return names[0]
  return `${names[0]} 等${names.length}件`
}

/** 列表商品封面：优先 coverImage，否则取首条明细封面 */
export function resolveOrderCoverImage(row) {
  if (row?.coverImage) return row.coverImage
  const items = row?.items || []
  const matched = items.find((item) => item?.coverImage)
  return matched?.coverImage || ''
}

export function canAdminCancelOrder(status) {
  return status === 'pending_payment'
}

/** 时间轴节点标题：展示状态流转 */
export function getTimelineNodeLabel(log) {
  const fromMeta = getRentOrderStatusMeta(log?.fromStatus)
  const toMeta = getRentOrderStatusMeta(log?.toStatus)
  if (log?.fromStatus && log?.toStatus && log.fromStatus !== log.toStatus) {
    return `${fromMeta.label} → ${toMeta.label}`
  }
  return toMeta.label
}

/** 操作人类型文案 */
export function formatOperatorType(type) {
  const map = {
    user: '用户',
    admin: '管理员',
    merchant: '商户',
    system: '系统',
  }
  return map[type] || type || '-'
}
