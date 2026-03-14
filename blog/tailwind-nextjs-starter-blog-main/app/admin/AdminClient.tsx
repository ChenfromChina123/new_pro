'use client'

import { useEffect, useMemo, useState } from 'react'

type PostItem = {
  slug: string
  title: string
  date: string
  draft: boolean
  summary: string
  extension: 'md' | 'mdx'
}

type EditablePost = {
  slug: string
  originalSlug: string
  title: string
  date: string
  draft: boolean
  summary: string
  tags: string
  authors: string
  layout: string
  body: string
  extension: 'md' | 'mdx'
}

const emptyPost: EditablePost = {
  slug: '',
  originalSlug: '',
  title: '',
  date: new Date().toISOString().slice(0, 10),
  draft: false,
  summary: '',
  tags: '',
  authors: 'default',
  layout: 'PostLayout',
  body: '',
  extension: 'mdx',
}

function toEditablePost(post: Record<string, unknown>): EditablePost {
  const tags = Array.isArray(post.tags)
    ? post.tags.filter((item): item is string => typeof item === 'string')
    : []
  const authors = Array.isArray(post.authors)
    ? post.authors.filter((item): item is string => typeof item === 'string')
    : []
  return {
    slug: typeof post.slug === 'string' ? post.slug : '',
    originalSlug: typeof post.slug === 'string' ? post.slug : '',
    title: typeof post.title === 'string' ? post.title : '',
    date: typeof post.date === 'string' ? post.date : new Date().toISOString().slice(0, 10),
    draft: Boolean(post.draft),
    summary: typeof post.summary === 'string' ? post.summary : '',
    tags: tags.join(', '),
    authors: authors.length ? authors.join(', ') : 'default',
    layout: typeof post.layout === 'string' ? post.layout : 'PostLayout',
    body: typeof post.body === 'string' ? post.body : '',
    extension: post.extension === 'md' ? 'md' : 'mdx',
  }
}

