<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BasePagination from '@/components/common/BasePagination.vue'
import { getRentCategoryTree } from '@/api/rental/category'
import {
  deleteRentGoods,
  getRentGoodsPage,
  updateRentGoodsState,
} from '@/api/rental/goods'
import { flattenRentCategories } from '@/utils/rentalCategory'
import { getMerchantOptions } from '@/api/merchant/merchant'
import { useAuthStore } from '@/stores/auth'
import StatusSwitch from '@/components/common/StatusSwitch.vue'
import { confirmAction, showError, showSuccess } from '@/utils/modal'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const categoryOptions = ref([])
const merchantOptions = ref([])

const filters = reactive({
  keyword: '',
  categoryId: null,
  merchantId: null,
  status: null,
})

async function loadMerchantOptions() {
  try {
    merchantOptions.value = await getMerchantOptions()
  } catch {
    merchantOptions.value = []
  }
}

async function loadCategoryOptions() {
  try {
    const tree = await getRentCategoryTree()
    categoryOptions.value = flattenRentCategories(Array.isArray(tree) ? tree : [])
  } catch {
    categoryOptions.value = []
  }
}

async function loadData() {
  loading.value = true
  try {
    const data = await getRentGoodsPage({
      page: pageNum.value,
      limit: pageSize.value,
      keyword: filters.keyword?.trim() || undefined,
      categoryId: filters.categoryId ?? undefined,
      merchantId: filters.merchantId ?? undefined,
      status: filters.status ?? undefined,
    })
    tableData.value = data.list || []
    total.value = data.total
  } catch (error) {
    tableData.value = []
    total.value = 0
    showError(error?.msg || error?.message || '加载商品列表失败')
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
  filters.categoryId = null
  filters.merchantId = null
  filters.status = null
  pageNum.value = 1
  loadData()
}

function goCreate() {
  router.push({ name: 'rental-goods-create' })
}

function goMerchantDetail(row) {
  if (!row.merchantId) return
  router.push({ path: '/merchant/list', query: { merchantId: row.merchantId } })
}

function goEdit(row) {
  router.push({ name: 'rental-goods-edit', params: { id: row.id } })
}

async function handleStatusChange(row, nextStatus) {
  const action = nextStatus === 1 ? '上架' : '下架'
  try {
    await confirmAction(`确认${action}商品「${row.name}」吗？`)
    await updateRentGoodsState({ id: row.id, status: nextStatus })
    row.status = nextStatus
    showSuccess(`已${action}`)
  } catch (error) {
    if (error?.msg || error?.message) {
      showError(error.msg || error.message)
    }
  }
}

async function removeRow(row) {
  await confirmAction(`确认删除商品「${row.name}」吗？`)
  try {
    await deleteRentGoods({ id: row.id })
    showSuccess('删除成功')
    await loadData()
  } catch (error) {
    showError(error?.msg || error?.message || '删除失败')
  }
}

function formatPrice(value) {
  if (value === null || value === undefined || value === '') return '-'
  return `¥${Number(value).toFixed(2)}`
}

function formatTags(tags) {
  if (!Array.isArray(tags) || !tags.length) return '-'
  return tags.join('、')
}

onMounted(async () => {
  if (route.query.merchantId) {
    filters.merchantId = Number(route.query.merchantId) || null
  }
  await Promise.all([loadCategoryOptions(), loadMerchantOptions()])
  await loadData()
})

watch(
  () => route.query.merchantId,
  (merchantId) => {
    if (merchantId !== undefined) {
      filters.merchantId = merchantId ? Number(merchantId) : null
      pageNum.value = 1
      loadData()
    }
  },
)
</script>

<template>
  <el-card shadow="never" class="page-card">
    <template #header>
      <div class="card-header">
        <strong>租赁商品</strong>
        <el-tooltip content="商品由商家在小程序发布，管理端主要用于监管与违规处理" placement="left">
          <el-button
            v-if="authStore.canSuperviseCreateGoods"
            type="primary"
            plain
            @click="goCreate"
          >
            监管新建
          </el-button>
        </el-tooltip>
      </div>
    </template>

    <el-form inline class="filter-form" @submit.prevent="handleSearch">
      <el-form-item label="关键词">
        <el-input v-model="filters.keyword" clearable placeholder="商品ID / 名称 / 副标题" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="所属分类">
        <el-select v-model="filters.categoryId" clearable filterable placeholder="全部分类" style="width: 160px">
          <el-option v-for="item in categoryOptions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="所属商户">
        <el-select v-model="filters.merchantId" clearable filterable placeholder="全部商户" style="width: 180px">
          <el-option v-for="item in merchantOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="上架状态">
        <el-select v-model="filters.status" clearable placeholder="全部" style="width: 120px">
          <el-option label="已上架" :value="1" />
          <el-option label="已下架" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="tableData" border empty-text="暂无商品，可点击右上角新增">
      <el-table-column label="封面" width="90">
        <template #default="{ row }">
          <el-image
            v-if="row.coverImage"
            :src="row.coverImage"
            fit="cover"
            style="width: 56px; height: 56px; border-radius: 4px"
          />
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="id" label="ID" width="90" />
      <el-table-column prop="name" label="商品名称" min-width="140" show-overflow-tooltip />
      <el-table-column prop="subtitle" label="副标题" min-width="120" show-overflow-tooltip />
      <el-table-column prop="categoryName" label="所属分类" min-width="110" />
      <el-table-column label="所属商户" min-width="120" show-overflow-tooltip>
        <template #default="{ row }">
          <el-button
            v-if="row.merchantId"
            link
            type="primary"
            @click="goMerchantDetail(row)"
          >
            {{ row.merchantName || `商户#${row.merchantId}` }}
          </el-button>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="日租价" width="90">
        <template #default="{ row }">{{ formatPrice(row.rentPriceDay) }}</template>
      </el-table-column>
      <el-table-column label="时租价" width="90">
        <template #default="{ row }">{{ formatPrice(row.rentPriceHour) }}</template>
      </el-table-column>
      <el-table-column label="过夜价" width="90">
        <template #default="{ row }">{{ formatPrice(row.rentPriceOvernight) }}</template>
      </el-table-column>
      <el-table-column label="押金" width="90">
        <template #default="{ row }">{{ formatPrice(row.depositAmount) }}</template>
      </el-table-column>
      <el-table-column prop="availableStock" label="可用库存" width="90" />
      <el-table-column prop="salesCount" label="销量" width="80" />
      <el-table-column label="标签" min-width="120" show-overflow-tooltip>
        <template #default="{ row }">{{ formatTags(row.tags) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="80" align="center">
        <template #default="{ row }">
          <StatusSwitch
            :model-value="Number(row.status ?? 0)"
            :active-value="1"
            :inactive-value="0"
            @change="(value) => handleStatusChange(row, value)"
          />
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="160" fixed="right">
        <template #default="{ row }">
          <div class="table-actions">
            <el-button size="small" type="warning" @click="goEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="removeRow(row)">删除</el-button>
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
</style>
