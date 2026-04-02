<template>
  <div v-loading="loading">
    <div class="page-header">
      <h2>预算管理</h2>
      <div style="display: flex; gap: 12px; align-items: center;">
        <el-button @click="changeMonth(-1)"><el-icon><ArrowLeft /></el-icon></el-button>
        <span style="font-size: 16px; font-weight: 500; min-width: 120px; text-align: center;">{{ currentMonth }}</span>
        <el-button @click="changeMonth(1)"><el-icon><ArrowRight /></el-icon></el-button>
        <el-button type="primary" @click="showAddDialog = true"><el-icon><Plus /></el-icon> 添加预算</el-button>
      </div>
    </div>

    <!-- 月度总预算 -->
    <el-card style="margin-bottom: 16px;" v-if="budgetData.length > 0">
      <template #header><span>📊 预算概览</span></template>
      <div style="display: flex; justify-content: space-between; margin-bottom: 12px;">
        <span>总预算: <strong>¥{{ totalBudget.toFixed(2) }}</strong></span>
        <span>已使用: <strong :style="{ color: usedPercent >= 100 ? '#F56C6C' : '#303133' }">¥{{ totalUsed.toFixed(2) }}</strong></span>
        <span>剩余: <strong :style="{ color: (totalBudget - totalUsed) < 0 ? '#F56C6C' : '#67C23A' }">¥{{ (totalBudget - totalUsed).toFixed(2) }}</strong></span>
      </div>
      <el-progress
        :percentage="Math.min(Math.round(usedPercent), 100)"
        :color="getProgressColor(usedPercent)"
        :stroke-width="20"
        :text-inside="true"
      />
    </el-card>

    <!-- 分类预算列表 -->
    <el-table :data="budgetData" stripe style="width: 100%;">
      <template #empty>
        <el-empty description="暂未设置预算">
          <el-button type="primary" @click="showAddDialog = true">添加预算</el-button>
        </el-empty>
      </template>
      <el-table-column prop="categoryName" label="分类" width="120" />
      <el-table-column label="预算金额" width="140" align="right">
        <template #default="{ row }">¥{{ row.amount.toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="已用金额" width="140" align="right">
        <template #default="{ row }">
          <span :style="{ color: row.usedAmount > row.amount ? '#F56C6C' : '#303133' }">
            ¥{{ row.usedAmount.toFixed(2) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="使用进度" min-width="200">
        <template #default="{ row }">
          <el-progress
            :percentage="Math.min(Math.round((row.usedAmount / row.amount) * 100), 100)"
            :color="getProgressColor(row.usedAmount / row.amount * 100)"
            :stroke-width="16"
            :text-inside="true"
          />
        </template>
      </el-table-column>
      <el-table-column label="剩余" width="120" align="right">
        <template #default="{ row }">
          <span :style="{ color: (row.amount - row.usedAmount) < 0 ? '#F56C6C' : '#67C23A' }">
            ¥{{ (row.amount - row.usedAmount).toFixed(2) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button text type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
          <el-popconfirm title="确定删除此预算？" @confirm="handleDelete(row.id)">
            <template #reference>
              <el-button text type="danger" size="small">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <!-- 添加/编辑预算弹窗 -->
    <el-dialog v-model="showAddDialog" :title="editingId ? '编辑预算' : '添加预算'" width="460px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="分类" v-if="!editingId">
          <el-select v-model="form.categoryId" placeholder="选择分类" style="width: 100%;" filterable>
            <el-option v-for="cat in expenseCategories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类" v-else>
          <el-input :model-value="editingCategoryName" disabled />
        </el-form-item>
        <el-form-item label="预算金额">
          <el-input-number v-model="form.amount" :min="1" :precision="2" :step="100" style="width: 100%;" placeholder="输入预算金额" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeDialog">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saveLoading">
          {{ editingId ? '保存' : '添加' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { getBudgetList, saveBudget, deleteBudget } from '@/api/budget'
import { listCategories } from '@/api/category'
import type { Budget, Category } from '@/types'
import dayjs from 'dayjs'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const saveLoading = ref(false)
const budgetData = ref<Budget[]>([])
const categories = ref<Category[]>([])
const currentMonth = ref(dayjs().format('YYYY-MM'))
const showAddDialog = ref(false)
const editingId = ref<number | null>(null)
const editingCategoryName = ref('')

const form = reactive({
  categoryId: undefined as number | undefined,
  amount: 0
})

const expenseCategories = computed(() => categories.value.filter(c => c.type === 2))

const totalBudget = computed(() => budgetData.value.reduce((sum, b) => sum + b.amount, 0))
const totalUsed = computed(() => budgetData.value.reduce((sum, b) => sum + b.usedAmount, 0))
const usedPercent = computed(() => totalBudget.value > 0 ? (totalUsed.value / totalBudget.value) * 100 : 0)

const getProgressColor = (percent: number) => {
  if (percent >= 100) return '#F56C6C'
  if (percent >= 80) return '#E6A23C'
  return '#409EFF'
}

const changeMonth = (offset: number) => {
  currentMonth.value = dayjs(currentMonth.value).add(offset, 'month').format('YYYY-MM')
  loadBudgets()
}

const loadBudgets = async () => {
  loading.value = true
  try {
    const res = await getBudgetList(currentMonth.value)
    budgetData.value = (res.data as any[]) || []
  } catch {
    budgetData.value = []
  } finally {
    loading.value = false
  }
}

const loadCategories = async () => {
  try {
    const res = await listCategories(2)
    categories.value = res.data
  } catch {}
}

const handleEdit = (row: Budget) => {
  editingId.value = row.id
  editingCategoryName.value = row.categoryName
  form.categoryId = row.categoryId
  form.amount = row.amount
  showAddDialog.value = true
}

const handleSave = async () => {
  if (!editingId.value && !form.categoryId) {
    ElMessage.warning('请选择分类')
    return
  }
  if (form.amount <= 0) {
    ElMessage.warning('请输入有效预算金额')
    return
  }
  saveLoading.value = true
  try {
    await saveBudget({
      categoryId: form.categoryId,
      month: currentMonth.value,
      amount: form.amount
    })
    ElMessage.success(editingId.value ? '更新成功' : '添加成功')
    closeDialog()
    loadBudgets()
  } catch {} finally {
    saveLoading.value = false
  }
}

const handleDelete = async (id: number) => {
  try {
    await deleteBudget(id)
    ElMessage.success('删除成功')
    loadBudgets()
  } catch {}
}

const closeDialog = () => {
  showAddDialog.value = false
  editingId.value = null
  editingCategoryName.value = ''
  form.categoryId = undefined
  form.amount = 0
}

onMounted(() => {
  loadBudgets()
  loadCategories()
})
</script>
