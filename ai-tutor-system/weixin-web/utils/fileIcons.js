/**
 * 文件图标映射工具
 * 根据文件扩展名返回对应的图标
 */

/**
 * 文件类型图标映射
 */
const iconMap = {
  // 文件夹
  folder: '📁',
  
  // 代码文件
  js: '📜',
  ts: '📘',
  jsx: '⚛️',
  tsx: '⚛️',
  vue: '💚',
  html: '🌐',
  css: '🎨',
  scss: '🎨',
  less: '🎨',
  json: '📋',
  xml: '📋',
  yaml: '📋',
  yml: '📋',
  wxml: '📱',
  wxss: '🎨',
  wxs: '📜',
  
  // 编程语言
  py: '🐍',
  java: '☕',
  kt: '🟣',
  go: '🔵',
  rs: '🦀',
  c: '🔵',
  cpp: '🔵',
  h: '📄',
  hpp: '📄',
  cs: '💜',
  php: '🐘',
  rb: '💎',
  swift: '🍎',
  scala: '🔴',
  
  // Shell 脚本
  sh: '🖥️',
  bash: '🖥️',
  zsh: '🖥️',
  bat: '🖥️',
  ps1: '🖥️',
  
  // 配置文件
  env: '⚙️',
  gitignore: '🙈',
  dockerignore: '🐳',
  editorconfig: '📝',
  eslintrc: '✨',
  prettierrc: '✨',
  
  // 数据文件
  sql: '🗃️',
  db: '🗃️',
  sqlite: '🗃️',
  
  // 文档
  md: '📝',
  txt: '📄',
  pdf: '📕',
  doc: '📘',
  docx: '📘',
  xls: '📗',
  xlsx: '📗',
  ppt: '📙',
  pptx: '📙',
  
  // 图片
  png: '🖼️',
  jpg: '🖼️',
  jpeg: '🖼️',
  gif: '🖼️',
  svg: '🖼️',
  webp: '🖼️',
  ico: '🖼️',
  bmp: '🖼️',
  
  // 音频
  mp3: '🎵',
  wav: '🎵',
  flac: '🎵',
  aac: '🎵',
  ogg: '🎵',
  
  // 视频
  mp4: '🎬',
  avi: '🎬',
  mkv: '🎬',
  mov: '🎬',
  wmv: '🎬',
  flv: '🎬',
  
  // 压缩文件
  zip: '📦',
  rar: '📦',
  tar: '📦',
  gz: '📦',
  '7z': '📦',
  bz2: '📦',
  
  // 可执行文件
  exe: '⚡',
  msi: '⚡',
  app: '⚡',
  dmg: '⚡',
  deb: '⚡',
  rpm: '⚡',
  
  // 其他
  log: '📋',
  lock: '🔒',
  map: '🗺️',
}

/**
 * 特殊文件名映射
 */
const specialFileMap = {
  'package.json': '📦',
  'package-lock.json': '🔒',
  'yarn.lock': '🔒',
  'pnpm-lock.yaml': '🔒',
  'tsconfig.json': '📘',
  'jsconfig.json': '📜',
  'vite.config.js': '⚡',
  'vite.config.ts': '⚡',
  'webpack.config.js': '📦',
  'rollup.config.js': '📦',
  '.gitignore': '🙈',
  '.env': '⚙️',
  '.env.local': '⚙️',
  '.env.development': '⚙️',
  '.env.production': '⚙️',
  'Dockerfile': '🐳',
  'docker-compose.yml': '🐳',
  'docker-compose.yaml': '🐳',
  'Makefile': '🔧',
  'README.md': '📖',
  'LICENSE': '📜',
  'CHANGELOG.md': '📝',
  '.eslintrc': '✨',
  '.eslintrc.js': '✨',
  '.eslintrc.json': '✨',
  '.prettierrc': '✨',
  '.prettierrc.js': '✨',
  '.prettierrc.json': '✨',
  'app.json': '📱',
  'app.js': '📱',
  'app.wxss': '📱',
  'project.config.json': '⚙️',
  'sitemap.json': '🗺️',
}

/**
 * 获取文件图标
 * @param {string} fileName - 文件名
 * @param {boolean} isDirectory - 是否为目录
 * @returns {string} 图标
 */
export function getFileIcon(fileName, isDirectory = false) {
  if (isDirectory) {
    return iconMap.folder
  }
  
  // 检查特殊文件名
  if (specialFileMap[fileName]) {
    return specialFileMap[fileName]
  }
  
  // 获取扩展名
  const ext = fileName.split('.').pop()?.toLowerCase()
  
  if (ext && iconMap[ext]) {
    return iconMap[ext]
  }
  
  // 默认图标
  return '📄'
}

/**
 * 获取文件类型
 * @param {string} fileName - 文件名
 * @returns {string} 文件类型
 */
export function getFileType(fileName) {
  const ext = fileName.split('.').pop()?.toLowerCase()
  
  if (!ext) return 'file'
  
  // 代码文件
  if (['js', 'ts', 'jsx', 'tsx', 'vue', 'html', 'css', 'scss', 'less', 'json', 'xml', 'yaml', 'yml', 'wxml', 'wxss', 'wxs'].includes(ext)) {
    return 'code'
  }
  
  // 编程语言
  if (['py', 'java', 'kt', 'go', 'rs', 'c', 'cpp', 'h', 'hpp', 'cs', 'php', 'rb', 'swift', 'scala'].includes(ext)) {
    return 'code'
  }
  
  // Shell 脚本
  if (['sh', 'bash', 'zsh', 'bat', 'ps1'].includes(ext)) {
    return 'script'
  }
  
  // 图片
  if (['png', 'jpg', 'jpeg', 'gif', 'svg', 'webp', 'ico', 'bmp'].includes(ext)) {
    return 'image'
  }
  
  // 音频
  if (['mp3', 'wav', 'flac', 'aac', 'ogg'].includes(ext)) {
    return 'audio'
  }
  
  // 视频
  if (['mp4', 'avi', 'mkv', 'mov', 'wmv', 'flv'].includes(ext)) {
    return 'video'
  }
  
  // 压缩文件
  if (['zip', 'rar', 'tar', 'gz', '7z', 'bz2'].includes(ext)) {
    return 'archive'
  }
  
  // 文档
  if (['md', 'txt', 'pdf', 'doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx'].includes(ext)) {
    return 'document'
  }
  
  return 'file'
}

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
 * @param {string} dateString - 日期字符串
 * @returns {string} 格式化的日期时间
 */
export function formatDateTime(dateString) {
  if (!dateString) return '-'
  
  const date = new Date(dateString)
  const now = new Date()
  const diff = now - date
  
  // 今天
  if (diff < 86400000 && date.getDate() === now.getDate()) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
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

export default {
  getFileIcon,
  getFileType,
  formatFileSize,
  formatDateTime
}
