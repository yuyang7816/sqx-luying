<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppDialog from '@/components/common/AppDialog.vue'
import BasePagination from '@/components/common/BasePagination.vue'
import { getMerchantDetail, getMerchantPage, updateMerchantStatus, adjustMerchantSecurityDeposit } from '@/api/merchant/merchant'
import { validateForm } from '@/utils/form'
import {
  MERCHANT_STATUS_OPTIONS,
  getMerchantStatusMeta,
} from '@/utils/merchant'
import LinkUser from '@/components/common/LinkUser.vue'
import StatusSwitch from '@/components/common/StatusSwitch.vue'
import { confirmAction, showError, showSuccess } from '@/utils/modal'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const detailLoading = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const filters = reactive({
  keyword: '',
  status: '',
})

const detailVisible = ref(false)
const detailData = ref(null)

const statusVisible = ref(false)
const statusLoading = ref(false)
const statusFormRef = ref()
const statusForm = reactive({
  id: null,
  name: '',
  status: 'active',
})
const statusRules = {
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
}

const depositVisible = ref(false)
const depositLoading = ref(false)
const depositFormRef = ref()
const depositForm = reactive({
  merchantId: null,
  merchantName: '',
  direction: 'add',
  amount: '',
  remark: '',
})
const depositRules = {
  direction: [{ required: true, message: '请选择调整方向', trigger: 'change' }],
  amount: [
    { required: true, message: '请输入调整金额', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        const num = Number(value)
        if (!Number.isFinite(num) || num <= 0) {
          callback(new Error('请输入大于0的金额'))
          return
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
}

function formatDepositAmount(value) {
  const num = Number(value || 0)
  return Number.isFinite(num) ? num.toFixed(2) : '0.00'
}

function formatDepositCell(row) {
  const held = formatDepositAmount(row.securityDepositAmount)
  const required = Number(row.securityDepositRequiredAmount || 0)
  if (required <= 0) {
    return `¥${held} / 无需缴纳`
  }
  return `¥${held} / ¥${required.toFixed(2)}`
}

function openDepositAdjust(row, direction) {
  depositForm.merchantId = row.id
  depositForm.merchantName = row.name
  depositForm.direction = direction
  depositForm.amount = ''
  depositForm.remark = ''
  depositVisible.value = true
}

async function submitDepositAdjust() {
  const valid = await validateForm(depositFormRef)
  if (!valid) return
  depositLoading.value = true
  try {
    await adjustMerchantSecurityDeposit({
      merchantId: depositForm.merchantId,
      direction: depositForm.direction,
      amount: Number(depositForm.amount),
      remark: depositForm.remark?.trim() || undefined,
    })
    showSuccess(depositForm.direction === 'add' ? '保证金已增加' : '保证金已扣减')
    depositVisible.value = false
    await loadData()
  } catch (error) {
    showError(error?.msg || error?.message || '调整保证金失败')
  } finally {
    depositLoading.value = false
  }
}

async function loadData() {
  loading.value = true
  try {
    const data = await getMerchantPage({
      page: pageNum.value,
      limit: pageSize.value,
      keyword: filters.keyword?.trim() || undefined,
      status: filters.status || undefined,
    })
    tableData.value = data.list || []
    total.value = data.total
  } catch (error) {
    tableData.value = []
    total.value = 0
    showError(error?.msg || error?.message || '加载商户列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pageNum.value = 1
  loadData()
}

function handleReset() {
  filters.keyword = ''
  filters.status = ''
  pageNum.value = 1
  loadData()
}

async function openDetailByMerchantId(merchantId) {
  const id = Number(merchantId)
  if (!id) return
  detailVisible.value = true
  detailLoading.value = true
  detailData.value = null
  try {
    detailData.value = await getMerchantDetail(id)
  } catch (error) {
    showError(error?.msg || error?.message || '加载商户详情失败')
    detailVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

async function openDetail(row) {
  detailVisible.value = true
  detailLoading.value = true
  detailData.value = null
  try {
    detailData.value = await getMerchantDetail(row.id)
  } catch (error) {
    showError(error?.msg || error?.message || '加载商户详情失败')
    detailVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

function openStatus(row) {
  statusForm.id = row.id
  statusForm.name = row.name
  statusForm.status = row.status === 'frozen' ? 'active' : 'frozen'
  statusVisible.value = true
}

async function submitStatus() {
  const valid = await validateForm(statusFormRef)
  if (!valid) return
  const actionLabel = statusForm.status === 'active' ? '启用' : '冻结'
  try {
    await confirmAction(`确认${actionLabel}商户「${statusForm.name}」？`)
  } catch {
    return
  }
  statusLoading.value = true
  try {
    await updateMerchantStatus({ id: statusForm.id, status: statusForm.status })
    showSuccess(`商户已${actionLabel}`)
    statusVisible.value = false
    await loadData()
  } catch (error) {
    showError(error?.msg || error?.message || `${actionLabel}失败`)
  } finally {
    statusLoading.value = false
  }
}

async function handleMerchantStatusChange(row, nextStatus) {
  const actionLabel = nextStatus === 'active' ? '启用' : '冻结'
  try {
    await confirmAction(`确认${actionLabel}商户「${row.name}」？`)
  } catch {
    return
  }
  try {
    await updateMerchantStatus({ id: row.id, status: nextStatus })
    row.status = nextStatus
    showSuccess(`商户已${actionLabel}`)
  } catch (error) {
    showError(error?.msg || error?.message || `${actionLabel}失败`)
  }
}

function goMerchantGoods(merchantId) {
  detailVisible.value = false
  router.push({ path: '/rental/goods', query: { merchantId } })
}

function goMerchantOrders(merchantId) {
  detailVisible.value = false
  router.push({ path: '/rental/orders', query: { merchantId } })
}

onMounted(loadData)

watch(
  () => route.query.merchantId,
  (merchantId) => {
    if (merchantId) {
      openDetailByMerchantId(merchantId)
    }
  },
  { immediate: true },
)
</script>

<template>
  <el-card shadow="never" class="page-card">
    <template #header>
      <strong>商户列表</strong>
    </template>

    <el-form :inline="true" class="filter-form" @submit.prevent="handleSearch">
      <el-form-item label="关键词">
        <el-input v-model="filters.keyword" clearable placeholder="ID / 名称 / 联系人 / 手机号" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="filters.status" clearable placeholder="全部">
          <el-option v-for="item in MERCHANT_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="tableData" border empty-text="暂无商户">
      <el-table-column prop="id" label="ID" width="90" />
      <el-table-column prop="name" label="商户名称" min-width="140" />
      <el-table-column prop="contactName" label="联系人" width="100" />
      <el-table-column prop="contactPhone" label="联系电话" width="130" />
      <el-table-column label="商户保证金" min-width="170">
        <template #default="{ row }">
          <div>{{ formatDepositCell(row) }}</div>
          <el-tag
            v-if="Number(row.securityDepositRequiredAmount || 0) > 0"
            size="small"
            :type="row.securityDepositPaid ? 'success' : 'warning'"
            style="margin-top: 4px"
          >
            {{ row.securityDepositStatusLabel || (row.securityDepositPaid ? '已缴足' : '未缴足') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80" align="center">
        <template #default="{ row }">
          <StatusSwitch
            :model-value="row.status"
            active-value="active"
            inactive-value="frozen"
            @change="(value) => handleMerchantStatusChange(row, value)"
          />
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" min-width="170" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <div class="table-actions">
            <el-button size="small" type="primary" @click="openDetail(row)">详情</el-button>
            <el-button size="small" type="success" @click="openDepositAdjust(row, 'add')">加保证金</el-button>
            <el-button size="small" type="warning" @click="openDepositAdjust(row, 'reduce')">减保证金</el-button>
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

  <AppDialog v-model="detailVisible" title="商户详情" width="680px" :show-confirm="false">
    <div v-loading="detailLoading">
      <el-descriptions v-if="detailData" :column="2" border>
        <el-descriptions-item label="ID">{{ detailData.id }}</el-descriptions-item>
        <el-descriptions-item label="商户名称">{{ detailData.name }}</el-descriptions-item>
        <el-descriptions-item label="联系人">{{ detailData.contactName }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ detailData.contactPhone }}</el-descriptions-item>
        <el-descriptions-item label="负责人用户ID">
          <LinkUser :user-id="detailData.userId" :label="String(detailData.userId || '-')" />
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <StatusSwitch
            :model-value="detailData.status"
            active-value="active"
            inactive-value="frozen"
            readonly
          />
        </el-descriptions-item>
        <el-descriptions-item label="创建时间" :span="2">{{ detailData.createTime || '-' }}</el-descriptions-item>
      </el-descriptions>

      <div v-if="detailData?.stores?.length" class="store-block">
        <div class="store-title">关联门店</div>
        <el-table :data="detailData.stores" border size="small">
          <el-table-column prop="id" label="ID" width="90" />
          <el-table-column prop="name" label="门店名称" min-width="120" />
          <el-table-column prop="address" label="地址" min-width="180" show-overflow-tooltip />
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
        </el-table>
      </div>
    </div>
    <template v-if="detailData" #footer>
      <el-space>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="primary" @click="goMerchantGoods(detailData.id)">查看商品</el-button>
        <el-button type="warning" @click="goMerchantOrders(detailData.id)">查看订单</el-button>
      </el-space>
    </template>
  </AppDialog>

  <AppDialog v-model="statusVisible" title="变更商户状态" width="480px" :loading="statusLoading" @confirm="submitStatus">
    <el-form ref="statusFormRef" :model="statusForm" :rules="statusRules" label-width="100px">
      <el-form-item label="商户名称">
        <el-input :model-value="statusForm.name" disabled />
      </el-form-item>
      <el-form-item label="目标状态" prop="status">
        <el-radio-group v-model="statusForm.status">
          <el-radio v-for="item in MERCHANT_STATUS_OPTIONS" :key="item.value" :value="item.value">
            {{ item.label }}
          </el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>
  </AppDialog>

  <AppDialog
    v-model="depositVisible"
    :title="depositForm.direction === 'add' ? '增加商户保证金' : '扣减商户保证金'"
    width="480px"
    :loading="depositLoading"
    @confirm="submitDepositAdjust"
  >
    <el-form ref="depositFormRef" :model="depositForm" :rules="depositRules" label-width="100px">
      <el-form-item label="商户名称">
        <el-input :model-value="depositForm.merchantName" disabled />
      </el-form-item>
      <el-form-item label="调整方向" prop="direction">
        <el-radio-group v-model="depositForm.direction">
          <el-radio value="add">增加</el-radio>
          <el-radio value="reduce">扣减</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="调整金额" prop="amount">
        <el-input v-model="depositForm.amount" type="number" min="0.01" step="0.01" placeholder="请输入金额" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="depositForm.remark" type="textarea" :rows="3" placeholder="可选，记录调整原因" />
      </el-form-item>
    </el-form>
  </AppDialog>
</template>

<style scoped>
.filter-form {
  margin-bottom: 12px;
}

.store-block {
  margin-top: 16px;
}

.store-title {
  margin-bottom: 8px;
  font-weight: 600;
}
</style>
