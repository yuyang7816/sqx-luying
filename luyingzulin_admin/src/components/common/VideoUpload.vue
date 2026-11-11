<script setup>
import { computed } from 'vue'
import { Plus, VideoCamera } from '@element-plus/icons-vue'
import { uploadFileApi } from '@/api/modules/system-config'
import { buildUploadSuccessPayload, resolveUploadUrl } from '@/utils/upload'
import { showError } from '@/utils/modal'

const props = defineProps({
  modelValue: { type: String, default: '' },
  tip: { type: String, default: '' },
})

const emit = defineEmits(['update:modelValue'])

const hasVideo = computed(() => Boolean(props.modelValue))

async function handleUpload(options) {
  try {
    const result = await uploadFileApi(options.file)
    const url = resolveUploadUrl(result)
    if (!url) {
      showError('上传成功但未返回视频地址')
      options.onError?.(new Error('missing upload url'))
      return
    }
    emit('update:modelValue', url)
    options.onSuccess?.(buildUploadSuccessPayload(result))
  } catch (error) {
    showError(error?.msg || error?.message || '视频上传失败')
    options.onError?.(error)
  }
}

function handleRemove() {
  emit('update:modelValue', '')
}

function beforeUpload(file) {
  const isVideo = file.type.startsWith('video/')
  const isLt50M = file.size / 1024 / 1024 < 50
  if (!isVideo) {
    showError('只能上传视频文件')
    return false
  }
  if (!isLt50M) {
    showError('视频大小不能超过 50MB')
    return false
  }
  return true
}
</script>

<template>
  <div class="video-upload">
    <div v-if="hasVideo" class="video-preview">
      <video :src="modelValue" controls class="video-player" />
      <el-button type="danger" link @click="handleRemove">移除视频</el-button>
    </div>
    <el-upload
      v-else
      class="video-uploader"
      :show-file-list="false"
      :http-request="handleUpload"
      :before-upload="beforeUpload"
      accept="video/*"
    >
      <div class="upload-trigger">
        <el-icon><VideoCamera /></el-icon>
        <span>上传教程视频</span>
      </div>
    </el-upload>
    <div v-if="tip" class="video-upload__tip">{{ tip }}</div>
  </div>
</template>

<style scoped>
.video-upload__tip {
  margin-top: 8px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.video-preview {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: flex-start;
}

.video-player {
  width: 100%;
  max-width: 360px;
  max-height: 220px;
  border-radius: 8px;
  background: #000;
}

.upload-trigger {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 148px;
  height: 148px;
  border: 1px dashed var(--el-border-color);
  border-radius: 8px;
  color: var(--el-text-color-secondary);
  cursor: pointer;
}

.upload-trigger:hover {
  border-color: var(--el-color-primary);
  color: var(--el-color-primary);
}
</style>
