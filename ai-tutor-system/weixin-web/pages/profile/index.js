// pages/profile/index.js
Page({
  data: {
    userInfo: null
  },

  onShow() {
    this.checkAuth();
  },

  checkAuth() {
    const userInfo = wx.getStorageSync('user');
    this.setData({ userInfo });
  },

  handleLogout() {
    wx.showModal({
      title: '提示',
      content: '确定要退出登录吗？',
      success: (res) => {
        if (res.confirm) {
          wx.removeStorageSync('token');
          wx.removeStorageSync('user');
          this.setData({ userInfo: null });
          wx.showToast({ title: '已退出', icon: 'success' });
        }
      }
    });
  },

  goToLogin() {
    wx.navigateTo({ url: '/pages/auth/login' });
  }
});
