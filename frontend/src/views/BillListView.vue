<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">📄 账单列表</h2>
      <el-button type="primary" @click="showBillDialog()">
        <el-icon><Plus /></el-icon> 记一笔
      </el-button>
    </div>

    <!-- 筛选区 -->
    <div class="filter-section">
      <el-form :inline="true" :model="filters">
        <el-form-item label="类型">
          <el-select v-model="filters.type" placeholder="全部" clearable style="width:120px" @change="loadBills">
            <el-option label="支出" :value="2" />
            <el-option label="收入" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="filters.categoryId" placeholder="全部" clearable style="width:140px" @change="loadBills">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
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
            style="width:260px"
            @change="loadBills"
          />
        </el-form-item>
        <el-form-item label="搜索">
          <el-input v-model="filters.keyword" placeholder="备注搜索" clearable style="width:160px" @keyup.enter="loadBills" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadBills">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 账单列表 -->
    <div class="bill-table-wrapper">
      <el-table :data="billStore.billList" v-loading="billStore.loading" stripe style="width:100%">
        <el-table-column prop="billDate" label="日期" width="120" />
        <el-table-column prop="categoryName" label="分类" width="120">
          <template #default="{ row }">
            {{ row.categoryName || '未分类' }}
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="80">
          <template #default="{ row }">
            <el-tag :type="row.type === 1 ? 'success' : 'danger'" size="small">
              {{ row.typeName || (row.type === 1 ? '收入' : '支出') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="amount" label="金额" width="120">
          <template #default="{ row }">
            <span :class="row.type === 1 ? 'income-text' : 'expense-text'" style="font-weight:600">
              {{ row.type === 1 ? '+' : '-' }}¥{{ row.amount }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="200" show-overflow-tooltip />
        <el-table-column prop="inputType" label="来源" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.inputType === 1" type="warning" size="small" effect="plain">AI</el-tag>
            <el-tag v-else size="small" effect="plain">手动</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="showBillDialog(row)">编辑</el-button>
            <el-popconfirm title="确定删除此账单？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button link type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="filters.page"
          v-model:page-size="filters.size"
          :total="billStore.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @current-change="loadBills"
          @size-change="loadBills"
        />
      </div>
    </div>

    <BillFormDialog
      v-model:visible="billDialogVisible"
      :bill="editingBill"
      @success="onBillSaved"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useBillStore } from '../stores/bill'
import { getCategoryList } from '../api/category'
import BillFormDialog from '../components/BillFormDialog.vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const billStore = useBillStore()
const billDialogVisible = ref(false)
const editingBill = ref(null)
const categories = ref([])

const filters = reactive({
  page: 1,
  size: 20,
  type: null,
  categoryId: null,
  dateRange: null,
  keyword: ''
})

async function loadBills() {
  const params = {
    page: filters.page,
    size: filters.size
  }
  if (filters.type) params.type = filters.type
  if (filters.categoryId) params.categoryId = filters.categoryId
  if (filters.dateRange?.length === 2) {
    params.startDate = filters.dateRange[0]
    params.endDate = filters.dateRange[1]
  }
  if (filters.keyword) params.keyword = filters.keyword
  await billStore.fetchBills(params)
}

function resetFilters() {
  filters.type = null
  filters.categoryId = null
  filters.dateRange = null
  filters.keyword = ''
  filters.page = 1
  loadBills()
}

function showBillDialog(bill = null) {
  editingBill.value = bill
  billDialogVisible.value = true
}

async function handleDelete(id) {
  await billStore.removeBill(id)
  ElMessage.success('删除成功')
  await loadBills()
}

async function onBillSaved() {
  billDialogVisible.value = false
  editingBill.value = null
  await loadBills()
}

onMounted(async () => {
  const [expenseList, incomeList] = await Promise.all([
    getCategoryList(2),
    getCategoryList(1)
  ])
  categories.value = [...(expenseList || []), ...(incomeList || [])]
  await loadBills()
})
</script>

<style scoped>
.filter-section {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}
.bill-table-wrapper {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
