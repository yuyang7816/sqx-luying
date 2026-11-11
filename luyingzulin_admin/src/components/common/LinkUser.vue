<script setup>
import { useRouter } from 'vue-router'
import { buildUserDetailQuery } from '@/utils/adminNavigation'

const props = defineProps({
  userId: { type: [Number, String], default: null },
  label: { type: String, default: '' },
  phone: { type: String, default: '' },
})

const router = useRouter()

function displayText() {
  if (props.label) return props.label
  if (props.phone) return props.phone
  if (props.userId) return `用户#${props.userId}`
  return '-'
}

function goDetail() {
  const target = buildUserDetailQuery(props.userId)
  if (target) {
    router.push(target)
  }
}
</script>

<template>
  <el-button v-if="userId" link type="primary" @click="goDetail">{{ displayText() }}</el-button>
  <span v-else>{{ displayText() }}</span>
</template>