export default function AdminClient() {
  const [ready, setReady] = useState(false)
  const [loggedIn, setLoggedIn] = useState(false)
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [message, setMessage] = useState('')
  const [posts, setPosts] = useState<PostItem[]>([])
  const [loadingPosts, setLoadingPosts] = useState(false)
  const [saving, setSaving] = useState(false)
  const [editor, setEditor] = useState<EditablePost>(emptyPost)

  const canSave = useMemo(
    () => Boolean(editor.title.trim() && editor.date.trim() && editor.body.trim() && !saving),
    [editor, saving]
  )

  const loadSession = async () => {
    const response = await fetch('/api/admin/auth/session', { credentials: 'include' })
    const result = await response.json().catch(() => ({}))
    if (response.ok && result.loggedIn) {
      setLoggedIn(true)
      setMessage('')
      return true
    }
    setLoggedIn(false)
    if (result.message) setMessage(result.message)
    return false
  }

  const loadPosts = async () => {
    setLoadingPosts(true)
    const response = await fetch('/api/admin/posts', { credentials: 'include' })
    const result = await response.json().catch(() => ({}))
    if (response.ok) {
      setPosts(Array.isArray(result.posts) ? result.posts : [])
    } else {
      setMessage(result.message || '获取文章列表失败。')
    }
    setLoadingPosts(false)
  }

  useEffect(() => {
    let mounted = true
    ;(async () => {
      const ok = await loadSession()
      if (mounted && ok) {
        await loadPosts()
      }
      if (mounted) setReady(true)
    })()
    return () => {
      mounted = false
    }
  }, [])

  const handleLogin = async (event: React.FormEvent) => {
    event.preventDefault()
    setMessage('')
    const response = await fetch('/api/admin/auth/login', {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password }),
    })
    const result = await response.json().catch(() => ({}))
    if (!response.ok) {
      setMessage(result.message || '登录失败。')
      return
    }
    setLoggedIn(true)
    setPassword('')
    await loadPosts()
  }

  const handleLogout = async () => {
    await fetch('/api/admin/auth/logout', { method: 'POST', credentials: 'include' })
    setLoggedIn(false)
    setPosts([])
    setEditor(emptyPost)
    setMessage('已退出登录。')
  }

  const handleOpenPost = async (slug: string) => {
    setMessage('')
    const response = await fetch(`/api/admin/posts/${encodeURIComponent(slug)}`, {
      credentials: 'include',
    })
    const result = await response.json().catch(() => ({}))
    if (!response.ok) {
      setMessage(result.message || '加载文章失败。')
      return
    }
    setEditor(toEditablePost((result.post || {}) as Record<string, unknown>))
  }

  const handleCreateNew = () => {
    setEditor({
      ...emptyPost,
      date: new Date().toISOString().slice(0, 10),
    })
  }

  const handleSave = async () => {
    if (!canSave) return
    setSaving(true)
    setMessage('')
    const payload = {
      originalSlug: editor.originalSlug || undefined,
      slug: editor.slug || undefined,
      title: editor.title,
      date: editor.date,
      draft: editor.draft,
      summary: editor.summary,
      tags: editor.tags
        .split(',')
        .map((item) => item.trim())
        .filter(Boolean),
      authors: editor.authors
        .split(',')
        .map((item) => item.trim())
        .filter(Boolean),
      layout: editor.layout,
      body: editor.body,
      extension: editor.extension,
    }
    const response = await fetch('/api/admin/posts', {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    })
    const result = await response.json().catch(() => ({}))
    if (!response.ok) {
      setMessage(result.message || '保存失败。')
      setSaving(false)
      return
    }
    setEditor((prev) => ({ ...prev, originalSlug: result.post.slug, slug: result.post.slug }))
    setMessage('保存成功。')
    await loadPosts()
    setSaving(false)
  }

  const handleDelete = async () => {
    if (!editor.originalSlug) {
      setMessage('当前是新文章，尚未保存。')
      return
    }
    if (!window.confirm(`确认删除文章「${editor.originalSlug}」吗？`)) {
      return
    }
    const response = await fetch(`/api/admin/posts/${encodeURIComponent(editor.originalSlug)}`, {
      method: 'DELETE',
      credentials: 'include',
    })
    const result = await response.json().catch(() => ({}))
    if (!response.ok) {
      setMessage(result.message || '删除失败。')
      return
    }
    setMessage('删除成功。')
    setEditor(emptyPost)
    await loadPosts()
  }

  if (!ready) {
    return <main className="mx-auto max-w-6xl px-6 py-10">加载中...</main>
  }

  if (!loggedIn) {
    return (
      <main className="mx-auto flex min-h-[80vh] max-w-md items-center justify-center px-6">
        <form
          className="w-full space-y-4 rounded-xl border border-gray-200 p-6 shadow-sm"
          onSubmit={handleLogin}
        >
          <h1 className="text-xl font-semibold">博客后台登录</h1>
          <div className="space-y-2">
            <label className="block text-sm" htmlFor="admin-username">
              用户名
            </label>
            <input
              id="admin-username"
              className="w-full rounded border border-gray-300 px-3 py-2"
              value={username}
              onChange={(event) => setUsername(event.target.value)}
              required
            />
          </div>
          <div className="space-y-2">
            <label className="block text-sm" htmlFor="admin-password">
              密码
            </label>
            <input
              id="admin-password"
              type="password"
              className="w-full rounded border border-gray-300 px-3 py-2"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              required
            />
          </div>
          <button className="w-full rounded bg-blue-600 px-3 py-2 text-white">登录</button>
          {message ? <p className="text-sm text-red-600">{message}</p> : null}
        </form>
      </main>
    )
  }

  return (
    <main className="mx-auto grid max-w-7xl grid-cols-12 gap-6 px-6 py-8">
      <aside className="col-span-12 space-y-3 rounded-xl border border-gray-200 p-4 lg:col-span-4">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-semibold">文章列表</h2>
          <button className="rounded border px-2 py-1 text-sm" onClick={handleCreateNew}>
            新建
          </button>
        </div>
        <div className="space-y-2">
          {loadingPosts ? <p className="text-sm text-gray-500">加载中...</p> : null}
          {posts.map((post) => (
            <button
              key={`${post.slug}-${post.extension}`}
              className="block w-full rounded border border-gray-200 p-3 text-left hover:bg-gray-50"
              onClick={() => handleOpenPost(post.slug)}
            >
              <p className="text-sm font-medium">{post.title || post.slug}</p>
              <p className="text-xs text-gray-500">
                {post.slug}.{post.extension} · {post.date || '无日期'} {post.draft ? '· 草稿' : ''}
              </p>
            </button>
          ))}
        </div>
      </aside>
      <section className="col-span-12 space-y-3 rounded-xl border border-gray-200 p-4 lg:col-span-8">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-semibold">
            {editor.originalSlug ? `编辑：${editor.originalSlug}` : '新建文章'}
          </h2>
          <div className="flex gap-2">
            <button className="rounded border px-3 py-1 text-sm" onClick={handleLogout}>
              退出
            </button>
            <button
              className="rounded border border-red-300 px-3 py-1 text-sm text-red-600"
              onClick={handleDelete}
            >
              删除
            </button>
            <button
              className="rounded bg-blue-600 px-3 py-1 text-sm text-white disabled:opacity-50"
              onClick={handleSave}
              disabled={!canSave}
            >
              {saving ? '保存中...' : '保存'}
            </button>
          </div>
        </div>
        <div className="grid grid-cols-2 gap-3">
          <label className="space-y-1 text-sm">
            <span>Slug</span>
            <input
              className="w-full rounded border border-gray-300 px-3 py-2"
              value={editor.slug}
              onChange={(event) => setEditor((prev) => ({ ...prev, slug: event.target.value }))}
              placeholder="为空时自动根据标题生成"
            />
          </label>
          <label className="space-y-1 text-sm">
            <span>日期</span>
            <input
              className="w-full rounded border border-gray-300 px-3 py-2"
              value={editor.date}
              onChange={(event) => setEditor((prev) => ({ ...prev, date: event.target.value }))}
              placeholder="YYYY-MM-DD"
            />
          </label>
          <label className="col-span-2 space-y-1 text-sm">
            <span>标题</span>
            <input
              className="w-full rounded border border-gray-300 px-3 py-2"
              value={editor.title}
              onChange={(event) => setEditor((prev) => ({ ...prev, title: event.target.value }))}
            />
          </label>
          <label className="col-span-2 space-y-1 text-sm">
            <span>摘要</span>
            <input
              className="w-full rounded border border-gray-300 px-3 py-2"
              value={editor.summary}
              onChange={(event) => setEditor((prev) => ({ ...prev, summary: event.target.value }))}
            />
          </label>
          <label className="space-y-1 text-sm">
            <span>标签（逗号分隔）</span>
            <input
              className="w-full rounded border border-gray-300 px-3 py-2"
              value={editor.tags}
              onChange={(event) => setEditor((prev) => ({ ...prev, tags: event.target.value }))}
            />
          </label>
          <label className="space-y-1 text-sm">
            <span>作者（逗号分隔）</span>
            <input
              className="w-full rounded border border-gray-300 px-3 py-2"
              value={editor.authors}
              onChange={(event) => setEditor((prev) => ({ ...prev, authors: event.target.value }))}
            />
          </label>
          <label className="space-y-1 text-sm">
            <span>布局</span>
            <select
              className="w-full rounded border border-gray-300 px-3 py-2"
              value={editor.layout}
              onChange={(event) => setEditor((prev) => ({ ...prev, layout: event.target.value }))}
            >
              <option value="PostLayout">PostLayout</option>
              <option value="PostSimple">PostSimple</option>
              <option value="PostBanner">PostBanner</option>
            </select>
          </label>
          <label className="space-y-1 text-sm">
            <span>扩展名</span>
            <select
              className="w-full rounded border border-gray-300 px-3 py-2"
              value={editor.extension}
              onChange={(event) =>
                setEditor((prev) => ({
                  ...prev,
                  extension: event.target.value === 'md' ? 'md' : 'mdx',
                }))
              }
            >
              <option value="mdx">mdx</option>
              <option value="md">md</option>
            </select>
          </label>
          <label className="flex items-center gap-2 text-sm">
            <input
              type="checkbox"
              checked={editor.draft}
              onChange={(event) => setEditor((prev) => ({ ...prev, draft: event.target.checked }))}
            />
            <span>草稿</span>
          </label>
        </div>
        <label className="space-y-1 text-sm">
          <span>正文（Markdown / MDX）</span>
          <textarea
            className="min-h-[380px] w-full rounded border border-gray-300 px-3 py-2 font-mono text-sm"
            value={editor.body}
            onChange={(event) => setEditor((prev) => ({ ...prev, body: event.target.value }))}
          />
        </label>
        {message ? <p className="text-sm text-blue-600">{message}</p> : null}
      </section>
    </main>
  )
}
