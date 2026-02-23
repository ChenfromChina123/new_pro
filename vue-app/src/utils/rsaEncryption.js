/**
 * RSA加密工具类
 * 用于在前端加密敏感信息（如密码）
 */

// 导入jsencrypt库来进行RSA加密
import JSEncrypt from 'jsencrypt'

class RsaEncryption {
  constructor() {
    this.publicKey = null
    this.initialized = false
  }

  /**
   * 初始化RSA加密，获取服务器公钥
   */
  async initialize() {
    try {
      // 从后端获取公钥
      const response = await fetch('/api/rsa/public-key')
      const data = await response.json()
      
      if (data.code === 200) {
        this.publicKey = data.data
        this.initialized = true
        console.log('RSA加密初始化成功')
        return true
      } else {
        console.error('获取RSA公钥失败:', data.message)
        return false
      }
    } catch (error) {
      console.error('RSA初始化失败:', error)
      return false
    }
  }

  /**
   * 加密文本
   * @param {string} text - 要加密的文本
   * @returns {string|null} - 加密后的文本，失败返回null
   */
  encrypt(text) {
    if (!this.initialized || !this.publicKey) {
      console.error('RSA未初始化或公钥为空')
      return null
    }

    try {
      const encrypt = new JSEncrypt()
      encrypt.setPublicKey(this.publicKey)
      return encrypt.encrypt(text)
    } catch (error) {
      console.error('RSA加密失败:', error)
      return null
    }
  }

  /**
   * 检查是否已初始化
   */
  isInitialized() {
    return this.initialized
  }
}

// 创建全局实例
const rsaEncryption = new RsaEncryption()

export default rsaEncryption