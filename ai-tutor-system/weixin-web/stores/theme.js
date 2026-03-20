/**
 * 主题 Store - 管理主题状态
 */
import { createStore, getFromStorage, saveToStorage } from './store'

const store = createStore('theme', () => ({
  isDarkMode: getFromStorage('darkMode', false)
}))

// 应用主题
store.applyTheme = function(value) {
  const app = getApp()
  if (value) {
    // 切换到深色模式
    if (app && app.globalData) {
      app.globalData.isDarkMode = true
    }
    // 设置 page 的 class
    const pages = getCurrentPages()
    if (pages.length > 0) {
      const currentPage = pages[pages.length - 1]
      if (currentPage) {
        currentPage.setData({ isDarkMode: true })
      }
    }
  } else {
    // 切换到浅色模式
    if (app && app.globalData) {
      app.globalData.isDarkMode = false
    }
    const pages = getCurrentPages()
    if (pages.length > 0) {
      const currentPage = pages[pages.length - 1]
      if (currentPage) {
        currentPage.setData({ isDarkMode: false })
      }
    }
  }
  saveToStorage('darkMode', value)
}

// 切换主题
store.toggleDarkMode = function() {
  const newValue = !this.state.isDarkMode
  this.state.isDarkMode = newValue
  this.applyTheme(newValue)
  this.setState({ isDarkMode: newValue })
  return newValue
}

// 设置主题
store.setDarkMode = function(value) {
  if (this.state.isDarkMode === value) return
  this.state.isDarkMode = value
  this.applyTheme(value)
  this.setState({ isDarkMode: value })
}

// 初始化主题
store.init = function() {
  this.applyTheme(this.state.isDarkMode)
}

export default store
