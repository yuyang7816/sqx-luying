<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import ImageUpload from '@/components/common/ImageUpload.vue'
import { getServiceConfigApi, saveServiceConfigApi } from '@/api/modules/app-user-config'
import { DEFAULT_SERVICE_CONFIG, SERVICE_CHANNEL_KEYS } from '@/constants/appUserConfig'
import { showError, showSuccess } from '@/utils/modal'

const loading = ref(false)
const saving = ref(false)

const channelOptions = [
  { key: 'onlineIm', label: '在线 IM' },
  { key: 'wechat', label: '微信客服' },
  { key: 'phone', label: '电话客服' },
  { key: 'wecom', label: '企业微信' },
]

function cloneDefaults(target = DEFAULT_SERVICE_CONFIG) {
  return JSON.parse(JSON.stringify(target))
}

const form = reactive(cloneDefaults())

const currentChannel = computed(() => form.channels[form.activeChannel] || {})

function handleChannelChange(key) {
  form.activeChannel = key
  SERVICE_CHANNEL_KEYS.forEach((itemKey) => {
    if (form.channels[itemKey]) {
      form.channels[itemKey].enabled = itemKey === key
    }
  })
}

async function loadData() {
  loading.value = true
  try {
    const { config } = await getServiceConfigApi()
    Object.assign(form, cloneDefaults(config))
  } catch (error) {
    showError(error?.msg || error?.message || '加载客服配置失败')
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  saving.value = true
  try {
    await saveServiceConfigApi({
      activeChannel: form.activeChannel,
      channels: form.channels,
    })
    showSuccess('客服配置已保存并刷新缓存')
    await loadData()
  } catch (error) {
    showError(error?.msg || error?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <el-card v-loading="loading" shadow="never" class="page-card">
    <template #header>
      <strong>客服中心配置</strong>
    </template>

    <el-alert
      type="info"
      :closable="false"
      show-icon
      class="config-tip"
      title="配置说明"
      description="对应用户端 /packageUser/service。四种客服方式互斥，仅可选择并展示一种。"
    />

    <div class="channel-picker">
      <div class="picker-label">客服方式</div>
      <el-radio-group :model-value="form.activeChannel" @change="handleChannelChange">
        <el-radio v-for="item in channelOptions" :key="item.key" :value="item.key">
          {{ item.label }}
        </el-radio>
      </el-radio-group>
    </div>

    <div class="channel-block">
      <el-form label-width="120px" class="inner-form">
        <el-form-item label="标题">
          <el-input v-model="currentChannel.title" maxlength="30" show-word-limit />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="currentChannel.desc" type="textarea" :rows="2" maxlength="120" show-word-limit />
        </el-form-item>

        <template v-if="form.activeChannel === 'onlineIm'">
          <el-form-item label="按钮文案">
            <el-input v-model="currentChannel.actionText" maxlength="20" />
          </el-form-item>
          <el-alert
            type="info"
            :closable="false"
            show-icon
            title="在线 IM 由平台官方客服接待，请在「官方客服会话」中回复用户消息；商家咨询仍走商户 IM。"
          />
        </template>

        <template v-if="form.activeChannel === 'wechat'">
          <el-form-item label="微信号">
            <el-input v-model="currentChannel.wechatId" maxlength="40" />
          </el-form-item>
          <el-form-item label="二维码">
            <ImageUpload v-model="currentChannel.qrCode" tip="建议 400×400" />
          </el-form-item>
        </template>

        <template v-if="form.activeChannel === 'phone'">
          <el-form-item label="电话号码">
            <el-input v-model="currentChannel.phoneNumber" maxlength="20" />
          </el-form-item>
          <el-form-item label="服务时间">
            <el-input v-model="currentChannel.serviceHours" maxlength="40" placeholder="如 08:30-20:30" />
          </el-form-item>
        </template>

        <template v-if="form.activeChannel === 'wecom'">
          <el-form-item label="企业名称">
            <el-input v-model="currentChannel.corpName" maxlength="40" />
          </el-form-item>
          <el-form-item label="跳转链接">
            <el-input v-model="currentChannel.corpLink" maxlength="200" />
          </el-form-item>
        </template>
      </el-form>
    </div>

    <div class="footer-actions">
      <el-button type="primary" :loading="saving" @click="handleSave">保存配置</el-button>
    </div>
  </el-card>
</template>

<style scoped>
.config-tip {
  margin-bottom: 20px;
}

.channel-picker {
  margin-bottom: 20px;
  padding: 16px 20px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-fill-color-blank);
}

.picker-label {
  margin-bottom: 12px;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.channel-block {
  margin-bottom: 20px;
  padding: 16px 20px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-fill-color-blank);
}

.inner-form {
  max-width: 760px;
}

.footer-actions {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid var(--el-border-color-lighter);
}
</style>
