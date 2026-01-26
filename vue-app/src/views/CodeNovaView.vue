<template>
  <div class="codenova-page" :class="{ 'dark': themeStore.isDarkMode }">
    <!-- 顶部导航栏 -->
    <nav class="glass-nav">
      <div class="nav-content">
        <div class="logo" @click="router.push('/')">
          <i class="fas fa-brain"></i>
          <span>AI 智能学习助手</span>
        </div>
        <div class="nav-actions">
          <button class="btn-back" @click="router.push('/')">
            <i class="fas fa-arrow-left"></i> 返回首页
          </button>
          <button class="theme-toggle" @click="themeStore.toggleDarkMode()">
            <i :class="themeStore.isDarkMode ? 'fas fa-sun' : 'fas fa-moon'"></i>
          </button>
        </div>
      </div>
    </nav>

    <!-- Hero 区域 -->
    <header class="hero-section">
      <div class="hero-bg">
        <div class="blob"></div>
        <div class="blob"></div>
      </div>
      <div class="hero-content animate-fade-in">
        <div class="logo-wrapper">
          <img src="https://img.icons8.com/fluency/240/code.png" alt="CodeNova Logo" class="app-icon" v-if="!imgError" @error="handleImgError">
          <div class="icon-placeholder" v-if="imgError"><i class="fas fa-code"></i></div>
        </div>
        <h1 class="title">CodeNova</h1>
        <p class="subtitle">随时随地写代码，开启移动开发新纪元</p>
        
        <div class="badge-group">
          <span class="badge blue"><i class="fas fa-certificate"></i> GPLv3</span>
          <span class="badge purple"><i class="fas fa-users"></i> 250+ 活跃用户</span>
          <span class="badge green"><i class="fab fa-android"></i> Android 8+</span>
        </div>

        <div class="action-group">
          <a :href="downloadUrl" class="btn-download" :download="`CodeNova-${version}.apk`">
            <i class="fas fa-cloud-download-alt"></i> 立即下载 APK ({{ version }})
          </a>
          <a :href="githubUrl" target="_blank" class="btn-github">
            <i class="fab fa-github"></i> GitHub 开源地址
          </a>
        </div>
      </div>
    </header>

    <main class="main-content">
      <!-- 简介卡片 -->
      <section class="info-section reveal">
        <div class="glass-card intro-card">
          <h2><i class="fas fa-info-circle"></i> 仓库简介</h2>
          <p>CodeNova 是一款专为 Android 打造的集成开发环境 (IDE)。它不仅是一个代码编辑器，更是一个完整的开发工具链，让您能够直接在手机或平板上构建、调试和管理项目。无论是 Python 脚本还是 C/C++ 应用，CodeNova 都能为您提供丝滑的编码体验。</p>
          <p class="mt-4">此外，CodeNova 深度集成了 <strong>Agent 终端助手</strong> 核心。Agent 是我们的智能自动化引擎，它能理解自然语言指令并自动执行复杂的文件操作、代码分析和终端命令。通过 CodeNova 界面，您可以直接调用 Agent 的能力，实现 AI 驱动的代码编写与项目自动化管理。</p>
        </div>
      </section>

      <!-- 功能特性 -->
      <section class="features-section reveal">
        <h2 class="section-title">功能特性</h2>
        <div class="features-grid">
          <div class="feature-category glass-card">
            <h3><i class="fas fa-star"></i> 核心功能</h3>
            <ul class="feature-list">
              <li v-for="feat in coreFeatures" :key="feat"><i class="fas fa-check"></i> {{ feat }}</li>
            </ul>
          </div>
          <div class="feature-category glass-card">
            <h3><i class="fas fa-robot"></i> AI & Agent 增强</h3>
            <ul class="feature-list">
              <li v-for="feat in aiFeatures" :key="feat"><i class="fas fa-magic"></i> {{ feat }}</li>
            </ul>
          </div>
          <div class="feature-category glass-card">
            <h3><i class="fas fa-terminal"></i> 运行环境</h3>
            <ul class="feature-list">
              <li v-for="feat in runtimeFeatures" :key="feat"><i class="fas fa-microchip"></i> {{ feat }}</li>
            </ul>
          </div>
        </div>
      </section>

      <!-- 快速开始 -->
      <section class="quickstart-section reveal">
        <div class="glass-card">
          <h2><i class="fas fa-bolt"></i> 快速开始</h2>
          <div class="steps">
            <div class="step">
              <div class="step-num">1</div>
              <p>确保设备已开启“允许安装未知来源应用”权限。</p>
            </div>
            <div class="step">
              <div class="step-num">2</div>
              <p>点击上方按钮下载最新版 CodeNova APK。</p>
            </div>
            <div class="step">
              <div class="step-num">3</div>
              <p>安装并启动，选择您的工作空间，开始编码！</p>
            </div>
          </div>
        </div>
      </section>
    </main>

    <footer class="page-footer">
      <p>版权所有 (c) CodeNova Team. 基于 GNU GPLv3 许可.</p>
      <div class="footer-links">
        <a href="#">项目技术文档</a>
        <a href="#">贡献指南</a>
        <a href="#">许可说明</a>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useThemeStore } from '@/stores/theme';
