<template>
  <div v-loading="loading">
    <div class="page-header">
      <h2>统计分析</h2>
      <div style="display: flex; gap: 12px; align-items: center;">
        <el-radio-group v-model="viewType" @change="handleViewTypeChange">
          <el-radio-button value="month">月报</el-radio-button>
          <el-radio-button value="year">年报</el-radio-button>
        </el-radio-group>
        <el-date-picker
          v-if="viewType === 'month'"
          v-model="selectedMonth"
          type="month"
          placeholder="选择月份"
          value-format="YYYY-MM"
          style="width: 160px;"
          @change="loadData"
        />
        <el-date-picker
          v-else
          v-model="selectedYear"
          type="year"
          placeholder="选择年份"
          value-format="YYYY"
          style="width: 120px;"
          @change="loadData"
        />
      </div>
    </div>

    <!-- 概览卡片 -->
    <div class="stat-cards">
      <div class="stat-card">
        <div class="label">{{ viewType === 'month' ? '月度' : '年度' }}收入</div>
        <div class="value income">¥ {{ currentStat.income }}</div>
      </div>
      <div class="stat-card">
        <div class="label">{{ viewType === 'month' ? '月度' : '年度' }}支出</div>
        <div class="value expense">¥ {{ currentStat.expense }}</div>
      </div>
      <div class="stat-card">
        <div class="label">{{ viewType === 'month' ? '月度' : '年度' }}结余</div>
        <div class="value balance">¥ {{ currentStat.balance }}</div>
      </div>
      <div class="stat-card">
        <div class="label">记账笔数</div>
        <div class="value" style="color: var(--el-color-primary);">{{ currentStat.count }} 笔</div>
      </div>
    </div>

    <!-- 图表区域 -->
    <el-row :gutter="16" style="margin-bottom: 20px;">
      <el-col :xs="24" :lg="12">
        <el-card>
          <template #header>
            <span>支出分类占比</span>
          </template>
          <div ref="pieChartRef" style="height: 350px;"></div>
          <el-empty v-if="!categoryRatios.length" description="暂无支出数据" style="margin-top: -200px;" />
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card>
          <template #header>
            <span>{{ viewType === 'month' ? '近6个月趋势' : '年度月度对比' }}</span>
          </template>
          <div ref="trendChartRef" style="height: 350px;"></div>
          <el-empty v-if="!trendData.length" description="暂无趋势数据" style="margin-top: -200px;" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 年度详情列表 (仅年报显示) -->
    <el-card v-if="viewType === 'year' && yearlyData" style="margin-bottom: 20px;">
      <template #header><span>📅 {{ selectedYear }} 年度月度明细</span></template>
      <el-table :data="yearlyData.monthlyDetails" stripe style="width: 100%;">
        <el-table-column prop="month" label="月份" width="120" />
        <el-table-column label="收入" align="right">
          <template #default="{ row }"><span class="income">¥{{ row.totalIncome.toFixed(2) }}</span></template>
        </el-table-column>
        <el-table-column label="支出" align="right">
          <template #default="{ row }"><span class="expense">¥{{ row.totalExpense.toFixed(2) }}</span></template>
        </el-table-column>
        <el-table-column label="结余" align="right">
          <template #default="{ row }">
            <span :class="row.balance >= 0 ? 'income' : 'expense'">¥{{ row.balance.toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="billCount" label="笔数" width="100" align="center" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { getMonthlyStat, getCategoryRatio, getTrend, getYearlyStat } from '@/api/stat'
import type { MonthlyStat, CategoryRatio, TrendItem, YearlyStat } from '@/types'
import dayjs from 'dayjs'
import * as echarts from 'echarts'

const router = useRouter()
const loading = ref(false)
const viewType = ref<'month' | 'year'>('month')
const selectedMonth = ref(dayjs().format('YYYY-MM'))
const selectedYear = ref(dayjs().format('YYYY'))

const monthlyStat = ref<MonthlyStat | null>(null)
const yearlyData = ref<YearlyStat | null>(null)
const categoryRatios = ref<CategoryRatio[]>([])
const trendData = ref<TrendItem[]>([])

const pieChartRef = ref<HTMLElement>()
const trendChartRef = ref<HTMLElement>()
let pieChart: echarts.ECharts | null = null
let trendChart: echarts.ECharts | null = null

const currentStat = computed(() => {
  if (viewType.value === 'month') {
    return {
      income: monthlyStat.value?.totalIncome?.toFixed(2) || '0.00',
      expense: monthlyStat.value?.totalExpense?.toFixed(2) || '0.00',
      balance: monthlyStat.value?.balance?.toFixed(2) || '0.00',
      count: monthlyStat.value?.billCount || 0
    }
  } else {
    return {
      income: yearlyData.value?.totalIncome?.toFixed(2) || '0.00',
      expense: yearlyData.value?.totalExpense?.toFixed(2) || '0.00',
      balance: yearlyData.value?.balance?.toFixed(2) || '0.00',
      count: yearlyData.value?.billCount || 0
    }
  }
})

const handleViewTypeChange = () => {
  loadData()
}

const loadData = async () => {
  loading.value = true
  try {
    if (viewType.value === 'month') {
      const [statRes, ratioRes, trendRes] = await Promise.all([
        getMonthlyStat(selectedMonth.value),
        getCategoryRatio(selectedMonth.value, 2),
        getTrend(6)
      ])
      monthlyStat.value = statRes.data
      categoryRatios.value = ratioRes.data
      trendData.value = trendRes.data
    } else {
      const res = await getYearlyStat(selectedYear.value)
      yearlyData.value = res.data
      categoryRatios.value = res.data.categoryBreakdown
      trendData.value = res.data.monthlyDetails.map(m => ({
        month: m.month,
        income: m.totalIncome,
        expense: m.totalExpense
      }))
    }
    await nextTick()
    renderPieChart()
    renderTrendChart()
  } catch (e) {} finally {
    loading.value = false
  }
}

const renderPieChart = () => {
  if (!pieChartRef.value) return
  if (!pieChart) {
    pieChart = echarts.init(pieChartRef.value)
    pieChart.on('click', (params: any) => {
      const ratio = categoryRatios.value.find(r => r.categoryName === params.name)
      if (ratio && ratio.categoryId) {
        let start, end
        if (viewType.value === 'month') {
          start = dayjs(selectedMonth.value).startOf('month').format('YYYY-MM-DD')
          end = dayjs(selectedMonth.value).endOf('month').format('YYYY-MM-DD')
        } else {
          start = `${selectedYear.value}-01-01`
          end = `${selectedYear.value}-12-31`
        }
        router.push({
          path: '/bills',
          query: { categoryId: ratio.categoryId, startDate: start, endDate: end, type: 2 }
        })
      }
    })
  }
  pieChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
    legend: { orient: 'vertical', left: 'left' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 6, borderColor: 'var(--el-bg-color)', borderWidth: 2 },
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
      data: categoryRatios.value.map(item => ({
        name: item.categoryName,
        value: item.amount
      }))
    }]
  })
}

