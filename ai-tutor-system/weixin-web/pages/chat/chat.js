// pages/chat/chat.js
const request = require('../../utils/request');
const config = require('../../config/config');

Page({
  data: {
    messages: [],
    inputValue: '',
    loading: false,
    lastMessageId: '',
    sessionId: '',
    userInfo: null
  },

  onLoad(options) {
    const userInfo = wx.getStorageSync('user');
    this.setData({ userInfo });
    
    if (options.sessionId) {
      this.setData({ sessionId: options.sessionId });
      this.loadChatHistory(options.sessionId);
    } else {
      this.createNewSession();
    }
  },

  async createNewSession() {
    try {
      const res = await request.post(config.api.chat.newSession);
      if (res && res.id) {
        this.setData({ sessionId: res.id });
      }
    } catch (err) {
      console.error('Failed to create session:', err);
    }
  },

  async loadChatHistory(sessionId) {
    try {
      const res = await request.get(config.api.chat.history(sessionId));
      if (res && Array.isArray(res)) {
        this.setData({
          messages: res.map((msg, index) => ({
            id: index,
            role: msg.role,
            content: msg.content
          }))
        });
        this.scrollToBottom();
      }
    } catch (err) {
      wx.showToast({
        title: '加载历史失败',
        icon: 'none'
      });
    }
  },

  handleInput(e) {
    this.setData({
      inputValue: e.detail.value
    });
  },

  async handleSend() {
    const { inputValue, messages, sessionId, loading } = this.data;
    if (!inputValue || loading) return;

    const userMessage = {
      id: Date.now(),
      role: 'user',
      content: inputValue
    };

    this.setData({
      messages: [...messages, userMessage],
      inputValue: '',
      loading: true
    });
    this.scrollToBottom();

    try {
      // 微信小程序不支持真正的 Server-Sent Events (SSE) 流式传输
      // 但可以通过 request 的 enableChunked 属性实现，这里为了简单先用普通请求
      const res = await request.post(config.api.chat.ask, {
        prompt: inputValue,
        session_id: sessionId
      });

      const assistantMessage = {
        id: Date.now() + 1,
        role: 'assistant',
        content: res.content || res.answer || '抱歉，我现在无法回答。'
      };

      this.setData({
        messages: [...this.data.messages, assistantMessage],
        loading: false
      });
      this.scrollToBottom();
    } catch (err) {
      wx.showToast({
        title: err.message || '发送失败',
        icon: 'none'
      });
      this.setData({ loading: false });
    }
  },

  scrollToBottom() {
    const lastMsg = this.data.messages[this.data.messages.length - 1];
    if (lastMsg) {
      this.setData({
        lastMessageId: `msg-${lastMsg.id}`
      });
    }
  }
});
