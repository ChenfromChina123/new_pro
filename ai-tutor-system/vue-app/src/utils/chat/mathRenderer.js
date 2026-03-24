/**
 * 数学公式渲染工具
 * 负责处理 LaTeX 公式的渲染和转换
 */
import katex from 'katex'
import 'katex/dist/katex.min.css'

/**
 * 清理标签中的中文和特殊字符
 * @param {string} str - 输入字符串
 * @returns {string} 清理后的字符串
 */
export const cleanTags = (str) => {
  if (!str) return ''
  return str
    .replace(/^[\s\u4e00-\u9fa5：:，,。.；;！!？?]*\d+\.\s*[\s\u4e00-\u9fa5：:，,。.；;！!？?]*/g, '')
    .replace(/[\s\u4e00-\u9fa5：:，,。.；;！!？?]*\)?\]?[\s\u4e00-\u9fa5：:，,。.；;！!？?]*$/g, '')
    .replace(/[\u4e00-\u9fa5：:，,。.；;！!？?]+/g, '')
    .trim()
}

/**
 * 渲染数学公式
 * @param {string} formula - LaTeX 公式
 * @param {boolean} displayMode - 是否为块级显示
 * @param {Array} placeholders - 占位符数组
 * @returns {string} 处理后的内容
 */
export const renderMathFormula = (content, placeholders = []) => {
  let processedContent = content

  const createPlaceholder = (formula, displayMode) => {
    try {
      const html = katex.renderToString(formula.trim(), {
        displayMode: displayMode,
        throwOnError: false,
        output: 'html'
      })
      const index = placeholders.length
      placeholders.push(html)
      return `MATH-PLACEHOLDER-${index}-END`
    } catch (error) {
      console.error('KaTeX渲染错误:', error)
      return formula
    }
  }

  // 处理 $$ ... $$
  processedContent = processedContent.replace(/\$\$([\s\S]+?)\$\$/g, (match, formula) => {
    const cleanedFormula = cleanTags(formula)
    return createPlaceholder(cleanedFormula, true)
  })

  // 处理 \[ ... \]
  processedContent = processedContent.replace(/\\\[([\s\S]+?)\\\]/g, (match, formula) => {
    const cleanedFormula = cleanTags(formula)
    return createPlaceholder(cleanedFormula, true)
  })

  // 处理 \( ... \)
  processedContent = processedContent.replace(/\\\(([\s\S]+?)\\\)/g, (match, formula) => {
    const cleanedFormula = cleanTags(formula)
    return createPlaceholder(cleanedFormula, false)
  })

  return processedContent
}

/**
 * 还原数学公式占位符
 * @param {string} content - HTML 内容
 * @param {Array} placeholders - 占位符数组
 * @returns {string} 还原后的 HTML
 */
export const restoreMathFormula = (content, placeholders) => {
  // 清理被错误包裹在代码块中的占位符
  let html = content.replace(/<pre[^>]*>\s*<code[^>]*>\s*(MATH-PLACEHOLDER-(\d+)-END)\s*<\/code>\s*<\/pre>/gi, '$1')
  html = html.replace(/<code[^>]*>\s*(MATH-PLACEHOLDER-(\d+)-END)\s*<\/code>/gi, '$1')

  // 还原占位符
  return html.replace(/MATH-PLACEHOLDER-(\d+)-END/g, (match, index) => {
    return placeholders[parseInt(index)] || match
  })
}

/**
 * 获取深度思考内容的字符长度
 * @param {string} content - 内容
 * @returns {number} 字符长度
 */
export const getReasoningLength = (content) => {
  if (!content) return 0
  const text = content
    .replace(/```[\s\S]*?```/g, '')
    .replace(/`[^`]+`/g, '')
    .replace(/#{1,6}\s+/g, '')
    .replace(/\*\*([^*]+)\*\*/g, '$1')
    .replace(/\*([^*]+)\*/g, '$1')
    .replace(/\[([^\]]+)\]\([^\)]+\)/g, '$1')
    .replace(/<[^>]+>/g, '')
    .replace(/\s+/g, ' ')
    .trim()
  return text.length
}

/**
 * 清理 null 字符序列
 * @param {string} content - 内容
 * @returns {string} 清理后的内容
 */
export const sanitizeNullRuns = (content) => {
  if (typeof content !== 'string') return content
  return content.replace(/(?:null){2,}/g, '')
}
