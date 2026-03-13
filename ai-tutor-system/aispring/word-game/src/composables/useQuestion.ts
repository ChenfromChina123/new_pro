import { nextTick, reactive, ref, watch } from "vue";
import type { WatchStopHandle } from "vue";

/**
 * 单词在输入框中的状态
 */
interface Word {
  text: string;
  isActive: boolean;
  userInput: string;
  incorrect: boolean;
  end: number;
  start: number;
  position: number;
  id: number;
}

/**
 * useInput 的初始化参数
 */
interface InputOptions {
  /** 获取当前英文句子 */
  source: () => string;
  /** 设置 input 光标位置 */
  setInputCursorPosition: (position: number) => void;
  /** 获取 input 光标位置 */
  getInputCursorPosition: () => number;
  /** 每次键盘输入后的回调（用于声音等副作用） */
  inputChangedCallback?: (e: KeyboardEvent) => void;
}

/**
 * 输入模式：
 * - Input：正常输入
 * - Fix：答案提交后有错误，等待修复
 * - Fix_Input：修复中，用户正在编辑某个错误单词
 */
enum Mode {
  Input = "input",
  Fix = "fix",
  Fix_Input = "fix-input",
}

const separator = " ";

/**
 * 判断字符串是否是"单词"（包含字母或数字），用于区分标点与单词
 */
export function isWord(content: string) {
  return /[a-zA-Z0-9]/.test(content);
}

/**
 * 核心打字输入逻辑
 * 移植自 earthworm apps/client/composables/main/question.ts
 * 已针对 Vue 3 独立项目优化，修复了全局变量污染与 watchEffect 循环触发问题
 */
