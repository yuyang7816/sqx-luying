<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import StatusSwitch from '@/components/common/StatusSwitch.vue'
import { getConfigSection } from '@/config/config-sections'
import { getSystemConfigListApi, refreshSystemConfigApi, updateSystemConfigApi } from '@/api/modules/system-config'
import { showError, showSuccess } from '@/utils/modal'
import { isAuth } from '@/utils/tool'

const section = computed(() => getConfigSection('feature-switches'))
const switchItems = computed(() => section.value?.settings || [])

const loading = ref(false)
const togglingKey = ref('')
const metaMap = ref({})
const values = reactive({})

async function loadData() {
  loading.value = true
  try {
    const data = await getSystemConfigListApi()
    const flat = (data || []).flatMap((group) => group.sysConfigEntityList || [])
    metaMap.value = Object.fromEntries(flat.map((item) => [item.paramKey, item]))
    for (const item of switchItems.value) {
      values[item.key] = String(metaMap.value[item.key]?.paramValue ?? '0')
    }
  } catch (error) {
    showError(error?.msg || error?.message || '加载配置失败')
  } finally {
    loading.value = false
  }
}

async function handleToggle(item, value) {
  const meta = metaMap.value[item.key]
  if (!meta?.id) {
    showError(`未找到配置项 ${item.key}，请确认已执行数据库 baseline 迁移`)
    return
  }
  togglingKey.value = item.key
  const previous = values[item.key]
  values[item.key] = String(value)
  try {
    await updateSystemConfigApi({
      id: meta.id,
      paramKey: item.key,
      paramValue: String(value),
      status: meta.status || '1',
      remark: meta.remark || item.note || '',
      paramType: meta.paramType,
      paramTypeDesc: meta.paramTypeDesc,
    })
    await refreshSystemConfigApi()
    showSuccess(value === '1' ? '已开启' : '已关闭')
  } catch (error) {
    values[item.key] = previous
    showError(error?.msg || error?.message || '操作失败')
  } finally {
    togglingKey.value = ''
  }
}

defineExpose({ reload: loadData })

onMounted(loadData)
</script>

<template>
  <div class="feature-switches">
    <p v-if="section?.description" class="feature-switches__desc">{{ section.description }}</p>
    <el-table v-loading="loading" :data="switchItems" border empty-text="暂无开关配置">
      <el-table-column prop="label" label="功能" min-width="160" />
      <el-table-column label="说明" min-width="360">
        <template #default="{ row }">
          {{ row.note || metaMap[row.key]?.remark || '—' }}
        </template>
      </el-table-column>
      <el-table-column prop="key" label="配置键" min-width="200">
        <template #default="{ row }">
          <span class="config-key">{{ row.key }}</span>
        </template>
      </el-table-column>
      <el-table-column label="开关" width="140" align="center">
        <template #default="{ row }">
          <StatusSwitch
            v-if="isAuth('sys:config:update')"
            :model-value="values[row.key] ?? '0'"
            active-value="1"
            inactive-value="0"
            active-text="开启"
            inactive-text="关闭"
            :loading="togglingKey === row.key"
            @change="(value) => handleToggle(row, value)"
          />
          <el-tag v-else :type="values[row.key] === '1' ? 'success' : 'info'">
            {{ values[row.key] === '1' ? '开启' : '关闭' }}
          </el-tag>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<style scoped>
.feature-switches__desc {
  margin: 0 0 12px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  line-height: 1.6;
}

.config-key {
  font-family: Consolas, Monaco, monospace;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
