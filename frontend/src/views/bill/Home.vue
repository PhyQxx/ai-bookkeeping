<template>
  <div>
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

    <!-- AI 解析结果展示 -->
    <el-dialog v-model="showResult" title="AI 解析结果" width="500px" v-if="parseResult">
      <div v-if="parseResult.success" style="padding: 16px;">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="金额">¥ {{ parseResult.amount }}</el-descriptions-item>
          <el-descriptions-item label="分类">{{ parseResult.category }}</el-descriptions-item>
          <el-descriptions-item label="日期">{{ parseResult.date }}</el-descriptions-item>
          <el-descriptions-item label="备注">{{ parseResult.remark || '无' }}</el-descriptions-item>
        </el-descriptions>
        <div style="margin-top: 16px; text-align: center; color: #67c23a;">
          <el-icon><CircleCheck /></el-icon> 已自动记账成功
        </div>
      </div>
      <div v-else style="text-align: center; color: #f56c6c; padding: 24px;">
        <el-icon size="48"><CircleClose /></el-icon>
        <p style="margin-top: 12px;">{{ parseResult.errorMessage }}</p>
      </div>
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
    <el-table :data="recentBills" stripe style="width: 100%;">
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
import { aiParseBill, createBill, listBills } from '@/api/bill'
import { listCategories } from '@/api/category'
import { getMonthlyStat } from '@/api/stat'
import type { AiParseVO, Bill, Category, MonthlyStat } from '@/types'
import dayjs from 'dayjs'
import { ElMessage } from 'element-plus'

const aiInput = ref('')
const aiLoading = ref(false)
const parseResult = ref<AiParseVO | null>(null)
const showResult = ref(false)
const recentBills = ref<Bill[]>([])
const categories = ref<Category[]>([])
const todayStats = ref<MonthlyStat | null>(null)
const monthStats = ref<MonthlyStat | null>(null)

const showManualDialog = ref(false)
const manualLoading = ref(false)
const manualForm = reactive({
  type: 2,
  amount: 0,
  categoryId: undefined as number | undefined,
  billDate: dayjs().format('YYYY-MM-DD'),
  remark: ''
})

const todayExpense = computed(() => {
  // 简单从最近账单中统计今日支出
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

const handleAiParse = async () => {
  if (!aiInput.value.trim()) return
  aiLoading.value = true
  try {
    const res = await aiParseBill({ input: aiInput.value })
    parseResult.value = res.data
    showResult.value = true
    aiInput.value = ''
    if (res.data.success) {
      ElMessage.success('AI 记账成功！')
      loadRecentBills()
    }
  } catch (e) {
    // 错误已在拦截器处理
  } finally {
    aiLoading.value = false
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
    ElMessage.success('记账成功！')
    showManualDialog.value = false
    manualForm.amount = 0
    manualForm.categoryId = undefined
    manualForm.remark = ''
    loadRecentBills()
  } catch (e) {
    // 错误已在拦截器处理
  } finally {
    manualLoading.value = false
  }
}

const loadRecentBills = async () => {
  try {
    const res = await listBills({ pageNum: 1, pageSize: 10 })
    recentBills.value = res.data.records
  } catch (e) {}
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

onMounted(() => {
  loadRecentBills()
  loadCategories()
  loadMonthStats()
})
</script>
