<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppDialog from '@/components/common/AppDialog.vue'
import BasePagination from '@/components/common/BasePagination.vue'
import ListFilterSummary from '@/components/common/ListFilterSummary.vue'
import {
  deleteAppUserApi,
  fetchUserOrderStats,
  getAppUserInfo,
  getAppUserPage,
  updateAppUserApi,
} from '@/api/modules/app-user'
import { updateUserWalletAmountApi, userWalletApi } from '@/api/modules/user-wallet'
import { adjustUserDeposit, getUserDepositPage } from '@/api/modules/user-deposit'
import { getReferralUserCommissionInfo, getReferralUserData, getReferralUserInviteList, updateReferralUserCommissionApi } from '@/api/modules/referral-user-commission'
import {
  buildAppUserListParams,
  formatUserSex,
  resolveUserDisplayName,
  resolveUserPhone,
  resolveUserAvatar,
} from '@/utils/appUser'
import { buildFilterSummary } from '@/utils/listFilter'
import { validateForm } from '@/utils/form'
import { formatMoney } from '@/utils/rentOrder'
import { isAuth } from '@/utils/tool'
import { confirmAction, showError, showSuccess } from '@/utils/modal'

import LinkUser from '@/components/common/LinkUser.vue'
import StatusSwitch from '@/components/common/StatusSwitch.vue'

const router = useRouter()
const route = useRoute()
const canViewUserOrders = isAuth('rental:order:list')

const loading = ref(false)
const statsLoading = ref(false)
const editDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const editDialogLoading = ref(false)
const detailDialogLoading = ref(false)
const formRef = ref()
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const editingRow = ref(null)
const detailData = ref(null)
const detailOrderStats = ref({ orderCount: 0, depositAmount: 0 })
const detailAssets = ref({
  walletAmount: 0,
  depositTotal: 0,
  depositAvailable: 0,
  depositFrozen: 0,
  referralCommission: 0,
  inviteCount: 0,
  commissionId: null,
})
const inviteList = ref([])

const commissionAdjustVisible = ref(false)
const commissionAdjustLoading = ref(false)
const commissionAdjustForm = reactive({
  id: null,
  userId: null,
  userLabel: '',
  commission: 0,
})

const walletAdjustVisible = ref(false)
const walletAdjustLoading = ref(false)
const walletAdjustForm = reactive({
  userId: null,
  userLabel: '',
  amount: 0,
  direction: 1,
})

const depositAdjustVisible = ref(false)
const depositAdjustLoading = ref(false)
const depositAdjustFormRef = ref()
const depositAdjustForm = reactive({
  userId: null,
  userName: '',
  phone: '',
  totalAmount: 0,
  availableAmount: 0,
  amount: 0,
  direction: 1,
  remark: '',
})
const depositAdjustRules = {
  amount: [{ required: true, message: '请输入调整金额', trigger: 'blur' }],
}

const filters = reactive({
  username: '',
  phone: '',
  status: null,
  platform: '',
  invitationCode: '',
  createTimeStart: '',
  createTimeEnd: '',
})

const filterSummaryItems = computed(() => buildFilterSummary([
  { key: 'username', label: '用户名', value: filters.username },
  { key: 'phone', label: '手机号', value: filters.phone },
  {
    key: 'status',
    label: '状态',
    value: filters.status,
    options: [{ label: '正常', value: 1 }, { label: '禁用', value: 2 }],
  },
  { key: 'platform', label: '注册来源', value: filters.platform },
  { key: 'invitationCode', label: '邀请码', value: filters.invitationCode },
  {
    key: 'createTimeStart',
    label: '注册开始',
    value: filters.createTimeStart,
  },
  {
    key: 'createTimeEnd',
    label: '注册结束',
    value: filters.createTimeEnd,
  },
]))

const form = reactive({
  userId: null,
  userName: '',
  phone: '',
  status: 1,
})

const rules = {
  userName: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
}

function buildListParams() {
  return buildAppUserListParams(filters, pageNum.value, pageSize.value)
}

