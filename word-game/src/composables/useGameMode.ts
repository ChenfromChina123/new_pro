import { ref } from "vue";

/**
 * 游戏显示模式：
 * - Question：显示输入区，用户正在作答
 * - Answer：已答对，显示完整句子供用户学习
 */
export enum GameMode {
  Question = "question",
  Answer = "answer",
}

const gameMode = ref<GameMode>(GameMode.Question);

/**
 * 管理游戏当前显示状态（问题模式 / 答案模式）
 */
export function useGameMode() {
  function showAnswer() {
    gameMode.value = GameMode.Answer;
  }

  function showQuestion() {
    gameMode.value = GameMode.Question;
  }

  function isAnswer() {
    return gameMode.value === GameMode.Answer;
  }

  function isQuestion() {
    return gameMode.value === GameMode.Question;
  }

  /** 重置为问题模式（切换题目时调用） */
  function resetMode() {
    gameMode.value = GameMode.Question;
  }

  return {
    gameMode,
    isAnswer,
    isQuestion,
    showAnswer,
    showQuestion,
    resetMode,
  };
}
