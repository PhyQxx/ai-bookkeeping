<template>
  <div>
    <div class="page-header">
      <h2>周期账单管理</h2>
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon> 新增规则
      </el-button>
    </div>

    <el-card>
      <el-table :data="rules" stripe v-loading="loading" style="width: 100%;">
        <el-table-column prop="remark" label="说明" min-width="150" />
        <el-table-column prop="categoryName" label="分类" width="120" />
        <el-table-column label="频率" width="120">
          <template #default="{ row }">
            <el-tag size="small">{{ formatFrequency(row) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="120" align="right">
          <template #default="{ row }">
            <span :style="{ color: row.type === 1 ? '#67c23a' : '#f56c6c', fontWeight: 'bold' }">
              {{ row.type === 1 ? '+' : '-' }}¥{{ row.amount.toFixed(2) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="lastGeneratedDate" label="最近生成" width="120">
          <template #default="{ row }">{{ row.lastGeneratedDate || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-popconfirm title="确定删除此规则？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button text type="danger" size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="showDialog" :title="form.id ? '编辑规则' : '新增规则'" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="账单类型">
          <el-radio-group v-model="form.type">
            <el-radio :value="2">支出</el-radio>
            <el-radio :value="1">收入</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="金额" required>
          <el-input-number v-model="form.amount" :min="0.01" :precision="2" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="分类" required>
          <el-select v-model="form.categoryId" placeholder="选择分类" style="width: 100%;">
            <el-option
              v-for="cat in filteredCategories"
              :key="cat.id"
              :label="cat.name"
              :value="cat.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="周期频率" required>
          <el-select v-model="form.frequency" style="width: 100%;">
            <el-option label="每天" value="DAILY" />
            <el-option label="每周" value="WEEKLY" />
            <el-option label="每月" value="MONTHLY" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.frequency === 'WEEKLY'" label="重复于">
          <el-select v-model="form.dayOfPeriod" style="width: 100%;">
            <el-option v-for="i in 7" :key="i" :label="`周${i === 7 ? '日' : i}`" :value="i" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.frequency === 'MONTHLY'" label="重复于">
          <el-select v-model="form.dayOfPeriod" style="width: 100%;">
            <el-option v-for="i in 31" :key="i" :label="`${i} 号`" :value="i" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始日期" required>
          <el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="结束日期">
          <el-date-picker v-model="form.endDate" type="date" value-format="YYYY-MM-DD" style="width: 100%;" placeholder="永久有效（选填）" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" placeholder="如：房租、视频会员" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { listRecurringBills, createRecurringBill, updateRecurringBill, deleteRecurringBill } from '@/api/recurring'
import { listCategories } from '@/api/category'
import type { RecurringBill, Category } from '@/types'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'

const rules = ref<RecurringBill[]>([])
const categories = ref<Category[]>([])
const loading = ref(false)
const showDialog = ref(false)
const submitting = ref(false)

const form = reactive({
  id: undefined as number | undefined,
  amount: 0,
  type: 2,
  categoryId: undefined as number | undefined,
  frequency: 'MONTHLY',
  dayOfPeriod: 1,
  startDate: dayjs().format('YYYY-MM-DD'),
  endDate: undefined as string | undefined,
  remark: '',
  status: 1
})

const filteredCategories = computed(() => 
  categories.value.filter(c => c.type === form.type)
)

const loadRules = async () => {
  loading.value = true
  try {
    const res = await listRecurringBills()
    rules.value = res.data
  } catch (e) {} finally {
    loading.value = false
  }
}

const formatFrequency = (row: RecurringBill) => {
  if (row.frequency === 'DAILY') return '每天'
  if (row.frequency === 'WEEKLY') return `每周 (周${row.dayOfPeriod === 7 ? '日' : row.dayOfPeriod})`
  if (row.frequency === 'MONTHLY') return `每月 (${row.dayOfPeriod}号)`
  return row.frequency
}

const handleAdd = () => {
  form.id = undefined
  form.amount = 0
  form.type = 2
  form.categoryId = undefined
  form.frequency = 'MONTHLY'
  form.dayOfPeriod = 1
  form.remark = ''
  showDialog.value = true
}

const handleEdit = (row: RecurringBill) => {
  Object.assign(form, row)
  showDialog.value = true
}

const handleStatusChange = async (row: RecurringBill) => {
  try {
    await updateRecurringBill(row.id, row)
    ElMessage.success(row.status === 1 ? '规则已启用' : '规则已暂停')
  } catch (e) {
    row.status = row.status === 1 ? 0 : 1
  }
}

const handleSubmit = async () => {
  if (!form.amount || !form.categoryId) {
    ElMessage.warning('请填写必要信息')
    return
  }
  submitting.value = true
  try {
    if (form.id) {
      await updateRecurringBill(form.id, form)
      ElMessage.success('更新成功')
    } else {
      await createRecurringBill(form)
      ElMessage.success('创建成功')
    }
    showDialog.value = false
    loadRules()
  } catch (e) {} finally {
    submitting.value = false
  }
}

const handleDelete = async (id: number) => {
  try {
    await deleteRecurringBill(id)
    ElMessage.success('删除成功')
    loadRules()
  } catch (e) {}
}

onMounted(() => {
  loadRules()
  listCategories().then(res => categories.value = res.data)
})
</script>
