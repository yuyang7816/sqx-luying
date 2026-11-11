<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import AppDialog from '@/components/common/AppDialog.vue'
import BasePagination from '@/components/common/BasePagination.vue'
import ListFilterSummary from '@/components/common/ListFilterSummary.vue'
import {
  getUserFeedbackInfo,
  getUserFeedbackPage,
  processUserFeedback,
} from '@/api/modules/user-feedback'
import { resolveUserPhone } from '@/utils/appUser'
import { validateForm } from '@/utils/form'
import { buildFilterSummary, filterParam } from '@/utils/listFilter'
import {
  FEEDBACK_STATUS_OPTIONS,
  FEEDBACK_TYPE_OPTIONS,
  getFeedbackTypeLabel,
} from '@/utils/userAssets'
import LinkUser from '@/components/common/LinkUser.vue'
import StatusSwitch from '@/components/common/StatusSwitch.vue'
import { FEEDBACK_SWITCH_ON_STATUSES } from '@/utils/statusSwitch'
import { showError, showSuccess } from '@/utils/modal'
import { useMenuBadgeStore } from '@/stores/menuBadge'

const menuBadgeStore = useMenuBadgeStore()
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const filters = reactive({
  keyword: '',
  phone: '',
  feedbackType: null,
  status: null,
})

const filterSummaryItems = computed(() => buildFilterSummary([
  { key: 'keyword', label: '关键词', value: filters.keyword },
  { key: 'phone', label: '手机号', value: filters.phone },
  { key: 'feedbackType', label: '类型', value: filters.feedbackType, options: FEEDBACK_TYPE_OPTIONS },
  { key: 'status', label: '状态', value: filters.status, options: FEEDBACK_STATUS_OPTIONS },
]))

const detailVisible = ref(false)
const detailLoading = ref(false)
const detailData = ref(null)

const processVisible = ref(false)
const processLoading = ref(false)
const processFormRef = ref()
const processForm = reactive({
  id: null,
  replyRemark: '',
})
const processRules = {
  replyRemark: [{ required: true, message: '请输入处理备注', trigger: 'blur' }],
}

