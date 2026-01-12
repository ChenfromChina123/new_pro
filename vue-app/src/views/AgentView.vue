<template>
  <div class="agent-standalone-page">
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
          <router-link to="/chat" class="nav-link">AI 问答</router-link>
          <router-link to="/public-files" class="nav-link">公共资源</router-link>
          
          <div class="nav-actions">
            <template v-if="!authStore.isAuthenticated">
              <router-link to="/login" class="btn-login">登录</router-link>
              <router-link to="/register" class="btn-register">立即加入</router-link>
            </template>
            <template v-else>
              <router-link to="/chat" class="btn-register">进入工作台</router-link>
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
        <div v-if="isMobileMenuOpen" class="mobile-menu-overlay" @click="isMobileMenuOpen = false">
          <div class="mobile-menu" @click.stop>
            <div class="mobile-menu-links">
              <router-link to="/chat" class="mobile-nav-link" @click="isMobileMenuOpen = false">
                <i class="fas fa-comments" /> AI 问答
              </router-link>
              <router-link to="/public-files" class="mobile-nav-link" @click="isMobileMenuOpen = false">
                <i class="fas fa-folder-open" /> 公共资源
              </router-link>
              <div class="mobile-menu-divider" />
              <template v-if="!authStore.isAuthenticated">
                <router-link to="/login" class="mobile-nav-link" @click="isMobileMenuOpen = false">
                  <i class="fas fa-sign-in-alt" /> 登录
                </router-link>
                <router-link to="/register" class="mobile-nav-link highlight" @click="isMobileMenuOpen = false">
                  <i class="fas fa-user-plus" /> 立即加入
                </router-link>
              </template>
              <template v-else>
                <router-link to="/chat" class="mobile-nav-link highlight" @click="isMobileMenuOpen = false">
                  <i class="fas fa-rocket" /> 进入工作台
                </router-link>
              </template>
            </div>
            <div class="mobile-menu-footer">
              <button class="mobile-theme-toggle" @click="themeStore.toggleDarkMode()">
                <i :class="themeStore.isDarkMode ? 'fas fa-sun' : 'fas fa-moon'" />
                {{ themeStore.isDarkMode ? '切换浅色模式' : '切换深色模式' }}
              </button>
            </div>
          </div>
        </div>
      </transition>
    </nav>

    <!-- Hero Section -->
    <header class="hero-section">
      <div class="hero-content animate-fade-in-up">
        <div class="badge">智能终端体</div>
        <h1 class="hero-title">
          小晨 <span class="gradient-text">终端助手</span>
        </h1>
        <p class="hero-subtitle">
          一个强大的 AI 终端助手，支持多种 LLM 模型，提供智能的命令行交互体验。专为开发者打造的生产力工具。
        </p>
        <div class="hero-actions">
          <a href="/xiaochen_terminal.zip" download class="btn-primary-lg">
            <i class="fas fa-download"></i> 立即下载
          </a>
          <button class="btn-secondary-lg" @click="scrollToFeatures">
            了解功能 <i class="fas fa-chevron-down" />
          </button>
        </div>
        <div class="hero-stats">
          <div class="stat-item">
            <span class="stat-number">LLM</span>
            <span class="stat-label">多模型支持</span>
          </div>
          <div class="stat-item">
            <span class="stat-number">Undo</span>
            <span class="stat-label">多级回滚系统</span>
          </div>
          <div class="stat-item">
            <span class="stat-number">OCR</span>
            <span class="stat-label">智能视觉识别</span>
          </div>
        </div>
      </div>
      <div class="hero-visual animate-float">
        <div class="visual-card terminal-mockup-wrapper">
          <div class="terminal-mockup">
            <div class="terminal-header">
              <span class="dot red"></span>
              <span class="dot yellow"></span>
              <span class="dot green"></span>
              <span class="terminal-title">xiaochen-agent --bash</span>
            </div>
            <div class="terminal-body">
              <div class="line"><span class="prompt">$</span> agent</div>
              <div class="line success">✓ 已加载会话: 20260113_项目优化.json</div>
              <div class="line">小晨助手 > 请帮我分析当前目录下的项目结构并生成 README。</div>
              <div class="line info">正在扫描目录...</div>
              <div class="line">AI > 好的，我已经识别到这是一个 Vue 3 项目。正在为您生成 README.md...</div>
              <div class="line success">[1/1] ✍️ 写入: README.md</div>
              <div class="line"><span class="prompt">$</span> <span class="cursor">_</span></div>
            </div>
          </div>
        </div>
        <div class="visual-blob" />
      </div>
    </header>

    <!-- Features Grid -->
    <section id="features" class="features-section">
      <div class="section-header reveal">
        <h2 class="section-title">核心特性</h2>
        <p class="section-subtitle">集成多项前沿技术，重塑您的终端使用体验</p>
      </div>
      <div class="features-grid">
        <div 
          v-for="(feature, index) in features" 
          :key="index" 
          class="feature-card reveal"
          :style="{ transitionDelay: `${index * 150}ms` }"
        >
          <div class="feature-icon" :style="{ backgroundColor: feature.color }">
            <i :class="feature.icon"></i>
          </div>
          <h3 class="feature-title">{{ feature.title }}</h3>
          <p class="feature-desc">{{ feature.description }}</p>
          <ul class="feature-list">
            <li v-for="item in feature.items" :key="item">
              <i class="fas fa-check-circle"></i> {{ item }}
            </li>
          </ul>
        </div>
      </div>
    </section>

    <!-- Usage Guide -->
    <section class="guide-section">
      <div class="container">
        <div class="section-header reveal">
          <h2 class="section-title">使用指南</h2>
          <p class="section-subtitle">简单几步，即可开启智能开发之旅</p>
        </div>
        
        <div class="guide-steps">
          <div class="step-item reveal">
            <div class="step-number">01</div>
            <div class="step-content">
              <h3>快速安装</h3>
              <p>下载压缩包并解压，根据您的操作系统运行 <code>scripts/install</code> 目录下的安装脚本。</p>
            </div>
          </div>
          <div class="step-item reveal" style="transition-delay: 150ms;">
            <div class="step-number">02</div>
            <div class="step-content">
              <h3>配置模型</h3>
              <p>首次启动程序会提示输入 API Key，支持 DeepSeek、豆包等主流模型。配置自动保存至 <code>config.json</code>。</p>
            </div>
          </div>
          <div class="step-item reveal" style="transition-delay: 300ms;">
            <div class="step-number">03</div>
            <div class="step-content">
              <h3>开始对话</h3>
              <p>在终端直接输入需求，AI 将智能理解意图并调用工具执行任务，如文件读写、命令执行等。</p>
            </div>
          </div>
        </div>

        <div class="commands-table-container reveal">
          <div class="commands-table">
            <h3>常用命令参考</h3>
            <div class="table-wrapper">
              <table>
                <thead>
                  <tr>
                    <th>命令</th>
                    <th>功能描述</th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <td><code>save [name]</code></td>
                    <td>保存当前会话，支持自定义名称以便后续加载</td>
                  </tr>
                  <tr>
                    <td><code>load &lt;id&gt;</code></td>
                    <td>加载指定的历史会话，恢复上下文环境</td>
                  </tr>
                  <tr>
                    <td><code>sessions</code></td>
                    <td>列出所有历史会话记录及其 ID</td>
                  </tr>
                  <tr>
                    <td><code>rollback</code> / <code>undo</code></td>
                    <td>智能撤销上一次文件系统操作，支持多级回退</td>
                  </tr>
                  <tr>
                    <td><code>ps</code> / <code>watch</code></td>
                    <td>实时监控后台异步任务进程的运行状态</td>
                  </tr>
                  <tr>
                    <td><code>help</code></td>
                    <td>获取详细的命令使用手册与参数说明</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Rollback System Highlight -->
    <section class="rollback-section reveal">
      <div class="container">
        <div class="rollback-layout">
          <div class="rollback-content">
            <div class="badge-dark">特色功能</div>
            <h2 class="gradient-text">专业级回退系统</h2>
            <p>内置强大的版本控制模块，为您提供多级文件回退、快照管理和版本对比功能。即使 AI 误操作，您也可以一键恢复项目状态。</p>
            <div class="rollback-features">
              <div class="r-item">
                <i class="fas fa-history"></i>
                <span>多级历史</span>
              </div>
              <div class="r-item">
                <i class="fas fa-code-branch"></i>
                <span>自动快照</span>
              </div>
              <div class="r-item">
                <i class="fas fa-search-plus"></i>
                <span>差异对比</span>
              </div>
            </div>
          </div>
          <div class="rollback-visual">
            <div class="code-window">
              <div class="code-header">
                <span class="dot red"></span>
                <span class="dot yellow"></span>
                <span class="dot green"></span>
              </div>
              <pre><code><span class="c-prompt">小晨助手 ></span> undo
