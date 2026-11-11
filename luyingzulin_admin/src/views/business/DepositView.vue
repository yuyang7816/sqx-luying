<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import AppDialog from '@/components/common/AppDialog.vue'
import BasePagination from '@/components/common/BasePagination.vue'
import ListFilterSummary from '@/components/common/ListFilterSummary.vue'
import NoticeBadge from '@/components/common/NoticeBadge.vue'
import {
  adjustUserDeposit,
  auditUserDepositRefund,
  getUserDepositLogPage,
  getUserDepositPage,
  getUserDepositRefundPage,
  refundUserDeposit,
} from '@/api/modules/user-deposit'
import { useRouteUserFilter } from '@/composables/useRouteUserFilter'
import { resolveUserPhone } from '@/utils/appUser'
import { formatMoney } from '@/utils/rentOrder'
import { validateForm } from '@/utils/form'
import { buildFilterSummary, filterParam } from '@/utils/listFilter'
import {
  DEPOSIT_ACCOUNT_STATUS_OPTIONS,
  DEPOSIT_LOG_TYPE_OPTIONS,
  getDepositLogTypeLabel,
} from '@/utils/userAssets'
import LinkUser from '@/components/common/LinkUser.vue'
import LinkOrder from '@/components/common/LinkOrder.vue'
import StatusSwitch from '@/components/common/StatusSwitch.vue'
import { DEPOSIT_LOG_SWITCH_ON_STATUSES } from '@/utils/statusSwitch'
import { showError, showSuccess } from '@/utils/modal'
import { useMenuBadgeStore } from '@/stores/menuBadge'

const route = useRoute()
const menuBadgeStore = useMenuBadgeStore()

const DEPOSIT_TABS = [
  { name: 'accounts', label: '押金账户' },
  { name: 'refunds', label: '退押审核' },
]

const REFUND_STATUS_OPTIONS = [
  { value: 'pending', label: '待审核' },
  { value: 'completed', label: '已通过' },
  { value: 'rejected', label: '已驳回' },
]

const activeTab = ref('accounts')
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const accountFilters = reactive({
  phone: '',
  userId: null,
  status: null,
})

const refundFilters = reactive({
  userId: null,
  status: 'pending',
})

const logVisible = ref(false)
const logLoading = ref(false)
const logData = ref([])
const logTotal = ref(0)
const logPageNum = ref(1)
const logPageSize = ref(10)
const logFilters = reactive({
  logType: '',
  orderId: '',
})
const currentAccount = ref(null)

const refundVisible = ref(false)
const refundLoading = ref(false)
const refundFormRef = ref()
const refundForm = reactive({
  userId: null,
  userName: '',
  phone: '',
  availableAmount: 0,
  amount: 0,
  remark: '',
})
const refundRules = {
  amount: [{ required: true, message: '请输入退押金额', trigger: 'blur' }],
}

const adjustVisible = ref(false)
const adjustLoading = ref(false)
const adjustFormRef = ref()
const adjustForm = reactive({
  userId: null,
  userName: '',
  phone: '',
  totalAmount: 0,
  availableAmount: 0,
  amount: 0,
  direction: 1,
  remark: '',
})
const adjustRules = {
  amount: [{ required: true, message: '请输入调整金额', trigger: 'blur' }],
}

const auditVisible = ref(false)
const auditLoading = ref(false)
const auditFormRef = ref()
const auditForm = reactive({
  logId: null,
  userName: '',
  amount: 0,
  auditAction: 'approve',
  remark: '',
})
const auditRules = {
  remark: [{
    validator: (_rule, value, callback) => {
      if (auditForm.auditAction === 'reject' && !String(value || '').trim()) {
        callback(new Error('驳回时请填写原因'))
        return
      }
      callback()
    },
    trigger: 'blur',
  }],
}

const pendingRefundCount = computed(() => menuBadgeStore.counts.pendingUserDepositRefundCount || 0)

function getRefundStatusLabel(status) {
  return REFUND_STATUS_OPTIONS.find((item) => item.value === status)?.label || status || '-'
}

function getRefundStatusTagType(status) {
  if (status === 'pending') return 'warning'
  if (status === 'completed') return 'success'
  if (status === 'rejected') return 'danger'
  return 'info'
}

