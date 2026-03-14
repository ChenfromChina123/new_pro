<template>
  <div class="landing-page">
    <!-- 导航栏 -->
    <nav 
      class="landing-nav" 
      :class="{ 'scrolled': isScrolled }"
    >
      <div class="nav-container">
        <div 
          class="logo" 
          @click="router.push('/')"
        >
          <i class="fas fa-brain" />
          <span>AI 智能学习助手</span>
        </div>
        <div class="nav-links">
          <router-link 
            to="/chat" 
            class="nav-link"
          >
            AI 问答
          </router-link>
          <router-link 
            to="/public-files" 
            class="nav-link"
          >
            公共资源
          </router-link>
          <router-link 
            to="/links" 
            class="nav-link"
          >
            资源推荐
          </router-link>
          <router-link 
            to="/codenova" 
            class="nav-link"
          >
            CodeNova
          </router-link>
          <div class="nav-actions">
            <template v-if="!authStore.isAuthenticated">
              <router-link 
                to="/login" 
                class="btn-login"
              >
                登录
              </router-link>
              <router-link 
                to="/register" 
                class="btn-register"
              >
                立即加入
              </router-link>
            </template>
            <template v-else>
              <router-link 
                to="/chat" 
                class="btn-register"
              >
                进入工作台
              </router-link>
            </template>
            <button 
              class="theme-toggle-btn" 
              @click="themeStore.toggleDarkMode()"
            >
              <i :class="themeStore.isDarkMode ? 'fas fa-sun' : 'fas fa-moon'" />
            </button>
          </div>
        </div>

        <!-- 移动端菜单按钮 -->
        <button 
          class="mobile-menu-btn"
          @click="isMobileMenuOpen = !isMobileMenuOpen"
        >
          <i :class="isMobileMenuOpen ? 'fas fa-times' : 'fas fa-bars'" />
        </button>
      </div>

      <!-- 移动端侧边栏菜单 -->
      <transition name="slide">
        <div 
          v-if="isMobileMenuOpen" 
          class="mobile-menu-overlay" 
          @click="isMobileMenuOpen = false"
        >
          <div 
            class="mobile-menu" 
            @click.stop
          >
            <div class="mobile-menu-header">
              <div class="logo">
                <i class="fas fa-brain" />
                <span>AI 学习助手</span>
              </div>
              <button 
                class="close-menu-btn"
                @click="isMobileMenuOpen = false"
              >
                <i class="fas fa-times" />
              </button>
            </div>
            <div class="mobile-menu-body">
              <router-link 
                to="/chat" 
                class="mobile-nav-link"
                @click="isMobileMenuOpen = false"
              >
                <i class="fas fa-comments" />
                <span>AI 问答</span>
              </router-link>
              <router-link 
                to="/public-files" 
                class="mobile-nav-link"
                @click="isMobileMenuOpen = false"
              >
                <i class="fas fa-folder-open" />
                <span>公共资源</span>
              </router-link>
              <router-link 
                to="/links" 
                class="mobile-nav-link"
                @click="isMobileMenuOpen = false"
              >
                <i class="fas fa-link" />
                <span>资源推荐</span>
              </router-link>
              <router-link 
                to="/codenova" 
                class="mobile-nav-link"
                @click="isMobileMenuOpen = false"
              >
                <i class="fas fa-code" />
                <span>CodeNova</span>
              </router-link>
              <div class="mobile-menu-divider" />
              <template v-if="!authStore.isAuthenticated">
                <router-link 
                  to="/login" 
                  class="mobile-nav-link"
                  @click="isMobileMenuOpen = false"
                >
                  <i class="fas fa-sign-in-alt" />
                  <span>登录</span>
                </router-link>
                <router-link 
                  to="/register" 
                  class="mobile-nav-link highlight"
                  @click="isMobileMenuOpen = false"
                >
                  <i class="fas fa-user-plus" />
                  <span>立即加入</span>
                </router-link>
              </template>
              <template v-else>
                <router-link 
                  to="/chat" 
                  class="mobile-nav-link highlight"
                  @click="isMobileMenuOpen = false"
                >
                  <i class="fas fa-desktop" />
                  <span>进入工作台</span>
                </router-link>
              </template>
            </div>
          </div>
        </div>
      </transition>
    </nav>

    <!-- 主内容区域 -->
    <div class="links-page">
      <!-- 头部区域 -->
      <div class="links-header">
        <div class="container">
          <h1 class="page-title">
            <i class="fas fa-link" />
            优质资源推荐
          </h1>
          <p class="page-subtitle">
            精选的外部链接资源，助你高效学习与工作
          </p>
        </div>
      </div>

      <!-- 链接卡片列表 -->
      <div class="links-content">
        <div class="container">
          <!-- 分类选择器 -->
          <div 
            v-if="!loading && !error && links.length > 0"
            class="category-filter"
          >
            <button
              v-for="cat in categories"
              :key="cat"
              class="category-btn"
              :class="{ active: selectedCategory === cat }"
              @click="selectedCategory = cat"
            >
              {{ cat }}
            </button>
          </div>

          <div 
            v-if="loading" 
            class="loading-container"
          >
            <i class="fas fa-spinner fa-spin" />
            <p>加载中...</p>
          </div>

          <div 
            v-else-if="error" 
            class="error-container"
          >
            <i class="fas fa-exclamation-circle" />
            <p>{{ error }}</p>
            <button 
              class="retry-btn" 
              @click="loadLinks"
            >
              重试
            </button>
          </div>

          <div 
            v-else-if="links.length === 0" 
            class="empty-container"
          >
            <i class="fas fa-inbox" />
            <p>暂无资源链接</p>
          </div>

          <!-- 按分类分组显示 -->
          <div 
            v-else
            class="links-sections"
          >
            <div
              v-for="(categoryLinks, categoryName) in linksByCategory"
              :key="categoryName"
              class="category-section"
            >
              <h2 
                v-if="selectedCategory === '全部'"
                class="category-title"
              >
                <i class="fas fa-folder-open" />
                {{ categoryName }}
              </h2>
              
              <div class="links-grid">
                <div
                  v-for="link in categoryLinks"
                  :key="link.id"
                  class="link-card"
                  @click="handleLinkClick(link)"
                >
                  <!-- 图片区域 -->
                  <div 
                    v-if="link.imageUrl" 
                    class="link-image"
                  >
                    <img 
                      :src="link.imageUrl" 
                      :alt="link.title"
                      @error="handleImageError"
                    >
                  </div>
                  
                  <!-- 内容区域 -->
                  <div class="link-content">
                    <h3 class="link-title">
                      {{ link.title }}
                    </h3>
                    
                    <p 
                      v-if="link.description" 
                      class="link-description"
                    >
                      {{ link.description }}
                    </p>
                    
                    <div class="link-meta">
                      <div class="link-url">
                        <i class="fas fa-external-link-alt" />
                        <span>{{ getDomain(link.url) }}</span>
                      </div>
                      <div class="link-clicks">
                        <i class="fas fa-mouse-pointer" />
                        <span>{{ link.clickCount || 0 }} 次点击</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { useAuthStore } from '@/stores/auth'
