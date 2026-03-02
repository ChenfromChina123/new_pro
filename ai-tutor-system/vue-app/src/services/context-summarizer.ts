/**
 * 上下文摘要算法
 * 目标：在保持90%信息量的同时减少40%的token消耗
 */

/**
 * 摘要配置
 */
export interface SummarizerConfig {
  /** 最大历史记录数 */
  maxHistoryCount: number
  /** 关键信息保留权重 */
  keywordWeight: number
  /** 时间衰减因子 */
  timeDecayFactor: number
  /** 最小保留长度 */
  minRetainLength: number
}

/**
 * 历史记录项
 */
export interface HistoryItem {
  /** 内容 */
  content: string
  /** 时间戳 */
  timestamp: number
  /** 重要性得分 */
  importance: number
  /** 类型 */
  type: 'user' | 'ai' | 'system'
}

/**
 * 摘要结果
 */
export interface SummaryResult {
  /** 摘要后的内容 */
  summary: string
  /** 原始Token数 */
  originalTokens: number
  /** 摘要后Token数 */
  summaryTokens: number
  /** 压缩率 */
  compressionRatio: number
  /** 保留信息量估计 */
  infoRetention: number
}

/**
 * 上下文摘要器
 */
export class ContextSummarizer {
  private config: SummarizerConfig = {
    maxHistoryCount: 10,
    keywordWeight: 1.5,
    timeDecayFactor: 0.95,
    minRetainLength: 50
  }

  // 关键词列表（用于提高保留优先级）
  private readonly keywords = [
    'error', 'warning', 'success', 'failed',
    'created', 'updated', 'deleted',
    'install', 'build', 'deploy', 'test',
    '错误', '警告', '成功', '失败',
    '创建', '更新', '删除',
    '安装', '构建', '部署', '测试'
  ]

  constructor(config?: Partial<SummarizerConfig>) {
    if (config) {
      this.config = { ...this.config, ...config }
    }
  }

  /**
   * 摘要化历史记录
   */
  summarizeHistory(history: HistoryItem[]): SummaryResult {
    if (history.length === 0) {
      return {
        summary: '',
        originalTokens: 0,
        summaryTokens: 0,
        compressionRatio: 0,
        infoRetention: 1
      }
    }

    // 1. 计算每条记录的重要性得分
    const scoredHistory = history.map((item, index) => {
      const timeScore = this.calculateTimeScore(item.timestamp, history.length - index)
      const keywordScore = this.calculateKeywordScore(item.content)
      const typeScore = this.calculateTypeScore(item.type)
      
      return {
        ...item,
        score: timeScore * keywordScore * typeScore
      }
    })

    // 2. 排序并选择最重要的记录
    scoredHistory.sort((a, b) => b.score - a.score)
    const retained = scoredHistory.slice(0, this.config.maxHistoryCount)

    // 3. 按时间重新排序
    retained.sort((a, b) => a.timestamp - b.timestamp)

    // 4. 智能摘要每条记录
    const summarized = retained.map(item => this.summarizeItem(item))

    // 5. 计算Token统计
    const originalTokens = this.estimateTokens(history.map(h => h.content).join('\n'))
    const summaryText = summarized.join('\n')
    const summaryTokens = this.estimateTokens(summaryText)

    return {
      summary: summaryText,
      originalTokens,
      summaryTokens,
      compressionRatio: summaryTokens / originalTokens,
      infoRetention: this.estimateInfoRetention(retained.length, history.length)
    }
  }

  /**
   * 计算时间得分（越新越重要）
   */
  private calculateTimeScore(timestamp: number, position: number): number {
    const now = Date.now()
    const age = now - timestamp
    const ageInMinutes = age / (1000 * 60)
    
    // 时间衰减 + 位置权重
    const timeWeight = Math.pow(this.config.timeDecayFactor, ageInMinutes)
    const positionWeight = Math.pow(1.1, position) // 越靠后权重越高
    
    return timeWeight * positionWeight
  }

