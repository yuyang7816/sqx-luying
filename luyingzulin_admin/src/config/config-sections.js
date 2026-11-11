/**
 * 系统配置页独立管理的开关/分区定义。
 * 在此登记的 paramKey 不会重复出现在「开关配置 / 参数配置」通用列表中。
 */
import { INVITE_POSTER_CONFIG_KEY, SERVICE_CONFIG_KEY } from '@/constants/appUserConfig'

/** @type {Array<{ key: string, label: string, note?: string }>} */
export const FEATURE_SWITCHES = [
  {
    key: 'sys_oa_oauth_enabled',
    label: '公众号授权登录',
    note: '开启后，用户在微信内置浏览器 H5 可使用公众号 OAuth 登录，并展示微信支付；关闭时仅支持验证码/密码登录与余额支付',
  },
]

/** @type {Record<string, { title: string, description?: string, settings: typeof FEATURE_SWITCHES }>} */
export const CONFIG_SECTIONS = {
  'feature-switches': {
    title: '功能开关',
    description: '影响用户端 H5/小程序行为的功能开关，切换后会自动刷新配置缓存',
    settings: FEATURE_SWITCHES,
  },
}

export const SYSTEM_CONFIG_TAB_SECTIONS = ['feature-switches']

const MANAGED_APP_CONFIG_KEYS = [SERVICE_CONFIG_KEY, INVITE_POSTER_CONFIG_KEY]

export function getConfigSection(sectionId) {
  return CONFIG_SECTIONS[sectionId] || null
}

/** 系统配置页各 Tab 已接管的 paramKey 集合 */
export function getSystemConfigTabKeys() {
  const keys = SYSTEM_CONFIG_TAB_SECTIONS.flatMap((sectionId) => {
    const section = CONFIG_SECTIONS[sectionId]
    return (section?.settings || []).map((item) => item.key)
  })
  return new Set([...keys, ...MANAGED_APP_CONFIG_KEYS])
}
