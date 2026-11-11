<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BasePagination from '@/components/common/BasePagination.vue'
import {
  auditRentInspection,
  getRentInspectionDetail,
  getRentInspectionPage,
} from '@/api/rental/inspection'
import { formatMoney } from '@/utils/rentOrder'
import {
  DEVICE_CONDITION_MAP,
  INSPECTION_AUDIT_STATUS_MAP,
  canAuditInspection,
  getDeviceConditionMeta,
  getInspectionAuditMeta,
} from '@/utils/rentInspection'
import LinkUser from '@/components/common/LinkUser.vue'
import LinkOrder from '@/components/common/LinkOrder.vue'
import StatusSwitch from '@/components/common/StatusSwitch.vue'
import { INSPECTION_AUDIT_ON_STATUSES } from '@/utils/statusSwitch'
import { confirmAction, showError, showSuccess } from '@/utils/modal'
import { useMenuBadgeStore } from '@/stores/menuBadge'

const route = useRoute()
const router = useRouter()
const menuBadgeStore = useMenuBadgeStore()

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const detailVisible = ref(false)
const detailLoading = ref(false)
const detailData = ref(null)

const auditVisible = ref(false)
const auditLoading = ref(false)
const auditForm = reactive({
  id: null,
  auditStatus: 'approved',
  auditRemark: '',
})

const filters = reactive({
  keyword: '',
  auditStatus: '',
  deviceCondition: '',
})

