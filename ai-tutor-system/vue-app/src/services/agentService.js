import request from '@/utils/request'

const BASE_URL = '/api/agent'

/**
 * Agent API 服务
 */

// ==================== 会话管理 ====================

/**
 * 获取会话列表
 */
export function getSessions() {
  return request.get(`${BASE_URL}/sessions`)
}

/**
 * 分页获取会话列表
 */
export function getSessionsPage(page = 0, size = 10) {
  return request.get(`${BASE_URL}/sessions/page`, { params: { page, size } })
}

/**
 * 获取活跃会话
 */
export function getActiveSessions() {
  return request.get(`${BASE_URL}/sessions/active`)
}

/**
 * 获取或创建当前活跃会话
 */
export function getOrCreateCurrentSession() {
  return request.get(`${BASE_URL}/sessions/current`)
}

/**
 * 获取会话详情
 */
export function getSession(sessionId) {
  return request.get(`${BASE_URL}/sessions/${sessionId}`)
}

/**
 * 创建会话
 */
export function createSession(data) {
  return request.post(`${BASE_URL}/sessions`, data)
}

/**
 * 更新会话
 */
export function updateSession(sessionId, data) {
  return request.put(`${BASE_URL}/sessions/${sessionId}`, data)
}

/**
 * 关闭会话
 */
export function closeSession(sessionId) {
  return request.post(`${BASE_URL}/sessions/${sessionId}/close`)
}

/**
 * 删除会话
 */
export function deleteSession(sessionId) {
  return request.delete(`${BASE_URL}/sessions/${sessionId}`)
}

// ==================== 任务管理 ====================

/**
 * 创建任务
 */
export function createTask(data) {
  return request.post(`${BASE_URL}/tasks`, data)
}

/**
 * 获取任务详情
 */
export function getTask(taskId) {
  return request.get(`${BASE_URL}/tasks/${taskId}`)
}

/**
 * 获取会话的任务列表
 */
export function getTasksBySession(sessionId) {
  return request.get(`${BASE_URL}/tasks/session/${sessionId}`)
}

/**
 * 分页获取会话的任务列表
 */
export function getTasksBySessionPage(sessionId, page = 0, size = 10) {
  return request.get(`${BASE_URL}/tasks/session/${sessionId}/page`, { params: { page, size } })
}

/**
 * 分页获取用户的任务列表
 */
export function getTasksByUserPage(page = 0, size = 10) {
  return request.get(`${BASE_URL}/tasks/user/page`, { params: { page, size } })
}

/**
 * 启动任务（SSE 流式响应）
 */
export function streamTask(taskId, callbacks) {
  const eventSource = new EventSource(`${BASE_URL}/tasks/${taskId}/stream`, {
    withCredentials: true
  })

  eventSource.addEventListener('start', (event) => {
    const data = JSON.parse(event.data)
    callbacks.onStart?.(data)
  })

  eventSource.addEventListener('progress', (event) => {
    const data = JSON.parse(event.data)
    callbacks.onProgress?.(data)
  })

  eventSource.addEventListener('tool_call', (event) => {
    const data = JSON.parse(event.data)
    callbacks.onToolCall?.(data)
  })

  eventSource.addEventListener('output', (event) => {
    callbacks.onOutput?.(event.data)
  })

  eventSource.addEventListener('complete', (event) => {
    callbacks.onComplete?.(event.data)
    eventSource.close()
  })

  eventSource.addEventListener('error', (event) => {
    const data = JSON.parse(event.data)
    callbacks.onError?.(data)
    eventSource.close()
  })

  eventSource.onerror = (error) => {
    callbacks.onError?.({ error: '连接错误' })
    eventSource.close()
  }

  return eventSource
}

/**
 * 取消任务
 */
export function cancelTask(taskId) {
  return request.post(`${BASE_URL}/tasks/${taskId}/cancel`)
}

/**
 * 获取正在运行的任务
 */
