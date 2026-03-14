import { NextRequest, NextResponse } from 'next/server'
import { getSessionCookieName } from '../../_lib/auth'

export async function POST(request: NextRequest) {
  const isHttps =
    request.headers.get('x-forwarded-proto')?.includes('https') ||
    request.nextUrl.protocol === 'https:'
  const response = NextResponse.json({ success: true })
  response.cookies.set(getSessionCookieName(), '', {
    httpOnly: true,
    secure: isHttps,
    sameSite: 'lax',
    path: '/',
    maxAge: 0,
  })
  return response
}
