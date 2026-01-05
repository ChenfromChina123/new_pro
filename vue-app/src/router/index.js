import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: () => import('@/components/AppLayout.vue'),
      children: [
        {
          path: '',
          redirect: '/chat'
        },
        {
          path: 'chat',
          name: 'Chat',
          component: () => import('@/views/ChatView.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'cloud-disk',
          name: 'CloudDisk',
          component: () => import('@/views/CloudDiskView.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'language-learning',
          name: 'LanguageLearning',
          component: () => import('@/views/LanguageLearningView.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'chat-management',
          name: 'ChatManagement',
          component: () => import('@/views/ChatManagementView.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'admin',
          name: 'Admin',
          component: () => import('@/views/AdminView.vue'),
          meta: { requiresAuth: true, requiresAdmin: true }
        },
        {
          path: 'settings',
          name: 'Settings',
          component: () => import('@/views/SettingsView.vue'),
          meta: { requiresAuth: true }
        }
      ]
    },
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/auth/LoginView.vue'),
      meta: { requiresGuest: true }
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('@/views/auth/RegisterView.vue'),
      meta: { requiresGuest: true }
    },
    {
      path: '/forgot-password',
      name: 'ForgotPassword',
      component: () => import('@/views/auth/ForgotPasswordView.vue'),
      meta: { requiresGuest: true }
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'NotFound',
      component: () => import('@/views/NotFoundView.vue')
    }
  ]
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()
  
  const t = authStore.token
  let expired = false
  if (t) {
    try {
      const p = t.split('.')[1]
      const b = p.replace(/-/g, '+').replace(/_/g, '/');
      const json = decodeURIComponent(atob(b).split('').map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)).join(''))
      const payload = JSON.parse(json)
      expired = !!(payload && payload.exp && payload.exp * 1000 <= Date.now())
    } catch (_) {}
  }
  
  if (expired) {
    authStore.logout()
    next({ name: 'Login' })
    return
  }
  
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    next({ name: 'Login' })
  } else if (to.meta.requiresGuest && authStore.isAuthenticated) {
    next({ name: 'Chat' })
  } else if (to.meta.requiresAdmin && !authStore.isAdmin) {
    next({ name: 'Chat' })
  } else {
    next()
  }
})

export default router

