/** 判断是否为开关类配置（0/1、true/false 等布尔语义） */
export function isSwitchConfig(row = {}) {
  const key = String(row.paramKey || '').toLowerCase()
  const value = String(row.paramValue ?? '').trim().toLowerCase()
  const remark = String(row.remark || '')

  if (/开关|是否启用|是否开启|开启|关闭/.test(remark)) {
    return true
  }
  if (/_enabled$|_switch$|_open$|_flag$/.test(key)) {
    return true
  }
  if (key.includes('sandbox') && (value === '1' || value === '2')) {
    return true
  }

  const switchValues = new Set(['0', '1', 'true', 'false', 'yes', 'no', 'on', 'off'])
  if (!switchValues.has(value)) {
    return false
  }

  if (/rate|fee|amount|min|max|price|secret|key|appid|domain|path|url|html|json/.test(key)) {
    return false
  }
  if (/^\d+\.\d+$/.test(value)) {
    return false
  }

  return value === '0' || value === '1' || ['true', 'false', 'yes', 'no', 'on', 'off'].includes(value)
}

/** 开关当前是否开启 */
export function isSwitchOn(row = {}) {
  const value = String(row.paramValue ?? '').trim().toLowerCase()
  if (String(row.paramKey || '').includes('sandbox')) {
    return value === '1'
  }
  return ['1', 'true', 'yes', 'on'].includes(value)
}

/** 开关开启时的写入值 */
export function getSwitchOnValue(row = {}) {
  if (String(row.paramKey || '').includes('sandbox')) {
    return '1'
  }
  const value = String(row.paramValue ?? '').trim().toLowerCase()
  if (['true', 'false'].includes(value)) return 'true'
  if (['yes', 'no'].includes(value)) return 'yes'
  if (['on', 'off'].includes(value)) return 'on'
  return '1'
}

/** 开关关闭时的写入值 */
export function getSwitchOffValue(row = {}) {
  if (String(row.paramKey || '').includes('sandbox')) {
    return '2'
  }
  const value = String(row.paramValue ?? '').trim().toLowerCase()
  if (['true', 'false'].includes(value)) return 'false'
  if (['yes', 'no'].includes(value)) return 'no'
  if (['on', 'off'].includes(value)) return 'off'
  return '0'
}

/** 按 paramType 分组并拆分开关/参数 */
export function splitConfigGroups(groups = []) {
  const switchGroups = []
  const paramGroups = []

  groups.forEach((group) => {
    const list = group.list || []
    const switchList = list.filter(isSwitchConfig)
    const paramList = list.filter((item) => !isSwitchConfig(item))

    if (switchList.length) {
      switchGroups.push({ ...group, list: switchList })
    }
    if (paramList.length) {
      paramGroups.push({ ...group, list: paramList })
    }
  })

  return { switchGroups, paramGroups }
}

/** 配置分类展示顺序 */
const TYPE_ORDER = ['sys', 'app', 'rental', 'referral', 'withdraw', 'sms', 'wx', 'pay', 'file']

export function sortConfigGroups(groups = []) {
  return [...groups].sort((a, b) => {
    const ai = TYPE_ORDER.indexOf(a.paramType)
    const bi = TYPE_ORDER.indexOf(b.paramType)
    const av = ai === -1 ? 999 : ai
    const bv = bi === -1 ? 999 : bi
    if (av !== bv) return av - bv
    return String(a.paramTypeDesc || a.paramType).localeCompare(String(b.paramTypeDesc || b.paramType), 'zh-CN')
  })
}
