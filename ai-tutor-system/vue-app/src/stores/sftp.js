import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request from '@/utils/request'

/**
 * SFTP 文件管理状态存储
 */
export const useSFTPStore = defineStore('sftp', () => {
  // 当前选中的服务器 ID
  const currentServerId = ref(null)
  
  // 当前路径
  const currentPath = ref('/')
  
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

  /**
   * 设置当前服务器
   * @param {number} serverId - 服务器 ID
   */
  function setCurrentServer(serverId) {
    currentServerId.value = serverId
    currentPath.value = '/'
    files.value = []
    selectedFiles.value = []
    connected.value = false
  }

  /**
   * 获取文件列表
   * @param {string} path - 目录路径
   */
  async function fetchFiles(path = currentPath.value) {
    if (!currentServerId.value) return
    
    loading.value = true
    error.value = null
    
    try {
      const response = await request.get(`/api/sftp/${currentServerId.value}/files`, {
        params: { path }
      })
      
      if (response.code === 200) {
        files.value = response.data.files || []
        currentPath.value = response.data.path
        selectedFiles.value = []
      } else {
        error.value = response.message
      }
    } catch (e) {
      error.value = e.message || '获取文件列表失败'
    } finally {
      loading.value = false
    }
  }

  /**
   * 进入目录
   * @param {string} dirName - 目录名
   */
  async function enterDirectory(dirName) {
    const newPath = currentPath.value === '/' 
      ? `/${dirName}` 
      : `${currentPath.value}/${dirName}`
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
    addTransferTask,
    updateTransferProgress,
    removeTransferTask,
    clearCompletedTasks
  }
})
