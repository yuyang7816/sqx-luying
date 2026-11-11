<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import AppDialog from '@/components/common/AppDialog.vue'
import ImageUpload from '@/components/common/ImageUpload.vue'
import {
  deleteRentCategory,
  getRentCategoryTree,
  saveRentCategory,
  updateRentCategory,
} from '@/api/rental/category'
import { validateForm } from '@/utils/form'
import { CATEGORY_FORM_RULES } from '@/views/rental/categoryFormRules'
import StatusSwitch from '@/components/common/StatusSwitch.vue'
import { confirmAction, showError, showSuccess } from '@/utils/modal'

const loading = ref(false)
const dialogVisible = ref(false)
const dialogLoading = ref(false)
const formRef = ref()
const treeData = ref([])
const editingId = ref(null)

const filters = reactive({
  name: '',
  status: null,
})

const initialForm = {
  id: null,
  parentId: 0,
  name: '',
  icon: '',
  sort: 0,
  status: 1,
}

const form = reactive({ ...initialForm })

const rules = CATEGORY_FORM_RULES

const treeSelectProps = {
  value: 'id',
  label: 'name',
  children: 'children',
  disabled: 'disabled',
}

function cloneTree(list = []) {
  return list.map((item) => ({
    ...item,
    children: cloneTree(item.children || []),
  }))
}

function collectDescendantIds(nodes = [], targetId, bucket = new Set()) {
  for (const node of nodes) {
    if (node.id === targetId) {
      walkChildren(node, bucket)
      break
    }
    collectDescendantIds(node.children || [], targetId, bucket)
  }
  return bucket
}

function walkChildren(node, bucket) {
  bucket.add(node.id)
  ;(node.children || []).forEach((child) => walkChildren(child, bucket))
}

function markDisabledNodes(list = [], currentId) {
  if (!currentId) return list
  const disabledIds = collectDescendantIds(list, currentId)
  const patcher = (nodes) => {
    nodes.forEach((node) => {
      node.disabled = disabledIds.has(node.id)
      patcher(node.children || [])
    })
    return nodes
  }
  return patcher(list)
}

const parentOptions = computed(() => [
  {
    id: 0,
    name: '顶级分类',
    disabled: false,
    children: markDisabledNodes(cloneTree(treeData.value), editingId.value),
  },
])

/** 按名称、状态客户端筛选树 */
function filterTree(nodes = []) {
  const keyword = filters.name.trim().toLowerCase()
  const status = filters.status

  const matcher = (node) => {
    const nameOk = !keyword || String(node.name || '').toLowerCase().includes(keyword)
    const statusOk = status === null || status === '' || Number(node.status ?? 1) === Number(status)
    return nameOk && statusOk
  }

  const walk = (list) => {
    const result = []
    list.forEach((node) => {
      const children = walk(node.children || [])
      if (matcher(node) || children.length) {
        result.push({ ...node, children })
      }
    })
    return result
  }

  return walk(nodes)
}

const displayTree = computed(() => filterTree(treeData.value))

async function loadData() {
  loading.value = true
  try {
    const data = await getRentCategoryTree()
    treeData.value = Array.isArray(data) ? data : []
  } catch (error) {
    treeData.value = []
    showError(error?.msg || error?.message || '加载分类树失败')
  } finally {
    loading.value = false
  }
}

function resetForm() {
  Object.assign(form, { ...initialForm })
}

function openCreate(parentId = 0) {
  editingId.value = null
  resetForm()
  form.parentId = parentId
  dialogVisible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  Object.assign(form, {
    id: row.id,
    parentId: row.parentId ?? 0,
    name: row.name,
    icon: row.icon || '',
    sort: row.sort ?? 0,
    status: row.status ?? 1,
  })
  dialogVisible.value = true
}

async function submitForm() {
  const valid = await validateForm(formRef)
  if (!valid) return

  dialogLoading.value = true
  try {
    const payload = {
      parentId: form.parentId,
      name: form.name,
      icon: form.icon,
      sort: form.sort,
      status: form.status,
    }
    if (editingId.value) {
      await updateRentCategory({ ...payload, id: editingId.value })
      showSuccess('分类已更新')
    } else {
      await saveRentCategory(payload)
      showSuccess('分类已创建')
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
  if (row.children?.length) {
    showError('请先删除或移动子分类')
    return
  }
  await confirmAction(`确认删除分类「${row.name}」吗？`)
  try {
    await deleteRentCategory({ id: row.id })
    showSuccess('删除成功')
    await loadData()
  } catch (error) {
    showError(error?.msg || error?.message || '删除失败，可能仍有关联商品')
  }
}

function resetFilters() {
  filters.name = ''
  filters.status = null
}

onMounted(loadData)
</script>

<template>
  <el-card shadow="never" class="page-card">
    <template #header>
      <div class="card-header">
        <strong>租赁分类</strong>
        <el-button type="primary" @click="openCreate(0)">新增顶级分类</el-button>
      </div>
    </template>

    <el-form inline class="filter-form" @submit.prevent>
      <el-form-item label="分类名称">
        <el-input v-model="filters.name" clearable placeholder="按名称筛选" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="filters.status" clearable placeholder="全部" style="width: 120px">
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="resetFilters">重置筛选</el-button>
        <el-button type="primary" plain @click="loadData">刷新</el-button>
      </el-form-item>
    </el-form>

    <el-table
      v-loading="loading"
      :data="displayTree"
      row-key="id"
      border
      default-expand-all
      :tree-props="{ children: 'children' }"
    >
      <el-table-column prop="name" label="分类名称" min-width="180" />
      <el-table-column prop="icon" label="图标" min-width="120">
        <template #default="{ row }">
          <el-image v-if="row.icon" :src="row.icon" style="width: 36px; height: 36px" fit="cover" />
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="sort" label="排序" width="80" />
      <el-table-column prop="status" label="状态" width="80" align="center">
        <template #default="{ row }">
          <StatusSwitch
            :model-value="Number(row.status ?? 1)"
            :active-value="1"
            :inactive-value="0"
            readonly
          />
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="260" fixed="right">
        <template #default="{ row }">
          <div class="table-actions">
            <el-button size="small" type="success" @click="openCreate(row.id)">子级</el-button>
            <el-button size="small" type="warning" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="removeRow(row)">删除</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <AppDialog
      v-model="dialogVisible"
      :title="editingId ? '编辑分类' : '新增分类'"
      :loading="dialogLoading"
      @confirm="submitForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="上级分类" prop="parentId">
          <el-tree-select
            v-model="form.parentId"
            :data="parentOptions"
            check-strictly
            :render-after-expand="false"
            :props="treeSelectProps"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" placeholder="如：帐篷睡袋" />
        </el-form-item>
        <el-form-item label="分类图标">
          <ImageUpload v-model="form.icon" tip="建议 128×128 正方形图标" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
    </AppDialog>
  </el-card>
</template>

<style scoped>
.filter-form {
  margin-bottom: 12px;
}
</style>
