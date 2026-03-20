/**
 * 格式化工具函数
 */

/**
 * 格式化文件大小
 * @param {number} bytes - 字节数
 * @returns {string} 格式化的大小
 */
export function formatFileSize(bytes) {
  if (bytes === 0) return '0 B'
  
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  const k = 1024
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + units[i]
}

/**
 * 格式化日期时间
 * @param {string|number} dateInput - 日期字符串或时间戳
 * @param {string} format - 格式化模式 'full' | 'date' | 'time' | 'relative'
 * @returns {string} 格式化的日期时间
 */
export function formatDateTime(dateInput, format = 'full') {
  if (!dateInput) return '-'
  
  const date = new Date(dateInput)
  if (isNaN(date.getTime())) return '-'
  
  const now = new Date()
  const diff = now - date
  
  if (format === 'relative') {
    // 相对时间
    if (diff < 60000) return '刚刚'
    if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
    if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
    if (diff < 604800000) return `${Math.floor(diff / 86400000)}天前`
  }
  
  if (format === 'time') {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  
  if (format === 'date') {
    return date.toLocaleDateString('zh-CN')
  }
  
  // 完整格式
  // 今天
  if (diff < 86400000 && date.getDate() === now.getDate()) {
    return '今天 ' + date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  
  // 昨天
  const yesterday = new Date(now)
  yesterday.setDate(yesterday.getDate() - 1)
  if (date.getDate() === yesterday.getDate()) {
    return '昨天 ' + date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  
  // 今年
  if (date.getFullYear() === now.getFullYear()) {
    return date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' }) + ' ' + 
           date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  
  // 其他
  return date.toLocaleDateString('zh-CN') + ' ' + 
         date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

/**
 * 格式化数字（添加千分位）
 * @param {number} num - 数字
 * @returns {string} 格式化后的字符串
 */
export function formatNumber(num) {
  if (num === null || num === undefined) return '0'
  return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}

/**
 * 格式化百分比
 * @param {number} value - 值 (0-1 之间)
 * @param {number} decimals - 小数位数
 * @returns {string} 格式化后的百分比
 */
export function formatPercent(value, decimals = 0) {
  if (value === null || value === undefined) return '0%'
  return (value * 100).toFixed(decimals) + '%'
}

/**
 * 格式化持续时间
 * @param {number} seconds - 秒数
 * @returns {string} 格式化后的持续时间
 */
export function formatDuration(seconds) {
  if (!seconds || seconds <= 0) return '0 秒'
  
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const secs = Math.floor(seconds % 60)
  
  const parts = []
  if (hours > 0) parts.push(`${hours}小时`)
  if (minutes > 0) parts.push(`${minutes}分钟`)
  if (secs > 0) parts.push(`${secs}秒`)
  
  return parts.join('') || '0 秒'
}

/**
 * 截断文本
 * @param {string} text - 文本
 * @param {number} maxLength - 最大长度
 * @param {string} suffix - 后缀
 * @returns {string} 截断后的文本
 */
export function truncateText(text, maxLength = 50, suffix = '...') {
  if (!text) return ''
  if (text.length <= maxLength) return text
  return text.substring(0, maxLength) + suffix
}

/**
 * 格式化手机号
 * @param {string} phone - 手机号
 * @returns {string} 格式化后的手机号（中间 4 位隐藏）
 */
export function formatPhone(phone) {
  if (!phone) return ''
  const str = phone.toString()
  if (str.length === 11) {
    return str.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
  }
  return str
}

/**
 * 格式化邮箱
 * @param {string} email - 邮箱
 * @returns {string} 格式化后的邮箱（部分隐藏）
 */
export function formatEmail(email) {
  if (!email) return ''
  const [username, domain] = email.split('@')
  if (!username || !domain) return email
  
  const maskedUsername = username.length > 2 
    ? username[0] + '*'.repeat(username.length - 2) + username[username.length - 1]
    : username
  
  return `${maskedUsername}@${domain}`
}

/**
 * 驼峰转短横线
 * @param {string} str - 驼峰命名字符串
 * @returns {string} 短横线命名字符串
 */
export function camelToKebab(str) {
  return str.replace(/([a-z])([A-Z])/g, '$1-$2').toLowerCase()
}

/**
 * 短横线转驼峰
 * @param {string} str - 短横线命名字符串
 * @returns {string} 驼峰命名字符串
 */
export function kebabToCamel(str) {
  return str.replace(/-([a-z])/g, (match, letter) => letter.toUpperCase())
}

/**
 * 首字母大写
 * @param {string} str - 字符串
 * @returns {string} 首字母大写的字符串
 */
export function capitalize(str) {
  if (!str) return ''
  return str.charAt(0).toUpperCase() + str.slice(1)
}

/**
 * 转义 HTML 特殊字符
 * @param {string} str - 原始字符串
 * @returns {string} 转义后的字符串
 */
export function escapeHtml(str) {
  if (!str) return ''
  const map = {
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#039;'
  }
  return str.replace(/[&<>"']/g, m => map[m])
}

/**
 * 去除 HTML 标签
 * @param {string} html - HTML 字符串
 * @returns {string} 纯文本
 */
export function stripHtml(html) {
  if (!html) return ''
  return html.replace(/<[^>]*>/g, '')
}

export default {
  formatFileSize,
  formatDateTime,
  formatNumber,
  formatPercent,
  formatDuration,
  truncateText,
  formatPhone,
  formatEmail,
  camelToKebab,
  kebabToCamel,
  capitalize,
  escapeHtml,
  stripHtml
}