async function loadData() {
  loading.value = true
  try {
    const data = await getRentInspectionPage({
      page: pageNum.value,
      limit: pageSize.value,
      keyword: filters.keyword?.trim() || undefined,
      auditStatus: filters.auditStatus || undefined,
      deviceCondition: filters.deviceCondition || undefined,
    })
    tableData.value = data.list || []
    total.value = data.total
  } catch (error) {
    tableData.value = []
    total.value = 0
    showError(error?.msg || error?.message || '加载验收记录失败')
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
  filters.auditStatus = ''
  filters.deviceCondition = ''
  pageNum.value = 1
  loadData()
}

async function openDetail(row) {
  detailVisible.value = true
  detailLoading.value = true
  detailData.value = null
  try {
    detailData.value = await getRentInspectionDetail(row.id)
  } catch (error) {
    showError(error?.msg || error?.message || '加载验收详情失败')
    detailVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

function openAudit(row) {
  auditForm.id = row.id
  auditForm.auditStatus = 'approved'
  auditForm.auditRemark = ''
  auditVisible.value = true
}

async function submitAudit() {
  if (!auditForm.id) return
  auditLoading.value = true
  try {
    await auditRentInspection({
      id: auditForm.id,
      auditStatus: auditForm.auditStatus,
      auditRemark: auditForm.auditRemark?.trim() || undefined,
    })
    showSuccess('复核成功')
    auditVisible.value = false
    detailVisible.value = false
    await loadData()
    void menuBadgeStore.refresh()
  } catch (error) {
    showError(error?.msg || error?.message || '复核失败')
  } finally {
    auditLoading.value = false
  }
}

function goOrder(row) {
  if (row.orderId) {
    router.push({ name: 'rental-order-detail', params: { id: row.orderId } })
  }
}

onMounted(() => {
  if (route.query.keyword) {
    filters.keyword = String(route.query.keyword)
  }
  if (route.query.auditStatus) {
    filters.auditStatus = String(route.query.auditStatus)
  }
  loadData()
})
</script>

<template>
  <el-card shadow="never" class="page-card">
    <template #header><strong>验收记录</strong></template>

    <el-form inline class="filter-form" @submit.prevent="handleSearch">
      <el-form-item label="订单ID">
        <el-input
          v-model="filters.keyword"
          clearable
          placeholder="订单ID / 验收ID"
          style="width: 200px"
          @keyup.enter="handleSearch"
        />
      </el-form-item>
      <el-form-item label="复核状态">
        <el-select v-model="filters.auditStatus" clearable placeholder="全部" style="width: 130px">
          <el-option
            v-for="(meta, key) in INSPECTION_AUDIT_STATUS_MAP"
            :key="key"
            :label="meta.label"
            :value="key"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="设备状况">
        <el-select v-model="filters.deviceCondition" clearable placeholder="全部" style="width: 130px">
          <el-option
            v-for="(meta, key) in DEVICE_CONDITION_MAP"
            :key="key"
            :label="meta.label"
            :value="key"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="tableData" border empty-text="暂无验收记录">
      <el-table-column prop="id" label="验收ID" width="100" />
      <el-table-column label="订单ID" width="100" show-overflow-tooltip>
        <template #default="{ row }">
          <LinkOrder :order-id="row.orderId" />
        </template>
      </el-table-column>
      <el-table-column label="用户" min-width="120">
        <template #default="{ row }">
          <LinkUser :user-id="row.userId" :label="row.userNickName || '-'" />
        </template>
      </el-table-column>
      <el-table-column label="手机号" width="130">
        <template #default="{ row }">
          <LinkUser :user-id="row.userId" :phone="row.userPhone || '-'" />
        </template>
      </el-table-column>
      <el-table-column label="设备状况" width="110">
        <template #default="{ row }">
          <el-tag :type="getDeviceConditionMeta(row.deviceCondition).type" size="small">
            {{ getDeviceConditionMeta(row.deviceCondition).label }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="扣款金额" width="100">
        <template #default="{ row }">{{ formatMoney(row.deductAmount) }}</template>
      </el-table-column>
      <el-table-column label="退还押金" width="100">
        <template #default="{ row }">{{ formatMoney(row.refundAmount) }}</template>
      </el-table-column>
      <el-table-column label="复核状态" width="90" align="center">
        <template #default="{ row }">
          <StatusSwitch
            :model-value="row.auditStatus"
            :active-values="INSPECTION_AUDIT_ON_STATUSES"
            readonly
          />
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="验收时间" min-width="170" />
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <div class="table-actions">
            <el-button size="small" type="primary" @click="openDetail(row)">详情</el-button>
            <el-button size="small" type="info" @click="goOrder(row)">订单</el-button>
            <el-button v-if="canAuditInspection(row)" size="small" type="success" @click="openAudit(row)">
              复核
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

  <el-dialog v-model="detailVisible" title="验收详情" width="640px" destroy-on-close>
    <div v-loading="detailLoading">
      <template v-if="detailData">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="验收ID">{{ detailData.id }}</el-descriptions-item>
          <el-descriptions-item label="订单ID">
            <LinkOrder :order-id="detailData.orderId" />
          </el-descriptions-item>
          <el-descriptions-item label="用户">
            <LinkUser :user-id="detailData.userId" :label="detailData.userNickName || '-'" />
          </el-descriptions-item>
          <el-descriptions-item label="手机号">
            <LinkUser :user-id="detailData.userId" :phone="detailData.userPhone || '-'" />
          </el-descriptions-item>
          <el-descriptions-item label="设备状况">
            <el-tag :type="getDeviceConditionMeta(detailData.deviceCondition).type" size="small">
              {{ getDeviceConditionMeta(detailData.deviceCondition).label }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="扣款金额">{{ formatMoney(detailData.deductAmount) }}</el-descriptions-item>
          <el-descriptions-item label="退还押金">{{ formatMoney(detailData.refundAmount) }}</el-descriptions-item>
          <el-descriptions-item label="复核状态">
            <el-tag :type="getInspectionAuditMeta(detailData.auditStatus).type" size="small">
              {{ getInspectionAuditMeta(detailData.auditStatus).label }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="验收时间">{{ detailData.createTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="验收备注" :span="2">{{ detailData.remark || '-' }}</el-descriptions-item>
          <el-descriptions-item v-if="detailData.auditRemark" label="复核备注" :span="2">
            {{ detailData.auditRemark }}
          </el-descriptions-item>
          <el-descriptions-item v-if="detailData.auditTime" label="复核时间" :span="2">
            {{ detailData.auditTime }}
          </el-descriptions-item>
        </el-descriptions>
        <div v-if="detailData.imageUrls?.length" class="detail-images">
          <p class="detail-images__title">验收图片</p>
          <el-space wrap>
            <el-image
              v-for="(url, index) in detailData.imageUrls"
              :key="`${url}-${index}`"
              :src="url"
              :preview-src-list="detailData.imageUrls"
              fit="cover"
              style="width: 88px; height: 88px; border-radius: 4px"
            />
          </el-space>
        </div>
      </template>
    </div>
    <template #footer>
      <el-button @click="detailVisible = false">关闭</el-button>
      <el-button
        v-if="detailData && canAuditInspection(detailData)"
        type="primary"
        @click="openAudit(detailData)"
      >
        复核
      </el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="auditVisible" title="验收复核" width="480px" destroy-on-close>
    <el-form label-width="88px">
      <el-form-item label="复核结果" required>
        <el-radio-group v-model="auditForm.auditStatus">
          <el-radio :value="'approved'">通过</el-radio>
          <el-radio :value="'rejected'">驳回</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="复核备注">
        <el-input
          v-model="auditForm.auditRemark"
          type="textarea"
          :rows="3"
          maxlength="200"
          show-word-limit
          placeholder="填写复核说明（驳回时建议填写原因）"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="auditVisible = false">取消</el-button>
      <el-button type="primary" :loading="auditLoading" @click="submitAudit">确认复核</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.filter-form {
  margin-bottom: 12px;
}

.detail-images {
  margin-top: 16px;
}

.detail-images__title {
  margin: 0 0 8px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
</style>
