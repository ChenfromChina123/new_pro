/**
 * 语言上下文 Provider
 * 根据 IP 检测结果自动切换语言
 */

'use client'

import React, { createContext, useContext } from 'react'
import { useGeoIP } from '@/hooks/useGeoIP'
import { zhCN, enUS } from '@/data/i18n'

type Translations = typeof zhCN | typeof enUS

interface LanguageContextType {
  translations: Translations
  locale: string
  dir: string
  loading: boolean
  isChina?: boolean
}

const LanguageContext = createContext<LanguageContextType | null>(null)

export function LanguageProvider({ children }: { children: React.ReactNode }) {
  const { isChina, loading } = useGeoIP()

  // 根据 IP 选择语言包
  const translations = (isChina ? zhCN : enUS) as Translations
  const locale = isChina ? 'zh-CN' : 'en-US'
  const dir = 'ltr'

  if (loading) {
    // 加载期间显示简单加载状态
    return (
      <LanguageContext.Provider value={{ translations, locale, dir, loading: true }}>
        {children}
      </LanguageContext.Provider>
    )
  }

  return (
    <LanguageContext.Provider value={{ translations, locale, dir, loading: false, isChina }}>
      {children}
    </LanguageContext.Provider>
  )
}

export function useLanguage() {
  const context = useContext(LanguageContext)
  if (!context) {
    throw new Error('useLanguage must be used within LanguageProvider')
  }
  return context
}