import request from '@/utils/request';
import { API_ENDPOINTS } from '@/config/api';

const router = useRouter();
const themeStore = useThemeStore();
const imgError = ref(false);

const version = ref('v1.1.0');
const downloadUrl = ref('/CodeNova.apk');
const githubUrl = ref('https://github.com/ChenfromChina123/CodeNova');
const updateDate = ref('2024-05-20');

const handleImgError = () => {
  imgError.value = true;
};

const fetchSoftwareInfo = async () => {
  try {
    const response = await request.get(API_ENDPOINTS.admin.publicResources);
    const softwareList = response.data || [];
    const codenova = softwareList.find(item => item.title.toLowerCase().includes('codenova'));
    if (codenova) {
      version.value = codenova.version || 'v1.1.0';
      // 使用增强的下载接口，URL 路径末尾包含文件名，确保移动端正确识别
      if (codenova.filePath) {
          // 假设 codenova.title 是 "CodeNova"，我们加上 .apk 后缀
          // 如果 title 已经包含 .apk，则不重复添加
          let fileName = codenova.title.trim();
          if (!fileName.toLowerCase().endsWith('.apk')) {
              fileName += '.apk';
          }
          // 对文件名进行 URL 编码
          fileName = encodeURIComponent(fileName);
          downloadUrl.value = `${request.defaults.baseURL}/api/resources/download/${codenova.id}/${fileName}`;
      } else {
          downloadUrl.value = '/CodeNova.apk';
      }
      
      // 如果后端返回了 GitHub 链接，则使用它
      if (codenova.url && codenova.url.includes('github.com')) {
        githubUrl.value = codenova.url;
      }
      const date = new Date(codenova.updatedAt || codenova.created_at);
      updateDate.value = date.toISOString().split('T')[0];
    }
  } catch (error) {
    console.error('加载软件信息失败:', error);
  }
};

const coreFeatures = [
  '平板设备完美支持',
  'ZIP 项目一键导入',
  '实时文件监控系统',
  '多窗格标签页界面',
  '会话状态深度持久化',
  '二进制/文本启发式检测'
];

const aiFeatures = [
  'AI 助手聊天（流式 Markdown）',
  'Agent 自动化工具调用',
  '代码变更 Diff 预览与回滚',
  '智能语法校验（Python）',
  '选中代码提问（携带上下文）'
];

const runtimeFeatures = [
  'QPython 运行时深度集成',
  'C/C++ Clang 工具链支持',
  '原生 PTY 终端集成',
  'Android 10+ 兼容性增强',
  'Pip 包管理与热更新支持'
];

onMounted(() => {
  fetchSoftwareInfo();
  document.title = 'CodeNova - 移动端集成开发环境';
  
  // 简单的滚动揭示效果
  const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.classList.add('active');
      }
    });
  }, { threshold: 0.1 });

  document.querySelectorAll('.reveal').forEach(el => observer.observe(el));
});
</script>

<style scoped>
.codenova-page {
  --primary: #2da44e;
  --primary-hover: #2c974b;
  --bg: #f6f8fa;
  --text: #24292f;
  --card-bg: rgba(255, 255, 255, 0.7);
  --border: rgba(210, 215, 222, 0.5);
  
  min-height: 100vh;
  background-color: var(--bg);
  color: var(--text);
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
  transition: all 0.3s ease;
}

.codenova-page.dark {
  --bg: #0d1117;
  --text: #c9d1d9;
  --card-bg: rgba(22, 27, 34, 0.7);
  --border: rgba(48, 54, 61, 0.5);
}

/* 导航栏 */
.glass-nav {
  position: fixed;
  top: 0;
  width: 100%;
  height: 64px;
  background: var(--card-bg);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--border);
  z-index: 1000;
}

.nav-content {
  max-width: 1200px;
  margin: 0 auto;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 2rem;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 700;
  font-size: 1.2rem;
  cursor: pointer;
  color: var(--primary);
}

.nav-actions {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.btn-back {
  background: none;
  border: 1px solid var(--border);
  color: var(--text);
  padding: 0.5rem 1rem;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-back:hover {
  background: var(--border);
}

.theme-toggle {
  background: none;
  border: none;
  color: var(--text);
  font-size: 1.2rem;
  cursor: pointer;
  padding: 5px;
}

/* Hero 区域 */
.hero-section {
  position: relative;
  padding: 120px 2rem 60px;
  text-align: center;
  overflow: hidden;
}

.hero-bg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: -1;
}

.blob {
  position: absolute;
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(45, 164, 78, 0.15) 0%, transparent 70%);
  filter: blur(40px);
  border-radius: 50%;
}

