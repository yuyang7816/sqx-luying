/** 性别文案 */
export const DEFAULT_AVATAR_URL = 'https://tu.mianyazuju.com/2026/09/01/eed55c6a-a41e-4a4a-b61a-d29d1ded313a.png'

/** 解析用户头像，空值时使用默认头像 */
export function resolveUserAvatar(avatar) {
  const value = String(avatar || '').trim()
  return value || DEFAULT_AVATAR_URL
}

export function formatUserSex(sex) {
  if (sex === 1) return '男'
  if (sex === 2) return '女'
  return '-'
}

/** 用户状态 Tag 元数据（UserListView 状态列） */
export function getUserStatusMeta(status) {
  if (Number(status) === 1) {
    return { label: '正常', type: 'success' }
  }
  return { label: '禁用', type: 'danger' }
}

/** 管理端展示完整手机号（优先明文，兼容嵌套 userEntity） */
export function resolveUserPhone(row = {}) {
  const user = row.userEntity
  return row.phone || user?.phone || row.phoneMasked || user?.phoneMasked || '-'
}

/** @deprecated 使用 resolveUserPhone */
export function formatPhoneColumn(row = {}) {
  return resolveUserPhone(row)
}

/** 用户展示名：优先用户名，其次手机号（兼容嵌套 userEntity） */
export function resolveUserDisplayName(row = {}) {
  const user = row.userEntity
  return row.userName || user?.userName || resolveUserPhone(row) || '-'
}

/** 构建 C 端用户分页查询参数（UserListView 筛选项） */
import { filterNumberParam } from './listFilter'

export function buildAppUserListParams(filters = {}, pageNum = 1, pageSize = 10) {
  return {
    page: pageNum,
    limit: pageSize,
    username: filters.username?.trim() || undefined,
    phone: filters.phone?.trim() || undefined,
    status: filterNumberParam(filters.status),
    platform: filters.platform?.trim() || undefined,
    invitationCode: filters.invitationCode?.trim() || undefined,
    createTimeStart: filters.createTimeStart || undefined,
    createTimeEnd: filters.createTimeEnd || undefined,
  }
}
