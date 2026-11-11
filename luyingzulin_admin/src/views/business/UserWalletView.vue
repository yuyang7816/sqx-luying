<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import AppDialog from '@/components/common/AppDialog.vue'
import BasePagination from '@/components/common/BasePagination.vue'
import ListFilterSummary from '@/components/common/ListFilterSummary.vue'
import { getAppUserInfo, getAppUserPage } from '@/api/modules/app-user'
import { updateUserWalletAmountApi, userWalletApi } from '@/api/modules/user-wallet'
import { useRouteUserFilter } from '@/composables/useRouteUserFilter'
import { resolveUserPhone } from '@/utils/appUser'
import { formatMoney } from '@/utils/rentOrder'
import LinkUser from '@/components/common/LinkUser.vue'
import { showError, showSuccess } from '@/utils/modal'

const router = useRouter()

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const filters = reactive({
  phone: '',
  userId: null,
})

const amountDialogVisible = ref(false)
const amountLoading = ref(false)
const amountForm = reactive({
  id: null,
  userId: null,
  userLabel: '',
  amount: 0,
  direction: 1,
  title: '系统调整',
  description: '',
})

async function enrichUserInfo(rows = []) {
  const enriched = await Promise.all(
    rows.map(async (row) => {
      try {
        const user = await getAppUserInfo(row.userId)
        return {
          ...row,
          userName: user?.userName || '-',
          phone: resolveUserPhone(user || {}),
        }
      } catch {
        return { ...row, userName: '-', phone: '-' }
      }
    }),
  )
  return enriched
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
    let userId = filters.userId || undefined
    if (!userId && filters.phone?.trim()) {
      userId = await resolveUserIdByPhone(filters.phone)
      if (!userId) {
        tableData.value = []
        total.value = 0
        return
      }
    }
    const data = await userWalletApi.page({
      page: pageNum.value,
      limit: pageSize.value,
      userId,
    })
    tableData.value = await enrichUserInfo(data.list || [])
    total.value = data.total
  } catch (error) {
    tableData.value = []
    total.value = 0
    showError(error?.msg || error?.message || '加载用户钱包失败')
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

function openAmount(row) {
  Object.assign(amountForm, {
    id: row.id,
    userId: row.userId,
    userLabel: `${row.userName || '-'}（${row.phone || '-'}）`,
    amount: 0,
    direction: 1,
    title: '系统调整',
    description: '',
  })
  amountDialogVisible.value = true
}

async function submitAmount() {
  amountLoading.value = true
  try {
    await updateUserWalletAmountApi({
      userId: amountForm.userId,
      operator: amountForm.direction === 2 ? 'decr' : 'incr',
      amount: amountForm.amount,
    })
    showSuccess('余额已调整')
    amountDialogVisible.value = false
    await loadData()
  } catch (error) {
    showError(error?.msg || error?.message || '调额失败')
  } finally {
    amountLoading.value = false
  }
}

function goTransactions(row) {
  router.push({
    path: '/business/wallet-transactions',
    query: { userId: row.userId },
  })
}

const { userFilterSummaryItems } = useRouteUserFilter(filters, loadData)
</script>

<template>
  <el-card shadow="never" class="page-card">
    <template #header>
      <strong>用户钱包</strong>
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

    <el-table v-loading="loading" :data="tableData" border empty-text="暂无钱包数据">
      <el-table-column label="手机号" width="130">
        <template #default="{ row }">
          <LinkUser :user-id="row.userId" :phone="row.phone" />
        </template>
      </el-table-column>
      <el-table-column label="用户名" min-width="120">
        <template #default="{ row }">
          <LinkUser :user-id="row.userId" :label="row.userName || '-'" />
        </template>
      </el-table-column>
      <el-table-column label="可用余额" width="120">
        <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
      </el-table-column>
      <el-table-column label="冻结金额" width="120">
        <template #default="{ row }">{{ formatMoney(row.frozenAmount) }}</template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" min-width="170" />
      <el-table-column prop="updateTime" label="更新时间" min-width="170" />
      <el-table-column label="操作" min-width="160" fixed="right">
        <template #default="{ row }">
          <div class="table-actions">
            <el-button size="small" type="warning" @click="openAmount(row)">调额</el-button>
            <el-button size="small" type="primary" @click="goTransactions(row)">流水</el-button>
          </div>
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

  <AppDialog v-model="amountDialogVisible" title="调整余额" :loading="amountLoading" @confirm="submitAmount">
    <el-form :model="amountForm" label-width="110px">
      <el-form-item label="用户">
        <el-input :model-value="amountForm.userLabel" disabled />
      </el-form-item>
      <el-form-item label="金额">
        <el-input-number v-model="amountForm.amount" :min="0" :precision="2" />
      </el-form-item>
      <el-form-item label="方向">
        <el-select v-model="amountForm.direction">
          <el-option label="收入" :value="1" />
          <el-option label="支出" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item label="标题">
        <el-input v-model="amountForm.title" />
      </el-form-item>
      <el-form-item label="描述">
        <el-input v-model="amountForm.description" type="textarea" :rows="3" />
      </el-form-item>
    </el-form>
  </AppDialog>
</template>

<style scoped>
.filter-form {
  margin-bottom: 12px;
}
</style>
