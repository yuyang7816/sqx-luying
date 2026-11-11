<script setup>
import { onMounted, reactive, ref } from 'vue'
import { getDeliveryFeeConfig, saveDeliveryFeeConfig } from '@/api/rental/pricing'
import { validateForm } from '@/utils/form'
import { showError, showSuccess } from '@/utils/modal'

const loading = ref(false)
const saving = ref(false)
const formRef = ref()

const form = reactive({
  id: null,
  paramValue: 15,
  status: '1',
  remark: '',
})

const rules = {
  paramValue: [
    { required: true, message: '请输入配送基础费用', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        const num = Number(value)
        if (Number.isNaN(num) || num < 0) {
          callback(new Error('请输入不小于 0 的金额'))
          return
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
}

async function loadConfig() {
  loading.value = true
  try {
    const data = await getDeliveryFeeConfig()
    form.id = data.id
    form.paramValue = Number(data.paramValue ?? 15)
    form.status = data.status || '1'
    form.remark = data.remark || ''
  } catch (error) {
    showError(error?.msg || error?.message || '加载配送费配置失败')
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  const valid = await validateForm(formRef)
  if (!valid) return
  saving.value = true
  try {
    const data = await saveDeliveryFeeConfig({
      id: form.id,
      paramValue: Number(form.paramValue).toFixed(2),
      status: form.status,
      remark: form.remark,
    })
    form.id = data.id
    showSuccess('配送费规则已保存并刷新缓存')
  } catch (error) {
    showError(error?.msg || error?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(loadConfig)
</script>

<template>
  <el-card v-loading="loading" shadow="never" class="page-card">
    <template #header>
      <div class="card-header">
        <strong>租赁定价配置</strong>
        <el-tag type="info">系统配置 · rental</el-tag>
      </div>
    </template>

    <el-alert
      type="info"
      :closable="false"
      show-icon
      title="配送费规则说明"
      description="用户选择「同城配送」取货方式时，订单将按此处配置的基础配送费计入应付总额。配置键为 rental.delivery.base_fee，保存后会自动刷新服务端配置缓存。"
      class="tip-alert"
    />

    <el-form ref="formRef" :model="form" :rules="rules" label-width="140px" class="config-form">
      <el-form-item label="配置键">
        <el-input model-value="rental.delivery.base_fee" readonly />
      </el-form-item>
      <el-form-item label="同城配送基础费" prop="paramValue">
        <el-input-number
          v-model="form.paramValue"
          :min="0"
          :precision="2"
          :step="1"
          controls-position="right"
          style="width: 200px"
        />
        <span class="unit">元</span>
      </el-form-item>
      <el-form-item label="状态">
        <el-radio-group v-model="form.status">
          <el-radio :value="'1'">启用</el-radio>
          <el-radio :value="'0'">禁用</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="配置说明" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="saving" @click="handleSave">保存并刷新缓存</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<style scoped>
.tip-alert {
  margin-bottom: 20px;
}

.config-form {
  max-width: 640px;
}

.unit {
  margin-left: 8px;
  color: var(--el-text-color-secondary);
}
</style>
