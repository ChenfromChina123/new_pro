<template>
  <section id="download-center" class="agent-download-center reveal">
    <div class="container">
      <div class="section-header">
        <h2 class="section-title">获取 Agent 终端助手</h2>
        <p class="section-subtitle">当前版本：v1.0.0 | 支持多平台部署，解锁深度自动化能力</p>
      </div>

      <div class="download-grid">
        <!-- 全平台完整包 (推荐) -->
        <div class="download-card primary">
          <div class="platform-icon">
            <i class="fas fa-box-open"></i>
          </div>
          <h3 class="platform-name">全平台完整包</h3>
          <p class="platform-desc">包含 Windows (.exe) 与 Linux 二进制文件。一包在手，随处部署。</p>
          <ul class="feature-list">
            <li><i class="fas fa-check"></i> 包含所有平台可执行文件</li>
            <li><i class="fas fa-check"></i> 适合多环境开发者</li>
            <li><i class="fas fa-check"></i> 内置安装与配置说明</li>
          </ul>
          <a href="/xiaochen_agent_v1.0.0.zip" download class="download-btn">
            <i class="fas fa-file-archive"></i> 下载全平台包 (v1.0.0)
          </a>
          <div class="version-tag">推荐下载</div>
        </div>

        <!-- Linux 独立版 -->
        <div class="download-card">
          <div class="platform-icon">
            <i class="fab fa-linux"></i>
          </div>
          <h3 class="platform-name">Linux 独立版</h3>
          <p class="platform-desc">适用于 Ubuntu, CentOS, Debian 等主流发行版。仅包含 Linux 核心程序。</p>
          <ul class="feature-list">
            <li><i class="fas fa-check"></i> 极简二进制安装</li>
            <li><i class="fas fa-check"></i> 完整的系统权限集成</li>
            <li><i class="fas fa-check"></i> 低内存占用 (约 15MB)</li>
          </ul>
          <a href="/downloads/xiaochen-agent-linux" download class="download-btn secondary">
            <i class="fas fa-download"></i> 立即下载 (v1.0.0)
          </a>
        </div>

        <!-- macOS 版本 (敬请期待) -->
        <div class="download-card disabled">
          <div class="platform-icon">
            <i class="fab fa-apple"></i>
          </div>
          <h3 class="platform-name">macOS 版本</h3>
          <p class="platform-desc">专为 Apple Silicon 与 Intel 芯片优化，深度集成 zsh 终端。</p>
          <div class="coming-soon">敬请期待</div>
        </div>
      </div>

      <!-- 版本说明 -->
      <div class="version-info">
        <div class="info-item">
          <h4><i class="fas fa-info-circle"></i> 安装说明</h4>
          <p>下载 Linux 二进制文件后，请运行 <code>chmod +x xiaochen-agent-linux</code> 赋予执行权限，然后通过 <code>./xiaochen-agent-linux</code> 启动。</p>
        </div>
        <div class="info-item">
          <h4><i class="fas fa-shield-alt"></i> 安全保障</h4>
          <p>所有二进制文件均经过自动化构建流水线生成，确保代码纯净，无任何第三方插件依赖。</p>
        </div>
      </div>

      <!-- 版本演进与计划 -->
      <div class="roadmap-section">
        <h3 class="roadmap-title">版本路线图</h3>
        <div class="roadmap-grid">
          <div class="roadmap-item active">
            <div class="roadmap-dot"></div>
            <div class="roadmap-content">
              <span class="version-badge">v1.0.0 (当前)</span>
              <h4>基础能力构建</h4>
              <p>实现 Linux 核心终端支持、多模型集成、基础回退系统及进程管理。</p>
            </div>
          </div>
          <div class="roadmap-item">
            <div class="roadmap-dot"></div>
            <div class="roadmap-content">
              <span class="version-badge">v1.2.0 (计划中)</span>
              <h4>Windows 原生适配</h4>
              <p>引入 PowerShell 深度集成，优化 Windows 文件系统 IO 性能。</p>
            </div>
          </div>
          <div class="roadmap-item">
            <div class="roadmap-dot"></div>
            <div class="roadmap-content">
              <span class="version-badge">v2.0.0 (远景)</span>
              <h4>协作式 Agent 矩阵</h4>
              <p>支持多 Agent 协同作业，引入分布式任务调度与视觉交互界面。</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
/**
 * Agent 下载中心组件
 * 提供 Linux 版本的下载功能及各平台版本规划展示
 */
</script>

<style scoped>
.agent-download-center {
  padding: 100px 0;
  background: var(--bg-secondary);
  border-top: 1px solid var(--border-color);
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 2rem;
}

.section-header {
  text-align: center;
  margin-bottom: 60px;
}

.section-title {
  font-size: 2.5rem;
  font-weight: 800;
  margin-bottom: 1rem;
  background: linear-gradient(135deg, #06b6d4 0%, #10b981 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.section-subtitle {
  color: var(--text-secondary);
  font-size: 1.1rem;
}

.download-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 2rem;
  margin-bottom: 60px;
}

