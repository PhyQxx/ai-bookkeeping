<template>
  <div v-loading="loading">
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
            <el-avatar :size="80" :src="userStore.userInfo?.avatar || ''" icon="UserFilled" />
            <h3 style="margin-top: 12px;">{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</h3>
            <p style="color: #909399; font-size: 14px; margin-bottom: 20px;">{{ userStore.userInfo?.username }}</p>
            <el-divider />
            <el-button @click="router.push('/audit-logs')" style="width: 100%;">
              <el-icon style="margin-right: 8px;"><List /></el-icon> 操作审计日志
            </el-button>
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
              <el-button type="primary" @click="handleSaveProfile" :loading="profileLoading">保存修改</el-button>
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
              <el-button type="primary" @click="handleChangePassword" :loading="passwordLoading">修改密码</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getUserInfo, updateUserInfo, changePassword } from '@/api/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const profileLoading = ref(false)
const passwordLoading = ref(false)

const profileForm = reactive({
  nickname: '',
  avatar: ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 加载用户信息
const loadUserInfo = async () => {
  loading.value = true
  try {
    const res = await getUserInfo()
    const data = res.data as any
    profileForm.nickname = data.nickname || ''
    profileForm.avatar = data.avatar || ''
    // 更新 store
    if (data.nickname || data.avatar) {
      const updated = { ...userStore.userInfo, ...data }
      userStore.userInfo = updated
      localStorage.setItem('user', JSON.stringify(updated))
    }
  } catch {
    // 接口不可用时使用本地数据
    profileForm.nickname = userStore.userInfo?.nickname || ''
    profileForm.avatar = userStore.userInfo?.avatar || ''
  } finally {
    loading.value = false
  }
}

const handleSaveProfile = async () => {
  profileLoading.value = true
  try {
    await updateUserInfo({
      nickname: profileForm.nickname,
      avatar: profileForm.avatar
    })
    // 更新本地 store
    const updated = { ...userStore.userInfo!, nickname: profileForm.nickname, avatar: profileForm.avatar }
    userStore.userInfo = updated
    localStorage.setItem('user', JSON.stringify(updated))
    ElMessage.success('保存成功')
  } catch {
    // 错误已在拦截器处理
  } finally {
    profileLoading.value = false
  }
}

const handleChangePassword = async () => {
  if (!passwordForm.oldPassword || !passwordForm.newPassword) {
    ElMessage.warning('请填写完整密码信息')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('两次密码不一致')
    return
  }
  if (passwordForm.newPassword.length < 6) {
    ElMessage.warning('新密码至少6位')
    return
  }
  passwordLoading.value = true
  try {
    await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    ElMessage.success('密码修改成功，请重新登录')
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    // 退出登录
    await userStore.logout()
    router.push('/login')
  } catch {
    // 错误已在拦截器处理
  } finally {
    passwordLoading.value = false
  }
}

onMounted(() => loadUserInfo())
</script>
