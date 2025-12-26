import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

export const useThemeStore = defineStore('theme', () => {
  // 核心状态：只由这个 store 管理
  const isDarkMode = ref(localStorage.getItem('darkMode') === 'true')
  
  // 内部同步方法
  const applyTheme = (value) => {
    if (value) {
      document.body.classList.add('dark-mode')
    } else {
      document.body.classList.remove('dark-mode')
    }
    localStorage.setItem('darkMode', value.toString())
  }

  // 切换主题
  function toggleDarkMode() {
    isDarkMode.value = !isDarkMode.value
    applyTheme(isDarkMode.value)
    return isDarkMode.value
  }
  
  // 显式设置主题
  function setDarkMode(value) {
    if (isDarkMode.value === value) return
    isDarkMode.value = value
    applyTheme(value)
  }
  
  // 初始化
  applyTheme(isDarkMode.value)
  
  return {
    isDarkMode,
    toggleDarkMode,
    setDarkMode
  }
})

