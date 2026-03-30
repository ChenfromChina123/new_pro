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
          
          await reactEngine.execute(taskId, input, sessionId, {
            onThought: (thought) => {
              res.write(`event: thought\ndata: ${JSON.stringify({ thought })}\n\n`);
            },
            onAction: (action) => {
              res.write(`event: action\ndata: ${JSON.stringify(action)}\n\n`);
            },
            onObservation: (observation) => {
              res.write(`event: observation\ndata: ${JSON.stringify({ observation })}\n\n`);
            },
            onComplete: (result) => {
              res.write(`event: complete\ndata: ${JSON.stringify(result)}\n\n`);
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
