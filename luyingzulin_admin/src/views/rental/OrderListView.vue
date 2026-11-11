<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BasePagination from '@/components/common/BasePagination.vue'
import { cancelRentOrder, getRentOrderPage } from '@/api/rental/order'
import {
  RENT_ORDER_TABS,
  canAdminCancelOrder,
  formatGoodsSummary,
  formatMoney,
  formatRentPeriod,
  getRentOrderStatusMeta,
  resolveOrderCoverImage,
} from '@/utils/rentOrder'
import LinkUser from '@/components/common/LinkUser.vue'
import LinkOrder from '@/components/common/LinkOrder.vue'
import StatusSwitch from '@/components/common/StatusSwitch.vue'
import { ORDER_SWITCH_ON_STATUSES } from '@/utils/statusSwitch'
import { confirmAction, showError, showSuccess } from '@/utils/modal'
import NoticeBadge from '@/components/common/NoticeBadge.vue'
import { useMenuBadgeStore } from '@/stores/menuBadge'

const router = useRouter()
const route = useRoute()
const menuBadgeStore = useMenuBadgeStore()

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const activeTab = ref('all')

const filters = reactive({
  keyword: '',
  merchantId: null,
  startTime: '',
  endTime: '',
})

function currentStatusFilter() {
  const tab = RENT_ORDER_TABS.find((item) => item.name === activeTab.value)
  return tab?.status
}

