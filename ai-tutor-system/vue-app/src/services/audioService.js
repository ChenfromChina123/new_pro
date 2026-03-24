/**
 * 音频服务单例
 * 统一管理音频播放，避免重复创建 Audio 对象
 */
class AudioService {
  constructor() {
    this.sentenceAudio = new Audio()
    this.wordAudioPool = []
    this.maxPoolSize = 5
    this.currentWordAudio = null
    this.isPlaying = false
  }

  /**
   * 播放句子音频
   * @param {string} text - 要播放的文本
   * @param {string} lang - 语言代码
   * @returns {Promise<void>}
   */
  playSentence(text, lang = 'en') {
    return new Promise((resolve, reject) => {
      this.stopSentence()

      const url = this.getTtsUrl(text, lang)
      this.sentenceAudio.src = url
      this.sentenceAudio.onended = () => {
        this.isPlaying = false
        resolve()
      }
      this.sentenceAudio.onerror = (error) => {
        this.isPlaying = false
        reject(error)
      }

      this.isPlaying = true
      this.sentenceAudio.play().catch(reject)
    })
  }

  /**
   * 停止句子音频播放
   */
  stopSentence() {
    if (this.sentenceAudio) {
      this.sentenceAudio.pause()
      this.sentenceAudio.currentTime = 0
      this.sentenceAudio.src = ''
    }
    this.isPlaying = false
  }

  /**
   * 播放单词音频
   * @param {string} word - 单词
   * @param {string} type - 音频类型 (1=美音, 2=英音)
   * @returns {Promise<void>}
   */
  playWordSound(word, type = 1) {
    return new Promise((resolve, reject) => {
      this.stopWordSound()

      this.currentWordAudio = this.wordAudioPool.pop() || new Audio()
      const url = `https://dict.youdao.com/dictvoice?audio=${encodeURIComponent(word)}&type=${type}`
      
      this.currentWordAudio.src = url
      this.currentWordAudio.onended = () => {
        this.isPlaying = false
        if (this.wordAudioPool.length < this.maxPoolSize) {
          this.wordAudioPool.push(this.currentWordAudio)
        }
        this.currentWordAudio = null
        resolve()
      }
      this.currentWordAudio.onerror = (error) => {
        this.isPlaying = false
        reject(error)
      }

      this.isPlaying = true
      this.currentWordAudio.play().catch(reject)
    })
  }

  /**
   * 停止单词音频播放
   */
  stopWordSound() {
    if (this.currentWordAudio) {
      this.currentWordAudio.pause()
      this.currentWordAudio.currentTime = 0
      this.currentWordAudio.src = ''
      if (this.wordAudioPool.length < this.maxPoolSize) {
        this.wordAudioPool.push(this.currentWordAudio)
      }
      this.currentWordAudio = null
    }
    this.isPlaying = false
  }

  /**
   * 停止所有音频
   */
  stopAll() {
    this.stopSentence()
    this.stopWordSound()
  }

  /**
   * 检查是否正在播放
   * @returns {boolean}
   */
  isCurrentlyPlaying() {
    return this.isPlaying
  }

  /**
   * 设置音量
   * @param {number} volume - 音量 (0-1)
   */
  setVolume(volume) {
    const vol = Math.max(0, Math.min(1, volume))
    this.sentenceAudio.volume = vol
    if (this.currentWordAudio) {
      this.currentWordAudio.volume = vol
    }
  }

  /**
   * 获取 TTS URL
   * @param {string} text - 文本
   * @param {string} lang - 语言
   * @returns {string} TTS URL
   */
  getTtsUrl(text, lang = 'en') {
    return `https://dict.youdao.com/dictvoice?audio=${encodeURIComponent(text)}&le=${lang}`
  }

  /**
   * 清理资源
   */
  dispose() {
    this.stopAll()
    this.wordAudioPool = []
    this.sentenceAudio = null
  }
}

const audioService = new AudioService()
export default audioService
