import type { Tool, ToolDescription, ToolResult } from './types';
import { TerminalTool } from './TerminalTool';
import { FileReadTool } from './FileReadTool';
import { FileEditTool } from './FileEditTool';
import { FileSearchTool } from './FileSearchTool';
import { LsTool } from './LsTool';
import { GitTool } from './GitTool';

/**
 * 工具注册表
 */
export class ToolRegistry {
  private tools: Map<string, Tool> = new Map();
  
  constructor() {
    this.registerDefaultTools();
  }
  
  /**
   * 注册默认工具
   */
  private registerDefaultTools(): void {
    this.register(new TerminalTool());
    this.register(new FileReadTool());
    this.register(new FileEditTool());
    this.register(new FileSearchTool());
    this.register(new LsTool());
    this.register(new GitTool());
  }
  
  /**
   * 注册工具
   */
  register(tool: Tool): void {
    this.tools.set(tool.name, tool);
  }
  
  /**
   * 获取工具
   */
  getTool(name: string): Tool | undefined {
    return this.tools.get(name);
  }
  
  /**
   * 获取所有工具描述
   */
  getToolDescriptions(): ToolDescription[] {
    const descriptions: ToolDescription[] = [];
    
    this.tools.forEach((tool) => {
      descriptions.push({
        name: tool.name,
        description: tool.description,
        parameters: tool.parameters
      });
    });
    
    return descriptions;
  }
  
  /**
   * 检查工具是否存在
   */
  hasTool(name: string): boolean {
    return this.tools.has(name);
  }
  
  /**
   * 获取所有工具名称
   */
  getToolNames(): string[] {
    return Array.from(this.tools.keys());
  }
}