const renderTrendChart = () => {
  if (!trendChartRef.value) return
  if (!trendChart) trendChart = echarts.init(trendChartRef.value)
  
  const isYear = viewType.value === 'year'
  const options: any = {
    tooltip: { trigger: 'axis' },
    legend: { data: ['收入', '支出'] },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: trendData.value.map(t => isYear ? t.month.split('-')[1] + '月' : t.month) },
    yAxis: { type: 'value' },
    series: [
      {
        name: '收入',
        type: isYear ? 'bar' : 'line',
        smooth: true,
        itemStyle: { color: '#67c23a' },
        areaStyle: isYear ? null : { opacity: 0.1 },
        data: trendData.value.map(t => t.income)
      },
      {
        name: '支出',
        type: isYear ? 'bar' : 'line',
        smooth: true,
        itemStyle: { color: '#f56c6c' },
        areaStyle: isYear ? null : { opacity: 0.1 },
        data: trendData.value.map(t => t.expense)
      }
    ]
  }
  trendChart.setOption(options)
}

const handleResize = () => {
  pieChart?.resize()
  trendChart?.resize()
}

onMounted(() => {
  loadData()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  pieChart?.dispose()
  trendChart?.dispose()
})
</script>

<style scoped>
.income { color: #67c23a; }
.expense { color: #f56c6c; }
.balance { font-weight: bold; }
</style>
