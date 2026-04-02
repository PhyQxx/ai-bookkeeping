<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">👤 个人中心</h2>
    </div>

    <div class="profile-card">
      <div class="avatar-section">
        <el-avatar :size="80" :src="userStore.avatar || undefined">
          {{ (userStore.nickname || userStore.username || 'U')[0] }}
        </el-avatar>
        <div class="user-info">
          <h3>{{ userStore.nickname || userStore.username }}</h3>
          <p class="user-username">@{{ userStore.username }}</p>
        </div>
      </div>

      <el-divider />

      <div class="info-section">
        <h4>基本信息</h4>
        <el-form label-width="80px" style="max-width:400px">
          <el-form-item label="用户名">
            <el-input :value="userStore.username" disabled />
          </el-form-item>
          <el-form-item label="昵称">
            <el-input v-model="newNickname" placeholder="修改昵称" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="updateNickname">保存修改</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-divider />

      <div class="danger-section">
        <el-button type="danger" @click="userStore.logout()">退出登录</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const newNickname = ref(userStore.nickname || '')

function updateNickname() {
  if (!newNickname.value.trim()) {
    ElMessage.warning('昵称不能为空')
    return
  }
  userStore.nickname = newNickname.value
  localStorage.setItem('user', JSON.stringify({
    username: userStore.username,
    nickname: newNickname.value,
    avatar: userStore.avatar
  }))
  ElMessage.success('昵称修改成功')
}
</script>

<style scoped>
.profile-card {
  background: #fff;
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  max-width: 600px;
}
.avatar-section {
  display: flex;
  align-items: center;
  gap: 20px;
}
.user-info h3 {
  font-size: 20px;
  margin-bottom: 4px;
}
.user-username {
  color: #909399;
  font-size: 14px;
}
.info-section {
  margin-top: 8px;
}
.info-section h4 {
  margin-bottom: 16px;
}
.danger-section {
  margin-top: 16px;
}
</style>
