const HttpClient = require('./httpclient');
const config = require('../config/config');

const http = new HttpClient({
  baseURL: config.baseURL,
  timeout: config.timeout
});

// 请求拦截器
http.interceptors.request.use((config) => {
  const token = wx.getStorageSync('token');
  if (token) {
    config.header['Authorization'] = `Bearer ${token}`;
  }
  return config;
});

// 响应拦截器
http.interceptors.response.use((response) => {
  const res = response.data;
  const statusCode = response.statusCode;

  if (statusCode === 200 || statusCode === 201) {
    return res;
  } else if (statusCode === 401) {
    // 暂时移除自动跳转登录页逻辑，仅抛出错误
    throw new Error('未授权 (401)');
  } else {
    throw new Error(res.message || res.detail || '请求失败');
  }
});

module.exports = http;
