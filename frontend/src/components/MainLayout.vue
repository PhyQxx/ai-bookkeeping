<template>
  <div class="main-layout">
    <aside class="sidebar">
      <div class="logo">💰 AI 智能记账</div>
      <el-menu :default-active="activeMenu" background-color="#001529" text-color="#ffffffa6" active-text-color="#fff" router>
        <el-menu-item index="/home"><el-icon><HomeFilled /></el-icon><span>首页</span></el-menu-item>
        <el-menu-item index="/bills"><el-icon><List /></el-icon><span>账单列表</span></el-menu-item>
        <el-menu-item index="/categories"><el-icon><Grid /></el-icon><span>分类管理</span></el-menu-item>
        <el-menu-item index="/budget"><el-icon><Wallet /></el-icon><span>预算管理</span></el-menu-item>
        <el-menu-item index="/stats"><el-icon><DataAnalysis /></el-icon><span>统计分析</span></el-menu-item>
        <el-menu-item index="/profile"><el-icon><User /></el-icon><span>个人中心</span></el-menu-item>
      </el-menu>
    </aside>
    <div class="content-area">
      <header class="top-header">
        <span style="font-size: 16px; font-weight: 500;">{{ $route.meta.title || 'AI 智能记账' }}</span>
        <div style="display: flex; align-items: center; gap: 16px;">
          <span style="color: #606266;">{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</span>
          <el-button type="danger" text @click="handleLogout"><el-icon><SwitchButton /></el-icon> 退出</el-button>
        </div>
      </header>
      <main class="main-content"><router-view /></main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { HomeFilled, List, Grid, DataAnalysis, User, SwitchButton, Wallet } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const activeMenu = computed(() => route.path)

const handleLogout = async () => {
  await userStore.logout()
  router.push('/login')
}
</script>
