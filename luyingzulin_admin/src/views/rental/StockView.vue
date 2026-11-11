<script setup>
import { onMounted, reactive, ref } from 'vue'
import BasePagination from '@/components/common/BasePagination.vue'
import { getRentCategoryTree } from '@/api/rental/category'
import { adjustRentStock, checkRentStockOccupy, getRentStockPage } from '@/api/rental/stock'
import { getRentGoodsDetail } from '@/api/rental/goods'
import { flattenRentCategories } from '@/utils/rentalCategory'
import { confirmAction, showError, showSuccess } from '@/utils/modal'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const categoryOptions = ref([])

const filters = reactive({
  keyword: '',
  categoryId: null,
})

const occupyVisible = ref(false)
const occupyLoading = ref(false)
const occupyResult = ref(null)
const occupyForm = reactive({
  goodsId: null,
  goodsName: '',
  startTime: '',
  endTime: '',
  quantity: 1,
})

const adjustVisible = ref(false)
const adjustLoading = ref(false)
const adjustForm = reactive({
  goodsId: null,
  goodsName: '',
  totalStock: 0,
  availableStock: 0,
  adjustQty: 0,
  reason: '',
})

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
    const data = await getRentStockPage({
      page: pageNum.value,
      limit: pageSize.value,
      keyword: filters.keyword?.trim() || undefined,
      categoryId: filters.categoryId ?? undefined,
    })
    tableData.value = data.list || []
    total.value = data.total
  } catch (error) {
    tableData.value = []
    total.value = 0
    showError(error?.msg || error?.message || '加载库存列表失败')
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
  pageNum.value = 1
  loadData()
}

function openOccupyQuery(row) {
  occupyForm.goodsId = row.id
  occupyForm.goodsName = row.goodsName || row.name || ''
  occupyForm.startTime = ''
  occupyForm.endTime = ''
  occupyForm.quantity = 1
  occupyResult.value = null
  occupyVisible.value = true
}

async function submitOccupyQuery() {
  if (!occupyForm.goodsId || !occupyForm.startTime || !occupyForm.endTime) {
    showError('请选择商品并填写完整租期时段')
    return
  }
  occupyLoading.value = true
  occupyResult.value = null
  try {
    occupyResult.value = await checkRentStockOccupy({
      goodsId: occupyForm.goodsId,
      specId: 0,
      startTime: occupyForm.startTime,
      endTime: occupyForm.endTime,
      quantity: occupyForm.quantity,
    })
  } catch (error) {
    showError(error?.msg || error?.message || '占用查询失败')
  } finally {
    occupyLoading.value = false
  }
}

function openAdjust(row) {
  adjustForm.goodsId = row.id
  adjustForm.goodsName = row.goodsName || row.name || ''
  adjustForm.totalStock = row.totalStock ?? 0
  adjustForm.availableStock = row.availableStock ?? 0
  adjustForm.adjustQty = 0
  adjustForm.reason = ''
  adjustVisible.value = true
  if (row.id && row.totalStock == null) {
    getRentGoodsDetail(row.id).then((detail) => {
      adjustForm.totalStock = detail?.totalStock ?? adjustForm.totalStock
      adjustForm.availableStock = detail?.availableStock ?? adjustForm.availableStock
    }).catch(() => {})
  }
}

async function submitAdjust() {
  if (!adjustForm.goodsId) return
  if (!adjustForm.adjustQty) {
    showError('请填写调整数量（正数入库，负数出库）')
    return
  }
  if (!adjustForm.reason?.trim()) {
    showError('请填写调整原因')
    return
  }
  const action = adjustForm.adjustQty > 0 ? '入库' : '出库'
  await confirmAction(`确认对「${adjustForm.goodsName}」${action} ${Math.abs(adjustForm.adjustQty)} 件吗？`)
  adjustLoading.value = true
  try {
    await adjustRentStock({
      goodsId: adjustForm.goodsId,
      adjustQty: adjustForm.adjustQty,
      reason: adjustForm.reason.trim(),
    })
    showSuccess('库存已调整')
    adjustVisible.value = false
    await loadData()
  } catch (error) {
    showError(error?.msg || error?.message || '库存调整失败')
  } finally {
    adjustLoading.value = false
  }
}

