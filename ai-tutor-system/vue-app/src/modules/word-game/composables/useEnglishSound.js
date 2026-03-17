const sentenceAudio = new Audio()
let sentencePlayId = 0
let sentenceOnError = null
let sentenceOnEnded = null

function getPronunciationUrl(english) {
  return `https://dict.youdao.com/dictvoice?type=2&audio=${encodeURIComponent(english)}`
}

function playWithBrowserTTS(english) {
  if (typeof window === 'undefined' || !window.speechSynthesis) return
  window.speechSynthesis.cancel()
  const u = new SpeechSynthesisUtterance(english)
  u.lang = 'en-US'
  u.rate = 0.95
  window.speechSynthesis.speak(u)
}

export function playEnglishSound(english) {
  if (!english) return
  const url = getPronunciationUrl(english)
  const currentPlayId = ++sentencePlayId
  let fallbackDone = false

  const doFallback = () => {
    if (fallbackDone || currentPlayId !== sentencePlayId) return
    fallbackDone = true
    if (sentenceOnError) sentenceAudio.removeEventListener('error', sentenceOnError)
    if (sentenceOnEnded) sentenceAudio.removeEventListener('ended', sentenceOnEnded)
    sentenceOnError = null
    sentenceOnEnded = null
    playWithBrowserTTS(english)
  }

  const onSentenceError = () => doFallback()
  const onSentenceEnded = () => {
    if (currentPlayId !== sentencePlayId) return
    if (sentenceOnError) sentenceAudio.removeEventListener('error', sentenceOnError)
    sentenceOnError = null
    sentenceOnEnded = null
  }

  if (sentenceOnError) sentenceAudio.removeEventListener('error', sentenceOnError)
  if (sentenceOnEnded) sentenceAudio.removeEventListener('ended', sentenceOnEnded)
  sentenceOnError = onSentenceError
  sentenceOnEnded = onSentenceEnded
  if (typeof window !== 'undefined' && window.speechSynthesis) {
    window.speechSynthesis.cancel()
  }
  sentenceAudio.pause()
  sentenceAudio.currentTime = 0
  sentenceAudio.addEventListener('error', onSentenceError, { once: true })
  sentenceAudio.addEventListener('ended', onSentenceEnded, { once: true })
  sentenceAudio.src = url
  sentenceAudio.play().catch(() => {
    doFallback()
  })
}

export function usePlayWordSound() {
  const wordAudio = new Audio()
  let isPlaying = false
  let lastWord = ''
  let wordPlayId = 0
  let wordOnError = null
  let wordOnEnded = null

  wordAudio.onplay = () => {
    isPlaying = true
  }
  wordAudio.onended = () => {
    isPlaying = false
  }

  function handlePlayWordSound(word) {
    if (!word) return
    if (isPlaying && lastWord === word) return
    lastWord = word
    const currentPlayId = ++wordPlayId
    let fallbackDone = false

    const doFallback = () => {
      if (fallbackDone || currentPlayId !== wordPlayId) return
      fallbackDone = true
      if (wordOnError) wordAudio.removeEventListener('error', wordOnError)
      if (wordOnEnded) wordAudio.removeEventListener('ended', wordOnEnded)
      wordOnError = null
      wordOnEnded = null
      playWithBrowserTTS(word)
    }

    const onWordError = () => doFallback()
    const onWordEnded = () => {
      if (currentPlayId !== wordPlayId) return
      if (wordOnError) wordAudio.removeEventListener('error', wordOnError)
      wordOnError = null
      wordOnEnded = null
    }

    if (wordOnError) wordAudio.removeEventListener('error', wordOnError)
    if (wordOnEnded) wordAudio.removeEventListener('ended', wordOnEnded)
    wordOnError = onWordError
    wordOnEnded = onWordEnded
    if (typeof window !== 'undefined' && window.speechSynthesis) {
      window.speechSynthesis.cancel()
    }
    wordAudio.pause()
    wordAudio.currentTime = 0
    wordAudio.addEventListener('error', onWordError, { once: true })
    wordAudio.addEventListener('ended', onWordEnded, { once: true })
    wordAudio.src = getPronunciationUrl(word)
    wordAudio.play().catch(() => {
      doFallback()
    })
  }

  return { handlePlayWordSound }
}