.download-card {
  background: var(--bg-primary);
  border-radius: 24px;
  padding: 40px;
  border: 1px solid var(--border-color);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.download-card.primary {
  border-color: #06b6d4;
  box-shadow: 0 10px 30px rgba(6, 182, 212, 0.1);
}

.download-card:hover:not(.disabled) {
  transform: translateY(-10px);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
  border-color: #10b981;
}

.platform-icon {
  font-size: 3rem;
  margin-bottom: 1.5rem;
  color: var(--text-secondary);
}

.primary .platform-icon {
  color: #06b6d4;
}

.platform-name {
  font-size: 1.5rem;
  font-weight: 700;
  margin-bottom: 1rem;
}

.platform-desc {
  color: var(--text-secondary);
  font-size: 0.95rem;
  line-height: 1.6;
  margin-bottom: 2rem;
  flex-grow: 1;
}

.feature-list {
  list-style: none;
  padding: 0;
  margin: 0 0 2.5rem 0;
}

.feature-list li {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 0.9rem;
  color: var(--text-primary);
  margin-bottom: 12px;
}

.feature-list li i {
  color: #10b981;
  font-size: 0.8rem;
}

.download-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  background: linear-gradient(135deg, #06b6d4 0%, #10b981 100%);
  color: white;
  padding: 16px 24px;
  border-radius: 12px;
  text-decoration: none;
  font-weight: 600;
  transition: all 0.3s ease;
}

.download-btn:hover {
  transform: scale(1.02);
  box-shadow: 0 5px 15px rgba(16, 185, 129, 0.3);
}

.download-btn.secondary {
  background: var(--bg-tertiary);
  color: var(--text-primary);
  border: 1px solid var(--border-color);
}

.download-btn.secondary:hover {
  background: var(--bg-secondary);
  border-color: #10b981;
  color: #10b981;
}

.version-tag {
  position: absolute;
  top: 20px;
  right: -30px;
  background: #10b981;
  color: white;
  padding: 5px 40px;
  font-size: 0.75rem;
  font-weight: 700;
  transform: rotate(45deg);
}

.download-card.disabled {
  opacity: 0.7;
  background: var(--bg-secondary);
  cursor: not-allowed;
}

.coming-soon {
  margin-top: auto;
  padding: 12px;
  background: var(--bg-tertiary);
  border-radius: 10px;
  text-align: center;
  font-weight: 600;
  color: var(--text-tertiary);
  font-size: 0.9rem;
}

.version-info {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
  gap: 3rem;
  background: var(--bg-primary);
  padding: 40px;
  border-radius: 20px;
  border: 1px solid var(--border-color);
}

.info-item h4 {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 1.1rem;
  font-weight: 700;
  margin-bottom: 1rem;
  color: var(--text-primary);
}

.info-item h4 i {
  color: #06b6d4;
}

.info-item p {
  color: var(--text-secondary);
  font-size: 0.95rem;
  line-height: 1.6;
}

code {
  background: var(--bg-tertiary);
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'Fira Code', monospace;
  color: #ef4444;
}

/* 版本路线图样式 */
.roadmap-section {
  margin-top: 80px;
  padding-top: 60px;
  border-top: 1px dashed var(--border-color);
}

.roadmap-title {
  font-size: 1.8rem;
  font-weight: 700;
  text-align: center;
  margin-bottom: 50px;
}

.roadmap-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 2rem;
  position: relative;
}

.roadmap-grid::before {
  content: '';
  position: absolute;
  top: 15px;
  left: 0;
  right: 0;
  height: 2px;
  background: var(--border-color);
  z-index: 0;
}

.roadmap-item {
  position: relative;
  z-index: 1;
}

.roadmap-dot {
  width: 30px;
  height: 30px;
  background: var(--bg-primary);
  border: 4px solid var(--border-color);
  border-radius: 50%;
  margin-bottom: 20px;
  transition: all 0.3s ease;
}

.roadmap-item.active .roadmap-dot {
  border-color: #10b981;
  background: #10b981;
  box-shadow: 0 0 15px rgba(16, 185, 129, 0.5);
}

.roadmap-content {
  background: var(--bg-primary);
  padding: 24px;
  border-radius: 16px;
  border: 1px solid var(--border-color);
  transition: all 0.3s ease;
}

.roadmap-item.active .roadmap-content {
  border-color: rgba(16, 185, 129, 0.3);
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.05);
}

.version-badge {
  display: inline-block;
  padding: 4px 12px;
  background: var(--bg-tertiary);
  border-radius: 20px;
  font-size: 0.75rem;
  font-weight: 700;
  margin-bottom: 12px;
  color: var(--text-secondary);
}

.roadmap-item.active .version-badge {
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
}

.roadmap-content h4 {
  font-size: 1.1rem;
  font-weight: 700;
  margin-bottom: 10px;
}

.roadmap-content p {
  font-size: 0.9rem;
  color: var(--text-secondary);
  line-height: 1.5;
}

@media (max-width: 992px) {
  .roadmap-grid {
    grid-template-columns: 1fr;
    gap: 3rem;
  }
  
  .roadmap-grid::before {
    top: 0;
    bottom: 0;
    left: 15px;
    width: 2px;
    height: auto;
  }
}

@media (max-width: 768px) {
  .section-title {
    font-size: 2rem;
  }
  
  .version-info {
    grid-template-columns: 1fr;
    padding: 30px 20px;
  }
}
</style>