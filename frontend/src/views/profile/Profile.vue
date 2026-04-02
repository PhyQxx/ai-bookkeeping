<template>
  <div>
    <div class="page-header">
      <h2>个人中心</h2>
    </div>

    <el-row :gutter="24">
      <el-col :span="8">
        <el-card>
          <template #header>
            <span>用户信息</span>
          </template>
          <div style="text-align: center; padding: 20px 0;">
            <el-avatar :size="80" icon="UserFilled" />
            <h3 style="margin-top: 12px;">{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</h3>
            <p style="color: #909399; font-size: 14px;">{{ userStore.userInfo?.username }}</p>
          </div>
        </el-card>
      </el-col>

      <el-col :span="16">
        <el-card>
          <template #header>
            <span>修改信息</span>
          </template>
          <el-form :model="profileForm" label-width="80px" style="max-width: 480px;">
            <el-form-item label="昵称">
              <el-input v-model="profileForm.nickname" placeholder="输入昵称" />
            </el-form-item>
            <el-form-item label="头像">
              <el-input v-model="profileForm.avatar" placeholder="头像URL" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSaveProfile">保存修改</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card style="margin-top: 16px;">
          <template #header>
            <span>修改密码</span>
          </template>
          <el-form :model="passwordForm" label-width="100px" style="max-width: 480px;">
            <el-form-item label="当前密码">
              <el-input v-model="passwordForm.oldPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="新密码">
              <el-input v-model="passwordForm.newPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="确认新密码">
              <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleChangePassword">修改密码</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()

const profileForm = reactive({
  nickname: userStore.userInfo?.nickname || '',
  avatar: userStore.userInfo?.avatar || ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const handleSaveProfile = () => {
  ElMessage.info('个人信息修改功能开发中...')
}

const handleChangePassword = () => {
  if (!passwordForm.oldPassword || !passwordForm.newPassword) {
    ElMessage.warning('请填写完整密码信息')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('两次密码不一致')
    return
  }
  ElMessage.info('密码修改功能开发中...')
}
</script>
