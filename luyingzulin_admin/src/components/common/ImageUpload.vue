<script setup>
import { computed } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { uploadFileApi } from '@/api/modules/system-config'
import { buildUploadSuccessPayload, resolveUploadUrl } from '@/utils/upload'
import { showError } from '@/utils/modal'

const props = defineProps({
  modelValue: { type: String, default: '' },
  /** 建议尺寸提示，仅展示 */
  tip: { type: String, default: '' },
  limit: { type: Number, default: 1 },
})

const emit = defineEmits(['update:modelValue'])

const fileList = computed(() => {
  if (!props.modelValue) return []
  return [{
    name: 'image',
    url: props.modelValue,
    status: 'success',
    uid: 1,
  }]
})

async function handleUpload(options) {
  try {
    const result = await uploadFileApi(options.file)
    const url = resolveUploadUrl(result)
    if (!url) {
      showError('上传成功但未返回图片地址')
      options.onError?.(new Error('missing upload url'))
      return
    }
    emit('update:modelValue', url)
    options.onSuccess?.(buildUploadSuccessPayload(result))
  } catch (error) {
    showError(error?.msg || error?.message || '图片上传失败')
    options.onError?.(error)
  }
}

function handleRemove() {
  emit('update:modelValue', '')
}

function beforeUpload(file) {
  const isImage = file.type.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 < 5
  if (!isImage) {
    showError('只能上传图片文件')
    return false
  }
  if (!isLt5M) {
    showError('图片大小不能超过 5MB')
    return false
  }
  return true
}
</script>

<template>
  <div class="image-upload">
    <el-upload
      list-type="picture-card"
      :file-list="fileList"
      :limit="limit"
      :http-request="handleUpload"
      :before-upload="beforeUpload"
      :on-remove="handleRemove"
      accept="image/*"
    >
      <el-icon><Plus /></el-icon>
    </el-upload>
    <div v-if="tip" class="image-upload__tip">{{ tip }}</div>
  </div>
</template>

<style scoped>
.image-upload__tip {
  margin-top: 8px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
</style>
