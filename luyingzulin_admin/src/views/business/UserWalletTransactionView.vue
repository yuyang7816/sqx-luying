<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import BasePagination from '@/components/common/BasePagination.vue'
import ListFilterSummary from '@/components/common/ListFilterSummary.vue'
import { getAppUserPage } from '@/api/modules/app-user'
import { userWalletTransactionApi } from '@/api/modules/user-wallet-transaction'
import { resolveUserPhone } from '@/utils/appUser'
import { buildFilterSummary, buildUserFilterSummary } from '@/utils/listFilter'
import { formatMoney } from '@/utils/rentOrder'
import {
  getWalletBizTypeLabel,
  getWalletDirectionLabel,
  getWalletTransactionTypeLabel,
  getWalletTxStatusMeta,
} from '@/utils/userAssets'
import LinkUser from '@/components/common/LinkUser.vue'
import { showError } from '@/utils/modal'

const route = useRoute()

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const filters = reactive({
  phone: '',
  title: '',
})

const presetUserId = ref(null)

const filterSummaryItems = computed(() => [
  ...buildUserFilterSummary({ phone: filters.phone, userId: presetUserId.value }),
  ...buildFilterSummary([{ key: 'title', label: '标题', value: filters.title }]),
])

async function resolveUserIdByPhone(phone) {
  const text = String(phone || '').trim()
  if (!text) return undefined
  const data = await getAppUserPage({ page: 1, limit: 1, phone: text })
  return data.list?.[0]?.userId
}

async function loadData() {
  loading.value = true
  try {
    let userId = presetUserId.value || undefined
    if (!userId && filters.phone?.trim()) {
      userId = await resolveUserIdByPhone(filters.phone)
      if (!userId) {
        tableData.value = []
        total.value = 0
        return
      }
    }
    const data = await userWalletTransactionApi.page({
      page: pageNum.value,
      limit: pageSize.value,
      userId,
    })
    let list = data.list || []
    if (filters.title?.trim()) {
      const keyword = filters.title.trim()
      list = list.filter((item) => String(item.title || '').includes(keyword))
    }
    tableData.value = list
    total.value = filters.title?.trim() ? list.length : data.total
  } catch (error) {
    tableData.value = []
    total.value = 0
    showError(error?.msg || error?.message || '加载钱包流水失败')
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
  filters.title = ''
  presetUserId.value = null
  pageNum.value = 1
  loadData()
}

watch(
  () => route.query,
  (query) => {
    presetUserId.value = query.userId ? Number(query.userId) : null
    if (query.phone) {
      filters.phone = String(query.phone)
    }
    if (query.userId || query.phone) {
      pageNum.value = 1
      loadData()
    }
  },
  { immediate: true },
)

onMounted(() => {
  if (!route.query.userId && !route.query.phone) {
    loadData()
  }
})
</script>

<template>
  <el-card shadow="never" class="page-card">
    <template #header>
      <strong>钱包流水</strong>
    </template>

    <el-form :inline="true" class="filter-form" @submit.prevent="handleSearch">
      <el-form-item label="手机号">
        <el-input v-model="filters.phone" clearable placeholder="用户手机号" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="标题">
        <el-input v-model="filters.title" clearable placeholder="交易标题关键词" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <ListFilterSummary :items="filterSummaryItems" />

    <el-table v-loading="loading" :data="tableData" border empty-text="暂无流水">
      <el-table-column label="手机号" width="130">
        <template #default="{ row }">
          <LinkUser :user-id="row.userId" :phone="resolveUserPhone(row)" />
        </template>
      </el-table-column>
      <el-table-column label="用户名" min-width="110">
        <template #default="{ row }">
          <LinkUser :user-id="row.userId" :label="row.userName || '-'" />
        </template>
      </el-table-column>
      <el-table-column prop="title" label="标题" min-width="140" />
      <el-table-column label="金额" width="110">
        <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
      </el-table-column>
      <el-table-column label="交易类型" width="110">
        <template #default="{ row }">{{ getWalletTransactionTypeLabel(row.transactionType) }}</template>
      </el-table-column>
      <el-table-column label="方向" width="80">
        <template #default="{ row }">{{ getWalletDirectionLabel(row.direction) }}</template>
      </el-table-column>
      <el-table-column label="业务类型" width="120">
        <template #default="{ row }">{{ getWalletBizTypeLabel(row.bizType) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="getWalletTxStatusMeta(row.status).type">
            {{ getWalletTxStatusMeta(row.status).label }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="160" show-overflow-tooltip />
      <el-table-column prop="createTime" label="创建时间" min-width="170" />
    </el-table>

    <BasePagination
      :current-page="pageNum"
      :page-size="pageSize"
      :total="total"
      @update:current-page="pageNum = $event; loadData()"
      @update:page-size="pageSize = $event; pageNum = 1; loadData()"
    />
  </el-card>
</template>

<style scoped>
.filter-form {
  margin-bottom: 12px;
}
</style>
