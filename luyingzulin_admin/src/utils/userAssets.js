import { lookupEnumLabel, lookupEnumMeta } from './listFilter'

/** 优惠券模板类型（与 server CouponType 一致） */
export const COUPON_TYPE_OPTIONS = [
  { value: 'amount_off', label: '满减券' },
  { value: 'discount', label: '折扣券' },
  { value: 'delivery_free', label: '免配送券' },
  { value: 'deposit_free', label: '免押券' },
]

export const COUPON_TYPE_MAP = Object.fromEntries(
  COUPON_TYPE_OPTIONS.map((item) => [item.value, item.label]),
)

/** 模板启用状态 */
export const COUPON_TEMPLATE_STATUS_OPTIONS = [
  { value: 1, label: '启用' },
  { value: 0, label: '停用' },
]

/** 押金账户状态 */
export const DEPOSIT_ACCOUNT_STATUS_OPTIONS = [
  { value: 1, label: '正常' },
  { value: 0, label: '冻结' },
]

/** 押金流水类型 */
export const DEPOSIT_LOG_TYPE_OPTIONS = [
  { value: 'pay', label: '缴纳押金' },
  { value: 'freeze', label: '冻结押金' },
  { value: 'unfreeze', label: '解冻押金' },
  { value: 'refund', label: '退还押金' },
  { value: 'deduct', label: '扣款' },
]

export const DEPOSIT_LOG_TYPE_MAP = Object.fromEntries(
  DEPOSIT_LOG_TYPE_OPTIONS.map((item) => [item.value, item.label]),
)

/** 用户反馈类型 */
export const FEEDBACK_TYPE_OPTIONS = [
  { value: 'order', label: '订单问题' },
  { value: 'order_service', label: '订单服务' },
  { value: 'product', label: '商品问题' },
  { value: 'suggestion', label: '建议反馈' },
  { value: 'other', label: '其他' },
]

export const FEEDBACK_TYPE_MAP = Object.fromEntries(
  FEEDBACK_TYPE_OPTIONS.map((item) => [item.value, item.label]),
)

/** 反馈处理状态 */
export const FEEDBACK_STATUS_OPTIONS = [
  { value: 'pending', label: '待处理' },
  { value: 'processed', label: '已处理' },
]

/** 提现状态 */
export const WITHDRAW_STATUS_OPTIONS = [
  { value: 1, label: '发起提现' },
  { value: 2, label: '提现成功' },
  { value: 3, label: '拒绝提现' },
]

export const WITHDRAW_STATUS_MAP = {
  1: { label: '发起提现', type: 'warning' },
  2: { label: '提现成功', type: 'success' },
  3: { label: '拒绝提现', type: 'danger' },
}

/** 提现来源 */
export const WITHDRAW_SOURCE_OPTIONS = [
  { value: 1, label: '佣金提现' },
  { value: 2, label: '钱包提现' },
]

export const WITHDRAW_SOURCE_MAP = {
  1: '佣金提现',
  2: '钱包提现',
}

/** 提现账户类型 */
export const WITHDRAW_ACCOUNT_TYPE_MAP = {
  1: '微信',
  2: '支付宝',
}

/** 钱包流水交易类型 */
export const WALLET_TRANSACTION_TYPE_MAP = {
  1: '充值',
  2: '消费',
  3: '退款',
  5: '系统调整',
  6: '退保证金',
}

/** 钱包流水方向 */
export const WALLET_DIRECTION_MAP = {
  1: '收入',
  2: '支出',
}

/** 钱包流水业务类型（与 server WalletBizType 一致） */
export const WALLET_BIZ_TYPE_OPTIONS = [
  { value: 1, label: '会员购买' },
  { value: 2, label: '订单支付' },
  { value: 3, label: '活动奖励' },
  { value: 5, label: '退款' },
  { value: 6, label: '退保证金' },
  { value: 7, label: '租赁支付' },
  { value: 8, label: '押金冻结' },
  { value: 9, label: '押金退还' },
  { value: 10, label: '余额提现' },
  { value: 11, label: '提现驳回返还' },
  { value: 12, label: '余额充值' },
]

export const WALLET_BIZ_TYPE_MAP = Object.fromEntries(
  WALLET_BIZ_TYPE_OPTIONS.map((item) => [item.value, item.label]),
)

/** 钱包流水状态 */
export const WALLET_TX_STATUS_MAP = {
  1: { label: '待处理', type: 'warning' },
  2: { label: '已完成', type: 'success' },
  3: { label: '已取消', type: 'info' },
  4: { label: '失败', type: 'danger' },
}

export function getCouponTypeLabel(code) {
  return lookupEnumLabel(COUPON_TYPE_MAP, code)
}

export function getDepositLogTypeLabel(code) {
  return lookupEnumLabel(DEPOSIT_LOG_TYPE_MAP, code)
}

export function getFeedbackTypeLabel(code) {
  return lookupEnumLabel(FEEDBACK_TYPE_MAP, code)
}

export function getWithdrawStatusMeta(status) {
  return lookupEnumMeta(WITHDRAW_STATUS_MAP, status)
}

export function getWithdrawSourceLabel(sourceType) {
  return lookupEnumLabel(WITHDRAW_SOURCE_MAP, sourceType)
}

export function getWithdrawAccountTypeLabel(accountType) {
  return lookupEnumLabel(WITHDRAW_ACCOUNT_TYPE_MAP, accountType)
}

export function getWalletTransactionTypeLabel(transactionType) {
  return lookupEnumLabel(WALLET_TRANSACTION_TYPE_MAP, transactionType)
}

export function getWalletDirectionLabel(direction) {
  return lookupEnumLabel(WALLET_DIRECTION_MAP, direction)
}

export function getWalletBizTypeLabel(bizType) {
  return lookupEnumLabel(WALLET_BIZ_TYPE_MAP, bizType)
}

export function getWalletTxStatusMeta(status) {
  return lookupEnumMeta(WALLET_TX_STATUS_MAP, status)
}

/** 解析发放优惠券手机号列表（CouponTemplateView 发放表单） */
export function parseGrantPhones(phonesText) {
  return String(phonesText || '')
    .split(/[\n,，;；\s]+/)
    .map((item) => item.trim())
    .filter(Boolean)
}

/** 提现驳回原因校验（WithdrawRecordView 审核弹窗） */
export function validateWithdrawRejectReason(auditAction, reason) {
  if (auditAction === 'reject' && !String(reason || '').trim()) {
    return '驳回时必须填写原因'
  }
  return ''
}

/** 构建提现审核提交参数 */
export function buildWithdrawConfirmPayload(auditForm) {
  return {
    id: auditForm.id,
    status: auditForm.auditAction === 'approve' ? 2 : 3,
    reason: auditForm.auditAction === 'reject' ? String(auditForm.reason || '').trim() : undefined,
  }
}
