import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '@/utils/request'
import { API_ENDPOINTS } from '@/config/api'

export const useCloudDiskStore = defineStore('cloudDisk', () => {
  // 状态
  const files = ref([])
  const folders = ref([])
  const currentFolder = ref('')
  const activeFolderPath = ref('')
  const selectedFiles = ref([])
  const isLoading = ref(false)
  let latestFetchToken = 0

  function normalizeFolderPath(folderPath) {
    return (folderPath || '').replace(/^\//, '').replace(/\/$/, '')
  }

  function applyActiveFlags(targetPath, targetId) {
    const walk = (node) => {
      const matches = targetId
        ? node?.id === targetId
        : (node?.folderPath || '') === (targetPath || '')

      node.isActive = Boolean(matches)

      const children = Array.isArray(node?.children) ? node.children : []
      for (const child of children) {
        walk(child)
      }
    }

    const roots = Array.isArray(folders.value) ? folders.value : []
    for (const r of roots) {
      walk(r)
    }
  }

  function setActiveFolder({ folderId = null, folderPath = '' } = {}) {
    const normalizedPath = normalizeFolderPath(folderPath)
    activeFolderPath.value = normalizedPath
    applyActiveFlags(normalizedPath, folderId)
  }
  
  // 初始化用户文件夹结构
  async function initFolderStructure() {
    try {
      const response = await request.post(API_ENDPOINTS.cloudDisk.initFolderStructure)
      await fetchFolders()
      return { success: true, message: response.message }
    } catch (error) {
      console.error('Init folder structure error:', error)
      return { 
        success: false, 
        message: error.response?.data?.message || '初始化文件夹结构失败' 
      }
    }
  }
  
  // 获取文件列表
  async function fetchFiles(folderPath = null) {
    const token = ++latestFetchToken
    isLoading.value = true
    try {
      // 标准化文件夹路径：
      // 1. 如果为null或undefined，使用空字符串
      // 2. 空字符串或"/"表示根目录，传递给后端时使用空字符串
      // 3. 其他情况确保以"/"开头，这是后端期望的格式
      let normalizedPath = folderPath || '';
      let pathToSend = '';
      
      if (normalizedPath === '/' || normalizedPath === '') {
        pathToSend = '';
      } else {
        // 确保路径以"/"开头，与后端存储格式一致
        pathToSend = normalizedPath.startsWith('/') ? normalizedPath : '/' + normalizedPath;
      }
      
      const response = await request.get(API_ENDPOINTS.cloudDisk.files, {
        params: { folderPath: pathToSend }
      })
      const list = (response && (response.data || response.files)) || (Array.isArray(response) ? response : [])
      if (token === latestFetchToken) {
        files.value = list || []
      }
      
      // 前端存储时使用不带"/"的格式，便于处理
      let folderForStore = normalizedPath.replace(/^\//, '').replace(/\/$/, '');
      if (token === latestFetchToken) {
        currentFolder.value = folderForStore;
        setActiveFolder({ folderPath: folderForStore })
      }
      return { success: true }
    } catch (error) {
      console.error('Fetch files error:', error)
      return { success: false, message: error.response?.data?.message || '获取文件列表失败' }
    } finally {
      isLoading.value = false
    }
  }
  
  async function startRenameFile(fileId, newName) {
    try {
      const response = await request.put(API_ENDPOINTS.cloudDisk.renameFile(fileId), { newName })
      return { success: true, data: response?.data || response }
    } catch (error) {
      return { success: false, message: error.response?.data?.message || '重命名失败', data: error.response?.data }
    }
  }

  async function resolveRenameFile(fileId, action, finalName) {
    try {
      const response = await request.post(API_ENDPOINTS.cloudDisk.resolveRenameFile(fileId), { action, finalName })
      await fetchFiles(currentFolder.value || null)
      return { success: true, file: response?.data || response }
    } catch (error) {
      return { success: false, message: error.response?.data?.message || '重命名处理失败' }
    }
  }
  
  // 构建树形结构
  function buildFolderTree(foldersList) {
    const folderMap = {};
    const pathMap = {};
    let rootFolderNode = null;
    
    // 第一步：创建所有文件夹节点，并找出根目录
    foldersList.forEach(folder => {
      let rawPath = folder.folderPath ?? folder.folder_path ?? '';
      let normalizedPath;
      if (rawPath === '/' || rawPath === '') {
        normalizedPath = '';
      } else {
        normalizedPath = rawPath.replace(/^\//, '').replace(/\/$/, '');
      }

      let rawParent = folder.parentPath ?? folder.parent_path ?? null;
      let normalizedParentPath;
      if (rawParent === '/' || rawParent === '' || rawParent === null) {
        normalizedParentPath = '';
      } else {
        normalizedParentPath = rawParent.replace(/^\//, '').replace(/\/$/, '');
      }
      
      const node = {
        id: folder.id,
        folderName: folder.folderName ?? folder.folder_name ?? '',
        folderPath: normalizedPath,
        parentPath: normalizedParentPath,
        isActive: false,
        originalFolderPath: folder.folderPath ?? folder.folder_path ?? '',
        originalParentPath: folder.parentPath ?? folder.parent_path ?? null,
        createdAt: folder.createdAt ?? folder.created_at ?? null,
        children: []
      };
      folderMap[folder.id] = node;
      pathMap[normalizedPath] = node;
      
      // 找出根目录（folderPath为""的文件夹）
      if (normalizedPath === '') {
        rootFolderNode = folderMap[folder.id];
      }
    });
    
    // 如果没有找到根目录，创建一个虚拟根目录
    if (!rootFolderNode) {
      rootFolderNode = {
        id: 'virtual-root',
        folderName: '根目录',
        folderPath: '', // 使用空字符串作为根目录路径，与currentFolder保持一致
        parentPath: null,
        isActive: false,
        createdAt: new Date().toISOString(),
        children: []
      };
      folderMap['virtual-root'] = rootFolderNode;
    }
    
    // 第二步：构建层级关系
    foldersList.forEach(folder => {
      const currentFolder = folderMap[folder.id];
      
      // 查找父文件夹
      let parentFound = false;
      
      // 获取当前文件夹标准化后的父路径
      let normalizedParentPath = currentFolder.parentPath;
      
      // 先尝试根据标准化后的parentPath查找父文件夹
      if (normalizedParentPath !== null) {
        // 查找父文件夹
        let parentNode = pathMap[normalizedParentPath];
        let parentId = parentNode ? parentNode.id : undefined;
        
        if (parentId) {
          // 检查循环引用
          let isCycle = false;
          let ancestor = folderMap[parentId];
          const visited = new Set();
          while (ancestor) {
            if (visited.has(ancestor.id)) break; // 防止死循环（如果已有循环）
            visited.add(ancestor.id);
            if (ancestor.id === currentFolder.id) {
              isCycle = true;
              break;
            }
            ancestor = ancestor._tempParent;
          }

          if (!isCycle) {
            if (parentId !== currentFolder.id) {
              folderMap[parentId].children.push(currentFolder);
              currentFolder._tempParent = folderMap[parentId];
            }
            parentFound = true;
          } else {
            console.warn(`Cycle detected for folder ${currentFolder.folderName} (ID: ${currentFolder.id}), adding to root`);
            if (rootFolderNode.id !== currentFolder.id) {
               rootFolderNode.children.push(currentFolder);
            }
            parentFound = true; // Treated as found (handled)
          }
        } else if (normalizedParentPath === '') {
          // 如果父路径是空字符串（根目录），添加到虚拟根目录
          if (rootFolderNode.id !== currentFolder.id) {
             rootFolderNode.children.push(currentFolder);
             currentFolder._tempParent = rootFolderNode;
          }
          parentFound = true;
        } else if (currentFolder.originalParentPath) {
          const op = currentFolder.originalParentPath;
          const opNorm = op === '/' || op === '' ? '' : op.replace(/^\//, '').replace(/\/$/, '');
          parentNode = pathMap[opNorm];
          parentId = parentNode ? parentNode.id : undefined;
          if (parentId) {
             // Check cycle for fallback parent
            let isCycle = false;
            let ancestor = folderMap[parentId];
            const visited = new Set();
            while (ancestor) {
              if (visited.has(ancestor.id)) break;
              visited.add(ancestor.id);
              if (ancestor.id === currentFolder.id) {
                isCycle = true;
                break;
              }
              ancestor = ancestor._tempParent;
            }

            if (!isCycle) {
              if (parentId !== currentFolder.id) {
                 folderMap[parentId].children.push(currentFolder);
                 currentFolder._tempParent = folderMap[parentId];
              }
              parentFound = true;
            } else {
               if (rootFolderNode.id !== currentFolder.id) {
                  rootFolderNode.children.push(currentFolder);
               }
               parentFound = true;
            }
          }
        }
      }
      
      // 如果根据parentPath找不到父文件夹，尝试根据folderPath推断
      if (!parentFound) {
        const currentNormalizedPath = currentFolder.folderPath || '';
        
        // 计算父文件夹路径
        const pathParts = currentNormalizedPath.split('/').filter(part => part.length > 0);
        
        // 构建父文件夹路径
        const parentPathParts = pathParts.slice(0, -1);
        // 如果父路径为空数组，说明是根目录，使用空字符串
        const parentPath = parentPathParts.length > 0 ? parentPathParts.join('/') : '';
        
        // 查找父文件夹
        const parentNode = pathMap[parentPath];
        const parentId = parentNode ? parentNode.id : undefined;
        
        if (parentId && parentId !== currentFolder.id) {
          folderMap[parentId].children.push(currentFolder);
          parentFound = true;
        } else {
          // 如果找不到父文件夹，直接添加到根目录
          if (rootFolderNode.id !== currentFolder.id) {
            rootFolderNode.children.push(currentFolder);
          }
        }
      }
    });
    
    // 清理临时属性
    Object.values(folderMap).forEach(node => {
      delete node._tempParent;
    });

    // 返回根目录，它包含了所有子文件夹
    return [rootFolderNode];
  }
  
  // 获取文件夹树
  async function fetchFolders() {
    try {
      const response = await request.get(API_ENDPOINTS.cloudDisk.folders)
      const foldersList = (response && (response.data || response.folders)) || (Array.isArray(response) ? response : [])
      // 构建树形结构
      folders.value = buildFolderTree(foldersList)
      setActiveFolder({ folderPath: activeFolderPath.value || currentFolder.value || '' })
      return { success: true }
    } catch (error) {
      console.error('Fetch folders error:', error)
      return { success: false, message: error.response?.data?.message || '获取文件夹列表失败' }
    }
  }
  
  // 上传文件
  async function uploadFile(file, folderPath = '', onProgress) {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('folderPath', folderPath)
    formData.append('folder', folderPath)
    
    try {
      const response = await request.post(API_ENDPOINTS.cloudDisk.upload, formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
        onUploadProgress: (progressEvent) => {
          if (onProgress && progressEvent.total) {
            const percentCompleted = Math.round((progressEvent.loaded * 100) / progressEvent.total)
            onProgress(percentCompleted)
          }
        }
      })
      
      // 刷新当前文件夹
      await fetchFiles(folderPath)
      
      // 手动检查并添加文件，防止列表未及时更新
      // 只有当上传的文件夹是当前显示的文件夹时才添加
      const normalizedUploadPath = (folderPath || '').replace(/^\//, '').replace(/\/$/, '')
      const normalizedCurrentPath = (currentFolder.value || '').replace(/^\//, '').replace(/\/$/, '')

      if (normalizedUploadPath === normalizedCurrentPath && response && response.code === 200 && response.data) {
        const uploadedFile = response.data
        const exists = files.value.some(f => f.id === uploadedFile.id)
        
        if (!exists) {
            // 确保字段格式正确 (优先使用后端返回的 snake_case，如果不存在则尝试 camelCase)
            files.value.push({
              ...uploadedFile,
              file_size: uploadedFile.file_size || uploadedFile.fileSize,
              upload_time: uploadedFile.upload_time || uploadedFile.uploadTime,
              file_type: uploadedFile.file_type || uploadedFile.fileType
            })
          }
      }
      
      return { success: true, file: response }
    } catch (error) {
      console.error('Upload file error:', error)
      return { 
        success: false, 
        message: error.response?.data?.message || '上传文件失败' 
      }
    }
  }

  async function uploadFolderZip(zipBlob, folderPath = '', onProgress) {
    const formData = new FormData()
    const zipFile = new File([zipBlob], 'folder.zip', { type: 'application/zip' })
    formData.append('file', zipFile)
    formData.append('folderPath', folderPath)
    try {
      const response = await request.post(API_ENDPOINTS.cloudDisk.uploadFolder, formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
        onUploadProgress: (progressEvent) => {
          if (onProgress && progressEvent.total) {
            const percentCompleted = Math.round((progressEvent.loaded * 100) / progressEvent.total)
            onProgress(percentCompleted)
          }
        }
      })
      await fetchFiles(folderPath)
      await fetchFolders()
      return { success: true, filesImported: response?.data || 0 }
    } catch (error) {
      return { success: false, message: error.response?.data?.message || '上传文件夹失败' }
    }
  }

  async function uploadFolderStream(filesList, folderPath = '', onProgress) {
    const formData = new FormData()
    for (const f of filesList) {
      formData.append('files', f)
      formData.append('paths', f.webkitRelativePath || f.name)
    }
    formData.append('folderPath', folderPath)
    try {
      const response = await request.post(API_ENDPOINTS.cloudDisk.uploadFolderStream, formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
        onUploadProgress: (progressEvent) => {
          if (onProgress && progressEvent.total) {
            const percentCompleted = Math.round((progressEvent.loaded * 100) / progressEvent.total)
            onProgress(percentCompleted)
          }
        }
      })
      await fetchFiles(folderPath)
      await fetchFolders()
      return { success: true, filesImported: response?.data || 0 }
    } catch (error) {
      return { success: false, message: error.response?.data?.message || '上传文件夹失败' }
    }
  }
  
  // 创建文件夹
  async function createFolder(folderName, folderPath = '', parentId = null) {
    try {
      const response = await request.post(API_ENDPOINTS.cloudDisk.createFolder, {
        folderName,
        folderPath,
        parentId
      })
      
      // 刷新文件夹树
      await fetchFolders()
      
      return { success: true }
    } catch (error) {
      console.error('Create folder error:', error)
      return { 
        success: false, 
        message: error.response?.data?.message || '创建文件夹失败' 
      }
    }
  }
  
  // 删除文件
  async function deleteFile(fileId) {
    try {
      await request.delete(API_ENDPOINTS.cloudDisk.delete(fileId))
      
      // 从列表中移除
      files.value = files.value.filter(f => f.id !== fileId)
      
      return { success: true }
    } catch (error) {
      console.error('Delete file error:', error)
      return { success: false, message: error.response?.data?.message || '删除文件失败' }
    }
  }
  
  // 批量删除文件
  async function deleteFiles(fileIds) {
    const results = await Promise.all(
      fileIds.map(id => deleteFile(id))
    )
    
    const successCount = results.filter(r => r.success).length
    return { 
      success: successCount === fileIds.length,
      successCount,
      totalCount: fileIds.length
    }
  }
  
  // 删除文件夹
  async function deleteFolder(folderId) {
    try {
      await request.post(API_ENDPOINTS.cloudDisk.deleteFolder, null, {
        params: { folderId }
      })
      
      // 刷新文件夹树
      await fetchFolders()
      
      return { success: true }
    } catch (error) {
      console.error('Delete folder error:', error)
      return { success: false, message: error.response?.data?.message || '删除文件夹失败' }
    }
  }
  
  // 移动文件
  async function moveFile(fileId, targetFolderId, targetPath) {
    try {
      const response = await request.put(API_ENDPOINTS.cloudDisk.moveFile, {
        targetFolderId,
        targetPath
      }, {
        params: { fileId }
      })
      
      // 刷新当前文件夹
      await fetchFiles(currentFolder.value || null)
      
      return { success: true, file: response }
    } catch (error) {
      console.error('Move file error:', error)
      return { success: false, message: error.response?.data?.message || '移动文件失败' }
    }
  }
  
  // 重命名文件夹
  async function renameFolder(folderId, newName) {
    try {
      const response = await request.put(API_ENDPOINTS.cloudDisk.renameFolder, {
        newName
      }, {
        params: { folderId }
      })
      
      // 刷新文件夹树
      await fetchFolders()
      
      return { success: true, folder: response }
    } catch (error) {
      console.error('Rename folder error:', error)
      return { success: false, message: error.response?.data?.message || '重命名文件夹失败' }
    }
  }
  
  // 下载文件 (Blob方式)
  async function downloadFileBlob(file) {
    try {
      const response = await request.get(API_ENDPOINTS.cloudDisk.download(file.id), {
        responseType: 'blob'
      })
      
      const url = window.URL.createObjectURL(new Blob([response]))
      const link = document.createElement('a')
      link.href = url
      link.setAttribute('download', file.filename)
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)
      return { success: true }
    } catch (error) {
      console.error('Download file error:', error)
      return { success: false, message: '下载文件失败' }
    }
  }

  // 下载文件夹 (Blob方式)
  async function downloadFolder(folderPath) {
    try {
      const response = await request.get(API_ENDPOINTS.cloudDisk.downloadFolder, {
        params: { folderPath },
        responseType: 'blob'
      })
      
      // 生成下载文件名，使用文件夹名或默认名
      const folderName = folderPath.split('/').pop() || 'download'
      const fileName = `${folderName}.zip`
      
      const url = window.URL.createObjectURL(new Blob([response]))
      const link = document.createElement('a')
      link.href = url
      link.setAttribute('download', fileName)
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)
      return { success: true }
    } catch (error) {
      console.error('Download folder error:', error)
      return { success: false, message: '下载文件夹失败' }
    }
  }

  // 获取文件预览URL (Blob方式)
  async function fetchPreviewUrl(fileId, mimeType) {
    try {
      const response = await request.get(API_ENDPOINTS.cloudDisk.download(fileId), {
        responseType: 'blob'
      })
      // 如果传入了mimeType，则强制设置Blob类型
      const blob = mimeType ? new Blob([response], { type: mimeType }) : response
      return window.URL.createObjectURL(blob)
    } catch (error) {
      console.error('Fetch preview url error:', error)
      return null
    }
  }

  // 下载文件
  function getDownloadUrl(fileId) {
    return `${request.defaults.baseURL}${API_ENDPOINTS.cloudDisk.download(fileId)}`
  }
  
  // 获取预览URL (兼容旧代码，但建议使用fetchPreviewUrl)
  function getPreviewUrl(fileId) {
    return getDownloadUrl(fileId)
  }
  
  // 切换文件选择
  function toggleFileSelection(fileId) {
    const index = selectedFiles.value.indexOf(fileId)
    if (index > -1) {
      selectedFiles.value.splice(index, 1)
    } else {
      selectedFiles.value.push(fileId)
    }
  }
  
  // 全选/取消全选
  function toggleSelectAll() {
    if (selectedFiles.value.length === files.value.length) {
      selectedFiles.value = []
    } else {
      selectedFiles.value = files.value.map(f => f.id)
    }
  }
  
  // 清空选择
  function clearSelection() {
    selectedFiles.value = []
  }
  
  return {
    files,
    folders,
    currentFolder,
    activeFolderPath,
    selectedFiles,
    isLoading,
    initFolderStructure,
    fetchFiles,
    fetchFolders,
    setActiveFolder,
    uploadFile,
    uploadFolderZip,
    uploadFolderStream,
    createFolder,
    deleteFile,
    deleteFiles,
    deleteFolder,
    moveFile,
    renameFolder,
    startRenameFile,
    resolveRenameFile,
    getDownloadUrl,
    downloadFileBlob,
    downloadFolder,
    fetchPreviewUrl,
    getPreviewUrl,
    toggleFileSelection,
    toggleSelectAll,
    clearSelection
  }
})

