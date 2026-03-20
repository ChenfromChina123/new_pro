/**
 * 封装类似 axios 的 HttpClient
 */
class HttpClient {
  constructor(config = {}) {
    this.defaults = {
      baseURL: '',
      timeout: 60000,
      header: {
        'Content-Type': 'application/json'
      },
      ...config
    };
    
    this.interceptors = {
      request: {
        handlers: [],
        use(fulfilled, rejected) {
          this.handlers.push({ fulfilled, rejected });
        }
      },
      response: {
        handlers: [],
        use(fulfilled, rejected) {
          this.handlers.push({ fulfilled, rejected });
        }
      }
    };
  }

  request(options) {
    let config = { ...this.defaults, ...options };
    config.header = { ...this.defaults.header, ...options.header };

    // 执行请求拦截器
    for (const interceptor of this.interceptors.request.handlers) {
      if (interceptor.fulfilled) {
        config = interceptor.fulfilled(config) || config;
      }
    }

    return new Promise((resolve, reject) => {
      wx.request({
        url: config.baseURL + config.url,
        method: config.method || 'GET',
        data: config.data,
        header: config.header,
        timeout: config.timeout,
        success: (res) => {
          // 执行响应拦截器
          let response = res;
          try {
            for (const interceptor of this.interceptors.response.handlers) {
              if (interceptor.fulfilled) {
                response = interceptor.fulfilled(response);
              }
            }
            resolve(response);
          } catch (err) {
            reject(err);
          }
        },
        fail: (err) => {
          let error = err;
          for (const interceptor of this.interceptors.response.handlers) {
            if (interceptor.rejected) {
              error = interceptor.rejected(error) || error;
            }
          }
          reject(error);
        }
      });
    });
  }

  get(url, config = {}) {
    return this.request({ ...config, url, method: 'GET' });
  }

  post(url, data, config = {}) {
    return this.request({ ...config, url, data, method: 'POST' });
  }

  put(url, data, config = {}) {
    return this.request({ ...config, url, data, method: 'PUT' });
  }

  delete(url, config = {}) {
    return this.request({ ...config, url, method: 'DELETE' });
  }
}

module.exports = HttpClient;
