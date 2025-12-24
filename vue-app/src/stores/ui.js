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
  
  // 自动迁移：确保所有标签都在 tabOrder 中
  const allTabs = ['terminal', 'files', 'checkpoints', 'approvals', 'session', 'decisions', 'identity', 'state']
  let storedTabOrder = getStoredState('tabOrder', allTabs)
  
  // 检查是否有缺失的标签
  const missingTabs = allTabs.filter(tab => !storedTabOrder.includes(tab))
  if (missingTabs.length > 0) {
    storedTabOrder = [...storedTabOrder, ...missingTabs]
    localStorage.setItem('terminal_ui_tabOrder', JSON.stringify(storedTabOrder))
  }
  
  const tabOrder = ref(storedTabOrder)

  /**
   * Save a state to localStorage and update reactive ref
   * @param {string} key 
   * @param {any} value 
   */
  const saveState = (key, value) => {
    localStorage.setItem(`terminal_ui_${key}`, JSON.stringify(value))
    
    // Update reactive ref if it exists
    if (key === 'sidebarCollapsed') sidebarCollapsed.value = value
    else if (key === 'rightPanelCollapsed') rightPanelCollapsed.value = value
    else if (key === 'taskListCollapsed') taskListCollapsed.value = value
    else if (key === 'sidebarWidth') sidebarWidth.value = value
    else if (key === 'rightPanelWidth') rightPanelWidth.value = value
    else if (key === 'activeTab') activeTab.value = value
    else if (key === 'tabOrder') tabOrder.value = value
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