.blob:nth-child(1) { top: -100px; left: -100px; }
.blob:nth-child(2) { bottom: -100px; right: -100px; }

.app-icon {
  width: 120px;
  height: 120px;
  border-radius: 28px;
  box-shadow: 0 20px 40px rgba(0,0,0,0.1);
  margin-bottom: 1.5rem;
}

.icon-placeholder {
  width: 120px;
  height: 120px;
  background: linear-gradient(135deg, #2da44e, #059669);
  border-radius: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 3rem;
  color: white;
  margin: 0 auto 1.5rem;
}

.title {
  font-size: 3.5rem;
  font-weight: 850;
  margin-bottom: 1rem;
  background: linear-gradient(135deg, var(--text) 30%, var(--primary));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.subtitle {
  font-size: 1.25rem;
  color: var(--text);
  opacity: 0.8;
  margin-bottom: 2rem;
}

.badge-group {
  display: flex;
  justify-content: center;
  gap: 10px;
  margin-bottom: 2.5rem;
  flex-wrap: wrap;
}

.badge {
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 0.85rem;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 6px;
}

.badge.blue { background: rgba(59, 130, 246, 0.1); color: #3b82f6; }
.badge.purple { background: rgba(139, 92, 246, 0.1); color: #8b5cf6; }
.badge.green { background: rgba(16, 185, 129, 0.1); color: #10b981; }

.action-group {
  display: flex;
  justify-content: center;
  gap: 1.5rem;
  flex-wrap: wrap;
}

.btn-download {
  background: var(--primary);
  color: white;
  padding: 1rem 2rem;
  border-radius: 12px;
  font-weight: 700;
  text-decoration: none;
  display: flex;
  align-items: center;
  gap: 10px;
  box-shadow: 0 10px 20px rgba(45, 164, 78, 0.3);
  transition: all 0.3s;
}

.btn-download:hover {
  transform: translateY(-3px);
  background: var(--primary-hover);
  box-shadow: 0 15px 30px rgba(45, 164, 78, 0.4);
}

.btn-github {
  background: var(--card-bg);
  color: var(--text);
  padding: 1rem 2rem;
  border-radius: 12px;
  font-weight: 700;
  text-decoration: none;
  display: flex;
  align-items: center;
  gap: 10px;
  border: 1px solid var(--border);
  transition: all 0.3s;
}

.btn-github:hover {
  background: var(--border);
  transform: translateY(-3px);
}

/* 内容区域 */
.main-content {
  max-width: 1000px;
  margin: 0 auto;
  padding: 2rem;
}

.glass-card {
  background: var(--card-bg);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid var(--border);
  border-radius: 20px;
  padding: 2rem;
  box-shadow: 0 8px 32px rgba(0,0,0,0.05);
}

.section-title {
  text-align: center;
  font-size: 2rem;
  margin: 4rem 0 2rem;
}

/* 截图网格 */
.screenshot-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 1.5rem;
}

.img-container {
  aspect-ratio: 16/9;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid var(--border);
  position: relative;
}

.img-container img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.img-placeholder {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: var(--border);
  display: none;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--text);
  opacity: 0.5;
}

/* 特性网格 */
.features-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 1.5rem;
}

.feature-category h3 {
  margin-bottom: 1.5rem;
  color: var(--primary);
  display: flex;
  align-items: center;
  gap: 10px;
}

.feature-list {
  list-style: none;
  padding: 0;
}

.feature-list li {
  margin-bottom: 0.8rem;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 0.95rem;
}

.feature-list li i {
  color: var(--primary);
  font-size: 0.8rem;
}

/* 快速开始步骤 */
.steps {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  margin-top: 1.5rem;
}

.step {
  display: flex;
  align-items: center;
  gap: 1.5rem;
}

.step-num {
  width: 32px;
  height: 32px;
  background: var(--primary);
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  flex-shrink: 0;
}

/* 动画 */
.reveal {
  opacity: 0;
  transform: translateY(30px);
  transition: all 0.6s ease-out;
}

.reveal.active {
  opacity: 1;
  transform: translateY(0);
}

.animate-fade-in {
  animation: fadeIn 1s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

.page-footer {
  text-align: center;
  padding: 4rem 2rem;
  border-top: 1px solid var(--border);
  margin-top: 4rem;
  color: var(--text);
  opacity: 0.7;
}

.footer-links {
  display: flex;
  justify-content: center;
  gap: 2rem;
  margin-top: 1rem;
}

.footer-links a {
  color: var(--text);
  text-decoration: none;
}

.footer-links a:hover {
  text-decoration: underline;
}

@media (max-width: 768px) {
  .title { font-size: 2.5rem; }
  .action-group { flex-direction: column; align-items: stretch; }
  .nav-content { padding: 0 1rem; }
  .nav-actions .btn-back span { display: none; }
}
</style>