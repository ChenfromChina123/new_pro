<template>
  <div class="auth-page">
    <!-- 装饰性背景元素 -->
    <div class="bg-shape shape-1"></div>
    <div class="bg-shape shape-2"></div>

    <div class="auth-container">
      <!-- 左侧网站功能介绍 -->
      <div class="auth-info">
        <h1 class="info-title">探索无限可能<br>开启智能学习新时代</h1>
        <p class="info-desc">
          AI学习助手为您提供全方位的智能辅导，通过个性化的学习路径规划、实时答疑和海量知识库，助您高效掌握核心技能，让学习变得更简单。
        </p>
        <div class="feature-list">
          <div class="feature-item">
            <div class="feature-icon"><i class="fas fa-brain"></i></div>
            <span>智能对话解析，随问随答</span>
          </div>
          <div class="feature-item">
            <div class="feature-icon"><i class="fas fa-folder-open"></i></div>
            <span>云端知识库，随时查阅</span>
          </div>
          <div class="feature-item">
            <div class="feature-icon"><i class="fas fa-chart-line"></i></div>
            <span>个性化学习，精准提升</span>
          </div>
        </div>
      </div>

      <!-- 右侧登录框 -->
      <div class="auth-card">
        <div class="auth-header">
          <h2 class="auth-title">AI学习助手</h2>
          <p class="auth-subtitle">欢迎回来，开启智能学习之旅</p>
        </div>

      <form
        class="auth-form"
        @submit.prevent="handleLogin"
      >
        <div class="form-group">
          <div class="input-wrapper">
            <i class="fas fa-envelope input-icon"></i>
            <input
              v-model="form.email"
              type="email"
              class="input"
              placeholder="请输入邮箱"
              required
              autocomplete="off"
            >
          </div>
        </div>

        <div class="form-group">
          <div class="input-wrapper">
            <i class="fas fa-lock input-icon"></i>
            <input
              v-model="form.password"
              type="password"
              class="input"
              placeholder="请输入密码"
              required
              autocomplete="new-password"
            >
          </div>
        </div>

        <div
          v-if="errorMessage"
          class="error-message"
        >
          <i class="fas fa-exclamation-circle"></i>
          {{ errorMessage }}
        </div>

        <button
          type="submit"
          class="btn btn-primary login-btn"
          :disabled="isLoading"
        >
          <span
            v-if="isLoading"
            class="loading-spinner"
          >
            <i class="fas fa-circle-notch fa-spin"></i>
          </span>
          <span>{{ isLoading ? '登录中...' : '立即登录' }}</span>
          <i v-if="!isLoading" class="fas fa-arrow-right btn-icon"></i>
        </button>

        <div class="guest-login-divider">
          <span>OR</span>
        </div>

        <button
          type="button"
          class="btn btn-secondary guest-btn"
          @click="handleGuestLogin"
        >
          <i class="fas fa-user-secret"></i>
          <span>游客试用体验</span>
        </button>

        <div class="auth-links">
          <router-link to="/register" class="link-item">
            还没有账号？<span class="highlight">立即注册</span>
          </router-link>
          <router-link to="/forgot-password" class="link-item">
            忘记密码？
          </router-link>
        </div>
      </form>
    </div>
  </div>
</div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useChatStore } from '@/stores/chat'
import { useCloudDiskStore } from '@/stores/cloudDisk'

const router = useRouter()
const authStore = useAuthStore()
const chatStore = useChatStore()
const cloudDiskStore = useCloudDiskStore()

const form = ref({
  email: '',
  password: ''
})

const isLoading = ref(false)
const errorMessage = ref('')

const handleLogin = async () => {
  isLoading.value = true
  errorMessage.value = ''

  const result = await authStore.login(form.value.email, form.value.password)

  if (result.success) {
    // 登录成功后，预加载聊天会话列表和云盘文件夹树
    try {
      // 并行加载资源，提高性能
      await Promise.all([
        chatStore.fetchSessions(),
        cloudDiskStore.fetchFolders()
      ])
    } catch (error) {
      console.error('预加载资源失败:', error)
      // 即使资源预加载失败，也允许用户进入聊天页面
    }

    router.push('/chat')
  } else {
    errorMessage.value = result.message
  }

  isLoading.value = false
}

/**
 * 处理游客登录
 * 游客模式下不需要 token，直接进入聊天页面
 */
const handleGuestLogin = () => {
  // 清除之前的登录信息
  authStore.logout()
  // 进入聊天页面
  router.push('/chat')
}
</script>