<span class="c-success">✓ 已撤销上一次对话涉及的所有 3 个文件修改</span>
<span class="c-success">✓ 已恢复到对话前的快照状态 [ID: snap_20260113]</span></code></pre>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Footer -->
    <footer class="landing-footer">
      <div class="footer-container">
        <div class="footer-brand">
          <div class="logo" @click="router.push('/')">
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
          <router-link to="/agent">Agent 终端助手</router-link>
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
const isMobileMenuOpen = ref(false)

/**
 * 处理滚动事件，更新导航栏状态并触发元素显现动画
 */
const handleScroll = () => {
  isScrolled.value = window.scrollY > 50
  revealOnScroll()
}

/**
 * 检查页面上的 .reveal 元素是否进入可视区域，并添加激活类
 */
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

/**
 * 平滑滚动到特性展示区域
 */
const scrollToFeatures = () => {
  const el = document.getElementById('features')
  if (el) {
    el.scrollIntoView({ behavior: 'smooth' })
  }
}

onMounted(() => {
  window.addEventListener('scroll', handleScroll)
  setTimeout(revealOnScroll, 100)
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})

const features = [
  {
    title: 'AI 智能核心',
    description: '深度集成 DeepSeek、豆包等大模型，智能理解复杂指令并执行任务。',
    icon: 'fas fa-robot',
    color: 'rgba(59, 130, 246, 0.1)',
    items: ['多模型无缝切换', '长文本上下文压缩', '工具调用可视化']
  },
  {
    title: '增强型回滚',
    description: '独立的文件版本控制系统，支持多级备份与一键快照恢复。',
    icon: 'fas fa-undo-alt',
    color: 'rgba(16, 185, 129, 0.1)',
    items: ['版本差异对比', '智能旧版清理', '项目快照管理']
  },
  {
    title: '终端进程管理',
    description: '实时监控和管理后台长运行任务，支持交互式输入。',
    icon: 'fas fa-terminal',
    color: 'rgba(245, 158, 11, 0.1)',
    items: ['简单进程 ID', '实时输出同步', 'Ctrl+C 中断保护']
  },
  {
    title: '智能 OCR 工具',
    description: '内置高性能 OCR 识别，支持图片和多页 PDF 文档内容提取。',
    icon: 'fas fa-eye',
    color: 'rgba(139, 92, 246, 0.1)',
    items: ['多并发识别', 'PDF 页码指定', '结果自动存储']
  }
]
</script>

