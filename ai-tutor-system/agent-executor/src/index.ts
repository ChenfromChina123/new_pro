/**
 * Agent 执行层入口文件
 */

import { createServer } from 'http';
import { ReActEngine } from './orchestrator/ReActEngine';
import { SandboxManager } from './sandbox/SandboxManager';
import { ToolRegistry } from './tools/ToolRegistry';
import { LLMClient } from './llm/LLMClient';
import { logger } from './utils/logger';

const PORT = process.env.PORT || 3001;

/**
 * 将多行文本安全编码为 SSE data 单行
 * @param {string} text 原始文本
 * @returns {string} 单行安全文本
 */
function escapeForSseData(text: string): string {
  return String(text ?? '')
    .replace(/\r/g, '')
    .replace(/\n/g, '\\n');
}

/**
 * 初始化执行层服务
 */
async function initialize() {
  logger.info('Initializing Agent Executor...');

  const toolRegistry = new ToolRegistry();
  const sandboxManager = new SandboxManager();
  const llmClient = new LLMClient();
  const reactEngine = new ReActEngine(llmClient, toolRegistry, sandboxManager);

  logger.info('Agent Executor initialized successfully');

  return { reactEngine, sandboxManager, toolRegistry };
}

/**
 * 创建 HTTP 服务器
 */
async function main() {
  const { reactEngine, sandboxManager } = await initialize();

  const server = createServer(async (req, res) => {
    const url = new URL(req.url || '/', `http://localhost:${PORT}`);

    res.setHeader('Content-Type', 'application/json');

    if (url.pathname === '/health') {
      res.end(JSON.stringify({ status: 'ok', timestamp: new Date().toISOString() }));
      return;
    }

    if (url.pathname === '/execute' && req.method === 'POST') {
      let body = '';
      req.on('data', chunk => body += chunk);
      req.on('end', async () => {
        try {
          const { taskId, input, sessionId } = JSON.parse(body);

          res.setHeader('Content-Type', 'text/event-stream');
          res.setHeader('Cache-Control', 'no-cache');
          res.setHeader('Connection', 'keep-alive');

          // ReAct 执行阶段：把 engine 的 thought/action/observation 转成前端需要的事件名
          let lastThought = '';
          let lastAction: any = null;
          let currentStep = 0;
          const totalSteps = 20; // 与 ReActEngine.maxSteps 保持一致（用于前端进度条）

          res.write(`event: start\ndata: ${JSON.stringify({ taskId })}\n\n`);

          await reactEngine.execute(taskId, input, sessionId, {
            onThought: (thought) => {
              lastThought = thought;
            },
            onAction: (action) => {
              lastAction = action;
            },
            onObservation: (observation) => {
              currentStep += 1;

              const safeObservation = escapeForSseData(observation);

              if (lastAction) {
                const toolName = lastAction.name;
                const toolInput = lastAction.params;
                const isError = typeof safeObservation === 'string' && safeObservation.toLowerCase().startsWith('error');

                // 工具调用事件（前端用于 ToolCallDisplay 展示）
                res.write(`event: tool_call\ndata: ${JSON.stringify({
                  id: `${Date.now()}-${currentStep}`,
                  toolName,
                  toolInput,
                  toolOutput: safeObservation,
                  status: isError ? 'failed' : 'success',
                  thought: lastThought,
                  observation: safeObservation,
                  stepNumber: currentStep
                })}\n\n`);

                // 终端输出事件（前端 AgentTerminal 展示）
                if (toolName === 'terminal_run') {
                  res.write(`event: output\ndata: ${safeObservation}\n\n`);
                }
              }

              // 进度事件（前端用于进度条）
              res.write(`event: progress\ndata: ${JSON.stringify({
                currentStep,
                totalSteps
              })}\n\n`);
            },
            onComplete: (result) => {
              res.write(`event: complete\ndata: ${escapeForSseData(result.output)}\n\n`);
              res.end();
            },
            onError: (error) => {
              res.write(`event: error\ndata: ${JSON.stringify({ error })}\n\n`);
              res.end();
            }
          });
        } catch (error) {
          logger.error('Execution error:', error);
          res.end(JSON.stringify({ error: 'Internal server error' }));
        }
      });
      return;
    }

    if (url.pathname === '/sandbox/create' && req.method === 'POST') {
      let body = '';
      req.on('data', chunk => body += chunk);
      req.on('end', async () => {
        try {
          const { sessionId } = JSON.parse(body);
          const sandbox = await sandboxManager.createSandbox(sessionId);
          res.end(JSON.stringify({ success: true, sandbox }));
        } catch (error) {
          logger.error('Sandbox creation error:', error);
          res.end(JSON.stringify({ error: 'Failed to create sandbox' }));
        }
      });
      return;
    }

    res.statusCode = 404;
    res.end(JSON.stringify({ error: 'Not found' }));
  });

  server.listen(PORT, () => {
    logger.info(`Agent Executor server running on port ${PORT}`);
  });

  process.on('SIGINT', () => {
    logger.info('Shutting down...');
    server.close(() => {
      logger.info('Server closed');
      process.exit(0);
    });
  });
}

main().catch(error => {
  logger.error('Fatal error:', error);
  process.exit(1);
});

export { ReActEngine, SandboxManager, ToolRegistry, LLMClient };
