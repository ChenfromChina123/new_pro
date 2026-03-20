// pages/auth/register.js
const request = require('../../utils/request');
const config = require('../../config/config');

Page({
  data: {
    username: '',
    password: '',
    confirmPassword: '',
    code: '',
    loading: false,
    codeLoading: false,
    countdown: 0
  },

  handleInput(e) {
    const { type } = e.currentTarget.dataset;
    this.setData({
      [type]: e.detail.value
    });
  },

  async sendCode() {
    const { username } = this.data;
    if (!username) {
      wx.showToast({
        title: '请输入手机号/邮箱',
        icon: 'none'
      });
      return;
    }

    this.setData({ codeLoading: true });
    
    try {
      await request.post(config.api.auth.sendCode, {
        username
      });
      
      wx.showToast({
        title: '验证码已发送',
        icon: 'success'
      });
      
      this.startCountdown();
    } catch (err) {
      wx.showToast({
        title: err.message || '发送失败',
        icon: 'none'
      });
    } finally {
      this.setData({ codeLoading: false });
    }
  },

  startCountdown() {
    this.setData({ countdown: 60 });
    const timer = setInterval(() => {
      const { countdown } = this.data;
      if (countdown <= 1) {
        clearInterval(timer);
        this.setData({ countdown: 0 });
      } else {
        this.setData({ countdown: countdown - 1 });
      }
    }, 1000);
  },

  async handleRegister() {
    const { username, password, confirmPassword, code } = this.data;
    
    if (!username || !password || !code) {
      wx.showToast({
        title: '请填写完整信息',
        icon: 'none'
      });
      return;
    }

    if (password !== confirmPassword) {
      wx.showToast({
        title: '两次密码输入不一致',
        icon: 'none'
      });
      return;
    }

    this.setData({ loading: true });
    
    try {
      const res = await request.post(config.api.auth.register, {
        username,
        password,
        code
      });
      
      wx.showToast({
        title: '注册成功',
        icon: 'success'
      });
      
      setTimeout(() => {
        wx.navigateTo({
          url: '/pages/auth/login'
        });
      }, 1500);
    } catch (err) {
      wx.showToast({
        title: err.message || '注册失败',
        icon: 'none'
      });
    } finally {
      this.setData({ loading: false });
    }
  },

  goToLogin() {
    wx.navigateBack();
  }
});
