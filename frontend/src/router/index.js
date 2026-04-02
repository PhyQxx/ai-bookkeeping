import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/LoginView.vue'),
    meta: { guest: true }
  },
  {
    path: '/',
    component: () => import('../views/LayoutView.vue'),
    redirect: '/home',
    children: [
      { path: 'home', name: 'Home', component: () => import('../views/HomeView.vue'), meta: { title: '首页' } },
      { path: 'bills', name: 'Bills', component: () => import('../views/BillListView.vue'), meta: { title: '账单' } },
      { path: 'categories', name: 'Categories', component: () => import('../views/CategoryView.vue'), meta: { title: '分类' } },
      { path: 'stats', name: 'Stats', component: () => import('../views/StatView.vue'), meta: { title: '统计' } },
      { path: 'profile', name: 'Profile', component: () => import('../views/ProfileView.vue'), meta: { title: '我的' } }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  if (to.meta.guest && userStore.isLoggedIn()) {
    next('/')
  } else if (!to.meta.guest && !userStore.isLoggedIn()) {
    next('/login')
  } else {
    next()
  }
})

export default router
