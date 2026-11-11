import { get, post, resolveBusinessResponse } from '@/utils/request'
import { resolvePageData } from '@/api/admin'

const PREFIX = 'admin/rental/goods'

/** 租赁商品分页（关键词/分类/状态筛选） */
export function getRentGoodsPage(params = {}) {
  return post(`${PREFIX}/page`, params)
    .then(resolveBusinessResponse)
    .then(resolvePageData)
}

/** 租赁商品详情（含规格/套餐/轮播图/库存） */
export function getRentGoodsDetail(id) {
  return get(`${PREFIX}/detail/${id}`).then(resolveBusinessResponse)
}

/** 新增租赁商品 */
export function saveRentGoods(data) {
  return post(`${PREFIX}/save`, data).then(resolveBusinessResponse)
}

/** 更新租赁商品 */
export function updateRentGoods(data) {
  return post(`${PREFIX}/update`, data).then(resolveBusinessResponse)
}

/** 删除租赁商品 */
export function deleteRentGoods(data) {
  return post(`${PREFIX}/delete`, data).then(resolveBusinessResponse)
}

/** 上下架：status 1上架 0下架 */
export function updateRentGoodsState(data) {
  return post(`${PREFIX}/updateState`, data).then(resolveBusinessResponse)
}
