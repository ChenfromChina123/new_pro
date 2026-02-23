# API集成文档

本文档说明Vue 3前端如何与FastAPI后端进行集成。

## 📡 API配置

所有API配置集中在 `src/config/api.js`：

```javascript
export const API_CONFIG = {
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:5000',
  timeout: 30000
}

export const API_ENDPOINTS = {
  // 各模块的API端点
}
```

## 🔐 认证流程

### 1. 用户注册

```javascript
// 发送验证码
await authStore.sendVerificationCode('user@example.com')

// 注册
await authStore.register('user@example.com', 'password123', '123456')
```

**对应API端点:**

- `POST /api/register/email` - 发送验证码
- `POST /api/register` - 注册用户

### 2. 用户登录

```javascript
const result = await authStore.login('user@example.com', 'password123')
if (result.success) {
  // Token自动保存到localStorage
  router.push('/chat')
}
```

**对应API端点:**

- `POST /api/login`

**请求格式:**
```javascript
FormData {
  username: 'user@example.com',
  password: 'password123'
}
```

**响应格式:**
```json
{
  "access_token": "eyJ0eXAiOiJKV1QiLCJhbGc...",
  "token_type": "bearer",
  "user_id": 1,
  "username": "user",
  "is_admin": false
}
```

### 3. Token管理

Token自动添加到所有请求的Authorization header:

```javascript
// src/utils/request.js
request.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})
```

## 💬 AI问答API

### 1. 流式问答

```javascript
await chatStore.sendMessage('你好', (chunk) => {
  // 处理每个chunk
  console.log(chunk)
})
```

**对应API端点:**

- `POST /api/ask-stream`

**请求格式:**
```json
{
  "prompt": "你好",
  "session_id": 123,
  "model": "deepseek"
}
```

**响应格式 (Server-Sent Events):**
```
data: {"content": "你"}
data: {"content": "好"}
data: {"content": "！"}
data: [DONE]
```

### 2. 会话管理

```javascript
// 创建新会话
await chatStore.createSession('新对话')

// 获取会话列表
await chatStore.fetchSessions()

// 获取会话消息
await chatStore.fetchSessionMessages(sessionId)

// 删除会话
await chatStore.deleteSession(sessionId)
```

**对应API端点:**

- `POST /api/chat-records/new-session`
- `GET /api/chat-records/sessions`
- `GET /api/chat-records/session/{session_id}`
- `DELETE /api/chat-records/session/{session_id}`

## ☁️ 云盘API

### 1. 文件上传

```javascript
await cloudDiskStore.uploadFile(file, folderPath, (progress) => {
  console.log(`上传进度: ${progress}%`)
})
```

**对应API端点:**

- `POST /api/cloud_disk/upload`

**请求格式 (multipart/form-data):**
```
file: [File]
folder: "path/to/folder"
```

### 2. 文件列表

```javascript
await cloudDiskStore.fetchFiles(folderPath)
```

**对应API端点:**

- `GET /api/cloud_disk/files?folder=path/to/folder`

**响应格式:**
```json
{
  "files": [
    {
      "id": 1,
      "filename": "document.pdf",
      "file_size": 1024000,
      "upload_time": "2024-01-01T12:00:00",
      "folder_path": "documents"
    }
  ]
}
```

### 3. 文件夹管理

```javascript
// 获取文件夹树
await cloudDiskStore.fetchFolders()

// 创建文件夹
await cloudDiskStore.createFolder('新文件夹', 'parent/path')

// 删除文件夹
await cloudDiskStore.deleteFolder(folderId)
```

**对应API端点:**

- `GET /api/cloud_disk/folders`
- `POST /api/cloud_disk/create-folder`
- `DELETE /api/cloud_disk/delete-folder/{folder_id}`

### 4. 文件操作

```javascript
// 下载文件
const url = cloudDiskStore.getDownloadUrl(fileId)
window.open(url)

// 预览文件
const previewUrl = cloudDiskStore.getPreviewUrl(fileId)

// 删除文件
await cloudDiskStore.deleteFile(fileId)

// 批量删除
await cloudDiskStore.deleteFiles([fileId1, fileId2])
```

**对应API端点:**

- `GET /api/cloud_disk/download/{file_id}`
- `GET /api/cloud_disk/preview/{file_id}`
- `DELETE /api/cloud_disk/delete/{file_id}`

## 📚 语言学习API

### 1. 单词表管理

```javascript
// 获取单词表列表
const response = await request.get(API_ENDPOINTS.language.vocabularyLists)

// 创建单词表
await request.post(API_ENDPOINTS.language.createList, {
  name: '四级词汇'
})
```

**对应API端点:**

- `GET /api/language/vocabulary-lists`
- `POST /api/language/vocabulary-lists`

### 2. 单词管理

```javascript
// 获取单词列表
const response = await request.get(
  API_ENDPOINTS.language.getWords(listId)
)

// 添加单词
await request.post(API_ENDPOINTS.language.addWord, {
  list_id: listId,
  word: 'hello',
  phonetic: '/həˈləʊ/',
  translation: '你好',
  example: 'Hello, world!'
})
```

