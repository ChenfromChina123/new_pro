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

  // --- Terminal UI State Persistence ---
  // Load initial states from localStorage or defaults
  const getStoredState = (key, defaultVal) => {
    const stored = localStorage.getItem(`terminal_ui_${key}`)
    return stored !== null ? JSON.parse(stored) : defaultVal
  }

  const sidebarCollapsed = ref(getStoredState('sidebarCollapsed', false))
  const rightPanelCollapsed = ref(getStoredState('rightPanelCollapsed', false))
  const taskListCollapsed = ref(getStoredState('taskListCollapsed', true))
  
  const sidebarWidth = ref(getStoredState('sidebarWidth', 260))
  const rightPanelWidth = ref(getStoredState('rightPanelWidth', window.innerWidth * 0.4))
  const activeTab = ref(getStoredState('activeTab', 'terminal'))
  const tabOrder = ref(getStoredState('tabOrder', ['terminal', 'files']))

  /**
   * Save a state to localStorage
   * @param {string} key 
   * @param {any} value 
   */
  const saveState = (key, value) => {
    localStorage.setItem(`terminal_ui_${key}`, JSON.stringify(value))
  }

  return {
    showToastMsg,
    toastMessage,
    showToast,
    // Terminal UI States
    sidebarCollapsed,
    rightPanelCollapsed,
    taskListCollapsed,
    sidebarWidth,
    rightPanelWidth,
    activeTab,
    tabOrder,
    saveState
  }
})
