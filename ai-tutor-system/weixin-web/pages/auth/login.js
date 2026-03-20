// pages/auth/login.js
const request = require('../../utils/request');
const config = require('../../config/config');

Page({
  data: {
    username: '',
    password: '',
    loading: false
  },

  /**
   * 处理输入框输入
   */
  handleInput(e) {
    const { type } = e.currentTarget.dataset;
    this.setData({
      [type]: e.detail.value
    });
  },

  /**
   * 处理登录逻辑
   */
  async handleLogin() {
    const { username, password } = this.data;
    
    if (!username || !password) {
      wx.showToast({
        title: '请输入用户名和密码',
        icon: 'none'
      });
      return;
    }

    this.setData({ loading: true });
    
    try {
      const res = await request.post(config.api.auth.login, {
        username,
        password
      });
      
      if (res && res.token) {
        wx.setStorageSync('token', res.token);
        wx.setStorageSync('user', res.user);
        
        wx.showToast({
          title: '登录成功',
          icon: 'success'
        });
        
        // 跳转到首页
        setTimeout(() => {
          wx.switchTab({
            url: '/pages/index/index'
          });
        }, 1500);
      } else {
        wx.showToast({
          title: res.message || '登录失败',
          icon: 'none'
        });
      }
    } catch (err) {
      console.error('Login error:', err);
      wx.showToast({
        title: err.message || '网络请求失败',
        icon: 'none'
      });
    } finally {
      this.setData({ loading: false });
    }
  },

  /**
   * 跳转到注册页
   */
  goToRegister() {
    wx.navigateTo({
      url: '/pages/auth/register'
    });
  }
});
