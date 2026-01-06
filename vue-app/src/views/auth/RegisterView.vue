<template>
  <div class="auth-page">
    <div class="auth-card">
      <h2 class="auth-title">
        AI学习助手 - 注册
      </h2>
      
      <form
        class="auth-form"
        @submit.prevent="handleRegister"
      >
        <div class="form-group">
          <label>用户名</label>
          <input
            v-model="form.username"
            type="text"
            class="input"
            placeholder="请输入用户名"
            required
          >
        </div>
        
        <div class="form-group">
          <label>邮箱</label>
          <div class="input-group">
            <input
              v-model="form.email"
              type="email"
              class="input"
              placeholder="请输入邮箱"
              required
            >
            <button
              type="button"
              class="btn btn-secondary"
              :disabled="isLoading || countdown > 0"
              @click="sendCode"
            >
              {{ countdown > 0 ? `${countdown}秒后重试` : '发送验证码' }}
            </button>
          </div>
        </div>
        
        <div class="form-group">
          <label>验证码</label>
          <input
            v-model="form.code"
            type="text"
            class="input"
            placeholder="请输入验证码"
            required
          >
        </div>
        
        <div class="form-group">
          <label>密码</label>
          <input
            v-model="form.password"
            type="password"
            class="input"
            placeholder="请输入密码（至少6位）"
            required
            minlength="6"
          >
        </div>
        
        <div class="form-group">
          <label>确认密码</label>
          <input
            v-model="form.confirmPassword"
            type="password"
            class="input"
            placeholder="请再次输入密码"
            required
          >
        </div>
        
        <div
          v-if="errorMessage"
          class="error-message"
        >
          {{ errorMessage }}
        </div>
        
        <div
          v-if="successMessage"
          class="success-message"
        >
          {{ successMessage }}
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
          {{ isLoading ? '注册中...' : '注册' }}
        </button>
        
        <div class="auth-links">
          <router-link to="/login">
            已有账号？立即登录
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

const router = useRouter()
const authStore = useAuthStore()

const form = ref({
  username: '',
  email: '',
  code: '',
  password: '',
  confirmPassword: ''
})

const isLoading = ref(false)
const countdown = ref(0)
const errorMessage = ref('')
const successMessage = ref('')

const sendCode = async () => {
  if (!form.value.email) {
    errorMessage.value = '请输入邮箱'
    return
  }
  
  isLoading.value = true
  errorMessage.value = ''
  
  const result = await authStore.sendVerificationCode(form.value.email)
  
  if (result.success) {
    successMessage.value = '验证码已发送，请查收邮件'
    countdown.value = 60
    
    const timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer)
      }
    }, 1000)
  } else {
    errorMessage.value = result.message
  }
  
  isLoading.value = false
}

const handleRegister = async () => {
  errorMessage.value = ''
  successMessage.value = ''
  
  if (form.value.password !== form.value.confirmPassword) {
    errorMessage.value = '两次输入的密码不一致'
    return
  }
  
  if (form.value.password.length < 6) {
    errorMessage.value = '密码长度至少为6位'
    return
  }
  
  isLoading.value = true
  
  const result = await authStore.register(
    form.value.email,
    form.value.password,
    form.value.code,
    form.value.username
  )
  
  if (result.success) {
    successMessage.value = '注册成功！即将跳转到登录页...'
    setTimeout(() => {
      router.push('/login')
    }, 2000)
  } else {
    errorMessage.value = result.message
  }
  
  isLoading.value = false
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

.input-group {
  display: flex;
  gap: 8px;
}

.input-group .input {
  flex: 1;
}

.input-group .btn {
  white-space: nowrap;
  padding: 10px 16px;
}

.error-message {
  padding: 12px;
  background-color: rgba(231, 76, 60, 0.1);
  border: 1px solid var(--danger-color);
  border-radius: 8px;
  color: var(--danger-color);
  font-size: 14px;
}

.success-message {
  padding: 12px;
  background-color: rgba(39, 174, 96, 0.1);
  border: 1px solid var(--success-color);
  border-radius: 8px;
  color: var(--success-color);
  font-size: 14px;
}

.btn-primary {
  width: 100%;
  padding: 14px;
  font-size: 16px;
  margin-top: 8px;
}
.input-group .btn {
  width: auto;
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
  
  .input-group {
    flex-direction: column;
  }
  
  .input-group .btn {
    width: 100%;
    margin-top: 0;
  }
}
</style>

