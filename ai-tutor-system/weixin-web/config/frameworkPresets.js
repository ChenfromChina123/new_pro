/**
 * 框架预设配置
 * 微信小程序专用配置
 */

/**
 * 微信小程序框架预设
 */
export const weappPreset = {
  name: '微信小程序',
  id: 'weapp',
  icon: '💚',
  languages: ['javascript', 'wxml', 'wxss'],
  keyFilePatterns: [
    'app.js',
    'app.json',
    'app.wxss',
    'project.config.json'
  ],
  projectStructure: [
    'pages/',
    'components/',
    'utils/',
    'images/',
    'styles/'
  ],
  commonCommands: {
    dev: '微信开发者工具 - 编译',
    build: '微信开发者工具 - 上传',
    preview: '微信开发者工具 - 预览'
  },
  envVariables: [],
  packageManager: 'npm',
  templates: [
    {
      title: '创建页面',
      description: '创建新的小程序页面',
      template: '请创建一个小程序页面 ${pageName}，包含 wxml、wxss、js、json 文件'
    },
    {
      title: '创建组件',
      description: '创建新的小程序组件',
      template: '请创建一个小程序组件 ${componentName}，包含 properties 和 methods'
    },
    {
      title: '添加 API 调用',
      description: '添加微信 API 调用',
      template: '请使用 wx.${apiName} 实现 ${functionality} 功能'
    }
  ]
}

/**
 * 可用的模型列表
 */
export const AVAILABLE_MODELS = [
  {
    id: 'deepseek-chat',
    name: 'DeepSeek Chat',
    provider: 'DeepSeek',
    description: '深度求索对话模型'
  },
  {
    id: 'doubao',
    name: '豆包',
    provider: '字节跳动',
    description: '字节跳动豆包模型'
  },
  {
    id: 'qwen-plus',
    name: '通义千问 Plus',
    provider: '阿里巴巴',
    description: '阿里巴巴通义千问'
  }
]

/**
 * 获取模型列表
 */
export function getAvailableModels() {
  return AVAILABLE_MODELS
}

/**
 * 根据 ID 获取模型
 */
export function getModelById(modelId) {
  return AVAILABLE_MODELS.find(m => m.id === modelId)
}
