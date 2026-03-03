'use client'

import { Comments as CommentsComponent } from 'pliny/comments'
import { useState } from 'react'
import siteMetadata from '@/data/siteMetadata'
import { useLanguage } from '@/contexts/LanguageProvider'

export default function Comments({ slug }: { slug: string }) {
  const [loadComments, setLoadComments] = useState(false)
  const { isChina } = useLanguage()

  // 检查 Giscus 配置是否完整
  const isConfigured =
    siteMetadata.comments?.provider === 'giscus' && siteMetadata.comments.giscusConfig?.repo

  if (!isConfigured) {
    return null
  }

  return (
    <div className="pt-8 pb-4">
      <h2 className="mb-6 text-2xl font-bold text-gray-900 dark:text-gray-100">
        {isChina ? '评论' : 'Comments'}
      </h2>
      {loadComments && siteMetadata.comments ? (
        <div className="relative">
          <CommentsComponent commentsConfig={siteMetadata.comments} slug={slug} />
        </div>
      ) : (
        <div className="rounded-lg bg-gray-100 p-6 text-center dark:bg-gray-800">
          <p className="mb-4 text-gray-700 dark:text-gray-300">
            {isChina
              ? '💬 本文评论由 Giscus 提供支持，使用 GitHub 账号登录即可发表评论。'
              : '💬 Comments are powered by Giscus. Sign in with your GitHub account to comment.'}
          </p>
          <button
            onClick={() => setLoadComments(true)}
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
