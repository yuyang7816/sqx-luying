import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getDashboardStats } from '@/api/modules/stats'
import { getPlatformCsUnreadSummary } from '@/api/modules/platform-cs'
import {
  MENU_BADGE_BY_PATH,
  resolveBadgeCount,
  resolveGroupBadge,
  resolveOrderTabBadge,
} from '@/constants/menuBadge'

const DEFAULT_COUNTS = {
  pendingCount: 0,
  pendingReturningCount: 0,
  pendingInspectingCount: 0,
  pendingInspectionCount: 0,
  pendingFeedbackCount: 0,
  pendingMerchantApplyCount: 0,
  pendingWithdrawCount: 0,
  pendingUserDepositRefundCount: 0,
  platformCsUnreadMessages: 0,
  orderStatusDistribution: [],
}

export const useMenuBadgeStore = defineStore('menuBadge', () => {
  const counts = ref({ ...DEFAULT_COUNTS })
  const loading = ref(false)
  let refreshTimer = 0

  function getPathBadge(path) {
    return resolveBadgeCount(counts.value, MENU_BADGE_BY_PATH[path])
  }

  function getGroupBadge(menuId, childPaths = []) {
    return resolveGroupBadge(counts.value, menuId, childPaths)
  }

  function getOrderTabBadge(tabName) {
    return resolveOrderTabBadge(counts.value, tabName)
  }

  async function refresh() {
    loading.value = true
    try {
      const [stats, platformCsUnread] = await Promise.all([
        getDashboardStats(),
        getPlatformCsUnreadSummary().catch(() => ({ unreadMessages: 0 })),
      ])
      counts.value = {
        ...DEFAULT_COUNTS,
        pendingCount: stats.pendingCount ?? 0,
        pendingReturningCount: stats.pendingReturningCount ?? 0,
        pendingInspectingCount: stats.pendingInspectingCount ?? 0,
        pendingInspectionCount: stats.pendingInspectionCount ?? 0,
        pendingFeedbackCount: stats.pendingFeedbackCount ?? 0,
        pendingMerchantApplyCount: stats.pendingMerchantApplyCount ?? 0,
        pendingWithdrawCount: stats.pendingWithdrawCount ?? 0,
        pendingUserDepositRefundCount: stats.pendingUserDepositRefundCount ?? 0,
        platformCsUnreadMessages: platformCsUnread?.unreadMessages ?? 0,
        orderStatusDistribution: stats.orderStatusDistribution ?? [],
      }
    } catch {
      counts.value = { ...DEFAULT_COUNTS }
    } finally {
      loading.value = false
    }
  }

  function startPolling(intervalMs = 60000) {
    stopPolling()
    refreshTimer = window.setInterval(() => {
      void refresh()
    }, intervalMs)
  }

  function stopPolling() {
    if (refreshTimer) {
      window.clearInterval(refreshTimer)
      refreshTimer = 0
    }
  }

  return {
    counts,
    loading,
    getPathBadge,
    getGroupBadge,
    getOrderTabBadge,
    refresh,
    startPolling,
    stopPolling,
  }
})
