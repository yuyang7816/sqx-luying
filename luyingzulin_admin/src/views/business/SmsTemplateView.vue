<script setup>
import CrudPage from '@/components/common/CrudPage.vue'
import { smsTemplateApi } from '@/api/modules/sms-template'

const columns = [
  { prop: 'id', label: 'ID', width: 80 },
  { prop: 'bizCode', label: '业务标识', minWidth: 140 },
  { prop: 'content', label: '模板内容', minWidth: 260, showOverflowTooltip: true },
  { prop: 'createTime', label: '创建时间', minWidth: 180 },
]
</script>

<template>
  <CrudPage title="短信模板" :api="smsTemplateApi" :columns="columns">
    <template #form="{ form }">
      <el-form-item label="业务标识"><el-input v-model="form.bizCode" placeholder="如 login / register" /></el-form-item>
      <el-form-item label="模板内容"><el-input v-model="form.content" type="textarea" :rows="4" /></el-form-item>
    </template>
  </CrudPage>
</template>
