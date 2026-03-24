import audioService from '@/services/audioService'

function playWithBrowserTTS(english) {
  if (typeof window === 'undefined' || !window.speechSynthesis) return
  window.speechSynthesis.cancel()
  const u = new SpeechSynthesisUtterance(english)
  u.lang = 'en-US'
  u.rate = 0.95
  window.speechSynthesis.speak(u)
}

/**
 * 播放英语句子音频
 * 使用 audioService 单例服务
 */
export function playEnglishSound(english) {
  if (!english) return
  
  audioService.playSentence(english, 'en')
    .catch(() => {
      playWithBrowserTTS(english)
    })
}

/**
 * 播放单词音频的 composable
 * 使用 audioService 单例服务
 */
export function usePlayWordSound() {
  let lastWord = ''

  function handlePlayWordSound(word) {
    if (!word) return
    if (audioService.isCurrentlyPlaying() && lastWord === word) return
    lastWord = word

    audioService.playWordSound(word, 2)
      .catch(() => {
        playWithBrowserTTS(word)
      })
  }

  return { handlePlayWordSound }
}
