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
      </div>
    </nav>

    <!-- Hero 区域 -->
    <section class="hero-section">
      <div class="hero-content animate-fade-in-up">
        <h1 class="hero-title">
          开启您的 <span class="gradient-text">智能学习</span> 之旅
        </h1>
        <p class="hero-subtitle">
          集成 AI 问答、个人云盘、语言学习于一体的智能化全方位学习平台。
        </p>
        <div class="hero-actions">
          <button 
            class="btn-primary-lg" 
            @click="router.push('/chat')"
          >
            <i class="fas fa-rocket" /> 免费开始使用
          </button>
          <a 
            href="/xiaochen_terminal.zip" 
            download
            class="btn-agent-download"
          >
            <i class="fas fa-terminal" /> 下载 Agent 终端助手 (Windows)
          </a>
          <button 
            class="btn-secondary-lg" 
            @click="scrollToFeatures"
          >
            了解更多 <i class="fas fa-chevron-down" />
          </button>
        </div>
        <div class="hero-stats">
          <div class="stat-item">
            <span class="stat-number">24/7</span>
            <span class="stat-label">AI 在线支持</span>
          </div>
          <div class="stat-item">
            <span class="stat-number">100%</span>
            <span class="stat-label">数据安全加密</span>
          </div>
          <div class="stat-item">
            <span class="stat-number">Multi</span>
            <span class="stat-label">多语言学习</span>
          </div>
        </div>
      </div>
      <div class="hero-visual animate-float">
        <div class="visual-card chat-preview">
          <div class="card-header">
            <div class="dot" />
            <div class="dot" />
            <div class="dot" />
          </div>
          <div class="card-body">
            <div class="message ai">
              您好！我是您的 AI 学习助手。有什么我可以帮您的吗？
            </div>
            <div class="message user">
              我想制定一个学习计划。
            </div>
            <div class="message ai typing">
              {{ typingText }}
            </div>
          </div>
        </div>
        <div class="visual-blob" />
      </div>
    </section>

  <!-- 特性展示 -->
  <section 
    id="features" 
    class="features-section"
  >
    <div class="section-header reveal">
      <h2 class="section-title">
        核心功能
      </h2>
      <p class="section-subtitle">为您提供全方位的学习生产力工具</p>
    </div>

    <div class="features-grid">
      <div 
        v-for="(feature, index) in features" 
        :key="index"
        class="feature-card reveal"
        :style="{ transitionDelay: `${index * 150}ms` }"
      >
        <div 
          class="feature-icon" 
          :style="{ backgroundColor: feature.color }"
        >
          <i :class="feature.icon" />
        </div>
        <h3 class="feature-title">
          {{ feature.title }}
        </h3>
        <p class="feature-desc">
          {{ feature.description }}
        </p>
        <ul class="feature-list">
          <li 
            v-for="item in feature.items" 
            :key="item"
          >
            <i class="fas fa-check-circle" /> {{ item }}
          </li>
        </ul>
      </div>
    </div>
  </section>

  <!-- 交互区域：体验 AI -->
  <section class="cta-section reveal">
    <div class="cta-container">
      <div class="cta-content">
        <h2>准备好提升您的效率了吗？</h2>
        <p>加入成千上万的学习者，利用 AI 的力量改变您的学习方式。</p>
        <div class="cta-btns">
          <button 
            class="btn-white" 
            @click="router.push('/register')"
          >
            立即注册账号
          </button>
          <button 
            class="btn-outline-white" 
            @click="router.push('/chat')"
          >
            以游客身份试用
          </button>
        </div>
      </div>
    </div>
  </section>

    <!-- 页脚 -->
    <footer class="landing-footer">
      <div class="footer-container">
        <div class="footer-brand">
          <div class="logo">
            <i class="fas fa-brain"></i>
            <span>AI 智能学习助手</span>
          </div>
          <p>让科技服务于学习，打造您的第二大脑。</p>
        </div>
        <div class="link-group">
          <h4>产品</h4>
          <router-link to="/chat">AI 问答</router-link>
          <router-link to="/cloud-disk">云盘管理</router-link>
          <router-link to="/language-learning">语言学习</router-link>
        </div>
        <div class="link-group">
          <h4>支持</h4>
          <router-link to="/public-files">公共资源</router-link>
          <a href="/xiaochen_terminal.zip" download>下载终端助手 (Win)</a>
          <a href="#">使用文档</a>
          <a href="#">常见问题</a>
        </div>
      </div>
      <div class="footer-bottom">
        <p>&copy; 2026 AI 智能学习助手. All rights reserved.</p>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useThemeStore } from '@/stores/theme'

