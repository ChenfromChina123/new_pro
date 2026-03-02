<template>
  <div class="author-page">
    <div class="container">
      <!-- 页面标题 -->
      <div class="mb-3 text-center">
        <h1 class="mb-2 text-4xl font-bold">关于作者</h1>
        <p class="text-lg text-secondary">了解 AI 智能学习助手背后的开发者</p>
      </div>

      <!-- 作者信息卡片 -->
      <div class="author-card card">
        <div class="author-header">
          <div class="author-avatar">Xi</div>

          <!-- 基本信息 -->
          <div class="author-info">
            <h2 class="text-3xl font-bold">XiHuiChen</h2>
            <p class="mt-1 text-xl text-secondary">Java 后端开发工程师</p>
            <p class="mt-1 text-secondary">本科在读 | 软件工程 | 专注后端开发与 AI 应用</p>

            <!-- 联系方式 -->
            <div class="author-contacts">
              <div class="contact-item">
                <span class="contact-icon">📧</span>
                <a
                  href="mailto:3301767269@qq.com"
                  class="contact-link"
                >
                  3301767269@qq.com
                </a>
              </div>
              <div class="contact-item">
                <span class="contact-icon">🎓</span>
                <span>桂林理工大学</span>
              </div>
              <div class="contact-item">
                <span class="contact-icon">🌐</span>
                <a
                  href="https://aistudy.icu"
                  target="_blank"
                  class="contact-link"
                >
                  aistudy.icu
                </a>
              </div>
            </div>
          </div>
        </div>

        <!-- 教育经历 -->
        <div class="section">
          <h3 class="section-title">教育经历</h3>
          <div class="education-card">
            <div class="flex justify-between">
              <div>
                <h4 class="text-xl font-semibold">桂林理工大学</h4>
                <p class="text-secondary">软件工程 · 本科 · 计算机科学与工程学院</p>
              </div>
              <span class="text-secondary"> 2023.09 - 2027.06 </span>
            </div>
          </div>
        </div>

        <!-- 专业技能 -->
        <div class="section">
          <h3 class="section-title">专业技能</h3>
          <div class="skills-grid">
            <div class="skill-card">
              <h4 class="skill-title">后端技术</h4>
              <div class="skill-tags">
                <span class="skill-tag backend">Java</span>
                <span class="skill-tag backend">Spring Boot</span>
                <span class="skill-tag backend">MySQL</span>
                <span class="skill-tag backend">Redis</span>
              </div>
            </div>
            <div class="skill-card">
              <h4 class="skill-title">前端技术</h4>
              <div class="skill-tags">
                <span class="skill-tag frontend">Vue.js</span>
                <span class="skill-tag frontend">React</span>
                <span class="skill-tag frontend">TypeScript</span>
                <span class="skill-tag frontend">Tailwind CSS</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 荣誉奖项 -->
        <div class="section">
          <h3 class="section-title">荣誉奖项</h3>
          <div class="awards-list">
            <div class="award-item">
              <span class="award-icon">🏆</span>
              <p class="award-text">第16届蓝桥杯全国软件类国赛三等奖</p>
            </div>
            <div class="award-item">
              <span class="award-icon">⭐</span>
              <p class="award-text">第六届全国大学生算法设计与编程挑战赛优秀奖</p>
            </div>
            <div class="award-item">
              <span class="award-icon">🎓</span>
              <p class="award-text">连续两年获得校级三等奖学金（2024、2025学年）</p>
            </div>
          </div>
        </div>

        <!-- 个人简介 -->
        <div class="section">
          <h3 class="section-title">个人简介</h3>
          <div class="bio-content">
            <p>
              热爱编程，专注于后端开发和AI应用。作为AI智能学习助手项目的主要开发者，致力于打造高效、易用的学习平台。
              在校期间积极参与各类编程竞赛，不断提升自己的技术能力和解决问题的思维方式。
            </p>
            <p class="mt-2">
              擅长使用Java进行后端开发，熟悉Spring
              Boot、MySQL、Redis等技术栈。同时具备前端开发能力，
              能够使用Vue.js、React等框架进行全栈开发。对AI技术有浓厚兴趣，正在探索将AI技术应用于教育领域。
            </p>
          </div>
        </div>

        <!-- 社交链接 -->
        <div class="section text-center">
          <h3 class="section-title">联系方式</h3>
          <div class="social-links">
            <a
              href="mailto:3301767269@qq.com"
              class="social-link"
              title="邮箱"
            >
              <span class="social-icon">📧</span>
            </a>
            <a
              href="https://aistudy.icu"
              target="_blank"
              class="social-link"
              title="个人网站"
            >
              <span class="social-icon">🌐</span>
            </a>
            <a
              href="https://cv.aistudy.icu"
              target="_blank"
              class="social-link"
              title="完整简历"
            >
              <span class="social-icon">📄</span>
            </a>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