<style scoped>
.agent-standalone-page {
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
  background-color: var(--bg-primary-transparent);
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

/* 移动端菜单按钮 */
.mobile-menu-btn {
  display: none;
  background: none;
  border: none;
  color: var(--text-primary);
  font-size: 1.5rem;
  cursor: pointer;
  z-index: 1100;
}

/* Hero Section */
.hero-section {
  padding: 180px 2rem 120px;
  max-width: 1200px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 5rem;
  align-items: center;
  position: relative;
}

.hero-section::before {
  content: '';
  position: absolute;
  top: -10%;
  right: -5%;
  width: 600px;
  height: 600px;
  background: radial-gradient(circle, rgba(59, 130, 246, 0.15) 0%, transparent 70%);
  filter: blur(80px);
  z-index: -1;
}

.hero-section::after {
  content: '';
  position: absolute;
  bottom: 0%;
  left: -5%;
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, rgba(45, 212, 191, 0.1) 0%, transparent 70%);
  filter: blur(60px);
  z-index: -1;
}

.hero-title {
  font-size: 4.5rem;
  line-height: 1.1;
  margin-bottom: 2rem;
  font-weight: 900;
  letter-spacing: -0.03em;
}

.gradient-text {
  background: linear-gradient(135deg, #3b82f6 0%, #2dd4bf 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  display: inline-block;
}

.hero-subtitle {
  font-size: 1.4rem;
  color: var(--text-secondary);
  margin-bottom: 3.5rem;
  line-height: 1.6;
  max-width: 560px;
  opacity: 0.9;
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
  text-decoration: none;
}

.btn-primary-lg:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 30px rgba(59, 130, 246, 0.35);
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
  gap: 3rem;
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

.badge {
  display: inline-block;
  padding: 0.5rem 1rem;
  background-color: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
  border-radius: 9999px;
  font-size: 0.875rem;
  font-weight: 600;
  margin-bottom: 1.5rem;
}

/* Hero Visual - Terminal Mockup */
.hero-visual {
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
}

.terminal-mockup-wrapper {
  width: 100%;
  max-width: 500px;
  z-index: 2;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.3);
  background: #1e1e1e;
  border: 1px solid #333;
}

