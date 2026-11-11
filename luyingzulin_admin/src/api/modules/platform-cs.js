import { get, post, resolveBusinessResponse } from '@/api/http'
import { resolvePageData } from '@/api/admin'

export function getPlatformCsConversationPage(params = {}) {
  return post('admin/platform-cs/conversation/page', params)
    .then(resolveBusinessResponse)
    .then(resolvePageData)
}

export function getPlatformCsUnreadSummary() {
  return post('admin/platform-cs/unread-summary', {}).then(resolveBusinessResponse)
}

export function openPlatformCsConversation(userId) {
  return get(`admin/platform-cs/conversation/open/${userId}`).then(resolveBusinessResponse)
}

export function getPlatformCsMessagePage(params = {}) {
  return post('admin/platform-cs/message/page', params)
    .then(resolveBusinessResponse)
    .then(resolvePageData)
}

export function sendPlatformCsReply(params = {}) {
  return post('admin/platform-cs/message/send', params).then(resolveBusinessResponse)
}

export function markPlatformCsConversationRead(conversationId) {
  return post(`admin/platform-cs/conversation/read/${conversationId}`, {}).then(resolveBusinessResponse)
}