// 页面元数据
document.title = "关于作者 - AI智能学习助手";
</script>

<style scoped>
.author-page {
  min-height: calc(100vh - 60px);
  padding-top: 20px;
}

.author-card {
  max-width: 900px;
  margin: 0 auto;
  padding: 32px;
}

.author-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 32px;
}

.author-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background: linear-gradient(135deg, #1d4ed8 0%, #8b5cf6 100%);
  color: white;
  font-size: 48px;
  font-weight: bold;
  box-shadow: var(--shadow-lg);
  margin-bottom: 20px;
}

.author-info {
  text-align: center;
}

.author-contacts {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 16px;
  margin-top: 16px;
}

.contact-item {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--text-secondary);
}

.contact-icon {
  font-size: 18px;
}

.contact-link {
  color: var(--primary-color);
  text-decoration: none;
  transition: color 0.2s ease;
}

.contact-link:hover {
  color: var(--primary-dark);
}

.section {
  margin-bottom: 32px;
}

.section-title {
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 16px;
  color: var(--text-primary);
}

.education-card {
  background-color: var(--bg-tertiary);
  padding: 20px;
  border-radius: var(--border-radius-md);
}

.skills-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 16px;
}

.skill-card {
  background-color: var(--bg-tertiary);
  padding: 20px;
  border-radius: var(--border-radius-md);
}

.skill-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 12px;
  color: var(--text-primary);
}

.skill-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.skill-tag {
  padding: 4px 12px;
  border-radius: 16px;
  font-size: 14px;
  font-weight: 500;
}

.skill-tag.backend {
  background-color: rgba(59, 130, 246, 0.1);
  color: #2563eb;
}

.skill-tag.frontend {
  background-color: rgba(16, 185, 129, 0.1);
  color: #059669;
}

.awards-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.award-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.award-icon {
  font-size: 20px;
  margin-top: 2px;
}

.award-text {
  font-size: 16px;
  font-weight: 500;
  color: var(--text-primary);
}

.bio-content {
  line-height: 1.7;
  color: var(--text-secondary);
}

.social-links {
  display: flex;
  justify-content: center;
  gap: 24px;
}

.social-link {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background-color: var(--bg-tertiary);
  color: var(--text-secondary);
  text-decoration: none;
  transition: all 0.2s ease;
}

.social-link:hover {
  background-color: var(--primary-color);
  color: white;
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.social-icon {
  font-size: 20px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .author-card {
    padding: 20px;
  }

  .author-header {
    margin-bottom: 24px;
  }

  .author-avatar {
    width: 100px;
    height: 100px;
    font-size: 40px;
  }

  .section {
    margin-bottom: 24px;
  }

  .skills-grid {
    grid-template-columns: 1fr;
  }

  .author-contacts {
    flex-direction: column;
    align-items: center;
  }
}

/* 深色模式适配 */
body.dark-mode .skill-tag.backend {
  background-color: rgba(59, 130, 246, 0.2);
  color: #93c5fd;
}

body.dark-mode .skill-tag.frontend {
  background-color: rgba(16, 185, 129, 0.2);
  color: #86efac;
}
</style>
