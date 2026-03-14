import { NextResponse } from 'next/server'

const oauthAuthorizeUrl = 'https://github.com/login/oauth/authorize'
const oauthStateCookie = 'decap_oauth_state'

function buildOrigin(request: Request) {
  const configuredOrigin = process.env.DECAP_PUBLIC_ORIGIN?.trim()
  if (configuredOrigin) {
    return configuredOrigin.replace(/\/+$/, '')
  }
  const forwardedProto = request.headers.get('x-forwarded-proto')?.split(',')[0]?.trim()
  const forwardedHost = request.headers.get('x-forwarded-host')?.split(',')[0]?.trim()
  const host = forwardedHost || request.headers.get('host') || ''
  const protocol = forwardedProto || new URL(request.url).protocol.replace(':', '')
  return `${protocol}://${host}`
}

export async function GET(request: Request) {
  const clientId = process.env.DECAP_GITHUB_CLIENT_ID?.trim()

  if (!clientId) {
    return NextResponse.json({ message: '缺少 DECAP_GITHUB_CLIENT_ID 环境变量。' }, { status: 500 })
  }

  const origin = buildOrigin(request)
  const state = crypto.randomUUID().replace(/-/g, '')
  const redirectUri = `${origin}/api/admin/oauth/callback`

  const authorizeUrl = new URL(oauthAuthorizeUrl)
  authorizeUrl.searchParams.set('client_id', clientId)
  authorizeUrl.searchParams.set('redirect_uri', redirectUri)
  authorizeUrl.searchParams.set('scope', 'repo')
  authorizeUrl.searchParams.set('state', state)

  const response = NextResponse.redirect(authorizeUrl)
  response.cookies.set(oauthStateCookie, state, {
    httpOnly: true,
    sameSite: 'lax',
    secure: origin.startsWith('https://'),
    path: '/api/admin/oauth',
    maxAge: 60 * 10,
  })

  return response
}
