<template>
  <div class="main-layout">
    <aside class="sidebar">
      <div class="logo">💰 AI 智能记账</div>
      <el-menu :default-active="activeMenu" background-color="#001529" text-color="#ffffffa6" active-text-color="#fff" router>
        <el-menu-item index="/home"><el-icon><HomeFilled /></el-icon><span>首页</span></el-menu-item>
        <el-menu-item index="/bills"><el-icon><List /></el-icon><span>账单列表</span></el-menu-item>
        <el-menu-item index="/categories"><el-icon><Grid /></el-icon><span>分类管理</span></el-menu-item>
        <el-menu-item index="/budget"><el-icon><Wallet /></el-icon><span>预算管理</span></el-menu-item>
        <el-menu-item index="/recurring"><el-icon><Timer /></el-icon><span>周期账单</span></el-menu-item>
        <el-menu-item index="/stats"><el-icon><DataAnalysis /></el-icon><span>统计分析</span></el-menu-item>
        <el-menu-item index="/profile"><el-icon><User /></el-icon><span>个人中心</span></el-menu-item>
      </el-menu>
    </aside>
    <div class="content-area">
      <header class="top-header">
        <div style="display: flex; align-items: center; gap: 12px;">
          <span style="font-size: 16px; font-weight: 600;">{{ $route.meta.title || 'AI 智能记账' }}</span>
          <el-divider direction="vertical" />
          <!-- 账本切换器 -->
          <el-dropdown trigger="click" @command="handleLedgerSwitch">
            <span class="ledger-selector">
              <el-icon><FolderOpened /></el-icon>
              {{ ledgers.find(l => l.id === currentLedgerId)?.name || '选择账本' }}
              <el-icon class="el-icon--right"><arrow-down /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-for="l in ledgers" :key="l.id" :command="l.id" :disabled="l.id === currentLedgerId">
                  {{ l.name }} <el-tag v-if="l.isDefault" size="small" type="info" style="margin-left: 8px;">默认</el-tag>
                </el-dropdown-item>
                <el-dropdown-item divided command="manage">
                  <el-icon><Grid /></el-icon> 管理账本
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>

        <div style="display: flex; align-items: center; gap: 16px;">
          <el-dropdown trigger="click">
            <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="item">
              <el-button circle><el-icon><Bell /></el-icon></el-button>
            </el-badge>
            <template #dropdown>
              <div class="notification-box">
                <div class="notification-header">
                  <span>消息通知 ({{ unreadCount }})</span>
                  <el-button type="primary" link size="small" @click="handleReadAll" v-if="unreadCount > 0">全部已读</el-button>
                </div>
                <el-scrollbar max-height="300px">
                  <div v-if="notifications.length === 0" class="empty-notify">暂无新消息</div>
                  <div v-for="item in notifications" :key="item.id" class="notify-item" @click="handleRead(item.id)">
                    <div class="notify-title">{{ item.title }}</div>
                    <div class="notify-content">{{ item.content }}</div>
                    <div class="notify-time">{{ item.createdAt }}</div>
                  </div>
                </el-scrollbar>
              </div>
            </template>
          </el-dropdown>
          <el-button circle @click="toggleTheme">
            <el-icon v-if="isDark"><Sunny /></el-icon>
            <el-icon v-else><Moon /></el-icon>
          </el-button>
          <span style="color: var(--el-text-color-regular);">{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</span>
          <el-button type="danger" text @click="handleLogout"><el-icon><SwitchButton /></el-icon> 退出</el-button>
        </div>
      </header>
      <main class="main-content"><router-view /></main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { HomeFilled, List, Grid, DataAnalysis, User, SwitchButton, Wallet, Moon, Sunny, Timer, Bell, FolderOpened, ArrowDown } from '@element-plus/icons-vue'
import { listUnreadNotifications, markNotificationAsRead, markAllAsRead, type Notification } from '@/api/notification'
import { listLedgers, switchLedger, type Ledger } from '@/api/ledger'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const activeMenu = computed(() => route.path)

const isDark = ref(localStorage.getItem('theme') === 'dark')
const notifications = ref<Notification[]>([])
const unreadCount = computed(() => notifications.value.length)

const ledgers = ref<Ledger[]>([])
const currentLedgerId = ref<number | undefined>(userStore.userInfo?.currentLedgerId)

const fetchLedgers = async () => {
  try {
    const res = await listLedgers()
    ledgers.value = res.data
    // 同时也更新当前选中的 ID
    const activeLedger = ledgers.value.find(l => l.isDefault === 1)
    if (!currentLedgerId.value && activeLedger) {
      currentLedgerId.value = activeLedger.id
    }
  } catch (e) {}
}

const handleLedgerSwitch = async (command: string | number) => {
  if (command === 'manage') {
    router.push('/ledgers')
    return
  }

  const id = Number(command)
  try {
    await switchLedger(id)
    currentLedgerId.value = id
    if (userStore.userInfo) {
      userStore.userInfo.currentLedgerId = id
      localStorage.setItem('user', JSON.stringify(userStore.userInfo))
    }
    // 刷新当前页面以重载所有数据
    window.location.reload()
  } catch (e) {}
}

const fetchNotifications = async () => {
  try {
    const res = await listUnreadNotifications()
    notifications.value = res.data
  } catch (e) {}
}

const handleRead = async (id: number) => {
  await markNotificationAsRead(id)
  await fetchNotifications()
}

const handleReadAll = async () => {
  await markAllAsRead()
  await fetchNotifications()
}

const toggleTheme = () => {
  isDark.value = !isDark.value
  const theme = isDark.value ? 'dark' : 'light'
  localStorage.setItem('theme', theme)
  applyTheme(theme)
}

const applyTheme = (theme: string) => {
  const html = document.documentElement
  if (theme === 'dark') {
    html.classList.add('dark')
  } else {
    html.classList.remove('dark')
  }
}

onMounted(() => {
  const savedTheme = localStorage.getItem('theme') || 'light'
  applyTheme(savedTheme)
  fetchNotifications()
  // 每 30 秒轮询一次消息
  setInterval(fetchNotifications, 30000)
})

const handleLogout = async () => {
  await userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.main-layout {
  display: flex;
  height: 100vh;
}

.sidebar {
  width: 240px;
  background-color: #001529;
  display: flex;
  flex-direction: column;
  transition: all 0.3s;
}

.logo {
  height: 64px;
  line-height: 64px;
  text-align: center;
  color: #fff;
  font-size: 20px;
  font-weight: bold;
  background-color: #002140;
}

.content-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.ledger-selector {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 12px;
  border-radius: 4px;
  background-color: var(--el-fill-color-light);
  color: var(--el-text-color-primary);
  font-size: 14px;
  transition: all 0.3s;
}
.ledger-selector:hover {
  background-color: var(--el-fill-color);
}

/* 消息通知样式 */
.notification-box {
  width: 320px;
  padding: 12px 0;
}
.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 16px 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  font-weight: bold;
  font-size: 14px;
}
.notify-item {
  padding: 12px 16px;
  cursor: pointer;
  border-bottom: 1px solid var(--el-border-color-extra-light);
  transition: background 0.3s;
}
.notify-item:hover {
  background-color: var(--el-fill-color-light);
}
.notify-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 4px;
  color: var(--el-text-color-primary);
}
.notify-content {
  font-size: 12px;
  color: var(--el-text-color-regular);
  line-height: 1.6;
  margin-bottom: 4px;
}
.notify-time {
  font-size: 11px;
  color: var(--el-text-color-placeholder);
}
.empty-notify {
  padding: 40px 0;
  text-align: center;
  color: var(--el-text-color-placeholder);
  font-size: 13px;
}
</style>
