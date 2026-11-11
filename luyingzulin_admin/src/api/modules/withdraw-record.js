import { post, resolveBusinessResponse } from '@/api/http'
import { createPageApi } from '@/api/admin'

export const withdrawRecordApi = createPageApi('admin/withdrawRecord')

export function confirmWithdrawRecordApi(data) {
  return post('admin/withdrawRecord/confirm', data).then(resolveBusinessResponse)
}
