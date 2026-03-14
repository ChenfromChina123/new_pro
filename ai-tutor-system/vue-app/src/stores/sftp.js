import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request from '@/utils/request'

/**
 * SFTP 文件管理状态存储
 */
export const useSFTPStore = defineStore('sftp', () => {
  const STORAGE_KEY = 'sftp_manager_state_v1'
  const loadPersistedState = () => {
    try {
      const raw = localStorage.getItem(STORAGE_KEY)
      if (!raw) {
        return {
          currentServerId: null,
          currentPath: '/',
          pathHistory: {}
        }
      }
      const parsed = JSON.parse(raw)
      return {
        currentServerId: parsed.currentServerId ?? null,
        currentPath: parsed.currentPath || '/',
        pathHistory: parsed.pathHistory || {}
      }
    } catch {
      return {
        currentServerId: null,
        currentPath: '/',
        pathHistory: {}
      }
    }
  }

  const persistedState = loadPersistedState()

  // 当前选中的服务器 ID
  const currentServerId = ref(persistedState.currentServerId)

  // 当前路径
  const currentPath = ref(persistedState.currentPath)

  const pathHistory = ref(persistedState.pathHistory)

  // 文件列表
  const files = ref([])

  // 选中的文件
  const selectedFiles = ref([])

  // 加载状态
  const loading = ref(false)

  // 错误信息
  const error = ref(null)

  // 传输任务列表
  const transferTasks = ref([])

  // 连接状态
  const connected = ref(false)

  // 剪切板
  const clipboard = ref({
    type: null, // 'cut' or 'copy'
    path: null,
    isDirectory: false
  })

  // 计算属性：选中的文件数量
  const selectedCount = computed(() => selectedFiles.value.length)

  // 计算属性：选中的文件总大小
  const selectedSize = computed(() => {
    return selectedFiles.value.reduce((total, file) => total + file.size, 0)
  })

  // 计算属性：传输中的任务数量
  const transferCount = computed(() => {
    return transferTasks.value.filter(t => t.status === 'transferring').length
  })

  function persistState() {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify({
        currentServerId: currentServerId.value,
        currentPath: currentPath.value,
        pathHistory: pathHistory.value
      }))
    } catch (e) {
      console.error('持久化 SFTP 状态失败:', e)
    }
  }

  /**
   * 设置当前服务器
   * @param {number} serverId - 服务器 ID
   */
  function setCurrentServer(serverId) {
    currentServerId.value = serverId
    if (serverId === null) {
      currentPath.value = '/'
      files.value = []
      selectedFiles.value = []
      connected.value = false
      persistState()
      return
    }
    const rememberedPath = pathHistory.value[String(serverId)]
    currentPath.value = rememberedPath || '/'
    files.value = []
    selectedFiles.value = []
    connected.value = false
    persistState()
  }

  /**
   * 获取文件列表
   * @param {string} path - 目录路径
   */
  async function fetchFiles(path = currentPath.value) {
    console.log('fetchFiles 被调用:', path)
    if (!currentServerId.value) {
      console.log('没有选中服务器')
      return
    }

    loading.value = true
    error.value = null

    try {
      console.log('发送请求获取文件列表:', path)
      const response = await request.get(`/api/sftp/${currentServerId.value}/files`, {
        params: { path }
      })

      console.log('请求响应:', response)
      if (response.code === 200) {
        console.log('获取文件列表成功:', response.data.files?.length || 0, '个文件')
        files.value = response.data.files || []
        currentPath.value = response.data.path
        if (currentServerId.value !== null) {
          pathHistory.value[String(currentServerId.value)] = currentPath.value
        }
        persistState()
        console.log('当前路径更新为:', currentPath.value)
        selectedFiles.value = []
      } else {
        console.log('获取文件列表失败:', response.message)
        error.value = response.message
      }
    } catch (e) {
      console.error('获取文件列表异常:', e)
      error.value = e.message || '获取文件列表失败'
    } finally {
      loading.value = false
      console.log('fetchFiles 完成')
    }
  }

  /**
   * 进入目录
   * @param {string} path - 目录路径（可以是完整路径或目录名）
   */
  async function enterDirectory(path) {
    console.log('enterDirectory 被调用:', path)
    console.log('当前路径:', currentPath.value)
    // 如果是完整路径，直接使用
    // 如果是目录名，拼接当前路径
    let newPath
    if (path.startsWith('/')) {
      newPath = path
      console.log('使用完整路径:', newPath)
    } else {
      newPath = currentPath.value === '/'
        ? `/${path}`
        : `${currentPath.value}/${path}`
      console.log('拼接路径:', newPath)
    }
    console.log('最终路径:', newPath)
    await fetchFiles(newPath)
  }

  /**
   * 返回上级目录
   */
  async function goUp() {
    if (currentPath.value === '/') return

    const parts = currentPath.value.split('/').filter(Boolean)
    parts.pop()
    const newPath = parts.length === 0 ? '/' : '/' + parts.join('/')
    await fetchFiles(newPath)
  }

  /**
   * 选择文件
   * @param {object} file - 文件对象
   * @param {boolean} multi - 是否多选
   */
  function selectFile(file, multi = false) {
    if (multi) {
      const index = selectedFiles.value.findIndex(f => f.path === file.path)
      if (index >= 0) {
        selectedFiles.value.splice(index, 1)
      } else {
        selectedFiles.value.push(file)
      }
    } else {
      selectedFiles.value = [file]
    }
  }

  /**
   * 全选/取消全选
   */
  function toggleSelectAll() {
    if (selectedFiles.value.length === files.value.length) {
      selectedFiles.value = []
    } else {
      selectedFiles.value = [...files.value]
    }
  }

  /**
   * 创建目录
   * @param {string} dirName - 目录名
   */
  async function createDirectory(dirName) {
    if (!currentServerId.value) return false

    const newPath = currentPath.value === '/'
      ? `/${dirName}`
      : `${currentPath.value}/${dirName}`

    try {
      const response = await request.post(`/api/sftp/${currentServerId.value}/mkdir`, null, {
        params: { path: newPath }
      })

      if (response.code === 200) {
        await fetchFiles()
        return true
      } else {
        error.value = response.message
        return false
      }
    } catch (e) {
      error.value = e.message || '创建目录失败'
      return false
    }
  }

  /**
   * 删除文件或目录
   * @param {string} path - 路径
   * @param {boolean} isDirectory - 是否为目录
   */
  async function deleteItem(path, isDirectory) {
    if (!currentServerId.value) return false

    try {
      const response = await request.delete(`/api/sftp/${currentServerId.value}/file`, {
        params: { path, isDirectory }
      })

      if (response.code === 200) {
        await fetchFiles()
        return true
      } else {
        error.value = response.message
        return false
      }
    } catch (e) {
      error.value = e.message || '删除失败'
      return false
    }
  }

  /**
   * 重命名文件或目录
   * @param {string} oldPath - 原路径
   * @param {string} newPath - 新路径
   */
  async function renameItem(oldPath, newPath) {
    if (!currentServerId.value) return false

    try {
      const response = await request.put(`/api/sftp/${currentServerId.value}/rename`, null, {
        params: { oldPath, newPath }
      })

      if (response.code === 200) {
        await fetchFiles()
        return true
      } else {
        error.value = response.message
        return false
      }
    } catch (e) {
      error.value = e.message || '重命名失败'
      return false
    }
  }

  /**
   * 复制文件或目录
   * @param {string} sourcePath - 源路径
   * @param {string} targetPath - 目标路径
   * @param {boolean} isDirectory - 是否为目录
   */
  async function copyItem(sourcePath, targetPath, isDirectory) {
    if (!currentServerId.value) return false

    try {
      const response = await request.post(`/api/sftp/${currentServerId.value}/copy`, null, {
        params: { sourcePath, targetPath, isDirectory }
      })

      if (response.code === 200) {
        await fetchFiles()
        return true
      } else {
        error.value = response.message
        return false
      }
    } catch (e) {
      error.value = e.message || '复制失败'
      return false
    }
  }

  /**
   * 剪切文件或目录
   * @param {string} path - 路径
   */
  function cutItem(path) {
    const file = files.value.find(f => f.path === path)
    if (file) {
      clipboard.value = {
        type: 'cut',
        path: path,
        isDirectory: file.isDirectory
      }
    }
  }

  /**
   * 复制到剪切板
   * @param {string} path - 路径
   */
  function copyToClipboard(path) {
    const file = files.value.find(f => f.path === path)
    if (file) {
      clipboard.value = {
        type: 'copy',
        path: path,
        isDirectory: file.isDirectory
      }
    }
  }

  /**
   * 添加传输任务
   * @param {object} task - 任务对象
   */
  function addTransferTask(task) {
    transferTasks.value.push({
      id: Date.now().toString(),
      fileName: task.fileName,
      filePath: task.filePath,
      totalSize: task.totalSize || 0,
      transferredSize: 0,
      progress: 0,
      speed: '0 B/s',
      status: 'pending',
      type: task.type,
      ...task
    })
  }

  /**
   * 更新传输任务进度
   * @param {object} progress - 进度信息
   */
  function updateTransferProgress(progress) {
    const task = transferTasks.value.find(t => t.taskId === progress.taskId)
    if (task) {
      task.transferredSize = progress.transferredSize
      task.progress = progress.progress
      task.speed = progress.speed
      task.status = progress.status.toLowerCase()

      if (progress.status === 'COMPLETED' || progress.status === 'FAILED' || progress.status === 'CANCELLED') {
        task.endTime = Date.now()
      }
    }
  }

  /**
   * 移除传输任务
   * @param {string} taskId - 任务 ID
   */
  function removeTransferTask(taskId) {
    const index = transferTasks.value.findIndex(t => t.taskId === taskId)
    if (index >= 0) {
      transferTasks.value.splice(index, 1)
    }
  }

  /**
   * 清除已完成的任务
   */
  function clearCompletedTasks() {
    transferTasks.value = transferTasks.value.filter(
      t => t.status !== 'completed' && t.status !== 'failed' && t.status !== 'cancelled'
    )
  }

  return {
    // 状态
    currentServerId,
    currentPath,
    files,
    selectedFiles,
    loading,
    error,
    transferTasks,
    connected,
    clipboard,

    // 计算属性
    selectedCount,
    selectedSize,
    transferCount,

    // 方法
    setCurrentServer,
    fetchFiles,
    enterDirectory,
    goUp,
    selectFile,
    toggleSelectAll,
    createDirectory,
    deleteItem,
    renameItem,
    copyItem,
    cutItem,
    copyToClipboard,
    addTransferTask,
    updateTransferProgress,
    removeTransferTask,
    clearCompletedTasks
  }
})
