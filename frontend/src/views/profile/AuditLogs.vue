<template>
  <div>
    <div class="page-header">
      <div style="display: flex; align-items: center; gap: 12px;">
        <el-button @click="router.back()" icon="ArrowLeft" circle />
        <h2>操作审计日志</h2>
      </div>
    </div>

    <el-card>
      <el-table :data="logs" stripe v-loading="loading" style="width: 100%;">
        <el-table-column prop="createdAt" label="时间" width="180" />
        <el-table-column prop="module" label="模块" width="120" />
        <el-table-column prop="action" label="操作" width="150" />
        <el-table-column prop="ip" label="IP地址" width="140" />
        <el-table-column prop="detail" label="详细信息" min-width="200" show-overflow-tooltip />
      </el-table>

      <div style="margin-top: 16px; display: flex; justify-content: flex-end;">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadLogs"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listMyAuditLogs, type AuditLog } from '@/api/audit'

const router = useRouter()
const logs = ref<AuditLog[]>([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)

const loadLogs = async () => {
  loading.value = true
  try {
    const res = await listMyAuditLogs({ pageNum: pageNum.value, pageSize: pageSize.value })
    logs.value = res.data.records
    total.value = res.data.total
  } catch (e) {} finally {
    loading.value = false
  }
}

onMounted(loadLogs)
</script>
