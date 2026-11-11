<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { cancelRentOrder, getRentOrderDetail } from '@/api/rental/order'
import {
  getRentInspectionByOrderId,
  getRentInspectionDetail,
} from '@/api/rental/inspection'
import {
  canAdminCancelOrder,
  formatMoney,
  formatOperatorType,
  formatPickupType,
  formatRentPeriod,
  formatRentType,
  getRentOrderStatusMeta,
  getTimelineNodeLabel,
} from '@/utils/rentOrder'
import {
  formatReturnApplyStatus,
  formatReturnMethod,
  getDeviceConditionMeta,
  getInspectionAuditMeta,
  getRenewPayStatusMeta,
} from '@/utils/rentInspection'
import LinkUser from '@/components/common/LinkUser.vue'
import ImagePreviewList from '@/components/common/ImagePreviewList.vue'
import { confirmAction, showError, showSuccess } from '@/utils/modal'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const actionLoading = ref(false)
const detail = ref(null)
const inspectionDetail = ref(null)

const orderId = computed(() => route.params.id)
const statusMeta = computed(() => getRentOrderStatusMeta(detail.value?.status))
const items = computed(() => detail.value?.items || [])
const logs = computed(() => detail.value?.logs || [])
const renewRecords = computed(() => detail.value?.renewRecords || [])
const returnApply = computed(() => detail.value?.returnApply || null)
const inspection = computed(() => detail.value?.inspection || inspectionDetail.value || null)

const depositRefundSummary = computed(() => {
  const insp = inspection.value
  if (!insp) return null
  return {
    depositAmount: detail.value?.depositAmount,
    deductAmount: insp.deductAmount,
    refundAmount: insp.refundAmount,
    auditStatus: insp.auditStatus,
  }
})

async function loadInspectionExtra(id) {
  if (detail.value?.inspection) {
    inspectionDetail.value = detail.value.inspection
    return
  }
  try {
    const brief = await getRentInspectionByOrderId(id)
    if (brief?.id) {
      inspectionDetail.value = await getRentInspectionDetail(brief.id)
    } else {
      inspectionDetail.value = null
    }
  } catch {
    inspectionDetail.value = null
  }
}

async function loadDetail() {
  loading.value = true
  try {
    detail.value = await getRentOrderDetail(orderId.value)
    if (detail.value?.id) {
      await loadInspectionExtra(detail.value.id)
    }
  } catch (error) {
    detail.value = null
    inspectionDetail.value = null
    showError(error?.msg || error?.message || '加载订单详情失败')
  } finally {
    loading.value = false
  }
}

async function handleCancel() {
  if (!detail.value) return
  await confirmAction(`确认代取消订单「#${detail.value.id}」吗？`)
  actionLoading.value = true
  try {
    await cancelRentOrder(detail.value.id)
    showSuccess('订单已取消')
    await loadDetail()
  } catch (error) {
    showError(error?.msg || error?.message || '取消失败')
  } finally {
    actionLoading.value = false
  }
}

function goBack() {
  router.push({ name: 'rental-orders' })
}

function goInspection() {
  if (inspection.value?.id) {
    router.push({ name: 'rental-inspections', query: { keyword: detail.value?.id } })
  }
}

onMounted(loadDetail)
</script>

