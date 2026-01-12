<template>
  <div class="agent-view">
    <!-- Hero Section -->
    <header class="hero-section">
      <div class="container">
        <div class="hero-content">
          <div class="badge">智能终端体</div>
          <h1>小晨终端助手</h1>
          <p class="subtitle">一个强大的 AI 终端助手，支持多种 LLM 模型，提供智能的命令行交互体验。专为开发者打造的生产力工具。</p>
          <div class="hero-actions">
            <a href="/xiaochen_terminal.zip" download class="btn btn-primary">
              <i class="fas fa-download"></i> 立即下载
            </a>
            <a href="#features" class="btn btn-secondary">了解功能</a>
          </div>
        </div>
        <div class="hero-image">
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
      </div>
    </header>

    <!-- Features Grid -->
    <section id="features" class="features-section">
      <div class="container">
        <div class="section-header">
          <h2>核心特性</h2>
          <p>集成多项前沿技术，重塑您的终端使用体验</p>
        </div>
        <div class="features-grid">
          <div v-for="(feature, index) in features" :key="index" class="feature-card">
            <div class="icon-box" :style="{ backgroundColor: feature.color }">
              <i :class="feature.icon"></i>
            </div>
            <h3>{{ feature.title }}</h3>
            <p>{{ feature.description }}</p>
            <ul class="feature-list">
              <li v-for="item in feature.items" :key="item">
                <i class="fas fa-check"></i> {{ item }}
              </li>
            </ul>
          </div>
        </div>
      </div>
    </section>

    <!-- Usage Guide -->
    <section class="guide-section">
      <div class="container">
        <div class="section-header">
          <h2>使用指南</h2>
          <p>简单几步，即可开启智能开发之旅</p>
        </div>
        
        <div class="guide-steps">
          <div class="step-item">
            <div class="step-number">01</div>
            <div class="step-content">
              <h3>快速安装</h3>
              <p>下载压缩包并解压，Windows 用户运行 <code>scripts/install.bat</code>，Linux/Mac 用户运行 <code>scripts/install.sh</code>。</p>
            </div>
          </div>
          <div class="step-item">
            <div class="step-number">02</div>
            <div class="step-content">
              <h3>配置模型</h3>
              <p>首次启动程序会提示输入 API Key，支持 DeepSeek、豆包等多种主流大模型。配置将自动保存至 <code>config.json</code>。</p>
            </div>
          </div>
          <div class="step-item">
            <div class="step-number">03</div>
            <div class="step-content">
              <h3>开始对话</h3>
              <p>在终端直接输入您的需求，AI 将智能理解意图并调用相关工具执行任务，如文件读写、命令执行等。</p>
            </div>
          </div>
        </div>

        <div class="commands-table">
          <h3>常用命令</h3>
          <div class="table-wrapper">
            <table>
              <thead>
                <tr>
                  <th>命令</th>
                  <th>描述</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td><code>save [name]</code></td>
                  <td>保存当前会话，支持自定义名称</td>
                </tr>
                <tr>
                  <td><code>load &lt;id&gt;</code></td>
                  <td>加载指定的历史会话</td>
                </tr>
                <tr>
                  <td><code>sessions</code></td>
                  <td>列出最近的历史会话记录</td>
                </tr>
                <tr>
                  <td><code>rollback</code></td>
                  <td>撤销上一次文件修改操作</td>
                </tr>
                <tr>
                  <td><code>ps</code> / <code>watch</code></td>
                  <td>查看和监控后台运行的任务进程</td>
                </tr>
                <tr>
                  <td><code>help</code></td>
                  <td>显示完整的命令帮助手册</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </section>

    <!-- Rollback System Highlight -->
    <section class="rollback-section">
      <div class="container">
        <div class="rollback-content">
          <div class="badge dark">特色功能</div>
          <h2>专业级回滚系统</h2>
          <p>小晨终端助手内置了强大的版本控制模块，为您提供多级文件回滚、快照管理和版本对比功能。即使 AI 误操作，您也可以一键恢复项目状态。</p>
          <div class="rollback-features">
            <div class="r-item">
              <i class="fas fa-history"></i>
              <span>多级版本历史</span>
            </div>
            <div class="r-item">
              <i class="fas fa-code-branch"></i>
              <span>快照管理</span>
            </div>
            <div class="r-item">
              <i class="fas fa-search-plus"></i>
              <span>差异对比 (Diff)</span>
            </div>
          </div>
        </div>
        <div class="rollback-code">
          <pre><code># 示例：一键回退
