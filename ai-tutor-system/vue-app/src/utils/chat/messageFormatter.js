/**
 * 消息格式化工具
 * 负责处理 Markdown 渲染和代码高亮
 */
import DOMPurify from 'dompurify'
import { marked } from 'marked'
import hljs from 'highlight.js'
import 'highlight.js/styles/github-dark.css'
import { renderMathFormula, restoreMathFormula, sanitizeNullRuns, cleanTags } from './mathRenderer'

// 自定义 marked 渲染器
const renderer = new marked.Renderer()
const originalCode = renderer.code

renderer.code = function(code, language, escaped) {
  const originalResult = originalCode.call(this, code, language, escaped)
  return originalResult.replace('<pre', '<pre style="position: relative">')
    .replace('</pre>', '<button class="copy-button" onclick="copyCodeBlock(this)"><i class="far fa-copy"></i><span>复制</span></button></pre>')
}

// 配置 marked
marked.setOptions({
  highlight: function(code, lang) {
    const language = hljs.getLanguage(lang) ? lang : 'plaintext'
    return hljs.highlight(code, { language }).value
  },
  langPrefix: 'hljs language-',
  breaks: true,
  gfm: true,
  renderer: renderer
})

/**
 * 全局复制代码函数
 */
window.copyCodeBlock = (element) => {
  const code = element.previousElementSibling.textContent
  const button = element
  const icon = button.querySelector('i')
  const text = button.querySelector('span')

  navigator.clipboard.writeText(code)
    .then(() => {
      const originalIconClass = icon ? icon.className : ''
      const originalText = text ? text.textContent : button.textContent

      if (icon) icon.className = 'fas fa-check'
      if (text) text.textContent = '已复制!'
      else if (!icon) button.textContent = '已复制!'

      button.classList.add('copied')

      setTimeout(() => {
        if (icon) icon.className = originalIconClass
        if (text) text.textContent = originalText
        else if (!icon) button.textContent = originalText
        button.classList.remove('copied')
      }, 2000)
    })
    .catch(err => {
      console.error('复制失败:', err)
    })
}

/**
 * 格式化消息内容
 * @param {string} content - 原始内容
 * @returns {string} 格式化后的 HTML
 */
export const formatMessage = (content) => {
  try {
    if (!content) return ''

    let processedContent = content
    processedContent = processedContent.replace(/`{4,}/g, '```')

    // 保护代码块
    const codeBlocks = []
    processedContent = processedContent.replace(/(```[\s\S]*?```|`[^`]+`)/g, (match) => {
      const index = codeBlocks.length
      codeBlocks.push(match)
      return `CODE_BLOCK_PLACEHOLDER_${index}_END`
    })

    // 处理数学公式
    const mathPlaceholders = []
    processedContent = renderMathFormula(processedContent, mathPlaceholders)

    // 还原代码块
    codeBlocks.forEach((block, i) => {
      processedContent = processedContent.replace(`CODE_BLOCK_PLACEHOLDER_${i}_END`, () => block)
    })

    // 解析 Markdown
    let html = marked.parse(processedContent.trim())

    // 还原数学公式
    html = restoreMathFormula(html, mathPlaceholders)

    // 后置清理
    html = html.replace(/\[\s*<p>\s*/g, '<p>')
    html = html.replace(/\s*<\/p>\s*\]/g, '</p>')
    html = html.replace(/<br\s*\/?>\]\s*<\/p>/g, '</p>')
    html = html.replace(/^\s*\[/g, '')
    html = html.replace(/\]\s*$/g, '')

    // 安全过滤
    return DOMPurify.sanitize(html, {
      ADD_ATTR: ['onclick', 'style', 'class'],
      ADD_TAGS: ['button', 'i', 'pre', 'code', 'span']
    })
  } catch (error) {
    console.error('消息格式化错误:', error)
    return content
  }
}

/**
 * 缓存的消息格式化函数
 */
export const formatMessageCached = (() => {
  const cache = new WeakMap()
  return (message, field) => {
    if (!message || !field) return ''
    const raw = sanitizeNullRuns(message[field] || '')
    let entry = cache.get(message)
    if (!entry) {
      entry = {}
      cache.set(message, entry)
    }
    const prev = entry[field]
    if (prev && prev.raw === raw) return prev.html
    const html = formatMessage(raw)
    entry[field] = { raw, html }
    return html
  }
})()

/**
 * 格式化深度思考内容（带缓存）
 */
export const formatReasoningCached = (() => {
  const cache = new WeakMap()
  return (message) => {
    if (!message || !message.reasoning_content) return ''
    const raw = sanitizeNullRuns(message.reasoning_content)
    let cached = cache.get(message)
    if (cached && cached.raw === raw) return cached.html
    const html = formatMessage(raw)
    cache.set(message, { raw, html })
    return html
  }
})()

/**
 * 格式化时间
 * @param {number|string} timestamp - 时间戳
 * @returns {string} 格式化后的时间字符串
 */
export const formatTime = (timestamp) => {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  return date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  })
}

/**
 * 格式化短时间
 * @param {number|string} timestamp - 时间戳
 * @returns {string} 格式化后的时间字符串
 */
export const formatTimeShort = (timestamp) => {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  return date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  })
}