async function loadAccountData() {
  loading.value = true
  try {
    const data = await getUserDepositPage({
      page: pageNum.value,
      limit: pageSize.value,
      userId: accountFilters.userId || undefined,
      phone: accountFilters.phone?.trim() || undefined,
      status: filterParam(accountFilters.status),
    })
    tableData.value = data.list || []
    total.value = data.total
  } catch (error) {
    tableData.value = []
    total.value = 0
    showError(error?.msg || error?.message || '加载押金账户失败')
  } finally {
    loading.value = false
  }
}

async function loadRefundData() {
  loading.value = true
  try {
    const data = await getUserDepositRefundPage({
      page: pageNum.value,
      limit: pageSize.value,
      userId: refundFilters.userId || undefined,
      status: refundFilters.status || undefined,
    })
    tableData.value = data.list || []
    total.value = data.total
  } catch (error) {
    tableData.value = []
    total.value = 0
    showError(error?.msg || error?.message || '加载退押申请失败')
  } finally {
    loading.value = false
  }
}

function loadCurrentTabData() {
  if (activeTab.value === 'refunds') {
    return loadRefundData()
  }
  return loadAccountData()
}

function handleSearch() {
  pageNum.value = 1
  loadCurrentTabData()
}

function handleAccountReset() {
  accountFilters.phone = ''
  accountFilters.userId = null
  accountFilters.status = null
  pageNum.value = 1
  loadAccountData()
}

function handleRefundReset() {
  refundFilters.userId = null
  refundFilters.status = 'pending'
  pageNum.value = 1
  loadRefundData()
}

function handleTabChange(tab) {
  pageNum.value = 1
  tableData.value = []
  total.value = 0
  if (tab === 'refunds') {
    loadRefundData()
    return
  }
  loadAccountData()
}

async function loadLogs() {
  if (!currentAccount.value?.userId) return
  logLoading.value = true
  try {
    const data = await getUserDepositLogPage({
      page: logPageNum.value,
      limit: logPageSize.value,
      userId: currentAccount.value.userId,
      logType: logFilters.logType || undefined,
      orderId: logFilters.orderId ? Number(logFilters.orderId) : undefined,
    })
    logData.value = data.list || []
    logTotal.value = data.total
  } catch (error) {
    logData.value = []
    logTotal.value = 0
    showError(error?.msg || error?.message || '加载押金流水失败')
  } finally {
    logLoading.value = false
  }
}

function openLogs(row) {
  currentAccount.value = row
  logFilters.logType = ''
  logFilters.orderId = ''
  logPageNum.value = 1
  logVisible.value = true
  loadLogs()
}

function searchLogs() {
  logPageNum.value = 1
  loadLogs()
}

function openManualRefund(row) {
  Object.assign(refundForm, {
    userId: row.userId,
    userName: row.userName || '-',
    phone: resolveUserPhone(row),
    availableAmount: Number(row.availableAmount || 0),
    amount: Number(row.availableAmount || 0),
    remark: '',
  })
  refundVisible.value = true
}

function openAdjust(row) {
  Object.assign(adjustForm, {
    userId: row.userId,
    userName: row.userName || '-',
    phone: resolveUserPhone(row),
    totalAmount: Number(row.totalAmount || 0),
    availableAmount: Number(row.availableAmount || 0),
    amount: 0,
    direction: 1,
    remark: '',
  })
  adjustVisible.value = true
}

function openAudit(row, action = 'approve') {
  auditForm.logId = row.id
  auditForm.userName = row.userName
  auditForm.amount = row.amount
  auditForm.auditAction = action
  auditForm.remark = ''
  auditVisible.value = true
}

async function submitManualRefund() {
  const valid = await validateForm(refundFormRef)
  if (!valid) return
  if (refundForm.amount > refundForm.availableAmount) {
    showError('退押金额不能超过可用押金')
    return
  }
  refundLoading.value = true
  try {
    await refundUserDeposit({
      userId: refundForm.userId,
      amount: refundForm.amount,
      remark: refundForm.remark?.trim() || undefined,
    })
    showSuccess('退押成功')
    refundVisible.value = false
    if (logVisible.value && currentAccount.value?.userId === refundForm.userId) {
      await loadLogs()
    }
    await loadAccountData()
  } catch (error) {
    showError(error?.msg || error?.message || '退押失败')
  } finally {
    refundLoading.value = false
  }
}

