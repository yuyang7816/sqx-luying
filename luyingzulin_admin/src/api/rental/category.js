import { get, post, resolveBusinessResponse } from '@/utils/request'

const PREFIX = 'admin/rental/category'

/** 租赁分类树（管理端） */
export function getRentCategoryTree() {
  return get(`${PREFIX}/tree`).then(resolveBusinessResponse)
}

/** 分类详情 */
export function getRentCategoryInfo(id) {
  return post(`${PREFIX}/info`, { id }).then(resolveBusinessResponse)
}

/** 新增分类 */
export function saveRentCategory(data) {
  return post(`${PREFIX}/save`, data).then(resolveBusinessResponse)
}

/** 更新分类 */
export function updateRentCategory(data) {
  return post(`${PREFIX}/update`, data).then(resolveBusinessResponse)
}

/** 删除分类 */
export function deleteRentCategory(data) {
  return post(`${PREFIX}/delete`, data).then(resolveBusinessResponse)
}
