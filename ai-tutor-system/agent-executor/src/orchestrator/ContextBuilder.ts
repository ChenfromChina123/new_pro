/**
 * 上下文构建器
 * 负责构建和维护 Agent 执行上下文
 */
export class ContextBuilder {
  private context: string;
  
  constructor() {
    this.context = '';
  }
  
  /**
   * 构建初始上下文
   */
  buildInitialContext(input: string, workingDirectory: string): string {
    this.context = `## 任务
${input}

## 工作目录
${workingDirectory}

## 资源限制
- 内存: 512MB
- CPU: 50%
- 超时: 60秒
- 最大步骤: 20

## 规则
1. 只能访问当前工作目录
2. 禁止执行危险命令
3. 每次文件操作前会自动创建快照
4. 必须遵循 ReAct 循环: 思考 -> 行动 -> 观察
`;
    return this.context;
  }
  
  /**
   * 更新上下文
   */
  updateContext(
    context: string,
    thought: string,
    action: { name: string; params: Record<string, unknown> } | null,
    observation: string
  ): string {
    const update = `

## 最新步骤
思考: ${thought}
行动: ${action ? `${action.name}(${JSON.stringify(action.params)})` : '无'}
观察: ${observation}
`;
    
    this.context = context + update;
    return this.context;
  }
  
  /**
   * 获取当前上下文
   */
  getContext(): string {
    return this.context;
  }
  
  /**
   * 压缩上下文（用于节省 Token）
   */
  compressContext(): string {
    const lines = this.context.split('\n');
    const compressedLines: string[] = [];
    let inSection = false;
    let sectionLines = 0;
    
    for (const line of lines) {
      if (line.startsWith('##')) {
        inSection = true;
        sectionLines = 0;
        compressedLines.push(line);
      } else if (inSection) {
        sectionLines++;
        if (sectionLines <= 50) {
          compressedLines.push(line);
        } else if (sectionLines === 51) {
          compressedLines.push('... (内容已压缩)');
        }
      }
    }
    
    this.context = compressedLines.join('\n');
    return this.context;
  }
}
