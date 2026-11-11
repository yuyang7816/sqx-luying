<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import AppDialog from '@/components/common/AppDialog.vue'
import BasePagination from '@/components/common/BasePagination.vue'
import ImagePreviewList from '@/components/common/ImagePreviewList.vue'
import { auditMerchantApply, getMerchantApplyPage } from '@/api/merchant/apply'
import { validateForm } from '@/utils/form'
import {
  APPLY_STATUS_OPTIONS,
  buildApplyAuditPayload,
  getApplyStatusMeta,
  validateApplyRejectReason,
} from '@/utils/merchant'
import StatusSwitch from '@/components/common/StatusSwitch.vue'
import { APPLY_SWITCH_ON_STATUSES } from '@/utils/statusSwitch'
import { showError, showSuccess } from '@/utils/modal'
import { useMenuBadgeStore } from '@/stores/menuBadge'

const route = useRoute()
const menuBadgeStore = useMenuBadgeStore()
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const filters = reactive({
  keyword: '',
  status: '',
})

const detailVisible = ref(false)
const detailRow = ref(null)

const auditVisible = ref(false)
const auditLoading = ref(false)
const auditFormRef = ref()
const auditForm = reactive({
  applyId: null,
  storeName: '',
  auditResult: 'approved',
  rejectReason: '',
})
const auditRules = {
  rejectReason: [{
    validator: (_rule, value, callback) => {
      const message = validateApplyRejectReason(auditForm.auditResult, value)
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
    const data = await getMerchantApplyPage({
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
    showError(error?.msg || error?.message || '加载入驻申请失败')
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

function openDetail(row) {
  detailRow.value = row
  detailVisible.value = true
}

function openAudit(row) {
  auditForm.applyId = row.id
  auditForm.storeName = row.storeName || row.merchantName
  auditForm.auditResult = 'approved'
  auditForm.rejectReason = ''
  auditVisible.value = true
}

function formatMoney(value) {
  if (value == null || value === '') return '-'
  const num = Number(value)
  return Number.isNaN(num) ? String(value) : `¥${num}`
}

function formatRegion(row = {}) {
  const parts = [row.province, row.city, row.district].filter(Boolean)
  return parts.length ? parts.join(' / ') : '-'
}

function formatLocation(row = {}) {
  if (row.latitude == null && row.longitude == null) return '-'
  return `${row.latitude ?? '-'}, ${row.longitude ?? '-'}`
}

const LICENSE_LABELS = ['营业执照', '身份证正面', '身份证反面']

async function submitAudit() {
  const valid = await validateForm(auditFormRef)
  if (!valid) return
  auditLoading.value = true
  try {
    await auditMerchantApply(buildApplyAuditPayload(auditForm))
    showSuccess(auditForm.auditResult === 'approved' ? '审核已通过' : '申请已驳回')
    auditVisible.value = false
    await loadData()
    void menuBadgeStore.refresh()
  } catch (error) {
    showError(error?.msg || error?.message || '审核失败')
  } finally {
    auditLoading.value = false
  }
}

onMounted(() => {
  if (route.query.status) {
    filters.status = String(route.query.status)
  }
  loadData()
})

watch(
  () => route.query.status,
  (status) => {
    if (status !== undefined) {
      filters.status = String(status || '')
      pageNum.value = 1
      loadData()
    }
  },
)
</script>

<template>
  <el-card shadow="never" class="page-card">
    <template #header>
      <strong>入驻审核</strong>
    </template>

    <el-form :inline="true" class="filter-form" @submit.prevent="handleSearch">
      <el-form-item label="关键词">
        <el-input v-model="filters.keyword" clearable placeholder="ID / 门店名称 / 联系人 / 手机号" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="filters.status" clearable placeholder="全部">
          <el-option v-for="item in APPLY_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="tableData" border empty-text="暂无入驻申请">
      <el-table-column prop="id" label="ID" width="90" />
      <el-table-column prop="storeName" label="门店名称" min-width="140" />
      <el-table-column prop="contactName" label="联系人" width="100" />
      <el-table-column prop="contactPhone" label="联系电话" width="130" />
      <el-table-column prop="storeAddress" label="门店地址" min-width="180" show-overflow-tooltip />
      <el-table-column label="状态" width="80" align="center">
        <template #default="{ row }">
          <StatusSwitch
            :model-value="row.status"
            :active-values="APPLY_SWITCH_ON_STATUSES"
            readonly
          />
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="申请时间" min-width="170" />
      <el-table-column prop="auditTime" label="审核时间" min-width="170" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <div class="table-actions">
            <el-button size="small" type="primary" @click="openDetail(row)">详情</el-button>
            <el-button v-if="row.status === 'reviewing'" size="small" type="success" @click="openAudit(row)">
              审核
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

  <AppDialog v-model="detailVisible" title="入驻申请详情" width="860px" :show-confirm="false">
    <el-descriptions v-if="detailRow" :column="2" border>
      <el-descriptions-item label="ID">{{ detailRow.id }}</el-descriptions-item>
      <el-descriptions-item label="申请编号">{{ detailRow.applyNo || '-' }}</el-descriptions-item>
      <el-descriptions-item label="门店名称">{{ detailRow.storeName || detailRow.merchantName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="联系人">{{ detailRow.contactName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="联系电话">{{ detailRow.contactPhone || '-' }}</el-descriptions-item>
      <el-descriptions-item label="营业时间">{{ detailRow.businessHours || '-' }}</el-descriptions-item>
      <el-descriptions-item label="门店地址" :span="2">{{ detailRow.storeAddress || '-' }}</el-descriptions-item>
      <el-descriptions-item label="所在地区" :span="2">{{ formatRegion(detailRow) }}</el-descriptions-item>
      <el-descriptions-item label="经纬度" :span="2">{{ formatLocation(detailRow) }}</el-descriptions-item>
      <el-descriptions-item label="配送范围" :span="2">{{ detailRow.serviceArea || '-' }}</el-descriptions-item>
      <el-descriptions-item label="基础配送费">{{ formatMoney(detailRow.baseDeliveryFee) }}</el-descriptions-item>
      <el-descriptions-item label="满额免配送">
        {{ detailRow.freeDeliveryThreshold != null ? `${formatMoney(detailRow.freeDeliveryThreshold)}（仅租金，不含押金）` : '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="经营范围" :span="2">
        {{ (detailRow.businessScopes || []).join('、') || '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="状态">
        <StatusSwitch
          :model-value="detailRow.status"
          :active-values="APPLY_SWITCH_ON_STATUSES"
          readonly
        />
      </el-descriptions-item>
      <el-descriptions-item label="申请时间">{{ detailRow.createTime || '-' }}</el-descriptions-item>
      <el-descriptions-item v-if="detailRow.rejectReason" label="驳回原因" :span="2">
        {{ detailRow.rejectReason }}
      </el-descriptions-item>
      <el-descriptions-item label="审核时间">{{ detailRow.auditTime || '-' }}</el-descriptions-item>
      <el-descriptions-item
        v-for="(url, index) in (detailRow.licenseUrls || [])"
        :key="`license-${index}`"
        :label="LICENSE_LABELS[index] || `资质材料${index + 1}`"
      >
        <ImagePreviewList :urls="url ? [url] : []" :size="96" />
      </el-descriptions-item>
      <el-descriptions-item v-if="detailRow.extraUrls?.length" label="门头封面" :span="2">
        <ImagePreviewList :urls="detailRow.extraUrls[0] ? [detailRow.extraUrls[0]] : []" :size="120" />
      </el-descriptions-item>
      <el-descriptions-item v-if="detailRow.extraUrls?.length > 1" label="门店轮播图" :span="2">
        <ImagePreviewList :urls="detailRow.extraUrls.slice(1)" :size="96" />
      </el-descriptions-item>
    </el-descriptions>
  </AppDialog>

  <AppDialog v-model="auditVisible" title="入驻审核" width="520px" :loading="auditLoading" @confirm="submitAudit">
    <el-form ref="auditFormRef" :model="auditForm" :rules="auditRules" label-width="100px">
      <el-form-item label="门店名称">
        <el-input :model-value="auditForm.storeName" disabled />
      </el-form-item>
      <el-form-item label="审核结果">
        <el-radio-group v-model="auditForm.auditResult">
          <el-radio value="approved">通过</el-radio>
          <el-radio value="rejected">驳回</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="auditForm.auditResult === 'rejected'" label="驳回原因" prop="rejectReason">
        <el-input v-model="auditForm.rejectReason" type="textarea" :rows="4" placeholder="请填写驳回原因" />
      </el-form-item>
    </el-form>
  </AppDialog>
</template>

<style scoped>
.filter-form {
  margin-bottom: 12px;
}
</style>
