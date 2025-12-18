import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useThemeStore = defineStore('theme', () => {
  const isDarkMode = ref(localStorage.getItem('darkMode') === 'true')
  
  function toggleDarkMode() {
    isDarkMode.value = !isDarkMode.value
    localStorage.setItem('darkMode', isDarkMode.value.toString())
    
    // 更新body类名
    if (isDarkMode.value) {
      document.body.classList.add('dark-mode')
    } else {
      document.body.classList.remove('dark-mode')
    }
  }
  
  function setDarkMode(value) {
    isDarkMode.value = value
    localStorage.setItem('darkMode', value.toString())
    
    if (value) {
      document.body.classList.add('dark-mode')
    } else {
      document.body.classList.remove('dark-mode')
    }
  }
  
  // 初始化时设置
  if (isDarkMode.value) {
    document.body.classList.add('dark-mode')
  }
  
  return {
    isDarkMode,
    toggleDarkMode,
    setDarkMode
  }
})

