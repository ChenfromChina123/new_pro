import { NextRequest, NextResponse } from 'next/server'
import { readSessionToken, verifySessionToken } from '../../_lib/auth'
import { deletePost, getPostBySlug } from '../../_lib/posts'

async function ensureAuthorized(request: NextRequest) {
  const token = readSessionToken(request)
  return verifySessionToken(token)
}

export async function GET(
  request: NextRequest,
  context: {
    params: Promise<{ slug: string }>
  }
) {
  const session = await ensureAuthorized(request)
  if (!session) {
    return NextResponse.json({ message: '未登录或登录已过期。' }, { status: 401 })
  }
  const { slug } = await context.params
  const post = await getPostBySlug(slug)
  if (!post) {
    return NextResponse.json({ message: '文章不存在。' }, { status: 404 })
  }
  return NextResponse.json({ post })
}

export async function DELETE(
  request: NextRequest,
  context: {
    params: Promise<{ slug: string }>
  }
) {
  const session = await ensureAuthorized(request)
  if (!session) {
    return NextResponse.json({ message: '未登录或登录已过期。' }, { status: 401 })
  }
  const { slug } = await context.params
  const removed = await deletePost(slug)
  if (!removed) {
    return NextResponse.json({ message: '文章不存在。' }, { status: 404 })
  }
  return NextResponse.json({ success: true })
}