小晨助手 > undo
✓ 已撤销上一次对话涉及的所有 3 个文件修改
✓ 已恢复到对话前的状态</code></pre>
        </div>
      </div>
    </section>

    <!-- Footer CTA -->
    <section class="cta-section">
      <div class="container">
        <h2>准备好提升您的终端效率了吗？</h2>
        <p>立即下载小晨终端助手，体验 AI 驱动的新一代开发环境。</p>
        <div class="cta-actions">
          <a href="/xiaochen_terminal.zip" download class="btn btn-primary btn-lg">立即下载</a>
          <router-link to="/" class="btn btn-outline btn-lg">返回首页</router-link>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
/**
 * AgentView.vue - Agent 终端助手介绍页面
 * 展示 Agent 的主要功能、使用指南及命令参考
 */
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
.agent-view {
  color: var(--text-primary);
  background-color: var(--bg-primary);
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
  overflow-x: hidden;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 2rem;
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

.badge.dark {
  background-color: rgba(255, 255, 255, 0.1);
  color: #fff;
}

/* Hero Section */
.hero-section {
  padding: 8rem 0 6rem;
  background: linear-gradient(135deg, var(--bg-primary) 0%, var(--bg-secondary) 100%);
}

.hero-section .container {
  display: flex;
  align-items: center;
  gap: 4rem;
}

.hero-content {
  flex: 1;
}

.hero-content h1 {
  font-size: 4rem;
  font-weight: 800;
  line-height: 1.1;
  margin-bottom: 1.5rem;
  background: linear-gradient(to right, #3b82f6, #8b5cf6);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.subtitle {
  font-size: 1.25rem;
  color: var(--text-secondary);
  margin-bottom: 2.5rem;
  line-height: 1.6;
}

.hero-actions {
  display: flex;
  gap: 1rem;
}

.hero-image {
  flex: 1;
}

.terminal-mockup {
  background-color: #1e1e1e;
  border-radius: 12px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
  overflow: hidden;
  border: 1px solid #333;
}

.terminal-header {
  background-color: #333;
  padding: 0.75rem 1rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
}

.red { background-color: #ff5f56; }
.yellow { background-color: #ffbd2e; }
.green { background-color: #27c93f; }

.terminal-title {
  color: #999;
  font-size: 0.75rem;
  margin-left: 0.5rem;
  font-family: monospace;
}

.terminal-body {
  padding: 1.5rem;
  font-family: 'Fira Code', 'Consolas', monospace;
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

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

/* Features Section */
.features-section {
  padding: 8rem 0;
}

.section-header {
  text-align: center;
  margin-bottom: 4rem;
}

.section-header h2 {
  font-size: 2.5rem;
  font-weight: 700;
  margin-bottom: 1rem;
}

.section-header p {
  color: var(--text-secondary);
  font-size: 1.1rem;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 2rem;
}

.feature-card {
  padding: 2.5rem;
  background-color: var(--bg-secondary);
  border-radius: 16px;
  border: 1px solid var(--border-color);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.feature-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 30px -10px rgba(0, 0, 0, 0.1);
}

.icon-box {
  width: 60px;
  height: 60px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 1.5rem;
}

.icon-box i {
  font-size: 1.5rem;
  color: #3b82f6;
}

.feature-card h3 {
  font-size: 1.25rem;
  font-weight: 600;
  margin-bottom: 1rem;
}

.feature-card p {
  color: var(--text-secondary);
  font-size: 0.95rem;
  margin-bottom: 1.5rem;
  line-height: 1.5;
}

.feature-list {
  list-style: none;
  padding: 0;
}

.feature-list li {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  font-size: 0.875rem;
  color: var(--text-secondary);
  margin-bottom: 0.5rem;
}

.feature-list li i {
  color: #10b981;
  font-size: 0.75rem;
}

/* Guide Section */
.guide-section {
  padding: 8rem 0;
  background-color: var(--bg-secondary);
}

.guide-steps {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 3rem;
  margin-bottom: 6rem;
}

.step-item {
  position: relative;
}

.step-number {
  font-size: 3rem;
  font-weight: 800;
  color: rgba(59, 130, 246, 0.1);
  margin-bottom: -1.5rem;
}

.step-content h3 {
  font-size: 1.5rem;
  font-weight: 600;
  margin-bottom: 1rem;
  position: relative;
}

.step-content p {
  color: var(--text-secondary);
  line-height: 1.6;
}

.commands-table {
  background-color: var(--bg-primary);
  border-radius: 16px;
  padding: 3rem;
  border: 1px solid var(--border-color);
}

.commands-table h3 {
  font-size: 1.75rem;
  font-weight: 700;
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
  color: var(--text-secondary);
  font-weight: 600;
}

td {
  padding: 1.25rem 1rem;
  border-bottom: 1px solid var(--border-color);
}

code {
  background-color: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
  padding: 0.2rem 0.4rem;
  border-radius: 4px;
  font-family: monospace;
}

/* Rollback Section */
.rollback-section {
  padding: 8rem 0;
  background-color: #111827;
  color: #fff;
}

.rollback-section .container {
  display: flex;
  align-items: center;
  gap: 6rem;
}

.rollback-content {
  flex: 1;
}

.rollback-content h2 {
  font-size: 2.5rem;
  font-weight: 700;
  margin-bottom: 1.5rem;
}

.rollback-content p {
  color: #9ca3af;
  font-size: 1.1rem;
  line-height: 1.7;
  margin-bottom: 2.5rem;
}

.rollback-features {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.r-item {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.r-item i {
  color: #3b82f6;
}

.rollback-code {
  flex: 1;
  background-color: #1f2937;
  border-radius: 12px;
  padding: 2rem;
  border: 1px solid #374151;
}

.rollback-code pre {
  margin: 0;
}

.rollback-code code {
  color: #e5e7eb;
  background: none;
  padding: 0;
  font-size: 0.95rem;
  line-height: 1.6;
}

/* CTA Section */
.cta-section {
  padding: 10rem 0;
  text-align: center;
}

.cta-section h2 {
  font-size: 3rem;
  font-weight: 800;
  margin-bottom: 1.5rem;
}

.cta-section p {
  font-size: 1.25rem;
  color: var(--text-secondary);
  margin-bottom: 3rem;
}

.cta-actions {
  display: flex;
  justify-content: center;
  gap: 1.5rem;
}

/* Buttons */
.btn {
  display: inline-flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 1.5rem;
  border-radius: 8px;
  font-weight: 600;
  transition: all 0.2s ease;
  cursor: pointer;
  text-decoration: none;
}

.btn-lg {
  padding: 1rem 2.5rem;
  font-size: 1.1rem;
}

.btn-primary {
  background-color: #3b82f6;
  color: #fff;
}

.btn-primary:hover {
  background-color: #2563eb;
  transform: translateY(-2px);
}

.btn-secondary {
  background-color: var(--bg-secondary);
  color: var(--text-primary);
  border: 1px solid var(--border-color);
}

.btn-secondary:hover {
  background-color: var(--border-color);
}

.btn-outline {
  border: 2px solid var(--border-color);
  color: var(--text-primary);
}

.btn-outline:hover {
  background-color: var(--border-color);
}

/* Responsive */
@media (max-width: 992px) {
  .hero-section .container,
  .rollback-section .container {
    flex-direction: column;
    text-align: center;
  }
  
  .hero-content h1 {
    font-size: 3rem;
  }
  
  .hero-actions, .cta-actions {
    justify-content: center;
  }
  
  .guide-steps {
    grid-template-columns: 1fr;
  }
  
  .rollback-features {
    align-items: center;
  }
}

@media (max-width: 640px) {
  .hero-content h1 {
    font-size: 2.5rem;
  }
  
  .hero-actions, .cta-actions {
    flex-direction: column;
  }
  
  .btn {
    width: 100%;
    justify-content: center;
  }
}
</style>
