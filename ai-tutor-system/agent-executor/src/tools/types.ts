/**
 * 工具参数定义
 */
export interface ToolParameter {
  type: string;
  description: string;
  required?: boolean;
  enum?: string[];
}

/**
 * 工具调用
 */
export interface ToolCall {
  name: string;
  params: Record<string, unknown>;
}

/**
 * 工具结果
 */
export interface ToolResult {
  success: boolean;
  output: string;
  error?: string;
}

/**
 * 工具描述
 */
export interface ToolDescription {
  name: string;
  description: string;
  parameters: Record<string, ToolParameter>;
}

/**
 * 工具接口
 */
export interface Tool {
  name: string;
  description: string;
  parameters: Record<string, ToolParameter>;
  execute(params: Record<string, unknown>, sandboxPath: string): Promise<ToolResult>;
}
