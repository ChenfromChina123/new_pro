<template>
  <div
    class="vocab-practice-card"
    :class="mode"
  >
    <div class="card-header">
      <div class="word-info">
        <h3
          v-if="mode === 'pronunciation'"
          class="word"
        >
          {{ word }}
        </h3>
        <h3
          v-else
          class="word spelling-hidden"
        >
          <span class="hidden-chars">
            <template
              v-for="(char, i) in word"
              :key="i"
            >_</template>
          </span>
        </h3>
        <span class="phonetic">{{ phonetic }}</span>
      </div>
      <div class="mode-switch">
        <button
          class="switch-btn"
          :class="{ active: mode === 'pronunciation' }"
          @click="mode = 'pronunciation'"
        >
          发音
        </button>
        <button
          class="switch-btn"
          :class="{ active: mode === 'spelling' }"
          @click="mode = 'spelling'"
        >
          拼写
        </button>
      </div>
    </div>

    <div class="translation-box">
      <p class="translation">
        {{ translation }}
      </p>
    </div>

    <div
      v-if="sentence"
      class="sentence-box"
    >
      <p
        class="sentence"
        v-html="formattedSentence"
      />
    </div>

    <!-- 拼写模式下的输入区域 -->
    <div
      v-if="mode === 'spelling'"
      class="spelling-input-area"
    >
      <input 
        ref="spellingInput"
        v-model="userInput" 
        class="spelling-input" 
        :placeholder="`请输入单词 ( ${word.length} 字母 )`"
        @keyup.enter="checkSpelling"
      >
      <button
        class="check-btn"
        :disabled="!userInput.trim() || isChecking"
        @click="checkSpelling"
      >
        验证
      </button>
    </div>

    <!-- 操作区 -->
    <div
      v-if="mode === 'pronunciation'"
      class="card-actions"
    >
      <button
        class="action-btn play-btn"
        :disabled="isPlaying"
        @click="playAudio"
      >
        <i
          class="fas"
          :class="isPlaying ? 'fa-spinner fa-spin' : 'fa-volume-up'"
        /> 朗读
      </button>
      <button 
        class="action-btn record-btn" 
        :class="{ recording: isRecording }" 
        title="按住录音" 
        @mousedown="startRecording"
        @mouseup="stopRecording"
        @mouseleave="stopRecording"
        @touchstart.prevent="startRecording"
        @touchend.prevent="stopRecording"
      >
        <i class="fas fa-microphone" /> {{ isRecording ? '松开结束' : '按住发音' }}
      </button>
    </div>

    <!-- 反馈区 -->
    <div
      v-if="feedback"
      class="feedback-area"
    >
      <div
        v-if="feedback.score !== undefined"
        class="feedback-score"
      >
        <span
          class="score"
          :class="scoreClass"
        >{{ feedback.score }}分</span>
      </div>
      <p class="feedback-text">
        {{ feedback.aiFeedback }}
      </p>
    </div>

    <!-- 发音历史区 -->
    <div
      v-if="pronunciationHistory.length > 0"
      class="history-section"
    >
      <div
        class="history-header"
        @click="showPronunciationHistory = !showPronunciationHistory"
      >
        <h4>🎤 发音历史 ({{ pronunciationHistory.length }}次)</h4>
        <span
          class="expand-icon"
          :class="{ expanded: showPronunciationHistory }"
        >▼</span>
      </div>
      <div
        v-if="showPronunciationHistory"
        class="history-list"
      >
        <div
          v-for="(record, index) in pronunciationHistory.slice(0, 5)"
          :key="index"
          class="history-item"
        >
          <div
            class="history-score"
            :class="getScoreClass(record.score)"
          >
            {{ record.score }}分
          </div>
          <div class="history-details">
            <div class="history-text">
              <span
                class="recognized"
                :class="{ correct: isCorrect(record) }"
              >{{ record.recognizedText }}</span>
              <span
                v-if="!isCorrect(record)"
                class="target"
              >→ {{ record.targetText }}</span>
            </div>
            <div class="history-meta">
              <span class="history-date">{{ formatDate(record.createdAt) }}</span>
              <span
                v-if="record.aiFeedback"
                class="history-feedback"
              >{{ record.aiFeedback }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 练习历史区 -->
    <div
      v-if="practiceHistory.length > 0"
      class="history-section"
    >
      <div
        class="history-header"
        @click="showPracticeHistory = !showPracticeHistory"
      >
        <h4>✏️ 练习历史 ({{ practiceHistory.length }}次)</h4>
        <span
          class="expand-icon"
          :class="{ expanded: showPracticeHistory }"
        >▼</span>
      </div>
      <div
        v-if="showPracticeHistory"
        class="history-list"
      >
        <div
          v-for="(record, index) in practiceHistory.slice(0, 5)"
          :key="index"
          class="history-item"
          :class="record.isCorrect ? 'correct' : 'incorrect'"
        >
          <div
            class="history-score"
            :class="record.isCorrect ? 'correct' : 'incorrect'"
          >
            {{ record.isCorrect ? '✓' : '✗' }}
          </div>
          <div class="history-details">
            <div class="history-text">
              <span
                class="user-input"
                :class="{ correct: record.isCorrect }"
              >{{ record.userInput || 'N/A' }}</span>
              <span
                v-if="!record.isCorrect"
                class="target"
              >→ {{ record.correctAnswer }}</span>
            </div>
            <div class="history-meta">
              <span class="history-type">{{ getPracticeTypeName(record.practiceType) }}</span>
              <span class="history-date">{{ formatDate(record.createdAt) }}</span>
              <span
                v-if="record.aiFeedback"
                class="history-feedback"
              >{{ record.aiFeedback }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { API_CONFIG } from '@/config/api'
import request from '@/utils/request'

const props = defineProps({
  word: { type: String, required: true },
  phonetic: { type: String, default: '' },
  translation: { type: String, default: '' },
  sentence: { type: String, default: '' },
  wordId: { type: Number, default: null },
  initialMode: { type: String, default: 'pronunciation' }
})

const mode = ref(props.initialMode || 'pronunciation')
const isPlaying = ref(false)
const isRecording = ref(false)
const userInput = ref('')
const isChecking = ref(false)
const feedback = ref(null)
const pronunciationHistory = ref([])
const practiceHistory = ref([])
const showPronunciationHistory = ref(false)
const showPracticeHistory = ref(false)

let mediaRecorder = null
let audioChunks = []

const formattedSentence = computed(() => {
  if (mode.value === 'spelling') {
    // 简单地把句子中的目标单词替换为下划线
    const regex = new RegExp(`\\b${props.word}\\b`, 'gi')
    return props.sentence.replace(regex, '<span class="blank">______</span>')
  }
  return props.sentence
})

const scoreClass = computed(() => {
  if (!feedback.value || feedback.value.score === undefined) return ''
  if (feedback.value.score >= 90) return 'excellent'
  if (feedback.value.score >= 70) return 'good'
  return 'needs-work'
})

// 加载发音历史
const loadPronunciationHistory = async () => {
  if (!props.wordId) return
  
  try {
    const response = await request.get(`/api/learning/pronunciation?wordId=${props.wordId}`)
    if (response && response.data && response.data.records) {
      pronunciationHistory.value = response.data.records
      // 如果有历史记录，默认展开
      if (pronunciationHistory.value.length > 0) {
        showPronunciationHistory.value = true
      }
    }
  } catch (error) {
    console.error('加载发音历史失败:', error)
  }
}

// 加载练习历史
const loadPracticeHistory = async () => {
  if (!props.wordId) return
  
  try {
    const response = await request.get(`/api/learning/practice?wordId=${props.wordId}`)
    if (response && response.data && response.data.records) {
      practiceHistory.value = response.data.records
      // 如果有历史记录，默认展开
      if (practiceHistory.value.length > 0) {
        showPracticeHistory.value = true
      }
    }
  } catch (error) {
    console.error('加载练习历史失败:', error)
  }
}

// 获取练习类型名称
const getPracticeTypeName = (type) => {
  const typeMap = {
    'spelling': '拼写',
    'review': '复习',
    'listening': '听写'
  }
  return typeMap[type] || type
}

// 获取分数样式
const getScoreClass = (score) => {
  if (score >= 90) return 'excellent'
  if (score >= 70) return 'good'
  return 'needs-work'
}

// 判断是否正确
const isCorrect = (record) => {
  return record.recognizedText.toLowerCase().includes(record.targetText.toLowerCase()) ||
         record.score >= 80
}

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  const now = new Date()
  const diff = now - date
  
  // 1 分钟内
  if (diff < 60000) return '刚刚'
  // 1 小时内
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  // 24 小时内
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  // 其他
  return date.toLocaleString('zh-CN', { 
    month: 'short', 
    day: 'numeric', 
    hour: '2-digit', 
    minute: '2-digit' 
  })
}

// 监听 wordId 变化，加载历史
watch(() => props.wordId, () => {
  loadPronunciationHistory()
  loadPracticeHistory()
})

onMounted(() => {
  loadPronunciationHistory()
  loadPracticeHistory()
})

// 播放发音 (优先有道 API)
const playAudio = () => {
  if (isPlaying.value) return
  isPlaying.value = true
  
  const audio = new Audio(`https://dict.youdao.com/dictvoice?audio=${encodeURIComponent(props.word)}&type=1`)
  
  audio.onended = () => {
    isPlaying.value = false
  }
  
  audio.onerror = () => {
    console.warn('Youdao API failed, falling back to Web Speech API')
    fallbackTTS(props.word)
  }
  
  audio.play().catch(e => {
    console.error('Audio play error:', e)
    fallbackTTS(props.word)
  })
}

const fallbackTTS = (text) => {
  if (!window.speechSynthesis) {
    isPlaying.value = false
    alert('您的浏览器不支持语音播报')
    return
  }
  const utterance = new SpeechSynthesisUtterance(text)
  utterance.lang = 'en-US'
  utterance.onend = () => { isPlaying.value = false }
  utterance.onerror = () => { isPlaying.value = false }
  window.speechSynthesis.speak(utterance)
}

// 录音相关
const startRecording = async () => {
  if (isRecording.value) return
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    
    // 初始化 AudioContext 用于静音检测
    const audioContext = new (window.AudioContext || window.webkitAudioContext)()
    const analyser = audioContext.createAnalyser()
    const microphone = audioContext.createMediaStreamSource(stream)
    microphone.connect(analyser)
    analyser.fftSize = 256
    const bufferLength = analyser.frequencyBinCount
    const dataArray = new Uint8Array(bufferLength)
    
    let isSilent = true
    const checkSilence = () => {
      if (!isRecording.value) return
      analyser.getByteFrequencyData(dataArray)
      // 计算平均音量
      const sum = dataArray.reduce((a, b) => a + b, 0)
      const average = sum / bufferLength
      if (average > 10) { // 设定音量阈值，大于10认为有声音
        isSilent = false
      }
      if (isRecording.value) {
        requestAnimationFrame(checkSilence)
      }
    }

    mediaRecorder = new MediaRecorder(stream)
    audioChunks = []
    
    mediaRecorder.ondataavailable = (event) => {
      if (event.data.size > 0) {
        audioChunks.push(event.data)
      }
    }
    
    mediaRecorder.onstop = async () => {
      const audioBlob = new Blob(audioChunks, { type: 'audio/webm' })
      stream.getTracks().forEach(track => track.stop())
      audioContext.close()
      
      if (isSilent) {
        feedback.value = { 
          score: 0, 
          aiFeedback: '⚠️ 未检测到声音，请检查麦克风或大声朗读。' 
        }
        return
      }
      
      // 转换为 16kHz WAV 格式给 whisper.cpp 使用
      try {
        const wavBlob = await convertToWav(audioBlob)
        await submitAudioForEvaluation(wavBlob)
      } catch (err) {
        console.error('Audio conversion failed:', err)
        feedback.value = { aiFeedback: '音频格式转换失败。' }
      }
    }
    
    mediaRecorder.start()
    isRecording.value = true
    feedback.value = null
    checkSilence() // 开始检测音量
  } catch (error) {
    console.error('Microphone access denied:', error)
    alert('无法访问麦克风，请检查浏览器权限设置。')
  }
}