async function submitAdjust() {
  const valid = await validateForm(adjustFormRef)
  if (!valid) return
  if (!adjustForm.amount || adjustForm.amount <= 0) {
    showError('调整金额必须大于 0')
    return
  }
  if (adjustForm.direction === 2 && adjustForm.amount > adjustForm.availableAmount) {
    showError('扣减金额不能超过可用押金')
    return
  }
  adjustLoading.value = true
  try {
    await adjustUserDeposit({
      userId: adjustForm.userId,
      amount: adjustForm.amount,
      direction: adjustForm.direction,
      remark: adjustForm.remark?.trim() || undefined,
    })
    showSuccess('押金已调整')
    adjustVisible.value = false
    if (logVisible.value && currentAccount.value?.userId === adjustForm.userId) {
      await loadLogs()
    }
    await loadAccountData()
  } catch (error) {
    showError(error?.msg || error?.message || '调押失败')
  } finally {
    adjustLoading.value = false
  }
}

async function submitAudit() {
  const valid = await validateForm(auditFormRef)
  if (!valid) return
  auditLoading.value = true
  try {
    await auditUserDepositRefund({
      logId: auditForm.logId,
      action: auditForm.auditAction,
      remark: auditForm.remark?.trim() || undefined,
    })
    showSuccess(auditForm.auditAction === 'approve' ? '退押已通过' : '退押已驳回')
    auditVisible.value = false
    await loadRefundData()
    void menuBadgeStore.refresh()
  } catch (error) {
    showError(error?.msg || error?.message || '审核失败')
  } finally {
    auditLoading.value = false
  }
}

const { userFilterSummaryItems } = useRouteUserFilter(accountFilters, loadAccountData)

const accountFilterSummaryItems = computed(() => [
  ...userFilterSummaryItems.value,
  ...buildFilterSummary([
    { key: 'status', label: '状态', value: accountFilters.status, options: DEPOSIT_ACCOUNT_STATUS_OPTIONS },
  ]),
])

const refundFilterSummaryItems = computed(() => buildFilterSummary([
  { key: 'status', label: '状态', value: refundFilters.status, options: REFUND_STATUS_OPTIONS },
]))

watch(
  () => route.query.tab,
  (tab) => {
    const nextTab = tab === 'refunds' ? 'refunds' : 'accounts'
    if (activeTab.value === nextTab && tableData.value.length) return
    activeTab.value = nextTab
    pageNum.value = 1
    loadCurrentTabData()
  },
  { immediate: true },
)
</script>

