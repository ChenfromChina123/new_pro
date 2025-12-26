<template>
  <header class="app-header">
    <div class="header-container">
      <div class="header-left">
        <div class="logo">
          <span class="logo-text">AI学习助手</span>
        </div>
      </div>
      
      <!-- 移动端汉堡菜单 -->
      <button 
        class="menu-toggle" 
        title="切换菜单"
        aria-label="切换菜单"
        @click="toggleMobileMenu"
      >
        <i class="fas fa-bars" />
      </button>
      
      <div class="header-right">
        <button 
          class="icon-btn" 
          title="切换主题" 
          aria-label="切换主题"
          @click.stop="handleToggleDarkMode"
        >
          <i
            v-if="themeStore.isDarkMode"
            class="fas fa-sun"
          />
          <i
            v-else
            class="fas fa-moon"
          />
        </button>
        
        <div class="user-menu">
          <button
            class="user-btn"
            aria-haspopup="true"
            aria-expanded="false"
          >
            <span class="user-avatar">
              <!-- 添加调试信息 -->
              <img 
                v-if="authStore.userInfo?.avatar" 
                :src="avatarUrl || authStore.userInfo.avatar" 
                :alt="authStore.username" 
                class="avatar-img"
                onerror="console.error('头像加载失败:', this.src)"
              >
              <i
                v-else
                class="fas fa-user"
              />
            </span>
            <span class="user-name">{{ authStore.username || '用户' }}</span>
            <span class="user-caret">
              <i class="fas fa-chevron-down" />
            </span>
          </button>
          <div
            class="user-dropdown"
            role="menu"
          >
            <button 
              class="dropdown-item" 
              role="menuitem" 
              @click="handleLogout"
            >
              <span class="dropdown-icon">
                <i class="fas fa-sign-out-alt" />
              </span>
              <span class="dropdown-text">退出登录</span>
            </button>
          </div>
        </div>
      </div>
      
      <!-- 移除移动端导航菜单，已移至侧边栏 -->
    </div>
  </header>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useThemeStore } from '@/stores/theme'
import { useSettingsStore } from '@/stores/settings'
import { API_CONFIG } from '@/config/api'

const router = useRouter()
const authStore = useAuthStore()
const themeStore = useThemeStore()
const settingsStore = useSettingsStore()
const avatarUrl = ref(null)

/**
 * 处理主题切换并同步到后端
 */
const handleToggleDarkMode = async () => {
  themeStore.toggleDarkMode()
  
  // 同步到后端设置 (如果已登录)
  if (authStore.isLoggedIn) {
    await settingsStore.updateSettings({ 
      theme: themeStore.isDarkMode ? 'dark' : 'light' 
    })
  }
}

// 移动端菜单状态
const isMobileMenuOpen = ref(false)

// 调试：监听用户信息变化
watch(
  () => authStore.userInfo,
  (newInfo) => {
    console.log('用户信息变化:', newInfo)
    console.log('头像URL:', newInfo?.avatar)
  },
  { deep: true }
)

// 初始化时刷新用户信息
onMounted(() => {
  console.log('AppHeader挂载，当前用户信息:', authStore.userInfo)
  // 强制刷新用户信息
  authStore.forceRefreshUserInfo()
})

watch(
  () => authStore.userInfo?.avatar,
  async (path) => {
    if (path) {
      try {
        const res = await fetch(`${API_CONFIG.baseURL}${path}`, {
          headers: { Authorization: `Bearer ${authStore.token}` }
        })
        if (res.ok) {
          const blob = await res.blob()
          avatarUrl.value = URL.createObjectURL(blob)
        } else {
          avatarUrl.value = null
        }
      } catch {
        avatarUrl.value = null
      }
    } else {
      avatarUrl.value = null
    }
  },
  { immediate: true }
)

const handleLogout = () => {
  if (confirm('确定要退出登录吗？')) {
    authStore.logout()
    router.push('/login')
  }
}

const toggleMobileMenu = () => {
  isMobileMenuOpen.value = !isMobileMenuOpen.value
}
</script>

<style scoped>
.app-header {
  background-color: var(--bg-secondary);
  border-bottom: 1px solid var(--border-color);
  box-shadow: var(--shadow-sm);
  position: sticky;
  top: 0;
  z-index: 2001;
  transition: box-shadow 0.2s ease;
}

.app-header:hover {
  box-shadow: var(--shadow-md);
}

.header-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 68px;
}

.header-left {
  display: flex;
  align-items: center;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 700;
  font-size: 18px;
  color: var(--text-primary);
  transition: all 0.2s ease;
}

.logo:hover {
  opacity: 0.9;
}

.logo-icon {
  font-size: 24px;
}