export function getRunningTasks(sessionId) {
  return request.get(`${BASE_URL}/tasks/running/session/${sessionId}`)
}

// ==================== 沙箱工具代理 ====================

/**
 * 获取系统隔离能力
 */
export function getCapabilities() {
  return request.get(`${BASE_URL}/sandbox/capabilities`)
}

/**
 * 列出沙箱文件
 */
export function listSandboxFiles(sessionId, path = '.') {
  return request.get(`${BASE_URL}/sandbox/${sessionId}/files`, { params: { path } })
}

/**
 * 读取沙箱文件内容
 */
export function readSandboxFile(sessionId, path) {
  return request.get(`${BASE_URL}/sandbox/${sessionId}/file`, { params: { path } })
}

/**
 * 写入沙箱文件内容
 */
export function writeSandboxFile(sessionId, path, content) {
  return request.post(`${BASE_URL}/sandbox/${sessionId}/file`, { path, content })
}

/**
 * 在沙箱中执行终端命令
 */
export function executeSandboxTerminal(sessionId, command, timeoutMs) {
  return request.post(`${BASE_URL}/sandbox/${sessionId}/terminal`, { command, timeoutMs })
}

/**
 * 在沙箱中执行 Git 命令
 */
export function executeSandboxGit(sessionId, command, message) {
  return request.post(`${BASE_URL}/sandbox/${sessionId}/git`, { command, message })
}

/**
 * 获取沙箱资源使用情况
 */
export function getSandboxUsage(sessionId) {
  return request.get(`${BASE_URL}/sandbox/${sessionId}/usage`)
}

/**
 * 删除沙箱
 */
export function deleteSandbox(sessionId) {
  return request.delete(`${BASE_URL}/sandbox/${sessionId}`)
}

/**
 * 通用工具执行接口
 */
export function executeTool(request) {
  return request.post(`${BASE_URL}/sandbox/execute`, request)
}

// ==================== 文件操作 ====================

/**
 * 获取文件树
 */
export function getFileTree(sessionId) {
  // 优先使用新的沙箱 API
  return listSandboxFiles(sessionId, '.')
  // 备用旧 API
  // return request.get(`${BASE_URL}/files`, { params: { sessionId } })
}

/**
 * 读取文件内容
 */
export function readFile(sessionId, path, startLine = 1, endLine = 100) {
  return request.get(`${BASE_URL}/files/content`, {
    params: { sessionId, path, startLine, endLine }
  })
}

/**
 * 编辑文件
 */
export function editFile(sessionId, data) {
  return request.put(`${BASE_URL}/files/content`, { sessionId, ...data })
}

/**
 * 搜索文件
 */
export function searchFiles(sessionId, data) {
  return request.post(`${BASE_URL}/files/search`, { sessionId, ...data })
}

// ==================== 终端执行 ====================

/**
 * 执行命令
 */
export function executeCommand(sessionId, data) {
  return request.post(`${BASE_URL}/terminal/execute`, { sessionId, ...data })
}

/**
 * 获取任务列表
 */
export function getTerminalJobs(sessionId) {
  return request.get(`${BASE_URL}/terminal/jobs`, { params: { sessionId } })
}

/**
 * 终止任务
 */
export function terminateJob(jobId) {
  return request.delete(`${BASE_URL}/terminal/jobs/${jobId}`)
}

// ==================== Git 操作 ====================

/**
 * 获取快照列表
 */
export function getSnapshots(sessionId) {
  return request.get(`${BASE_URL}/git/snapshots`, { params: { sessionId } })
}

/**
 * 创建快照
 */
export function createSnapshot(sessionId, message) {
  return request.post(`${BASE_URL}/git/snapshots`, { sessionId, message })
}

/**
 * 回滚到上一个快照
 */
export function rollback(sessionId) {
  return request.post(`${BASE_URL}/git/rollback`, { sessionId })
}

/**
 * 获取差异
 */
export function getDiff(sessionId) {
  return request.get(`${BASE_URL}/git/diff`, { params: { sessionId } })
}
