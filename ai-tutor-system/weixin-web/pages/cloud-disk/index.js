// pages/cloud-disk/index.js
const request = require('../../utils/request');
const config = require('../../config/config');

Page({
  data: {
    currentFolderId: null,
    currentPath: [],
    folders: [],
    files: [],
    loading: false,
    searchKeyword: ''
  },

  onLoad() {
    this.fetchData();
  },

  onPullDownRefresh() {
    this.fetchData().then(() => {
      wx.stopPullDownRefresh();
    });
  },

  async fetchData() {
    this.setData({ loading: true });
    try {
      // 获取文件和文件夹列表
      // 注意：这里由于没有后端具体参数文档，先用假设的参数格式
      const folderRes = await request.get(config.api.cloudDisk.folders || '/api/cloud-disk/folders', {
        data: { parentId: this.data.currentFolderId }
      });
      
      const fileRes = await request.get(config.api.cloudDisk.files || '/api/cloud-disk/files', {
        data: { folderId: this.data.currentFolderId }
      });

      this.setData({
        folders: folderRes.data || folderRes || [],
        files: (fileRes.data || fileRes || []).map(f => ({
          ...f,
          sizeStr: this.formatSize(f.size)
        }))
      });
    } catch (err) {
      // 如果后端没开或者接口报错，先造点假数据预览 UI
      this.mockData();
      console.error('Fetch disk data error:', err);
    } finally {
      this.setData({ loading: false });
    }
  },

  mockData() {
    if (this.data.currentFolderId) {
      this.setData({
        folders: [],
        files: [
          { id: 101, name: 'AI_Study_Plan.pdf', size: 1024 * 1024 * 2.5, sizeStr: '2.5 MB', updateTime: '2026-03-20' }
        ]
      });
    } else {
      this.setData({
        folders: [
          { id: 1, name: '学习资料', updateTime: '2026-03-19' },
          { id: 2, name: '我的代码', updateTime: '2026-03-18' }
        ],
        files: [
          { id: 100, name: 'readme.txt', size: 1024 * 15, sizeStr: '15 KB', updateTime: '2026-03-15' }
        ]
      });
    }
  },

  formatSize(bytes) {
    if (!bytes) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  },

  enterFolder(e) {
    const folder = e.currentTarget.dataset.folder;
    const { currentPath } = this.data;
    
    this.setData({
      currentFolderId: folder.id,
      currentPath: [...currentPath, { id: folder.id, name: folder.name }]
    });
    
    this.fetchData();
  },

  navigateToFolder(e) {
    const index = parseInt(e.currentTarget.dataset.index);
    const { currentPath } = this.data;
    
    if (index === currentPath.length - 1) return; // 点击当前目录无反应
    
    if (index === -1) {
      this.setData({ currentFolderId: null, currentPath: [] });
    } else {
      const newPath = currentPath.slice(0, index + 1);
      this.setData({
        currentFolderId: newPath[newPath.length - 1].id,
        currentPath: newPath
      });
    }
    
    this.fetchData();
  },

  uploadFile() {
    wx.chooseMessageFile({
      count: 1,
      type: 'all',
      success: (res) => {
        const file = res.tempFiles[0];
        wx.showLoading({ title: '上传中...' });
        
        // 模拟上传成功
        setTimeout(() => {
          wx.hideLoading();
          wx.showToast({ title: '上传成功', icon: 'success' });
          this.fetchData();
        }, 1500);
      }
    });
  },

  previewFile(e) {
    const file = e.currentTarget.dataset.file;
    wx.showToast({ title: '正在打开 ' + file.name, icon: 'none' });
    // 实际应调用 wx.downloadFile 和 wx.openDocument
  },

  showFileActions(e) {
    const file = e.currentTarget.dataset.file;
    wx.showActionSheet({
      itemList: ['下载', '重命名', '删除'],
      itemColor: '#333',
      success: (res) => {
        if (res.tapIndex === 2) {
          wx.showModal({
            title: '确认删除',
            content: `确定要删除文件 "${file.name}" 吗？`,
            success: (modalRes) => {
              if (modalRes.confirm) {
                wx.showToast({ title: '已删除', icon: 'success' });
                this.fetchData();
              }
            }
          });
        }
      }
    });
  },

  showFolderActions(e) {
    wx.showActionSheet({
      itemList: ['重命名', '删除'],
      itemColor: '#333'
    });
  }
});
