<template>
  <div class="word-game-view">
    <iframe
      ref="frameRef"
      :src="wordGameUrl"
      class="word-game-frame"
      allow="autoplay; clipboard-read; clipboard-write"
      title="单词记忆"
      @load="onFrameLoad"
    ></iframe>

    <!-- 加载遮罩 -->
    <div v-if="loading" class="loading-mask">
      <div class="loading-spinner">
        <i class="fas fa-circle-notch fa-spin"></i>
        <span>加载中...</span>
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
  loading.value = false
  sendThemeToFrame(themeStore.isDarkMode)
  syncAuthToFrame(authStore.isAuthenticated)
  postToFrame({ type: 'REQUEST_STATS' })
}

/** 监听主题变化 */
watch(() => themeStore.isDarkMode, sendThemeToFrame)

/** 监听登录状态变化，实时同步给 iframe */
watch(() => authStore.isAuthenticated, syncAuthToFrame)

onMounted(() => {
  if (frameRef.value?.contentDocument?.readyState === 'complete') {
    sendThemeToFrame(themeStore.isDarkMode)
  }
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
</style>
