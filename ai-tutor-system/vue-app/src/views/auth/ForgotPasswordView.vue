<template>
  <div class="auth-page">
    <div class="auth-card">
      <h2 class="auth-title">
        🔐 重置密码
      </h2>
      
      <form
        class="auth-form"
        @submit.prevent="handleResetPassword"
      >
        <div class="form-group">
          <label>邮箱</label>
          <input
            v-model="form.email"
            type="email"
            class="input"
            placeholder="请输入注册时的邮箱"
            required
          >
        </div>
        
        <div class="form-group">
          <label>验证码</label>
          <div class="verify-code-group">
            <input
              v-model="form.verificationCode"
              type="text"
              class="input"
              placeholder="请输入6位验证码"
              maxlength="6"
              required
            >
            <button
              type="button"
              class="btn btn-secondary code-btn"
              :disabled="isSendingCode || countdown > 0"
              @click="handleSendCode"
            >
              {{ countdown > 0 ? `${countdown}s后重发` : '获取验证码' }}
            </button>
          </div>
        </div>
        
        <div class="form-group">
          <label>新密码</label>
          <input
            v-model="form.newPassword"
            type="password"
            class="input"
            placeholder="请输入新密码（6-20字符）"
            required
          >
        </div>
        
        <div class="form-group">
          <label>确认新密码</label>
          <input
            v-model="form.confirmPassword"
            type="password"
            class="input"
            placeholder="请再次输入新密码"
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
        
        <div class="form-actions">
          <button
            type="button"
            class="btn btn-secondary"
            @click="handleCancel"
          >
            取消
          </button>
          <button
            type="submit"
            class="btn btn-primary"
            :disabled="isLoading"
          >
            <span
              v-if="isLoading"
              class="loading"
            />
            {{ isLoading ? '重置中...' : '重置密码' }}
          </button>
        </div>
        
        <div class="auth-links">
          <router-link to="/login">
            返回登录
          </router-link>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const form = reactive({
  email: '',
  verificationCode: '',
  newPassword: '',
  confirmPassword: ''
})

const isLoading = ref(false)
const isSendingCode = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const countdown = ref(0)
let countdownTimer = null

const handleSendCode = async () => {
  if (!form.email) {
    errorMessage.value = '请输入邮箱'
    return
  }
  
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
    errorMessage.value = '请输入有效的邮箱格式'
    return
  }
  
  isSendingCode.value = true
  errorMessage.value = ''
  
  try {
    const result = await authStore.sendResetCode(form.email)
    if (result.success) {
      startCountdown()
      successMessage.value = '验证码已发送至邮箱，请注意查收'
    } else {
      errorMessage.value = result.message
    }
  } catch {
    errorMessage.value = '发送验证码失败，请稍后重试'
  } finally {
    isSendingCode.value = false
  }
}

const startCountdown = () => {
  countdown.value = 60
  
  if (countdownTimer) {
    clearInterval(countdownTimer)
  }
  
  countdownTimer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(countdownTimer)
      countdownTimer = null
    }
  }, 1000)
}

const handleResetPassword = async () => {
  errorMessage.value = ''
  successMessage.value = ''
  
  // 表单验证
  if (!form.email || !form.verificationCode || !form.newPassword || !form.confirmPassword) {
    errorMessage.value = '请填写所有必填字段'
    return
  }
  
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
    errorMessage.value = '请输入有效的邮箱格式'
    return
  }
  
  if (form.verificationCode.length !== 6) {
    errorMessage.value = '验证码必须为6位数字'
    return
  }
  
  if (form.newPassword.length < 6 || form.newPassword.length > 20) {
    errorMessage.value = '新密码长度需在6-20字符之间'
    return
  }
  
  if (form.newPassword !== form.confirmPassword) {
    errorMessage.value = '两次输入的密码不一致'
    return
  }
  
  isLoading.value = true
  
  try {
    const result = await authStore.resetPassword(
      form.email,
      form.verificationCode,
      form.newPassword
    )
    
    if (result.success) {
      successMessage.value = '密码重置成功！即将跳转登录页...'
      setTimeout(() => {
        router.push('/login')
      }, 2000)
    } else {
      errorMessage.value = result.message
    }
  } catch {
    errorMessage.value = '重置密码失败，请检查验证码是否正确'
  } finally {
    isLoading.value = false
  }
}

const handleCancel = () => {
  router.push('/login')
}

onUnmounted(() => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
  }
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

.verify-code-group {
  display: flex;
  gap: 10px;
  align-items: stretch;
}

.verify-code-group .input {
  flex: 1;
}

.code-btn {
  white-space: nowrap;
  min-width: 120px;
  padding: 0 16px;
  font-size: 14px;
}

.input {
  width: 100%;
  padding: 14px;
  border: 1px solid var(--card-border);
  border-radius: 8px;
  font-size: 16px;
  background-color: var(--input-bg);
  color: var(--text-primary);
  transition: border-color 0.3s, box-shadow 0.3s;
}

.input:focus {
  outline: none;
  border-color: var(--primary-color);
  box-shadow: 0 0 0 2px rgba(52, 152, 219, 0.1);
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

.form-actions {
  display: flex;
  gap: 12px;
  margin-top: 8px;
}

.btn {
  flex: 1;
  padding: 14px;
  font-size: 16px;
  font-weight: 600;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.btn-primary {
  background-color: var(--primary-color);
  color: white;
}

.btn-primary:hover {
  background-color: var(--secondary-color);
}

.btn-primary:disabled {
  background-color: var(--gray-color);
  cursor: not-allowed;
}

.btn-secondary {
  background-color: var(--light-gray);
  color: var(--dark-color);
}

.btn-secondary:hover {
  background-color: var(--gray-color);
  color: white;
}

.btn-secondary:disabled {
  background-color: var(--light-gray);
  color: var(--gray-color);
  cursor: not-allowed;
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
  
  .verify-code-group {
    flex-direction: column;
  }
  
  .code-btn {
    width: 100%;
    min-width: unset;
  }
  
  .form-actions {
    flex-direction: column;
  }
  
  .btn {
    width: 100%;
  }
}

.loading {
  display: inline-block;
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-radius: 50%;
  border-top-color: white;
  animation: spin 1s ease-in-out infinite;
  margin-right: 8px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>