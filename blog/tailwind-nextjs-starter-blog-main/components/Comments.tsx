'use client'

import { useState, useCallback } from 'react'
import siteMetadata from '@/data/siteMetadata'
import { useLanguage } from '@/contexts/LanguageProvider'

export default function Comments({ slug }: { slug: string }) {
  const [loadComments, setLoadComments] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const { isChina } = useLanguage()

  // 处理 Giscus 消息事件
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

  // 检查是否为 Giscus 配置
  const isGiscus = siteMetadata.comments?.provider === 'giscus'
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const giscusConfig = isGiscus ? (siteMetadata.comments as any).giscusConfig : null
  const isConfigured = isGiscus && giscusConfig?.repo

  if (!isConfigured) {
    return (
      <div className="pt-8 pb-4">
        <h2 className="mb-6 text-2xl font-bold text-gray-900 dark:text-gray-100">
          {isChina ? '评论' : 'Comments'}
        </h2>
        <div className="rounded-lg bg-yellow-50 p-6 text-center dark:bg-yellow-900/20">
          <p className="text-gray-700 dark:text-gray-300">
            {isChina
              ? '📝 评论功能尚未配置。管理员需要在 GitHub 仓库启用 Discussions 并配置 Giscus。'
              : '📝 Comments are not configured yet. Admin needs to enable Discussions on GitHub repo and configure Giscus.'}
          </p>
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
          <iframe
            src={`https://giscus.app/client?repo=${encodeURIComponent(
              giscusConfig.repo
            )}&repoId=${giscusConfig.repositoryId || ''}&category=${
              giscusConfig.category || ''
            }&categoryId=${giscusConfig.categoryId || ''}&mapping=${
              giscusConfig.mapping || 'pathname'
            }&reactions=${giscusConfig.reactions || '1'}&theme=${giscusConfig.theme || 'light'}`}
            className="min-h-[300px] w-full"
            onLoad={() => {
              window.addEventListener('message', handleMessage)
            }}
            title="Giscus Comments"
          />
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