**对应API端点:**

- `GET /api/language/vocabulary-lists/{list_id}/words`
- `POST /api/language/words`

### 3. AI生成文章

```javascript
const response = await request.post(
  API_ENDPOINTS.language.generateArticle,
  { list_id: listId }
)
```

**对应API端点:**

- `POST /api/language/generate-article`

## 👨‍💼 管理后台API

### 1. 统计数据

```javascript
const response = await request.get(API_ENDPOINTS.admin.statistics)
```

**对应API端点:**

- `GET /api/admin/statistics`

**响应格式:**
```json
{
  "total_users": 100,
  "total_chats": 1000,
  "total_files": 500,
  "total_storage": 10737418240
}
```

### 2. 用户管理

```javascript
const response = await request.get(API_ENDPOINTS.admin.users)
```

**对应API端点:**

- `GET /api/admin/users`

## 🔧 HTTP拦截器

### 请求拦截器

自动添加认证Token:

```javascript
request.interceptors.request.use(
  (config) => {
    const authStore = useAuthStore()
    if (authStore.token) {
      config.headers.Authorization = `Bearer ${authStore.token}`
    }
    return config
  }
)
```

### 响应拦截器

统一错误处理:

```javascript
request.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (error.response?.status === 401) {
      // 未授权，跳转登录
      authStore.logout()
      router.push('/login')
    }
    return Promise.reject(error)
  }
)
```

## 🚨 错误处理

### 后端错误格式

```json
{
  "detail": "错误信息"
}
```

### 前端处理

```javascript
try {
  await request.post('/api/endpoint', data)
} catch (error) {
  const message = error.response?.data?.detail || '操作失败'
  alert(message)
}
```

## 📝 API调用示例

### 完整的文件上传示例

```javascript
async function uploadFileWithProgress(file) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('folder', 'documents')
  
  try {
    const response = await request.post(
      API_ENDPOINTS.cloudDisk.upload,
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data'
        },
        onUploadProgress: (progressEvent) => {
          const percentCompleted = Math.round(
            (progressEvent.loaded * 100) / progressEvent.total
          )
          console.log(`上传进度: ${percentCompleted}%`)
        }
      }
    )
    
    console.log('上传成功:', response)
    return response
  } catch (error) {
    console.error('上传失败:', error)
    throw error
  }
}
```

### 完整的流式问答示例

```javascript
async function streamChat(message) {
  const response = await fetch(
    `${API_CONFIG.baseURL}/api/ask-stream`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      },
      body: JSON.stringify({
        prompt: message,
        session_id: currentSessionId,
        model: 'deepseek'
      })
    }
  )
  
  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  
  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    
    const chunk = decoder.decode(value)
    const lines = chunk.split('\n')
    
    for (const line of lines) {
      if (line.startsWith('data: ')) {
        const data = line.slice(6)
        if (data === '[DONE]') continue
        
        try {
          const parsed = JSON.parse(data)
          console.log('收到内容:', parsed.content)
          // 更新UI
        } catch (e) {
          console.error('解析错误:', e)
        }
      }
    }
  }
}
```

## 🔗 跨域配置

### 开发环境

使用Vite代理 (vite.config.js):

```javascript
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:5000',
      changeOrigin: true
    }
  }
}
```

### 生产环境

后端配置CORS:

```python
from fastapi.middleware.cors import CORSMiddleware

app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:3000", "https://your-domain.com"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)
```

## 📊 API性能优化

### 1. 请求缓存

```javascript
const cache = new Map()

async function fetchWithCache(url, ttl = 60000) {
  const cached = cache.get(url)
  if (cached && Date.now() - cached.time < ttl) {
    return cached.data
  }
  
  const data = await request.get(url)
  cache.set(url, { data, time: Date.now() })
  return data
}
```

### 2. 请求去重

```javascript
const pending = new Map()

async function fetchWithDedup(url) {
  if (pending.has(url)) {
    return pending.get(url)
  }
  
  const promise = request.get(url).finally(() => {
    pending.delete(url)
  })
  
  pending.set(url, promise)
  return promise
}
```

### 3. 批量请求

```javascript
async function batchFetch(urls) {
  return Promise.all(urls.map(url => request.get(url)))
}
```

## 🐛 调试技巧

### 1. 启用请求日志

```javascript
request.interceptors.request.use(config => {
  console.log('[Request]', config.method.toUpperCase(), config.url, config.data)
  return config
})

request.interceptors.response.use(response => {
  console.log('[Response]', response.config.url, response.data)
  return response
})
```

### 2. Mock API

开发时可以使用Mock数据:

```javascript
if (import.meta.env.DEV && import.meta.env.VITE_USE_MOCK) {
  // 拦截请求，返回Mock数据
}
```

## 📚 相关资源

- [FastAPI文档](https://fastapi.tiangolo.com/)
- [Axios文档](https://axios-http.com/)
- [后端API文档](http://localhost:5000/docs)

---

**Happy Coding!** 🚀

