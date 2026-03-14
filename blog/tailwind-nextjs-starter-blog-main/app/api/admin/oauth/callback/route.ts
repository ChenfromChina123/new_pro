import { NextResponse } from 'next/server'

const oauthTokenUrl = 'https://github.com/login/oauth/access_token'
const oauthStateCookie = 'decap_oauth_state'

function buildOrigin(request: Request) {
  const forwardedProto = request.headers.get('x-forwarded-proto')
  const forwardedHost = request.headers.get('x-forwarded-host')
  const host = forwardedHost || request.headers.get('host') || ''
  const protocol = forwardedProto || new URL(request.url).protocol.replace(':', '')
  return `${protocol}://${host}`
}

function buildHtml(origin: string, messageType: 'success' | 'error', payload: string) {
  const message = `authorization:github:${messageType}:${payload}`
  return `<!doctype html><html><body><script>
  (function() {
    if (window.opener && window.opener !== window) {
      window.opener.postMessage(${JSON.stringify(message)}, ${JSON.stringify(origin)});
    }
    window.close();
  })();
  </script></body></html>`
}

export async function GET(request: Request) {
  const url = new URL(request.url)
  const code = url.searchParams.get('code')?.trim()
  const state = url.searchParams.get('state')?.trim()
  const oauthError = url.searchParams.get('error')?.trim()
  const origin = buildOrigin(request)
  const cookieStore = request.headers.get('cookie') || ''
  const cookieStateMatch = cookieStore.match(/(?:^|;\s*)decap_oauth_state=([^;]+)/)
  const cookieState = cookieStateMatch ? decodeURIComponent(cookieStateMatch[1]) : ''

  if (oauthError) {
    return new NextResponse(buildHtml(origin, 'error', oauthError), {
      status: 200,
      headers: { 'content-type': 'text/html; charset=utf-8' },
    })
  }

  if (!code || !state || !cookieState || state !== cookieState) {
    return new NextResponse(buildHtml(origin, 'error', 'invalid_oauth_state'), {
      status: 200,
      headers: { 'content-type': 'text/html; charset=utf-8' },
    })
  }

  const clientId = process.env.DECAP_GITHUB_CLIENT_ID?.trim()
  const clientSecret = process.env.DECAP_GITHUB_CLIENT_SECRET?.trim()

  if (!clientId || !clientSecret) {
    return new NextResponse(buildHtml(origin, 'error', 'missing_oauth_env'), {
      status: 200,
      headers: { 'content-type': 'text/html; charset=utf-8' },
    })
  }

  const redirectUri = `${origin}/api/admin/oauth/callback`
  const tokenResponse = await fetch(oauthTokenUrl, {
    method: 'POST',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      client_id: clientId,
      client_secret: clientSecret,
      code,
      redirect_uri: redirectUri,
      state,
    }),
    cache: 'no-store',
  })

  const tokenData = (await tokenResponse.json().catch(() => null)) as {
    access_token?: string
    error?: string
  } | null

  const accessToken = tokenData?.access_token?.trim()
  if (!accessToken) {
    const error = tokenData?.error?.trim() || 'token_exchange_failed'
    return new NextResponse(buildHtml(origin, 'error', error), {
      status: 200,
      headers: { 'content-type': 'text/html; charset=utf-8' },
    })
  }

  const payload = JSON.stringify({ token: accessToken, provider: 'github' })
  const response = new NextResponse(buildHtml(origin, 'success', payload), {
    status: 200,
    headers: { 'content-type': 'text/html; charset=utf-8' },
  })
  response.cookies.set(oauthStateCookie, '', {
    maxAge: 0,
    path: '/api/admin/oauth',
  })
  return response
}
