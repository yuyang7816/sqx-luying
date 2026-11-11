<script setup>
import * as echarts from 'echarts'
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh } from '@element-plus/icons-vue'
import { getDashboardStats } from '@/api/modules/stats'
import NoticeBadge from '@/components/common/NoticeBadge.vue'
import { BRAND_ADMIN_TITLE, BRAND_COLORS } from '@/constants/brand'
import { useAuthStore } from '@/stores/auth'
import { useMenuBadgeStore } from '@/stores/menuBadge'

const router = useRouter()
const authStore = useAuthStore()
const menuBadgeStore = useMenuBadgeStore()
const loading = ref(false)
const trendRef = ref()
const pieRef = ref()
let trendChart
let pieChart

const stats = ref({
  rentalCategoryTotal: 0,
  rentalGoodsTotal: 0,
  todayOrderCount: 0,
  todayPayAmount: 0,
  rentingOrderCount: 0,
  pendingCount: 0,
  pendingReturningCount: 0,
  pendingInspectingCount: 0,
  pendingInspectionCount: 0,
  pendingFeedbackCount: 0,
  pendingMerchantApplyCount: 0,
  pendingWithdrawCount: 0,
  totalMerchantCount: 0,
  totalReferralCommission: 0,
  todayNewUserCount: 0,
  todayInviteRegisterCount: 0,
  totalAppUserCount: 0,
  weekOrderCount: 0,
  weekPayAmount: 0,
  orderTrend: [],
  orderStatusDistribution: [],
})

const todayText = computed(() => {
  const now = new Date()
  return now.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    weekday: 'long',
  })
})

const primaryKpis = [
  { key: 'todayOrderCount', label: '今日订单', path: '/rental/orders', tone: 'green', suffix: '单' },
  { key: 'todayPayAmount', label: '今日成交额', path: '/rental/orders', tone: 'lime', suffix: '元', isMoney: true },
  { key: 'rentingOrderCount', label: '租赁中', path: '/rental/orders?status=renting', tone: 'forest', suffix: '单' },
  { key: 'pendingCount', label: '待处理', path: '/rental/inspections?auditStatus=pending', tone: 'warn', suffix: '项' },
]

const assetKpis = [
  { key: 'weekPayAmount', label: '近7日成交额', path: '/rental/orders', isMoney: true },
  { key: 'weekOrderCount', label: '近7日订单', path: '/rental/orders', suffix: '单' },
  { key: 'rentalGoodsTotal', label: '租赁商品', path: '/rental/goods', suffix: '件' },
  { key: 'totalAppUserCount', label: 'C端用户', path: '/users', suffix: '人' },
]

const growthKpis = [
  { key: 'todayNewUserCount', label: '今日新增用户', path: '/users', suffix: '人' },
  { key: 'todayInviteRegisterCount', label: '今日邀请注册', path: '/users', suffix: '人' },
  { key: 'totalReferralCommission', label: '累计分销佣金', path: '/business/referral-commissions', isMoney: true },
  { key: 'totalMerchantCount', label: '活跃商户', path: '/merchant/list', suffix: '家' },
]

const todoKpis = [
  { key: 'pendingFeedbackCount', label: '待处理反馈', path: '/business/feedbacks?status=pending', suffix: '条' },
  { key: 'pendingMerchantApplyCount', label: '待审核商户', path: '/merchant/applies?status=pending', suffix: '家' },
  { key: 'pendingWithdrawCount', label: '待处理提现', path: '/business/withdraw-records?status=1', suffix: '笔' },
]

const quickLinks = [
  { label: '数据统计', path: '/dashboard', badgePath: '/dashboard' },
  { label: '租赁订单', path: '/rental/orders', badgePath: '/rental/orders' },
  { label: '验收记录', path: '/rental/inspections', badgePath: '/rental/inspections' },
  { label: '入驻审核', path: '/merchant/applies', badgePath: '/merchant/applies' },
  { label: '用户反馈', path: '/business/feedbacks', badgePath: '/business/feedbacks' },
  { label: '提现管理', path: '/business/withdraw-records', badgePath: '/business/withdraw-records' },
  { label: '库存管理', path: '/rental/stock' },
  { label: '系统配置', path: '/system/configs' },
]