const router = useRouter()
const authStore = useAuthStore()
const themeStore = useThemeStore()

const isScrolled = ref(false)
const typingText = ref('')
const fullText = '正在为您生成个性化建议...'

const features = [
  {
    title: '智能 AI 问答',
    description: '基于最新大模型的智能助手，为您解答疑惑、总结知识、激发灵感。',
    icon: 'fas fa-comments',
    color: 'rgba(29, 78, 216, 0.1)',
    items: ['多模型支持', '上下文记忆', '流式实时响应']
  },
  {
    title: '个人学习云盘',
    description: '安全可靠的文件存储空间，随时随地访问您的学习资料。',
    icon: 'fas fa-cloud',
    color: 'rgba(16, 185, 129, 0.1)',
    items: ['文件夹管理', '多格式预览', '高速上传下载']
  },
  {
    title: '深度语言学习',
    description: '专为语言学习者设计的工具集，提升词汇量与阅读能力。',
    icon: 'fas fa-language',
    color: 'rgba(245, 158, 11, 0.1)',
    items: ['智能单词库', 'AI 文章生成', '阅读进度追踪']
  },
  {
    title: 'Agent 终端助手 (Windows)',
    description: '功能强大的本地终端助手，专为 Windows 环境优化，提供更深度的系统集成与自动化处理能力。',
    icon: 'fas fa-terminal',
    color: 'rgba(139, 92, 246, 0.1)',
    items: ['本地文件处理', '自动化脚本运行', '高效命令行交互']
  }
]

let typingInterval = null

const startTyping = () => {
  let i = 0
  typingInterval = setInterval(() => {
    typingText.value = fullText.slice(0, i)
    i++
    if (i > fullText.length) {
      i = 0 // 循环播放
    }
  }, 150)
}

const handleScroll = () => {
  isScrolled.value = window.scrollY > 50
  revealOnScroll()
}

const revealOnScroll = () => {
  const reveals = document.querySelectorAll('.reveal')
  reveals.forEach(el => {
    const windowHeight = window.innerHeight
    const elementTop = el.getBoundingClientRect().top
    const elementVisible = 150
    if (elementTop < windowHeight - elementVisible) {
      el.classList.add('active')
    }
  })
}

const scrollToFeatures = () => {
  document.getElementById('features').scrollIntoView({ behavior: 'smooth' })
}

onMounted(() => {
  window.addEventListener('scroll', handleScroll)
  startTyping()
  setTimeout(revealOnScroll, 100) // 初始检查
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
  if (typingInterval) clearInterval(typingInterval)
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

/* 导航栏 */
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
  background-color: var(--bg-primary-transparent, rgba(255, 255, 255, 0.8));
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--border-color);
  height: 64px;
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
  gap: 12px;
  font-size: 1.25rem;
  font-weight: 800;
  color: var(--primary-color);
  cursor: pointer;
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

.nav-actions {
  display: flex;
  align-items: center;
  gap: 1.25rem;
}

/* 按钮样式 */
.btn-login {
  text-decoration: none;
  color: var(--text-primary);
  padding: 0.5rem 1rem;
  font-weight: 600;
  font-size: 0.95rem;
}

.btn-register {
  text-decoration: none;
  background: var(--gradient-primary);
  color: white;
  padding: 0.6rem 1.5rem;
  border-radius: 12px;
  font-weight: 700;
  font-size: 0.95rem;
  transition: all 0.3s;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.2);
}

.btn-register:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.3);
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

/* Hero 区域 */
.hero-section {
  padding: 180px 2rem 120px;
  max-width: 1200px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 5rem;
  align-items: center;
}

.hero-title {
  font-size: 4rem;
  line-height: 1.1;
  margin-bottom: 2rem;
  font-weight: 850;
  letter-spacing: -0.02em;
}

.gradient-text {
  background: linear-gradient(135deg, #3b82f6 0%, #2dd4bf 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.hero-subtitle {
  font-size: 1.35rem;
  color: var(--text-secondary);
  margin-bottom: 3rem;
  line-height: 1.6;
  max-width: 540px;
}

.hero-actions {
  display: flex;
  gap: 1.25rem;
  margin-bottom: 4rem;
}

.btn-primary-lg {
  background: var(--gradient-primary);
  color: white;
  border: none;
  padding: 1.1rem 2.5rem;
  border-radius: 14px;
  font-size: 1.15rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 10px;
  box-shadow: 0 8px 25px rgba(59, 130, 246, 0.25);
}

.btn-primary-lg:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 30px rgba(59, 130, 246, 0.35);
}

