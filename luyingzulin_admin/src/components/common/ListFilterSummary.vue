<script setup>
import { computed } from 'vue'

const props = defineProps({
  items: { type: Array, default: () => [] },
  emptyText: { type: String, default: '未设置条件，展示全部数据' },
})

const activeItems = computed(() => (props.items || []).filter((item) => item?.text))
</script>

<template>
  <div class="list-filter-summary">
    <span class="list-filter-summary__label">当前筛选：</span>
    <template v-if="activeItems.length">
      <el-tag
        v-for="item in activeItems"
        :key="item.key"
        size="small"
        type="info"
        effect="plain"
        class="list-filter-summary__tag"
      >
        {{ item.label }}：{{ item.text }}
      </el-tag>
    </template>
    <span v-else class="list-filter-summary__empty">{{ emptyText }}</span>
  </div>
</template>

<style scoped>
.list-filter-summary {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
  padding: 10px 14px;
  border-radius: 12px;
  background: #f8fafc;
  border: 1px solid var(--el-border-color-lighter);
}

.list-filter-summary__label {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.list-filter-summary__empty {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.list-filter-summary__tag {
  margin: 0;
}
</style>
