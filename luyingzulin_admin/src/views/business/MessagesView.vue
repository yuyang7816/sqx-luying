<script setup>
import CrudPage from '@/components/common/CrudPage.vue'
import LinkUser from '@/components/common/LinkUser.vue'
import LinkOrder from '@/components/common/LinkOrder.vue'
import ImagePreviewList from '@/components/common/ImagePreviewList.vue'
import StatusSwitch from '@/components/common/StatusSwitch.vue'
import { messagesApi } from '@/api/modules/messages'
import { resolveUserDisplayName, resolveUserPhone } from '@/utils/appUser'

const columns = [
  { prop: 'id', label: 'ID', width: 80 },
  { prop: 'title', label: '标题', minWidth: 160 },
  { prop: 'type', label: '类型', width: 100 },
  { prop: 'phone', label: '手机号', width: 130 },
  { prop: 'userId', label: '用户', width: 120 },
  { prop: 'isRead', label: '状态', width: 80 },
  { prop: 'createTime', label: '创建时间', minWidth: 180 },
]

const typeMap = {
  1: '系统公告',
  2: '用户消息',
  4: '官方客服',
}

function formatType(value) {
  return typeMap[value] || value || '-'
}
</script>

<template>
  <CrudPage
    title="消息管理"
    :api="messagesApi"
    :columns="columns"
    show-detail
    :create-defaults="{ type: 1, isRead: 0 }"
  >
    <template #col-type="{ row }">{{ formatType(row.type) }}</template>
    <template #col-phone="{ row }">
      <LinkUser v-if="row.userId" :user-id="row.userId" :phone="resolveUserPhone(row)" />
      <span v-else>-</span>
    </template>
    <template #col-userId="{ row }">
      <LinkUser v-if="row.userId" :user-id="row.userId" :label="resolveUserDisplayName(row)" />
      <span v-else>全部用户</span>
    </template>
    <template #col-isRead="{ row }">
      <StatusSwitch
        :model-value="row.isRead"
        :active-value="1"
        :inactive-value="0"
        readonly
      />
    </template>
    <template #form="{ form }">
      <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
      <el-form-item label="内容"><el-input v-model="form.content" type="textarea" :rows="4" /></el-form-item>
      <el-form-item label="图片URLs"><el-input v-model="form.imageUrls" placeholder="逗号分隔" /></el-form-item>
      <el-form-item label="跳转链接"><el-input v-model="form.url" /></el-form-item>
      <el-form-item label="类型">
        <el-select v-model="form.type">
          <el-option label="系统公告" :value="1" />
          <el-option label="用户消息" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item label="用户ID"><el-input-number v-model="form.userId" :min="0" /></el-form-item>
      <el-form-item label="关联数据ID"><el-input-number v-model="form.dataId" :min="0" /></el-form-item>
    </template>
    <template #detail="{ data }">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="ID">{{ data.id }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ formatType(data.type) }}</el-descriptions-item>
        <el-descriptions-item label="标题" :span="2">{{ data.title || '-' }}</el-descriptions-item>
        <el-descriptions-item label="手机号">
          <LinkUser v-if="data.userId" :user-id="data.userId" :phone="resolveUserPhone(data)" />
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="用户">
          <LinkUser v-if="data.userId" :user-id="data.userId" :label="resolveUserDisplayName(data)" />
          <span v-else>全部用户</span>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <StatusSwitch
            :model-value="data.isRead"
            :active-value="1"
            :inactive-value="0"
            readonly
          />
        </el-descriptions-item>
        <el-descriptions-item label="关联数据ID">{{ data.dataId ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="跳转链接">{{ data.url || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间" :span="2">{{ data.createTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="内容" :span="2">{{ data.content || '-' }}</el-descriptions-item>
        <el-descriptions-item label="消息图片" :span="2">
          <ImagePreviewList :urls="data.imageUrls" :size="88" />
        </el-descriptions-item>
      </el-descriptions>
    </template>
  </CrudPage>
</template>
