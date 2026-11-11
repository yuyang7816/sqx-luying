import { post, resolveBusinessResponse } from '@/api/http'
import { resolvePageData } from '@/api/admin'

export function getReferralUserCommissionPage(params = {}) {
  return post('admin/referral/userCommission/page', params)
    .then(resolveBusinessResponse)
    .then(resolvePageData)
}

export function getReferralUserCommissionInfo(userId) {
  return post('admin/referral/userCommission/info', null, { params: { userId } })
    .then(resolveBusinessResponse)
}

export function getReferralUserData(userId) {
  return post('admin/referral/userCommission/referralData', null, { params: { userId } })
    .then(resolveBusinessResponse)
}

export function getReferralUserInviteList(userId, params = {}) {
  return post('admin/referral/userCommission/inviteList', null, {
    params: {
      userId,
      page: params.page ?? 1,
      limit: params.limit ?? 20,
    },
  })
    .then(resolveBusinessResponse)
    .then(resolvePageData)
}

export function updateReferralUserCommissionApi(data) {
  return post('admin/referral/userCommission/update', data).then(resolveBusinessResponse)
}
