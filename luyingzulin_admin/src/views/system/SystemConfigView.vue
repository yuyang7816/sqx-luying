<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import AppDialog from '@/components/common/AppDialog.vue'
import FeatureSwitchesView from '@/views/settings/FeatureSwitchesView.vue'
import { getSystemConfigTabKeys } from '@/config/config-sections'
import {
  deleteSystemConfigApi,
  getSystemConfigListApi,
  refreshSystemConfigApi,
  saveSystemConfigApi,
  updateSystemConfigApi,
} from '@/api/modules/system-config'
import { validateForm } from '@/utils/form'
import { confirmAction, showError, showSuccess } from '@/utils/modal'
import StatusSwitch from '@/components/common/StatusSwitch.vue'
import { isAuth } from '@/utils/tool'
import {
  getSwitchOffValue,
  getSwitchOnValue,
  isSwitchOn,
  sortConfigGroups,
  splitConfigGroups,
} from '@/utils/systemConfig'

const loading = ref(false)
const switchSavingKey = ref('')
const dialogVisible = ref(false)
const dialogLoading = ref(false)
const formRef = ref()
const rawGroups = ref([])
const viewMode = ref('feature')
const activeParamTab = ref('')
const tableData = ref([])
const editingId = ref(null)
const managedConfigKeys = getSystemConfigTabKeys()

function excludeManagedKeys(list = []) {
  return list.filter((item) => !managedConfigKeys.has(item.paramKey))
}

const form = reactive({
  id: null,
  paramKey: '',
  paramValue: '',
  status: '1',
  remark: '',
  paramType: '',
  paramTypeDesc: '',
})

const rules = {
  paramKey: [{ required: true, message: '请输入配置键', trigger: 'blur' }],
  paramValue: [{ required: true, message: '请输入配置值', trigger: 'blur' }],
}

const switchGroups = computed(() => {
  const groups = splitConfigGroups(rawGroups.value).switchGroups
  return groups
    .map((group) => ({ ...group, list: excludeManagedKeys(group.list) }))
    .filter((group) => group.list.length)
})
const paramGroups = computed(() => {
  const groups = splitConfigGroups(rawGroups.value).paramGroups
  return groups
    .map((group) => ({ ...group, list: excludeManagedKeys(group.list) }))
    .filter((group) => group.list.length)
})

const activeParamGroup = computed(() =>
  paramGroups.value.find((item) => item.paramType === activeParamTab.value) || null,
)

function syncTableData() {
  tableData.value = activeParamGroup.value?.list || []
}

async function loadData() {
  loading.value = true
  try {
    const data = await getSystemConfigListApi()
    rawGroups.value = sortConfigGroups((data || []).map((item) => ({
      paramType: item.paramType,
      paramTypeDesc: item.paramTypeDesc || item.paramType,
      list: item.sysConfigEntityList || [],
    })))

    if (!activeParamTab.value && paramGroups.value.length) {
      activeParamTab.value = paramGroups.value[0].paramType
    }
    if (viewMode.value === 'param' && !paramGroups.value.length) {
      viewMode.value = switchGroups.value.length ? 'switch' : 'feature'
    }
    syncTableData()
  } finally {
    loading.value = false
  }
}

function handleViewModeChange(mode) {
  viewMode.value = mode
  if (mode === 'param') {
    if (!activeParamTab.value && paramGroups.value.length) {
      activeParamTab.value = paramGroups.value[0].paramType
    }
    syncTableData()
  }
}

function handleParamTabChange() {
  syncTableData()
}