function getQuickLinkBadge(item) {
  if (!item.badgePath) return 0
  return menuBadgeStore.getPathBadge(item.badgePath)
}

const piePalette = ['#7ed321', '#6bc01a', '#5cb816', '#9ed85a', '#c5e894', '#4a9e12', '#b8d99a', '#d94b4b']

function formatValue(item, rawValue) {
  if (item.isMoney) {
    return Number(rawValue || 0).toFixed(2)
  }
  return `${rawValue ?? 0}${item.suffix || ''}`
}

function goTo(path) {
  if (!path) return
  const [routePath, queryString] = path.split('?')
  if (queryString) {
    router.push({ path: routePath, query: Object.fromEntries(new URLSearchParams(queryString)) })
    return
  }
  router.push(path)
}

function disposeCharts() {
  trendChart?.dispose()
  pieChart?.dispose()
  trendChart = null
  pieChart = null
}

function renderTrendChart() {
  if (!trendRef.value) return
  trendChart ||= echarts.init(trendRef.value)
  const trend = stats.value.orderTrend || []
  trendChart.setOption({
    color: [BRAND_COLORS.primary, BRAND_COLORS.primaryDark],
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(26, 33, 20, 0.92)',
      borderWidth: 0,
      textStyle: { color: '#fff' },
    },
    legend: {
      data: ['订单数', '成交额(元)'],
      top: 0,
      textStyle: { color: BRAND_COLORS.muted },
    },
    grid: { left: 48, right: 48, top: 42, bottom: 28 },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: trend.map((item) => item.date),
      axisLine: { lineStyle: { color: '#d8e4cc' } },
      axisLabel: { color: BRAND_COLORS.muted },
    },
    yAxis: [
      {
        type: 'value',
        name: '订单',
        axisLabel: { color: BRAND_COLORS.muted },
        splitLine: { lineStyle: { color: 'rgba(26,33,20,0.06)' } },
      },
      {
        type: 'value',
        name: '金额',
        axisLabel: { color: BRAND_COLORS.muted },
        splitLine: { show: false },
      },
    ],
    series: [
      {
        name: '订单数',
        type: 'line',
        smooth: true,
        symbolSize: 8,
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(126, 211, 33, 0.28)' },
            { offset: 1, color: 'rgba(126, 211, 33, 0.02)' },
          ]),
        },
        data: trend.map((item) => item.orderCount ?? 0),
      },
      {
        name: '成交额(元)',
        type: 'line',
        smooth: true,
        yAxisIndex: 1,
        symbolSize: 8,
        data: trend.map((item) => Number(item.payAmount ?? 0)),
      },
    ],
  })
}

function renderPieChart() {
  if (!pieRef.value) return
  pieChart ||= echarts.init(pieRef.value)
  const distribution = stats.value.orderStatusDistribution || []
  pieChart.setOption({
    color: piePalette,
    tooltip: {
      trigger: 'item',
      formatter: '{b}<br/>{c} 单 ({d}%)',
      backgroundColor: 'rgba(26, 33, 20, 0.92)',
      borderWidth: 0,
      textStyle: { color: '#fff' },
    },
    legend: {
      orient: 'vertical',
      right: 0,
      top: 'center',
      textStyle: { color: BRAND_COLORS.muted },
    },
    series: [
      {
        type: 'pie',
        radius: ['42%', '68%'],
        center: ['36%', '50%'],
        itemStyle: {
          borderRadius: 8,
          borderColor: '#fff',
          borderWidth: 2,
        },
        label: { show: false },
        data: distribution.map((item) => ({
          name: item.label || item.status,
          value: item.count ?? 0,
        })),
      },
    ],
  })
}

function renderCharts() {
  nextTick(() => {
    renderTrendChart()
    renderPieChart()
  })
}

function handleResize() {
  trendChart?.resize()
  pieChart?.resize()
}

async function loadStats() {
  loading.value = true
  try {
    stats.value = await getDashboardStats()
    await menuBadgeStore.refresh()
    renderCharts()
  } finally {
    loading.value = false
  }
}

watch(
  () => stats.value.orderTrend,
  () => {
    if ((stats.value.orderTrend || []).length) {
      renderCharts()
    }
  },
)

