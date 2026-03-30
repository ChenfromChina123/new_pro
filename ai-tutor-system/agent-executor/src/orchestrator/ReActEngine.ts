import type { LLMClient } from '../llm/LLMClient';
import type { ToolRegistry } from '../tools/ToolRegistry';
import type { SandboxManager } from '../sandbox/SandboxManager';
import type { ToolCall, ToolResult } from '../tools/types';
import { ContextBuilder } from './ContextBuilder';
import { logger } from '../utils/logger';

/**
 * ReAct 步骤接口
 */
export interface ReActStep {
  thought: string;
  action: ToolCall | null;
  observation: string;
}

/**
 * ReAct 执行结果
 */
export interface ReActResult {
  success: boolean;
  output: string;
  steps: ReActStep[];
  totalTokens: number;
  executionTimeMs: number;
}

/**
 * 执行回调
 */
export interface ExecutionCallbacks {
  onThought: (thought: string) => void;
  onAction: (action: ToolCall) => void;
  onObservation: (observation: string) => void;
  onComplete: (result: ReActResult) => void;
  onError: (error: string) => void;
}

/**
 * ReAct 循环引擎
 * 实现 思考 -> 行动 -> 观察 循环
 */
export class ReActEngine {
  private llmClient: LLMClient;
  private toolRegistry: ToolRegistry;
  private sandboxManager: SandboxManager;
  private contextBuilder: ContextBuilder;
  
  private readonly maxSteps = 20;
  private readonly timeoutMs = 60000;
  
  constructor(
    llmClient: LLMClient,
    toolRegistry: ToolRegistry,
    sandboxManager: SandboxManager
  ) {
    this.llmClient = llmClient;
    this.toolRegistry = toolRegistry;
    this.sandboxManager = sandboxManager;
    this.contextBuilder = new ContextBuilder();
  }
  
  /**
   * 执行任务
   */
  async execute(
    taskId: string,
    input: string,
    sessionId: string,
    callbacks: ExecutionCallbacks
  ): Promise<ReActResult> {
    const startTime = Date.now();
    const steps: ReActStep[] = [];
    let totalTokens = 0;
    let isComplete = false;
    let finalOutput = '';
    
    try {
      const sandbox = await this.sandboxManager.getSandbox(sessionId);
      if (!sandbox) {
        throw new Error(`Sandbox not found for session: ${sessionId}`);
      }
      
      const context = this.contextBuilder.buildInitialContext(input, sandbox.workingDirectory);
      
      for (let step = 0; step < this.maxSteps && !isComplete; step++) {
        if (Date.now() - startTime > this.timeoutMs) {
          throw new Error('Execution timeout');
        }
        
        const thought = await this.think(context, steps);
        callbacks.onThought(thought);
        
        const action = await this.planAction(thought, context);
        if (!action) {
          isComplete = true;
          finalOutput = thought;
          break;
        }
        
        callbacks.onAction(action);
        
        const observation = await this.executeAction(action, sandbox.path);
        callbacks.onObservation(observation);
        
        steps.push({ thought, action, observation });
        
        this.contextBuilder.updateContext(context, thought, action, observation);
        
        if (this.shouldComplete(observation)) {
          isComplete = true;
          finalOutput = observation;
        }
      }
      
      const result: ReActResult = {
        success: true,
        output: finalOutput,
        steps,
        totalTokens,
        executionTimeMs: Date.now() - startTime
      };
      
      callbacks.onComplete(result);
      return result;
      
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : String(error);
      logger.error(`Execution failed for task ${taskId}:`, errorMessage);
      callbacks.onError(errorMessage);
      
      return {
        success: false,
        output: errorMessage,
        steps,
        totalTokens,
        executionTimeMs: Date.now() - startTime
      };
    }
  }
  
  /**
   * 思考阶段
   */
  private async think(context: string, previousSteps: ReActStep[]): Promise<string> {
    const prompt = this.buildThinkPrompt(context, previousSteps);
    const response = await this.llmClient.generate(prompt);
    return response.text;
  }
  
  /**
   * 规划行动
   */
  private async planAction(thought: string, context: string): Promise<ToolCall | null> {
    const prompt = this.buildActionPrompt(thought, context);
    const response = await this.llmClient.generate(prompt);
    
    return this.parseToolCall(response.text);
  }
  
  /**
   * 执行行动
   */
  private async executeAction(action: ToolCall, sandboxPath: string): Promise<string> {
    const tool = this.toolRegistry.getTool(action.name);
    if (!tool) {
      return `Error: Unknown tool '${action.name}'`;
    }
    
    try {
      const result = await tool.execute(action.params, sandboxPath);
      return result.output;
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : String(error);
      return `Error: ${errorMessage}`;
    }
  }
  
  /**
   * 判断是否完成
   */
  private shouldComplete(observation: string): boolean {
    const completionIndicators = [
      '任务完成',
      'task completed',
      'done',
      'finished',
      '成功',
      'success'
    ];
    
    const lowerObs = observation.toLowerCase();
    return completionIndicators.some(indicator => 
      lowerObs.includes(indicator.toLowerCase())
    );
  }
  
  /**
   * 构建思考提示词
   */
  private buildThinkPrompt(context: string, previousSteps: ReActStep[]): string {
    let prompt = `## 当前上下文
${context}

`;
    
    if (previousSteps.length > 0) {
      prompt += `## 之前的步骤
`;
      previousSteps.forEach((step, index) => {
        prompt += `### 步骤 ${index + 1}
思考: ${step.thought}
行动: ${step.action ? `${step.action.name}(${JSON.stringify(step.action.params)})` : '无'}
观察: ${step.observation}

`;
      });
    }
    
    prompt += `请思考下一步应该做什么。如果任务已完成，请说明结果。`;
    
    return prompt;
  }
  
  /**
   * 构建行动提示词
   */
  private buildActionPrompt(thought: string, context: string): string {
    const availableTools = this.toolRegistry.getToolDescriptions();
    
    return `## 思考
${thought}

## 可用工具
${availableTools.map(t => `- ${t.name}: ${t.description}`).join('\n')}

## 工具调用格式
<tool_name>{"param1": "value1", "param2": "value2"}</tool_name>

如果需要调用工具，请按上述格式输出。如果任务已完成，请输出 "任务完成" 或 "Task completed"。`;
  }
  
  /**
   * 解析工具调用
   */
  private parseToolCall(text: string): ToolCall | null {
    const toolCallRegex = /<(\w+)>\s*(\{[\s\S]*?\})\s*<\/\1>/;
    const match = text.match(toolCallRegex);
    
    if (!match) {
      return null;
    }
    
    const toolName = match[1];
    let params: Record<string, unknown> = {};
    
    try {
      params = JSON.parse(match[2]);
    } catch {
      params = {};
    }
    
    return { name: toolName, params };
  }
}
