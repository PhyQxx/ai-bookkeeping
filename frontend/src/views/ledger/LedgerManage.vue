<template>
  <div>
    <div class="page-header">
      <h2>账本管理</h2>
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon> 新增账本
      </el-button>
    </div>

    <el-card>
      <el-table :data="ledgers" stripe v-loading="loading" style="width: 100%;">
        <el-table-column prop="name" label="账本名称" min-width="120">
          <template #default="{ row }">
            <span style="font-weight: bold;">{{ row.name }}</span>
            <el-tag v-if="row.isDefault" size="small" type="success" style="margin-left: 8px;">默认</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="说明" min-width="200" />
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button text type="warning" size="small" @click="handleSwitch(row.id)" :disabled="row.id === currentLedgerId">切换</el-button>
            <el-popconfirm title="确定删除此账本及其中所有账单吗？此操作不可恢复！" @confirm="handleDelete(row.id)" :disabled="row.isDefault === 1">
              <template #reference>
                <el-button text type="danger" size="small" :disabled="row.isDefault === 1">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="showDialog" :title="form.id ? '编辑账本' : '新增账本'" width="450px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="如：个人、家庭、出差" maxlength="20" show-word-limit />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="form.description" type="textarea" placeholder="关于此账本的用途描述" rows="3" />
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
import { ref, reactive, onMounted } from 'vue'
import { listLedgers, createLedger, updateLedger, deleteLedger, switchLedger, type Ledger } from '@/api/ledger'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

const userStore = useUserStore()
const ledgers = ref<Ledger[]>([])
const loading = ref(false)
const showDialog = ref(false)
const submitting = ref(false)
const currentLedgerId = ref(userStore.userInfo?.currentLedgerId)

const form = reactive({
  id: undefined as number | undefined,
  name: '',
  description: ''
})

const loadLedgers = async () => {
  loading.value = true
  try {
    const res = await listLedgers()
    ledgers.value = res.data
  } catch (e) {} finally {
    loading.value = false
  }
}

const handleAdd = () => {
  form.id = undefined
  form.name = ''
  form.description = ''
  showDialog.value = true
}

const handleEdit = (row: Ledger) => {
  form.id = row.id
  form.name = row.name
  form.description = row.description || ''
  showDialog.value = true
}

const handleSwitch = async (id: number) => {
  try {
    await switchLedger(id)
    ElMessage.success('账本切换成功')
    if (userStore.userInfo) {
      userStore.userInfo.currentLedgerId = id
      localStorage.setItem('user', JSON.stringify(userStore.userInfo))
    }
    window.location.reload()
  } catch (e) {}
}

const handleSubmit = async () => {
  if (!form.name.trim()) {
    ElMessage.warning('请输入账本名称')
    return
  }
  submitting.value = true
  try {
    if (form.id) {
      await updateLedger(form.id, form)
      ElMessage.success('更新成功')
    } else {
      await createLedger(form)
      ElMessage.success('创建成功')
    }
    showDialog.value = false
    loadLedgers()
  } catch (e) {} finally {
    submitting.value = false
  }
}

const handleDelete = async (id: number) => {
  try {
    await deleteLedger(id)
    ElMessage.success('删除成功')
    loadLedgers()
  } catch (e) {}
}

onMounted(loadLedgers)
</script>
