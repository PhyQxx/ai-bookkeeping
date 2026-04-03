import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'Login', component: () => import('@/views/auth/Login.vue'), meta: { requiresAuth: false } },
    { path: '/register', name: 'Register', component: () => import('@/views/auth/Register.vue'), meta: { requiresAuth: false } },
    {
      path: '/',
      component: () => import('@/components/MainLayout.vue'),
      redirect: '/home',
      meta: { requiresAuth: true },
      children: [
        { path: 'home', name: 'Home', component: () => import('@/views/bill/Home.vue'), meta: { title: '首页' } },
        { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/dashboard/Dashboard.vue'), meta: { title: '数据看板' } },
        { path: 'bills', name: 'Bills', component: () => import('@/views/bill/BillList.vue'), meta: { title: '账单列表' } },
        { path: 'categories', name: 'Categories', component: () => import('@/views/category/CategoryManage.vue'), meta: { title: '分类管理' } },
        { path: 'budget', name: 'Budget', component: () => import('@/views/budget/BudgetManage.vue'), meta: { title: '预算管理' } },
        { path: 'stats', name: 'Stats', component: () => import('@/views/stat/Statistics.vue'), meta: { title: '统计分析' } },
        { path: 'profile', name: 'Profile', component: () => import('@/views/profile/Profile.vue'), meta: { title: '个人中心' } }
      ]
    },
    { path: '/:pathMatch(.*)*', redirect: '/login' }
  ]
})

router.beforeEach((to, _from, next) => {
  const userStore = useUserStore()
  if (to.meta.requiresAuth !== false && !userStore.isLoggedIn()) {
    next('/login')
  } else if ((to.path === '/login' || to.path === '/register') && userStore.isLoggedIn()) {
    next('/home')
  } else {
    next()
  }
})

export default router