<template>
  <div v-loading="loading" class="order-detail-page">
    <el-page-header class="page-header" @back="goBack">
      <template #content>
        <span class="page-title">订单详情</span>
        <span v-if="detail?.id" class="order-no">订单 #{{ detail.id }}</span>
      </template>
    </el-page-header>

    <el-empty v-if="!loading && !detail" description="订单不存在或加载失败" />

    <template v-else-if="detail">
      <el-card shadow="never" class="page-card status-card">
        <div class="status-head">
          <div>
            <el-tag size="large" :type="statusMeta.type">{{ statusMeta.label }}</el-tag>
            <p class="status-tip">下单时间：{{ detail.createTime || '-' }}</p>
          </div>
          <el-space>
            <el-button v-if="inspection?.id" @click="goInspection">查看验收</el-button>
            <el-button
              v-if="canAdminCancelOrder(detail.status)"
              type="danger"
              :loading="actionLoading"
              @click="handleCancel"
            >
              代取消
            </el-button>
          </el-space>
        </div>
      </el-card>

      <el-row :gutter="16">
        <el-col :xs="24" :md="12">
          <el-card shadow="never" class="page-card">
            <template #header><strong>用户信息</strong></template>
            <el-descriptions :column="1" border>
              <el-descriptions-item label="用户ID">
                <LinkUser :user-id="detail.userId" :label="String(detail.userId || '-')" />
              </el-descriptions-item>
              <el-descriptions-item label="昵称">
                <LinkUser
                  :user-id="detail.userId"
                  :label="detail.userNickName || detail.nickName || '-'"
                />
              </el-descriptions-item>
              <el-descriptions-item label="手机号">
                <LinkUser
                  :user-id="detail.userId"
                  :phone="detail.userPhone || detail.phone || '-'"
                />
              </el-descriptions-item>
              <el-descriptions-item label="联系人">
                {{ detail.contactName || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="联系电话">
                {{ detail.contactPhone || '-' }}
              </el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-col>

        <el-col :xs="24" :md="12">
          <el-card shadow="never" class="page-card">
            <template #header><strong>租期与取货</strong></template>
            <el-descriptions :column="1" border>
              <el-descriptions-item label="租赁类型">
                {{ formatRentType(detail.rentType) }}
              </el-descriptions-item>
              <el-descriptions-item label="租期">
                {{ formatRentPeriod(detail) }}
              </el-descriptions-item>
              <el-descriptions-item label="取货方式">
                {{ formatPickupType(detail.pickupType) }}
              </el-descriptions-item>
              <el-descriptions-item label="门店/地址">
                {{ detail.pickupStoreName || detail.addressDetail || detail.address || '-' }}
              </el-descriptions-item>
              <el-descriptions-item v-if="detail.pickupCode" label="取货码">
                {{ detail.pickupCode }}
              </el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-col>
      </el-row>

      <el-card shadow="never" class="page-card">
        <template #header><strong>商品明细</strong></template>
        <el-table :data="items" border empty-text="暂无商品明细">
          <el-table-column label="封面" width="80">
            <template #default="{ row }">
              <el-image
                v-if="row.coverImage"
                :src="row.coverImage"
                fit="cover"
                style="width: 48px; height: 48px; border-radius: 4px"
              />
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column prop="goodsName" label="商品名称" min-width="140" show-overflow-tooltip />
          <el-table-column prop="specInfo" label="规格" min-width="120" show-overflow-tooltip />
          <el-table-column label="租赁类型" width="90">
            <template #default="{ row }">{{ formatRentType(row.rentType) }}</template>
          </el-table-column>
          <el-table-column prop="qty" label="数量" width="70" />
          <el-table-column label="租金" width="100">
            <template #default="{ row }">{{ formatMoney(row.rentPrice ?? row.itemAmount) }}</template>
          </el-table-column>
          <el-table-column label="押金" width="100">
            <template #default="{ row }">{{ formatMoney(row.depositAmount) }}</template>
          </el-table-column>
          <el-table-column label="小计" width="100">
            <template #default="{ row }">{{ formatMoney(row.itemAmount) }}</template>
          </el-table-column>
        </el-table>
      </el-card>

      <el-row :gutter="16">
        <el-col :xs="24" :md="12">
          <el-card shadow="never" class="page-card">
            <template #header><strong>费用明细</strong></template>
            <div class="fee-lines">
              <div class="fee-line"><span>租金合计</span><span>{{ formatMoney(detail.rentAmount) }}</span></div>
              <div class="fee-line"><span>押金合计</span><span>{{ formatMoney(detail.depositAmount) }}</span></div>
              <div class="fee-line"><span>配送费</span><span>{{ formatMoney(detail.deliveryAmount) }}</span></div>
              <div class="fee-line"><span>优惠抵扣</span><span>-{{ formatMoney(detail.couponAmount) }}</span></div>
              <div class="fee-line total"><span>应付总额</span><span>{{ formatMoney(detail.payAmount) }}</span></div>
            </div>
          </el-card>
        </el-col>

        <el-col :xs="24" :md="12">
          <el-card shadow="never" class="page-card">
            <template #header><strong>关键时间节点</strong></template>
            <el-descriptions :column="1" border>
              <el-descriptions-item label="支付时间">{{ detail.payTime || '-' }}</el-descriptions-item>
              <el-descriptions-item label="取货时间">{{ detail.pickupTime || '-' }}</el-descriptions-item>
              <el-descriptions-item label="归还时间">{{ detail.returnTime || '-' }}</el-descriptions-item>
              <el-descriptions-item label="完成时间">{{ detail.completeTime || '-' }}</el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-col>
      </el-row>

      <el-card shadow="never" class="page-card">
        <template #header><strong>续租记录</strong></template>
        <el-table v-if="renewRecords.length" :data="renewRecords" border>
          <el-table-column prop="renewNo" label="续租单号" min-width="160" show-overflow-tooltip />
          <el-table-column prop="extraDays" label="续租天数" width="90" />
          <el-table-column label="续租租金" width="110">
            <template #default="{ row }">{{ formatMoney(row.renewAmount) }}</template>
          </el-table-column>
          <el-table-column label="续租前结束" min-width="170">
            <template #default="{ row }">{{ row.oldEndTime || '-' }}</template>
          </el-table-column>
          <el-table-column label="续租后结束" min-width="170">
            <template #default="{ row }">{{ row.newEndTime || '-' }}</template>
          </el-table-column>
          <el-table-column label="支付状态" width="100">
            <template #default="{ row }">
              <el-tag :type="getRenewPayStatusMeta(row.payStatus).type" size="small">
                {{ getRenewPayStatusMeta(row.payStatus).label }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="payTime" label="支付时间" min-width="170" />
        </el-table>
        <el-empty v-else description="暂无续租记录" />
      </el-card>

      <el-card shadow="never" class="page-card">
        <template #header><strong>归还信息</strong></template>
        <el-descriptions v-if="returnApply" :column="2" border>
          <el-descriptions-item label="归还方式">
            {{ formatReturnMethod(returnApply.returnMethod) }}
          </el-descriptions-item>
          <el-descriptions-item label="计划归还日期">
            {{ returnApply.returnDate || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="申请状态">
            {{ formatReturnApplyStatus(returnApply.applyStatus) }}
          </el-descriptions-item>
          <el-descriptions-item label="申请时间">
            {{ returnApply.applyTime || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="归还备注" :span="2">
            {{ returnApply.remark || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="归还图片" :span="2">
            <ImagePreviewList :urls="returnApply.imageUrls" :size="88" />
          </el-descriptions-item>
        </el-descriptions>
        <el-empty v-else description="暂无归还申请" />
      </el-card>

      <el-row :gutter="16">
        <el-col :xs="24" :md="12">
          <el-card shadow="never" class="page-card">
            <template #header><strong>验收结果</strong></template>
            <template v-if="inspection">
              <el-descriptions :column="1" border>
                <el-descriptions-item label="设备状况">
                  <el-tag :type="getDeviceConditionMeta(inspection.deviceCondition).type" size="small">
                    {{ getDeviceConditionMeta(inspection.deviceCondition).label }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="扣款金额">
                  {{ formatMoney(inspection.deductAmount) }}
                </el-descriptions-item>
                <el-descriptions-item label="退还押金">
                  {{ formatMoney(inspection.refundAmount) }}
                </el-descriptions-item>
                <el-descriptions-item label="复核状态">
                  <el-tag :type="getInspectionAuditMeta(inspection.auditStatus).type" size="small">
                    {{ getInspectionAuditMeta(inspection.auditStatus).label }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="验收时间">
                  {{ inspection.createTime || '-' }}
                </el-descriptions-item>
                <el-descriptions-item label="验收备注">
                  {{ inspection.remark || '-' }}
                </el-descriptions-item>
              </el-descriptions>
              <div v-if="inspection.imageUrls?.length" class="inspection-images">
                <p class="section-subtitle">验收图片</p>
                <el-space wrap>
                  <el-image
                    v-for="(url, index) in inspection.imageUrls"
                    :key="`${url}-${index}`"
                    :src="url"
                    :preview-src-list="inspection.imageUrls"
                    fit="cover"
                    style="width: 72px; height: 72px; border-radius: 4px"
                  />
                </el-space>
              </div>
            </template>
            <el-empty v-else description="暂无验收记录" />
          </el-card>
        </el-col>

        <el-col :xs="24" :md="12">
          <el-card shadow="never" class="page-card">
            <template #header><strong>押金退还</strong></template>
            <template v-if="depositRefundSummary">
              <div class="fee-lines">
                <div class="fee-line"><span>订单押金</span><span>{{ formatMoney(depositRefundSummary.depositAmount) }}</span></div>
                <div class="fee-line"><span>验收扣款</span><span>-{{ formatMoney(depositRefundSummary.deductAmount) }}</span></div>
                <div class="fee-line total"><span>应退押金</span><span>{{ formatMoney(depositRefundSummary.refundAmount) }}</span></div>
              </div>
              <p v-if="depositRefundSummary.auditStatus === 'pending'" class="deposit-tip">
                验收待复核，退押金额以复核通过为准
              </p>
            </template>
            <el-empty v-else description="订单未完成验收，暂无退押明细" />
          </el-card>
        </el-col>
      </el-row>

      <el-card shadow="never" class="page-card">
        <template #header><strong>状态时间轴</strong></template>
        <el-timeline v-if="logs.length">
          <el-timeline-item
            v-for="log in logs"
            :key="log.id || `${log.createTime}-${log.toStatus}`"
            :timestamp="log.createTime"
            placement="top"
          >
            <p class="timeline-title">{{ getTimelineNodeLabel(log) }}</p>
            <p v-if="log.operatorType" class="timeline-meta">
              操作方：{{ formatOperatorType(log.operatorType) }}
            </p>
            <p v-if="log.remark" class="timeline-remark">{{ log.remark }}</p>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else description="暂无状态流转记录" />
      </el-card>
    </template>
  </div>
</template>

<style scoped>
.order-detail-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  margin-bottom: 4px;
}

.page-title {
  font-weight: 600;
}

.order-no {
  margin-left: 12px;
  color: var(--el-text-color-secondary);
}

.status-card :deep(.el-card__body) {
  padding-top: 16px;
  padding-bottom: 16px;
}

.status-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.status-tip {
  margin: 8px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.fee-lines {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.fee-line {
  display: flex;
  justify-content: space-between;
  color: var(--el-text-color-regular);
}

.fee-line.total {
  margin-top: 8px;
  padding-top: 12px;
  border-top: 1px solid var(--el-border-color-lighter);
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.timeline-title {
  margin: 0;
  font-weight: 500;
}

.timeline-meta {
  margin: 4px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.timeline-remark {
  margin: 4px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.inspection-images {
  margin-top: 12px;
}

.section-subtitle {
  margin: 0 0 8px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.deposit-tip {
  margin: 12px 0 0;
  color: var(--el-color-warning);
  font-size: 13px;
}
</style>
