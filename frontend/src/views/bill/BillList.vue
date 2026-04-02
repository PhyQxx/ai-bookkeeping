<template>
  <div>
    <!-- 筛选栏 -->
    <el-card style="margin-bottom: 16px;">
      <el-form :inline="true" :model="filters">
        <el-form-item label="类型">
          <el-select v-model="filters.type" placeholder="全部" clearable style="width: 120px;">
            <el-option label="支出" :value="2" />
            <el-option label="收入" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="filters.categoryId" placeholder="全部" clearable style="width: 140px;">
            <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker
            v-model="filters.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 260px;"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadBills">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 账单表格 -->
    <el-table :data="bills" stripe v-loading="loading" style="width: 100%;">
      <el-table-column prop="billDate" label="日期" width="120" sortable />
      <el-table-column prop="categoryName" label="分类" width="100" />
      <el-table-column prop="remark" label="备注" show-overflow-tooltip />
      <el-table-column label="类型" width="80">
        <template #default="{ row }">
          <el-tag :type="row.type === 1 ? 'success' : 'danger'" size="small">{{ row.typeName }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="金额" width="130" align="right" sortable :sort-method="sortByAmount">
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
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button text type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
          <el-popconfirm title="确定删除？" @confirm="handleDelete(row.id)">
            <template #reference>
              <el-button text type="danger" size="small">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div style="margin-top: 16px; display: flex; justify-content: flex-end;">
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="loadBills"
        @current-change="loadBills"
      />
    </div>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="showEditDialog" title="编辑账单" width="480px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="类型">
          <el-radio-group v-model="editForm.type">
            <el-radio-button :value="2">支出</el-radio-button>
            <el-radio-button :value="1">收入</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="金额">
          <el-input-number v-model="editForm.amount" :min="0.01" :precision="2" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="editForm.categoryId" placeholder="选择分类" style="width: 100%;">
            <el-option v-for="cat in filteredCategories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker v-model="editForm.billDate" type="date" style="width: 100%;" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="editForm.remark" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" @click="handleUpdate" :loading="editLoading">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { listBills, updateBill, deleteBill } from '@/api/bill'
import { listCategories } from '@/api/category'
import type { Bill, Category } from '@/types'
import { ElMessage } from 'element-plus'

const bills = ref<Bill[]>([])
const categories = ref<Category[]>([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)

const filters = reactive({
  type: undefined as number | undefined,
  categoryId: undefined as number | undefined,
  dateRange: null as string[] | null
})

const showEditDialog = ref(false)
const editLoading = ref(false)
const editForm = reactive({
  id: 0,
  type: 2,
  amount: 0,
  categoryId: undefined as number | undefined,
  billDate: '',
  remark: ''
})

const filteredCategories = computed(() =>
  categories.value.filter(c => c.type === editForm.type)
)

const sortByAmount = (a: Bill, b: Bill) => a.amount - b.amount

const loadBills = async () => {
  loading.value = true
  try {
    const params: any = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (filters.type) params.type = filters.type
    if (filters.categoryId) params.categoryId = filters.categoryId
    if (filters.dateRange?.[0]) params.startDate = filters.dateRange[0]
    if (filters.dateRange?.[1]) params.endDate = filters.dateRange[1]
    const res = await listBills(params)
    bills.value = res.data.records
    total.value = res.data.total
  } catch (e) {} finally {
    loading.value = false
  }
}

const resetFilters = () => {
  filters.type = undefined
  filters.categoryId = undefined
  filters.dateRange = null
  pageNum.value = 1
  loadBills()
}

const handleEdit = (row: Bill) => {
  editForm.id = row.id
  editForm.type = row.type
  editForm.amount = row.amount
  editForm.categoryId = row.categoryId
  editForm.billDate = row.billDate
  editForm.remark = row.remark
  showEditDialog.value = true
}

const handleUpdate = async () => {
  editLoading.value = true
  try {
    await updateBill(editForm.id, editForm)
    ElMessage.success('更新成功')
    showEditDialog.value = false
    loadBills()
  } catch (e) {} finally {
    editLoading.value = false
  }
}

const handleDelete = async (id: number) => {
  try {
    await deleteBill(id)
    ElMessage.success('删除成功')
    loadBills()
  } catch (e) {}
}

onMounted(() => {
  loadBills()
  listCategories().then(res => { categories.value = res.data }).catch(() => {})
})
</script>
