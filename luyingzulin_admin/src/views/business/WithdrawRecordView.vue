<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useRouteUserFilter } from '@/composables/useRouteUserFilter'
import AppDialog from '@/components/common/AppDialog.vue'
import BasePagination from '@/components/common/BasePagination.vue'
import ListFilterSummary from '@/components/common/ListFilterSummary.vue'
import { confirmWithdrawRecordApi, withdrawRecordApi } from '@/api/modules/withdraw-record'
import { resolveUserDisplayName, resolveUserPhone } from '@/utils/appUser'
import { formatMoney } from '@/utils/rentOrder'
import { validateForm } from '@/utils/form'
import { buildFilterSummary, isFilterActive } from '@/utils/listFilter'
import {
  WITHDRAW_STATUS_OPTIONS,
  buildWithdrawConfirmPayload,
  getWithdrawAccountTypeLabel,
  getWithdrawSourceLabel,
  getWithdrawStatusMeta,
  validateWithdrawRejectReason,
} from '@/utils/userAssets'
import LinkUser from '@/components/common/LinkUser.vue'
import { showError, showSuccess } from '@/utils/modal'
import { useMenuBadgeStore } from '@/stores/menuBadge'

const router = useRouter()
const route = useRoute()
const menuBadgeStore = useMenuBadgeStore()

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const filters = reactive({
  phone: '',
  userId: null,
  status: null,
})

const auditVisible = ref(false)
const auditLoading = ref(false)
const detailVisible = ref(false)
const detailData = ref(null)
const auditFormRef = ref()
const auditForm = reactive({
  id: null,
  amount: 0,
  auditAction: 'approve',
  reason: '',
})
const auditRules = {
  reason: [{
    validator: (_rule, value, callback) => {
      const message = validateWithdrawRejectReason(auditForm.auditAction, value)
      if (message) {
        callback(new Error(message))
        return
      }
      callback()
    },
    trigger: 'blur',
  }],
}