function formatInviteeColumn(row = {}) {
  const count = Number(row.inviteCount || 0)
  if (count <= 0) return '-'
  const summary = String(row.inviteeSummary || '').trim()
  if (!summary) return `${count}人`
  return `${count}人（${summary}${count > 3 ? '…' : ''}）`
}

function formatInviteeTooltip(row = {}) {
  const count = Number(row.inviteCount || 0)
  const summary = String(row.inviteeSummary || '').trim()
  if (!summary) return `${count}人`
  if (count > 3) return `${summary} 等${count}人`
  return summary
}

async function enrichOrderStats(rows = []) {
  if (!rows.length) return
  statsLoading.value = true
  try {
    const enriched = await Promise.all(
      rows.map(async (row) => {
        const stats = await fetchUserOrderStats(row.userName || resolveUserPhone(row))
        return { ...row, orderCount: stats.orderCount, depositAmount: stats.depositAmount }
      }),
    )
    tableData.value = enriched
  } finally {
    statsLoading.value = false
  }
}

async function loadData() {
  loading.value = true
  try {
    const data = await getAppUserPage(buildListParams())
    tableData.value = data.list || []
    total.value = data.total
    await enrichOrderStats(tableData.value)
  } catch (error) {
    tableData.value = []
    total.value = 0
    showError(error?.msg || error?.message || '加载用户列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pageNum.value = 1
  loadData()
}

function handleReset() {
  Object.assign(filters, {
    username: '',
    phone: '',
    status: null,
    platform: '',
    invitationCode: '',
    createTimeStart: '',
    createTimeEnd: '',
  })
  pageNum.value = 1
  loadData()
}

async function loadDetailAssets(userId, phone) {
  detailAssets.value = {
    walletAmount: 0,
    depositTotal: 0,
    depositAvailable: 0,
    depositFrozen: 0,
    referralCommission: 0,
    inviteCount: 0,
    commissionId: null,
  }
  inviteList.value = []
  const [walletRes, depositRes, referralRes, referralDataRes] = await Promise.allSettled([
    userWalletApi.page({ page: 1, limit: 1, userId }),
    getUserDepositPage({ page: 1, limit: 1, phone: phone || undefined }),
    getReferralUserCommissionInfo(userId),
    getReferralUserData(userId),
  ])
  if (walletRes.status === 'fulfilled') {
    detailAssets.value.walletAmount = Number(walletRes.value.list?.[0]?.amount || 0)
  }
  if (depositRes.status === 'fulfilled') {
    const deposit = depositRes.value.list?.[0] || {}
    detailAssets.value.depositTotal = Number(deposit.totalAmount || 0)
    detailAssets.value.depositAvailable = Number(deposit.availableAmount || 0)
    detailAssets.value.depositFrozen = Number(deposit.frozenAmount || 0)
  }
  if (referralRes.status === 'fulfilled') {
    detailAssets.value.referralCommission = Number(referralRes.value?.commission || 0)
    detailAssets.value.commissionId = referralRes.value?.id ?? null
  }
  if (referralDataRes.status === 'fulfilled') {
    detailAssets.value.inviteCount = Number(referralDataRes.value?.num || 0)
    if (!detailAssets.value.referralCommission) {
      detailAssets.value.referralCommission = Number(referralDataRes.value?.commission || 0)
    }
  }
}

async function fillDetail(userId, extra = {}) {
  const info = await getAppUserInfo(userId)
  const stats = await fetchUserOrderStats(info.userName || resolveUserPhone(info))
  detailData.value = { ...info, ...extra, status: extra.status ?? info.status ?? 1 }
  detailOrderStats.value = stats
  await loadDetailAssets(userId, resolveUserPhone(info))
  inviteList.value = []
  if (info.invitationCode) {
    try {
      const invitePage = await getReferralUserInviteList(userId, { page: 1, limit: 20 })
      inviteList.value = invitePage.list || []
    } catch {
      inviteList.value = []
    }
  }
}

async function openDetailByUserId(userId) {
  const id = Number(userId)
  if (!id) return
  detailDialogVisible.value = true
  detailDialogLoading.value = true
  detailData.value = null
  detailOrderStats.value = { orderCount: 0, depositAmount: 0 }
  try {
    await fillDetail(id)
  } catch (error) {
    showError(error?.msg || error?.message || '加载用户详情失败')
    detailDialogVisible.value = false
  } finally {
    detailDialogLoading.value = false
  }
}

async function handleUserStatusChange(row, nextStatus) {
  const actionLabel = nextStatus === 1 ? '启用' : '禁用'
  try {
    await confirmAction(`确认${actionLabel}用户「${resolveUserDisplayName(row)}」吗？`)
    await updateAppUserApi({
      userId: row.userId,
      userName: row.userName,
      status: nextStatus,
    })
    row.status = nextStatus
    showSuccess('用户状态已更新')
  } catch (error) {
    if (error?.msg || error?.message) {
      showError(error.msg || error.message)
    }
  }
}

async function openDetail(row) {
  detailDialogVisible.value = true
  detailDialogLoading.value = true
  detailData.value = null
  detailOrderStats.value = { orderCount: 0, depositAmount: 0 }
  try {
    await fillDetail(row.userId, { status: row.status, statusLabel: row.statusLabel })
  } catch (error) {
    showError(error?.msg || error?.message || '加载用户详情失败')
    detailDialogVisible.value = false
  } finally {
    detailDialogLoading.value = false
  }
}

async function openEdit(row) {
  editingRow.value = row
  editDialogVisible.value = true
  editDialogLoading.value = true
  try {
    const data = await getAppUserInfo(row.userId)
    Object.assign(form, {
      userId: data.userId,
      userName: data.userName,
      phone: data.phone || resolveUserPhone(row),
      status: row.status ?? data.status ?? 1,
    })
  } catch (error) {
    showError(error?.msg || error?.message || '加载用户失败')
    editDialogVisible.value = false
  } finally {
    editDialogLoading.value = false
  }
}

async function handleSubmit() {
  const valid = await validateForm(formRef)
  if (!valid) return
  editDialogLoading.value = true
  try {
    await updateAppUserApi({
      userId: form.userId,
      userName: form.userName,
      status: form.status,
    })
    showSuccess('用户已更新')
    editDialogVisible.value = false
    loadData()
  } finally {
    editDialogLoading.value = false
  }
}

async function handleDelete(row) {
  await confirmAction(`确认删除用户「${resolveUserDisplayName(row)}」吗？`)
  await deleteAppUserApi({ userId: row.userId })
  showSuccess('用户已删除')
  loadData()
}

function goUserOrders(row) {
  router.push({
    name: 'rental-orders',
    query: { keyword: row.userName || '' },
  })
}

function goCommissions(row) {
  router.push({
    path: '/business/referral-commissions',
    query: { userId: row.userId, phone: resolveUserPhone(row) || undefined },
  })
}

function openCommissionAdjust() {
  if (!detailData.value) return
  Object.assign(commissionAdjustForm, {
    id: detailAssets.value.commissionId,
    userId: detailData.value.userId,
    userLabel: `${detailData.value.userName || '-'}（${resolveUserPhone(detailData.value)}）`,
    commission: Number(detailAssets.value.referralCommission || 0),
  })
  commissionAdjustVisible.value = true
}

async function submitCommissionAdjust() {
  commissionAdjustLoading.value = true
  try {
    await updateReferralUserCommissionApi({
      id: commissionAdjustForm.id,
      userId: commissionAdjustForm.userId,
      commission: commissionAdjustForm.commission,
    })
    showSuccess('佣金余额已更新')
    commissionAdjustVisible.value = false
    detailAssets.value.referralCommission = commissionAdjustForm.commission
    await loadData()
  } catch (error) {
    showError(error?.msg || error?.message || '更新失败')
  } finally {
    commissionAdjustLoading.value = false
  }
}

function goAssetPage(path, userId, phone) {
  detailDialogVisible.value = false
  router.push({
    path,
    query: { userId, phone: phone || undefined },
  })
}

function openWalletAdjust() {
  if (!detailData.value) return
  Object.assign(walletAdjustForm, {
    userId: detailData.value.userId,
    userLabel: `${detailData.value.userName || '-'}（${resolveUserPhone(detailData.value)}）`,
    amount: 0,
    direction: 1,
  })
  walletAdjustVisible.value = true
}

async function submitWalletAdjust() {
  if (!walletAdjustForm.amount || walletAdjustForm.amount <= 0) {
    showError('调整金额必须大于 0')
    return
  }
  walletAdjustLoading.value = true
  try {
    await updateUserWalletAmountApi({
      userId: walletAdjustForm.userId,
      operator: walletAdjustForm.direction === 2 ? 'decr' : 'incr',
      amount: walletAdjustForm.amount,
    })
    showSuccess('余额已调整')
    walletAdjustVisible.value = false
    await loadDetailAssets(walletAdjustForm.userId, resolveUserPhone(detailData.value))
  } catch (error) {
    showError(error?.msg || error?.message || '调额失败')
  } finally {
    walletAdjustLoading.value = false
  }
}

async function openDepositAdjust() {
  if (!detailData.value) return
  try {
    const data = await getUserDepositPage({
      page: 1,
      limit: 1,
      userId: detailData.value.userId,
    })
    const row = data.list?.[0] || {}
    Object.assign(depositAdjustForm, {
      userId: detailData.value.userId,
      userName: detailData.value.userName || '-',
      phone: resolveUserPhone(detailData.value),
      totalAmount: Number(row.totalAmount || detailAssets.value.depositAvailable || 0),
      availableAmount: Number(row.availableAmount || detailAssets.value.depositAvailable || 0),
      amount: 0,
      direction: 1,
      remark: '',
    })
    depositAdjustVisible.value = true
  } catch (error) {
    showError(error?.msg || error?.message || '加载押金账户失败')
  }
}

async function submitDepositAdjust() {
  const valid = await validateForm(depositAdjustFormRef)
  if (!valid) return
  if (!depositAdjustForm.amount || depositAdjustForm.amount <= 0) {
    showError('调整金额必须大于 0')
    return
  }
  if (depositAdjustForm.direction === 2 && depositAdjustForm.amount > depositAdjustForm.availableAmount) {
    showError('扣减金额不能超过可用押金')
    return
  }
  depositAdjustLoading.value = true
  try {
    await adjustUserDeposit({
      userId: depositAdjustForm.userId,
      amount: depositAdjustForm.amount,
      direction: depositAdjustForm.direction,
      remark: depositAdjustForm.remark?.trim() || undefined,
    })
    showSuccess('押金已调整')
    depositAdjustVisible.value = false
    await loadDetailAssets(depositAdjustForm.userId, resolveUserPhone(detailData.value))
  } catch (error) {
    showError(error?.msg || error?.message || '调押失败')
  } finally {
    depositAdjustLoading.value = false
  }
}

onMounted(loadData)

watch(
  () => route.query.userId,
  (userId) => {
    if (userId) {
      openDetailByUserId(userId)
    }
  },
  { immediate: true },
)
</script>

<template>
  <el-card shadow="never" class="page-card">
    <template #header>
      <div class="card-header">
        <strong>C端用户</strong>
      </div>
    </template>

    <el-form :inline="true" class="search-grid">
      <el-form-item label="用户名">
        <el-input v-model="filters.username" clearable placeholder="用户名" />
      </el-form-item>
      <el-form-item label="手机号">
        <el-input v-model="filters.phone" clearable placeholder="手机号" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="filters.status" clearable placeholder="全部">
          <el-option label="正常" :value="1" />
          <el-option label="禁用" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item label="注册来源">
        <el-input v-model="filters.platform" clearable placeholder="app/小程序/公众号" />
      </el-form-item>
      <el-form-item label="邀请码">
        <el-input v-model="filters.invitationCode" clearable placeholder="邀请码" />
      </el-form-item>
      <el-form-item label="注册时间">
        <el-date-picker
          v-model="filters.createTimeStart"
          type="datetime"
          value-format="YYYY-MM-DD HH:mm:ss"
          placeholder="开始时间"
          style="width: 190px"
        />
        <span class="range-sep">至</span>
        <el-date-picker
          v-model="filters.createTimeEnd"
          type="datetime"
          value-format="YYYY-MM-DD HH:mm:ss"
          placeholder="结束时间"
          style="width: 190px"
        />
      </el-form-item>
      <el-form-item>
        <el-space>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-space>
      </el-form-item>
    </el-form>

    <ListFilterSummary :items="filterSummaryItems" />

    <el-table v-loading="loading || statsLoading" :data="tableData" border empty-text="暂无用户数据">
      <el-table-column prop="userId" label="用户ID" min-width="90">
        <template #default="{ row }">
          <LinkUser :user-id="row.userId" :label="String(row.userId)" />
        </template>
      </el-table-column>
      <el-table-column prop="userName" label="用户名" min-width="120" show-overflow-tooltip>
        <template #default="{ row }">
          <LinkUser :user-id="row.userId" :label="row.userName || '-'" />
        </template>
      </el-table-column>
      <el-table-column label="手机号" min-width="130">
        <template #default="{ row }">
          <LinkUser :user-id="row.userId" :phone="resolveUserPhone(row)" />
        </template>
      </el-table-column>
      <el-table-column label="订单数" min-width="90" align="center">
        <template #default="{ row }">
          <el-button
            v-if="canViewUserOrders && Number(row.orderCount) > 0"
            link
            type="primary"
            @click="goUserOrders(row)"
          >
            {{ row.orderCount ?? 0 }}
          </el-button>
          <span v-else>{{ row.orderCount ?? 0 }}</span>
        </template>
      </el-table-column>
      <el-table-column label="押金合计" min-width="110" align="right">
        <template #default="{ row }">{{ formatMoney(row.depositAmount) }}</template>
      </el-table-column>
      <el-table-column prop="platform" label="注册来源" min-width="110" show-overflow-tooltip />
      <el-table-column label="上级" min-width="130" show-overflow-tooltip>
        <template #default="{ row }">
          <LinkUser
            v-if="row.inviterUserId"
            :user-id="row.inviterUserId"
            :label="row.inviterUserName || row.inviterPhone || '-'"
          />
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="下级" min-width="150" show-overflow-tooltip>
        <template #default="{ row }">
          <el-tooltip
            v-if="Number(row.inviteCount) > 0"
            :content="formatInviteeTooltip(row)"
            placement="top"
            :disabled="!row.inviteeSummary"
          >
            <span>{{ formatInviteeColumn(row) }}</span>
          </el-tooltip>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="佣金余额" min-width="110" align="right">
        <template #default="{ row }">{{ formatMoney(row.commission) }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" min-width="90" align="center">
        <template #default="{ row }">
          <StatusSwitch
            :model-value="row.status"
            :active-value="1"
            :inactive-value="2"
            @change="(value) => handleUserStatusChange(row, value)"
          />
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="注册时间" min-width="170" />
      <el-table-column label="操作" min-width="280" fixed="right">
        <template #default="{ row }">
          <div class="table-actions">
            <el-button size="small" type="primary" @click="openDetail(row)">详情</el-button>
            <el-button size="small" type="warning" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="canViewUserOrders" size="small" type="info" @click="goUserOrders(row)">
              订单
            </el-button>
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

    <el-dialog v-model="detailDialogVisible" title="用户详情" width="720px" destroy-on-close>
      <div v-loading="detailDialogLoading">
        <template v-if="detailData">
          <div class="detail-section-title">头像</div>
          <div class="detail-avatar">
            <el-image :src="resolveUserAvatar(detailData.avatar)" fit="cover" style="width: 72px; height: 72px; border-radius: 50%" />
          </div>

          <div class="detail-section-title">基本信息</div>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="用户ID">{{ detailData.userId }}</el-descriptions-item>
            <el-descriptions-item label="用户名">{{ detailData.userName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="手机号">{{ resolveUserPhone(detailData) }}</el-descriptions-item>
            <el-descriptions-item label="性别">{{ formatUserSex(detailData.sex) }}</el-descriptions-item>
            <el-descriptions-item label="年龄">{{ detailData.age ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="上级邀请码">{{ detailData.superiorInvitationCode || '-' }}</el-descriptions-item>
            <el-descriptions-item label="下级人数">{{ detailAssets.inviteCount ?? 0 }}</el-descriptions-item>
            <el-descriptions-item label="佣金余额">{{ formatMoney(detailAssets.referralCommission) }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <StatusSwitch
                :model-value="detailData.status"
                :active-value="1"
                :inactive-value="2"
                readonly
              />
            </el-descriptions-item>
            <el-descriptions-item label="注册来源">{{ detailData.platform || '-' }}</el-descriptions-item>
            <el-descriptions-item label="邀请码">{{ detailData.invitationCode || '-' }}</el-descriptions-item>
            <el-descriptions-item label="注册时间">{{ detailData.createTime || '-' }}</el-descriptions-item>
          </el-descriptions>

          <div class="detail-section-title">下级用户（最近 20 条）</div>
          <el-table :data="inviteList" border size="small" empty-text="暂无下级用户">
            <el-table-column label="用户名" min-width="120">
              <template #default="{ row }">
                <LinkUser :user-id="row.userId" :label="row.userName || '-'" />
              </template>
            </el-table-column>
            <el-table-column label="手机号" min-width="130">
              <template #default="{ row }">
                <LinkUser :user-id="row.userId" :phone="resolveUserPhone(row)" />
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="注册时间" min-width="170" />
          </el-table>

          <div class="detail-section-title">账户统计</div>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="地址数量">{{ detailData.addressCount ?? 0 }}</el-descriptions-item>
            <el-descriptions-item label="收藏数量">{{ detailData.favoriteCount ?? 0 }}</el-descriptions-item>
          </el-descriptions>

          <div class="detail-section-title">资产概览</div>
          <el-descriptions :column="3" border>
            <el-descriptions-item label="钱包余额">{{ formatMoney(detailAssets.walletAmount) }}</el-descriptions-item>
            <el-descriptions-item label="押金合计">{{ formatMoney(detailAssets.depositTotal) }}</el-descriptions-item>
            <el-descriptions-item label="可用押金">{{ formatMoney(detailAssets.depositAvailable) }}</el-descriptions-item>
            <el-descriptions-item label="冻结押金">{{ formatMoney(detailAssets.depositFrozen) }}</el-descriptions-item>
            <el-descriptions-item label="分销佣金">{{ formatMoney(detailAssets.referralCommission) }}</el-descriptions-item>
          </el-descriptions>
          <div class="asset-actions">
            <el-button size="small" type="primary" plain @click="openWalletAdjust">调钱包</el-button>
            <el-button size="small" type="warning" plain @click="openDepositAdjust">调押</el-button>
          </div>
          <div class="asset-actions asset-actions-secondary">
            <el-button size="small" @click="goAssetPage('/business/wallets', detailData.userId, resolveUserPhone(detailData))">
              用户钱包
            </el-button>
            <el-button size="small" @click="goAssetPage('/business/wallet-transactions', detailData.userId, resolveUserPhone(detailData))">
              钱包流水
            </el-button>
            <el-button size="small" @click="goAssetPage('/business/referral-commissions', detailData.userId, resolveUserPhone(detailData))">
              分销佣金
            </el-button>
            <el-button size="small" @click="goAssetPage('/business/withdrawal-accounts', detailData.userId, resolveUserPhone(detailData))">
              提现账户
            </el-button>
            <el-button size="small" @click="goAssetPage('/business/withdraw-records', detailData.userId, resolveUserPhone(detailData))">
              提现记录
            </el-button>
            <el-button size="small" type="warning" plain @click="goAssetPage('/business/deposits', detailData.userId, resolveUserPhone(detailData))">
              押金管理
            </el-button>
          </div>

          <div class="detail-section-title">订单统计</div>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="订单总数">{{ detailOrderStats.orderCount }}</el-descriptions-item>
            <el-descriptions-item label="押金合计">{{ formatMoney(detailOrderStats.depositAmount) }}</el-descriptions-item>
          </el-descriptions>
        </template>
      </div>
      <template #footer>
        <el-space>
          <el-button @click="detailDialogVisible = false">关闭</el-button>
          <el-button v-if="detailData" type="warning" @click="openCommissionAdjust">调整佣金</el-button>
          <el-button
            v-if="detailData"
            type="success"
            @click="goCommissions(detailData); detailDialogVisible = false"
          >
            佣金流水
          </el-button>
          <el-button
            v-if="canViewUserOrders && detailData"
            type="primary"
            @click="goUserOrders(detailData); detailDialogVisible = false"
          >
            查看订单
          </el-button>
        </el-space>
      </template>
    </el-dialog>

    <AppDialog v-model="editDialogVisible" title="编辑用户" :loading="editDialogLoading" @confirm="handleSubmit">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item label="用户名" prop="userName">
          <el-input v-model="form.userName" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input :model-value="form.phone" disabled placeholder="登录手机号不可修改" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status">
            <el-option label="正常" :value="1" />
            <el-option label="禁用" :value="2" />
          </el-select>
        </el-form-item>
      </el-form>
    </AppDialog>

    <AppDialog v-model="walletAdjustVisible" title="调钱包" :loading="walletAdjustLoading" @confirm="submitWalletAdjust">
      <el-form :model="walletAdjustForm" label-width="100px">
        <el-form-item label="用户">
          <el-input :model-value="walletAdjustForm.userLabel" disabled />
        </el-form-item>
        <el-form-item label="当前余额">
          <el-input :model-value="formatMoney(detailAssets.walletAmount)" disabled />
        </el-form-item>
        <el-form-item label="调整方向">
          <el-select v-model="walletAdjustForm.direction">
            <el-option label="增加" :value="1" />
            <el-option label="减少" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="调整金额">
          <el-input-number v-model="walletAdjustForm.amount" :min="0.01" :precision="2" />
        </el-form-item>
      </el-form>
    </AppDialog>

    <AppDialog v-model="commissionAdjustVisible" title="调整佣金余额" width="480px" :loading="commissionAdjustLoading" @confirm="submitCommissionAdjust">
      <el-form :model="commissionAdjustForm" label-width="100px">
        <el-form-item label="用户">
          <el-input :model-value="commissionAdjustForm.userLabel" disabled />
        </el-form-item>
        <el-form-item label="佣金余额">
          <el-input-number v-model="commissionAdjustForm.commission" :min="0" :precision="2" />
        </el-form-item>
      </el-form>
    </AppDialog>

    <AppDialog v-model="depositAdjustVisible" title="调押" width="520px" :loading="depositAdjustLoading" @confirm="submitDepositAdjust">
      <el-form ref="depositAdjustFormRef" :model="depositAdjustForm" :rules="depositAdjustRules" label-width="100px">
        <el-form-item label="用户">
          <el-input :model-value="`${depositAdjustForm.userName}（${depositAdjustForm.phone}）`" disabled />
        </el-form-item>
        <el-form-item label="可用押金">
          <el-input :model-value="formatMoney(depositAdjustForm.availableAmount)" disabled />
        </el-form-item>
        <el-form-item label="调整方向">
          <el-select v-model="depositAdjustForm.direction">
            <el-option label="增加押金" :value="1" />
            <el-option label="减少押金" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="调整金额" prop="amount">
          <el-input-number v-model="depositAdjustForm.amount" :min="0.01" :precision="2" />
        </el-form-item>
        <el-form-item label="运营备注">
          <el-input v-model="depositAdjustForm.remark" type="textarea" :rows="3" placeholder="调整原因说明" />
        </el-form-item>
      </el-form>
    </AppDialog>
  </el-card>
</template>

<style scoped>
.range-sep {
  margin: 0 8px;
  color: var(--el-text-color-secondary);
}

.detail-section-title {
  margin: 16px 0 8px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.detail-section-title:first-child {
  margin-top: 0;
}

.detail-avatar {
  margin-bottom: 8px;
}

.detail-empty-text {
  margin-bottom: 8px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.asset-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.asset-actions-secondary {
  margin-top: 8px;
}
</style>
