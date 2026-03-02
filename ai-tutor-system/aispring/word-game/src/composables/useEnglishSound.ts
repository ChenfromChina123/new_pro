/**
 * 英文读音播放组合式函数
 * 优先使用有道词典 API；失败时降级为浏览器自带语音合成（Web Speech API）
 * 有道：https://dict.youdao.com/dictvoice?type=2&audio=word
 */

/** 全局单例 Audio 实例，避免重复创建 */
const audio = new Audio();

/**
 * 根据英文文本生成有道词典发音 URL（默认美式）
 */
function getPronunciationUrl(english: string): string {
  return `https://dict.youdao.com/dictvoice?type=2&audio=${encodeURIComponent(english)}`;
}

/**
 * 使用浏览器自带语音合成（Web Speech API）朗读英文
 * @param english 要朗读的英文文本
 */
function playWithBrowserTTS(english: string): void {
  if (typeof window === "undefined" || !window.speechSynthesis) return;
  window.speechSynthesis.cancel();
  const u = new SpeechSynthesisUtterance(english);
  u.lang = "en-US";
  u.rate = 0.95;
  window.speechSynthesis.speak(u);
}

/**
 * 播放英文句子/单词读音
 * 先尝试有道；若加载/播放失败则降级为浏览器自带 TTS
 * @param english 要朗读的英文文本
 */
export function playEnglishSound(english: string | undefined): void {
  if (!english) return;
  const url = getPronunciationUrl(english);
  let fallbackDone = false;

  const doFallback = () => {
    if (fallbackDone) return;
    fallbackDone = true;
    audio.removeEventListener("error", onSentenceError);
    audio.removeEventListener("ended", onSentenceEnded);
    playWithBrowserTTS(english);
  };

  const onSentenceError = () => doFallback();
  const onSentenceEnded = () => {
    audio.removeEventListener("error", onSentenceError);
  };

  audio.pause();
  audio.currentTime = 0;
  audio.addEventListener("error", onSentenceError, { once: true });
  audio.addEventListener("ended", onSentenceEnded, { once: true });
  audio.src = url;
  audio.play().catch(() => doFallback());
}

/**
 * 播放单词读音（点击单词时使用）
 * 为避免与句子音频共享实例而产生冲突，单独创建实例
 */
export function usePlayWordSound() {
  const wordAudio = new Audio();
  let isPlaying = false;
  let lastWord = "";

  wordAudio.onplay = () => { isPlaying = true; };
  wordAudio.onended = () => { isPlaying = false; };

  /**
   * 播放指定单词的读音
   * @param word 要朗读的单词
   */
  function handlePlayWordSound(word: string): void {
    // 同一单词正在播放时，不重复触发
    if (isPlaying && lastWord === word) return;
    lastWord = word;
    wordAudio.src = getPronunciationUrl(word);
    wordAudio.play().catch(() => {});
  }

  return { handlePlayWordSound };
}
