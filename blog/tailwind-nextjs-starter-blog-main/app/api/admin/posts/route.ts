import { NextRequest, NextResponse } from 'next/server'
import { compile } from '@mdx-js/mdx'
import { readSessionToken, verifySessionToken } from '../_lib/auth'
import { getPostList, savePost } from '../_lib/posts'

async function ensureAuthorized(request: NextRequest) {
  const token = readSessionToken(request)
  const session = verifySessionToken(token)
  return session
}

export async function GET(request: NextRequest) {
  const session = await ensureAuthorized(request)
  if (!session) {
    return NextResponse.json({ message: '未登录或登录已过期。' }, { status: 401 })
  }
  const posts = await getPostList()
  return NextResponse.json({ posts })
}

export async function POST(request: NextRequest) {
  const session = await ensureAuthorized(request)
  if (!session) {
    return NextResponse.json({ message: '未登录或登录已过期。' }, { status: 401 })
  }
  const payload = (await request.json().catch(() => null)) as Record<string, unknown> | null
  if (!payload) {
    return NextResponse.json({ message: '请求体格式错误。' }, { status: 400 })
  }
  try {
    const saved = await savePost(payload)
    await compile(saved.body || '', { format: 'mdx' })
    return NextResponse.json({ success: true, post: saved })
  } catch (error) {
    const message = error instanceof Error ? error.message : '保存失败'
    return NextResponse.json({ message }, { status: 400 })
  }
}