const stopRecording = () => {
  if (isRecording.value && mediaRecorder && mediaRecorder.state === 'recording') {
    mediaRecorder.stop()
    isRecording.value = false
  }
}

const submitAudioForEvaluation = async (blob) => {
  feedback.value = { aiFeedback: '正在分析发音...' }
  try {
    const formData = new FormData()
    formData.append('audio', blob, 'record.wav')
    formData.append('targetText', props.word)
    
    const response = await request.post('/api/ai/speech/evaluate', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    
    if (response && response.data) {
      feedback.value = response.data
    } else {
      feedback.value = { aiFeedback: '评测失败，未获取到结果。' }
    }
  } catch (error) {
    console.error('Speech eval error:', error)
    feedback.value = { aiFeedback: '评测服务请求失败，请稍后再试。' }
  }
}

// 转换为 16kHz 16-bit Mono WAV 给 whisper.cpp 使用
const convertToWav = async (webmBlob) => {
  const audioContext = new (window.AudioContext || window.webkitAudioContext)({ sampleRate: 16000 })
  const arrayBuffer = await webmBlob.arrayBuffer()
  const audioBuffer = await audioContext.decodeAudioData(arrayBuffer)
  
  const numOfChannels = 1
  const sampleRate = 16000
  const length = audioBuffer.length
  
  const offlineCtx = new OfflineAudioContext(numOfChannels, length, sampleRate)
  const source = offlineCtx.createBufferSource()
  source.buffer = audioBuffer
  source.connect(offlineCtx.destination)
  source.start()
  const renderedBuffer = await offlineCtx.startRendering()
  
  const audioData = renderedBuffer.getChannelData(0)
  const wavBuffer = encodeWAV(audioData, sampleRate)
  return new Blob([wavBuffer], { type: 'audio/wav' })
}

const encodeWAV = (samples, sampleRate) => {
  const buffer = new ArrayBuffer(44 + samples.length * 2)
  const view = new DataView(buffer)
  
  const writeString = (view, offset, string) => {
    for (let i = 0; i < string.length; i++) {
      view.setUint8(offset + i, string.charCodeAt(i))
    }
  }
  
  writeString(view, 0, 'RIFF')
  view.setUint32(4, 36 + samples.length * 2, true)
  writeString(view, 8, 'WAVE')
  writeString(view, 12, 'fmt ')
  view.setUint32(16, 16, true)
  view.setUint16(20, 1, true)
  view.setUint16(22, 1, true)
  view.setUint32(24, sampleRate, true)
  view.setUint32(28, sampleRate * 2, true)
  view.setUint16(32, 2, true)
  view.setUint16(34, 16, true)
  writeString(view, 36, 'data')
  view.setUint32(40, samples.length * 2, true)
  
  let offset = 44
  for (let i = 0; i < samples.length; i++, offset += 2) {
    let s = Math.max(-1, Math.min(1, samples[i]))
    view.setInt16(offset, s < 0 ? s * 0x8000 : s * 0x7FFF, true)
  }
  
  return view
}

// 拼写检查
const checkSpelling = async () => {
  if (!userInput.value.trim() || isChecking.value) return
  
  if (userInput.value.trim().toLowerCase() === props.word.toLowerCase()) {
    feedback.value = { aiFeedback: '拼写完全正确！🎉', score: 100 }
    return
  }
  
  isChecking.value = true
  feedback.value = { aiFeedback: '正在分析拼写错误...' }
  
  try {
    const response = await request.post('/api/ai/spelling/evaluate', {
      targetWord: props.word,
      userSpelling: userInput.value.trim()
    })
    
    if (response && response.data) {
      feedback.value = response.data
    } else {
      feedback.value = { aiFeedback: '错误：与标准拼写不符。' }
    }
  } catch (error) {
    console.error('Spelling eval error:', error)
    feedback.value = { aiFeedback: '请求拼写分析失败。' }
  } finally {
    isChecking.value = false
  }
}
</script>

<style scoped>
.vocab-practice-card {
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 16px;
  background-color: var(--bg-secondary);
  margin: 12px 0;
  max-width: 400px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
  transition: all 0.3s ease;
}

.vocab-practice-card:hover {
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  border-color: var(--primary-color);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.word-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.word {
  margin: 0;
  font-size: 1.5rem;
  color: var(--text-primary);
  font-weight: 600;
}

.hidden-chars {
  letter-spacing: 4px;
  color: var(--text-secondary);
}

.phonetic {
  font-size: 0.9rem;
  color: var(--text-secondary);
  font-family: 'Courier New', Courier, monospace;
}

.mode-switch {
  display: flex;
  background: var(--bg-primary);
  border-radius: 6px;
  padding: 2px;
}

.switch-btn {
  border: none;
  background: transparent;
  padding: 4px 10px;
  font-size: 0.8rem;
  color: var(--text-secondary);
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
}

.switch-btn.active {
  background: var(--primary-color);
  color: white;
}

.translation-box {
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px dashed var(--border-color);
}

.translation {
  margin: 0;
  color: var(--text-primary);
  font-size: 1rem;
}

.sentence-box {
  margin-bottom: 16px;
  background: var(--bg-primary);
  padding: 10px;
  border-radius: 6px;
  border-left: 3px solid var(--primary-color);
}

.sentence {
  margin: 0;
  font-size: 0.95rem;
  color: var(--text-primary);
  line-height: 1.5;
}

:deep(.blank) {
  display: inline-block;
  min-width: 40px;
  color: var(--text-secondary);
}

.card-actions {
  display: flex;
  gap: 10px;
}

.action-btn {
  flex: 1;
  padding: 10px;
  border: none;
  border-radius: 8px;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  transition: all 0.2s;
}

.play-btn {
  background-color: var(--bg-primary);
  color: var(--primary-color);
  border: 1px solid var(--primary-color);
}

.play-btn:hover {
  background-color: var(--primary-color);
  color: white;
}

.record-btn {
  background-color: var(--primary-color);
  color: white;
}

.record-btn.recording {
  background-color: #ef4444;
  animation: pulse-red 1.5s infinite;
}

@keyframes pulse-red {
  0% { box-shadow: 0 0 0 0 rgba(239, 68, 68, 0.4); }
  70% { box-shadow: 0 0 0 10px rgba(239, 68, 68, 0); }
  100% { box-shadow: 0 0 0 0 rgba(239, 68, 68, 0); }
}

.spelling-input-area {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.spelling-input {
  flex: 1;
  padding: 10px 12px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--bg-primary);
  color: var(--text-primary);
  font-size: 1rem;
  outline: none;
  transition: border-color 0.2s;
}

.spelling-input:focus {
  border-color: var(--primary-color);
}

.check-btn {
  padding: 0 16px;
  background: var(--primary-color);
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
}

.check-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.feedback-area {
  margin-top: 16px;
  padding: 12px;
  background: rgba(16, 185, 129, 0.1);
  border-radius: 8px;
  border-left: 3px solid #10b981;
}

.feedback-score {
  margin-bottom: 8px;
}

.score {
  font-weight: bold;
  font-size: 1.1rem;
}

.score.excellent { color: #10b981; }
.score.good { color: #3b82f6; }
.score.needs-work { color: #f59e0b; }

.feedback-text {
  margin: 0;
  font-size: 0.9rem;
  color: var(--text-primary);
  line-height: 1.4;
}

/* 发音历史样式 */
.history-section {
  margin-top: 16px;
  border-top: 1px solid var(--border-color);
  padding-top: 12px;
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
  padding: 8px 0;
  transition: all 0.2s;
}

.history-header:hover {
  opacity: 0.8;
}

.history-header h4 {
  margin: 0;
  font-size: 0.95rem;
  color: var(--text-primary);
}

.expand-icon {
  font-size: 0.8rem;
  color: var(--text-secondary);
  transition: transform 0.3s;
}

.expand-icon.expanded {
  transform: rotate(180deg);
}

.history-list {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.history-item {
  display: flex;
  gap: 12px;
  padding: 10px;
  background: var(--bg-primary);
  border-radius: 8px;
  border-left: 3px solid var(--primary-color);
  transition: all 0.2s;
}

.history-item:hover {
  transform: translateX(4px);
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.history-score {
  min-width: 50px;
  padding: 6px 10px;
  border-radius: 6px;
  font-weight: bold;
  font-size: 0.9rem;
  text-align: center;
  height: fit-content;
}

.history-score.excellent {
  background: rgba(16, 185, 129, 0.15);
  color: #10b981;
}

.history-score.good {
  background: rgba(59, 130, 246, 0.15);
  color: #3b82f6;
}

.history-score.needs-work {
  background: rgba(245, 158, 11, 0.15);
  color: #f59e0b;
}

.history-details {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.history-text {
  font-size: 0.9rem;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.history-text .recognized {
  font-weight: 500;
}

.history-text .recognized.correct {
  color: #10b981;
  text-decoration: underline;
  text-decoration-style: wavy;
}

.history-text .target {
  color: var(--text-secondary);
  font-size: 0.85rem;
}

.history-meta {
  display: flex;
  gap: 12px;
  font-size: 0.8rem;
  color: var(--text-secondary);
}

.history-date {
  font-weight: 500;
}

.history-feedback {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 练习历史样式 */
.history-item.correct {
  border-left-color: #10b981;
}

.history-item.incorrect {
  border-left-color: #ef4444;
}

.history-score.correct {
  background: rgba(16, 185, 129, 0.15);
  color: #10b981;
  font-size: 1.2rem;
}

.history-score.incorrect {
  background: rgba(239, 68, 68, 0.15);
  color: #ef4444;
  font-size: 1.2rem;
}

.history-text .user-input {
  font-weight: 500;
}

.history-text .user-input.correct {
  color: #10b981;
  text-decoration: underline;
  text-decoration-style: wavy;
}

.history-type {
  background: var(--primary-color);
  color: white;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 500;
}
</style>
