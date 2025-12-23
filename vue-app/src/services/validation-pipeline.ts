/**
 * 多阶段验证机制
 * 流程：意图识别 → 权限校验 → 环境检测 → 操作执行
 */

import {
  TerminalMessage,
  ValidationPhase,
  ValidationResult,
  ValidationContext,
  ActionScope
} from '@/types/terminal-message'
import permissionManager from './permission-manager'

/**
 * 意图识别结果
 */
export interface IntentRecognitionResult {
  /** 意图类型 */
  intent: 'plan' | 'execute' | 'query' | 'chat'
  /** 置信度 */
  confidence: number
  /** 提取的参数 */
  params: Record<string, any>
}

/**
 * 环境检测结果
 */
export interface EnvironmentCheckResult {
  /** 环境是否就绪 */
  ready: boolean
  /** 缺失的依赖 */
  missingDependencies?: string[]
  /** 环境变量缺失 */
  missingEnvVars?: string[]
  /** 建议 */
  suggestions?: string[]
}

/**
 * 验证管道
 */
export class ValidationPipeline {
  private static instance: ValidationPipeline

  private constructor() {}

  static getInstance(): ValidationPipeline {
    if (!ValidationPipeline.instance) {
      ValidationPipeline.instance = new ValidationPipeline()
    }
    return ValidationPipeline.instance
  }

  /**
   * 执行完整的验证流程
   */
  async validate(message: TerminalMessage): Promise<ValidationContext> {
    const context: ValidationContext = {
      message,
      results: [],
      totalDuration: 0,
      allPassed: true
    }

    const startTime = Date.now()

    try {
      // 阶段1: 意图识别
      const intentResult = await this.validateIntent(message)
      context.results.push(intentResult)
      if (!intentResult.passed) {
        context.allPassed = false
        return context
      }

      // 阶段2: 权限校验
      const permissionResult = await this.validatePermission(message)
      context.results.push(permissionResult)
      if (!permissionResult.passed) {
        context.allPassed = false
        return context
      }

      // 阶段3: 环境检测
      const environmentResult = await this.validateEnvironment(message)
      context.results.push(environmentResult)
      if (!environmentResult.passed) {
        context.allPassed = false
        return context
      }

      // 阶段4: 操作执行准备（预检）
      const executionResult = await this.validateExecution(message)
      context.results.push(executionResult)
      if (!executionResult.passed) {
        context.allPassed = false
      }

    } finally {
      context.totalDuration = Date.now() - startTime
    }

    return context
  }

  /**
   * 阶段1: 意图识别
   */
  private async validateIntent(message: TerminalMessage): Promise<ValidationResult> {
    const startTime = Date.now()
    
    try {
      const intent = this.recognizeIntent(message)
      
      // 验证意图是否明确
      if (intent.confidence < 0.6) {
        return {
          phase: ValidationPhase.INTENT_RECOGNITION,
          passed: false,
          reason: `意图不明确，置信度过低: ${intent.confidence}`,
          duration: Date.now() - startTime
        }
      }

      // 验证是否有必要的参数
      if (intent.intent === 'execute' && !message.action.method) {
        return {
          phase: ValidationPhase.INTENT_RECOGNITION,
          passed: false,
          reason: '执行意图缺少操作方法',
          duration: Date.now() - startTime
        }
      }

      return {
        phase: ValidationPhase.INTENT_RECOGNITION,
        passed: true,
        duration: Date.now() - startTime
      }
    } catch (error: any) {
      return {
        phase: ValidationPhase.INTENT_RECOGNITION,
        passed: false,
        reason: `意图识别失败: ${error.message}`,
        duration: Date.now() - startTime
      }
    }
  }

  /**
   * 阶段2: 权限校验
   */
  private async validatePermission(message: TerminalMessage): Promise<ValidationResult> {
    const startTime = Date.now()
    
    try {
      const { action } = message
      
      const result = await permissionManager.checkPermission(
        action.method,
        action.scope,
        action.requiredPermission,
        action.target
      )

      if (!result.allowed) {
        return {
          phase: ValidationPhase.PERMISSION_CHECK,
          passed: false,
          reason: result.reason || '权限不足',
          duration: Date.now() - startTime
        }
      }

      return {
        phase: ValidationPhase.PERMISSION_CHECK,
        passed: true,
        duration: Date.now() - startTime
      }
    } catch (error: any) {
      return {
        phase: ValidationPhase.PERMISSION_CHECK,
        passed: false,
        reason: `权限校验失败: ${error.message}`,
        duration: Date.now() - startTime
      }
    }
  }

  /**
   * 阶段3: 环境检测
   */
  private async validateEnvironment(message: TerminalMessage): Promise<ValidationResult> {
    const startTime = Date.now()
    
    try {
      const envCheck = await this.checkEnvironment(message)

      if (!envCheck.ready) {
        const reasons = [
          envCheck.missingDependencies?.length && 
            `缺少依赖: ${envCheck.missingDependencies.join(', ')}`,
          envCheck.missingEnvVars?.length && 
            `缺少环境变量: ${envCheck.missingEnvVars.join(', ')}`
        ].filter(Boolean).join('; ')

        return {
          phase: ValidationPhase.ENVIRONMENT_DETECTION,
          passed: false,
          reason: reasons || '环境未就绪',
          duration: Date.now() - startTime
        }
      }

      return {
        phase: ValidationPhase.ENVIRONMENT_DETECTION,
        passed: true,
        duration: Date.now() - startTime
      }
    } catch (error: any) {
      return {
        phase: ValidationPhase.ENVIRONMENT_DETECTION,
        passed: false,
        reason: `环境检测失败: ${error.message}`,
        duration: Date.now() - startTime
      }
    }
  }