.btn-agent-download {
  background: var(--bg-secondary);
  color: var(--text-primary);
  border: 1px solid var(--border-color);
  padding: 1.1rem 2rem;
  border-radius: 14px;
  font-size: 1.15rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
}

.btn-agent-download:hover {
  transform: translateY(-3px);
  background: var(--bg-tertiary);
  border-color: var(--primary-color);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
}

.btn-secondary-lg {
  background: var(--bg-secondary);
  color: var(--text-primary);
  border: 1px solid var(--border-color);
  padding: 1.1rem 2.2rem;
  border-radius: 14px;
  font-size: 1.15rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-secondary-lg:hover {
  background: var(--bg-tertiary);
  border-color: var(--primary-color);
}

.hero-stats {
  display: flex;
  gap: 4rem;
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-number {
  font-size: 1.75rem;
  font-weight: 800;
  color: var(--primary-color);
}

.stat-label {
  font-size: 0.95rem;
  font-weight: 600;
  color: var(--text-tertiary);
}

/* Hero 视觉 */
.hero-visual {
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
}

.visual-card {
  background: var(--bg-primary);
  border-radius: 20px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.15);
  border: 1px solid var(--border-color);
  width: 100%;
  max-width: 420px;
  overflow: hidden;
  position: relative;
  z-index: 2;
  backdrop-filter: blur(20px);
}

.card-header {
  padding: 14px 18px;
  background: var(--bg-secondary);
  display: flex;
  gap: 8px;
  border-bottom: 1px solid var(--border-color);
}

.dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
}