onMounted(async () => {
  await loadCategoryOptions()
  await loadData()
})
</script>

<template>
  <el-card shadow="never" class="page-card">
    <template #header><strong>库存管理</strong></template>

    <el-form inline class="filter-form" @submit.prevent="handleSearch">
      <el-form-item label="商品名称">
        <el-input
          v-model="filters.keyword"
          clearable
          placeholder="商品ID / 名称"
          style="width: 200px"
          @keyup.enter="handleSearch"
        />
      </el-form-item>
      <el-form-item label="分类">
        <el-select v-model="filters.categoryId" clearable placeholder="全部分类" style="width: 180px">
          <el-option
            v-for="item in categoryOptions"
            :key="item.id"
            :label="item.label"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="tableData" border empty-text="暂无库存数据">
      <el-table-column prop="id" label="ID" width="90" show-overflow-tooltip />
      <el-table-column label="商品名称" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">{{ row.goodsName || row.name || '-' }}</template>
      </el-table-column>
      <el-table-column prop="categoryName" label="分类" min-width="120" show-overflow-tooltip />
      <el-table-column label="总库存" width="90">
        <template #default="{ row }">{{ row.totalStock ?? '-' }}</template>
      </el-table-column>
      <el-table-column label="可用库存" width="90">
        <template #default="{ row }">{{ row.availableStock ?? '-' }}</template>
      </el-table-column>
      <el-table-column label="占用库存" width="90">
        <template #default="{ row }">
          {{ row.occupiedStock != null ? row.occupiedStock : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <div class="table-actions">
            <el-button size="small" type="primary" @click="openOccupyQuery(row)">占用</el-button>
            <el-button size="small" type="warning" @click="openAdjust(row)">调整</el-button>
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

  <el-dialog v-model="occupyVisible" title="时段占用查询" width="520px" destroy-on-close>
    <el-form label-width="96px">
      <el-form-item label="商品">
        <el-input :model-value="occupyForm.goodsName" disabled />
      </el-form-item>
      <el-form-item label="租期开始" required>
        <el-date-picker
          v-model="occupyForm.startTime"
          type="datetime"
          value-format="YYYY-MM-DD HH:mm:ss"
          placeholder="开始时间"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="租期结束" required>
        <el-date-picker
          v-model="occupyForm.endTime"
          type="datetime"
          value-format="YYYY-MM-DD HH:mm:ss"
          placeholder="结束时间"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="租赁数量" required>
        <el-input-number v-model="occupyForm.quantity" :min="1" />
      </el-form-item>
    </el-form>
    <el-alert
      v-if="occupyResult"
      :title="occupyResult.available ? '该时段可租' : '该时段库存不足'"
      :type="occupyResult.available ? 'success' : 'warning'"
      :description="`剩余可租数量：${occupyResult.remainStock ?? 0}`"
      show-icon
      :closable="false"
      class="occupy-result"
    />
    <template #footer>
      <el-button @click="occupyVisible = false">关闭</el-button>
      <el-button type="primary" :loading="occupyLoading" @click="submitOccupyQuery">查询</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="adjustVisible" title="手动调整库存" width="480px" destroy-on-close>
    <el-form label-width="96px">
      <el-form-item label="商品">
        <el-input :model-value="adjustForm.goodsName" disabled />
      </el-form-item>
      <el-form-item label="当前总库存">
        <el-input :model-value="String(adjustForm.totalStock)" disabled />
      </el-form-item>
      <el-form-item label="当前可用">
        <el-input :model-value="String(adjustForm.availableStock)" disabled />
      </el-form-item>
      <el-form-item label="调整数量" required>
        <el-input-number v-model="adjustForm.adjustQty" :step="1" />
        <p class="form-tip">正数入库，负数出库</p>
      </el-form-item>
      <el-form-item label="调整原因" required>
        <el-input
          v-model="adjustForm.reason"
          type="textarea"
          :rows="3"
          maxlength="200"
          show-word-limit
          placeholder="如：盘点入库、损坏出库"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="adjustVisible = false">取消</el-button>
      <el-button type="primary" :loading="adjustLoading" @click="submitAdjust">确认调整</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.filter-form {
  margin-bottom: 12px;
}

.occupy-result {
  margin-top: 8px;
}

.form-tip {
  margin: 4px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
</style>
