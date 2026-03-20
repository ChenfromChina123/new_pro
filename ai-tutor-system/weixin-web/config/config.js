/**
 * 小程序配置
 */

// API 基础地址
// 注意：小程序必须使用完整的 URL，不支持相对路径
const API_BASE_URL = 'https://your-domain.com/api' // 请替换为实际的域名

// 或者在开发环境使用本地代理
// const API_BASE_URL = 'http://localhost:5000/api'

export default {
  // API 配置
  apiBase: API_BASE_URL,
  
  // 超时时间（毫秒）
  timeout: 120000,
  
  // 请求头
  headers: {
    'Content-Type': 'application/json'
  },
  
  // 文件上传大小限制（字节）
  maxUploadSize: 100 * 1024 * 1024, // 100MB
  
  // 分页配置
  pageSize: 20,
  
  // 是否启用调试模式
  debug: true,
  
  // 版本信息
  version: '1.0.0',
  
  // 功能开关
  features: {
    enableChat: true,
    enableCloudDisk: true,
    enableWordGame: true,
    enableAiArticle: true,
    enablePublicFiles: true
  }
}
