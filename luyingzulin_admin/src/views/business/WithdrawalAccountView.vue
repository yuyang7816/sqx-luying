<script setup>
import { reactive, ref } from 'vue'
import BasePagination from '@/components/common/BasePagination.vue'
import ListFilterSummary from '@/components/common/ListFilterSummary.vue'
import { withdrawalAccountApi } from '@/api/modules/withdrawal-account'
import { useRouteUserFilter } from '@/composables/useRouteUserFilter'
import { resolveUserDisplayName, resolveUserPhone } from '@/utils/appUser'
import LinkUser from '@/components/common/LinkUser.vue'
import { showError } from '@/utils/modal'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const detailVisible = ref(false)
const detailLoading = ref(false)
const detailData = ref(null)

const filters = reactive({
  phone: '',
  userId: null,
})

const typeMap = {
  1: '支付宝',
  2: '微信',
}

function formatType(value) {
  return typeMap[value] || value || '-'
}

async function loadData() {
  loading.value = true
  try {
    const data = await withdrawalAccountApi.page({
      page: pageNum.value,
      limit: pageSize.value,
      userId: filters.userId || undefined,
    })
    tableData.value = data.list || []
    total.value = data.total
  } catch (error) {
    tableData.value = []
    total.value = 0
    showError(error?.msg || error?.message || '加载提现账户失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pageNum.value = 1
  loadData()
}

function handleReset() {
  filters.phone = ''
  filters.userId = null
  pageNum.value = 1
  loadData()
}

async function openDetail(row) {
  detailVisible.value = true
  detailLoading.value = true
  detailData.value = null
  try {
    detailData.value = await withdrawalAccountApi.info(row.id)
  } catch (error) {
    showError(error?.msg || error?.message || '加载详情失败')
    detailVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

const { userFilterSummaryItems } = useRouteUserFilter(filters, loadData)
</script>

<template>
  <el-card shadow="never" class="page-card">
    <template #header>
      <strong>提现账户</strong>
    </template>

    <el-form :inline="true" class="filter-form" @submit.prevent="handleSearch">
      <el-form-item label="手机号">
        <el-input v-model="filters.phone" clearable placeholder="用户手机号" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <ListFilterSummary :items="userFilterSummaryItems" />

    <el-table v-loading="loading" :data="tableData" border empty-text="暂无提现账户">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column label="手机号" width="130">
        <template #default="{ row }">
          <LinkUser :user-id="row.userId" :phone="resolveUserPhone(row)" />
        </template>
      </el-table-column>
      <el-table-column label="用户" min-width="120">
        <template #default="{ row }">
          <LinkUser :user-id="row.userId" :label="resolveUserDisplayName(row)" />
        </template>
      </el-table-column>
      <el-table-column prop="accountName" label="账户名称" min-width="140" />
      <el-table-column prop="accountNumber" label="账号" min-width="160" />
      <el-table-column label="类型" width="100">
        <template #default="{ row }">{{ formatType(row.type) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" @click="openDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <BasePagination
      :current-page="pageNum"
      :page-size="pageSize"
      :total="total"
      @update:current-page="pageNum = $event; loadData()"
      @update:page-size="pageSize = $event; pageNum = 1; loadData()"
    />

    <el-dialog v-model="detailVisible" title="提现账户详情" width="720px" destroy-on-close>
      <div v-loading="detailLoading">
        <el-descriptions v-if="detailData" :column="2" border>
          <el-descriptions-item label="ID">{{ detailData.id }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ formatType(detailData.type) }}</el-descriptions-item>
          <el-descriptions-item label="手机号">
            <LinkUser :user-id="detailData.userId" :phone="resolveUserPhone(detailData)" />
          </el-descriptions-item>
          <el-descriptions-item label="用户">
            <LinkUser :user-id="detailData.userId" :label="resolveUserDisplayName(detailData)" />
          </el-descriptions-item>
          <el-descriptions-item label="账户名称">{{ detailData.accountName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="账号">{{ detailData.accountNumber || '-' }}</el-descriptions-item>
          <el-descriptions-item label="真实姓名">{{ detailData.realName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ detailData.createTime || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>
