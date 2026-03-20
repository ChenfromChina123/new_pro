// pages/word-game/index.js
Page({
  data: {
    packages: []
  },

  onLoad() {
    this.fetchPackages();
  },

  fetchPackages() {
    // 模拟课程包数据
    this.setData({
      packages: [
        {
          id: 1,
          name: '星荣零基础学英语',
          tag: '零基础',
          description: '从零开始，系统掌握日常英语词汇与基础句型，适合完全零基础的学习者。',
          courseCount: 55,
          wordCount: 8865,
          clicks: 6
        },
        {
          id: 2,
          name: '四级高频词汇冲刺',
          tag: '四级',
          description: '精选大学英语四级历年真题高频核心词汇，结合例句快速突破。',
          courseCount: 30,
          wordCount: 2500,
          clicks: 128
        },
        {
          id: 3,
          name: '考研英语大纲词汇',
          tag: '考研',
          description: '全面覆盖考研英语大纲要求的必考词汇，深度解析熟词生义。',
          courseCount: 60,
          wordCount: 5500,
          clicks: 45
        }
      ]
    });
  },

  goToCourses(e) {
    const packageId = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/word-game/courses/courses?id=${packageId}`
    });
  }
});
