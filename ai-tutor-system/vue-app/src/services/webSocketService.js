/**
 * WebSocket 服务单例
 * 统一管理 WebSocket 连接，避免重复创建
 */
class WebSocketService {
  constructor() {
    this.connections = new Map()
    this.reconnectAttempts = new Map()
    this.maxReconnectAttempts = 5
    this.reconnectDelay = 3000
  }

  /**
   * 获取或创建 WebSocket 连接
   * @param {string} id - 连接标识
   * @param {string} url - WebSocket URL
   * @param {object} options - 配置选项
   * @returns {WebSocket} WebSocket 实例
   */
  getConnection(id, url, options = {}) {
    if (this.connections.has(id)) {
      const existing = this.connections.get(id)
      if (existing.ws.readyState === WebSocket.OPEN || existing.ws.readyState === WebSocket.CONNECTING) {
        return existing.ws
      }
    }

    const ws = this.createConnection(id, url, options)
    return ws
  }

  /**
   * 创建新的 WebSocket 连接
   */
  createConnection(id, url, options = {}) {
    const ws = new WebSocket(url)
    const connectionInfo = {
      ws,
      url,
      options,
      onMessage: options.onMessage || null,
      onOpen: options.onOpen || null,
      onClose: options.onClose || null,
      onError: options.onError || null
    }

    ws.onopen = (event) => {
      this.reconnectAttempts.set(id, 0)
      if (connectionInfo.onOpen) {
        connectionInfo.onOpen(event)
      }
    }

    ws.onmessage = (event) => {
      if (connectionInfo.onMessage) {
        connectionInfo.onMessage(event)
      }
    }

    ws.onclose = (event) => {
      if (connectionInfo.onClose) {
        connectionInfo.onClose(event)
      }
      if (!event.wasClean && this.reconnectAttempts.get(id) < this.maxReconnectAttempts) {
        this.scheduleReconnect(id)
      }
    }

    ws.onerror = (error) => {
      if (connectionInfo.onError) {
        connectionInfo.onError(error)
      }
    }

    this.connections.set(id, connectionInfo)
    return ws
  }

  /**
   * 安排重连
   */
  scheduleReconnect(id) {
    const attempts = this.reconnectAttempts.get(id) || 0
    this.reconnectAttempts.set(id, attempts + 1)

    setTimeout(() => {
      const connectionInfo = this.connections.get(id)
      if (connectionInfo) {
        this.createConnection(id, connectionInfo.url, connectionInfo.options)
      }
    }, this.reconnectDelay * (attempts + 1))
  }

  /**
   * 发送消息
   * @param {string} id - 连接标识
   * @param {string|object} message - 消息内容
   */
  send(id, message) {
    const connectionInfo = this.connections.get(id)
    if (connectionInfo && connectionInfo.ws.readyState === WebSocket.OPEN) {
      const data = typeof message === 'string' ? message : JSON.stringify(message)
      connectionInfo.ws.send(data)
      return true
    }
    return false
  }

  /**
   * 关闭指定连接
   * @param {string} id - 连接标识
   */
  closeConnection(id) {
    const connectionInfo = this.connections.get(id)
    if (connectionInfo) {
      connectionInfo.ws.close()
      this.connections.delete(id)
      this.reconnectAttempts.delete(id)
    }
  }

  /**
   * 关闭所有连接
   */
  closeAll() {
    this.connections.forEach((_, id) => {
      this.closeConnection(id)
    })
  }

  /**
   * 检查连接状态
   * @param {string} id - 连接标识
   * @returns {string} 连接状态
   */
  getState(id) {
    const connectionInfo = this.connections.get(id)
    if (!connectionInfo) return 'DISCONNECTED'

    const states = ['CONNECTING', 'OPEN', 'CLOSING', 'CLOSED']
    return states[connectionInfo.ws.readyState] || 'UNKNOWN'
  }

  /**
   * 检查是否已连接
   * @param {string} id - 连接标识
   * @returns {boolean}
   */
  isConnected(id) {
    return this.getState(id) === 'OPEN'
  }
}

const webSocketService = new WebSocketService()
export default webSocketService
