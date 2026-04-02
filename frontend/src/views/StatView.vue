<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">📊 统计分析</h2>
    </div>

    <!-- 月份选择 -->
    <div class="month-selector">
      <el-date-picker
        v-model="selectedMonth"
        type="month"
        placeholder="选择月份"
        format="YYYY年MM月"
        value-format="YYYY-MM"
        @change="loadAll"
      />
    </div>

    <!-- 月度总览 -->
    <div class="stat-card-grid">
      <div class="stat-card">
        <div class="label">本月收入</div>
        <div class="value income-text">¥{{ monthlyStat.totalIncome || '0.00' }}</div>
      </div>
      <div class="stat-card">
        <div class="label">本月支出</div>
        <div class="value expense-text">¥{{ monthlyStat.totalExpense || '0.00' }}</div>
      </div>
      <div class="stat-card">
        <div class="label">本月结余</div>
        <div class="value" :class="(monthlyStat.balance || 0) >= 0 ? 'income-text' : 'expense-text'">
          ¥{{ monthlyStat.balance || '0.00' }}
        </div>
      </div>
      <div class="stat-card">
        <div class="label">记账笔数</div>
        <div class="value">{{ monthlyStat.billCount || 0 }}</div>
      </div>
    </div>

    <!-- 图表区 -->
    <div class="charts-row">
      <div class="chart-container" style="flex:1">
        <h3>支出分类占比</h3>
        <div ref="pieChartRef" style="height:350px"></div>
      </div>
      <div class="chart-container" style="flex:1">
        <h3>收支趋势</h3>
        <div ref="lineChartRef" style="height:350px"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getMonthlyStat, getCategoryRatio, getTrend } from '../api/stat'
import { getCurrentMonth } from '../utils'

const selectedMonth = ref(getCurrentMonth())
const monthlyStat = ref({})
const pieChartRef = ref(null)
const lineChartRef = ref(null)
let pieChart = null
let lineChart = null

async function loadAll() {
  const month = selectedMonth.value
  const [stat, ratios, trends] = await Promise.all([
    getMonthlyStat(month),
    getCategoryRatio(month, 2),
    getTrend(6)
  ])
  monthlyStat.value = stat || {}
  await nextTick()
  renderPieChart(ratios || [])
  renderLineChart(trends || [])
}

function renderPieChart(data) {
  if (!pieChartRef.value) return
  if (!pieChart) pieChart = echarts.init(pieChartRef.value)
  pieChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
    legend: { bottom: 0, type: 'scroll' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
      data: data.map(d => ({
        name: d.categoryName,
        value: Number(d.amount),
        ratio: d.ratio
      }))
    }]
  })
}

function renderLineChart(data) {
  if (!lineChartRef.value) return
  if (!lineChart) lineChart = echarts.init(lineChartRef.value)
  lineChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['收入', '支出'], bottom: 0 },
    grid: { left: 50, right: 20, top: 20, bottom: 40 },
    xAxis: { type: 'category', data: data.map(d => d.month) },
    yAxis: { type: 'value' },
    series: [
      { name: '收入', type: 'line', smooth: true, data: data.map(d => d.income), itemStyle: { color: '#67C23A' }, areaStyle: { color: 'rgba(103,194,58,0.1)' } },
      { name: '支出', type: 'line', smooth: true, data: data.map(d => d.expense), itemStyle: { color: '#F56C6C' }, areaStyle: { color: 'rgba(245,108,108,0.1)' } }
    ]
  })
}

function handleResize() {
  pieChart?.resize()
  lineChart?.resize()
}

onMounted(async () => {
  await loadAll()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  pieChart?.dispose()
  lineChart?.dispose()
})
</script>

<style scoped>
.month-selector {
  margin-bottom: 20px;
}
.charts-row {
  display: flex;
  gap: 20px;
  margin-top: 20px;
}
.chart-container h3 {
  margin-bottom: 12px;
  font-size: 16px;
}
@media (max-width: 1024px) {
  .charts-row {
    flex-direction: column;
  }
}
</style>