.logo-text {
  letter-spacing: -0.5px;
  background: var(--gradient-primary);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.header-nav {
  display: flex;
  align-items: center;
  gap: 4px;
  flex: 1;
  justify-content: center;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 18px;
  color: var(--text-secondary);
  text-decoration: none;
  border-radius: var(--border-radius-md);
  transition: all 0.2s ease;
  font-size: 14px;
  font-weight: 500;
  position: relative;
}

.nav-item::before {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  width: 0;
  height: 2px;
  background: var(--gradient-primary);
  transition: all 0.2s ease;
  transform: translateX(-50%);
}

.nav-item:hover {
  background-color: var(--bg-tertiary);
  color: var(--primary-color);
}

.nav-item.active {
  background-color: var(--bg-tertiary);
  color: var(--primary-color);
}

.nav-item.active::before {
  width: 30%;
}

.nav-icon {
  font-size: 16px;
}

.nav-text {
  letter-spacing: 0.2px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.icon-btn {
  width: 42px;
  height: 42px;
  border: none;
  background-color: var(--bg-tertiary);
  border-radius: var(--border-radius-md);
  cursor: pointer;
  font-size: 18px;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-secondary);
}

.icon-btn:hover {
  background-color: var(--primary-color);
  color: white;
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
}

.user-menu {
  position: relative;
}

.user-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  border: 1px solid var(--border-color);
  background-color: var(--bg-secondary);
  color: var(--text-primary);
  border-radius: var(--border-radius-md);
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s ease;
  box-shadow: var(--shadow-sm);
}

.user-btn:hover {
  border-color: var(--primary-color);
  background-color: var(--bg-tertiary);
  box-shadow: var(--shadow-md);
  transform: translateY(-1px);
}

.user-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  overflow: hidden;
  background-color: var(--bg-tertiary);
  border: 2px solid var(--border-color);
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
  transition: all 0.2s ease;
}

.avatar-img:hover {
  transform: scale(1.1);
}

.user-avatar i {
  font-size: 18px;
  color: var(--text-secondary);
}

.user-name {
  letter-spacing: 0.2px;
}

.user-caret {
  font-size: 10px;
  color: var(--text-tertiary);
  transition: transform 0.2s ease;
}

.user-menu:hover .user-caret {
  transform: rotate(180deg);
}

.user-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  background-color: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius-md);
  box-shadow: var(--shadow-lg);
  min-width: 180px;
  opacity: 0;
  visibility: hidden;
  transform: translateY(-10px);
  transition: all 0.2s ease;
  z-index: 101;
}

.user-menu:hover .user-dropdown {
  opacity: 1;
  visibility: visible;
  transform: translateY(0);
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 12px 16px;
  border: none;
  background: none;
  color: var(--text-primary);
  text-align: left;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s ease;
  border-radius: 0;
}

.dropdown-item:first-child {
  border-radius: var(--border-radius-md) var(--border-radius-md) 0 0;
}

.dropdown-item:last-child {
  border-radius: 0 0 var(--border-radius-md) var(--border-radius-md);
}

.dropdown-item:hover {
  background-color: var(--bg-tertiary);
  color: var(--danger-color);
}

.dropdown-icon {
  font-size: 14px;
}

.dropdown-text {
  letter-spacing: 0.2px;
}

/* 移动端菜单切换按钮 */
.menu-toggle {
  display: none;
  width: 42px;
  height: 42px;
  border: none;
  background-color: var(--bg-tertiary);
  border-radius: var(--border-radius-md);
  cursor: pointer;
  font-size: 18px;
  transition: all 0.2s ease;
  color: var(--text-secondary);
}

.menu-toggle:hover {
  background-color: var(--primary-color);
  color: white;
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
}

/* 移动端导航容器 */
.mobile-nav {
  position: fixed;
  top: 68px;
  left: 0;
  width: 100%;
  background-color: var(--bg-secondary);
  border-bottom: 1px solid var(--border-color);
  box-shadow: var(--shadow-lg);
  transform: translateY(-100%);
  opacity: 0;
  visibility: hidden;
  transition: all 0.3s ease;
  z-index: 90;
}

.mobile-nav.mobile-nav-open {
  transform: translateY(0);
  opacity: 1;
  visibility: visible;
}

/* 移动端导航菜单 */
.mobile-nav-menu {
  display: flex;
  flex-direction: column;
  gap: 0;
}

/* 移动端导航项 */
.mobile-nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 24px;
  color: var(--text-secondary);
  text-decoration: none;
  transition: all 0.2s ease;
  font-size: 14px;
  font-weight: 500;
  border-bottom: 1px solid var(--border-color);
}

.mobile-nav-item:last-child {
  border-bottom: none;
}

.mobile-nav-item:hover {
  background-color: var(--bg-tertiary);
  color: var(--primary-color);
  padding-left: 28px;
}

.mobile-nav-item.active {
  background-color: var(--bg-tertiary);
  color: var(--primary-color);
  border-left: 3px solid var(--primary-color);
  padding-left: 21px;
}

/* 响应式设计 */
@media (max-width: 992px) {
  .header-nav .nav-text {
    display: none;
  }
  
  .nav-item {
    padding: 10px;
  }
  
  .nav-icon {
    font-size: 18px;
  }
}

@media (max-width: 768px) {
  .header-nav {
    display: none;
  }
  
  .menu-toggle {
    display: flex;
    align-items: center;
    justify-content: center;
  }
  
  .logo {
    font-size: 16px;
  }
  
  .logo-text {
    display: block;
    font-size: 14px;
  }
  
  .user-name {
    display: none;
  }
  
  .user-btn {
    padding: 10px;
  }
}

@media (max-width: 480px) {
  .header-container {
    padding: 0 16px;
  }
  
  .logo-text {
    font-size: 13px;
  }
  
  .nav-icon {
    font-size: 16px;
  }
}
</style>
