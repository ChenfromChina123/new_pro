import { NextResponse } from 'next/server'

type ValidatePayload = {
  title?: string
  date?: string
  tags?: string[]
  draft?: boolean
  summary?: string
  lastmod?: string
  authors?: string[]
  layout?: string
  body?: string
}

const allowedLayouts = new Set(['PostLayout', 'PostSimple', 'PostBanner'])

function normalizeString(value: unknown) {
  return typeof value === 'string' ? value.trim() : ''
}

function normalizeStringArray(value: unknown) {
  if (!Array.isArray(value)) return []
  return value.map((item) => normalizeString(item)).filter(Boolean)
}

function buildMDXDocument(payload: ValidatePayload) {
  const title = normalizeString(payload.title)
  const date = normalizeString(payload.date)
  const summary = normalizeString(payload.summary)
  const lastmod = normalizeString(payload.lastmod)
  const layout = normalizeString(payload.layout) || 'PostLayout'
  const tags = normalizeStringArray(payload.tags)
  const authors = normalizeStringArray(payload.authors)
  const body = typeof payload.body === 'string' ? payload.body : ''
  const draft = Boolean(payload.draft)

  const errors: string[] = []
  if (!title) errors.push('缺少标题（title）。')
  if (!date) errors.push('缺少发布日期（date）。')
  if (!body.trim()) errors.push('正文为空，请填写正文内容。')
  if (layout && !allowedLayouts.has(layout)) {
    errors.push(`布局无效：${layout}，可选值为 PostLayout / PostSimple / PostBanner。`)
  }
  if (!authors.length) errors.push('作者不能为空，请至少填写一个作者标识。')
  if (errors.length) {
    return { ok: false as const, errors }
  }

  const frontmatterLines = [
    '---',
    `title: ${title}`,
    `date: ${date}`,
    `tags: [${tags.map((tag) => `'${tag}'`).join(', ')}]`,
    `draft: ${draft ? 'true' : 'false'}`,
    `summary: ${summary || '文章摘要'}`,
    `authors: [${authors.map((author) => `'${author}'`).join(', ')}]`,
    `layout: ${layout}`,
    ...(lastmod ? [`lastmod: ${lastmod}`] : []),
    '---',
    '',
    body,
  ]

  return { ok: true as const, document: frontmatterLines.join('\n') }
}

function parseErrorMessage(error: unknown) {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return '未知 MDX 解析错误。'
}

export async function POST(request: Request) {
  try {
    const payload = (await request.json()) as ValidatePayload
    const built = buildMDXDocument(payload)
    if (!built.ok) {
      return NextResponse.json(
        { valid: false, message: `发布已拒绝：${built.errors.join('；')}` },
        { status: 400 }
      )
    }

    const { compile } = await import('@mdx-js/mdx')
    await compile(built.document, { format: 'mdx' })

    return NextResponse.json({ valid: true, message: 'MDX 解析通过，可发布。' })
  } catch (error) {
    const message = parseErrorMessage(error)
    return NextResponse.json(
      { valid: false, message: `发布已拒绝：MDX 解析失败。${message}` },
      { status: 400 }
    )
  }
}