  /**
   * 阶段4: 操作执行预检
   */
  private async validateExecution(message: TerminalMessage): Promise<ValidationResult> {
    const startTime = Date.now()
    
    try {
      const { action } = message

      // 检查目标路径
      if (action.target && !this.isValidPath(action.target)) {
        return {
          phase: ValidationPhase.OPERATION_EXECUTION,
          passed: false,
          reason: `无效的目标路径: ${action.target}`,
          duration: Date.now() - startTime
        }
      }

      // 检查操作参数
      if (action.params && !this.areValidParams(action.method, action.params)) {
        return {
          phase: ValidationPhase.OPERATION_EXECUTION,
          passed: false,
          reason: '操作参数不完整或无效',
          duration: Date.now() - startTime
        }
      }

      return {
        phase: ValidationPhase.OPERATION_EXECUTION,
        passed: true,
        duration: Date.now() - startTime
      }
    } catch (error: any) {
      return {
        phase: ValidationPhase.OPERATION_EXECUTION,
        passed: false,
        reason: `执行预检失败: ${error.message}`,
        duration: Date.now() - startTime
      }
    }
  }

  /**
   * 识别用户意图
   */
  private recognizeIntent(message: TerminalMessage): IntentRecognitionResult {
    const { action } = message

    // 基于操作方法识别意图
    const executeKeywords = ['execute', 'run', 'install', 'build', 'deploy']
    const queryKeywords = ['read', 'get', 'list', 'search', 'find']
    const planKeywords = ['create', 'setup', 'init', 'generate', 'scaffold']

    const method = action.method.toLowerCase()

    if (executeKeywords.some(kw => method.includes(kw))) {
      return { intent: 'execute', confidence: 0.9, params: action.params || {} }
    }

    if (queryKeywords.some(kw => method.includes(kw))) {
      return { intent: 'query', confidence: 0.85, params: action.params || {} }
    }

    if (planKeywords.some(kw => method.includes(kw))) {
      return { intent: 'plan', confidence: 0.8, params: action.params || {} }
    }

    // 默认为聊天
    return { intent: 'chat', confidence: 0.6, params: {} }
  }

  /**
   * 检查环境
   */
  private async checkEnvironment(message: TerminalMessage): Promise<EnvironmentCheckResult> {
    const { context } = message

    // 检查项目框架相关的依赖
    const missingDependencies: string[] = []
    const missingEnvVars: string[] = []
    const suggestions: string[] = []

    // 根据框架检查必要依赖
    if (context.project.framework === 'vue') {
      // Vue项目检查
      if (!context.visibleFiles.some(f => f.includes('package.json'))) {
        missingDependencies.push('package.json')
        suggestions.push('请确保在Vue项目根目录')
      }
    } else if (context.project.framework === 'react') {
      // React项目检查
      if (!context.visibleFiles.some(f => f.includes('package.json'))) {
        missingDependencies.push('package.json')
      }
    }

    // 检查环境变量（如果需要执行命令）
    if (message.action.scope === 'execute') {
      if (context.project.env === 'production' && !process.env.NODE_ENV) {
        missingEnvVars.push('NODE_ENV')
      }
    }

    return {
      ready: missingDependencies.length === 0 && missingEnvVars.length === 0,
      missingDependencies: missingDependencies.length > 0 ? missingDependencies : undefined,
      missingEnvVars: missingEnvVars.length > 0 ? missingEnvVars : undefined,
      suggestions: suggestions.length > 0 ? suggestions : undefined
    }
  }

  /**
   * 验证路径是否有效
   */
  private isValidPath(path: string): boolean {
    // 基本路径验证
    if (!path || path.trim() === '') return false
    
    // 禁止危险路径
    const dangerousPatterns = [
      /\.\./,           // 父目录遍历
      /^\/etc\//,       // 系统配置
      /^\/sys\//,       // 系统目录
      /^\/proc\//,      // 进程目录
      /^C:\\Windows/i   // Windows系统目录
    ]

    return !dangerousPatterns.some(pattern => pattern.test(path))
  }

  /**
   * 验证参数是否有效
   */
  private areValidParams(method: string, params: Record<string, any>): boolean {
    // 根据不同方法验证必需参数
    const requiredParams: Record<string, string[]> = {
      'write_file': ['path', 'content'],
      'execute_command': ['command'],
      'read_file': ['path'],
      'ensure_file': ['path', 'content']
    }

    const required = requiredParams[method]
    if (!required) return true // 无特定要求

    return required.every(key => key in params && params[key] !== undefined)
  }
}

export default ValidationPipeline.getInstance()

