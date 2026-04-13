<template>
  <div>
    <!-- 预算使用进度 -->
    <el-card v-if="budgetUsage" style="margin-bottom: 16px;">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <span>💰 当月预算</span>
          <el-button text type="primary" @click="$router.push('/budget')">管理预算</el-button>
        </div>
      </template>
      <div style="margin-bottom: 8px; display: flex; justify-content: space-between;">
        <span>总预算: ¥{{ budgetUsage.totalBudget?.toFixed(2) }}</span>
        <span>已使用: ¥{{ budgetUsage.totalUsed?.toFixed(2) }}</span>
      </div>
      <el-progress
        :percentage="budgetPercentage"
        :color="budgetColor"
        :stroke-width="20"
        :text-inside="true"
      />
      <div v-if="budgetPercentage >= 100" style="color: #f56c6c; margin-top: 8px; font-weight: bold;">
        ⚠️ 已超支 ¥{{ (budgetUsage.totalUsed - budgetUsage.totalBudget).toFixed(2) }}！请控制消费
      </div>
    </el-card>
    <el-card v-else-if="budgetLoaded" style="margin-bottom: 16px; text-align: center; padding: 16px;">
      <el-empty description="尚未设置本月预算" :image-size="60">
        <el-button type="primary" @click="$router.push('/budget')">设置预算</el-button>
      </el-empty>
    </el-card>

    <!-- AI 记账输入 -->
    <div class="ai-input-section">
      <h3>🤖 AI 智能记账</h3>
      <p>输入自然语言，AI 自动识别金额、分类、时间，例如："今天午饭35元"、"昨天打车花了28.5"</p>
      <div style="display: flex; gap: 12px;">
        <el-input
          v-model="aiInput"
          placeholder="试试输入：今天午饭35元"
          size="large"
          style="flex: 1;"
          @keyup.enter="handleAiParse"
          :disabled="aiLoading"
        />
        <el-button type="warning" size="large" @click="handleAiParse" :loading="aiLoading" style="min-width: 120px;">
          <el-icon><MagicStick /></el-icon>
          AI 记账
        </el-button>
      </div>
    </div>

    <!-- AI 预览确认弹窗 -->
    <el-dialog v-model="showPreview" title="AI 解析结果" width="520px" :close-on-click-modal="false">
      <!-- 解析失败 -->
      <div v-if="previewResult && !previewResult.success" style="text-align: center; padding: 24px;">
        <el-icon size="48" color="#F56C6C"><CircleClose /></el-icon>
        <p style="margin-top: 12px; color: #606266;">AI 未能识别您的输入，您可以尝试更详细的描述，或点击下方按钮手动记账</p>
        <el-button type="primary" style="margin-top: 16px;" @click="showPreview = false; showManualDialog = true">
          <el-icon><Edit /></el-icon> 手动记账
        </el-button>
      </div>
      <!-- 解析成功：可编辑表单 -->
      <div v-else-if="previewResult">
        <el-form :model="confirmForm" label-width="80px">
          <el-form-item label="类型">
            <el-radio-group v-model="confirmForm.type">
              <el-radio-button :value="2">支出</el-radio-button>
              <el-radio-button :value="1">收入</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="金额" required>
            <el-input-number v-model="confirmForm.amount" :min="0.01" :precision="2" style="width: 100%;" />
          </el-form-item>
          <el-form-item label="分类" required>
            <el-select v-model="confirmForm.categoryId" placeholder="选择分类" style="width: 100%;" filterable>
              <el-option
                v-for="cat in previewCategories"
                :key="cat.id"
                :label="cat.name"
                :value="cat.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="日期">
            <el-date-picker v-model="confirmForm.date" type="date" placeholder="选择日期" style="width: 100%;" value-format="YYYY-MM-DD" />
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="confirmForm.remark" placeholder="备注（选填）" />
          </el-form-item>
        </el-form>
      </div>
      <template #footer v-if="previewResult?.success">
        <el-button @click="showPreview = false">取消</el-button>
        <el-button type="primary" @click="handleAiConfirm" :loading="confirmLoading">确认记账</el-button>
      </template>
    </el-dialog>

    <!-- 今日概览 -->
    <div class="stat-cards">
      <div class="stat-card">
        <div class="label">今日支出</div>
        <div class="value expense">¥ {{ todayExpense }}</div>
      </div>
      <div class="stat-card">
        <div class="label">今日收入</div>
        <div class="value income">¥ {{ todayIncome }}</div>
      </div>
      <div class="stat-card">
        <div class="label">今日笔数</div>
        <div class="value balance">{{ todayCount }}</div>
      </div>
      <div class="stat-card">
        <div class="label">本月支出</div>
        <div class="value expense">¥ {{ monthExpense }}</div>
      </div>
    </div>

    <!-- 手动记账按钮 -->
    <div class="page-header">
      <h2>最近账单</h2>
      <el-button type="primary" @click="showManualDialog = true">
        <el-icon><Plus /></el-icon>
        手动记账
      </el-button>
    </div>

    <!-- 手动记账弹窗 -->
    <el-dialog v-model="showManualDialog" title="手动记账" width="480px">
      <el-form :model="manualForm" label-width="80px">
        <el-form-item label="类型">
          <el-radio-group v-model="manualForm.type">
            <el-radio-button :value="2">支出</el-radio-button>
            <el-radio-button :value="1">收入</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="金额">
          <el-input-number v-model="manualForm.amount" :min="0.01" :precision="2" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="manualForm.categoryId" placeholder="选择分类" style="width: 100%;">
            <el-option
              v-for="cat in filteredCategories"
              :key="cat.id"
              :label="cat.name"
              :value="cat.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker v-model="manualForm.billDate" type="date" placeholder="选择日期" style="width: 100%;" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="manualForm.remark" placeholder="备注（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showManualDialog = false">取消</el-button>
        <el-button type="primary" @click="handleManualCreate" :loading="manualLoading">确认记账</el-button>
      </template>
    </el-dialog>

    <!-- 最近账单列表 -->
    <el-table :data="recentBills" stripe style="width: 100%;" v-loading="billsLoading"
      :row-class-name="({ row }: { row: Bill }) => row.id === highlightId ? 'highlight-row' : ''">
      <template #empty>
        <el-empty description="暂无账单记录" />
      </template>
      <el-table-column prop="billDate" label="日期" width="120" />
      <el-table-column prop="categoryName" label="分类" width="100" />
      <el-table-column prop="remark" label="备注" />
      <el-table-column label="类型" width="80">
        <template #default="{ row }">
          <el-tag :type="row.type === 1 ? 'success' : 'danger'" size="small">
            {{ row.typeName }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="金额" width="120" align="right">
        <template #default="{ row }">
          <span :style="{ color: row.type === 1 ? '#67c23a' : '#f56c6c', fontWeight: 'bold' }">
            {{ row.type === 1 ? '+' : '-' }}¥{{ row.amount }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="方式" width="80">
        <template #default="{ row }">
          <el-tag v-if="row.inputType === 1" type="warning" size="small" effect="plain">AI</el-tag>
          <el-tag v-else size="small" effect="plain">手动</el-tag>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { aiParsePreview, aiConfirmBill, createBill, listBills } from '@/api/bill'
import { listCategories } from '@/api/category'
import { getMonthlyStat } from '@/api/stat'
import { getBudgetUsage } from '@/api/budget'
import type { AiParsePreviewVO, AiConfirmRequest, Bill, Category, MonthlyStat, BudgetUsage } from '@/types'
import dayjs from 'dayjs'
import { ElMessage, ElMessageBox } from 'element-plus'

const aiInput = ref('')
const aiLoading = ref(false)
const previewResult = ref<AiParsePreviewVO | null>(null)
const showPreview = ref(false)
const confirmLoading = ref(false)
const confirmForm = reactive({
  type: 2,
  amount: 0,
  categoryId: undefined as number | undefined,
  date: dayjs().format('YYYY-MM-DD'),
  remark: ''
})

const recentBills = ref<Bill[]>([])
const highlightId = ref<number | null>(null) // M5-05: highlight new bill
const categories = ref<Category[]>([])
const monthStats = ref<MonthlyStat | null>(null)
const billsLoading = ref(false)

// 预算相关
const budgetUsage = ref<BudgetUsage | null>(null)
const budgetLoaded = ref(false)

const showManualDialog = ref(false)
const manualLoading = ref(false)
const manualForm = reactive({
  type: 2,
  amount: 0,
  categoryId: undefined as number | undefined,
  billDate: dayjs().format('YYYY-MM-DD'),
  remark: ''
})

const budgetPercentage = computed(() => {
  if (!budgetUsage.value || budgetUsage.value.totalBudget <= 0) return 0
  return Math.min(Math.round((budgetUsage.value.totalUsed / budgetUsage.value.totalBudget) * 100), 100)
})

const budgetColor = computed(() => {
  if (budgetPercentage.value >= 100) return '#F56C6C'
  if (budgetPercentage.value >= 80) return '#E6A23C'
  return '#409EFF'
})

const previewCategories = computed(() =>
  categories.value.filter(c => c.type === confirmForm.type)
)

const todayExpense = computed(() => {
  const today = dayjs().format('YYYY-MM-DD')
  return recentBills.value
    .filter(b => b.billDate === today && b.type === 2)
    .reduce((sum, b) => sum + b.amount, 0)
    .toFixed(2)
})

const todayIncome = computed(() => {
  const today = dayjs().format('YYYY-MM-DD')
  return recentBills.value
    .filter(b => b.billDate === today && b.type === 1)
    .reduce((sum, b) => sum + b.amount, 0)
    .toFixed(2)
})

const todayCount = computed(() => {
  const today = dayjs().format('YYYY-MM-DD')
  return recentBills.value.filter(b => b.billDate === today).length
})

const monthExpense = computed(() => monthStats.value?.totalExpense?.toFixed(2) || '0.00')

const filteredCategories = computed(() =>
  categories.value.filter(c => c.type === manualForm.type)
)

const checkOverspend = () => {
  if (budgetUsage.value && budgetUsage.value.totalBudget > 0) {
    if (budgetUsage.value.totalUsed > budgetUsage.value.totalBudget) {
      const overAmount = (budgetUsage.value.totalUsed - budgetUsage.value.totalBudget).toFixed(2)
      ElMessageBox.alert(
        `本月已支出 ¥${budgetUsage.value.totalUsed.toFixed(2)}，超出预算 ¥${overAmount}，请注意控制消费！`,
        '⚠️ 预算超支警告',
        { type: 'warning', confirmButtonText: '知道了' }
      )
    } else if (budgetUsage.value.totalUsed / budgetUsage.value.totalBudget >= 0.8) {
      ElMessageBox.alert(
        `本月预算使用已达 ${Math.round(budgetUsage.value.totalUsed / budgetUsage.value.totalBudget * 100)}%，请注意控制消费。`,
        '💡 预算提醒',
        { type: 'info', confirmButtonText: '知道了' }
      )
    }
  }
}

const handleAiParse = async () => {
  if (!aiInput.value.trim()) return
  aiLoading.value = true
  try {
    const res = await aiParsePreview(aiInput.value)
    previewResult.value = res.data
    if (res.data.success) {
      // 预填充确认表单
      confirmForm.amount = res.data.amount || 0
      confirmForm.type = 2 // 默认支出
      confirmForm.date = res.data.date || dayjs().format('YYYY-MM-DD')
      confirmForm.remark = res.data.remark || ''
      confirmForm.categoryId = res.data.categoryId || undefined
    }
    showPreview.value = true
    aiInput.value = ''
  } catch (e) {
    // 错误已在拦截器处理
  } finally {
    aiLoading.value = false
  }
}

const handleAiConfirm = async () => {
  if (!confirmForm.amount || confirmForm.amount <= 0) {
    ElMessage.warning('请输入有效金额')
    return
  }
  if (!confirmForm.categoryId) {
    ElMessage.warning('请选择分类')
    return
  }
  confirmLoading.value = true
  try {
    const data: AiConfirmRequest = {
      amount: confirmForm.amount,
      type: confirmForm.type,
      categoryId: confirmForm.categoryId,
      date: confirmForm.date,
      remark: confirmForm.remark
    }
    await aiConfirmBill(data)
    ElMessage({ message: '✅ 记账成功！', type: 'success', duration: 2000, showClose: true })
    showPreview.value = false
    await loadRecentBills()
    // M5-05: highlight new bill
    const newBill = recentBills.value[0]
    if (newBill) {
      highlightId.value = newBill.id
      setTimeout(() => { highlightId.value = null }, 2000)
    }
    loadBudgetUsage()
    setTimeout(checkOverspend, 1000)
  } catch (e) {
    // 错误已在拦截器处理
  } finally {
    confirmLoading.value = false
  }
}

const handleManualCreate = async () => {
  if (!manualForm.amount || manualForm.amount <= 0) {
    ElMessage.warning('请输入有效金额')
    return
  }
  manualLoading.value = true
  try {
    await createBill(manualForm)
    ElMessage({ message: '✅ 记账成功！', type: 'success', duration: 2000, showClose: true })
    showManualDialog.value = false
    manualForm.amount = 0
    manualForm.categoryId = undefined
    manualForm.remark = ''
    await loadRecentBills()
    // M5-05: highlight new bill
    const newBill = recentBills.value[0]
    if (newBill) {
      highlightId.value = newBill.id
      setTimeout(() => { highlightId.value = null }, 2000)
    }
    loadBudgetUsage()
    setTimeout(checkOverspend, 1000)
  } catch (e) {
    // 错误已在拦截器处理
  } finally {
    manualLoading.value = false
  }
}

const loadRecentBills = async () => {
  billsLoading.value = true
  try {
    const res = await listBills({ pageNum: 1, pageSize: 10 })
    recentBills.value = res.data.records
  } catch (e) {} finally {
    billsLoading.value = false
  }
}

const loadCategories = async () => {
  try {
    const res = await listCategories()
    categories.value = res.data
  } catch (e) {}
}

const loadMonthStats = async () => {
  try {
    const res = await getMonthlyStat(dayjs().format('YYYY-MM'))
    monthStats.value = res.data
  } catch (e) {}
}

const loadBudgetUsage = async () => {
  try {
    const res = await getBudgetUsage(dayjs().format('YYYY-MM'))
    budgetUsage.value = res.data
    budgetLoaded.value = true
  } catch {
    budgetLoaded.value = true
  }
}

onMounted(() => {
  loadRecentBills()
  loadCategories()
  loadMonthStats()
  loadBudgetUsage()
})
</script>

<style scoped>
/* M5-05: Highlight animation for new bill */
:deep(.highlight-row) {
  background-color: #e1f3d8 !important;
  transition: background-color 0.5s ease;
  animation: highlightPulse 2s ease-out forwards;
}
@keyframes highlightPulse {
  0% { background-color: #b3e19d; }
  50% { background-color: #e1f3d8; }
  100% { background-color: transparent; }
}
</style>
