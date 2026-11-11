/** 侧边栏树形菜单：按业务域分组，待办项在 constants/menuBadge.js 配置角标 */
export const staticMenuTree = [
  {
    id: 'dashboard',
    title: '数据统计',
    path: '/dashboard',
    icon: 'DataBoard',
  },
  {
    id: 'rental-order',
    title: '订单履约',
    icon: 'List',
    children: [
      { id: 'rental-orders', title: '租赁订单', path: '/rental/orders', icon: 'Document' },
      { id: 'rental-inspections', title: '验收记录', path: '/rental/inspections', icon: 'CircleCheck' },
      { id: 'rental-stock', title: '库存管理', path: '/rental/stock', icon: 'Box' },
    ],
  },
  {
    id: 'rental-goods',
    title: '商品与定价',
    icon: 'ShoppingBag',
    children: [
      { id: 'rental-goods-list', title: '租赁商品', path: '/rental/goods', icon: 'Goods' },
      { id: 'rental-categories', title: '租赁分类', path: '/rental/categories', icon: 'Menu' },
      { id: 'rental-pricing', title: '定价配置', path: '/rental/pricing', icon: 'Coin' },
    ],
  },
  {
    id: 'merchant',
    title: '商户管理',
    icon: 'OfficeBuilding',
    children: [
      { id: 'merchant-applies', title: '入驻审核', path: '/merchant/applies', icon: 'DocumentChecked' },
      { id: 'merchant-list', title: '商户列表', path: '/merchant/list', icon: 'Shop' },
      { id: 'merchant-deposit-refunds', title: '保证金退还', path: '/merchant/deposit-refunds', icon: 'Money' },
    ],
  },
  {
    id: 'users',
    title: 'C端用户',
    path: '/users',
    icon: 'User',
  },
  {
    id: 'operations',
    title: '客服运营',
    icon: 'ChatLineRound',
    children: [
      { id: 'business-feedbacks', title: '用户反馈', path: '/business/feedbacks', icon: 'ChatLineRound' },
      { id: 'business-platform-cs', title: '官方客服会话', path: '/business/platform-cs', icon: 'Service' },
      { id: 'business-service-config', title: '客服中心配置', path: '/business/service-config', icon: 'Headset' },
      { id: 'business-messages', title: '消息管理', path: '/business/messages', icon: 'Bell' },
    ],
  },
  {
    id: 'marketing',
    title: '营销配置',
    icon: 'Picture',
    children: [
      { id: 'business-banners', title: '首页配置', path: '/business/banners', icon: 'Picture' },
      { id: 'business-coupon-templates', title: '优惠券模板', path: '/business/coupon-templates', icon: 'Ticket' },
    ],
  },
  {
    id: 'business-invite-poster-config',
    title: '邀请配置',
    path: '/business/invite-poster-config',
    icon: 'PictureFilled',
  },
  {
    id: 'business-deposits',
    title: '押金管理',
    path: '/business/deposits',
    icon: 'Wallet',
  },
  {
    id: 'business-withdraw-records',
    title: '提现管理',
    path: '/business/withdraw-records',
    icon: 'CreditCard',
  },
  {
    id: 'system-configs',
    title: '系统配置',
    path: '/system/configs',
    icon: 'Setting',
  },
  {
    id: 'system',
    title: '系统设置',
    icon: 'Tools',
    children: [
      { id: 'system-admins', title: '管理员', path: '/system/admins', icon: 'Key' },
      { id: 'system-roles', title: '角色管理', path: '/system/roles', icon: 'Grid' },
      { id: 'system-menus', title: '菜单管理', path: '/system/menus', icon: 'Notebook' },
      { id: 'system-permissions', title: '权限管理', path: '/system/permissions', icon: 'Lock' },
      { id: 'system-dicts', title: '字典管理', path: '/system/dicts', icon: 'Files' },
      { id: 'system-logs', title: '操作日志', path: '/system/logs', icon: 'Iphone' },
      { id: 'business-sms-templates', title: '短信模板', path: '/business/sms-templates', icon: 'Promotion' },
    ],
  },
]

/** 兼容旧逻辑：返回树形菜单 */
export function buildStaticMenuTree() {
  return staticMenuTree
}

/** 扁平化所有叶子菜单项 */
export function flattenMenuTree(tree = staticMenuTree) {
  return tree.flatMap((item) => {
    if (item.children?.length) {
      return item.children
    }
    return [item]
  })
}

/** @deprecated 使用 staticMenuTree */
export const staticMenuGroups = staticMenuTree
  .filter((item) => item.children?.length)
  .map((item) => ({
    id: item.id,
    title: item.title,
    items: item.children,
  }))
  .concat(
    staticMenuTree
      .filter((item) => !item.children?.length)
      .map((item) => ({ id: item.id, title: '工作台', items: [item] })),
  )

export function flattenMenuGroups(groups = staticMenuGroups) {
  return groups.flatMap((group) => group.items)
}
