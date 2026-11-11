/**
 * 兼容旧 import 路径，统一转发至 src/utils/request.js
 */
export {
  default,
  get,
  post,
  resolveBusinessResponse,
  isBusinessSuccess,
} from '@/utils/request'
