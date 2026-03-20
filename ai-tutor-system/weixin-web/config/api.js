/**
 * API 端点配置
 */

export const API_ENDPOINTS = {
  // 认证相关
  auth: {
    login: '/auth/login',
    register: '/auth/register',
    sendVerificationCode: '/auth/register/send-code',
    sendResetCode: '/auth/forgot-password/send-code',
    forgotPassword: '/auth/forgot-password',
    uploadAvatar: '/users/upload-avatar'
  },
  
  // AI 问答
  chat: {
    ask: '/chat/ask',
    askStream: '/ask-stream',
    saveRecord: '/chat-records/save',
    getSessions: '/chat-records/sessions',
    getSessionMessages: (sessionId) => `/chat-records/session/${sessionId}`,
    createSession: '/chat-records/new-session',
    deleteSession: (sessionId) => `/chat-records/session/${sessionId}`
  },
  
  // 云盘管理
  cloudDisk: {
    initFolderStructure: '/cloud_disk/init-folder-structure',
    upload: '/cloud_disk/upload',
    uploadFolder: '/cloud_disk/upload-folder',
    files: '/cloud_disk/files',
    folders: '/cloud_disk/folders',
    createFolder: '/cloud_disk/create-folder',
    download: (fileId) => `/cloud_disk/download/${fileId}`,
    delete: (fileId) => `/cloud_disk/delete/${fileId}`,
    deleteFolder: '/cloud_disk/delete-folder',
    moveFile: '/cloud_disk/move-file',
    renameFolder: '/cloud_disk/rename-folder',
    renameFile: (fileId) => `/cloud_disk/rename-file?fileId=${fileId}`,
    resolveRenameFile: (fileId) => `/cloud_disk/resolve-rename-file?fileId=${fileId}`,
    quota: '/cloud_disk/quota',
    getContent: (fileId) => `/cloud_disk/content/${fileId}`,
    updateContent: (fileId) => `/cloud_disk/content/${fileId}`
  },
  
  // 管理后台
  admin: {
    statistics: '/admin/statistics',
    users: '/admin/users',
    files: '/admin/files',
    getFileContent: (fileId) => `/admin/files/content/${fileId}`,
    updateFileContent: (fileId) => `/admin/files/content/${fileId}`,
    downloadFile: (fileId) => `/admin/files/download/${fileId}`,
    tokenAuditStats: '/admin/token-audit/stats',
    tokenAuditRecords: '/admin/token-audit/records',
    resources: '/resources',
    publicResources: '/resources/public',
    updateResource: (id) => `/resources/${id}`,
    uploadSoftware: (id) => `/resources/${id}/upload`,
    externalLinks: '/admin/external-links'
  },
  
  // 自定义模型
  customModels: {
    list: '/custom-models',
    create: '/custom-models',
    update: (modelId) => `/custom-models/${modelId}`,
    delete: (modelId) => `/custom-models/${modelId}`,
    test: (modelId) => `/custom-models/${modelId}/test`
  },
  
  // 反馈相关
  feedback: {
    create: '/feedback',
    list: '/feedback',
    detail: (feedbackId) => `/feedback/${feedbackId}`,
    admin: {
      list: '/feedback/admin/all',
      update: (feedbackId) => `/feedback/admin/${feedbackId}`,
      delete: (feedbackId) => `/feedback/admin/${feedbackId}`
    }
  },
  
  // 翻译功能
  translation: {
    translate: '/translation/translate'
  },
  
  // 笔记相关
  notes: {
    save: '/notes/save',
    list: '/notes/list',
    detail: (noteId) => `/notes/${noteId}`,
    delete: (noteId) => `/notes/${noteId}`
  },
  
  // 用户设置
  settings: {
    get: '/settings',
    update: '/settings',
    delete: '/settings'
  },
  
  // 词汇学习
  vocabulary: {
    lists: '/vocabulary/lists',
    words: (listId) => `/vocabulary/lists/${listId}/words`,
    addWord: (listId) => `/vocabulary/lists/${listId}/words`,
    listProgress: (listId) => `/vocabulary/lists/${listId}/progress`,
    deleteList: (listId) => `/vocabulary/lists/${listId}`,
    deleteWord: (wordId) => `/vocabulary/words/${wordId}`,
    updateProgress: '/vocabulary/progress',
    review: '/vocabulary/review',
    stats: '/vocabulary/stats',
    activity: '/vocabulary/activity',
    searchPublic: '/vocabulary/public/search',
    generateTopics: '/vocabulary/articles/topics',
    generateArticle: '/vocabulary/articles/generate',
    getArticles: '/vocabulary/articles',
    getArticle: (articleId) => `/vocabulary/articles/${articleId}`,
    downloadArticlePdf: (articleId) => `/vocabulary/articles/${articleId}/download-pdf`
  },
  
  // AI 文章
  aiArticle: {
    wordLibrary: '/ai/article/word-library',
    recommendTheme: '/ai/article/recommend-theme',
    generate: '/ai/article/generate',
    historyList: '/ai/article/history-list',
    historyDetail: '/ai/article/history-detail',
    exportPdf: '/ai/article/export-pdf',
    deleteHistory: '/ai/article/delete-history',
    clearHistory: '/ai/article/clear-history'
  },
  
  // 公共文件
  publicFiles: {
    list: '/public-files',
    download: (filename) => `/public-files/download/${encodeURIComponent(filename)}`,
    upload: '/public-files/upload',
    delete: (filename) => `/public-files/${encodeURIComponent(filename)}`
  },
  
  // 单词游戏
  wordGame: {
    courses: '/word-game/courses',
    coursesList: '/word-game/courses/list',
    startGame: '/word-game/game/start',
    submitAnswer: '/word-game/game/submit',
    getQuestion: '/word-game/game/question',
    completeGame: '/word-game/game/complete',
    stats: '/word-game/stats'
  }
}