<style scoped>
.auth-page {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: linear-gradient(135deg, var(--gradient-start, #f0f4f8), var(--gradient-end, #e0e8f0));
  overflow: hidden;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
}

/* AI 科技感悬浮背景形状 */
.bg-shape {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  z-index: 0;
  opacity: 0.5;
  animation: float 10s infinite ease-in-out;
}

.shape-1 {
  top: -10%;
  left: -10%;
  width: 500px;
  height: 500px;
  background: var(--primary-color, #3b82f6);
  animation-delay: 0s;
}

.shape-2 {
  bottom: -10%;
  right: -10%;
  width: 400px;
  height: 400px;
  background: #8b5cf6;
  animation-delay: -5s;
}

@keyframes float {
  0%, 100% { transform: translateY(0) scale(1); }
  50% { transform: translateY(-20px) scale(1.05); }
}

.auth-container {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  max-width: 1000px;
  width: 100%;
  gap: 60px;
}

.auth-info {
  flex: 1;
  color: var(--text-primary, #1f2937);
  padding-right: 20px;
}

.info-title {
  font-size: 40px;
  font-weight: 800;
  line-height: 1.3;
  margin-bottom: 20px;
  color: var(--text-primary, #111827);
  letter-spacing: -1px;
}

.info-desc {
  font-size: 16px;
  line-height: 1.7;
  color: var(--text-secondary, #4b5563);
  margin-bottom: 36px;
}

.feature-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 16px;
  font-weight: 500;
  color: var(--text-primary, #374151);
}

.feature-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: rgba(59, 130, 246, 0.1);
  color: var(--primary-color, #3b82f6);
  font-size: 18px;
}

.auth-card {
  flex-shrink: 0;
  position: relative;
  z-index: 1;
  background: var(--bg-secondary, rgba(255, 255, 255, 0.9));
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--border-color, rgba(255, 255, 255, 0.2));
  border-radius: 24px;
  padding: 40px 32px;
  max-width: 380px;
  width: 100%;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.15);
}

.auth-header {
  text-align: center;
  margin-bottom: 36px;
}

.logo-icon {
  font-size: 48px;
  background: linear-gradient(135deg, var(--primary-color, #3b82f6), #8b5cf6);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  margin-bottom: 16px;
  display: inline-block;
  filter: drop-shadow(0 4px 6px rgba(59, 130, 246, 0.2));
}

.auth-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary, #1f2937);
  margin: 0 0 8px 0;
  letter-spacing: -0.5px;
}

.auth-subtitle {
  font-size: 14px;
  color: var(--text-secondary, #6b7280);
  margin: 0;
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
}

.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.input-icon {
  position: absolute;
  left: 16px;
  color: var(--text-secondary, #9ca3af);
  font-size: 16px;
  transition: color 0.3s ease;
}

.input {
  width: 100%;
  padding: 14px 16px 14px 44px;
  border: 1px solid var(--border-color, #e5e7eb);
  border-radius: 12px;
  font-size: 15px;
  color: var(--text-primary, #1f2937);
  background: var(--bg-primary, #f9fafb);
  transition: all 0.3s ease;
  outline: none;
}

.input:focus {
  border-color: var(--primary-color, #3b82f6);
  background: var(--bg-secondary, #ffffff);
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.1);
}

.input-wrapper:focus-within .input-icon {
  color: var(--primary-color, #3b82f6);
}

.error-message {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background-color: rgba(231, 76, 60, 0.1);
  border: 1px solid var(--danger-color, #e74c3c);
  border-radius: 12px;
  color: var(--danger-color, #e74c3c);
  font-size: 14px;
  animation: shake 0.5s ease-in-out;
}

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-4px); }
  75% { transform: translateX(4px); }
}

.btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  padding: 14px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 12px;
  border: none;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-primary {
  background: linear-gradient(135deg, var(--primary-color, #3b82f6), #6366f1);
  color: #ffffff;
  box-shadow: 0 4px 14px rgba(59, 130, 246, 0.3);
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.4);
}

.btn-primary:active:not(:disabled) {
  transform: translateY(0);
}

.btn-primary:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.btn-icon {
  font-size: 14px;
  transition: transform 0.3s ease;
}

.btn-primary:hover:not(:disabled) .btn-icon {
  transform: translateX(4px);
}

.guest-login-divider {
  display: flex;
  align-items: center;
  margin: 4px 0;
  color: var(--text-secondary, #9ca3af);
}

.guest-login-divider::before,
.guest-login-divider::after {
  content: "";
  flex: 1;
  height: 1px;
  background-color: var(--border-color, #e5e7eb);
}

.guest-login-divider span {
  padding: 0 16px;
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 1px;
  text-transform: uppercase;
}

.guest-btn {
  background-color: transparent;
  border: 1px solid var(--border-color, #d1d5db);
  color: var(--text-primary, #374151);
}

.guest-btn:hover {
  background-color: var(--bg-tertiary, #f3f4f6);
  border-color: var(--primary-color, #3b82f6);
  color: var(--primary-color, #3b82f6);
}

.auth-links {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}

.link-item {
  color: var(--text-secondary, #6b7280);
  text-decoration: none;
  font-size: 14px;
  transition: color 0.2s ease;
}

.link-item:hover {
  color: var(--primary-color, #3b82f6);
}

.highlight {
  color: var(--primary-color, #3b82f6);
  font-weight: 500;
}

@media (max-width: 900px) {
  .auth-container {
    flex-direction: column;
    justify-content: center;
    gap: 40px;
  }

  .auth-info {
    text-align: center;
    padding-right: 0;
  }

  .feature-list {
    align-items: center;
  }
}

@media (max-width: 768px) {
  .auth-card {
    padding: 32px 24px;
    border-radius: 20px;
  }

  .auth-title {
    font-size: 24px;
  }

  .shape-1 {
    width: 300px;
    height: 300px;
  }

  .shape-2 {
    width: 250px;
    height: 250px;
  }
}
</style>
