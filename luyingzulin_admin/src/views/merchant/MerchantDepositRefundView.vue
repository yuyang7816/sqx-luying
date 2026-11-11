<script setup>
import { computed, reactive, ref } from 'vue'
import AppDialog from '@/components/common/AppDialog.vue'
import BasePagination from '@/components/common/BasePagination.vue'
import ListFilterSummary from '@/components/common/ListFilterSummary.vue'
import { auditMerchantDepositRefund, getMerchantDepositRefundPage } from '@/api/merchant/merchant'
import { formatMoney } from '@/utils/rentOrder'
import { validateForm } from '@/utils/form'
import { buildFilterSummary } from '@/utils/listFilter'
import { showError, showSuccess } from '@/utils/modal'

const REFUND_STATUS_OPTIONS = [
  { value: 'pending', label: '待审核' },
  { value: 'completed', label: '已通过' },
  { value: 'rejected', label: '已驳回' },
]

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const filters = reactive({
  merchantId: null,
  status: 'pending',
})

const auditVisible = ref(false)
const auditLoading = ref(false)
const auditFormRef = ref()
const auditForm = reactive({
  logId: null,
  merchantName: '',
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

function getStatusLabel(status) {
  return REFUND_STATUS_OPTIONS.find((item) => item.value === status)?.label || status || '-'
}

function getStatusTagType(status) {
  if (status === 'pending') return 'warning'
  if (status === 'completed') return 'success'
  if (status === 'rejected') return 'danger'
  return 'info'
}

async function loadData() {
  loading.value = true
  try {
    const data = await getMerchantDepositRefundPage({
      page: pageNum.value,
      limit: pageSize.value,
      merchantId: filters.merchantId || undefined,
      status: filters.status || undefined,
    })
    tableData.value = data.list || []
    total.value = data.total
  } catch (error) {
    tableData.value = []
    total.value = 0
    showError(error?.msg || error?.message || '加载退保申请失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pageNum.value = 1
  loadData()
}

function handleReset() {
  filters.merchantId = null
  filters.status = 'pending'
  pageNum.value = 1
  loadData()
}

function openAudit(row, action = 'approve') {
  auditForm.logId = row.id
  auditForm.merchantName = row.merchantName
  auditForm.amount = row.amount
  auditForm.auditAction = action
  auditForm.remark = ''
  auditVisible.value = true
}

async function submitAudit() {
  const valid = await validateForm(auditFormRef)
  if (!valid) return
  auditLoading.value = true
  try {
    await auditMerchantDepositRefund({
      logId: auditForm.logId,
      action: auditForm.auditAction,
      remark: auditForm.remark?.trim() || undefined,
    })
    showSuccess(auditForm.auditAction === 'approve' ? '退保已通过' : '退保已驳回')
    auditVisible.value = false
    await loadData()
  } catch (error) {
    showError(error?.msg || error?.message || '审核失败')
  } finally {
    auditLoading.value = false
  }
}

const filterSummaryItems = computed(() => buildFilterSummary([
  { key: 'status', label: '状态', value: filters.status, options: REFUND_STATUS_OPTIONS },
]))

loadData()
</script>

<template>
  <el-card shadow="never" class="page-card">
    <template #header>
      <strong>商户保证金退还</strong>
    </template>

    <el-form :inline="true" class="filter-form" @submit.prevent="handleSearch">
      <el-form-item label="商户ID">
        <el-input v-model.number="filters.merchantId" clearable placeholder="商户ID" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="filters.status" clearable placeholder="全部">
          <el-option v-for="item in REFUND_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <ListFilterSummary :items="filterSummaryItems" />

    <el-table v-loading="loading" :data="tableData" border empty-text="暂无退保申请">
      <el-table-column prop="logNo" label="流水号" min-width="170" />
      <el-table-column prop="merchantName" label="商户" min-width="140" />
      <el-table-column prop="contactPhone" label="联系电话" width="130" />
      <el-table-column label="退还金额" width="120">
        <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="getStatusTagType(row.status)">{{ getStatusLabel(row.status) }}</el-tag>
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

    <BasePagination
      v-model:page="pageNum"
      v-model:limit="pageSize"
      :total="total"
      @change="loadData"
    />
  </el-card>

  <AppDialog
    v-model="auditVisible"
    :title="auditForm.auditAction === 'approve' ? '通过退保申请' : '驳回退保申请'"
    width="520px"
    :loading="auditLoading"
    @confirm="submitAudit"
  >
    <el-form ref="auditFormRef" :model="auditForm" :rules="auditRules" label-width="100px">
      <el-form-item label="商户">
        <el-input :model-value="auditForm.merchantName" disabled />
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
.muted-text {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
</style>