async function loadData() {
  loading.value = true
  try {
    const data = await withdrawRecordApi.page({
      page: pageNum.value,
      limit: pageSize.value,
      userId: filters.userId || undefined,
    })
    let list = data.list || []
    if (isFilterActive(filters.status)) {
      list = list.filter((item) => item.status === filters.status)
    }
    tableData.value = list
    total.value = isFilterActive(filters.status) ? list.length : data.total
  } catch (error) {
    tableData.value = []
    total.value = 0
    showError(error?.msg || error?.message || '加载提现记录失败')
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
  filters.status = null
  pageNum.value = 1
  loadData()
}

function getAccountDisplay(row) {
  const account = row.withdrawalAccount
  if (!account) return getWithdrawAccountTypeLabel(row.accountType)
  return `${getWithdrawAccountTypeLabel(row.accountType)} ${account.account || account.realName || ''}`.trim()
}

function openDetail(row) {
  detailData.value = row
  detailVisible.value = true
}

function openAudit(row) {
  auditForm.id = row.id
  auditForm.amount = row.amount
  auditForm.auditAction = 'approve'
  auditForm.reason = ''
  auditVisible.value = true
}

async function submitAudit() {
  const valid = await validateForm(auditFormRef)
  if (!valid) return
  auditLoading.value = true
  try {
    await confirmWithdrawRecordApi(buildWithdrawConfirmPayload(auditForm))
    showSuccess(auditForm.auditAction === 'approve' ? '提现已通过' : '提现已驳回')
    auditVisible.value = false
    await loadData()
    void menuBadgeStore.refresh()
  } catch (error) {
    showError(error?.msg || error?.message || '审核失败')
  } finally {
    auditLoading.value = false
  }
}

function goWalletTransactions(row) {
  router.push({
    path: '/business/wallet-transactions',
    query: { userId: row.userId },
  })
}

const { userFilterSummaryItems } = useRouteUserFilter(filters, loadData)

const filterSummaryItems = computed(() => [
  ...userFilterSummaryItems.value,
  ...buildFilterSummary([
    { key: 'status', label: '状态', value: filters.status, options: WITHDRAW_STATUS_OPTIONS },
  ]),
])

watch(
  () => route.query.status,
  (status) => {
    if (status !== undefined) {
      filters.status = status === '' ? null : Number(status)
      pageNum.value = 1
      loadData()
    }
  },
  { immediate: true },
)
</script>

<template>
  <el-card shadow="never" class="page-card">
    <template #header>
      <strong>提现管理</strong>
    </template>

    <el-form :inline="true" class="filter-form" @submit.prevent="handleSearch">
      <el-form-item label="手机号">
        <el-input v-model="filters.phone" clearable placeholder="用户手机号" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="filters.status" clearable placeholder="全部">
          <el-option v-for="item in WITHDRAW_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <ListFilterSummary :items="filterSummaryItems" />

    <el-table v-loading="loading" :data="tableData" border empty-text="暂无提现记录">
      <el-table-column label="手机号" width="130">
        <template #default="{ row }">
          <LinkUser :user-id="row.userId" :phone="resolveUserPhone(row)" />
        </template>
      </el-table-column>
      <el-table-column label="用户" min-width="120">
        <template #default="{ row }">
          <LinkUser
            :user-id="row.userId"
            :label="resolveUserDisplayName(row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="来源" width="110">
        <template #default="{ row }">{{ getWithdrawSourceLabel(row.sourceType) }}</template>
      </el-table-column>
      <el-table-column label="提现金额" width="110">
        <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
      </el-table-column>
      <el-table-column label="到账金额" width="110">
        <template #default="{ row }">{{ formatMoney(row.actualAmount) }}</template>
      </el-table-column>
      <el-table-column label="手续费" width="100">
        <template #default="{ row }">{{ formatMoney(row.serviceFee) }}</template>
      </el-table-column>
      <el-table-column label="提现账户" min-width="160">
        <template #default="{ row }">{{ getAccountDisplay(row) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="110" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="getWithdrawStatusMeta(row.status).type">
            {{ getWithdrawStatusMeta(row.status).label }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="reason" label="驳回原因" min-width="140" show-overflow-tooltip />
      <el-table-column prop="withdrawTime" label="转账时间" min-width="170" />
      <el-table-column prop="createTime" label="申请时间" min-width="170" />
      <el-table-column label="操作" min-width="220" fixed="right">
        <template #default="{ row }">
          <div class="table-actions">
            <el-button size="small" type="primary" @click="openDetail(row)">详情</el-button>
            <el-button v-if="row.status === 1" size="small" type="success" @click="openAudit(row)">审核</el-button>
            <el-button
              v-if="row.sourceType === 2"
              size="small"
              type="info"
              @click="goWalletTransactions(row)"
            >
              流水
            </el-button>
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

  <el-dialog v-model="detailVisible" title="提现详情" width="720px" destroy-on-close>
    <el-descriptions v-if="detailData" :column="2" border>
      <el-descriptions-item label="记录ID">{{ detailData.id }}</el-descriptions-item>
      <el-descriptions-item label="用户">
        <LinkUser :user-id="detailData.userId" :label="resolveUserDisplayName(detailData)" />
      </el-descriptions-item>
      <el-descriptions-item label="手机号">
        <LinkUser :user-id="detailData.userId" :phone="resolveUserPhone(detailData)" />
      </el-descriptions-item>
      <el-descriptions-item label="来源">{{ getWithdrawSourceLabel(detailData.sourceType) }}</el-descriptions-item>
      <el-descriptions-item label="状态">
        <el-tag size="small" :type="getWithdrawStatusMeta(detailData.status).type">
          {{ getWithdrawStatusMeta(detailData.status).label }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="提现金额">{{ formatMoney(detailData.amount) }}</el-descriptions-item>
      <el-descriptions-item label="到账金额">{{ formatMoney(detailData.actualAmount) }}</el-descriptions-item>
      <el-descriptions-item label="手续费">{{ formatMoney(detailData.serviceFee) }}</el-descriptions-item>
      <el-descriptions-item label="账户类型">{{ getWithdrawAccountTypeLabel(detailData.accountType) }}</el-descriptions-item>
      <el-descriptions-item label="提现账户" :span="2">{{ getAccountDisplay(detailData) }}</el-descriptions-item>
      <el-descriptions-item label="申请时间">{{ detailData.createTime || '-' }}</el-descriptions-item>
      <el-descriptions-item label="转账时间">{{ detailData.withdrawTime || '-' }}</el-descriptions-item>
      <el-descriptions-item v-if="detailData.reason" label="驳回原因" :span="2">{{ detailData.reason }}</el-descriptions-item>
    </el-descriptions>
    <template #footer>
      <el-button @click="detailVisible = false">关闭</el-button>
      <el-button
        v-if="detailData?.sourceType === 2"
        type="primary"
        @click="goWalletTransactions(detailData); detailVisible = false"
      >
        查看钱包流水
      </el-button>
    </template>
  </el-dialog>

  <AppDialog v-model="auditVisible" title="提现审核" width="520px" :loading="auditLoading" @confirm="submitAudit">
    <el-form ref="auditFormRef" :model="auditForm" :rules="auditRules" label-width="100px">
      <el-form-item label="提现金额">
        <el-input :model-value="formatMoney(auditForm.amount)" disabled />
      </el-form-item>
      <el-form-item label="审核结果">
        <el-radio-group v-model="auditForm.auditAction">
          <el-radio value="approve">通过并转账</el-radio>
          <el-radio value="reject">驳回</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="auditForm.auditAction === 'reject'" label="驳回原因" prop="reason">
        <el-input v-model="auditForm.reason" type="textarea" :rows="4" placeholder="请填写驳回原因" />
      </el-form-item>
    </el-form>
  </AppDialog>
</template>

<style scoped>
.filter-form {
  margin-bottom: 12px;
}
</style>
