// API配置
const getBaseURL = () => {
  if (import.meta.env.VITE_API_BASE_URL) {
    return import.meta.env.VITE_API_BASE_URL;
  }
  // 如果当前主机名是 aistudy.icu，则自动切换到该域名的 API
  if (typeof window !== 'undefined' && window.location.hostname === 'aistudy.icu') {
    return ''; // 使用相对路径，自动跟随当前域名、协议和端口（Nginx 转发）
  }
  return 'http://localhost:5000';
};

export const API_CONFIG = {
  baseURL: getBaseURL(),
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
}

// API端点
export const API_ENDPOINTS = {
  // 认证相关
  auth: {
    register: '/api/auth/register',
    login: '/api/auth/login',
    sendVerificationCode: '/api/auth/register/send-code',
    forgotPassword: '/api/auth/forgot-password',
    sendResetCode: '/api/auth/forgot-password/send-code',
    uploadAvatar: '/api/users/upload-avatar'
  },
  
  // AI问答
  chat: {
    askStream: '/api/ask-stream',
    ask: '/api/ask',
    saveRecord: '/api/chat-records/save',
    getSessions: '/api/chat-records/sessions',
    getSessionMessages: (sessionId) => `/api/chat-records/session/${sessionId}`,
    createSession: '/api/chat-records/new-session',
    deleteSession: (sessionId) => `/api/chat-records/session/${sessionId}`
  },
  
  // 云盘管理
  cloudDisk: {
    initFolderStructure: '/api/cloud_disk/init-folder-structure',
    upload: '/api/cloud_disk/upload',
    uploadFolder: '/api/cloud_disk/upload-folder',
    uploadFolderStream: '/api/cloud_disk/upload-folder-stream',
    files: '/api/cloud_disk/files',
    folders: '/api/cloud_disk/folders',
    createFolder: '/api/cloud_disk/create-folder',
    download: (fileId) => `/api/cloud_disk/download/${fileId}`,
    downloadFolder: '/api/cloud_disk/download-folder',
    delete: (fileId) => `/api/cloud_disk/delete/${fileId}`,
    deleteFolder: '/api/cloud_disk/delete-folder',
    moveFile: '/api/cloud_disk/move-file',
    renameFolder: '/api/cloud_disk/rename-folder',
    renameFile: (fileId) => `/api/cloud_disk/rename-file?fileId=${fileId}`,
    resolveRenameFile: (fileId) => `/api/cloud_disk/resolve-rename-file?fileId=${fileId}`,
    quota: '/api/cloud_disk/quota',
    getContent: (fileId) => `/api/cloud_disk/content/${fileId}`,
    updateContent: (fileId) => `/api/cloud_disk/content/${fileId}`
  },
  
  // 管理后台
  admin: {
    statistics: '/api/admin/statistics',
    users: '/api/admin/users',
    files: '/api/admin/files',
    getFileContent: (fileId) => `/api/admin/files/content/${fileId}`,
    updateFileContent: (fileId) => `/api/admin/files/content/${fileId}`
  },
  
  // 自定义模型
  customModels: {
    list: '/api/custom-models',
    create: '/api/custom-models',
    update: (modelId) => `/api/custom-models/${modelId}`,
    delete: (modelId) => `/api/custom-models/${modelId}`,
    test: (modelId) => `/api/custom-models/${modelId}/test`
  },
  
  // 反馈相关
  feedback: {
    create: '/api/feedback',
    list: '/api/feedback',
    detail: (feedbackId) => `/api/feedback/${feedbackId}`,
    admin: {
      list: '/api/feedback/admin/all',
      update: (feedbackId) => `/api/feedback/admin/${feedbackId}`,
      delete: (feedbackId) => `/api/feedback/admin/${feedbackId}`
    }
  },
  
  // 笔记相关
  notes: {
    save: '/api/notes/save',
    list: '/api/notes/list',
    detail: (noteId) => `/api/notes/${noteId}`,
    delete: (noteId) => `/api/notes/${noteId}`
  },
  
  // 用户设置
  settings: {
    get: '/api/settings',
    update: '/api/settings',
    delete: '/api/settings'
  },
  
  // 词汇学习
  vocabulary: {
    lists: '/api/vocabulary/lists',
    words: (listId) => `/api/vocabulary/lists/${listId}/words`,
    addWord: (listId) => `/api/vocabulary/lists/${listId}/words`,
    listProgress: (listId) => `/api/vocabulary/lists/${listId}/progress`,
    deleteList: (listId) => `/api/vocabulary/lists/${listId}`,
    deleteWord: (wordId) => `/api/vocabulary/words/${wordId}`,
    updateProgress: '/api/vocabulary/progress',
    review: '/api/vocabulary/review',
    stats: '/api/vocabulary/stats',
    activity: '/api/vocabulary/activity',
    searchPublic: '/api/vocabulary/public/search',
    generateTopics: '/api/vocabulary/articles/topics',
    generateArticle: '/api/vocabulary/articles/generate',
    getArticles: '/api/vocabulary/articles',
    getArticle: (articleId) => `/api/vocabulary/articles/${articleId}`,
    downloadArticlePdf: (articleId) => `/api/vocabulary/articles/${articleId}/download-pdf`
  },

  // 公共文件
  publicFiles: {
    list: '/api/public-files',
    download: (filename) => `/api/public-files/download/${filename}`,
    upload: '/api/public-files/upload'
  }
}

