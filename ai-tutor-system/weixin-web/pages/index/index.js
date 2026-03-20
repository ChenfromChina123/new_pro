// index.js
Page({
  data: {
    userInfo: null
  },

  onShow() {
    this.checkAuth();
  },

  checkAuth() {
    const userInfo = wx.getStorageSync('user');
    this.setData({ userInfo: userInfo || {} });
  },

  goToChat() {
    if (!this.checkLogin()) return;
    wx.navigateTo({ url: '/pages/chat/chat' });
  },

  goToWordGame() {
    if (!this.checkLogin()) return;
    wx.switchTab({ url: '/pages/word-game/index' });
  },

  goToCloudDisk() {
    wx.navigateTo({ url: '/pages/cloud-disk/index' });
  },

  goToAiArticle() {
    wx.navigateTo({ url: '/pages/ai-article/index' });
  },

  goToProfile() {
    wx.switchTab({ url: '/pages/profile/index' });
  },

  checkLogin() {
    // 暂时移除登录验证
    return true;
  }
});
