/**
 * 英文读音播放组合式函数
 * 使用有道词典 API 播放美式/英式发音
 * 美式发音：https://dict.youdao.com/dictvoice?type=2&audio=word
 * 英式发音：https://dict.youdao.com/dictvoice?type=1&audio=word
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
 * 播放英文句子/单词读音
 * @param english 要朗读的英文文本
 */
export function playEnglishSound(english: string | undefined): void {
  if (!english) return;
  const url = getPronunciationUrl(english);
  // 若当前正在播放同一音频，则重播
  audio.pause();
  audio.currentTime = 0;
  audio.src = url;
  audio.play().catch(() => {
    // 部分浏览器在未经用户交互前禁止自动播放，静默忽略
  });
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
