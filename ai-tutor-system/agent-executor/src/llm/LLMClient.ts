import type { ToolResult } from './types';

/**
 * LLM 客户端接口
 */
export interface LLMResponse {
  text: string;
  tokens: number;
}

/**
 * LLM 客户端
 */
export class LLMClient {
  private apiKey: string;
  private baseUrl: string;
  private model: string;
  
  constructor() {
    this.apiKey = process.env.LLM_API_KEY || '';
    this.baseUrl = process.env.LLM_BASE_URL || 'https://api.openai.com/v1';
    this.model = process.env.LLM_MODEL || 'gpt-4';
  }
  
  /**
   * 生成响应
   */
  async generate(prompt: string): Promise<LLMResponse> {
    try {
      const response = await fetch(`${this.baseUrl}/chat/completions`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${this.apiKey}`
        },
        body: JSON.stringify({
          model: this.model,
          messages: [
            {
              role: 'system',
              content: this.getSystemPrompt()
            },
            {
              role: 'user',
              content: prompt
            }
          ],
          temperature: 0.7,
          max_tokens: 2000
        })
      });
      
      if (!response.ok) {
        throw new Error(`LLM API error: ${response.status}`);
      }
      
      const data = await response.json();
      
      return {
        text: data.choices[0].message.content,
        tokens: data.usage?.total_tokens || 0
      };
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : String(error);
      throw new Error(`LLM generation failed: ${errorMessage}`);
    }
  }
  
  /**
   * 获取系统提示词
   */
  private getSystemPrompt(): string {
    return `# Role: AI Sandbox Developer Agent
你是一个运行在高度受限（2核/1.8G RAM）且隔离的 Linux 沙箱环境中的高级智能体。你通过精准的工具调用协助用户完成代码编写、系统运维和项目管理。

## 🔴 VOID RULES (绝对执行准则)
1. **SANDBOX_ONLY**: 只能访问当前工作目录。禁止尝试访问 \`/etc\`, \`/root\` 等绝对路径。
2. **GIT_SHADOW**: 系统在每次工具调用前会自动创建 Git 快照。若操作失败或逻辑错误，必须调用 \`<undo_last_action>{}</undo_last_action>\`。
3. **MEMORY_FIRST**: 1.8G 内存严禁一次性读取 > 500 行的文件。禁止执行高内存占用的扫描命令。
4. **ANCHOR_EDIT**: 修改已有文件时，禁止重写全文。必须使用 \`<edit_file_by_anchor>\`，通过 10 字符锚点定位。
5. **REACT_PROCESS**: 必须遵循 "思考 -> 行动 -> 观察" 循环。在调用工具前，必须输出 \`<thinking>\` 标签说明理由。
6. **NO_WASTE**: 保持回复极其简练。任务完成后仅需简短确认。如果没有任务，仅使用自然语言交流。

## 🧩 TOOL PROTOCOL (XML + JSON 混合格式)
所有工具调用必须严格遵循 \`<tool_name>{ "json_args": "value" }</tool_name>\`。

### 可用工具：
- terminal_run: 执行终端命令
- read_file: 分页读取文件
- edit_file_by_anchor: 精确编辑文件
- search_in_files: 搜索文件内容
- ls: 列出目录
- undo_last_action: 撤销上一步操作`;
  }
}
