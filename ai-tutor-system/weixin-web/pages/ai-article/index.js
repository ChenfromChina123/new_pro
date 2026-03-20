// pages/ai-article/index.js
const request = require('../../utils/request');
const config = require('../../config/config');

Page({
  data: {
    articles: [],
    keyword: '',
    page: 0,
    size: 10,
    loading: false,
    hasMore: true
  },

  onShow() {
    this.setData({ page: 0, articles: [], hasMore: true });
    this.loadArticles();
  },

  onSearchInput(e) {
    this.setData({ keyword: e.detail.value });
  },

  onSearch() {
    this.setData({ page: 0, articles: [], hasMore: true });
    this.loadArticles();
  },

  async loadArticles() {
    if (this.data.loading || !this.data.hasMore) return;
    this.setData({ loading: true });

    try {
      const res = await request.get('/api/ai/article/history', {
        data: {
          page: this.data.page,
          size: this.data.size,
          keyword: this.data.keyword
        }
      });

      const newArticles = res.content || [];
      this.setData({
        articles: [...this.data.articles, ...newArticles],
        hasMore: !res.last,
        page: this.data.page + 1
      });
    } catch (err) {
      console.error('加载文章失败', err);
      // Mock data for preview if API fails
      if (this.data.page === 0) {
        this.setData({
          articles: [
            { id: 1, topic: 'The Future of AI', difficultyLevel: 'B2', articleLength: '中等', targetLanguage: 'en', createdAt: '2026-03-20 10:00' },
            { id: 2, topic: 'Healthy Lifestyle', difficultyLevel: 'A2', articleLength: '短', targetLanguage: 'en', createdAt: '2026-03-19 15:30' }
          ],
          hasMore: false
        });
      }
    } finally {
      this.setData({ loading: false });
    }
  },

  loadMore() {
    this.loadArticles();
  },

  goToGenerate() {
    wx.navigateTo({ url: '/pages/ai-article/generate' });
  },

  viewDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.showToast({ title: '查看文章 ' + id, icon: 'none' });
    // TODO: Navigate to detail page
  }
});
