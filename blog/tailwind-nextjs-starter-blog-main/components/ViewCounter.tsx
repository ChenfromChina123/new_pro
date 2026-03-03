/**
 * 文章阅读量统计组件
 * 使用 localStorage 进行本地计数（简单实现）
 * 生产环境建议使用服务端统计
 */

'use client'

import { useEffect, useState } from 'react'

interface ViewCounterProps {
  slug: string
}

export default function ViewCounter({ slug }: ViewCounterProps) {
  const [views, setViews] = useState(0)
  const [loaded, setLoaded] = useState(false)

  useEffect(() => {
    // 从 localStorage 获取当前文章的阅读量
    const viewKey = `views:${slug}`
    const storedViews = localStorage.getItem(viewKey)

    if (storedViews) {
      // 如果已有记录，增加阅读量
      const newViews = parseInt(storedViews) + 1
      localStorage.setItem(viewKey, newViews.toString())
      setViews(newViews)
    } else {
      // 如果是首次访问，初始化为 1
      localStorage.setItem(viewKey, '1')
      setViews(1)
    }

    setLoaded(true)
  }, [slug])

  if (!loaded) return null

  return <span className="text-sm text-gray-500 dark:text-gray-400">👁️ {views} 次阅读</span>
}
