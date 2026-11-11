import { ref } from 'vue'
import { getAppUserPage } from '@/api/modules/app-user'
import { showError } from '@/utils/modal'

/**
 * 顶栏全局手机号搜索：跳转用户列表并打开详情
 */
export function useGlobalPhoneSearch(router) {
  const searchPhone = ref('')

  async function submitPhoneSearch() {
    const phone = searchPhone.value.trim()
    if (!/^1\d{10}$/.test(phone)) {
      showError('请输入11位手机号')
      return
    }
    try {
      const data = await getAppUserPage({ page: 1, limit: 1, phone })
      const user = data.list?.[0]
      if (!user?.userId) {
        showError('未找到该手机号用户')
        return
      }
      searchPhone.value = ''
      await router.push({ path: '/users', query: { userId: user.userId } })
    } catch (error) {
      showError(error?.msg || error?.message || '搜索失败')
    }
  }

  return { searchPhone, submitPhoneSearch }
}