import { useThemeStore } from '@/stores/theme'

const router = useRouter()
const authStore = useAuthStore()
const themeStore = useThemeStore()

const isScrolled = ref(false)
const isMobileMenuOpen = ref(false)

const links = ref([])
const loading = ref(true)
const error = ref(null)
const selectedCategory = ref('全部')

/**
 * 获取所有分类
 */
const categories = computed(() => {
  const cats = new Set(['全部'])
  links.value.forEach(link => {
    if (link.category) {
      cats.add(link.category)
    }
  })
  return Array.from(cats)
})

/**
 * 过滤后的链接列表
 */
const filteredLinks = computed(() => {
  if (selectedCategory.value === '全部') {
    return links.value
  }
  return links.value.filter(link => link.category === selectedCategory.value)
})

/**
 * 按分类分组的链接
 */
const linksByCategory = computed(() => {
  const grouped = {}
  
  if (selectedCategory.value !== '全部') {
    grouped[selectedCategory.value] = filteredLinks.value
    return grouped
  }
  
  // 按分类分组
  links.value.forEach(link => {
    const cat = link.category || '其他'
    if (!grouped[cat]) {
      grouped[cat] = []
    }
    grouped[cat].push(link)
  })
  
  return grouped
})

/**
 * 监听滚动事件
 */
