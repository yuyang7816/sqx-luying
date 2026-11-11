/** Banner 跳转类型：与 url 字段编码约定，供管理端配置与 uniapp 解析 */
export const BANNER_JUMP_TYPES = [
  { value: 'goods', label: '商品详情' },
  { value: 'category', label: '分类' },
  { value: 'link', label: '外链' },
  { value: 'h5', label: 'H5' },
]

const JUMP_PREFIX = /^(goods|category|link|h5):([\s\S]*)$/

/** 从 Banner.url 解析跳转类型与目标 */
export function parseBannerJump(url = '') {
  const raw = String(url || '').trim()
  const matched = raw.match(JUMP_PREFIX)
  if (!matched) {
    return {
      jumpType: raw ? 'link' : '',
      jumpTarget: raw,
    }
  }
  return {
    jumpType: matched[1],
    jumpTarget: matched[2],
  }
}

/** 表单字段合成 Banner.url */
export function composeBannerJump(jumpType, jumpTarget) {
  const type = String(jumpType || '').trim()
  const target = String(jumpTarget || '').trim()
  if (!type || !target) return ''
  return `${type}:${target}`
}

export function getBannerJumpLabel(jumpType) {
  return BANNER_JUMP_TYPES.find((item) => item.value === jumpType)?.label || jumpType || '-'
}