async function loadData() {
  loading.value = true
  try {
    const data = await getUserFeedbackPage({
      page: pageNum.value,
      limit: pageSize.value,
      keyword: filters.keyword?.trim() || undefined,
      phone: filters.phone?.trim() || undefined,
      feedbackType: filterParam(filters.feedbackType),
      status: filterParam(filters.status),
    })
    tableData.value = data.list || []
    total.value = data.total
  } catch (error) {
    tableData.value = []
    total.value = 0
    showError(error?.msg || error?.message || '加载反馈列表失败')
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
  filters.phone = ''
  filters.feedbackType = null
  filters.status = null
  pageNum.value = 1
  loadData()
}

async function openDetail(row) {
  detailVisible.value = true
  detailLoading.value = true
  detailData.value = null
  try {
    detailData.value = await getUserFeedbackInfo(row.id)
  } catch (error) {
    showError(error?.msg || error?.message || '加载反馈详情失败')
    detailVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

function openProcess(row) {
  processForm.id = row.id
  processForm.replyRemark = ''
  processVisible.value = true
}

async function submitProcess() {
  const valid = await validateForm(processFormRef)
  if (!valid) return
  processLoading.value = true
  try {
    await processUserFeedback({
      id: processForm.id,
      replyRemark: processForm.replyRemark.trim(),
    })
    showSuccess('反馈已处理')
    processVisible.value = false
    detailVisible.value = false
    await loadData()
    void menuBadgeStore.refresh()
  } catch (error) {
    showError(error?.msg || error?.message || '处理失败')
  } finally {
    processLoading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <el-card shadow="never" class="page-card">
    <template #header>
      <strong>用户反馈</strong>
    </template>

    <el-form :inline="true" class="filter-form" @submit.prevent="handleSearch">
      <el-form-item label="关键词">
        <el-input v-model="filters.keyword" clearable placeholder="ID / 内容" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="手机号">
        <el-input v-model="filters.phone" clearable placeholder="用户手机号" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="类型">
        <el-select v-model="filters.feedbackType" clearable placeholder="全部">
          <el-option v-for="item in FEEDBACK_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="filters.status" clearable placeholder="全部">
          <el-option v-for="item in FEEDBACK_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <ListFilterSummary :items="filterSummaryItems" />

    <el-table v-loading="loading" :data="tableData" border empty-text="暂无反馈">
      <el-table-column prop="id" label="ID" width="90" />
      <el-table-column label="手机号" width="130">
        <template #default="{ row }">
          <LinkUser :user-id="row.userId" :phone="resolveUserPhone(row)" />
        </template>
      </el-table-column>
      <el-table-column prop="userName" label="用户名" min-width="110">
        <template #default="{ row }">
          <LinkUser :user-id="row.userId" :label="row.userName || '-'" />
        </template>
      </el-table-column>
      <el-table-column label="类型" width="110">
        <template #default="{ row }">{{ row.feedbackTypeLabel || getFeedbackTypeLabel(row.feedbackType) }}</template>
      </el-table-column>
      <el-table-column prop="content" label="反馈内容" min-width="200" show-overflow-tooltip />
      <el-table-column prop="contactPhone" label="联系电话" width="130" />
      <el-table-column label="状态" width="80" align="center">
        <template #default="{ row }">
          <StatusSwitch
            :model-value="row.status"
            :active-values="FEEDBACK_SWITCH_ON_STATUSES"
            readonly
          />
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="提交时间" min-width="170" />
      <el-table-column label="操作" min-width="160" fixed="right">
        <template #default="{ row }">
          <div class="table-actions">
            <el-button size="small" type="primary" @click="openDetail(row)">详情</el-button>
            <el-button v-if="row.status === 'pending'" size="small" type="success" @click="openProcess(row)">
              处理
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

  <el-dialog v-model="detailVisible" title="反馈详情" width="720px" destroy-on-close>
    <div v-loading="detailLoading">
      <el-descriptions v-if="detailData" :column="2" border>
        <el-descriptions-item label="ID">{{ detailData.id }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <StatusSwitch
            :model-value="detailData.status"
            :active-values="FEEDBACK_SWITCH_ON_STATUSES"
            readonly
          />
        </el-descriptions-item>
        <el-descriptions-item label="用户">
          <LinkUser :user-id="detailData.userId" :label="detailData.userName || '-'" />
        </el-descriptions-item>
        <el-descriptions-item label="手机号">
          <LinkUser :user-id="detailData.userId" :phone="resolveUserPhone(detailData)" />
        </el-descriptions-item>
        <el-descriptions-item label="反馈类型">
          {{ detailData.feedbackTypeLabel || getFeedbackTypeLabel(detailData.feedbackType) }}
        </el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ detailData.contactPhone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="提交时间" :span="2">{{ detailData.createTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="反馈内容" :span="2">{{ detailData.content || '-' }}</el-descriptions-item>
        <el-descriptions-item v-if="detailData.replyRemark" label="处理备注" :span="2">
          {{ detailData.replyRemark }}
        </el-descriptions-item>
        <el-descriptions-item v-if="detailData.processTime" label="处理时间" :span="2">
          {{ detailData.processTime }}
        </el-descriptions-item>
      </el-descriptions>
      <div v-if="detailData?.imageUrls?.length" class="image-list">
        <div class="image-title">反馈图片</div>
        <el-image
          v-for="(url, index) in detailData.imageUrls"
          :key="`${url}-${index}`"
          :src="url"
          :preview-src-list="detailData.imageUrls"
          fit="cover"
          class="feedback-image"
        />
      </div>
    </div>
    <template #footer>
      <el-button @click="detailVisible = false">关闭</el-button>
      <el-button
        v-if="detailData?.status === 'pending'"
        type="primary"
        @click="openProcess(detailData)"
      >
        处理
      </el-button>
    </template>
  </el-dialog>

  <AppDialog v-model="processVisible" title="处理反馈" width="520px" :loading="processLoading" @confirm="submitProcess">
    <el-form ref="processFormRef" :model="processForm" :rules="processRules" label-width="100px">
      <el-form-item label="反馈ID">
        <el-input :model-value="processForm.id" disabled />
      </el-form-item>
      <el-form-item label="处理备注" prop="replyRemark">
        <el-input v-model="processForm.replyRemark" type="textarea" :rows="4" placeholder="填写处理结果或回复内容" />
      </el-form-item>
    </el-form>
  </AppDialog>
</template>

<style scoped>
.filter-form {
  margin-bottom: 12px;
}
.image-list {
  margin-top: 16px;
}
.image-title {
  margin-bottom: 8px;
  font-weight: 600;
}
.feedback-image {
  width: 96px;
  height: 96px;
  margin-right: 8px;
  border-radius: 4px;
}
</style>