const handleScroll = () => {
  isScrolled.value = window.scrollY > 50
}

onMounted(() => {
  window.addEventListener('scroll', handleScroll)
  loadLinks()
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})

/**
 * 加载链接列表
 */
const loadLinks = async () => {
  try {
    loading.value = true
    error.value = null
    
    const response = await axios.get('/api/external-links')
    console.log('🔵 后端返回的链接列表:', response.data)
    
    links.value = response.data.data || []
    
    console.log('🔵 加载的链接数量:', links.value.length)
    links.value.forEach(link => {
      console.log(`📊 LinkId: ${link.id}, Title: ${link.title}, ClickCount: ${link.clickCount}`)
    })
  } catch (err) {
    // 静默处理错误，避免在用户界面显示技术细节
    error.value = '加载失败，请稍后重试'
    // 仅在开发环境输出详细错误
    if (import.meta.env.DEV) {
      console.error('加载链接失败:', err)
    }
  } finally {
    loading.value = false
  }
}

/**
 * 处理链接点击，同步记录到后端数据库
 */
const handleLinkClick = async (link) => {
  console.log('🔵 点击链接 - LinkId:', link.id, '当前点击次数:', link.clickCount)
  
  // 立即打开链接，提供更好的用户体验
  window.open(link.url, '_blank', 'noopener,noreferrer')
  
  try {
    // 发送点击记录请求到后端，同步更新数据库
    const response = await axios.post(`/api/external-links/${link.id}/click`)
    console.log('🔵 后端响应:', response.data)
    
    // 后端返回最新的点击次数，直接使用（确保数据一致性）
    const newClickCount = response.data?.data
    console.log('🔵 解析到的新点击次数:', newClickCount, '类型:', typeof newClickCount)
    
    if (typeof newClickCount === 'number') {
      // 更新本地链接的点击次数（使用响应式更新）
      const linkIndex = links.value.findIndex(l => l.id === link.id)
      console.log('🔵 找到的链接索引:', linkIndex)
      
      if (linkIndex !== -1) {
        const oldCount = links.value[linkIndex].clickCount
        links.value[linkIndex].clickCount = newClickCount
        console.log(`✅ 点击次数已更新 - LinkId: ${link.id}, 旧值: ${oldCount}, 新值: ${newClickCount}`)
      }
    } else {
      console.warn('⚠️ 后端返回的点击次数格式不正确:', response.data)
    }
  } catch (err) {
    console.error('❌ 记录点击失败:', err)
    if (err.response) {
      console.error('❌ 错误响应:', err.response.data)
    }
  }
}

/**
 * 获取域名
 */
const getDomain = (url) => {
  try {
    const urlObj = new URL(url)
    return urlObj.hostname
  } catch {
    return url
  }
}

/**
 * 处理图片加载失败
 */
const handleImageError = (e) => {
  e.target.style.display = 'none'
}

onMounted(() => {
  loadLinks()
})
</script>

<style scoped>
.landing-page {
  min-height: 100vh;
  background-color: var(--bg-primary);
  color: var(--text-primary);
  overflow-x: hidden;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
}

/* 导航栏样式 */
.landing-nav {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 72px;
  z-index: 1000;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  padding: 0 2rem;
}

.landing-nav.scrolled {
  background-color: var(--bg-primary-transparent, rgba(255, 255, 255, 0.7));
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border-bottom: 1px solid var(--border-color);
  height: 64px;
  box-shadow: 0 4px 30px rgba(0, 0, 0, 0.03);
}

.nav-container {
  max-width: 1200px;
  margin: 0 auto;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 1.2rem;
  font-weight: 800;
  color: var(--text-primary);
  cursor: pointer;
  letter-spacing: -0.02em;
}

