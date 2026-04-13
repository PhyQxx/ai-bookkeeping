import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  getDashboardSummary,
  getDashboardTrend,
  getCategoryRanking,
  getBudgetProgress,
  type DashboardSummary,
  type DailyTrend,
  type CategoryRanking,
  type BudgetProgress
} from '@/api/dashboard'

export const useDashboardStore = defineStore('dashboard', () => {
  const summary = ref<DashboardSummary | null>(null)
  const trend = ref<DailyTrend[]>([])
  const categoryRanking = ref<CategoryRanking[]>([])
  const budgetProgress = ref<BudgetProgress[]>([])
  const loading = ref(false)
  const lastFetch = ref(0)
  const STALE_TIME = 5 * 60 * 1000 // 5 min stale-while-revalidate

  const isStale = () => Date.now() - lastFetch.value > STALE_TIME

  async function loadAll(force = false) {
    if (!force && !isStale() && summary.value) return
    loading.value = true
    try {
      const [s, t, c, b] = await Promise.all([
        getDashboardSummary(),
        getDashboardTrend(),
        getCategoryRanking(),
        getBudgetProgress()
      ])
      summary.value = s.data
      trend.value = t.data
      categoryRanking.value = c.data
      budgetProgress.value = b.data
      lastFetch.value = Date.now()
    } finally {
      loading.value = false
    }
  }

  // Stale-while-revalidate: return cached data, refresh in background
  async function loadAllSWR() {
    if (summary.value) {
      // Return cached, refresh in background
      loadAll(true)
      return
    }
    await loadAll()
  }

  function $reset() {
    summary.value = null
    trend.value = []
    categoryRanking.value = []
    budgetProgress.value = []
    lastFetch.value = 0
  }

  return { summary, trend, categoryRanking, budgetProgress, loading, loadAll, loadAllSWR, $reset }
})
