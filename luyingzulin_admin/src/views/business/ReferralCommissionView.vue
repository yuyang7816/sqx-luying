<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import BasePagination from '@/components/common/BasePagination.vue'
import ListFilterSummary from '@/components/common/ListFilterSummary.vue'
import { referralCommissionApi } from '@/api/modules/referral-commission'
import { getAppUserPage } from '@/api/modules/app-user'
import LinkUser from '@/components/common/LinkUser.vue'
import LinkOrder from '@/components/common/LinkOrder.vue'
import { formatMoney } from '@/utils/rentOrder'
import { buildUserFilterSummary } from '@/utils/listFilter'
import { showError } from '@/utils/modal'

const route = useRoute()

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const filters = reactive({
  phone: '',
})

const presetUserId = ref(null)

const filterSummaryItems = computed(() => buildUserFilterSummary({
  phone: filters.phone,
  userId: presetUserId.value,
}))

const detailVisible = ref(false)
const detailLoading = ref(false)
const detailData = ref(null)

const typeMap = {
  0: '提现',
  1: '订单佣金',
}

const directionMap = {
  1: '收入',
  2: '支出',
}

function formatType(value) {
  return typeMap[value] ?? value ?? '-'
}

function formatDirection(value) {
  return directionMap[value] ?? value ?? '-'
}

function formatRate(value) {
  if (value === null || value === undefined || value === '') return '-'
  const num = Number(value)
  if (Number.isNaN(num)) return value
  return `${(num * 100).toFixed(2)}%`
}

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
    const data = await referralCommissionApi.page({
      page: pageNum.value,
      limit: pageSize.value,
      userId,
      phone: filters.phone?.trim() || undefined,
    })
    tableData.value = data.list || []
    total.value = data.total
  } catch (error) {
    tableData.value = []
    total.value = 0
    showError(error?.msg || error?.message || '加载分销佣金流水失败')
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
  presetUserId.value = null
  pageNum.value = 1
  loadData()
}

async function openDetail(row) {
  detailVisible.value = true
  detailLoading.value = true
  detailData.value = null
  try {
    detailData.value = await referralCommissionApi.info(row.id)
  } catch (error) {
    showError(error?.msg || error?.message || '加载详情失败')
    detailVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

function applyRouteQuery(query = route.query) {
  presetUserId.value = query.userId ? Number(query.userId) : null
  if (query.phone) {
    filters.phone = String(query.phone)
  }
}

watch(
  () => route.query,
  (query) => {
    applyRouteQuery(query)
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
      <strong>分销佣金</strong>
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

    <ListFilterSummary :items="filterSummaryItems" />

    <el-table v-loading="loading" :data="tableData" border empty-text="暂无佣金流水">
      <el-table-column label="用户" min-width="140">
        <template #default="{ row }">
          <LinkUser
            :user-id="row.userId"
            :phone="row.userPhone"
            :label="row.userName || row.userPhone"
          />
        </template>
      </el-table-column>
      <el-table-column label="订单" min-width="150">
        <template #default="{ row }">
          <LinkOrder v-if="row.orderId" :order-id="row.orderId" />
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="referralLevel" label="层级" width="80" />
      <el-table-column label="比例" width="90">
        <template #default="{ row }">{{ formatRate(row.rate) }}</template>
      </el-table-column>
      <el-table-column label="佣金" width="100">
        <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
      </el-table-column>
      <el-table-column label="类型" width="100">
        <template #default="{ row }">{{ formatType(row.type) }}</template>
      </el-table-column>
      <el-table-column prop="createTime" label="时间" min-width="170" />
      <el-table-column label="操作" width="90" fixed="right">
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
  </el-card>

  <el-dialog v-model="detailVisible" title="佣金流水详情" width="720px" destroy-on-close>
    <div v-loading="detailLoading">
      <el-descriptions v-if="detailData" :column="2" border>
        <el-descriptions-item label="ID">{{ detailData.id }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ formatType(detailData.type) }}</el-descriptions-item>
        <el-descriptions-item label="用户">
          <LinkUser
            :user-id="detailData.userId"
            :phone="detailData.userPhone"
            :label="detailData.userName || detailData.userPhone"
          />
        </el-descriptions-item>
        <el-descriptions-item label="订单">
          <LinkOrder v-if="detailData.orderId" :order-id="detailData.orderId" />
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="层级">{{ detailData.referralLevel || '-' }}</el-descriptions-item>
        <el-descriptions-item label="比例">{{ formatRate(detailData.rate) }}</el-descriptions-item>
        <el-descriptions-item label="佣金">{{ formatMoney(detailData.amount) }}</el-descriptions-item>
        <el-descriptions-item label="方向">{{ formatDirection(detailData.direction) }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detailData.createTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="标题" :span="2">{{ detailData.title || '-' }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ detailData.description || '-' }}</el-descriptions-item>
      </el-descriptions>
    </div>
    <template #footer>
      <el-button @click="detailVisible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.filter-form {
  margin-bottom: 12px;
}
</style>
