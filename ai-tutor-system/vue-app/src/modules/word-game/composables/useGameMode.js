import { ref } from 'vue'

const gameMode = ref('question')

export function useGameMode() {
  function showAnswer() {
    gameMode.value = 'answer'
  }

  function showQuestion() {
    gameMode.value = 'question'
  }

  function isAnswer() {
    return gameMode.value === 'answer'
  }

  function isQuestion() {
    return gameMode.value === 'question'
  }

  function resetMode() {
    gameMode.value = 'question'
  }

  return {
    gameMode,
    isAnswer,
    isQuestion,
    showAnswer,
    showQuestion,
    resetMode
  }
}
