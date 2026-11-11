import { computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getAppUserInfo, getAppUserPage } from '@/api/modules/app-user'
import { resolveUserPhone } from '@/utils/appUser'
import { buildUserFilterSummary } from '@/utils/listFilter'

/**
 * 运营页读取 route.query.userId / phone 预填筛选项并触发查询
 * @param {object} filters 需含 phone 字段；可选 userId 字段
 * @param {Function} loadData 查询回调
 * @param {{ runOnMounted?: boolean }} options
 */
export function useRouteUserFilter(filters, loadData, options = {}) {
  const route = useRoute()
  const { runOnMounted = true } = options

  const userFilterSummaryItems = computed(() => buildUserFilterSummary(filters))

  async function resolvePhoneByUserId(userId) {
    const id = Number(userId)
    if (!id) return ''
    try {
      const user = await getAppUserInfo(id)
      return resolveUserPhone(user)
    } catch {
      try {
        const data = await getAppUserPage({ page: 1, limit: 1, userId: id })
        return resolveUserPhone(data.list?.[0] || {})
      } catch {
        return ''
      }
    }
  }

  async function resolveUserIdByPhone(phone) {
    const text = String(phone || '').trim()
    if (!text) return null
    try {
      const data = await getAppUserPage({ page: 1, limit: 1, phone: text })
      return data.list?.[0]?.userId || null
    } catch {
      return null
    }
  }

  async function applyRouteQuery() {
    const queryUserId = route.query.userId
    const queryPhone = route.query.phone

    if (queryPhone !== undefined) {
      filters.phone = String(queryPhone || '')
    }
    if (queryUserId !== undefined) {
      filters.userId = queryUserId ? Number(queryUserId) : null
      if (filters.userId && !filters.phone?.trim()) {
        const phone = await resolvePhoneByUserId(filters.userId)
        if (phone) {
          filters.phone = phone
        }
      }
    } else if (!Object.prototype.hasOwnProperty.call(route.query, 'userId')) {
      filters.userId = null
    }

    if (!filters.userId && filters.phone?.trim()) {
      filters.userId = await resolveUserIdByPhone(filters.phone)
    }
  }

  async function syncAndLoad() {
    await applyRouteQuery()
    loadData()
  }

  watch(
    () => [route.query.userId, route.query.phone],
    () => {
      syncAndLoad()
    },
  )

  if (runOnMounted) {
    onMounted(() => {
      syncAndLoad()
    })
  }

  return { applyRouteQuery, syncAndLoad, userFilterSummaryItems }
}
