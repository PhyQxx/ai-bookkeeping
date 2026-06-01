<template>
  <div class="dashboard" v-loading="dashboardStore.loading">
    <!-- Summary Cards -->
    <el-row :gutter="16" style="margin-bottom: 20px;">
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="summary-card income-card">
          <div class="summary-label">本月收入</div>
          <div class="summary-value income">¥{{ fmt(summary?.totalIncome) }}</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="summary-card expense-card">
          <div class="summary-label">本月支出</div>
          <div class="summary-value expense">¥{{ fmt(summary?.totalExpense) }}</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="summary-card balance-card">
          <div class="summary-label">本月结余</div>
          <div class="summary-value" :class="summary && summary.balance >= 0 ? 'income' : 'expense'">
            ¥{{ fmt(summary?.balance) }}
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="summary-card count-card">
          <div class="summary-label">本月笔数</div>
          <div class="summary-value">{{ summary?.billCount ?? 0 }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Charts Row -->
    <el-row :gutter="16" style="margin-bottom: 20px;">
      <el-col :xs="24" :lg="16">
        <el-card shadow="hover">
          <template #header><span>📈 近30天收支趋势</span></template>
          <div ref="trendChartRef" style="height: 320px;"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="8">
        <el-card shadow="hover">
          <template #header><span>🍩 分类消费占比</span></template>
          <div ref="pieChartRef" style="height: 320px;"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Bottom Row -->
    <el-row :gutter="16">
      <el-col :xs="24" :lg="12">
        <el-card shadow="hover">
          <template #header><span>🏆 分类消费排行 Top5</span></template>
          <el-table :data="dashboardStore.categoryRanking" stripe style="width: 100%;">
            <el-table-column type="index" label="#" width="50" />
            <el-table-column prop="categoryName" label="分类">
              <template #default="{ row }">
                <el-button type="primary" link @click="router.push({ path: '/bills', query: { categoryId: row.categoryId, type: 2 } })">
                  {{ row.categoryName }}
                </el-button>
              </template>
            </el-table-column>
            <el-table-column prop="amount" label="金额" align="right">
              <template #default="{ row }">
                <span style="color: #f56c6c; font-weight: bold;">¥{{ row.amount.toFixed(2) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="count" label="笔数" width="80" align="center" />
            <el-table-column label="占比" width="120">
              <template #default="{ row }">
                <el-progress :percentage="Math.round(row.percentage * 100)" :stroke-width="14" :text-inside="true" />
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="dashboardStore.categoryRanking.length === 0" description="暂无数据" />
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card shadow="hover">
          <template #header><span>💰 预算使用进度</span></template>
          <div v-for="item in dashboardStore.budgetProgress" :key="item.budgetId" style="margin-bottom: 16px;">
            <div style="display: flex; justify-content: space-between; margin-bottom: 4px;">
              <span>{{ item.categoryName }}</span>
              <span :style="{ color: item.overBudget ? '#f56c6c' : '#606266' }">
                ¥{{ item.usedAmount.toFixed(2) }} / ¥{{ item.budgetAmount.toFixed(2) }}
              </span>
            </div>
            <el-progress
              :percentage="Math.min(Math.round(item.usagePercent), 100)"
              :color="item.overBudget ? '#f56c6c' : item.usagePercent >= 80 ? '#E6A23C' : '#409EFF'"
              :stroke-width="20"
              :text-inside="true"
            />
            <div v-if="item.overBudget" style="color: #f56c6c; font-size: 12px; margin-top: 2px;">
              ⚠️ 超支 ¥{{ (item.usedAmount - item.budgetAmount).toFixed(2) }}
            </div>
          </div>
          <el-empty v-if="dashboardStore.budgetProgress.length === 0" description="暂未设置预算" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useDashboardStore } from '@/stores/dashboard'
import * as echarts from 'echarts'

const router = useRouter()
const dashboardStore = useDashboardStore()
const summary = computed(() => dashboardStore.summary)

const trendChartRef = ref<HTMLElement>()
const pieChartRef = ref<HTMLElement>()
let trendChart: echarts.ECharts | null = null
let pieChart: echarts.ECharts | null = null

const fmt = (val?: number) => (val ?? 0).toFixed(2)

const COLORS = ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272']

function initTrendChart() {
  if (!trendChartRef.value) return
  trendChart = echarts.init(trendChartRef.value)
  updateTrendChart()
}

function updateTrendChart() {
  if (!trendChart || !dashboardStore.trend.length) return
  const dates = dashboardStore.trend.map(t => t.date.slice(5))
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['收入', '支出'], top: 0 },
    grid: { left: 50, right: 20, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: dates, axisLabel: { interval: 4 } },
    yAxis: { type: 'value' },
    series: [
      { name: '收入', type: 'line', data: dashboardStore.trend.map(t => t.income), smooth: true, itemStyle: { color: '#67c23a' }, areaStyle: { color: 'rgba(103,194,58,0.1)' } },
      { name: '支出', type: 'line', data: dashboardStore.trend.map(t => t.expense), smooth: true, itemStyle: { color: '#f56c6c' }, areaStyle: { color: 'rgba(245,108,108,0.1)' } }
    ]
  })
}

function initPieChart() {
  if (!pieChartRef.value) return
  if (!pieChart) {
    pieChart = echarts.init(pieChartRef.value)
    pieChart.on('click', (params: any) => {
      const rank = dashboardStore.categoryRanking.find(r => r.categoryName === params.name)
      if (rank && rank.categoryId) {
        router.push({
          path: '/bills',
          query: { categoryId: rank.categoryId, type: 2 }
        })
      }
    })
  }
  updatePieChart()
}

function updatePieChart() {
  if (!pieChart || !dashboardStore.categoryRanking.length) return
  pieChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
    legend: { orient: 'vertical', left: 'left', top: 'middle' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['60%', '50%'],
      data: dashboardStore.categoryRanking.map((r, i) => ({
        name: r.categoryName,
        value: r.amount,
        itemStyle: { color: COLORS[i % COLORS.length] }
      })),
      label: { formatter: '{b}\n{d}%' },
      emphasis: { itemStyle: { shadowBlur: 10, shadowColor: 'rgba(0,0,0,0.3)' } }
    }]
  })
}

function handleResize() {
  trendChart?.resize()
  pieChart?.resize()
}

watch(() => dashboardStore.trend, () => { nextTick(updateTrendChart) }, { deep: true })
watch(() => dashboardStore.categoryRanking, () => { nextTick(updatePieChart) }, { deep: true })

onMounted(async () => {
  await dashboardStore.loadAll()
  nextTick(() => {
    initTrendChart()
    initPieChart()
  })
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  trendChart?.dispose()
  pieChart?.dispose()
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.dashboard { padding: 0; }
.summary-card { text-align: center; }
.summary-label { font-size: 14px; color: #909399; margin-bottom: 8px; }
.summary-value { font-size: 24px; font-weight: bold; }
.income { color: #67c23a; }
.expense { color: #f56c6c; }
</style>
