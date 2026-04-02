<template>
  <div>
    <div class="page-header">
      <h2>统计分析</h2>
      <div style="display: flex; gap: 12px; align-items: center;">
        <el-date-picker
          v-model="selectedMonth"
          type="month"
          placeholder="选择月份"
          value-format="YYYY-MM"
          style="width: 160px;"
          @change="loadData"
        />
      </div>
    </div>

    <!-- 月度概览卡片 -->
    <div class="stat-cards">
      <div class="stat-card">
        <div class="label">月度收入</div>
        <div class="value income">¥ {{ monthlyStat?.totalIncome?.toFixed(2) || '0.00' }}</div>
      </div>
      <div class="stat-card">
        <div class="label">月度支出</div>
        <div class="value expense">¥ {{ monthlyStat?.totalExpense?.toFixed(2) || '0.00' }}</div>
      </div>
      <div class="stat-card">
        <div class="label">月度结余</div>
        <div class="value balance">¥ {{ monthlyStat?.balance?.toFixed(2) || '0.00' }}</div>
      </div>
      <div class="stat-card">
        <div class="label">预算使用</div>
        <div v-if="budgetUsageData" style="margin-top: 4px;">
          <div style="font-size: 14px; color: #909399; margin-bottom: 4px;">
            ¥{{ budgetUsageData.totalUsed.toFixed(2) }} / ¥{{ budgetUsageData.totalBudget.toFixed(2) }}
          </div>
          <el-progress
            :percentage="Math.min(Math.round(budgetUsageData.totalUsed / budgetUsageData.totalBudget * 100), 100)"
            :color="budgetUsageData.totalUsed / budgetUsageData.totalBudget >= 1 ? '#F56C6C' : budgetUsageData.totalUsed / budgetUsageData.totalBudget >= 0.8 ? '#E6A23C' : '#409EFF'"
            :stroke-width="12"
            :text-inside="true"
          />
        </div>
        <div v-else style="font-size: 14px; color: #909399;">{{ monthlyStat?.billCount || 0 }} 笔账单</div>
      </div>
    </div>

    <!-- 图表区域 -->
    <el-row :gutter="16">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>支出分类占比</span>
          </template>
          <div ref="pieChartRef" style="height: 350px;"></div>
          <el-empty v-if="!categoryRatios.length" description="暂无支出数据" style="margin-top: -200px;" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>收支趋势（近6个月）</span>
          </template>
          <div ref="lineChartRef" style="height: 350px;"></div>
          <el-empty v-if="!trendData.length" description="暂无趋势数据" style="margin-top: -200px;" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { getMonthlyStat, getCategoryRatio, getTrend } from '@/api/stat'
import { getBudgetUsage } from '@/api/budget'
import type { MonthlyStat, CategoryRatio, TrendItem, BudgetUsage } from '@/types'
import dayjs from 'dayjs'
import * as echarts from 'echarts'

const selectedMonth = ref(dayjs().format('YYYY-MM'))
const monthlyStat = ref<MonthlyStat | null>(null)
const categoryRatios = ref<CategoryRatio[]>([])
const trendData = ref<TrendItem[]>([])
const budgetUsageData = ref<BudgetUsage | null>(null)

const pieChartRef = ref<HTMLElement>()
const lineChartRef = ref<HTMLElement>()
let pieChart: echarts.ECharts | null = null
let lineChart: echarts.ECharts | null = null

const loadData = async () => {
  try {
    const [statRes, ratioRes, trendRes] = await Promise.all([
      getMonthlyStat(selectedMonth.value),
      getCategoryRatio(selectedMonth.value, 2),
      getTrend(6)
    ])
    monthlyStat.value = statRes.data
    categoryRatios.value = ratioRes.data
    trendData.value = trendRes.data
    // 加载预算使用情况
    try {
      const budgetRes = await getBudgetUsage(selectedMonth.value)
      budgetUsageData.value = budgetRes.data
    } catch {
      budgetUsageData.value = null
    }
    await nextTick()
    renderPieChart()
    renderLineChart()
  } catch (e) {}
}

const renderPieChart = () => {
  if (!pieChartRef.value) return
  if (!pieChart) pieChart = echarts.init(pieChartRef.value)
  pieChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
    legend: { orient: 'vertical', left: 'left' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
      data: categoryRatios.value.map(item => ({
        name: item.categoryName,
        value: item.amount
      }))
    }]
  })
}

const renderLineChart = () => {
  if (!lineChartRef.value) return
  if (!lineChart) lineChart = echarts.init(lineChartRef.value)
  lineChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['收入', '支出'] },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: trendData.value.map(t => t.month) },
    yAxis: { type: 'value' },
    series: [
      {
        name: '收入',
        type: 'line',
        smooth: true,
        itemStyle: { color: '#67c23a' },
        areaStyle: { opacity: 0.1 },
        data: trendData.value.map(t => t.income)
      },
      {
        name: '支出',
        type: 'line',
        smooth: true,
        itemStyle: { color: '#f56c6c' },
        areaStyle: { opacity: 0.1 },
        data: trendData.value.map(t => t.expense)
      }
    ]
  })
}

const handleResize = () => {
  pieChart?.resize()
  lineChart?.resize()
}

onMounted(() => {
  loadData()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  pieChart?.dispose()
  lineChart?.dispose()
})
</script>
