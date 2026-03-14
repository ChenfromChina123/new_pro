'use client'

import { useState, useCallback, useEffect, useRef } from 'react'
import siteMetadata from '@/data/siteMetadata'
import { useLanguage } from '@/contexts/LanguageProvider'

export default function Comments({ slug }: { slug: string }) {
  const [loadComments, setLoadComments] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const commentsRef = useRef<HTMLDivElement>(null)
  const { isChina } = useLanguage()

  const handleMessage = useCallback((event: MessageEvent) => {
    if (
      event.origin === 'https://giscus.app' &&
      event.data &&
      typeof event.data === 'object' &&
      'giscus' in event.data
    ) {
      const giscusData = event.data.giscus as { discussion?: { url?: string }; error?: string }
      if (giscusData.error) {
        setError(giscusData.error)
      }
    }
  }, [])

  const isGiscus = siteMetadata.comments?.provider === 'giscus'
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const giscusConfig = isGiscus ? (siteMetadata.comments as any).giscusConfig : null
  const missingKeys = [
    !giscusConfig?.repositoryId ? 'NEXT_PUBLIC_GISCUS_REPO_ID' : null,
    !giscusConfig?.categoryId ? 'NEXT_PUBLIC_GISCUS_CATEGORY_ID' : null,
  ].filter(Boolean) as string[]
  const isConfigured =
    isGiscus && giscusConfig?.repo && giscusConfig?.repositoryId && giscusConfig?.categoryId

  useEffect(() => {
    window.addEventListener('message', handleMessage)
    return () => {
      window.removeEventListener('message', handleMessage)
    }
  }, [handleMessage])

  useEffect(() => {
    if (!loadComments || !isConfigured || !commentsRef.current) return
    const container = commentsRef.current
    container.innerHTML = ''
    const script = document.createElement('script')
    script.src = 'https://giscus.app/client.js'
    script.async = true
    script.crossOrigin = 'anonymous'
    script.setAttribute('data-repo', giscusConfig.repo)
    script.setAttribute('data-repo-id', giscusConfig.repositoryId)
    script.setAttribute('data-category', giscusConfig.category || 'General')
    script.setAttribute('data-category-id', giscusConfig.categoryId)
    script.setAttribute('data-mapping', giscusConfig.mapping || 'pathname')
    script.setAttribute('data-reactions-enabled', giscusConfig.reactions || '1')
    script.setAttribute('data-emit-metadata', giscusConfig.metadata || '0')
    script.setAttribute('data-theme', giscusConfig.theme || 'light')
    script.setAttribute('data-lang', giscusConfig.lang || 'zh-CN')
    script.onerror = () => {
      setError(isChina ? '评论加载失败，请检查网络或稍后重试。' : 'Failed to load comments.')
    }
    container.appendChild(script)
  }, [giscusConfig, isChina, isConfigured, loadComments, slug])

  if (!isConfigured) {
    return (
      <div className="pt-8 pb-4">
        <h2 className="mb-6 text-2xl font-bold text-gray-900 dark:text-gray-100">
          {isChina ? '评论' : 'Comments'}
        </h2>
        <div className="rounded-lg bg-yellow-50 p-6 text-center dark:bg-yellow-900/20">
          <p className="text-gray-700 dark:text-gray-300">
            {isChina
              ? `📝 评论功能尚未完整配置。缺少：${missingKeys.join('、') || 'Giscus 参数'}。`
              : `📝 Comments are not fully configured. Missing: ${missingKeys.join(', ') || 'giscus config'}.`}
          </p>
          <a
            href="https://giscus.app/zh-CN"
            target="_blank"
            rel="noreferrer"
            className="text-primary-600 hover:text-primary-500 mt-3 inline-block text-sm font-medium underline"
          >
            {isChina ? '打开 Giscus 配置页' : 'Open Giscus setup page'}
          </a>
        </div>
      </div>
    )
  }

  return (
    <div className="pt-8 pb-4">
      <h2 className="mb-6 text-2xl font-bold text-gray-900 dark:text-gray-100">
        {isChina ? '评论' : 'Comments'}
      </h2>
      {loadComments ? (
        <div className="relative min-h-[200px]">
          {error && (
            <div className="mb-4 rounded-lg bg-red-50 p-4 text-red-700 dark:bg-red-900/20 dark:text-red-400">
              <p className="font-medium">{isChina ? '加载评论失败' : 'Failed to load comments'}</p>
              <p className="mt-1 text-sm opacity-80">{error}</p>
              <button
                onClick={() => {
                  setError(null)
                  setLoadComments(false)
                }}
                className="mt-2 text-sm font-medium underline hover:no-underline"
              >
                {isChina ? '重试' : 'Retry'}
              </button>
            </div>
          )}
          <div ref={commentsRef} className="min-h-[300px] w-full" />
        </div>
      ) : (
        <div className="rounded-lg bg-gray-100 p-6 text-center dark:bg-gray-800">
          <p className="mb-4 text-gray-700 dark:text-gray-300">
            {isChina
              ? '💬 本文评论由 Giscus 提供支持，使用 GitHub 账号登录即可发表评论。'
              : '💬 Comments are powered by Giscus. Sign in with your GitHub account to comment.'}
          </p>
          <button
            onClick={() => {
              setError(null)
              setLoadComments(true)
            }}
            className="bg-primary-500 hover:bg-primary-600 rounded-md px-6 py-2 font-medium text-white transition-colors"
          >
            {isChina ? '加载评论' : 'Load Comments'}
          </button>
          <div className="mt-4 text-sm text-gray-500 dark:text-gray-400">
            {isChina ? '需要 GitHub 账号' : 'GitHub account required'}
          </div>
        </div>
      )}
    </div>
  )
}
