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
import { ref, onMounted } from 'vue'
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

// 开发阶段自动登录
onMounted(() => {
  form.value.email = '3301767269@qq.com'
  form.value.password = '123456'
  handleLogin()
})
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
</style>

