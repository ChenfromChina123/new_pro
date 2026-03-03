/**
 * IP 地理位置检测 Hook
 * 根据访问者 IP 判断是否为中国大陆用户，自动切换语言
 */

import { useEffect, useState } from 'react'

export function useGeoIP() {
  const [isChina, setIsChina] = useState(false)
  const [loading, setLoading] = useState(true)
  const [ipInfo, setIpInfo] = useState(null)

  useEffect(() => {
    /**
     * 检测是否为中国 IP
     * 使用多个免费 IP 地理位置 API 作为备选
     */
    const checkIP = async () => {
      try {
        // 方案 1: 使用 ipapi.co API
        const response = await fetch('https://ipapi.co/json/')
        if (!response.ok) throw new Error('API 1 failed')
        
        const data = await response.json()
        setIpInfo(data)
        
        // 判断是否为中国大陆
        const isCN = data.country_code === 'CN' || data.country_name === 'China'
        setIsChina(isCN)
      } catch (error1) {
        try {
          // 方案 2: 使用 ip-api.com (无需 API key)
          const response2 = await fetch('http://ip-api.com/json/?lang=zh-CN')
          if (!response2.ok) throw new Error('API 2 failed')
          
          const data2 = await response2.json()
          setIpInfo(data2)
          
          const isCN = data2.countryCode === 'CN' || data2.country === '中国'
          setIsChina(isCN)
        } catch (error2) {
          // 方案 3: 使用 ipwhois.app
          try {
            const response3 = await fetch('https://ipwhois.app/json/?lang=zh-CN')
            if (!response3.ok) throw new Error('API 3 failed')
            
            const data3 = await response3.json()
            setIpInfo(data3)
            
            const isCN = data3.country_code === 'CN'
            setIsChina(isCN)
          } catch (error3) {
            // 所有 API 都失败，默认根据时区判断
            const timeZone = Intl.DateTimeFormat().resolvedOptions().timeZone
            const isChinaTZ = timeZone === 'Asia/Shanghai' || timeZone === 'Asia/Chongqing'
            setIsChina(isChinaTZ)
            console.log('IP API 全部失败，使用时区判断:', isChinaTZ)
          }
        }
      } finally {
        setLoading(false)
      }
    }

    checkIP()
  }, [])

  return { isChina, loading, ipInfo }
}