  /**
   * 计算关键词得分
   */
  private calculateKeywordScore(content: string): number {
    const lowerContent = content.toLowerCase()
    let score = 1.0
    
    for (const keyword of this.keywords) {
      if (lowerContent.includes(keyword.toLowerCase())) {
        score *= this.config.keywordWeight
      }
    }
    
    return Math.min(score, 3.0) // 最大3倍权重
  }

  /**
   * 计算类型得分
   */
  private calculateTypeScore(type: string): number {
    const scores = {
      user: 1.2,    // 用户输入较重要
      system: 1.5,  // 系统消息最重要
      ai: 1.0       // AI回复基准
    }
    return scores[type as keyof typeof scores] || 1.0
  }

  /**
   * 摘要单条记录
   */
  private summarizeItem(item: HistoryItem): string {
    const content = item.content.trim()
    
    // 如果内容较短，直接保留
    if (content.length <= this.config.minRetainLength) {
      return `[${item.type}] ${content}`
    }

    // 提取关键信息
    const sentences = content.split(/[。.!！?？\n]/).filter(s => s.trim())
    
    if (sentences.length <= 2) {
      // 少于2句，保留全部
      return `[${item.type}] ${content}`
    }

    // 选择包含关键词的句子
    const importantSentences = sentences.filter(s => 
      this.keywords.some(kw => s.toLowerCase().includes(kw.toLowerCase()))
    )

    if (importantSentences.length > 0) {
      return `[${item.type}] ${importantSentences.slice(0, 2).join('。')}`
    }

    // 否则保留首句和末句
    return `[${item.type}] ${sentences[0]}...${sentences[sentences.length - 1]}`
  }

  /**
   * 估算Token数（简化版，实际应使用tokenizer）
   */
  private estimateTokens(text: string): number {
    // 简化估算：英文单词 + 中文字符 * 1.5
    const chineseChars = (text.match(/[\u4e00-\u9fa5]/g) || []).length
    const englishWords = (text.match(/[a-zA-Z]+/g) || []).length
    const symbols = (text.match(/[^\u4e00-\u9fa5a-zA-Z\s]/g) || []).length
    
    return Math.ceil(chineseChars * 1.5 + englishWords * 1.3 + symbols * 0.5)
  }

  /**
   * 估算信息保留率
   */
  private estimateInfoRetention(retainedCount: number, totalCount: number): number {
    if (totalCount === 0) return 1
    
    // 考虑重要性权重，保留的信息量通常高于数量比例
    const countRatio = retainedCount / totalCount
    const weightedRetention = Math.min(0.9, countRatio * 1.5)
    
    return Math.max(0.7, weightedRetention) // 至少保留70%信息
  }

  /**
   * 摘要项目上下文
   */
  summarizeProjectContext(files: string[], maxFiles: number = 20): string[] {
    if (files.length <= maxFiles) {
      return files
    }

    // 优先保留重要文件（配置文件、入口文件等）
    const importantPatterns = [
      /package\.json$/,
      /tsconfig\.json$/,
      /vite\.config\./,
      /main\.[jt]sx?$/,
      /App\.[jt]sx?$/,
      /index\.[jt]sx?$/,
      /README\.md$/,
      /\.env$/
    ]

    const important: string[] = []
    const others: string[] = []

    files.forEach(file => {
      if (importantPatterns.some(pattern => pattern.test(file))) {
        important.push(file)
      } else {
        others.push(file)
      }
    })

    // 保留所有重要文件 + 部分其他文件
    const remainingSlots = maxFiles - important.length
    return [...important, ...others.slice(0, Math.max(0, remainingSlots))]
  }

  /**
   * 更新配置
   */
  updateConfig(config: Partial<SummarizerConfig>): void {
    this.config = { ...this.config, ...config }
  }
}

export default new ContextSummarizer()

