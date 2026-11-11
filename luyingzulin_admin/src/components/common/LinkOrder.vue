<script setup>
import { useRouter } from 'vue-router'
import { buildOrderDetailRoute } from '@/utils/adminNavigation'

const props = defineProps({
  orderId: { type: [Number, String], default: null },
  label: { type: String, default: '' },
})

const router = useRouter()

function displayText() {
  if (props.label) return props.label
  if (props.orderId) return `订单#${props.orderId}`
  return '-'
}

function goDetail() {
  const target = buildOrderDetailRoute(props.orderId)
  if (target) {
    router.push(target)
  }
}
</script>

<template>
  <el-button v-if="orderId" link type="primary" @click="goDetail">{{ displayText() }}</el-button>
  <span v-else>{{ displayText() }}</span>
</template>
