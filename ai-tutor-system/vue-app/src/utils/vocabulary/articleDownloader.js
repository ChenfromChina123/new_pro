/**
 * 文章下载工具
 * 负责将 AI 生成的文章导出为不同格式
 */

/**
 * 下载文章为 HTML 格式
 * @param {Object} article - 文章对象
 * @param {string} article.title - 文章标题
 * @param {string} article.content - 文章内容
 */
export const downloadAsHtml = (article) => {
  const htmlContent = `
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>${escapeHtml(article.title || 'AI生成文章')}</title>
  <style>
    body {
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
      max-width: 800px;
      margin: 0 auto;
      padding: 40px 20px;
      line-height: 1.8;
      color: #333;
    }
    h1 {
      text-align: center;
      color: #1a1a1a;
      margin-bottom: 30px;
    }
    .highlight-word {
      background: linear-gradient(120deg, #a8edea 0%, #fed6e3 100%);
      padding: 2px 6px;
      border-radius: 4px;
      font-weight: 500;
    }
    .meta {
      text-align: center;
      color: #666;
      margin-bottom: 30px;
      font-size: 14px;
    }
    p {
      text-indent: 2em;
      margin-bottom: 16px;
    }
  </style>
</head>
<body>
  <h1>${escapeHtml(article.title || 'AI生成文章')}</h1>
  <div class="meta">生成时间: ${new Date().toLocaleString('zh-CN')}</div>
  <div class="content">${article.content || ''}</div>
</body>
</html>
  `.trim()

  downloadFile(htmlContent, `${article.title || 'article'}.html`, 'text/html;charset=utf-8')
}

/**
 * 下载文章为纯文本格式
 * @param {Object} article - 文章对象
 */
export const downloadAsTxt = (article) => {
  const plainContent = stripHtml(article.content || '')
  const textContent = `${article.title || 'AI生成文章'}\n${'='.repeat(50)}\n\n生成时间: ${new Date().toLocaleString('zh-CN')}\n\n${plainContent}`

  downloadFile(textContent, `${article.title || 'article'}.txt`, 'text/plain;charset=utf-8')
}

/**
 * 下载文章为 Markdown 格式
 * @param {Object} article - 文章对象
 */
export const downloadAsMarkdown = (article) => {
  const markdownContent = `# ${article.title || 'AI生成文章'}\n\n> 生成时间: ${new Date().toLocaleString('zh-CN')}\n\n${htmlToMarkdown(article.content || '')}`

  downloadFile(markdownContent, `${article.title || 'article'}.md`, 'text/markdown;charset=utf-8')
}

/**
 * 触发文件下载
 * @param {string} content - 文件内容
 * @param {string} filename - 文件名
 * @param {string} mimeType - MIME 类型
 */
const downloadFile = (content, filename, mimeType) => {
  const blob = new Blob([content], { type: mimeType })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

/**
 * 转义 HTML 特殊字符
 * @param {string} str - 输入字符串
 * @returns {string} 转义后的字符串
 */
const escapeHtml = (str) => {
  if (!str) return ''
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;')
}

/**
 * 移除 HTML 标签
 * @param {string} html - HTML 内容
 * @returns {string} 纯文本
 */
const stripHtml = (html) => {
  if (!html) return ''
  return html
    .replace(/<br\s*\/?>/gi, '\n')
    .replace(/<\/p>/gi, '\n\n')
    .replace(/<[^>]+>/g, '')
    .replace(/&nbsp;/g, ' ')
    .replace(/&amp;/g, '&')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .trim()
}

/**
 * 将 HTML 转换为 Markdown
 * @param {string} html - HTML 内容
 * @returns {string} Markdown 内容
 */
const htmlToMarkdown = (html) => {
  if (!html) return ''
  return html
    .replace(/<h1[^>]*>(.*?)<\/h1>/gi, '# $1\n\n')
    .replace(/<h2[^>]*>(.*?)<\/h2>/gi, '## $1\n\n')
    .replace(/<h3[^>]*>(.*?)<\/h3>/gi, '### $1\n\n')
    .replace(/<p[^>]*>(.*?)<\/p>/gi, '$1\n\n')
    .replace(/<strong[^>]*>(.*?)<\/strong>/gi, '**$1**')
    .replace(/<em[^>]*>(.*?)<\/em>/gi, '*$1*')
    .replace(/<br\s*\/?>/gi, '\n')
    .replace(/<[^>]+>/g, '')
    .replace(/&nbsp;/g, ' ')
    .trim()
}

export default {
  downloadAsHtml,
  downloadAsTxt,
  downloadAsMarkdown
}