onMounted(() => {
  loadStats()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  disposeCharts()
})
</script>

<template>
  <div v-loading="loading" class="dashboard-page">
    <section class="dashboard-hero">
      <div>
        <p class="dashboard-hero__date">{{ todayText }}</p>
        <h2>{{ BRAND_ADMIN_TITLE }}</h2>
        <p class="dashboard-hero__welcome">欢迎回来，{{ authStore.profile?.name || '管理员' }} · 以下是露营租赁运营数据概览</p>
      </div>
      <el-button type="primary" plain :icon="Refresh" @click="loadStats">刷新数据</el-button>
    </section>

    <section class="kpi-grid kpi-grid--primary">
      <article
        v-for="item in primaryKpis"
        :key="item.key"
        class="kpi-card"
        :class="`kpi-card--${item.tone}`"
        @click="goTo(item.path)"
      >
        <span class="kpi-card__label">
          {{ item.label }}
          <NoticeBadge v-if="item.key === 'pendingCount'" :value="stats[item.key]" />
        </span>
        <strong class="kpi-card__value">{{ formatValue(item, stats[item.key]) }}</strong>
        <span class="kpi-card__hint">点击查看详情</span>
      </article>
    </section>

    <p class="dashboard-tip">待处理 = 待归还 + 验收中 + 待复核验收，建议优先跟进避免影响押金退还</p>

    <section class="kpi-grid kpi-grid--compact">
      <article
        v-for="item in todoKpis"
        :key="item.key"
        class="mini-card mini-card--todo"
        @click="goTo(item.path)"
      >
        <span class="mini-card__label">
          {{ item.label }}
          <NoticeBadge :value="stats[item.key]" />
        </span>
        <strong>{{ formatValue(item, stats[item.key]) }}</strong>
      </article>
    </section>

    <section class="kpi-grid kpi-grid--compact">
      <article v-for="item in assetKpis" :key="item.key" class="mini-card" @click="goTo(item.path)">
        <span>{{ item.label }}</span>
        <strong>{{ formatValue(item, stats[item.key]) }}</strong>
      </article>
    </section>

    <section class="chart-grid">
      <article class="chart-card">
        <header class="chart-card__head">
          <div>
            <strong>近7日经营趋势</strong>
            <p>订单量与成交额走势，用于判断活动与周末峰值</p>
          </div>
        </header>
        <div ref="trendRef" class="chart-card__canvas" />
      </article>

      <article class="chart-card">
        <header class="chart-card__head">
          <div>
            <strong>订单状态分布</strong>
            <p>当前全量订单所处阶段，便于调配验收人力</p>
          </div>
        </header>
        <div ref="pieRef" class="chart-card__canvas chart-card__canvas--pie" />
      </article>
    </section>

    <section class="panel-card">
      <header class="panel-card__head">
        <strong>增长与分销</strong>
        <span>拉新、邀请与佣金结算情况</span>
      </header>
      <div class="growth-grid">
        <article v-for="item in growthKpis" :key="item.key" class="growth-item" @click="goTo(item.path)">
          <span>{{ item.label }}</span>
          <strong>{{ formatValue(item, stats[item.key]) }}</strong>
        </article>
      </div>
    </section>

    <section class="panel-card">
      <header class="panel-card__head">
        <strong>快捷入口</strong>
        <span>日常运营高频操作</span>
      </header>
      <div class="quick-grid">
        <button v-for="item in quickLinks" :key="item.path" type="button" class="quick-btn" @click="goTo(item.path)">
          <span class="quick-btn__label">
            {{ item.label }}
            <NoticeBadge :value="getQuickLinkBadge(item)" />
          </span>
        </button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.dashboard-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.dashboard-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 24px 28px;
  border-radius: 24px;
  background:
    radial-gradient(circle at top right, rgba(126, 211, 33, 0.14), transparent 38%),
    linear-gradient(135deg, #ffffff 0%, #f7fbf2 100%);
  border: 1px solid rgba(126, 211, 33, 0.14);
  box-shadow: 0 10px 30px rgba(26, 33, 20, 0.04);
}

.dashboard-hero__date {
  margin: 0 0 6px;
  color: #71806a;
  font-size: 13px;
}

