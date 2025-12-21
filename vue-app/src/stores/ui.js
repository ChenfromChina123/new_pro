import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUIStore = defineStore('ui', () => {
  const showToastMsg = ref(false)
  const toastMessage = ref('')
  let toastTimer = null

  /**
   * 显示全局提示
   * @param {string} msg 提示内容
   * @param {number} duration 持续时间（毫秒）
   */
  const showToast = (msg, duration = 3000) => {
    toastMessage.value = msg
    showToastMsg.value = true
    if (toastTimer) clearTimeout(toastTimer)
    toastTimer = setTimeout(() => {
      showToastMsg.value = false
    }, duration)
  }

  return {
    showToastMsg,
    toastMessage,
    showToast
  }
})
