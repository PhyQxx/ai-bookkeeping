<template>
  <div class="home-page">
    <!-- AI 输入区 -->
    <div class="ai-input-section">
      <div class="ai-input-card">
        <h3>🤖 AI 智能记账</h3>
        <p class="ai-hint">试试说："今天午饭花了35元" 或 "昨天打车25块"</p>
        <div class="ai-input-row">
          <el-input
            v-model="aiInput"
            placeholder="用自然语言记一笔..."
            size="large"
            @keyup.enter="handleAiParse"
            :disabled="billStore.aiLoading"
          >
            <template #append>
              <el-button type="primary" :loading="billStore.aiLoading" @click="handleAiParse">
                <el-icon><Promotion /></el-icon> 解析
              </el-button>
            </template>
          </el-input>
        </div>
        <!-- AI解析结果 -->
        <div v-if="billStore.aiResult" class="ai-result">
          <template v-if="billStore.aiResult.success">
            <el-tag :type="billStore.aiResult.type === 1 ? 'success' : 'danger'" size="large">
              {{ billStore.aiResult.type === 1 ? '收入' : '支出' }}
            </el-tag>
            <div class="ai-result-items">
              <div class="ai-result-item">
                <span class="ai-result-label">金额</span>
                <span class="ai-result-value" :class="billStore.aiResult.type === 1 ? 'income-text' : 'expense-text'">
                  ¥{{ billStore.aiResult.amount }}
                </span>
              </div>
              <div class="ai-result-item">
                <span class="ai-result-label">分类</span>
                <span class="ai-result-value">{{ billStore.aiResult.category }}</span>
              </div>
              <div class="ai-result-item">
                <span class="ai-result-label">日期</span>
                <span class="ai-result-value">{{ billStore.aiResult.date }}</span>
              </div>
              <div v-if="billStore.aiResult.remark" class="ai-result-item">
                <span class="ai-result-label">备注</span>
                <span class="ai-result-value">{{ billStore.aiResult.remark }}</span>
              </div>
            </div>
            <el-button type="success" size="small" @click="confirmAiBill" v-if="!billStore.aiResult.billId">
              ✅ 确认记账
            </el-button>
            <el-tag v-else type="success">已自动记账</el-tag>
          </template>
          <template v-else>
            <el-alert :title="billStore.aiResult.errorMessage || '解析失败，请换个说法试试'" type="warning" show-icon :closable="false" />
          </template>
        </div>
      </div>
    </div>

    <!-- 今日概览 -->
    <div class="stat-card-grid">
      <div class="stat-card">
        <div class="label">今日支出</div>
        <div class="value expense-text">¥{{ todayExpense }}</div>
      </div>
      <div class="stat-card">
        <div class="label">今日收入</div>
        <div class="value income-text">¥{{ todayIncome }}</div>
      </div>
      <div class="stat-card">
        <div class="label">今日笔数</div>
        <div class="value">{{ todayCount }}</div>
      </div>
      <div class="stat-card">
        <div class="label">本月支出</div>
        <div class="value expense-text">¥{{ monthExpense }}</div>
      </div>
    </div>

    <!-- 快捷操作 -->
    <div class="quick-actions">
      <el-button type="primary" @click="showBillDialog()">
        <el-icon><EditPen /></el-icon> 手动记账
      </el-button>
      <el-button @click="$router.push('/bills')">
        <el-icon><List /></el-icon> 查看全部账单
      </el-button>
    </div>

    <!-- 最近账单 -->
    <div class="recent-bills">
      <h3 class="section-title">最近账单</h3>
      <div v-if="recentBills.length === 0" class="empty-state">
        <el-empty description="暂无账单，快来记一笔吧" />
      </div>
      <div v-else class="bill-list">
        <div v-for="bill in recentBills" :key="bill.id" class="bill-item">
          <div class="bill-info">
            <el-tag :type="bill.type === 1 ? 'success' : 'danger'" size="small" effect="plain">
              {{ bill.typeName || (bill.type === 1 ? '收入' : '支出') }}
            </el-tag>
            <span class="bill-category">{{ bill.categoryName || '未分类' }}</span>
            <span class="bill-remark">{{ bill.remark }}</span>
            <span class="bill-date">{{ bill.billDate }}</span>
          </div>
          <div class="bill-amount" :class="bill.type === 1 ? 'income-text' : 'expense-text'">
            {{ bill.type === 1 ? '+' : '-' }}¥{{ bill.amount }}
          </div>
        </div>
      </div>
    </div>

    <!-- 手动记账弹窗 -->
    <BillFormDialog
      v-model:visible="billDialogVisible"
      :bill="editingBill"
      @success="onBillSaved"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useBillStore } from '../stores/bill'
