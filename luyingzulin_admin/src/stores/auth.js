import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { getNavApi, loginApi, logoutApi } from '@/api/modules/auth'
import { buildStaticMenuTree } from '@/router/staticMenu'
import { clearPermissions, normalizeMenuTree, setPermissions } from '@/utils/tool'

const STORAGE_KEY = 'admin-auth'

function getStoredAuth() {
  if (typeof window === 'undefined') {
    return null
  }
  const raw = window.sessionStorage.getItem(STORAGE_KEY)
  return raw ? JSON.parse(raw) : null
}

export const useAuthStore = defineStore('auth', () => {
  const stored = getStoredAuth()
  const token = ref(stored?.token || '')
  const refreshToken = ref(stored?.refreshToken || '')
  const expiresAt = ref(stored?.expiresAt || 0)
  const profile = ref(stored?.profile || { name: '', roleCode: '', roleName: '', email: '' })
  const permissions = ref(stored?.permissions || [])
  const menuList = ref(stored?.menuList || buildStaticMenuTree())
  const navLoaded = ref(Boolean((stored?.permissions || []).length || (stored?.menuList || []).length))
  const isLoggedIn = computed(() => Boolean(token.value) && expiresAt.value > Date.now())

  function persist() {
    if (token.value) {
      window.sessionStorage.setItem(STORAGE_KEY, JSON.stringify({
        token: token.value,
        refreshToken: refreshToken.value,
        expiresAt: expiresAt.value,
        profile: profile.value,
        permissions: permissions.value,
        menuList: menuList.value,
      }))
    } else {
      window.sessionStorage.removeItem(STORAGE_KEY)
    }
  }

  function applyStoredAuth(cached) {
    token.value = cached.token
    refreshToken.value = cached.refreshToken
    expiresAt.value = cached.expiresAt
    profile.value = cached.profile
    permissions.value = cached.permissions || []
    menuList.value = buildStaticMenuTree()
    navLoaded.value = Boolean(permissions.value.length || menuList.value.length)
    setPermissions(permissions.value)
  }

  function clearSession() {
    token.value = ''
    refreshToken.value = ''
    expiresAt.value = 0
    profile.value = { name: '', roleCode: '', roleName: '', email: '' }
    permissions.value = []
    menuList.value = buildStaticMenuTree()
    navLoaded.value = false
    clearPermissions()
    persist()
  }

  function extractNavPayload(navData) {
    return navData?.data || navData || {}
  }

  function resolveMenuList(rawMenuList = []) {
    const normalized = normalizeMenuTree(rawMenuList)
    menuList.value = buildStaticMenuTree()
    return normalized
  }

  async function fetchNavData() {
    const navData = await getNavApi()
    const payload = extractNavPayload(navData)
    permissions.value = payload.permissions || []
    resolveMenuList(payload.menuList || [])
    navLoaded.value = true
    setPermissions(permissions.value)
    persist()
    return payload
  }

  async function restore() {
    const cached = getStoredAuth()
    if (!cached || !cached.token || cached.expiresAt <= Date.now()) {
      clearSession()
      return false
    }
    applyStoredAuth(cached)
    if (!menuList.value.length || !menuList.value.some((item) => item.children?.length)) {
      menuList.value = buildStaticMenuTree()
      persist()
    }
    if (navLoaded.value) {
      return true
    }
    try {
      await fetchNavData()
    } catch (error) {
      clearSession()
      throw error
    }
    return true
  }

  async function login(form) {
    const data = await loginApi(form)
    token.value = data.token
    refreshToken.value = data.refreshToken || ''
    expiresAt.value = data.expiresAt || Date.now() + 2 * 60 * 60 * 1000
    profile.value = data.profile || { name: form.username, roleCode: '', roleName: '', email: '' }
    permissions.value = []
    menuList.value = []
    navLoaded.value = false
    persist()
    const navData = await fetchNavData()
    return { ...data, permissions: navData.permissions || permissions.value, menuList: menuList.value }
  }

  async function logout() {
    const hadSession = Boolean(token.value)
    clearSession()
    if (hadSession) {
      await logoutApi()
    }
  }

  function hasPermission(permission) {
    if (permissions.value.includes('*')) {
      return true
    }
    return permissions.value.includes(permission)
  }

  /** 是否允许监管新建商品（需 rent:goods:save 或超管） */
  const canSuperviseCreateGoods = computed(() => hasPermission('rent:goods:save'))

  return { token, refreshToken, expiresAt, profile, permissions, menuList, navLoaded, isLoggedIn, clearSession, restore, login, logout, hasPermission, canSuperviseCreateGoods }
})
