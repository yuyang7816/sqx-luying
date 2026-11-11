export function getUUID() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const random = (Math.random() * 16) | 0
    const value = c === 'x' ? random : (random & 0x3) | 0x8
    return value.toString(16)
  })
}

export function getApiBaseUrl() {
  // const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:9135/sqx_fast/'
  const baseUrl = import.meta.env.VITE_API_BASE_URL || 'https://luyingzladmin.xianmaxiong.com/sqx_fast/'
  return baseUrl.endsWith('/') ? baseUrl : `${baseUrl}/`
}

export function getCaptchaUrl(uuid) {
  return `${getApiBaseUrl()}captcha.jpg?uuid=${uuid}`
}

export function isAuth(key) {
  const permissions = JSON.parse(window.sessionStorage.getItem('permissions') || '[]')
  if (permissions.includes('*')) return true
  if (permissions.indexOf(key) !== -1) return true
  // 兼容旧菜单权限标识
  const legacyMap = {
    'sys:config:list': 'allocationList:list',
    'sys:config:update': 'allocationList:update',
    'sys:config:save': 'allocationList:update',
    'sys:config:delete': 'allocationList:update',
    'sys:user:list': 'sys:user:list',
  }
  const legacy = legacyMap[key]
  return legacy ? permissions.indexOf(legacy) !== -1 : false
}

export function setPermissions(permissions = []) {
  window.sessionStorage.setItem('permissions', JSON.stringify(permissions))
}

export function clearPermissions() {
  window.sessionStorage.removeItem('permissions')
}

export function clearAuthCache() {
  window.sessionStorage.removeItem('admin-auth')
  clearPermissions()
}

const routePathMap = {
  home: '/dashboard',
  dashboard: '/dashboard',
  user: '/system/admins',
  role: '/system/roles',
  menu: '/system/menus',
  dict: '/system/dicts',
  log: '/system/logs',
  permission: '/system/permissions',
  perms: '/system/permissions',
  'sys/user': '/system/admins',
  'sys/role': '/system/roles',
  'sys/menu': '/system/menus',
  'sys/dict': '/system/dicts',
  'sys/config': '/system/configs',
  'sys/log': '/system/logs',
  userList: '/users',
  couponTemplate: '/business/coupon-templates',
  couponTemplates: '/business/coupon-templates',
  userDeposit: '/business/deposits',
  deposit: '/business/deposits',
  userFeedback: '/business/feedbacks',
  feedback: '/business/feedbacks',
  bannerList: '/business/banners',
  message: '/business/messages',
  messageList: '/business/messages',
  userWallet: '/business/wallets',
  walletTransaction: '/business/wallet-transactions',
  referralCommission: '/business/referral-commissions',
  withdrawRecord: '/business/withdraw-records',
  withdrawalAccount: '/business/withdrawal-accounts',
  smsTemplate: '/business/sms-templates',
  allocationList: '/system/configs',
  common: '/system/configs',
}

export function mapMenuUrlToPath(url) {
  const key = String(url || '').trim()
  if (key.startsWith('/')) {
    return key
  }
  return routePathMap[key] || ''
}

export function buildMissingPagePath(menuUrl = '', title = '') {
  const query = new URLSearchParams({
    menuUrl,
    title,
  })
  return `/404?${query.toString()}`
}

function isGarbledMenuName(name = '') {
  const text = String(name).trim()
  return !text || /^[?？]+$/.test(text)
}

export function normalizeMenuTree(menuList = []) {
  return menuList
    .map((item) => ({
      id: item.menuId,
      title: item.name,
      icon: item.icon,
      type: item.type,
      perms: item.perms || '',
      path: item.type === 1 ? mapMenuUrlToPath(item.url) || buildMissingPagePath(item.url, item.name) : '',
      children: normalizeMenuTree(item.list || []),
    }))
    .filter((item) => item.type !== 2 && !isGarbledMenuName(item.title))
}
