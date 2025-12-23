/**
 * 终端消息结构语义化定义
 * 深度集成 Cursor 工作流规范
 */

/**
 * 操作范围类型
 */
export type ActionScope = 'read' | 'write' | 'execute'

/**
 * 权限等级
 */
export type PermissionLevel = 'basic' | 'operation' | 'system'

/**
 * 项目环境信息
 */
export interface ProjectContext {
  /** 环境标识 (development/production/test) */
  env: string
  /** 语言栈 */
  lang: string[]
  /** 框架类型 */
  framework: string
  /** 关键目录结构 */
  root: string[]
}

/**
 * 元数据信息
 */
export interface MessageMeta {
  /** 对话指纹 (会话唯一标识) */
  session: string
  /** 当前 Token 消耗 */
  token: number
  /** 消息时间戳 */
  timestamp: number
  /** 消息ID */
  messageId: string
}

/**
 * 上下文信息
 */
export interface MessageContext {
  /** 项目上下文 */
  project: ProjectContext
  /** 摘要化历史记录 (最近10条) */
  history: string[]
  /** 可见文件列表 */
  visibleFiles: string[]
  /** 可见函数列表 */
  visibleFunctions: string[]
}

/**
 * 操作动作
 */
export interface MessageAction {
  /** 操作范围 */
  scope: ActionScope
  /** 操作对象路径 */
  target: string
  /** 操作方法签名 */
  method: string
  /** 操作参数 */
  params?: Record<string, any>
  /** 所需权限等级 */
  requiredPermission: PermissionLevel
}

/**
 * 终端消息完整结构
 */
export interface TerminalMessage {
  /** 元数据 */
  meta: MessageMeta
  /** 上下文信息 */
  context: MessageContext
  /** 操作动作 */
  action: MessageAction
}

/**
 * 验证阶段
 */
export enum ValidationPhase {
  /** 意图识别 */
  INTENT_RECOGNITION = 'intent_recognition',
  /** 权限校验 */
  PERMISSION_CHECK = 'permission_check',
  /** 环境检测 */
  ENVIRONMENT_DETECTION = 'environment_detection',
  /** 操作执行 */
  OPERATION_EXECUTION = 'operation_execution'
}

/**
 * 验证结果
 */
export interface ValidationResult {
  /** 验证阶段 */
  phase: ValidationPhase
  /** 是否通过 */
  passed: boolean
  /** 失败原因 */
  reason?: string
  /** 耗时(ms) */
  duration: number
}

/**
 * 多阶段验证上下文
 */
export interface ValidationContext {
  /** 原始消息 */
  message: TerminalMessage
  /** 各阶段验证结果 */
  results: ValidationResult[]
  /** 总耗时 */
  totalDuration: number
  /** 是否全部通过 */
  allPassed: boolean
}