async function loadData() {
  loading.value = true
  try {
    const data = await getRentOrderPage({
      page: pageNum.value,
      limit: pageSize.value,
      status: currentStatusFilter(),
      keyword: filters.keyword?.trim() || undefined,
      merchantId: filters.merchantId ?? undefined,
      startTime: filters.startTime || undefined,
      endTime: filters.endTime || undefined,
    })
    tableData.value = data.list || []
    total.value = data.total
  } catch (error) {
    tableData.value = []
    total.value = 0
    showError(error?.msg || error?.message || '加载订单列表失败')
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
  filters.merchantId = null
  filters.startTime = ''
  filters.endTime = ''
  pageNum.value = 1
  loadData()
}

function handleTabChange() {
  pageNum.value = 1
  loadData()
}

function goDetail(row) {
  router.push({ name: 'rental-order-detail', params: { id: row.id } })
}

async function handleCancel(row) {
  await confirmAction(`确认代取消订单「#${row.id}」吗？取消后将释放库存预占。`)
  try {
    await cancelRentOrder(row.id)
    showSuccess('订单已取消')
    await loadData()
    void menuBadgeStore.refresh()
  } catch (error) {
    showError(error?.msg || error?.message || '取消失败')
  }
}

function exportCurrentPage() {
  if (!tableData.value.length) {
    showError('当前页无数据可导出')
    return
  }
  const headers = ['订单ID', '用户', '手机号', '商品', '租期', '应付', '状态', '下单时间']
  const rows = tableData.value.map((row) => {
    const statusMeta = getRentOrderStatusMeta(row.status)
    return [
      row.id || '',
      row.userNickName || row.nickName || '',
      row.userPhone || row.phone || '',
      formatGoodsSummary(row),
      formatRentPeriod(row),
      row.payAmount ?? '',
      statusMeta.label,
      row.createTime || '',
    ]
  })
  const csv = [headers, ...rows]
    .map((line) => line.map((cell) => `"${String(cell).replace(/"/g, '""')}"`).join(','))
    .join('\n')
  const blob = new Blob([`\ufeff${csv}`], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `租赁订单_${new Date().toISOString().slice(0, 10)}.csv`
  link.click()
  URL.revokeObjectURL(url)
  showSuccess('已导出当前页数据')
}

watch(
  () => [route.query.status, route.query.keyword, route.query.merchantId],
  ([status, keyword, merchantId]) => {
    if (status && RENT_ORDER_TABS.some((tab) => tab.name === status)) {
      activeTab.value = status
    }
    if (typeof keyword === 'string') {
      filters.keyword = keyword
    }
    if (merchantId !== undefined) {
      filters.merchantId = merchantId ? Number(merchantId) : null
    }
  },
  { immediate: true },
)

onMounted(loadData)
</script>

<template>
  <el-card shadow="never" class="page-card">
    <template #header>
      <div class="card-header">
        <strong>租赁订单</strong>
        <el-button @click="exportCurrentPage">导出当前页</el-button>
      </div>
    </template>

    <el-tabs v-model="activeTab" class="order-tabs" @tab-change="handleTabChange">
      <el-tab-pane v-for="tab in RENT_ORDER_TABS" :key="tab.name" :name="tab.name">
        <template #label>
          <span class="order-tab-label">
            {{ tab.label }}
            <NoticeBadge :value="menuBadgeStore.getOrderTabBadge(tab.name)" />
          </span>
        </template>
      </el-tab-pane>
    </el-tabs>

    <el-form inline class="filter-form" @submit.prevent="handleSearch">
      <el-form-item label="关键词">
        <el-input
          v-model="filters.keyword"
          clearable
          placeholder="订单ID / 用户手机号 / 商品名"
          style="width: 240px"
          @keyup.enter="handleSearch"
        />
      </el-form-item>
      <el-form-item label="下单时间">
        <el-date-picker
          v-model="filters.startTime"
          type="datetime"
          value-format="YYYY-MM-DD HH:mm:ss"
          placeholder="开始时间"
          style="width: 190px"
        />
        <span class="range-sep">至</span>
        <el-date-picker
          v-model="filters.endTime"
          type="datetime"
          value-format="YYYY-MM-DD HH:mm:ss"
          placeholder="结束时间"
          style="width: 190px"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="tableData" border empty-text="暂无订单数据">
      <el-table-column label="订单ID" width="100" show-overflow-tooltip>
        <template #default="{ row }">
          <LinkOrder :order-id="row.id" />
        </template>
      </el-table-column>
      <el-table-column label="用户" min-width="120">
        <template #default="{ row }">
          <LinkUser
            :user-id="row.userId"
            :label="row.userNickName || row.nickName || '-'"
          />
        </template>
      </el-table-column>
      <el-table-column label="手机号" width="130">
        <template #default="{ row }">
          <LinkUser
            :user-id="row.userId"
            :phone="row.userPhone || row.phone || '-'"
          />
        </template>
      </el-table-column>
      <el-table-column label="商品" min-width="220" show-overflow-tooltip>
        <template #default="{ row }">
          <div class="goods-cell">
            <el-image
              v-if="resolveOrderCoverImage(row)"
              :src="resolveOrderCoverImage(row)"
              fit="cover"
              class="goods-cell__cover"
            />
            <span class="goods-cell__name">{{ formatGoodsSummary(row) }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="租期" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">{{ formatRentPeriod(row) }}</template>
      </el-table-column>
      <el-table-column label="应付" width="100">
        <template #default="{ row }">{{ formatMoney(row.payAmount) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="80" align="center">
        <template #default="{ row }">
          <StatusSwitch
            :model-value="row.status"
            :active-values="ORDER_SWITCH_ON_STATUSES"
            readonly
          />
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="下单时间" min-width="170" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <div class="table-actions">
            <el-button size="small" type="primary" @click="goDetail(row)">详情</el-button>
            <el-button
              v-if="canAdminCancelOrder(row.status)"
              size="small"
              type="danger"
              @click="handleCancel(row)"
            >
              取消
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
</template>

<style scoped>
.filter-form {
  margin-bottom: 12px;
}

.order-tabs {
  margin-bottom: 8px;
}

.order-tab-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.range-sep {
  margin: 0 8px;
  color: var(--el-text-color-secondary);
}

.goods-cell {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.goods-cell__cover {
  width: 48px;
  height: 48px;
  border-radius: 4px;
  flex-shrink: 0;
}

.goods-cell__name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
