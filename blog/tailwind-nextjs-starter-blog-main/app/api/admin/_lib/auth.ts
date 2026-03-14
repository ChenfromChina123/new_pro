import crypto from 'crypto'
import { NextRequest } from 'next/server'

const cookieName = 'blog_admin_session'
const sessionTtlSeconds = 60 * 60 * 8

type SessionPayload = {
  u: string
  exp: number
}

function base64UrlEncode(value: string) {
  return Buffer.from(value, 'utf8').toString('base64url')
}

function base64UrlDecode(value: string) {
  return Buffer.from(value, 'base64url').toString('utf8')
}

function sign(input: string, secret: string) {
  return crypto.createHmac('sha256', secret).update(input).digest('base64url')
}

function safeEqual(a: string, b: string) {
  const left = Buffer.from(a)
  const right = Buffer.from(b)
  if (left.length !== right.length) {
    return false
  }
  return crypto.timingSafeEqual(left, right)
}

function getAdminConfig() {
  const username = process.env.BLOG_ADMIN_USERNAME?.trim()
  const password = process.env.BLOG_ADMIN_PASSWORD?.trim()
  const secret = process.env.BLOG_ADMIN_SESSION_SECRET?.trim()
  if (!username || !password || !secret) {
    return null
  }
  return { username, password, secret }
}

export function canUseAdminAuth() {
  return Boolean(getAdminConfig())
}

export function validateAdminCredential(username: string, password: string) {
  const config = getAdminConfig()
  if (!config) {
    return false
  }
  return safeEqual(username, config.username) && safeEqual(password, config.password)
}

export function createSessionToken(username: string) {
  const config = getAdminConfig()
  if (!config) {
    throw new Error('missing_admin_env')
  }
  const payload: SessionPayload = {
    u: username,
    exp: Date.now() + sessionTtlSeconds * 1000,
  }
  const payloadText = JSON.stringify(payload)
  const payloadEncoded = base64UrlEncode(payloadText)
  const signature = sign(payloadEncoded, config.secret)
  return `${payloadEncoded}.${signature}`
}

export function readSessionToken(request: NextRequest) {
  return request.cookies.get(cookieName)?.value || ''
}

export function verifySessionToken(token: string) {
  const config = getAdminConfig()
  if (!config || !token) {
    return null
  }
  const parts = token.split('.')
  if (parts.length !== 2) {
    return null
  }
  const payloadEncoded = parts[0]
  const signature = parts[1]
  const expected = sign(payloadEncoded, config.secret)
  if (!safeEqual(signature, expected)) {
    return null
  }
  try {
    const payload = JSON.parse(base64UrlDecode(payloadEncoded)) as SessionPayload
    if (!payload?.u || !payload?.exp || payload.exp < Date.now()) {
      return null
    }
    return { username: payload.u }
  } catch {
    return null
  }
}

export function getSessionCookieName() {
  return cookieName
}

export function getSessionTtlSeconds() {
  return sessionTtlSeconds
}
