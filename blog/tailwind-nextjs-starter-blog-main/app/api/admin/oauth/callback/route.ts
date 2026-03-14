import { NextResponse } from 'next/server'

const oauthTokenUrl = 'https://github.com/login/oauth/access_token'
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

function buildHtml(origin: string, messageType: 'success' | 'error', payload: string) {
  const message = `authorization:github:${messageType}:${payload}`
  const shouldClose = messageType === 'success'
  const title = shouldClose ? '授权完成' : '授权失败'
  const detail = shouldClose
    ? '正在返回后台页面，请稍候...'
    : `请关闭此窗口后重试，错误信息：${payload}`
  const structuredMessage = shouldClose
    ? `authorization:github:success:${JSON.stringify({ token: payload, provider: 'github' })}`
    : ''
  const plainMessage = shouldClose ? `authorization:github:success:${payload}` : ''
  const messageBundle = shouldClose ? [structuredMessage, plainMessage, message] : [message]
  return `<!doctype html><html><body><script>
  (function() {
    var messages = ${JSON.stringify(messageBundle)};
    var send = function() {
      if (!window.opener || window.opener === window) {
        return;
      }
      for (var i = 0; i < messages.length; i += 1) {
        window.opener.postMessage(messages[i], ${JSON.stringify(origin)});
        window.opener.postMessage(messages[i], '*');
      }
    };
    if (window.opener && window.opener !== window) {
      send();
      setTimeout(send, 300);
      setTimeout(send, 900);
    }
    var messageNode = document.createElement('div');
    messageNode.style.cssText = 'font-family:-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif;padding:24px;line-height:1.6;color:#111827;';
    messageNode.innerHTML = '<h2 style="margin:0 0 8px;">${title}</h2><p style="margin:0;">${detail}</p>';
    document.body.appendChild(messageNode);
    if (${shouldClose}) {
      setTimeout(function() { window.close(); }, 1800);
    }
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

  const response = new NextResponse(buildHtml(origin, 'success', accessToken), {
    status: 200,
    headers: { 'content-type': 'text/html; charset=utf-8' },
  })
  response.cookies.set(oauthStateCookie, '', {
    maxAge: 0,
    path: '/api/admin/oauth',
  })
  return response
}
