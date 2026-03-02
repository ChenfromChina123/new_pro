<template>
  <div class="word-game-view">
    <iframe
      ref="frameRef"
      :src="wordGameUrl"
      class="word-game-frame"
      allow="autoplay; clipboard-read; clipboard-write"
      title="单词记忆"
      @load="onFrameLoad"
      @error="onFrameError"
    ></iframe>

    <!-- 加载遮罩 -->
    <div v-if="loading && !loadFailed" class="loading-mask">
      <div class="loading-spinner">
        <i class="fas fa-circle-notch fa-spin"></i>
        <span>加载中...</span>
      </div>
    </div>

    <!-- 连接失败时提示（开发环境未启动 word-game 服务时显示） -->
    <div v-if="loadFailed" class="load-failed-mask">
      <div class="load-failed-box">
        <i class="fas fa-plug-circle-xmark"></i>
        <p class="load-failed-title">无法连接单词记忆服务</p>
        <p class="load-failed-desc">localhost 拒绝了我们的连接请求，请确认已启动 word-game 服务。</p>
        <p class="load-failed-steps" v-if="isDev">
          在 <code>word-game</code> 目录下执行：<br />
          <code>npm run dev</code>（前端，端口 5200）<br />
          <code>npm run server</code>（后端 API，端口 5201）
        </p>
        <button type="button" class="btn btn-primary" @click="retryLoad">重试</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { useThemeStore } from '@/stores/theme'
import { useAuthStore } from '@/stores/auth'

// 生产环境必须指向 word-game 独立域名（earthworm.aistudy.icu → 5010），
// 若用 /word-game/ 会请求主站导致返回主站 SPA、iframe 内重复侧栏且加载不到 word-game 后端
const wordGameUrl = import.meta.env.DEV
  ? 'http://localhost:5200'
  : 'https://earthworm.aistudy.icu'

const themeStore = useThemeStore()
const authStore = useAuthStore()
const frameRef = ref(null)
const loading = ref(true)
const loadFailed = ref(false)
const isDev = import.meta.env.DEV

/** 向 iframe 发送消息 */
function postToFrame(payload) {
  frameRef.value?.contentWindow?.postMessage(payload, '*')
}

/** 同步主题到 iframe */
function sendThemeToFrame(isDark) {
  postToFrame({ type: 'AISPRING_THEME', isDark })
}

/**
 * 同步登录状态到 iframe
 * 登录时发送 token + userId，退出时发送 LOGOUT
 */
function syncAuthToFrame(isAuth) {
  if (isAuth) {
    postToFrame({
      type: 'AISPRING_LOGIN',
      token: authStore.token,
      userId: authStore.userInfo?.id,
    })
  } else {
    postToFrame({ type: 'AISPRING_LOGOUT' })
  }
}

/** iframe 加载完成：同步主题、认证状态，并请求统计数据 */
const onFrameLoad = () => {
  if (loadTimeout) {
    clearTimeout(loadTimeout)
    loadTimeout = null
  }
  loading.value = false
  loadFailed.value = false
  sendThemeToFrame(themeStore.isDarkMode)
  syncAuthToFrame(authStore.isAuthenticated)
  postToFrame({ type: 'REQUEST_STATS' })
}

/** iframe 加载失败（如未启动 word-game 服务） */
const onFrameError = () => {
  loading.value = false
  loadFailed.value = true
}

/** 重试加载 */
const retryLoad = () => {
  loadFailed.value = false
  loading.value = true
  if (frameRef.value) {
    frameRef.value.src = wordGameUrl
  }
}

/** 监听主题变化 */
watch(() => themeStore.isDarkMode, sendThemeToFrame)

/** 监听登录状态变化，实时同步给 iframe */
watch(() => authStore.isAuthenticated, syncAuthToFrame)

let loadTimeout = null
onMounted(() => {
  if (frameRef.value?.contentDocument?.readyState === 'complete') {
    sendThemeToFrame(themeStore.isDarkMode)
  }
  // 若一段时间后仍在 loading，视为连接失败（如未启动 word-game 服务）
  loadTimeout = setTimeout(() => {
    if (loading.value) {
      loading.value = false
      loadFailed.value = true
    }
  }, 8000)
})

onUnmounted(() => {
  if (loadTimeout) clearTimeout(loadTimeout)
})
</script>

<style scoped>
.word-game-view {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
  background-color: var(--bg-primary);
}

.word-game-frame {
  width: 100%;
  height: 100%;
  border: none;
  display: block;
}

.loading-mask {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--bg-primary);
  z-index: 10;
}

.loading-spinner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  color: var(--text-secondary);
  font-size: 14px;
}

.loading-spinner i {
  font-size: 32px;
  color: var(--primary-color);
}

.load-failed-mask {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--bg-primary);
  z-index: 10;
}

.load-failed-box {
  text-align: center;
  max-width: 420px;
  padding: 32px 24px;
}

.load-failed-box > i {
  font-size: 48px;
  color: var(--text-tertiary);
  margin-bottom: 16px;
}

.load-failed-title {
  font-size: 1.1rem;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 8px 0;
}

.load-failed-desc {
  font-size: 0.9rem;
  color: var(--text-secondary);
  margin: 0 0 16px 0;
  line-height: 1.5;
}

.load-failed-steps {
  font-size: 0.85rem;
  color: var(--text-tertiary);
  text-align: left;
  background: var(--bg-secondary);
  padding: 12px 16px;
  border-radius: 8px;
  margin: 0 0 20px 0;
  line-height: 1.6;
}

.load-failed-steps code {
  background: var(--bg-tertiary);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 0.8rem;
}

.load-failed-box .btn {
  margin-top: 8px;
}
</style>
