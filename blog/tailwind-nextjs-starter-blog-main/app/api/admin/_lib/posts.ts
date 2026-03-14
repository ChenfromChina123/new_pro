import path from 'path'
import { promises as fs } from 'fs'
import matter from 'gray-matter'
import { slug as toSlug } from 'github-slugger'

type EditablePost = {
  slug: string
  title: string
  date: string
  draft: boolean
  summary: string
  tags: string[]
  authors: string[]
  layout: string
  body: string
  extension: 'md' | 'mdx'
}

type SavePayload = Partial<EditablePost> & { slug?: string; originalSlug?: string }

const blogDir = path.join(process.cwd(), 'data', 'blog')
const allowedLayouts = new Set(['PostLayout', 'PostSimple', 'PostBanner'])

function normalizeString(value: unknown) {
  return typeof value === 'string' ? value.trim() : ''
}

function normalizeStringArray(value: unknown) {
  if (!Array.isArray(value)) return []
  return value.map((item) => normalizeString(item)).filter(Boolean)
}

function normalizeSlug(value: string) {
  const normalized = toSlug(value || '')
  return normalized
    .replace(/[^a-z0-9-]/g, '')
    .replace(/-+/g, '-')
    .replace(/^-|-$/g, '')
}

function resolveFilePath(slug: string) {
  return {
    mdx: path.join(blogDir, `${slug}.mdx`),
    md: path.join(blogDir, `${slug}.md`),
  }
}

async function exists(filePath: string) {
  try {
    await fs.access(filePath)
    return true
  } catch {
    return false
  }
}

export async function getPostList() {
  const names = await fs.readdir(blogDir)
  const entries = await Promise.all(
    names
      .filter((name) => name.endsWith('.md') || name.endsWith('.mdx'))
      .map(async (name) => {
        const filePath = path.join(blogDir, name)
        const raw = await fs.readFile(filePath, 'utf8')
        const parsed = matter(raw)
        const stat = await fs.stat(filePath)
        const extension = name.endsWith('.mdx') ? 'mdx' : 'md'
        return {
          slug: name.replace(/\.(md|mdx)$/i, ''),
          title: normalizeString(parsed.data.title) || name,
          date: normalizeString(parsed.data.date),
          draft: Boolean(parsed.data.draft),
          summary: normalizeString(parsed.data.summary),
          extension,
          updatedAt: stat.mtimeMs,
        }
      })
  )
  return entries.sort((a, b) => b.updatedAt - a.updatedAt)
}

export async function getPostBySlug(slug: string): Promise<EditablePost | null> {
  const normalized = normalizeSlug(slug)
  if (!normalized) return null
  const files = resolveFilePath(normalized)
  const filePath = (await exists(files.mdx)) ? files.mdx : (await exists(files.md)) ? files.md : ''
  if (!filePath) return null
  const raw = await fs.readFile(filePath, 'utf8')
  const parsed = matter(raw)
  const extension = filePath.endsWith('.mdx') ? 'mdx' : 'md'
  return {
    slug: normalized,
    title: normalizeString(parsed.data.title),
    date: normalizeString(parsed.data.date),
    draft: Boolean(parsed.data.draft),
    summary: normalizeString(parsed.data.summary),
    tags: normalizeStringArray(parsed.data.tags),
    authors: normalizeStringArray(parsed.data.authors).length
      ? normalizeStringArray(parsed.data.authors)
      : ['default'],
    layout: normalizeString(parsed.data.layout) || 'PostLayout',
    body: typeof parsed.content === 'string' ? parsed.content : '',
    extension,
  }
}

function buildDocument(payload: EditablePost) {
  const lines = [
    '---',
    `title: ${payload.title}`,
    `date: ${payload.date}`,
    `draft: ${payload.draft ? 'true' : 'false'}`,
    `summary: ${payload.summary || '文章摘要'}`,
    `tags: [${payload.tags.map((item) => `'${item}'`).join(', ')}]`,
    `authors: [${payload.authors.map((item) => `'${item}'`).join(', ')}]`,
    `layout: ${payload.layout}`,
    '---',
    '',
    payload.body,
  ]
  return lines.join('\n')
}

export async function savePost(input: SavePayload) {
  const title = normalizeString(input.title)
  const date = normalizeString(input.date)
  const summary = normalizeString(input.summary)
  const tags = normalizeStringArray(input.tags)
  const authors = normalizeStringArray(input.authors).length
    ? normalizeStringArray(input.authors)
    : ['default']
  const layout = normalizeString(input.layout) || 'PostLayout'
  const body = typeof input.body === 'string' ? input.body : ''
  const extension = input.extension === 'md' ? 'md' : 'mdx'
  const originalSlug = normalizeSlug(normalizeString(input.originalSlug))
  const slugSource = normalizeString(input.slug) || title
  const slug = normalizeSlug(slugSource)

  if (!title) throw new Error('标题不能为空')
  if (!date) throw new Error('发布日期不能为空')
  if (!slug) throw new Error('slug 无效')
  if (!body.trim()) throw new Error('正文不能为空')
  if (!allowedLayouts.has(layout)) throw new Error('layout 不合法')

  const payload: EditablePost = {
    slug,
    title,
    date,
    draft: Boolean(input.draft),
    summary,
    tags,
    authors,
    layout,
    body,
    extension,
  }

  const targetFile = path.join(blogDir, `${slug}.${extension}`)
  const originalFiles = originalSlug ? resolveFilePath(originalSlug) : null
  const targetExists = await exists(targetFile)
  if (!originalSlug && targetExists) {
    throw new Error('slug 已存在，请修改后重试')
  }

  await fs.mkdir(blogDir, { recursive: true })
  await fs.writeFile(targetFile, buildDocument(payload), 'utf8')

  if (originalFiles && originalSlug !== slug) {
    if (await exists(originalFiles.md)) await fs.unlink(originalFiles.md)
    if (await exists(originalFiles.mdx)) await fs.unlink(originalFiles.mdx)
  } else if (originalFiles) {
    const staleFile = extension === 'md' ? originalFiles.mdx : originalFiles.md
    if (await exists(staleFile)) await fs.unlink(staleFile)
  }

  return payload
}

export async function deletePost(slug: string) {
  const normalized = normalizeSlug(slug)
  if (!normalized) return false
  const files = resolveFilePath(normalized)
  let removed = false
  if (await exists(files.md)) {
    await fs.unlink(files.md)
    removed = true
  }
  if (await exists(files.mdx)) {
    await fs.unlink(files.mdx)
    removed = true
  }
  return removed
}
