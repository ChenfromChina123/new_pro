// pages/word-game/play/play.js
const request = require('../../../utils/request');
const config = require('../../../config/config');

Page({
  data: {
    started: true,
    gameOver: false,
    score: 0,
    timeLeft: 30,
    currentWord: null,
    answer: '',
    blanks: [],
    cursorIndex: 0,
    inputFocus: false,
    timer: null,
    audioContext: null,
    showCustomToast: false,
    toastType: '',
    toastMsg: ''
  },

  onLoad(options) {
    this.initAudioContext();
    this.startGame();
  },

  onUnload() {
    if (this.data.timer) clearInterval(this.data.timer);
    if (this.data.audioContext) this.data.audioContext.destroy();
  },

  initAudioContext() {
    const audioContext = wx.createInnerAudioContext();
    this.setData({ audioContext });
  },

  async startGame() {
    this.setData({
      started: true,
      gameOver: false,
      score: 0,
      timeLeft: 30,
      answer: ''
    });
    await this.fetchNextWord();
    this.startTimer();
  },

  startTimer() {
    if (this.data.timer) clearInterval(this.data.timer);
    const timer = setInterval(() => {
      const { timeLeft } = this.data;
      if (timeLeft <= 1) {
        this.endGame();
      } else {
        this.setData({ timeLeft: timeLeft - 1 });
      }
    }, 1000);
    this.setData({ timer });
  },

  async fetchNextWord() {
    try {
      // 模拟包含句子的数据
      const words = [
        { word: 'i don\'t want to do it', mean: '我不想做这件事情', phonetic: '/aɪ/ /doʊnt/ /wɒnt/ /tu/ /du/ /ɪt/' },
        { word: 'i dislike this food', mean: '我不喜欢这个食物', phonetic: '/aɪ/ /dɪsˈlaɪk/ /ðɪs/ /fud/' },
        { word: 'apple is sweet', mean: '苹果是甜的', phonetic: '/ˈæpl/ /ɪz/ /swiːt/' },
        { word: 'intelligent system', mean: '智能系统', phonetic: '/ɪnˈtɛlɪdʒənt/ /ˈsɪstəm/' }
      ];
      const randomIndex = Math.floor(Math.random() * words.length);
      const nextWord = words[randomIndex];
      
      // 按空格拆分句子为单词数组
      const wordParts = nextWord.word.split(' ');
      // 初始化填空数组：每个元素代表一个字母的填空状态，空格也作为一个元素
      const blanks = [];
      for (let i = 0; i < nextWord.word.length; i++) {
        const char = nextWord.word[i];
        if (char === ' ') {
          // 空格作为一个分隔元素
          blanks.push({ char: ' ', isSpace: true, wordIndex: blanks.length });
        } else {
          // 字母作为填空元素
          blanks.push({ char: '', isSpace: false, wordIndex: blanks.length });
        }
      }
      
      this.setData({ 
        currentWord: nextWord,
        answer: '',
        blanks: blanks,
        cursorIndex: 0,
        inputFocus: true
      });
      
      // 自动播放发音
      this.playVoice();
      
    } catch (err) {
      console.error('Fetch word error:', err);
    }
  },

  playVoice() {
    const word = this.data.currentWord?.word;
    if (!word || !this.data.audioContext) return;

    // 首选有道 API
    const youdaoUrl = `https://dict.youdao.com/dictvoice?audio=${encodeURIComponent(word)}&type=1`;
    // 备选百度/浏览器 API (微信小程序中没有原生浏览器speechSynthesis，通常使用云服务兜底)
    const baiduUrl = `https://fanyi.baidu.com/gettts?lan=en&text=${encodeURIComponent(word)}&spd=3&source=web`;

    const audioCtx = this.data.audioContext;
    audioCtx.src = youdaoUrl;
    
    // 监听播放错误，降级到备选方案
    audioCtx.onError((res) => {
      console.warn('Youdao TTS failed, fallback to secondary TTS', res);
      audioCtx.src = baiduUrl;
      audioCtx.play();
    });

    audioCtx.play();
  },

  focusInput() {
    this.setData({ inputFocus: true });
  },

  showToast(type, msg) {
    this.setData({
      showCustomToast: true,
      toastType: type,
      toastMsg: msg
    });
    setTimeout(() => {
      this.setData({ showCustomToast: false });
    }, 1500);
  },

  showHint() {
    if (!this.data.currentWord) return;
    const word = this.data.currentWord.word;
    this.showToast('error', `提示：${word}`);
    this.playVoice();
  },

  handleInput(e) {
    const value = e.detail.value.toLowerCase();
    const targetWord = this.data.currentWord.word.toLowerCase();
    
    // 限制输入长度不能超过目标句子
    let finalValue = value;
    if (finalValue.length > targetWord.length) {
      finalValue = finalValue.substring(0, targetWord.length);
    }
    
    // 更新填空显示：每个字母对应一个位置
    const blanks = this.data.blanks.map((item, index) => {
      if (item.isSpace) {
        // 空格位置保持空格
        return { char: ' ', isSpace: true, wordIndex: item.wordIndex };
      } else {
        // 字母位置显示对应的输入字符
        const char = finalValue[index] || '';
        return { char, isSpace: false, wordIndex: item.wordIndex };
      }
    });
    
    // 计算光标位置 (当前填写到第几个字母，跳过空格)
    let cursorIndex = finalValue.length;
    if (cursorIndex > blanks.length - 1) cursorIndex = blanks.length - 1;
    
    this.setData({ 
      answer: finalValue,
      blanks: blanks,
      cursorIndex: cursorIndex
    });

    // 如果所有字母都填满了，自动校验
    const isFull = finalValue.length === targetWord.length && finalValue.replace(/\s/g, '').length === targetWord.replace(/\s/g, '').length;

    if (isFull) {
      setTimeout(() => {
        this.checkAnswer();
      }, 300);
    }
  },

  async checkAnswer() {
    const { answer, currentWord, score, timeLeft } = this.data;
    // 比较去掉多余空格后的字符串
    if (answer.trim() === currentWord.word.toLowerCase()) {
      this.showToast('success', '太棒了！');
      this.setData({
        score: score + 10,
        timeLeft: timeLeft + 5
      });
      setTimeout(async () => {
        await this.fetchNextWord();
      }, 1000);
    } else {
      this.showToast('error', '再想想哦~');
      // 清空输入，重新尝试
      const blanks = this.data.blanks.map(item => {
        if (item.isSpace) {
          return { char: ' ', isSpace: true, wordIndex: item.wordIndex };
        } else {
          return { char: '', isSpace: false, wordIndex: item.wordIndex };
        }
      });
      this.setData({
        answer: '',
        blanks: blanks,
        cursorIndex: 0,
        inputFocus: true
      });
    }
  },

  endGame() {
    clearInterval(this.data.timer);
    this.setData({
      gameOver: true,
      started: false
    });
  },

  quitGame() {
    if (this.data.timer) clearInterval(this.data.timer);
    if (this.data.audioContext) this.data.audioContext.destroy();
    wx.navigateBack();
  }
});
