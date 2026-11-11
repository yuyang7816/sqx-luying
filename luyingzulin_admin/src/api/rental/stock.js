import { post, resolveBusinessResponse } from '@/utils/request'
import { resolvePageData } from '@/api/admin'
import { getRentGoodsDetail, getRentGoodsPage, updateRentGoods } from '@/api/rental/goods'

const PREFIX = 'admin/rental/stock'

/** 库存分页（优先专用接口，未就绪时降级为商品分页） */
export async function getRentStockPage(params = {}) {
  try {
    return await post(`${PREFIX}/page`, params)
      .then(resolveBusinessResponse)
      .then(resolvePageData)
  } catch {
    const goodsPage = await getRentGoodsPage({
      page: params.page,
      limit: params.limit,
      keyword: params.keyword,
      categoryId: params.categoryId,
    })
    const list = (goodsPage.list || []).map((row) => {
      const available = row.availableStock ?? 0
      const totalStock = row.totalStock ?? null
      const occupiedStock = totalStock != null ? Math.max(totalStock - available, 0) : null
      return {
        ...row,
        goodsName: row.name,
        totalStock,
        availableStock: available,
        occupiedStock,
      }
    })
    return { list, total: goodsPage.total }
  }
}

/**
 * 时段占用查询（优先 admin 接口，降级 app 校验接口）
 * @returns {{ available: boolean, remainStock: number }}
 */
export async function checkRentStockOccupy(data) {
  try {
    return await post(`${PREFIX}/occupy/query`, data).then(resolveBusinessResponse)
  } catch {
    return post('app/rental/stock/check', data).then(resolveBusinessResponse)
  }
}

/**
 * 手动调整库存（优先 admin 接口，降级商品更新 totalStock）
 */
export async function adjustRentStock(data) {
  const { goodsId, adjustQty, reason } = data
  try {
    return await post(`${PREFIX}/adjust`, data).then(resolveBusinessResponse)
  } catch {
    const detail = await getRentGoodsDetail(goodsId)
    const currentTotal = Number(detail?.totalStock ?? detail?.availableStock ?? 0)
    const nextTotal = Math.max(currentTotal + Number(adjustQty || 0), 0)
    await updateRentGoods({
      ...detail,
      totalStock: nextTotal,
      adjustReason: reason,
    })
    return { totalStock: nextTotal }
  }
}