export function useInput({
  source,
  setInputCursorPosition,
  getInputCursorPosition,
  inputChangedCallback,
}: InputOptions) {
  // --- 状态移入函数内部，避免全局污染 ---
  const mode = ref<Mode>(Mode.Input);
  const inputValue = ref("");
  const userInputWords = reactive<Word[]>([]);
  let currentEditWord: Word | null = null;
  let stopWatch: WatchStopHandle | null = null;

  /** 初始化/重置输入状态，每次切换新题目时调用 */
  function initialize() {
    stopWatch && stopWatch();
    mode.value = Mode.Input;
    inputValue.value = "";
    userInputWords.length = 0;
    setupUserInputWords();
    updateActiveWord(getInputCursorPosition());
  }

  /** 清空输入内容（外部可用） */
  function clearInput() {
    inputValue.value = "";
  }

  /** 外部设置 inputValue 时同步更新各 word 的 userInput */
  function setInputValue(val: string) {
    inputValue.value = val;
    resetAllWordUserInput();
    inputSyncUserInputWords();
    updateActiveWord(val ? getInputCursorPosition() : 0);
  }

  /** 创建一个单词状态对象 */
  function createWord(word: string, id: number) {
    return reactive({
      text: word,
      isActive: false,
      userInput: "",
      incorrect: false,
      start: 0,
      end: 0,
      position: 0,
      id,
    } as Word);
  }

  /** 监听 source() 变化，解析单词列表 */
  function setupUserInputWords() {
    // 修复：改用 watch 明确监听 source()，避免 watchEffect 自动追踪 resetUserInputWords 里的 inputValue 导致死循环
    stopWatch = watch(
      source,
      (english) => {
        resetUserInputWords();
        if (!english) return;

        console.log('[调试 useInput] 正在解析英文句子:', english);
        let inputWordIndex = 0;
        const words = english.split(separator);

        words.forEach((text, index) => {
          if (isWord(text)) {
            const word = createWord(text, index);
            // 修复：使用 push 确保 Vue 3 数组响应式
            userInputWords.push(word);
            // 第一个单词设为激活
            if (inputWordIndex === 0) {
              word.isActive = true;
            }
            inputWordIndex++;
          }
        });
        console.log('[调试 useInput] 解析完成，userInputWords 长度:', userInputWords.length);

        // 解析完成后，同步当前输入内容
        inputSyncUserInputWords();
        updateActiveWord(getInputCursorPosition());
      },
      { immediate: true }
    );
  }

  /** 将 userInputWords 的 userInput 同步回 inputValue */
  function userInputWordsSyncInput() {
    inputValue.value = userInputWords.map(({ userInput }) => userInput).join(separator);
  }

  /** 将 inputValue 按空格拆分，同步到各 word 的 userInput 及位置信息 */
  function inputSyncUserInputWords() {
    let position = 0;
    inputValue.value.split(separator).forEach((input, index) => {
      if (userInputWords[index] !== undefined) {
        userInputWords[index].userInput = input;
        userInputWords[index].start = position;
        userInputWords[index].end = position + input.length;
        position += input.length + 1;
      }
    });
  }

  function resetAllWordUserInput() {
    userInputWords.forEach((word) => { word.userInput = ""; });
  }

  function resetAllWordActive() {
    userInputWords.forEach((word) => { word.isActive = false; });
  }

  /** 根据光标位置确定当前激活的单词 */
  function updateActiveWord(position: number) {
    resetAllWordActive();
    for (let i = 0; i < userInputWords.length; i++) {
      const word = userInputWords[i];
      if (position >= word.start && position <= word.end) {
        word.isActive = true;
        break;
      }
    }
  }

  /** 检查所有单词是否均正确 */
  function checkWordCorrect() {
    return userInputWords.every((w) => !w.incorrect);
  }

  /** 末尾单词允许省略句号 */
  function formatLastWordUserInput(word: Word, index: number) {
    const isLastWord = userInputWords.length - 1 === index;
    if (isLastWord && word.userInput.endsWith(".")) {
      word.userInput = word.userInput.slice(0, -1);
    }
  }

  /** 标记所有错误单词 */
  function markIncorrectWord() {
    userInputWords.forEach((word, index) => {
      formatLastWordUserInput(word, index);
      const formatted = formatInputText(word.userInput);
      word.incorrect = formatted !== word.text.toLocaleLowerCase();
    });
  }

  function lastWordIsActive() {
    return userInputWords[userInputWords.length - 1]?.isActive ?? false;
  }

  /** 从当前编辑位置往后找下一个错误单词 */
  function findNextIncorrectWord() {
    if (!currentEditWord) return;
    const wordIndex = userInputWords.findIndex((w) => w.id === currentEditWord.id);
    for (let i = wordIndex + 1; i < userInputWords.length; i++) {
      if (userInputWords[i].incorrect) return userInputWords[i];
    }
  }

  /** 模糊匹配：将各种引号统一为标准引号 */
  function formatInputText(word: string) {
    return word.toLocaleLowerCase().replace(/'|'|"|"|"/g, "'");
  }

  function isLastIncorrectWord() {
    return !findNextIncorrectWord();
  }

  function getFirstIncorrectWord() {
    return userInputWords.find((w) => w.incorrect);
  }

  /** 清空某个错误单词，并将光标定位到该单词起始位置 */
  async function clearNextIncorrectWord(word: Word) {
    word.userInput = "";
    currentEditWord = word;
    userInputWordsSyncInput();
    await nextTick();
    setInputCursorPosition(word.start);
    updateActiveWord(word.start);
  }

  /**
   * 提交答案
   * - 全部正确：调用 correctCallback，清空输入
   * - 有错误：进入 Fix 模式，调用 wrongCallback
   */
  function submitAnswer(correctCallback?: () => void, wrongCallback?: () => void) {
    if (mode.value === Mode.Fix) return;
    resetAllWordActive();
    markIncorrectWord();
    if (checkWordCorrect()) {
      mode.value = Mode.Input;
      correctCallback?.();
      inputValue.value = "";
    } else {
      mode.value = Mode.Fix;
      wrongCallback?.();
    }
  }

  async function fixFirstIncorrectWord() {
    if (mode.value === Mode.Fix) {
      mode.value = Mode.Fix_Input;
      await clearNextIncorrectWord(getFirstIncorrectWord()!);
    }
  }

  async function fixNextIncorrectWord() {
    if (mode.value === Mode.Fix_Input) {
      await clearNextIncorrectWord(findNextIncorrectWord()!);
    }
  }

  async function fixIncorrectWord() {
    if (mode.value === Mode.Fix) {
      await fixFirstIncorrectWord();
    } else if (mode.value === Mode.Fix_Input) {
      await fixNextIncorrectWord();
    }
  }

  function isEmptyOfCurrentEditWord() {
    return currentEditWord?.userInput?.length <= 0;
  }

  function findPreviousIncorrectWord() {
    if (!currentEditWord) return;
    const wordIndex = userInputWords.findIndex((w) => w.id === currentEditWord.id);
    for (let i = wordIndex - 1; i >= 0; i--) {
      if (userInputWords[i].incorrect) return userInputWords[i];
    }
  }

  async function activePreviousIncorrectWord() {
    const prev = findPreviousIncorrectWord();
    if (prev) {
      currentEditWord = prev;
      await nextTick();
      updateActiveWord(prev.end);
      setInputCursorPosition(prev.end);
    }
  }

  interface KeyboardInputOptions {
    useSpaceSubmitAnswer?: {
      enable: boolean;
      rightCallback?: () => void;
      errorCallback?: () => void;
    };
  }

  function handleSpaceSubmitAnswer(opt: KeyboardInputOptions["useSpaceSubmitAnswer"]) {
    if (opt?.enable) {
      submitAnswer(opt.rightCallback, opt.errorCallback);
    }
  }

  /**
   * 处理键盘事件
   * 包含：方向键拦截、空格提交、Fix 模式修复跳转
   */
  function handleKeyboardInput(e: KeyboardEvent, options?: KeyboardInputOptions) {
    // 禁止方向键移动光标
    if (["ArrowUp", "ArrowDown", "ArrowLeft", "ArrowRight"].includes(e.code)) {
      e.preventDefault();
      return;
    }

    // 正常/修复输入模式下，空格在最后一个单词时触发提交
    if (mode.value !== Mode.Fix && e.code === "Space" && lastWordIsActive()) {
      e.preventDefault();
      e.stopPropagation();
      handleSpaceSubmitAnswer(options?.useSpaceSubmitAnswer);
      return;
    }

    // Fix 模式：任意键触发定位第一个错误单词
    if (mode.value === Mode.Fix) {
      if (e.code === "Space" || e.code === "Backspace") {
        e.preventDefault();
      }
      fixFirstIncorrectWord();
      inputChangedCallback?.(e);
      return;
    }

    // Fix_Input 模式：最后一个错误单词时空格提交
    if (mode.value === Mode.Fix_Input && e.code === "Space" && isLastIncorrectWord()) {
      e.preventDefault();
      e.stopPropagation();
      handleSpaceSubmitAnswer(options?.useSpaceSubmitAnswer);
      return;
    }

    // Fix_Input 模式：当前单词已清空时，退格回到上一个错误单词
    if (mode.value === Mode.Fix_Input && e.code === "Backspace" && isEmptyOfCurrentEditWord()) {
      e.preventDefault();
      activePreviousIncorrectWord();
      inputChangedCallback?.(e);
      return;
    }

    // 其他情况下空格触发修复跳转
    if (mode.value !== Mode.Input && e.code === "Space") {
      e.preventDefault();
      fixIncorrectWord();
      inputChangedCallback?.(e);
      return;
    }

    inputChangedCallback?.(e);
  }

  function resetUserInputWords() {
    mode.value = Mode.Input;
    inputValue.value = "";
    userInputWords.splice(0, userInputWords.length);
  }

  function isFixInputMode() { return mode.value === Mode.Fix_Input; }
  function isFixMode() { return mode.value === Mode.Fix; }
  function findWordById(id: number) { return userInputWords.find((w) => w.id === id); }

  return {
    inputValue,
    userInputWords,
    submitAnswer,
    setInputValue,
    handleKeyboardInput,
    fixIncorrectWord,
    resetUserInputWords,
    isFixInputMode,
    isFixMode,
    findWordById,
    initialize,
  };
}
