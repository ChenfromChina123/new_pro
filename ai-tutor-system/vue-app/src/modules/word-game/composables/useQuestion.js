import { nextTick, reactive, ref, watch } from 'vue'

const separator = ' '

export function isWord(content) {
  return /[a-zA-Z0-9]/.test(content)
}

export function useInput({
  source,
  setInputCursorPosition,
  getInputCursorPosition,
  inputChangedCallback
}) {
  const mode = ref('input')
  const inputValue = ref('')
  const userInputWords = reactive([])
  let currentEditWord = null
  let stopWatch = null

  function initialize() {
    if (stopWatch) stopWatch()
    mode.value = 'input'
    inputValue.value = ''
    userInputWords.length = 0
    // 清空所有响应式数据
    userInputWords.splice(0, userInputWords.length)
    setupUserInputWords()
    updateActiveWord(getInputCursorPosition())
  }

  function setInputValue(val) {
    inputValue.value = val
    resetAllWordUserInput()
    inputSyncUserInputWords()
    updateActiveWord(val ? getInputCursorPosition() : 0)
  }

  function createWord(word, id) {
    return reactive({
      text: word,
      isActive: false,
      userInput: '',
      incorrect: false,
      start: 0,
      end: 0,
      position: 0,
      id
    })
  }

  function setupUserInputWords() {
    stopWatch = watch(
      source,
      (english) => {
        if (!english) return
        // 只在英语内容变化时重新初始化
        const words = english.split(separator)
        let inputWordIndex = 0
        words.forEach((text, index) => {
          if (isWord(text)) {
            const word = createWord(text, index)
            userInputWords.push(word)
            if (inputWordIndex === 0) {
              word.isActive = true
            }
            inputWordIndex += 1
          }
        })
        inputSyncUserInputWords()
        updateActiveWord(getInputCursorPosition())
      },
      { immediate: true }
    )
  }

  function userInputWordsSyncInput() {
    inputValue.value = userInputWords.map(({ userInput }) => userInput).join(separator)
  }

  function inputSyncUserInputWords() {
    let position = 0
    inputValue.value.split(separator).forEach((input, index) => {
      if (userInputWords[index] !== undefined) {
        userInputWords[index].userInput = input
        userInputWords[index].start = position
        userInputWords[index].end = position + input.length
        position += input.length + 1
      }
    })
  }

  function resetAllWordUserInput() {
    userInputWords.forEach(word => {
      word.userInput = ''
    })
  }

  function resetAllWordActive() {
    userInputWords.forEach(word => {
      word.isActive = false
    })
  }

  function updateActiveWord(position) {
    resetAllWordActive()
    for (let i = 0; i < userInputWords.length; i += 1) {
      const word = userInputWords[i]
      if (position >= word.start && position <= word.end) {
        word.isActive = true
        break
      }
    }
  }

  function checkWordCorrect() {
    return userInputWords.every(w => !w.incorrect)
  }

  function formatLastWordUserInput(word, index) {
    const isLastWord = userInputWords.length - 1 === index
    if (isLastWord && word.userInput.endsWith('.')) {
      word.userInput = word.userInput.slice(0, -1)
    }
  }

  function formatInputText(word) {
    return String(word || '').toLowerCase().replace(/[‘’“”"]/g, "'")
  }

  function markIncorrectWord() {
    userInputWords.forEach((word, index) => {
      formatLastWordUserInput(word, index)
      const formatted = formatInputText(word.userInput)
      word.incorrect = formatted !== word.text.toLowerCase()
    })
  }

  function submitAnswer(correctCallback, wrongCallback) {
    if (mode.value === 'fix') return
    resetAllWordActive()
    markIncorrectWord()
    if (checkWordCorrect()) {
      mode.value = 'input'
      if (correctCallback) correctCallback()
      inputValue.value = ''
    } else {
      mode.value = 'fix'
      if (wrongCallback) wrongCallback()
    }
  }

  function getFirstIncorrectWord() {
    return userInputWords.find(w => w.incorrect)
  }

  async function clearNextIncorrectWord(word) {
    word.userInput = ''
    currentEditWord = word
    userInputWordsSyncInput()
    await nextTick()
    setInputCursorPosition(word.start)
    updateActiveWord(word.start)
  }

  async function fixFirstIncorrectWord() {
    if (mode.value === 'fix') {
      mode.value = 'fix-input'
      await clearNextIncorrectWord(getFirstIncorrectWord())
    }
  }

  function findNextIncorrectWord() {
    if (!currentEditWord) return null
    const wordIndex = userInputWords.findIndex(w => w.id === currentEditWord.id)
    for (let i = wordIndex + 1; i < userInputWords.length; i += 1) {
      if (userInputWords[i].incorrect) return userInputWords[i]
    }
    return null
  }

  async function fixNextIncorrectWord() {
    if (mode.value === 'fix-input') {
      const next = findNextIncorrectWord()
      if (next) {
        await clearNextIncorrectWord(next)
      }
    }
  }

  async function fixIncorrectWord() {
    if (mode.value === 'fix') {
      await fixFirstIncorrectWord()
    } else if (mode.value === 'fix-input') {
      await fixNextIncorrectWord()
    }
  }

  function isEmptyOfCurrentEditWord() {
    return (currentEditWord?.userInput?.length || 0) <= 0
  }

  function findPreviousIncorrectWord() {
    if (!currentEditWord) return null
    const wordIndex = userInputWords.findIndex(w => w.id === currentEditWord.id)
    for (let i = wordIndex - 1; i >= 0; i -= 1) {
      if (userInputWords[i].incorrect) return userInputWords[i]
    }
    return null
  }

  async function activePreviousIncorrectWord() {
    const prev = findPreviousIncorrectWord()
    if (prev) {
      currentEditWord = prev
      await nextTick()
      updateActiveWord(prev.end)
      setInputCursorPosition(prev.end)
    }
  }

  function lastWordIsActive() {
    return userInputWords[userInputWords.length - 1]?.isActive || false
  }

  function isLastIncorrectWord() {
    return !findNextIncorrectWord()
  }

  function handleSpaceSubmitAnswer(opt) {
    if (opt?.enable) {
      submitAnswer(opt.rightCallback, opt.errorCallback)
    }
  }

  function handleKeyboardInput(e, options) {
    if (['ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight'].includes(e.code)) {
      e.preventDefault()
      return
    }

    if (mode.value !== 'fix' && e.code === 'Space' && lastWordIsActive()) {
      e.preventDefault()
      e.stopPropagation()
      handleSpaceSubmitAnswer(options?.useSpaceSubmitAnswer)
      return
    }

    if (mode.value === 'fix') {
      if (e.code === 'Space' || e.code === 'Backspace') {
        e.preventDefault()
      }
      fixFirstIncorrectWord()
      if (inputChangedCallback) inputChangedCallback(e)
      return
    }

    if (mode.value === 'fix-input' && e.code === 'Space' && isLastIncorrectWord()) {
      e.preventDefault()
      e.stopPropagation()
      handleSpaceSubmitAnswer(options?.useSpaceSubmitAnswer)
      return
    }

    if (mode.value === 'fix-input' && e.code === 'Backspace' && isEmptyOfCurrentEditWord()) {
      e.preventDefault()
      activePreviousIncorrectWord()
      if (inputChangedCallback) inputChangedCallback(e)
      return
    }

    if (mode.value !== 'input' && e.code === 'Space') {
      e.preventDefault()
      fixIncorrectWord()
      if (inputChangedCallback) inputChangedCallback(e)
      return
    }

    if (inputChangedCallback) inputChangedCallback(e)
  }

  function resetUserInputWords() {
    mode.value = 'input'
    inputValue.value = ''
    userInputWords.splice(0, userInputWords.length)
  }

  function isFixMode() {
    return mode.value === 'fix'
  }

  function findWordById(id) {
    return userInputWords.find(w => w.id === id)
  }

  return {
    inputValue,
    userInputWords,
    submitAnswer,
    setInputValue,
    handleKeyboardInput,
    fixIncorrectWord,
    resetUserInputWords,
    isFixMode,
    findWordById,
    initialize
  }
}
