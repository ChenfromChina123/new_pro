import { NextRequest, NextResponse } from 'next/server'
import {
  canUseAdminAuth,
  createSessionToken,
  getSessionCookieName,
  getSessionTtlSeconds,
  validateAdminCredential,
} from '../../_lib/auth'

type LoginPayload = {
  username?: string
  password?: string
}

export async function POST(request: NextRequest) {
  if (!canUseAdminAuth()) {
    return NextResponse.json(
      {
        success: false,
        message: '后台登录未配置，请设置 BLOG_ADMIN_USERNAME/PASSWORD/SESSION_SECRET。',
      },
      { status: 500 }
    )
  }

  const payload = (await request.json().catch(() => ({}))) as LoginPayload
  const username = typeof payload.username === 'string' ? payload.username.trim() : ''
  const password = typeof payload.password === 'string' ? payload.password : ''

  if (!validateAdminCredential(username, password)) {
    return NextResponse.json({ success: false, message: '用户名或密码错误。' }, { status: 401 })
  }

  const token = createSessionToken(username)
  const isHttps =
    request.headers.get('x-forwarded-proto')?.includes('https') ||
    request.nextUrl.protocol === 'https:'
  const response = NextResponse.json({ success: true, username })
  response.cookies.set(getSessionCookieName(), token, {
    httpOnly: true,
    secure: isHttps,
    sameSite: 'lax',
    path: '/',
    maxAge: getSessionTtlSeconds(),
  })
  return response
}
