<script setup>
import { computed } from 'vue'
import { formatBadgeValue } from '@/constants/menuBadge'

const props = defineProps({
  value: { type: Number, default: 0 },
  max: { type: Number, default: 99 },
  hiddenZero: { type: Boolean, default: true },
})

const displayValue = computed(() => formatBadgeValue(props.value, props.max))
const visible = computed(() => !props.hiddenZero || Number(props.value) > 0)
</script>

<template>
  <span v-if="visible" class="notice-badge">{{ displayValue }}</span>
</template>

<style scoped>
.notice-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 6px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  line-height: 1;
  color: #fff;
  background: var(--el-color-danger);
  flex-shrink: 0;
}
</style>
