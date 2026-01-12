<template>
  <div class="auth-page">
    <div class="auth-card">
      <h2 class="auth-title">
        AI学习助手 - 登录
      </h2>
      
      <form
        class="auth-form"
        @submit.prevent="handleLogin"
      >
        <div class="form-group">
          <label>邮箱</label>
          <input
            v-model="form.email"
            type="email"
            class="input"
            placeholder="请输入邮箱"
            required
            autocomplete="off"
          >
        </div>
        
        <div class="form-group">
          <label>密码</label>
          <input
            v-model="form.password"
            type="password"
            class="input"
            placeholder="请输入密码"
            required
            autocomplete="new-password"
          >
        </div>
        
        <div
          v-if="errorMessage"
          class="error-message"
        >
          {{ errorMessage }}
        </div>
        
        <button
          type="submit"
          class="btn btn-primary"
          :disabled="isLoading"
        >
          <span
            v-if="isLoading"
            class="loading"
          />
          {{ isLoading ? '登录中...' : '登录' }}
        </button>

        <div class="guest-login-divider">
          <span>或</span>
        </div>

        <button
          type="button"
          class="btn btn-secondary guest-btn"
          @click="handleGuestLogin"
        >
          <i class="fas fa-user-secret" style="margin-right: 8px;"></i>
          游客试用
        </button>
        
        <div class="auth-links">
          <router-link to="/register">
            还没有账号？立即注册
          </router-link>
          <br>
          <router-link to="/forgot-password">
            忘记密码？重置密码
          </router-link>
        </div>
      </form>
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
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: linear-gradient(135deg, var(--gradient-start), var(--gradient-end));
}

.auth-card {
  background-color: var(--bg-secondary);
  border-radius: 16px;
  padding: 40px;
  max-width: 450px;
  width: 100%;
  box-shadow: 0 30px 60px rgba(0, 0, 0, 0.3);
}

.auth-title {
  font-size: 24px;
  font-weight: 600;
  text-align: center;
  margin-bottom: 32px;
  color: var(--text-primary);
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-group label {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.error-message {
  padding: 12px;
  background-color: rgba(231, 76, 60, 0.1);
  border: 1px solid var(--danger-color);
  border-radius: 8px;
  color: var(--danger-color);
  font-size: 14px;
}

.btn {
  width: 100%;
  padding: 14px;
  font-size: 16px;
  margin-top: 8px;
}

.guest-login-divider {
  display: flex;
  align-items: center;
  margin: 8px 0;
  color: var(--text-secondary);
}

.guest-login-divider::before,
.guest-login-divider::after {
  content: "";
  flex: 1;
  height: 1px;
  background-color: var(--border-color);
}

.guest-login-divider span {
  padding: 0 12px;
  font-size: 14px;
}

.guest-btn {
  background-color: transparent;
  border: 1px solid var(--border-color);
  color: var(--text-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.2s;
}

.guest-btn:hover {
  background-color: var(--bg-tertiary);
  border-color: var(--primary-color);
  color: var(--primary-color);
}

.auth-links {
  text-align: center;
  margin-top: 16px;
}

.auth-links a {
  color: var(--primary-color);
  text-decoration: none;
  font-size: 14px;
}

.auth-links a:hover {
  text-decoration: underline;
}

@media (max-width: 768px) {
  .auth-card {
    padding: 24px;
    border-radius: 12px;
    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
  }

  .auth-title {
    font-size: 20px;
    margin-bottom: 24px;
  }

  .auth-page {
    padding: 16px;
  }
}
</style>

