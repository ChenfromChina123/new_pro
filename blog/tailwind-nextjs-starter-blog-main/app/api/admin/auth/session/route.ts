import { NextRequest, NextResponse } from 'next/server'
import { canUseAdminAuth, readSessionToken, verifySessionToken } from '../../_lib/auth'

export async function GET(request: NextRequest) {
  if (!canUseAdminAuth()) {
    return NextResponse.json(
      { loggedIn: false, message: '后台登录未配置，请先设置环境变量。' },
      { status: 500 }
    )
  }
  const token = readSessionToken(request)
  const session = verifySessionToken(token)
  if (!session) {
    return NextResponse.json({ loggedIn: false }, { status: 200 })
  }
  return NextResponse.json({ loggedIn: true, username: session.username }, { status: 200 })
}
