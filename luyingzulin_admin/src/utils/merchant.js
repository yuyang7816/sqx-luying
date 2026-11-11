/** 入驻申请状态 */
export const APPLY_STATUS_OPTIONS = [
  { label: '待提交', value: 'pending' },
  { label: '审核中', value: 'reviewing' },
  { label: '已通过', value: 'approved' },
  { label: '已驳回', value: 'rejected' },
]

/** 商户状态 */
export const MERCHANT_STATUS_OPTIONS = [
  { label: '启用', value: 'active' },
  { label: '冻结', value: 'frozen' },
]

const APPLY_STATUS_MAP = Object.fromEntries(APPLY_STATUS_OPTIONS.map((item) => [item.value, item.label]))
const MERCHANT_STATUS_MAP = Object.fromEntries(MERCHANT_STATUS_OPTIONS.map((item) => [item.value, item.label]))

export function getApplyStatusMeta(status) {
  const label = APPLY_STATUS_MAP[status] || status || '-'
  if (status === 'reviewing') return { label, type: 'warning' }
  if (status === 'approved') return { label, type: 'success' }
  if (status === 'rejected') return { label, type: 'danger' }
  return { label, type: 'info' }
}

export function getMerchantStatusMeta(status) {
  const label = MERCHANT_STATUS_MAP[status] || status || '-'
  if (status === 'active') return { label, type: 'success' }
  if (status === 'frozen') return { label, type: 'danger' }
  return { label, type: 'info' }
}

export function validateApplyRejectReason(auditResult, reason) {
  if (auditResult === 'rejected' && !String(reason || '').trim()) {
    return '驳回时必须填写原因'
  }
  return ''
}

export function buildApplyAuditPayload(form) {
  return {
    applyId: form.applyId,
    auditResult: form.auditResult,
    rejectReason: form.auditResult === 'rejected' ? String(form.rejectReason || '').trim() : undefined,
  }
}