<template>
  <el-card shadow="never" class="page-card">
    <template #header>
      <strong>押金管理</strong>
    </template>

    <el-tabs v-model="activeTab" class="deposit-tabs" @tab-change="handleTabChange">
      <el-tab-pane v-for="tab in DEPOSIT_TABS" :key="tab.name" :name="tab.name">
        <template #label>
          <span class="deposit-tab-label">
            {{ tab.label }}
            <NoticeBadge v-if="tab.name === 'refunds'" :value="pendingRefundCount" />
          </span>
        </template>
      </el-tab-pane>
    </el-tabs>

    <template v-if="activeTab === 'accounts'">
      <el-form :inline="true" class="filter-form" @submit.prevent="handleSearch">
        <el-form-item label="手机号">
          <el-input v-model="accountFilters.phone" clearable placeholder="用户手机号" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="accountFilters.status" clearable placeholder="全部">
            <el-option v-for="item in DEPOSIT_ACCOUNT_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleAccountReset">重置</el-button>
        </el-form-item>
      </el-form>

      <ListFilterSummary :items="accountFilterSummaryItems" />

      <el-table v-loading="loading" :data="tableData" border empty-text="暂无押金账户">
        <el-table-column label="手机号" width="130">
          <template #default="{ row }">
            <LinkUser :user-id="row.userId" :phone="resolveUserPhone(row)" />
          </template>
        </el-table-column>
        <el-table-column label="用户名" min-width="120">
          <template #default="{ row }">
            <LinkUser :user-id="row.userId" :label="row.userName || '-'" />
          </template>
        </el-table-column>
        <el-table-column label="押金总额" width="120">
          <template #default="{ row }">{{ formatMoney(row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column label="冻结金额" width="120">
          <template #default="{ row }">{{ formatMoney(row.frozenAmount) }}</template>
        </el-table-column>
        <el-table-column label="可用押金" width="120">
          <template #default="{ row }">{{ formatMoney(row.availableAmount) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <StatusSwitch
              :model-value="row.status"
              :active-value="1"
              :inactive-value="0"
              readonly
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="260" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button size="small" type="primary" @click="openLogs(row)">流水</el-button>
              <el-button size="small" type="success" @click="openAdjust(row)">调押</el-button>
              <el-button
                v-if="Number(row.availableAmount) > 0"
                size="small"
                type="warning"
                @click="openManualRefund(row)"
              >
                退押
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </template>

    <template v-else>
      <el-form :inline="true" class="filter-form" @submit.prevent="handleSearch">
        <el-form-item label="用户ID">
          <el-input v-model.number="refundFilters.userId" clearable placeholder="用户ID" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="refundFilters.status" clearable placeholder="全部">
            <el-option v-for="item in REFUND_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleRefundReset">重置</el-button>
        </el-form-item>
      </el-form>

      <ListFilterSummary :items="refundFilterSummaryItems" />

      <el-table v-loading="loading" :data="tableData" border empty-text="暂无退押申请">
        <el-table-column prop="logNo" label="流水号" min-width="170" />
        <el-table-column label="用户" min-width="140">
          <template #default="{ row }">
            <LinkUser :user-id="row.userId" :label="row.userName || '-'" />
          </template>
        </el-table-column>
        <el-table-column label="手机号" width="130">
          <template #default="{ row }">
            <LinkUser :user-id="row.userId" :phone="row.phone || row.phoneMasked" />
          </template>
        </el-table-column>
        <el-table-column label="退还金额" width="120">
          <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getRefundStatusTagType(row.status)">{{ getRefundStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="申请说明" min-width="160" show-overflow-tooltip />
        <el-table-column prop="createTime" label="申请时间" width="170" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'pending'">
              <el-button size="small" type="success" @click="openAudit(row, 'approve')">通过</el-button>
              <el-button size="small" type="danger" plain @click="openAudit(row, 'reject')">驳回</el-button>
            </template>
            <span v-else class="muted-text">已处理</span>
          </template>
        </el-table-column>
      </el-table>
    </template>

    <BasePagination
      :current-page="pageNum"
      :page-size="pageSize"
      :total="total"
      @update:current-page="pageNum = $event; loadCurrentTabData()"
      @update:page-size="pageSize = $event; pageNum = 1; loadCurrentTabData()"
    />
  </el-card>

  <el-dialog v-model="logVisible" title="押金流水" width="920px" destroy-on-close>
    <div v-if="currentAccount" class="log-header">
      <span>用户：<LinkUser :user-id="currentAccount.userId" :label="currentAccount.userName || '-'" /></span>
      <span>手机号：<LinkUser :user-id="currentAccount.userId" :phone="resolveUserPhone(currentAccount)" /></span>
      <span>可用押金：{{ formatMoney(currentAccount.availableAmount) }}</span>
    </div>
    <el-form :inline="true" class="filter-form" @submit.prevent="searchLogs">
      <el-form-item label="流水类型">
        <el-select v-model="logFilters.logType" clearable placeholder="全部">
          <el-option v-for="item in DEPOSIT_LOG_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="订单ID">
        <el-input v-model="logFilters.orderId" clearable placeholder="租赁订单ID" @keyup.enter="searchLogs" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="searchLogs">查询</el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="logLoading" :data="logData" border empty-text="暂无流水">
      <el-table-column prop="logNo" label="流水号" min-width="150" />
      <el-table-column label="类型" width="110">
        <template #default="{ row }">{{ row.logTypeLabel || getDepositLogTypeLabel(row.logType) }}</template>
      </el-table-column>
      <el-table-column label="金额" width="110">
        <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
      </el-table-column>
      <el-table-column label="关联订单ID" width="120">
        <template #default="{ row }">
          <LinkOrder :order-id="row.orderId" />
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80" align="center">
        <template #default="{ row }">
          <StatusSwitch
            :model-value="row.status"
            :active-values="DEPOSIT_LOG_SWITCH_ON_STATUSES"
            readonly
          />
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
      <el-table-column prop="createTime" label="时间" min-width="170" />
    </el-table>
    <BasePagination
      :current-page="logPageNum"
      :page-size="logPageSize"
      :total="logTotal"
      @update:current-page="logPageNum = $event; loadLogs()"
      @update:page-size="logPageSize = $event; logPageNum = 1; loadLogs()"
    />
  </el-dialog>

  <AppDialog v-model="adjustVisible" title="人工调押" width="520px" :loading="adjustLoading" @confirm="submitAdjust">
    <el-form ref="adjustFormRef" :model="adjustForm" :rules="adjustRules" label-width="100px">
      <el-form-item label="用户">
        <el-input :model-value="`${adjustForm.userName}（${adjustForm.phone}）`" disabled />
      </el-form-item>
      <el-form-item label="押金总额">
        <el-input :model-value="formatMoney(adjustForm.totalAmount)" disabled />
      </el-form-item>
      <el-form-item label="可用押金">
        <el-input :model-value="formatMoney(adjustForm.availableAmount)" disabled />
      </el-form-item>
      <el-form-item label="调整方向">
        <el-select v-model="adjustForm.direction">
          <el-option label="增加押金" :value="1" />
          <el-option label="减少押金" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item label="调整金额" prop="amount">
        <el-input-number v-model="adjustForm.amount" :min="0.01" :precision="2" />
      </el-form-item>
      <el-form-item label="运营备注">
        <el-input v-model="adjustForm.remark" type="textarea" :rows="3" placeholder="调整原因说明" />
      </el-form-item>
    </el-form>
  </AppDialog>

  <AppDialog v-model="refundVisible" title="人工退押" width="520px" :loading="refundLoading" @confirm="submitManualRefund">
    <el-form ref="refundFormRef" :model="refundForm" :rules="refundRules" label-width="100px">
      <el-form-item label="用户">
        <el-input :model-value="`${refundForm.userName}（${refundForm.phone}）`" disabled />
      </el-form-item>
      <el-form-item label="可用押金">
        <el-input :model-value="formatMoney(refundForm.availableAmount)" disabled />
      </el-form-item>
      <el-form-item label="退押金额" prop="amount">
        <el-input-number v-model="refundForm.amount" :min="0.01" :max="refundForm.availableAmount" :precision="2" />
      </el-form-item>
      <el-form-item label="运营备注">
        <el-input v-model="refundForm.remark" type="textarea" :rows="3" placeholder="退押原因说明" />
      </el-form-item>
    </el-form>
  </AppDialog>

  <AppDialog
    v-model="auditVisible"
    :title="auditForm.auditAction === 'approve' ? '通过退押申请' : '驳回退押申请'"
    width="520px"
    :loading="auditLoading"
    @confirm="submitAudit"
  >
    <el-form ref="auditFormRef" :model="auditForm" :rules="auditRules" label-width="100px">
      <el-form-item label="用户">
        <el-input :model-value="auditForm.userName" disabled />
      </el-form-item>
      <el-form-item label="退还金额">
        <el-input :model-value="formatMoney(auditForm.amount)" disabled />
      </el-form-item>
      <el-form-item label="审核备注" prop="remark">
        <el-input
          v-model="auditForm.remark"
          type="textarea"
          :rows="3"
          :placeholder="auditForm.auditAction === 'reject' ? '请填写驳回原因' : '可选，记录审核说明'"
        />
      </el-form-item>
    </el-form>
  </AppDialog>
</template>

<style scoped>
.filter-form {
  margin-bottom: 12px;
}

.deposit-tabs {
  margin-bottom: 12px;
}

.deposit-tab-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.log-header {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 12px;
  color: var(--el-text-color-regular);
}

.muted-text {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
</style>
