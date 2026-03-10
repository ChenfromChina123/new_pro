import request from '@/utils/request'

/**
 * SFTP API 服务
 * 提供文件操作的 HTTP 请求封装
 */
const API_BASE = '/api/sftp'

export default {
  /**
   * 获取文件列表
   * @param {number} serverId - 服务器 ID
   * @param {string} path - 目录路径
   * @returns {Promise} 文件列表响应
   */
  async listFiles(serverId, path = '/') {
    return request.get(`${API_BASE}/${serverId}/files`, {
      params: { path }
    })
  },

  /**
   * 上传文件
   * @param {number} serverId - 服务器 ID
   * @param {string} path - 目标目录路径
   * @param {File} file - 文件对象
   * @param {function} onProgress - 进度回调
   * @returns {Promise} 上传响应
   */
  async uploadFile(serverId, path, file, onProgress) {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('path', path)

    return request.post(`${API_BASE}/${serverId}/upload`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      },
      onUploadProgress: (progressEvent) => {
        if (onProgress && progressEvent.total) {
          const progress = Math.round((progressEvent.loaded * 100) / progressEvent.total)
          onProgress(progress)
        }
      }
    })
  },

  /**
   * 下载文件
   * @param {number} serverId - 服务器 ID
   * @param {string} path - 文件路径
   * @returns {string} 下载 URL
   */
  getDownloadUrl(serverId, path) {
    const token = localStorage.getItem('token')
    return `${API_BASE}/${serverId}/download?path=${encodeURIComponent(path)}&token=${token}`
  },

  /**
   * 下载文件（Blob）
   * @param {number} serverId - 服务器 ID
   * @param {string} path - 文件路径
   * @param {function} onProgress - 进度回调
   * @returns {Promise<Blob>} 文件 Blob
   */
  async downloadFileBlob(serverId, path, onProgress) {
    const response = await request.get(`${API_BASE}/${serverId}/download`, {
      params: { path },
      responseType: 'blob',
      onDownloadProgress: (progressEvent) => {
        if (onProgress && progressEvent.total) {
          const progress = Math.round((progressEvent.loaded * 100) / progressEvent.total)
          onProgress(progress)
        }
      }
    })
    return response
  },

  /**
   * 删除文件或目录
   * @param {number} serverId - 服务器 ID
   * @param {string} path - 文件/目录路径
   * @param {boolean} isDirectory - 是否为目录
   * @returns {Promise} 删除响应
   */
  async deleteItem(serverId, path, isDirectory = false) {
    return request.delete(`${API_BASE}/${serverId}/file`, {
      params: { path, isDirectory }
    })
  },

  /**
   * 重命名文件或目录
   * @param {number} serverId - 服务器 ID
   * @param {string} oldPath - 原路径
   * @param {string} newPath - 新路径
   * @returns {Promise} 重命名响应
   */
  async rename(serverId, oldPath, newPath) {
    return request.put(`${API_BASE}/${serverId}/rename`, null, {
      params: { oldPath, newPath }
    })
  },

  /**
   * 创建目录
   * @param {number} serverId - 服务器 ID
   * @param {string} path - 目录路径
   * @returns {Promise} 创建响应
   */
  async mkdir(serverId, path) {
    return request.post(`${API_BASE}/${serverId}/mkdir`, null, {
      params: { path }
    })
  },

  /**
   * 检查路径是否存在
   * @param {number} serverId - 服务器 ID
   * @param {string} path - 路径
   * @returns {Promise} 是否存在
   */
  async exists(serverId, path) {
    return request.get(`${API_BASE}/${serverId}/exists`, {
      params: { path }
    })
  },

  /**
   * 获取文件信息
   * @param {number} serverId - 服务器 ID
   * @param {string} path - 文件路径
   * @returns {Promise} 文件信息
   */
  async getFileInfo(serverId, path) {
    return request.get(`${API_BASE}/${serverId}/info`, {
      params: { path }
    })
  }
}