.dot:nth-child(1) { background: #ff5f56; }
.dot:nth-child(2) { background: #ffbd2e; }
.dot:nth-child(3) { background: #27c93f; }

.card-body {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 300px;
}

.message {
  padding: 12px 16px;
  border-radius: 14px;
  max-width: 85%;
  font-size: 0.95rem;
  line-height: 1.5;
}

.message.ai {
  background: var(--bg-secondary);
  color: var(--text-primary);
  align-self: flex-start;
  border-bottom-left-radius: 4px;
}

.message.user {
  background: var(--primary-color);
  color: white;
  align-self: flex-end;
  border-bottom-right-radius: 4px;
}

.typing::after {
  content: '';
  display: inline-block;
  width: 4px;
  height: 14px;
  background: var(--primary-color);
  margin-left: 4px;
  animation: blink 1s infinite;
  vertical-align: middle;
}

.visual-blob {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 140%;
  height: 140%;
  background: radial-gradient(circle, rgba(59, 130, 246, 0.15) 0%, transparent 70%);
  z-index: 1;
  pointer-events: none;
}

/* 特性展示 */
.features-section {
  padding: 120px 2rem;
  max-width: 1200px;
  margin: 0 auto;
}

.section-header {
  text-align: center;
  margin-bottom: 5rem;
}

.section-title {
  font-size: 3rem;
  font-weight: 850;
  margin-bottom: 1.25rem;
  letter-spacing: -0.01em;
}

.section-subtitle {
  font-size: 1.25rem;
  color: var(--text-secondary);
  max-width: 600px;
  margin: 0 auto;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 2.5rem;
}

.feature-card {
  background: var(--bg-secondary);
  padding: 3rem;
  border-radius: 28px;
  border: 1px solid var(--border-color);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.feature-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: var(--gradient-primary);
  opacity: 0;
  transition: opacity 0.3s;
}

.feature-card:hover {
  transform: translateY(-12px);
  box-shadow: 0 20px 40px -15px rgba(0, 0, 0, 0.1);
  border-color: var(--primary-color-transparent);
}

.feature-card:hover::before {
  opacity: 1;
}

.feature-icon {
  width: 72px;
  height: 72px;
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.75rem;
  color: var(--primary-color);
  margin-bottom: 2rem;
  transition: transform 0.3s;
}

.feature-card:hover .feature-icon {
  transform: scale(1.1) rotate(5deg);
}

.feature-title {
  font-size: 1.6rem;
  font-weight: 800;
  margin-bottom: 1.25rem;
}

.feature-desc {
  color: var(--text-secondary);
  line-height: 1.7;
  margin-bottom: 2rem;
  font-size: 1.05rem;
}

.feature-list {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.feature-list li {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 0.95rem;
  font-weight: 600;
  color: var(--text-secondary);
}

.feature-list i {
  color: #10b981;
}

/* CTA 区域 */
.cta-section {
  padding: 120px 2rem;
}

.cta-container {
  max-width: 1200px;
  margin: 0 auto;
  background: linear-gradient(135deg, #1d4ed8 0%, #3b82f6 50%, #60a5fa 100%);
  border-radius: 40px;
  padding: 6rem 4rem;
  text-align: center;
  color: white;
  position: relative;
  overflow: hidden;
  box-shadow: 0 30px 60px -12px rgba(29, 78, 216, 0.4);
}

.cta-container::after {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(255,255,255,0.1) 0%, transparent 70%);
  animation: rotate 20s linear infinite;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.cta-content {
  position: relative;
  z-index: 2;
}

.cta-content h2 {
  font-size: 3.5rem;
  font-weight: 850;
  margin-bottom: 1.5rem;
  letter-spacing: -0.02em;
}

.cta-content p {
  font-size: 1.4rem;
  margin-bottom: 3.5rem;
  opacity: 0.9;
  max-width: 700px;
  margin-left: auto;
  margin-right: auto;
  line-height: 1.5;
}

.cta-btns {
  display: flex;
  justify-content: center;
  gap: 2rem;
}

.btn-white {
  background: white;
  color: var(--primary-color);
  border: none;
  padding: 1.2rem 3rem;
  border-radius: 16px;
  font-weight: 800;
  font-size: 1.2rem;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.15);
}

.btn-white:hover {
  transform: translateY(-5px);
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.2);
}

.btn-outline-white {
  background: transparent;
  color: white;
  border: 2px solid rgba(255, 255, 255, 0.5);
  padding: 1.2rem 3rem;
  border-radius: 16px;
  font-weight: 800;
  font-size: 1.2rem;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-outline-white:hover {
  background: rgba(255, 255, 255, 0.1);
  border-color: white;
  transform: translateY(-5px);
}

/* 页脚 */
.landing-footer {
  background: var(--bg-primary);
  padding: 100px 2rem 50px;
  border-top: 1px solid var(--border-color);
}

.footer-container {
  max-width: 1200px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 1.5fr 1fr 1fr;
  gap: 4rem;
  margin-bottom: 80px;
}

.footer-brand .logo {
  margin-bottom: 1.5rem;
}

.footer-brand p {
  color: var(--text-secondary);
  max-width: 320px;
  line-height: 1.7;
  font-size: 1.05rem;
}

.link-group h4 {
  margin-bottom: 2rem;
  font-size: 1.1rem;
  font-weight: 800;
  color: var(--text-primary);
}

.link-group {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.link-group a {
  text-decoration: none;
  color: var(--text-secondary);
  transition: all 0.2s;
  font-size: 1rem;
  font-weight: 500;
}

.link-group a:hover {
  color: var(--primary-color);
  padding-left: 5px;
}

.footer-bottom {
  text-align: center;
  padding-top: 50px;
  border-top: 1px solid var(--border-color);
  color: var(--text-tertiary);
  font-size: 0.95rem;
  font-weight: 500;
}

/* 动画 */
.reveal {
  opacity: 0;
  transform: translateY(30px);
  transition: all 0.8s cubic-bezier(0.4, 0, 0.2, 1);
}

.reveal.active {
  opacity: 1;
  transform: translateY(0);
}

.animate-fade-in-up {
  animation: fadeInUp 0.8s cubic-bezier(0.4, 0, 0.2, 1) forwards;
}

.animate-float {
  animation: float 8s ease-in-out infinite;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes float {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  33% { transform: translateY(-20px) rotate(2deg); }
  66% { transform: translateY(10px) rotate(-1deg); }
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

/* 响应式 */
@media (max-width: 1024px) {
  .hero-section {
    grid-template-columns: 1fr;
    text-align: center;
    padding-top: 140px;
    gap: 4rem;
  }
  
  .hero-subtitle {
    margin-left: auto;
    margin-right: auto;
  }
  
  .hero-actions, .hero-stats {
    justify-content: center;
  }
  
  .hero-visual {
    max-width: 480px;
    margin: 0 auto;
  }
  
  .features-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .footer-container {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 768px) {
  .nav-links {
    display: none;
  }
  
  .hero-title {
    font-size: 2.8rem;
  }
  
  .features-grid {
    grid-template-columns: 1fr;
  }
  
  .cta-btns {
    flex-direction: column;
    padding: 0 2rem;
  }
  
  .cta-content h2 {
    font-size: 2.5rem;
  }
  
  .footer-container {
    grid-template-columns: 1fr;
    gap: 3rem;
  }
}
</style>
