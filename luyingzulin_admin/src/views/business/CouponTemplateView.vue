<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import AppDialog from '@/components/common/AppDialog.vue'
import BasePagination from '@/components/common/BasePagination.vue'
import ListFilterSummary from '@/components/common/ListFilterSummary.vue'
import {
  deleteCouponTemplate,
  getCouponTemplateInfo,
  getCouponTemplatePage,
  grantCoupon,
  saveCouponTemplate,
  updateCouponTemplate,
} from '@/api/modules/coupon-template'
import { formatMoney } from '@/utils/rentOrder'
import { validateForm } from '@/utils/form'
import { buildFilterSummary, filterParam } from '@/utils/listFilter'
import {
  COUPON_TEMPLATE_STATUS_OPTIONS,
  COUPON_TYPE_OPTIONS,
  getCouponTypeLabel,
  parseGrantPhones,
} from '@/utils/userAssets'
import StatusSwitch from '@/components/common/StatusSwitch.vue'
import { confirmAction, showError, showSuccess } from '@/utils/modal'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const filters = reactive({
  keyword: '',
  couponType: null,
  status: null,
})

const filterSummaryItems = computed(() => buildFilterSummary([
  { key: 'keyword', label: '名称', value: filters.keyword },
  { key: 'couponType', label: '类型', value: filters.couponType, options: COUPON_TYPE_OPTIONS },
  { key: 'status', label: '状态', value: filters.status, options: COUPON_TEMPLATE_STATUS_OPTIONS },
]))

const formVisible = ref(false)
const formLoading = ref(false)
const formRef = ref()
const editingId = ref(null)
const form = reactive({
  name: '',
  couponType: 'amount_off',
  amount: 0,
  minAmount: 0,
  discountRate: null,
  validStart: '',
  validEnd: '',
  scopeType: 'all',
  scopeValue: '',
  status: 1,
  remark: '',
})

const formRules = {
  name: [{ required: true, message: '请输入优惠券名称', trigger: 'blur' }],
  couponType: [{ required: true, message: '请选择类型', trigger: 'change' }],
  amount: [{ required: true, message: '请输入面额', trigger: 'blur' }],
}

const grantVisible = ref(false)
const grantLoading = ref(false)
const grantForm = reactive({
  templateId: null,
  templateName: '',
  phonesText: '',
})

