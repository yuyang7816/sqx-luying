import { get, post, resolveBusinessResponse } from '@/api/http'
import { resolvePageData } from '@/api/admin'

/** 优惠券模板分页 */
export function getCouponTemplatePage(params = {}) {
  return post('admin/coupon/template/page', params)
    .then(resolveBusinessResponse)
    .then(resolvePageData)
}

/** 优惠券模板详情 */
export function getCouponTemplateInfo(id) {
  return get('admin/coupon/template/info', { id }).then(resolveBusinessResponse)
}

export function saveCouponTemplate(data) {
  return post('admin/coupon/template/save', data).then(resolveBusinessResponse)
}

export function updateCouponTemplate(data) {
  return post('admin/coupon/template/update', data).then(resolveBusinessResponse)
}

export function deleteCouponTemplate(id) {
  return post('admin/coupon/template/delete', { id }).then(resolveBusinessResponse)
}

/** 向用户发放优惠券 */
export function grantCoupon(data) {
  return post('admin/coupon/grant', data).then(resolveBusinessResponse)
}