.terminal-mockup {
  font-family: 'Fira Code', 'Consolas', monospace;
}

.terminal-header {
  background-color: #333;
  padding: 0.75rem 1rem;
  display: flex;
  align-items: center;
  gap: 8px;
}

.dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
}

.dot.red { background: #ff5f56; }
.dot.yellow { background: #ffbd2e; }
.dot.green { background: #27c93f; }

.terminal-title {
  color: #999;
  font-size: 0.8rem;
  margin-left: 8px;
}

.terminal-body {
  padding: 1.5rem;
  color: #d4d4d4;
  font-size: 0.9rem;
  line-height: 1.6;
}

.line {
  margin-bottom: 4px;
}

.prompt { color: #3b82f6; font-weight: bold; margin-right: 8px; }
.success { color: #4ade80; }
.info { color: #60a5fa; }
.cursor {
  display: inline-block;
  width: 8px;
  height: 15px;
  background: #fff;
  animation: blink 1s infinite;
  vertical-align: middle;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

/* Features Grid */
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
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
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
  padding: 0;
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

/* Usage Guide */
.guide-section {
  padding: 100px 2rem;
  background: var(--bg-secondary-transparent);
}

.guide-steps {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 3rem;
  margin-bottom: 5rem;
}

.step-item {
  position: relative;
}

.step-number {
  font-size: 4rem;
  font-weight: 900;
  color: var(--primary-color);
  opacity: 0.1;
  line-height: 1;
  margin-bottom: -1.5rem;
}

.step-content h3 {
  font-size: 1.5rem;
  margin-bottom: 1rem;
}

.step-content p {
  color: var(--text-secondary);
  line-height: 1.6;
}

.commands-table-container {
  background: var(--bg-primary);
  border-radius: 24px;
  padding: 3rem;
  border: 1px solid var(--border-color);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.05);
}

.commands-table h3 {
  margin-bottom: 2rem;
  font-size: 1.75rem;
  text-align: center;
}

.table-wrapper {
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th {
  text-align: left;
  padding: 1.25rem;
  border-bottom: 2px solid var(--border-color);
  color: var(--text-tertiary);
  font-weight: 700;
}

td {
  padding: 1.25rem;
  border-bottom: 1px solid var(--border-color);
}

code {
  background: var(--bg-tertiary);
  padding: 0.2rem 0.5rem;
  border-radius: 6px;
  color: var(--primary-color);
  font-family: monospace;
}

/* Rollback System */
.rollback-section {
  padding: 120px 2rem;
}

.rollback-layout {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 5rem;
  align-items: center;
}

.badge-dark {
  display: inline-block;
  padding: 0.5rem 1rem;
  background: var(--bg-tertiary);
  border-radius: 99px;
  font-size: 0.85rem;
  font-weight: 700;
  margin-bottom: 1.5rem;
}

.rollback-content h2 {
  font-size: 3rem;
  margin-bottom: 1.5rem;
}

.rollback-content p {
  font-size: 1.2rem;
  color: var(--text-secondary);
  margin-bottom: 2.5rem;
  line-height: 1.7;
}

.rollback-features {
  display: flex;
  gap: 2.5rem;
}

.r-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.r-item i {
  font-size: 1.5rem;
  color: var(--primary-color);
}

.r-item span {
  font-weight: 600;
  font-size: 0.9rem;
}

.code-window {
  background: #1e1e1e;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.3);
  border: 1px solid #333;
}

.code-header {
  background: #333;
  padding: 10px 15px;
  display: flex;
  gap: 8px;
}

.code-window pre {
  padding: 2rem;
  margin: 0;
  font-family: 'Fira Code', monospace;
  font-size: 0.95rem;
}

.c-prompt { color: #3b82f6; }
.c-success { color: #4ade80; }

/* Reveal Animation */
.reveal {
  opacity: 0;
  transform: translateY(30px);
  transition: all 0.8s cubic-bezier(0.4, 0, 0.2, 1);
}

.reveal.active {
  opacity: 1;
  transform: translateY(0);
}

/* Mobile Menu */
.mobile-menu-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
  z-index: 1050;
}

.mobile-menu {
  position: absolute;
  top: 0;
  right: 0;
  width: 280px;
  height: 100%;
  background: var(--bg-primary);
  padding: 80px 2rem 2rem;
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.mobile-nav-link {
  display: flex;
  align-items: center;
  gap: 12px;
  text-decoration: none;
  color: var(--text-primary);
  font-size: 1.1rem;
  font-weight: 600;
  padding: 0.75rem 0;
}

.mobile-nav-link.highlight {
  color: var(--primary-color);
}

/* Footer */
.landing-footer {
  background: var(--bg-secondary);
  padding: 80px 2rem 40px;
  border-top: 1px solid var(--border-color);
}

.footer-container {
  max-width: 1200px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 2fr 1fr 1fr;
  gap: 4rem;
  margin-bottom: 4rem;
}

.footer-brand .logo {
  margin-bottom: 1.5rem;
}

.footer-brand p {
  color: var(--text-secondary);
  max-width: 300px;
}

.link-group h4 {
  margin-bottom: 1.5rem;
  font-size: 1.1rem;
}

.link-group a {
  display: block;
  text-decoration: none;
  color: var(--text-secondary);
  margin-bottom: 0.75rem;
  transition: color 0.2s;
}

.link-group a:hover {
  color: var(--primary-color);
}

.footer-bottom {
  max-width: 1200px;
  margin: 0 auto;
  padding-top: 2rem;
  border-top: 1px solid var(--border-color);
  text-align: center;
  color: var(--text-tertiary);
  font-size: 0.9rem;
}

/* Animations */
.animate-fade-in-up {
  animation: fadeInUp 0.8s cubic-bezier(0.4, 0, 0.2, 1) forwards;
}

.animate-float {
  animation: float 6s ease-in-out infinite;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(40px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-20px); }
}

/* Responsive */
@media (max-width: 1024px) {
  .hero-section {
    grid-template-columns: 1fr;
    text-align: center;
    padding-top: 140px;
    gap: 4rem;
  }
  
  .hero-content {
    display: flex;
    flex-direction: column;
    align-items: center;
  }
  
  .hero-title {
    font-size: 3.5rem;
  }
  
  .hero-subtitle {
    margin-left: auto;
    margin-right: auto;
  }
  
  .hero-actions {
    justify-content: center;
  }
  
  .hero-stats {
    justify-content: center;
  }
  
  .features-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .guide-steps {
    grid-template-columns: 1fr;
    gap: 2rem;
  }
  
  .rollback-layout {
    grid-template-columns: 1fr;
    gap: 3rem;
  }
}

@media (max-width: 768px) {
  .nav-links, .nav-actions {
    display: none;
  }
  
  .mobile-menu-btn {
    display: block;
  }
  
  .hero-title {
    font-size: 2.75rem;
  }
  
  .hero-actions {
    flex-direction: column;
    width: 100%;
    max-width: 300px;
  }
  
  .features-grid {
    grid-template-columns: 1fr;
  }
  
  .footer-container {
    grid-template-columns: 1fr;
    gap: 2.5rem;
  }
}
  gap: 0.5rem;
}

.dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
}

.dot.red { background-color: #ff5f56; }
.dot.yellow { background-color: #ffbd2e; }
.dot.green { background-color: #27c93f; }

.terminal-title {
  color: #999;
  font-size: 0.75rem;
  margin-left: 0.5rem;
}

.terminal-body {
  padding: 1.5rem;
  font-size: 0.9rem;
  color: #d4d4d4;
  line-height: 1.6;
}

.prompt { color: #3b82f6; font-weight: bold; }
.success { color: #4ade80; }
.info { color: #60a5fa; }
.cursor {
  display: inline-block;
  width: 8px;
  height: 1.2rem;
  background-color: #fff;
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
  background: radial-gradient(circle, rgba(45, 212, 191, 0.15) 0%, transparent 70%);
  z-index: 1;
  pointer-events: none;
}

/* Features Grid */
.features-section {
  padding: 100px 2rem;
  max-width: 1200px;
  margin: 0 auto;
}

.section-header {
  text-align: center;
  margin-bottom: 4rem;
}

.section-title {
  font-size: 2.5rem;
  font-weight: 850;
  margin-bottom: 1rem;
}

.section-subtitle {
  font-size: 1.1rem;
  color: var(--text-secondary);
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 2rem;
}

.feature-card {
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  padding: 2.5rem;
  border-radius: 24px;
  transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
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
  border-color: var(--primary-color);
}

.feature-card:hover::before {
  opacity: 1;
}

.feature-icon {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.5rem;
  color: var(--primary-color);
  margin-bottom: 1.5rem;
  transition: transform 0.3s;
}

.feature-card:hover .feature-icon {
  transform: scale(1.1) rotate(5deg);
}

.feature-title {
  font-size: 1.4rem;
  font-weight: 800;
  margin-bottom: 1rem;
}

.feature-desc {
  color: var(--text-secondary);
  line-height: 1.6;
  margin-bottom: 1.5rem;
  font-size: 1rem;
}

.feature-list {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.feature-list li {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--text-secondary);
}

.feature-list i {
  color: #10b981;
}

/* Usage Guide */
.guide-section {
  padding: 100px 2rem;
  background-color: var(--bg-tertiary);
}

.container {
  max-width: 1200px;
  margin: 0 auto;
}

.guide-steps {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 3rem;
  margin-bottom: 5rem;
}

.step-item {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.step-number {
  font-size: 3rem;
  font-weight: 900;
  color: var(--primary-color);
  opacity: 0.2;
  line-height: 1;
}

.step-content h3 {
  font-size: 1.5rem;
  font-weight: 800;
  margin-bottom: 1rem;
}

.step-content p {
  color: var(--text-secondary);
  line-height: 1.6;
}

.step-content code {
  background: var(--bg-secondary);
  padding: 0.2rem 0.4rem;
  border-radius: 4px;
  font-family: 'Fira Code', monospace;
  font-size: 0.9rem;
}

.commands-table-container {
  background: var(--bg-primary);
  border-radius: 24px;
  padding: 3rem;
  border: 1px solid var(--border-color);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.05);
}

.commands-table h3 {
  font-size: 1.5rem;
  font-weight: 800;
  margin-bottom: 2rem;
  text-align: center;
}

.table-wrapper {
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th {
  text-align: left;
  padding: 1rem;
  border-bottom: 2px solid var(--border-color);
  font-weight: 700;
  color: var(--text-primary);
}

td {
  padding: 1rem;
  border-bottom: 1px solid var(--border-color);
  color: var(--text-secondary);
}

td code {
  background: var(--bg-secondary);
  padding: 0.2rem 0.4rem;
  border-radius: 4px;
  font-family: 'Fira Code', monospace;
  font-size: 0.85rem;
  color: var(--primary-color);
}

/* Rollback Section */
.rollback-section {
  padding: 120px 2rem;
  background-color: #0f172a;
  color: white;
}

.rollback-layout {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 5rem;
  align-items: center;
}

.badge-dark {
  display: inline-block;
  padding: 0.5rem 1.25rem;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: #fff;
  border-radius: 9999px;
  font-size: 0.875rem;
  font-weight: 600;
  margin-bottom: 2rem;
}

.rollback-content h2 {
  font-size: 3.5rem;
  font-weight: 850;
  margin-bottom: 2rem;
}

.rollback-content p {
  font-size: 1.2rem;
  line-height: 1.7;
  color: #94a3b8;
  margin-bottom: 3rem;
}

.rollback-features {
  display: flex;
  gap: 2.5rem;
}

.r-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.r-item i {
  font-size: 1.5rem;
  color: #3b82f6;
}

.r-item span {
  font-size: 0.95rem;
  font-weight: 600;
}

.code-window {
  background: #1e293b;
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid #334155;
  box-shadow: 0 30px 60px -12px rgba(0, 0, 0, 0.4);
}

.code-header {
  background: #334155;
  padding: 12px 18px;
  display: flex;
  gap: 8px;
}

.code-header .dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.code-window pre {
  padding: 2rem;
  margin: 0;
  font-family: 'Fira Code', monospace;
  font-size: 0.95rem;
  line-height: 1.7;
}

.c-prompt { color: #94a3b8; }
.c-success { color: #4ade80; }

/* Footer */
.landing-footer {
  background-color: var(--bg-primary);
  padding: 80px 2rem 40px;
  border-top: 1px solid var(--border-color);
}

.footer-container {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  gap: 4rem;
  margin-bottom: 60px;
}

.footer-brand {
  max-width: 300px;
}

.footer-brand p {
  color: var(--text-secondary);
  margin-top: 1.5rem;
  line-height: 1.6;
}

.link-group h4 {
  font-size: 1.1rem;
  font-weight: 700;
  margin-bottom: 1.5rem;
}

.link-group {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.link-group a {
  text-decoration: none;
  color: var(--text-secondary);
  transition: color 0.2s;
}

.link-group a:hover {
  color: var(--primary-color);
}

.footer-bottom {
  max-width: 1200px;
  margin: 0 auto;
  padding-top: 40px;
  border-top: 1px solid var(--border-color);
  text-align: center;
  color: var(--text-tertiary);
  font-size: 0.9rem;
}

/* Animations */
.animate-fade-in-up {
  animation: fadeInUp 0.8s cubic-bezier(0.4, 0, 0.2, 1) forwards;
}

.animate-float {
  animation: float 6s ease-in-out infinite;
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(30px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-20px); }
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

/* Scroll Reveal */
.reveal {
  opacity: 0;
  transform: translateY(30px);
  transition: all 0.8s cubic-bezier(0.4, 0, 0.2, 1);
}

.reveal.active {
  opacity: 1;
  transform: translateY(0);
}

/* Mobile Responsive */
.mobile-menu-btn {
  display: none;
  background: none;
  border: none;
  color: var(--text-primary);
  font-size: 1.5rem;
  cursor: pointer;
  z-index: 1001;
}

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
  
  .hero-actions {
    justify-content: center;
  }
  
  .hero-stats {
    justify-content: center;
  }
  
  .rollback-layout {
    grid-template-columns: 1fr;
    text-align: center;
  }
  
  .rollback-features {
    justify-content: center;
  }
  
  .hero-title {
    font-size: 3.5rem;
  }

  .footer-container {
    flex-direction: column;
    text-align: center;
    align-items: center;
  }
}

@media (max-width: 768px) {
  .mobile-menu-btn {
    display: block;
  }

  .nav-links, .nav-actions {
    display: none;
  }

  .guide-steps {
    grid-template-columns: 1fr;
    gap: 2rem;
  }
  
  .hero-title {
    font-size: 2.8rem;
  }
  
  .rollback-section {
    padding: 60px 1.5rem;
  }
  
  .rollback-content h2 {
    font-size: 2.2rem;
  }

  .hero-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .btn-primary-lg, .btn-secondary-lg {
    width: 100%;
    justify-content: center;
  }
}

/* 移动端菜单样式 */
.mobile-menu-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
  z-index: 2000;
}

.mobile-menu {
  position: absolute;
  top: 0;
  right: 0;
  width: 280px;
  height: 100vh;
  background: var(--bg-primary);
  padding: 80px 1.5rem 2rem;
  display: flex;
  flex-direction: column;
  box-shadow: -4px 0 20px rgba(0, 0, 0, 0.2);
}

.mobile-nav-link {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 1rem;
  text-decoration: none;
  color: var(--text-primary);
  font-weight: 600;
  border-radius: 12px;
  transition: all 0.2s;
}

.mobile-nav-link i {
  width: 20px;
  color: var(--primary-color);
}

.mobile-nav-link:hover {
  background: var(--bg-secondary);
}

.mobile-nav-link.highlight {
  background: var(--gradient-primary);
  color: white;
  margin-top: 0.5rem;
}

.mobile-nav-link.highlight i {
  color: white;
}

.mobile-menu-divider {
  height: 1px;
  background: var(--border-color);
  margin: 1rem 0;
}

.mobile-theme-toggle {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 1rem;
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  color: var(--text-primary);
  font-weight: 600;
  cursor: pointer;
  margin-top: auto;
}

.slide-enter-active, .slide-leave-active {
  transition: all 0.3s ease;
}

.slide-enter-from, .slide-leave-to {
  opacity: 0;
}

.slide-enter-active .mobile-menu {
  transition: transform 0.3s ease;
}

.slide-enter-from .mobile-menu {
  transform: translateX(100%);
}

.slide-leave-active .mobile-menu {
  transition: transform 0.3s ease;
}

.slide-leave-to .mobile-menu {
  transform: translateX(100%);
}
</style>
