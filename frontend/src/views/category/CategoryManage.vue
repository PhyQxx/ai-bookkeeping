<template>
  <div>
    <div class="page-header">
      <h2>分类管理</h2>
      <el-button type="primary" @click="showAddDialog = true">
        <el-icon><Plus /></el-icon>
        新增分类
      </el-button>
    </div>

    <!-- 分类列表 -->
    <el-tabs v-model="activeTab" @tab-change="loadCategories">
      <el-tab-pane label="支出分类" name="2" />
      <el-tab-pane label="收入分类" name="1" />
    </el-tabs>

    <el-table :data="categories" stripe style="width: 100%;">
      <el-table-column prop="name" label="分类名称" />
      <el-table-column prop="icon" label="图标" width="100" />
      <el-table-column prop="sortOrder" label="排序" width="80" />
      <el-table-column label="类型" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.isSystem" type="info" size="small">系统预设</el-tag>
          <el-tag v-else type="success" size="small">自定义</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140">
        <template #default="{ row }">
          <el-button text type="primary" size="small" @click="handleEdit(row)" :disabled="row.isSystem">编辑</el-button>
          <el-popconfirm title="确定删除？" @confirm="handleDelete(row.id)" :disabled="row.isSystem">
            <template #reference>
              <el-button text type="danger" size="small" :disabled="row.isSystem">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="showAddDialog" :title="editingId ? '编辑分类' : '新增分类'" width="420px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称">
          <el-input v-model="form.name" placeholder="分类名称" />
        </el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="form.type" :disabled="!!editingId">
            <el-radio-button :value="2">支出</el-radio-button>
            <el-radio-button :value="1">收入</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="form.icon" placeholder="图标标识（选填）" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeDialog">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saveLoading">
          {{ editingId ? '保存' : '创建' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { listCategories, createCategory, updateCategory, deleteCategory } from '@/api/category'
import type { Category } from '@/types'
import { ElMessage } from 'element-plus'

const categories = ref<Category[]>([])
const activeTab = ref('2')
const showAddDialog = ref(false)
const saveLoading = ref(false)
const editingId = ref<number | null>(null)

const form = reactive({
  name: '',
  type: 2,
  icon: '',
  sortOrder: 0
})

const loadCategories = async () => {
  try {
    const res = await listCategories(Number(activeTab.value))
    categories.value = res.data
  } catch (e) {}
}

const handleEdit = (row: Category) => {
  editingId.value = row.id
  form.name = row.name
  form.type = row.type
  form.icon = row.icon || ''
  form.sortOrder = row.sortOrder || 0
  showAddDialog.value = true
}

const handleSave = async () => {
  if (!form.name) {
    ElMessage.warning('请输入分类名称')
    return
  }
  saveLoading.value = true
  try {
    if (editingId.value) {
      await updateCategory(editingId.value, form)
      ElMessage.success('更新成功')
    } else {
      await createCategory(form)
      ElMessage.success('创建成功')
    }
    closeDialog()
    loadCategories()
  } catch (e) {} finally {
    saveLoading.value = false
  }
}

const handleDelete = async (id: number) => {
  try {
    await deleteCategory(id)
    ElMessage.success('删除成功')
    loadCategories()
  } catch (e) {}
}

const closeDialog = () => {
  showAddDialog.value = false
  editingId.value = null
  form.name = ''
  form.type = Number(activeTab.value)
  form.icon = ''
  form.sortOrder = 0
}

onMounted(() => loadCategories())
</script>
