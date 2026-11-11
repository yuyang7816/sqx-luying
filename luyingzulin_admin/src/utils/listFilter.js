export function filterParam(value) {
  return isFilterActive(value) ? value : undefined
}

export function filterNumberParam(value) {
  return isFilterActive(value) ? Number(value) : undefined
}

/** 下拉「全部」哨兵值（Element Plus 需 null/undefined 才显示 placeholder） */
export const FILTER_ALL = null

export function isFilterActive(value) {
  return value !== null && value !== undefined && value !== ''
}

export function resolveOptionLabel(value, options = []) {
  const hit = options.find((item) => item.value === value || String(item.value) === String(value))
  return hit?.label || ''
}

/** 枚举 map 查中文文案，兼容 number / string key */
export function lookupEnumLabel(map = {}, value, fallback = '-') {
  if (!isFilterActive(value)) return fallback
  const textKey = String(value).trim()
  if (Object.prototype.hasOwnProperty.call(map, textKey)) return map[textKey]
  const numKey = Number(textKey)
  if (!Number.isNaN(numKey) && Object.prototype.hasOwnProperty.call(map, numKey)) return map[numKey]
  return fallback
}

/** 枚举 meta map 查 label/type，兼容 number / string key */
export function lookupEnumMeta(map = {}, value, fallback = { label: '-', type: 'info' }) {
  if (!isFilterActive(value)) return fallback
  const textKey = String(value).trim()
  if (map[textKey]) return map[textKey]
  const numKey = Number(textKey)
  if (!Number.isNaN(numKey) && map[numKey]) return map[numKey]
  return fallback
}

/**
 * 构建筛选回显项
 * @param {Array<{ key: string, label: string, value: any, options?: Array, format?: Function }>} fields
 */
export function buildFilterSummary(fields = []) {
  const items = []
  fields.forEach(({ key, label, value, options, format }) => {
    if (!isFilterActive(value)) return
    const text = format ? format(value) : (options?.length ? resolveOptionLabel(value, options) : String(value))
    if (!text) return
    items.push({ key, label, text })
  })
  return items
}

/** 用户维度筛选项（手机号 / 用户ID） */
export function buildUserFilterSummary(filters = {}) {
  if (filters.phone?.trim()) {
    return [{ key: 'phone', label: '手机号', text: filters.phone.trim() }]
  }
  if (filters.userId) {
    return [{ key: 'userId', label: '用户ID', text: String(filters.userId) }]
  }
  return []
}
