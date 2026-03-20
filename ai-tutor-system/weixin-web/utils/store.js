/**
 * 全局状态管理 - 类似 Pinia 的简化版
 * 用于管理小程序的全局状态
 */

// 存储所有 store 实例
const stores = {}

// 辅助函数：从 localStorage 获取数据
function getFromStorage(key, defaultValue = null) {
  try {
    const value = wx.getStorageSync(key)
    return value ? JSON.parse(value) : defaultValue
  } catch (e) {
    console.error('getFromStorage error:', e)
    return defaultValue
  }
}

// 辅助函数：保存数据到 localStorage
function saveToStorage(key, value) {
  try {
    wx.setStorageSync(key, JSON.stringify(value))
    return true
  } catch (e) {
    console.error('saveToStorage error:', e)
    return false
  }
}

// 辅助函数：从 localStorage 移除数据
function removeFromStorage(key) {
  try {
    wx.removeStorageSync(key)
    return true
  } catch (e) {
    console.error('removeFromStorage error:', e)
    return false
  }
}

/**
 * 创建 store
 * @param {string} id - store 的唯一标识
 * @param {function} stateFn - 返回 state 对象的函数
 * @returns {object} - store 实例
 */
export function createStore(id, stateFn) {
  if (stores[id]) {
    return stores[id]
  }

  const state = stateFn()
  
  // 创建响应式对象（简化版）
  const store = {
    id,
    state,
    // 订阅者列表
    _subscribers: [],
    
    // 更新状态
    setState(updates) {
      Object.assign(this.state, updates)
      // 通知所有订阅者
      this._subscribers.forEach(callback => callback(this.state))
    },
    
    // 订阅状态变化
    subscribe(callback) {
      this._subscribers.push(callback)
      // 返回取消订阅的函数
      return () => {
        const index = this._subscribers.indexOf(callback)
        if (index > -1) {
          this._subscribers.splice(index, 1)
        }
      }
    }
  }

  stores[id] = store
  return store
}

/**
 * 获取 store 实例
 * @param {string} id - store 的唯一标识
 * @returns {object} - store 实例
 */
export function getStore(id) {
  return stores[id]
}

/**
 * 移除 store 实例
 * @param {string} id - store 的唯一标识
 */
export function removeStore(id) {
  delete stores[id]
}

// 导出工具函数
export { getFromStorage, saveToStorage, removeFromStorage }
