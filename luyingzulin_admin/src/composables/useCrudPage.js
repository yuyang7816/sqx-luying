import { onMounted, reactive, ref } from 'vue'
import { confirmAction, showError, showSuccess } from '@/utils/modal'

export function useCrudPage(api, { defaultQuery = {}, idKey = 'id' } = {}) {
  const loading = ref(false)
  const dialogVisible = ref(false)
  const dialogLoading = ref(false)
  const detailVisible = ref(false)
  const detailLoading = ref(false)
  const detailData = ref(null)
  const tableData = ref([])
  const total = ref(0)
  const pageNum = ref(1)
  const pageSize = ref(10)
  const editingId = ref(null)
  const form = reactive({})

  async function loadData(extra = {}) {
    loading.value = true
    try {
      const data = await api.page({
        page: pageNum.value,
        limit: pageSize.value,
        ...defaultQuery,
        ...extra,
      })
      tableData.value = data.list
      total.value = data.total
    } finally {
      loading.value = false
    }
  }

  function openCreate(initial = {}) {
    editingId.value = null
    Object.keys(form).forEach((key) => delete form[key])
    Object.assign(form, initial)
    dialogVisible.value = true
  }

  async function openDetail(row) {
    detailVisible.value = true
    detailLoading.value = true
    detailData.value = null
    try {
      detailData.value = await api.info(row[idKey])
    } catch (error) {
      showError(error?.msg || error?.message || '加载详情失败')
      detailVisible.value = false
    } finally {
      detailLoading.value = false
    }
  }

  async function openEdit(row) {
    editingId.value = row[idKey]
    dialogLoading.value = true
    dialogVisible.value = true
    try {
      const data = await api.info(row[idKey])
      Object.keys(form).forEach((key) => delete form[key])
      Object.assign(form, data || row)
    } catch (error) {
      showError(error?.msg || error?.message || '加载详情失败')
      dialogVisible.value = false
    } finally {
      dialogLoading.value = false
    }
  }

  async function submitForm(extra = {}) {
    dialogLoading.value = true
    try {
      const payload = { ...form, ...extra }
      if (editingId.value) {
        await api.update({ ...payload, [idKey]: editingId.value })
        showSuccess('更新成功')
      } else {
        await api.save(payload)
        showSuccess('创建成功')
      }
      dialogVisible.value = false
      await loadData()
    } catch (error) {
      showError(error?.msg || error?.message || '保存失败')
    } finally {
      dialogLoading.value = false
    }
  }

  async function removeRow(row) {
    await confirmAction('确认删除该记录吗？')
    await api.delete({ [idKey]: row[idKey] })
    showSuccess('删除成功')
    await loadData()
  }

  onMounted(() => loadData())

  return {
    loading,
    dialogVisible,
    dialogLoading,
    detailVisible,
    detailLoading,
    detailData,
    tableData,
    total,
    pageNum,
    pageSize,
    editingId,
    form,
    loadData,
    openCreate,
    openDetail,
    openEdit,
    submitForm,
    removeRow,
  }
}
