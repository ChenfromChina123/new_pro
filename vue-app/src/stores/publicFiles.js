import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '@/utils/request'
import { API_ENDPOINTS } from '@/config/api'

export const usePublicFilesStore = defineStore('publicFiles', () => {
  const files = ref([])
  const isLoading = ref(false)

  // 获取文件列表
  async function fetchFiles() {
    isLoading.value = true
    try {
      const response = await request.get(API_ENDPOINTS.publicFiles.list)
      // 兼容直接返回数组或 ApiResponse 包装的情况
      files.value = response.data || response || []
      return { success: true }
    } catch (error) {
      console.error('Fetch public files error:', error)
      return { 
        success: false, 
        message: error.response?.data?.message || '获取文件列表失败' 
      }
    } finally {
      isLoading.value = false
    }
  }

  // 上传文件
  async function uploadFile(file) {
    const formData = new FormData()
    formData.append('file', file)
    
    try {
      const response = await request.post(API_ENDPOINTS.publicFiles.upload, formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      })
      await fetchFiles()
      return { success: true, message: '上传成功' }
    } catch (error) {
      console.error('Upload public file error:', error)
      return { 
        success: false, 
        message: error.response?.data?.message || '上传失败' 
      }
    }
  }

  // 下载文件 URL
  function getDownloadUrl(filename) {
    const baseURL = API_ENDPOINTS.publicFiles.download(filename)
    if (baseURL.startsWith('http')) return baseURL
    
    // 如果是相对路径，需要拼接 baseURL
    const apiBase = import.meta.env.VITE_API_BASE_URL || 'http://localhost:5000'
    return `${apiBase}${baseURL}`
  }
  
  // 下载文件
  async function downloadFile(filename) {
    try {
       // 触发浏览器下载
       const link = document.createElement('a')
       link.href = getDownloadUrl(filename)
       link.download = filename
       document.body.appendChild(link)
       link.click()
       document.body.removeChild(link)
       return { success: true }
    } catch (error) {
       return { success: false, message: '下载失败' }
    }
  }

  // 删除文件
  async function deleteFile(filename) {
    try {
      await request.delete(API_ENDPOINTS.publicFiles.delete(filename))
      await fetchFiles()
      return { success: true, message: '删除成功' }
    } catch (error) {
      console.error('Delete public file error:', error)
      return { 
        success: false, 
        message: error.response?.data?.message || '删除失败' 
      }
    }
  }

  return {
    files,
    isLoading,
    fetchFiles,
    uploadFile,
    downloadFile,
    deleteFile
  }
})
