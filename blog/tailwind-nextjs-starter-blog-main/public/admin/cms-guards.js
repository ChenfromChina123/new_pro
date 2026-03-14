;(function () {
  const getText = (value) => (typeof value === 'string' ? value : '')
  const getList = (value) => {
    if (!value || typeof value.toJS !== 'function') return []
    const result = value.toJS()
    return Array.isArray(result) ? result : []
  }

  const validateBlogEntry = async ({ entry, collection }) => {
    const collectionName =
      collection && typeof collection.get === 'function' ? collection.get('name') : ''
    if (collectionName !== 'blog' && collectionName !== 'blog_md') return

    const data = entry && typeof entry.get === 'function' ? entry.get('data') : null
    const payload = {
      title: getText(data && data.get ? data.get('title') : ''),
      date: getText(data && data.get ? data.get('date') : ''),
      tags: getList(data && data.get ? data.get('tags') : []),
      draft: Boolean(data && data.get ? data.get('draft') : false),
      summary: getText(data && data.get ? data.get('summary') : ''),
      lastmod: getText(data && data.get ? data.get('lastmod') : ''),
      authors: getList(data && data.get ? data.get('authors') : []),
      layout: getText(data && data.get ? data.get('layout') : ''),
      body: getText(data && data.get ? data.get('body') : ''),
    }

    const response = await fetch('/api/admin/validate-mdx', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    })

    const result = await response.json().catch(() => ({}))
    if (!response.ok || result.valid === false) {
      const message = result.message || '发布已拒绝：MDX 解析失败。'
      throw new Error(message)
    }
  }

  const waitAndRegister = () => {
    if (!window.CMS || typeof window.CMS.registerEventListener !== 'function') {
      setTimeout(waitAndRegister, 100)
      return
    }
    window.CMS.registerEventListener({ name: 'prePublish', handler: validateBlogEntry })
  }

  waitAndRegister()
})()
