/**
 * 解析 /file/upload 返回的可访问 URL
 * 服务端 UploadRes：path（完整 URL）> relativePath（相对路径）
 */
export function resolveUploadUrl(result = {}) {
  const path = String(result.path || result.url || '').trim()
  if (path) return path
  const relativePath = String(result.relativePath || '').trim()
  if (!relativePath) return ''
  if (/^https?:\/\//i.test(relativePath)) return relativePath
  return relativePath.startsWith('/') ? relativePath : `/${relativePath}`
}

/** 构造 el-upload onSuccess 回调数据 */
export function buildUploadSuccessPayload(result = {}) {
  const url = resolveUploadUrl(result)
  return { url, ...result }
}