import { getMonthlyStat } from '../api/stat'
import { formatMoney, getToday, getCurrentMonth } from '../utils'
import BillFormDialog from '../components/BillFormDialog.vue'
import { Promotion, EditPen, List } from '@element-plus/icons-vue'

const billStore = useBillStore()
const aiInput = ref('')
const billDialogVisible = ref(false)
const editingBill = ref(null)
const todayExpense = ref('0.00')
const todayIncome = ref('0.00')
const todayCount = ref(0)
const monthExpense = ref('0.00')

const recentBills = computed(() => billStore.billList.slice(0, 10))

async function handleAiParse() {
  if (!aiInput.value.trim()) return
  await billStore.parseAi(aiInput.value)
  if (billStore.aiResult?.success && !billStore.aiResult.billId) {
    await billStore.fetchBills({ page: 1, size: 10 })
    await loadStats()
  }
}

async function confirmAiBill() {
  const r = billStore.aiResult
  if (r.billId) return
  await billStore.addBill({
    amount: r.amount,
    type: r.type,
    categoryId: r.categoryId,
    billDate: r.date || getToday(),
    remark: r.remark || ''
  })
  aiInput.value = ''
  billStore.aiResult = null
  await billStore.fetchBills({ page: 1, size: 10 })
  await loadStats()
}

function showBillDialog(bill = null) {
  editingBill.value = bill
  billDialogVisible.value = true
}

async function onBillSaved() {
  billDialogVisible.value = false
  editingBill.value = null
  await billStore.fetchBills({ page: 1, size: 10 })
  await loadStats()
}

async function loadStats() {
  try {
    const stat = await getMonthlyStat(getCurrentMonth())
    if (stat) {
      monthExpense.value = formatMoney(stat.totalExpense)
    }
  } catch {}
}

onMounted(async () => {
  await billStore.fetchBills({ page: 1, size: 10 })
  await loadStats()
  // 计算今日数据
  const today = getToday()
  const todayBills = billStore.billList.filter(b => b.billDate === today)
  todayExpense.value = formatMoney(todayBills.filter(b => b.type === 2).reduce((s, b) => s + Number(b.amount), 0))
  todayIncome.value = formatMoney(todayBills.filter(b => b.type === 1).reduce((s, b) => s + Number(b.amount), 0))
  todayCount.value = todayBills.length
})
</script>

<style scoped>
.ai-input-section {
  margin-bottom: 20px;
}
.ai-input-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}
.ai-input-card h3 {
  margin-bottom: 4px;
}
.ai-hint {
  color: #909399;
  font-size: 13px;
  margin-bottom: 16px;
}
.ai-input-row {
  max-width: 600px;
}
.ai-result {
  margin-top: 16px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
}
.ai-result-items {
  display: flex;
  gap: 24px;
  margin: 12px 0;
  flex-wrap: wrap;
}
.ai-result-item {
  display: flex;
  flex-direction: column;
}
.ai-result-label {
  font-size: 12px;
  color: #909399;
}
.ai-result-value {
  font-size: 16px;
  font-weight: 600;
}
.quick-actions {
  margin-bottom: 20px;
  display: flex;
  gap: 12px;
}
.section-title {
  font-size: 18px;
  margin-bottom: 16px;
}
.bill-list {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  overflow: hidden;
}
.bill-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 20px;
  border-bottom: 1px solid #f0f0f0;
  transition: background 0.2s;
}
.bill-item:hover {
  background: #fafafa;
}
.bill-item:last-child {
  border-bottom: none;
}
.bill-info {
  display: flex;
  align-items: center;
  gap: 12px;
}
.bill-category {
  font-weight: 500;
}
.bill-remark {
  color: #909399;
  font-size: 13px;
}
.bill-date {
  color: #c0c4cc;
  font-size: 12px;
}
.bill-amount {
  font-size: 16px;
  font-weight: 700;
  white-space: nowrap;
}
.empty-state {
  background: #fff;
  border-radius: 12px;
  padding: 40px;
}
</style>