async function loadData() {
  loading.value = true
  try {
    const data = await getCouponTemplatePage({
      page: pageNum.value,
      limit: pageSize.value,
      keyword: filters.keyword?.trim() || undefined,
      couponType: filterParam(filters.couponType),
      status: filterParam(filters.status),
    })
    tableData.value = data.list || []
    total.value = data.total
  } catch (error) {
    tableData.value = []
    total.value = 0
    showError(error?.msg || error?.message || '加载优惠券模板失败')
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
  filters.couponType = null
  filters.status = null
  pageNum.value = 1
  loadData()
}

function resetForm() {
  Object.assign(form, {
    name: '',
    couponType: 'amount_off',
    amount: 0,
    minAmount: 0,
    discountRate: null,
    validStart: '',
    validEnd: '',
    scopeType: 'all',
    scopeValue: '',
    status: 1,
    remark: '',
  })
}

function openCreate() {
  editingId.value = null
  resetForm()
  formVisible.value = true
}

async function openEdit(row) {
  editingId.value = row.id
  formVisible.value = true
  formLoading.value = true
  try {
    const data = await getCouponTemplateInfo(row.id)
    Object.assign(form, {
      name: data.name || '',
      couponType: data.couponType || 'amount_off',
      amount: Number(data.amount || 0),
      minAmount: Number(data.minAmount || 0),
      discountRate: data.discountRate != null ? Number(data.discountRate) : null,
      validStart: data.validStart || '',
      validEnd: data.validEnd || '',
      scopeType: data.scopeType || 'all',
      scopeValue: data.scopeValue || '',
      status: data.status ?? 1,
      remark: data.remark || '',
    })
  } catch (error) {
    showError(error?.msg || error?.message || '加载模板详情失败')
    formVisible.value = false
  } finally {
    formLoading.value = false
  }
}

async function submitForm() {
  const valid = await validateForm(formRef)
  if (!valid) return
  formLoading.value = true
  try {
    const payload = {
      ...form,
      id: editingId.value || undefined,
      minAmount: form.minAmount || 0,
      discountRate: form.couponType === 'discount' ? form.discountRate : undefined,
    }
    if (editingId.value) {
      await updateCouponTemplate(payload)
      showSuccess('模板已更新')
    } else {
      await saveCouponTemplate(payload)
      showSuccess('模板已创建')
    }
    formVisible.value = false
    await loadData()
  } catch (error) {
    showError(error?.msg || error?.message || '保存失败')
  } finally {
    formLoading.value = false
  }
}

async function handleDelete(row) {
  await confirmAction(`确认删除模板「${row.name}」吗？`)
  try {
    await deleteCouponTemplate(row.id)
    showSuccess('已删除')
    await loadData()
  } catch (error) {
    showError(error?.msg || error?.message || '删除失败')
  }
}

function openGrant(row) {
  grantForm.templateId = row.id
  grantForm.templateName = row.name
  grantForm.phonesText = ''
  grantVisible.value = true
}

async function submitGrant() {
  const phones = parseGrantPhones(grantForm.phonesText)
  if (!phones.length) {
    showError('请输入至少一个手机号')
    return
  }
  grantLoading.value = true
  try {
    const result = await grantCoupon({ templateId: grantForm.templateId, phones })
    showSuccess(`已发放 ${result?.grantCount ?? phones.length} 张优惠券`)
    grantVisible.value = false
  } catch (error) {
    showError(error?.msg || error?.message || '发放失败')
  } finally {
    grantLoading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <el-card shadow="never" class="page-card">
    <template #header>
      <div class="card-header">
        <strong>优惠券模板</strong>
        <el-button type="primary" @click="openCreate">新增模板</el-button>
      </div>
    </template>

    <el-form :inline="true" class="filter-form" @submit.prevent="handleSearch">
      <el-form-item label="名称">
        <el-input v-model="filters.keyword" clearable placeholder="ID / 模板名称" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="类型">
        <el-select v-model="filters.couponType" clearable placeholder="全部">
          <el-option v-for="item in COUPON_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="filters.status" clearable placeholder="全部">
          <el-option v-for="item in COUPON_TEMPLATE_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <ListFilterSummary :items="filterSummaryItems" />

    <el-table v-loading="loading" :data="tableData" border empty-text="暂无优惠券模板">
      <el-table-column prop="id" label="ID" width="90" />
      <el-table-column prop="name" label="名称" min-width="140" />
      <el-table-column label="类型" width="110">
        <template #default="{ row }">{{ row.couponTypeLabel || getCouponTypeLabel(row.couponType) }}</template>
      </el-table-column>
      <el-table-column label="面额/折扣" width="120">
        <template #default="{ row }">
          <span v-if="row.couponType === 'discount'">{{ row.discountRate }}折</span>
          <span v-else>{{ formatMoney(row.amount) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="门槛" width="100">
        <template #default="{ row }">{{ formatMoney(row.minAmount) }}</template>
      </el-table-column>
      <el-table-column prop="validStart" label="有效期起" min-width="170" />
      <el-table-column prop="validEnd" label="有效期止" min-width="170" />
      <el-table-column prop="scopeType" label="适用范围" width="100" />
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
      <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
      <el-table-column prop="createTime" label="创建时间" min-width="170" />
      <el-table-column label="操作" min-width="220" fixed="right">
        <template #default="{ row }">
          <div class="table-actions">
            <el-button size="small" type="warning" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="success" @click="openGrant(row)">发放</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
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

  <AppDialog
    v-model="formVisible"
    :title="editingId ? '编辑优惠券模板' : '新增优惠券模板'"
    width="720px"
    :loading="formLoading"
    @confirm="submitForm"
  >
    <el-form ref="formRef" v-loading="formLoading" :model="form" :rules="formRules" label-width="110px">
      <el-form-item label="名称" prop="name">
        <el-input v-model="form.name" placeholder="优惠券名称" />
      </el-form-item>
      <el-form-item label="类型" prop="couponType">
        <el-select v-model="form.couponType">
          <el-option v-for="item in COUPON_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="form.couponType === 'discount'" label="折扣率">
        <el-input-number v-model="form.discountRate" :min="0.1" :max="9.9" :precision="1" :step="0.1" />
      </el-form-item>
      <el-form-item v-else label="面额" prop="amount">
        <el-input-number v-model="form.amount" :min="0" :precision="2" />
      </el-form-item>
      <el-form-item label="最低消费">
        <el-input-number v-model="form.minAmount" :min="0" :precision="2" />
      </el-form-item>
      <el-form-item label="有效期起">
        <el-date-picker v-model="form.validStart" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="开始时间" />
      </el-form-item>
      <el-form-item label="有效期止">
        <el-date-picker v-model="form.validEnd" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="结束时间" />
      </el-form-item>
      <el-form-item label="适用范围">
        <el-select v-model="form.scopeType">
          <el-option label="全场" value="all" />
          <el-option label="指定分类" value="category" />
          <el-option label="指定商品" value="goods" />
        </el-select>
      </el-form-item>
      <el-form-item label="范围值">
        <el-input v-model="form.scopeValue" placeholder="分类/商品ID，多个逗号分隔" />
      </el-form-item>
      <el-form-item label="状态">
        <el-radio-group v-model="form.status">
          <el-radio v-for="item in COUPON_TEMPLATE_STATUS_OPTIONS" :key="item.value" :value="item.value">{{ item.label }}</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="form.remark" type="textarea" :rows="3" />
      </el-form-item>
    </el-form>
  </AppDialog>

  <AppDialog v-model="grantVisible" title="发放优惠券" width="520px" :loading="grantLoading" @confirm="submitGrant">
    <el-form label-width="100px">
      <el-form-item label="模板">
        <el-input :model-value="grantForm.templateName" disabled />
      </el-form-item>
      <el-form-item label="手机号">
        <el-input
          v-model="grantForm.phonesText"
          type="textarea"
          :rows="5"
          placeholder="每行一个手机号，或用逗号分隔"
        />
      </el-form-item>
    </el-form>
  </AppDialog>
</template>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.filter-form {
  margin-bottom: 12px;
}
</style>
