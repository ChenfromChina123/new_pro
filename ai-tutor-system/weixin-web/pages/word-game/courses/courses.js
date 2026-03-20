// pages/word-game/courses/courses.js
Page({
  data: {
    packageId: null,
    packageInfo: {},
    courses: []
  },

  onLoad(options) {
    if (options.id) {
      this.setData({ packageId: options.id });
    }
    this.fetchCourses();
  },

  fetchCourses() {
    // 模拟数据
    const mockCourses = Array.from({ length: 20 }, (_, i) => {
      const num = i + 1;
      const numStr = num < 10 ? `0${num}` : `${num}`;
      const cnNums = ['一', '二', '三', '四', '五', '六', '七', '八', '九', '十', '十一', '十二', '十三', '十四', '十五', '十六', '十七', '十八', '十九', '二十'];
      
      return {
        id: num,
        courseNumStr: numStr,
        name: `第${cnNums[i]}课`,
        wordCount: Math.floor(Math.random() * 100) + 100, // 100-200
        progress: num < 6 ? Math.floor(Math.random() * 50) + 1 : 0
      };
    });

    this.setData({
      packageInfo: {
        title: '星荣零基础学英语',
        courseCount: 55
      },
      courses: mockCourses
    });
  },

  goBack() {
    wx.navigateBack();
  },

  goToPlay(e) {
    const courseId = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/word-game/play/play?courseId=${courseId}`
    });
  }
});