function openCreate() {
  const targetGroup = viewMode.value === 'switch'
    ? switchGroups.value[0]
    : activeParamGroup.value
  editingId.value = null
  Object.assign(form, {
    id: null,
    paramKey: '',
    paramValue: viewMode.value === 'switch' ? '1' : '',
    status: '1',
    remark: '',
    paramType: targetGroup?.paramType || '',
    paramTypeDesc: targetGroup?.paramTypeDesc || '',
  })
  dialogVisible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  Object.assign(form, {
    id: row.id,
    paramKey: row.paramKey,
    paramValue: row.paramValue,
    status: row.status || '1',
    remark: row.remark || '',
    paramType: row.paramType || activeParamTab.value,
    paramTypeDesc: row.paramTypeDesc,
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await validateForm(formRef)
  if (!valid) return
  dialogLoading.value = true
  try {
    if (editingId.value) await updateSystemConfigApi({ ...form })
    else await saveSystemConfigApi({ ...form })
    showSuccess(editingId.value ? '配置已更新' : '配置已新增')
    dialogVisible.value = false
    await loadData()
  } finally {
    dialogLoading.value = false
  }
}

async function handleDelete(row) {
  await confirmAction(`确认删除配置“${row.paramKey}”吗？`)
  await deleteSystemConfigApi([row.id])
  showSuccess('配置已删除')
  await loadData()
}

async function handleRefresh() {
  await refreshSystemConfigApi()
  showSuccess('配置缓存已刷新')
}

async function handleSwitchChange(row, checked) {
  if (!isAuth('sys:config:update')) {
    showError('暂无修改权限')
    return
  }
  const nextValue = checked ? getSwitchOnValue(row) : getSwitchOffValue(row)
  switchSavingKey.value = row.paramKey
  try {
    await updateSystemConfigApi({
      ...row,
      paramValue: nextValue,
    })
    row.paramValue = nextValue
    showSuccess('开关已更新')
  } catch (error) {
    showError(error?.msg || error?.message || '开关更新失败')
  } finally {
    switchSavingKey.value = ''
  }
}

function switchLabel(row) {
  return row.remark || row.paramKey
}

onMounted(loadData)
</script>

<template>
  <el-card shadow="never" class="page-card">
    <template #header>
      <div class="card-header">
        <strong>系统配置</strong>
        <el-space>
          <el-button
            v-if="viewMode !== 'feature' && isAuth('sys:config:save')"
            type="primary"
            @click="openCreate"
          >
            新增配置
          </el-button>
          <el-button v-if="isAuth('sys:config:update')" @click="handleRefresh">刷新缓存</el-button>
        </el-space>
      </div>
    </template>

    <el-tabs v-model="viewMode" class="view-mode-tabs" @tab-change="handleViewModeChange">
      <el-tab-pane label="功能开关" name="feature" />
      <el-tab-pane label="开关配置" name="switch" />
      <el-tab-pane label="参数配置" name="param" />
    </el-tabs>

    <div v-if="viewMode === 'feature'" class="feature-panel">
      <FeatureSwitchesView />
    </div>

    <div v-else-if="viewMode === 'switch'" v-loading="loading" class="switch-panel">
      <template v-if="switchGroups.length">
        <el-card
          v-for="group in switchGroups"
          :key="group.paramType"
          shadow="never"
          class="config-group-card"
        >
          <template #header>
            <div class="group-title">{{ group.paramTypeDesc || group.paramType }}</div>
          </template>
          <div class="switch-list">
            <div v-for="row in group.list" :key="row.id || row.paramKey" class="switch-row">
              <div class="switch-copy">
                <span class="switch-label">{{ switchLabel(row) }}</span>
                <span class="switch-key">{{ row.paramKey }}</span>
              </div>
              <el-switch
                :model-value="isSwitchOn(row)"
                :loading="switchSavingKey === row.paramKey"
                :disabled="!isAuth('sys:config:update')"
                @change="(checked) => handleSwitchChange(row, checked)"
              />
            </div>
          </div>
        </el-card>
      </template>
      <el-empty v-else description="暂无开关类配置" />
    </div>

    <div v-else v-loading="loading" class="param-panel">
      <el-tabs
        v-if="paramGroups.length"
        v-model="activeParamTab"
        class="param-type-tabs"
        @tab-change="handleParamTabChange"
      >
        <el-tab-pane
          v-for="item in paramGroups"
          :key="item.paramType"
          :label="item.paramTypeDesc"
          :name="item.paramType"
        >
          <el-table :data="tableData" border>
            <el-table-column prop="paramKey" label="配置键" min-width="200" />
            <el-table-column prop="paramValue" label="配置值" min-width="320" show-overflow-tooltip />
            <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="80" align="center">
              <template #default="{ row }">
                <StatusSwitch
                  :model-value="row.status"
                  active-value="1"
                  inactive-value="0"
                  readonly
                />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="{ row }">
                <div class="table-actions">
                  <el-button v-if="isAuth('sys:config:update')" size="small" type="warning" @click="openEdit(row)">
                    编辑
                  </el-button>
                  <el-button v-if="isAuth('sys:config:delete')" size="small" type="danger" @click="handleDelete(row)">
                    删除
                  </el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
      <el-empty v-else description="暂无参数类配置" />
    </div>

    <AppDialog
      v-model="dialogVisible"
      :title="editingId ? '编辑配置' : '新增配置'"
      :loading="dialogLoading"
      width="640px"
      @confirm="handleSubmit"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item label="配置键" prop="paramKey">
          <el-input v-model="form.paramKey" :readonly="Boolean(editingId)" />
        </el-form-item>
        <el-form-item label="配置值" prop="paramValue">
          <el-input v-if="!editingId && viewMode === 'switch'" v-model="form.paramValue" placeholder="开关类建议填 1 或 0" />
          <el-input v-else v-model="form.paramValue" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status">
            <el-option label="启用" value="1" />
            <el-option label="禁用" value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="开关类可在备注中标注「开关」便于归类" />
        </el-form-item>
      </el-form>
    </AppDialog>
  </el-card>
</template>

<style scoped>
.feature-panel {
  margin-top: 4px;
}

.view-mode-tabs {
  margin-bottom: 16px;
}

.param-type-tabs {
  margin-top: 4px;
}

.config-group-card {
  margin-bottom: 16px;
  border: 1px solid var(--el-border-color-lighter);
}

.group-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--el-text-color-primary);
}

.switch-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.switch-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  border-radius: 12px;
  background: #f8fafc;
}

.switch-copy {
  flex: 1;
  min-width: 0;
}

.switch-label,
.switch-key {
  display: block;
}

.switch-label {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.switch-key {
  margin-top: 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  word-break: break-all;
}
</style>
