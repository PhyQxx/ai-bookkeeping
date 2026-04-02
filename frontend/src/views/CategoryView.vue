<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">🏷️ 分类管理</h2>
      <el-button type="primary" @click="showCategoryDialog()">
        <el-icon><Plus /></el-icon> 新增分类
      </el-button>
    </div>

    <el-tabs v-model="activeType" @tab-change="loadCategories">
      <el-tab-pane label="支出分类" name="2" />
      <el-tab-pane label="收入分类" name="1" />
    </el-tabs>

    <div class="category-grid">
      <div v-for="cat in categoryList" :key="cat.id" class="category-card" :class="{ 'system-card': cat.isSystem }">
        <div class="category-icon">{{ cat.icon || '📁' }}</div>
        <div class="category-name">{{ cat.name }}</div>
        <div class="category-actions">
          <el-tag v-if="cat.isSystem" size="small" type="info">系统预设</el-tag>
          <template v-else>
            <el-button link type="primary" @click="showCategoryDialog(cat)">编辑</el-button>
            <el-popconfirm title="确定删除此分类？" @confirm="handleDelete(cat.id)">
              <template #reference>
                <el-button link type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </div>
      </div>
      <div v-if="categoryList.length === 0" class="empty-state">
        <el-empty description="暂无分类" />
      </div>
    </div>

    <!-- 新增/编辑分类弹窗 -->
    <el-dialog v-model="dialogVisible" :title="editingCategory ? '编辑分类' : '新增分类'" width="400px">
      <el-form :model="categoryForm" :rules="categoryRules" ref="categoryRef" label-width="80px">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="categoryForm.name" placeholder="如：餐饮、交通" />
        </el-form-item>
        <el-form-item label="图标" prop="icon">
          <el-input v-model="categoryForm.icon" placeholder="Emoji，如：🍔🚗💰" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="categoryForm.sortOrder" :min="0" :max="999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getCategoryList, createCategory, updateCategory, deleteCategory } from '../api/category'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const activeType = ref('2')
const categoryList = ref([])
const dialogVisible = ref(false)
const editingCategory = ref(null)
const categoryRef = ref()

const categoryForm = reactive({ name: '', icon: '', sortOrder: 0 })
const categoryRules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
}

async function loadCategories() {
  categoryList.value = await getCategoryList(activeType.value) || []
}

function showCategoryDialog(cat = null) {
  editingCategory.value = cat
  if (cat) {
    categoryForm.name = cat.name
    categoryForm.icon = cat.icon || ''
    categoryForm.sortOrder = cat.sortOrder || 0
  } else {
    categoryForm.name = ''
    categoryForm.icon = ''
    categoryForm.sortOrder = 0
  }
  dialogVisible.value = true
}

async function handleSave() {
  await categoryRef.value.validate()
  const data = {
    name: categoryForm.name,
    type: Number(activeType.value),
    icon: categoryForm.icon,
    sortOrder: categoryForm.sortOrder
  }
  if (editingCategory.value) {
    await updateCategory(editingCategory.value.id, data)
    ElMessage.success('更新成功')
  } else {
    await createCategory(data)
    ElMessage.success('创建成功')
  }
  dialogVisible.value = false
  await loadCategories()
}

async function handleDelete(id) {
  await deleteCategory(id)
  ElMessage.success('删除成功')
  await loadCategories()
}

onMounted(() => loadCategories())
</script>

<style scoped>
.category-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 16px;
  margin-top: 16px;
}
.category-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  transition: transform 0.2s;
}
.category-card:hover {
  transform: translateY(-2px);
}
.system-card {
  opacity: 0.7;
}
.category-icon {
  font-size: 32px;
  margin-bottom: 8px;
}
.category-name {
  font-weight: 500;
  margin-bottom: 8px;
}
.category-actions {
  font-size: 12px;
}
.empty-state {
  grid-column: 1 / -1;
}
</style>
