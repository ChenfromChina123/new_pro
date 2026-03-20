// pages/ai-article/generate.js
const request = require('../../utils/request');

Page({
  data: {
    theme: '',
    wordListStr: '',
    
    languages: [
      { label: '英语', value: 'en' },
      { label: '日语', value: 'ja' },
      { label: '韩语', value: 'ko' }
    ],
    langIndex: 0,
    
    difficulties: ['A1 (初学)', 'A2 (基础)', 'B1 (进阶)', 'B2 (中高)', 'C1 (高级)', 'C2 (精通)'],
    diffIndex: 1,
    
    lengths: ['短篇 (约100词)', '中篇 (约250词)', '长篇 (约500词)'],
    lenIndex: 1,
    
    loading: false
  },

  onInput(e) {
    const field = e.currentTarget.dataset.field;
    this.setData({ [field]: e.detail.value });
  },

  onPickerChange(e) {
    const field = e.currentTarget.dataset.field;
    const value = e.detail.value;
    
    if (field === 'targetLanguage') this.setData({ langIndex: value });
    if (field === 'difficulty') this.setData({ diffIndex: value });
    if (field === 'lengthType') this.setData({ lenIndex: value });
  },

  async getRecommendTheme() {
    wx.showLoading({ title: 'AI 推荐中...' });
    try {
      const words = this.data.wordListStr ? this.data.wordListStr.split(',').map(w => w.trim()).filter(w => w) : [];
      const res = await request.post('/api/ai/article/recommend-theme', { wordList: words.length ? words : ['daily'] });
      if (res && res.length > 0) {
        this.setData({ theme: res[0] });
        wx.showToast({ title: '已获取推荐', icon: 'success' });
      }
    } catch (err) {
      wx.showToast({ title: '推荐失败', icon: 'none' });
      // mock
      this.setData({ theme: 'My Daily Routine and Healthy Habits' });
    } finally {
      wx.hideLoading();
    }
  },

  async generateArticle() {
    const { theme, wordListStr, langIndex, languages, diffIndex, difficulties, lenIndex, lengths } = this.data;
    
    if (!theme.trim()) {
      return wx.showToast({ title: '请输入文章主题', icon: 'none' });
    }

    this.setData({ loading: true });
    
    const words = wordListStr ? wordListStr.split(',').map(w => w.trim()).filter(w => w) : [];
    
    const payload = {
      theme: theme,
      targetLanguage: languages[langIndex].value,
      difficulty: difficulties[diffIndex].split(' ')[0], // 提取如 'A2'
      lengthType: lenIndex === 0 ? 'short' : (lenIndex === 1 ? 'medium' : 'long'),
      wordList: words
    };

    try {
      await request.post('/api/ai/article/generate', payload);
      wx.showToast({ title: '生成成功', icon: 'success' });
      setTimeout(() => {
        wx.navigateBack();
      }, 1500);
    } catch (err) {
      console.error(err);
      wx.showToast({ title: err.message || '生成失败', icon: 'none' });
      // mock success for preview
      setTimeout(() => {
        wx.navigateBack();
      }, 1000);
    } finally {
      this.setData({ loading: false });
    }
  }
});