.logo i {
  background: linear-gradient(135deg, #10b981 0%, #3b82f6 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  font-size: 1.4rem;
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 2.5rem;
}

.nav-link {
  text-decoration: none;
  color: var(--text-secondary);
  font-weight: 600;
  font-size: 0.95rem;
  transition: all 0.2s;
}

.nav-link:hover {
  color: var(--primary-color);
}

.nav-link.router-link-active {
  color: var(--primary-color);
}

.nav-actions {
  display: flex;
  align-items: center;
  gap: 1.25rem;
}

.btn-login {
  text-decoration: none;
  color: var(--text-primary);
  padding: 0.5rem 1rem;
  font-weight: 600;
  font-size: 0.95rem;
}

.btn-register {
  text-decoration: none;
  background: linear-gradient(135deg, #10b981 0%, #3b82f6 100%);
  color: white;
  padding: 0.75rem 1.75rem;
  border-radius: 12px;
  font-weight: 600;
  font-size: 0.95rem;
  transition: all 0.3s;
  box-shadow: 0 8px 20px rgba(16, 185, 129, 0.2);
}

.btn-register:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 25px rgba(16, 185, 129, 0.3);
}

.theme-toggle-btn {
  background: var(--bg-tertiary);
  border: 1px solid var(--border-color);
  color: var(--text-primary);
  cursor: pointer;
  font-size: 1.1rem;
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.theme-toggle-btn:hover {
  background: var(--bg-secondary);
  transform: rotate(15deg);
}

/* 移动端菜单 */
.mobile-menu-btn {
  display: none;
  background: var(--bg-tertiary);
  border: 1px solid var(--border-color);
  color: var(--text-primary);
  width: 44px;
  height: 44px;
  border-radius: 10px;
  font-size: 1.2rem;
  cursor: pointer;
  align-items: center;
  justify-content: center;
}

.mobile-menu-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 1001;
  backdrop-filter: blur(4px);
}

.mobile-menu {
  position: fixed;
  top: 0;
  right: 0;
  width: 80%;
  max-width: 320px;
  height: 100vh;
  background: var(--bg-primary);
  box-shadow: -4px 0 20px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  z-index: 1002;
}

.mobile-menu-header {
  padding: 1.5rem;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.close-menu-btn {
  background: var(--bg-tertiary);
  border: 1px solid var(--border-color);
  color: var(--text-primary);
  width: 36px;
  height: 36px;
  border-radius: 8px;
  font-size: 1.1rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.mobile-menu-body {
  flex: 1;
  padding: 1rem 0;
  overflow-y: auto;
}

.mobile-nav-link {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem 1.5rem;
  text-decoration: none;
  color: var(--text-primary);
  font-weight: 600;
  font-size: 1rem;
  transition: all 0.2s;
}

.mobile-nav-link:hover {
  background: var(--bg-secondary);
  color: var(--primary-color);
}

.mobile-nav-link.highlight {
  color: var(--primary-color);
}

.mobile-menu-divider {
  height: 1px;
  background: var(--border-color);
  margin: 1rem 1.5rem;
}

.slide-enter-active,
.slide-leave-active {
  transition: opacity 0.3s;
}

.slide-enter-active .mobile-menu,
.slide-leave-active .mobile-menu {
  transition: transform 0.3s;
}

.slide-enter-from,
.slide-leave-to {
  opacity: 0;
}

.slide-enter-from .mobile-menu,
.slide-leave-to .mobile-menu {
  transform: translateX(100%);
}

/* 链接页面主体 */
.links-page {
  padding-top: 72px;
}

.links-header {
  padding: 2.5rem 2rem 1.5rem;
  text-align: center;
  background: linear-gradient(135deg, var(--bg-secondary) 0%, var(--bg-primary) 100%);
  border-bottom: 1px solid var(--border-color);
}

.page-title {
  font-size: 2rem;
  font-weight: 700;
  margin-bottom: 0.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  color: var(--text-primary);
}

.page-title i {
  font-size: 1.8rem;
  background: linear-gradient(135deg, #10b981 0%, #3b82f6 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.page-subtitle {
  font-size: 1rem;
  color: var(--text-secondary);
  opacity: 0.9;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 1rem;
}

.links-content {
  padding: 2rem 2rem;
  min-height: 60vh;
}

/* 分类过滤器 */
.category-filter {
  display: flex;
  justify-content: center;
  gap: 0.75rem;
  margin-bottom: 2rem;
  flex-wrap: wrap;
}

.category-btn {
  padding: 0.5rem 1rem;
  background: var(--bg-secondary);
  border: 2px solid var(--border-color);
  border-radius: 10px;
  color: var(--text-primary);
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.category-btn:hover {
  border-color: var(--primary-color);
  background: var(--bg-tertiary);
  transform: translateY(-2px);
}

.category-btn.active {
  background: linear-gradient(135deg, #10b981 0%, #3b82f6 100%);
  color: white;
  border-color: transparent;
  box-shadow: 0 4px 15px rgba(16, 185, 129, 0.3);
}

/* 分类区域 */
.links-sections {
  display: flex;
  flex-direction: column;
  gap: 3rem;
}

.category-section {
  animation: fadeIn 0.5s ease-in-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.category-title {
  font-size: 1.4rem;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 1rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding-bottom: 0.5rem;
  border-bottom: 2px solid var(--border-color);
}

.category-title i {
  color: var(--primary-color);
  font-size: 1.2rem;
}

.loading-container,
.error-container,
.empty-container {
  text-align: center;
  padding: 4rem 2rem;
  color: var(--text-secondary);
}

.loading-container i,
.error-container i,
.empty-container i {
  font-size: 4rem;
  margin-bottom: 1rem;
  opacity: 0.5;
  color: var(--text-tertiary);
}

.loading-container p,
.error-container p,
.empty-container p {
  font-size: 1.2rem;
  margin-bottom: 1rem;
  color: var(--text-secondary);
}

.retry-btn {
  padding: 0.75rem 2rem;
  background: linear-gradient(135deg, #10b981 0%, #3b82f6 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.retry-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(16, 185, 129, 0.3);
}

.links-grid {
  display: flex;
  overflow-x: auto;
  gap: 1.5rem;
  padding: 1rem 0.5rem;
  scroll-behavior: smooth;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: none; /* Firefox */
}

.links-grid::-webkit-scrollbar {
  display: none; /* Chrome, Safari, Edge */
}

.link-card {
  flex: 0 0 240px; /* 固定宽度 */
  width: 240px;
  background: var(--bg-primary);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
  position: relative;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.05);
}

.link-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.12);
  border-color: var(--primary-color);
}

.link-image {
  width: 100%;
  height: 120px;
  overflow: hidden;
  background: linear-gradient(135deg, var(--bg-secondary) 0%, var(--bg-tertiary) 100%);
}

.link-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.link-card:hover .link-image img {
  transform: scale(1.05);
}

.link-content {
  padding: 0.8rem;
}

.link-title {
  font-size: 1rem;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 0.4rem;
  line-height: 1.3;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.link-description {
  font-size: 0.85rem;
  color: var(--text-secondary);
  line-height: 1.4;
  margin-bottom: 0.6rem;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  height: 2.4em;
}

.link-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 0.6rem;
  border-top: 1px solid var(--border-color);
  font-size: 0.75rem;
  color: var(--text-tertiary);
}

.link-url,
.link-clicks {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.link-url i,
.link-clicks i {
  font-size: 0.85rem;
}

/* 响应式设计 */
@media (max-width: 992px) {
  .nav-links {
    display: none;
  }

  .mobile-menu-btn {
    display: flex;
  }
}

@media (max-width: 768px) {
  .page-title {
    font-size: 1.75rem;
  }

  .page-title i {
    font-size: 1.5rem;
  }

  .page-subtitle {
    font-size: 0.9rem;
  }
  
  .category-filter {
    gap: 0.5rem;
  }
  
  .category-btn {
    padding: 0.4rem 0.8rem;
    font-size: 0.8rem;
  }
  
  .category-title {
    font-size: 1.2rem;
  }

  .links-grid {
    gap: 1rem;
    padding: 0.5rem 0.25rem;
  }

  .link-card {
    flex: 0 0 200px;
    width: 200px;
    margin: 0;
  }
}

@media (max-width: 480px) {
  .links-header {
    padding: 1.5rem 1rem 1rem;
  }

  .page-title {
    font-size: 1.5rem;
    flex-direction: column;
    gap: 0.5rem;
  }

  .landing-nav {
    padding: 0 1rem;
  }
}
</style>
