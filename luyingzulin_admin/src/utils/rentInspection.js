/** 设备状况文案（与 server 枚举一致） */
export const DEVICE_CONDITION_MAP = {
  good: { label: '完好', type: 'success' },
  minor_damage: { label: '轻微损坏', type: 'warning' },
  major_damage: { label: '严重损坏', type: 'danger' },
}

/** 复核状态文案 */
export const INSPECTION_AUDIT_STATUS_MAP = {
  pending: { label: '待复核', type: 'warning' },
  approved: { label: '已通过', type: 'success' },
  rejected: { label: '已驳回', type: 'danger' },
}

/** 续租支付状态 */
export const RENEW_PAY_STATUS_MAP = {
  pending: { label: '待支付', type: 'warning' },
  paid: { label: '已支付', type: 'success' },
  cancelled: { label: '已取消', type: 'info' },
}

/** 归还方式 */
export const RETURN_METHOD_MAP = {
  store: '到店归还',
  pickup_point: '同城送达',
  delivery: '同城送达',
}

/** 归还申请状态 */
export const RETURN_APPLY_STATUS_MAP = {
  pending: { label: '待处理', type: 'warning' },
  approved: { label: '已确认', type: 'success' },
  rejected: { label: '已拒绝', type: 'danger' },
}

export function getDeviceConditionMeta(code) {
  return DEVICE_CONDITION_MAP[code] || { label: code || '-', type: 'info' }
}

export function getInspectionAuditMeta(code) {
  return INSPECTION_AUDIT_STATUS_MAP[code] || { label: code || '-', type: 'info' }
}

export function getRenewPayStatusMeta(code) {
  return RENEW_PAY_STATUS_MAP[code] || { label: code || '-', type: 'info' }
}

export function formatReturnMethod(code) {
  return RETURN_METHOD_MAP[code] || code || '-'
}

export function formatReturnApplyStatus(code) {
  return RETURN_APPLY_STATUS_MAP[code]?.label || code || '-'
}

export function canAuditInspection(row) {
  return row?.auditStatus === 'pending'
}
