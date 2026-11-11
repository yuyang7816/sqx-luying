<script setup>
import { computed } from 'vue'

const props = defineProps({
  urls: { type: [String, Array], default: () => [] },
  size: { type: Number, default: 72 },
})

const list = computed(() => {
  if (Array.isArray(props.urls)) {
    return props.urls.map((item) => String(item || '').trim()).filter(Boolean)
  }
  if (!props.urls) return []
  return String(props.urls)
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
})

const imageStyle = computed(() => ({
  width: `${props.size}px`,
  height: `${props.size}px`,
  borderRadius: '4px',
}))
</script>

<template>
  <el-space v-if="list.length" wrap>
    <el-image
      v-for="(url, index) in list"
      :key="`${url}-${index}`"
      :src="url"
      :preview-src-list="list"
      fit="cover"
      :style="imageStyle"
    />
  </el-space>
  <span v-else>-</span>
</template>
