/**
 * 三级权限沙箱管理系统
 * 实现基础层、操作层、系统层的动态权限控制
 */

import { PermissionLevel, ActionScope } from '@/types/terminal-message'

/**
 * 权限配置
 */
export interface PermissionConfig {
  /** 权限等级 */
  level: PermissionLevel
  /** 允许的操作范围 */
  allowedScopes: ActionScope[]
  /** 允许的操作方法 */
  allowedMethods: string[]
  /** 是否需要审批 */
  requiresApproval: boolean
  /** 审批超时时间(ms) */
  approvalTimeout?: number
}

/**
 * 审批请求
 */
export interface ApprovalRequest {
  /** 请求ID */
  id: string
  /** 操作描述 */
  description: string
  /** 所需权限 */
  requiredPermission: PermissionLevel
  /** 操作范围 */
  scope: ActionScope
  /** 目标路径 */
  target: string
  /** 请求时间 */
  timestamp: number
  /** 审批状态 */
  status: 'pending' | 'approved' | 'rejected' | 'timeout'
}

/**
 * 权限管理器
 */
export class PermissionManager {
  private static instance: PermissionManager
  private currentLevel: PermissionLevel = 'basic'
  private pendingApprovals: Map<string, ApprovalRequest> = new Map()
  private approvalCallbacks: Map<string, (approved: boolean) => void> = new Map()

  // 三级权限配置
  private readonly permissionConfigs: Record<PermissionLevel, PermissionConfig> = {
    // 基础层：文件读取、环境查询
    basic: {
      level: 'basic',
      allowedScopes: ['read'],
      allowedMethods: [
        'read_file',
        'list_directory',
        'get_file_info',
        'search_files',
        'get_environment'
      ],
      requiresApproval: false
    },
    // 操作层：代码生成、文件修改
    operation: {
      level: 'operation',
      allowedScopes: ['read', 'write'],
      allowedMethods: [
        'read_file',
        'write_file',
        'ensure_file',
        'delete_file',
        'rename_file',
        'create_directory',
        'generate_code',
        'refactor_code'
      ],
      requiresApproval: true,
      approvalTimeout: 30000 // 30秒
    },
    // 系统层：依赖安装、进程管理
    system: {
      level: 'system',
      allowedScopes: ['read', 'write', 'execute'],
      allowedMethods: [
        'execute_command',
        'install_dependency',
        'run_script',
        'start_process',
        'stop_process',
        'modify_config',
        'git_operation'
      ],
      requiresApproval: true,
      approvalTimeout: 60000 // 60秒
    }
  }

  private constructor() {}

  /**
   * 获取单例实例
   */
  static getInstance(): PermissionManager {
    if (!PermissionManager.instance) {
      PermissionManager.instance = new PermissionManager()
    }
    return PermissionManager.instance
  }

  /**
   * 设置当前权限等级
   */
  setPermissionLevel(level: PermissionLevel): void {
    this.currentLevel = level
    console.log(`[PermissionManager] 权限等级已设置为: ${level}`)
  }

  /**
   * 获取当前权限等级
   */
  getCurrentLevel(): PermissionLevel {
    return this.currentLevel
  }

  /**
   * 检查操作是否有权限
   */
  async checkPermission(
    method: string,
    scope: ActionScope,
    requiredLevel: PermissionLevel,
    target?: string
  ): Promise<{ allowed: boolean; reason?: string }> {
    // 检查是否在允许的方法列表中
    const config = this.permissionConfigs[requiredLevel]
    if (!config.allowedMethods.includes(method)) {
      return {
        allowed: false,
        reason: `方法 ${method} 不在权限等级 ${requiredLevel} 的允许列表中`
      }
    }

    // 检查操作范围
    if (!config.allowedScopes.includes(scope)) {
      return {
        allowed: false,
        reason: `操作范围 ${scope} 超出权限等级 ${requiredLevel} 的允许范围`
      }
    }

    // 检查当前用户权限是否足够
    const levelHierarchy: PermissionLevel[] = ['basic', 'operation', 'system']
    const currentIndex = levelHierarchy.indexOf(this.currentLevel)
    const requiredIndex = levelHierarchy.indexOf(requiredLevel)

    if (currentIndex < requiredIndex) {
      return {
        allowed: false,
        reason: `当前权限 ${this.currentLevel} 不足，需要 ${requiredLevel} 权限`
      }
    }

    // 检查是否需要审批
    if (config.requiresApproval) {
      const approved = await this.requestApproval(method, scope, requiredLevel, target || '')
      if (!approved) {
        return {
          allowed: false,
          reason: '操作未获得审批'
        }
      }
    }

    return { allowed: true }
  }

  /**
   * 请求操作审批
   */
  private async requestApproval(
    method: string,
    scope: ActionScope,
    requiredLevel: PermissionLevel,
    target: string
  ): Promise<boolean> {
    const request: ApprovalRequest = {
      id: `approval_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
      description: `执行 ${method} 操作 (${scope}) 于 ${target}`,
      requiredPermission: requiredLevel,
      scope,
      target,
      timestamp: Date.now(),
      status: 'pending'
    }

    this.pendingApprovals.set(request.id, request)

    // 触发审批UI（通过事件）
    window.dispatchEvent(
      new CustomEvent('permission-approval-request', { detail: request })
    )

    // 等待审批结果
    return new Promise((resolve) => {
      const timeout = this.permissionConfigs[requiredLevel].approvalTimeout || 30000

      // 设置超时
      const timer = setTimeout(() => {
        request.status = 'timeout'
        this.pendingApprovals.delete(request.id)
        this.approvalCallbacks.delete(request.id)
        resolve(false)
      }, timeout)

      // 保存回调
      this.approvalCallbacks.set(request.id, (approved: boolean) => {
        clearTimeout(timer)
        request.status = approved ? 'approved' : 'rejected'
        this.pendingApprovals.delete(request.id)
        this.approvalCallbacks.delete(request.id)
        resolve(approved)
      })
    })
  }

  /**
   * 审批操作
   */
  approveRequest(requestId: string, approved: boolean): void {
    const callback = this.approvalCallbacks.get(requestId)
    if (callback) {
      callback(approved)
    }
  }

  /**
   * 获取待审批请求列表
   */
  getPendingApprovals(): ApprovalRequest[] {
    return Array.from(this.pendingApprovals.values()).filter(
      (req) => req.status === 'pending'
    )
  }

  /**
   * 自动提升权限（用于开发模式）
   */
  enableDevMode(): void {
    this.currentLevel = 'system'
    console.log('[PermissionManager] 开发模式已启用，权限提升至 system')
  }

  /**
   * 重置权限
   */
  reset(): void {
    this.currentLevel = 'basic'
    this.pendingApprovals.clear()
    this.approvalCallbacks.clear()
    console.log('[PermissionManager] 权限已重置')
  }
}

export default PermissionManager.getInstance()