.dashboard-hero h2 {
  margin: 0 0 8px;
  font-size: 24px;
  color: #1a2114;
}

.dashboard-hero__welcome {
  margin: 0;
  color: #6b7a5e;
  font-size: 14px;
}

.kpi-grid {
  display: grid;
  gap: 16px;
}

.kpi-grid--primary {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.kpi-grid--compact {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.kpi-card {
  position: relative;
  padding: 22px 20px;
  border-radius: 22px;
  cursor: pointer;
  overflow: hidden;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  background: #fff;
  border: 1px solid rgba(26, 33, 20, 0.06);
  box-shadow: 0 8px 24px rgba(26, 33, 20, 0.04);
}

.kpi-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 14px 32px rgba(26, 33, 20, 0.08);
}

.kpi-card::after {
  content: '';
  position: absolute;
  inset: auto -20px -20px auto;
  width: 88px;
  height: 88px;
  border-radius: 50%;
  opacity: 0.16;
}

.kpi-card--green::after { background: #7ed321; }
.kpi-card--lime::after { background: #6bc01a; }
.kpi-card--forest::after { background: #5cb816; }
.kpi-card--warn::after { background: #e6a817; }

.kpi-card__label {
  display: block;
  color: #71806a;
  font-size: 13px;
}

.kpi-card__value {
  display: block;
  margin-top: 10px;
  font-size: 34px;
  line-height: 1.1;
  color: #1a2114;
}

.kpi-card--green .kpi-card__value { color: #5cb816; }
.kpi-card--lime .kpi-card__value { color: #6bc01a; }
.kpi-card--forest .kpi-card__value { color: #4a9e12; }
.kpi-card--warn .kpi-card__value { color: #d48806; }

.kpi-card__hint {
  display: block;
  margin-top: 10px;
  color: #aab592;
  font-size: 12px;
}

.dashboard-tip {
  margin: -4px 0 0;
  padding: 0 4px;
  color: #8a9a7a;
  font-size: 12px;
}

.mini-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 18px 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(26, 33, 20, 0.06);
  cursor: pointer;
}

.mini-card span {
  color: #71806a;
  font-size: 13px;
}

.mini-card strong {
  font-size: 24px;
  color: #1a2114;
}

.mini-card--todo strong {
  color: #d48806;
}

.chart-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(320px, 0.9fr);
  gap: 16px;
}

.chart-card,
.panel-card {
  padding: 20px 22px;
  border-radius: 24px;
  background: #fff;
  border: 1px solid rgba(26, 33, 20, 0.06);
  box-shadow: 0 8px 24px rgba(26, 33, 20, 0.04);
}

.chart-card__head,
.panel-card__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.chart-card__head strong,
.panel-card__head strong {
  display: block;
  font-size: 16px;
  color: #1a2114;
}

.chart-card__head p,
.panel-card__head span {
  margin: 6px 0 0;
  color: #8a9a7a;
  font-size: 12px;
}

.chart-card__canvas {
  width: 100%;
  height: 320px;
}

.chart-card__canvas--pie {
  height: 320px;
}

.growth-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-top: 8px;
}

.growth-item {
  padding: 16px;
  border-radius: 16px;
  background: #f9fbf6;
  cursor: pointer;
}

.growth-item span {
  display: block;
  color: #71806a;
  font-size: 13px;
}

.growth-item strong {
  display: block;
  margin-top: 8px;
  font-size: 26px;
  color: #4a9e12;
}

.quick-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 8px;
}

.quick-btn {
  padding: 10px 16px;
  border: 1px solid rgba(126, 211, 33, 0.22);
  border-radius: 999px;
  background: rgba(126, 211, 33, 0.08);
  color: #4a9e12;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.quick-btn__label,
.mini-card__label,
.order-tab-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.quick-btn:hover {
  background: linear-gradient(135deg, #7ed321, #6bc01a);
  color: #fff;
  border-color: transparent;
}

@media (max-width: 1200px) {
  .kpi-grid--primary,
  .kpi-grid--compact,
  .growth-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .chart-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .dashboard-hero {
    flex-direction: column;
    align-items: flex-start;
  }

  .kpi-grid--primary,
  .kpi-grid--compact,
  .growth-grid {
    grid-template-columns: 1fr;
  }
}
</style>
