/**
 * agent-executor 工具代理 API
 * 
 * 为 aispring 提供文件/终端/Git 操作的代理能力
 * 支持直接调用沙箱内的工具
 */

import { $ } from 'bun';
import { SandboxManager } from './sandbox/SandboxManager';
import { logger } from './utils/logger';
import { stripAnsi } from './utils/ansiStripper';

export interface ToolProxyRequest {
  sessionId: string;
  tool: 'terminal_run' | 'read_file' | 'write_file' | 'edit_file' | 'search' | 'ls' | 'git';
  params: Record<string, unknown>;
}

export interface ToolProxyResponse {
  success: boolean;
  output?: string;
  error?: string;
}

/**
 * 工具代理路由处理器
 */
export function createToolProxyRouter(sandboxManager: SandboxManager) {
  return async function handleToolProxy(req: import('http').IncomingMessage, res: import('http').ServerResponse): Promise<boolean> {
    const url = new URL(req.url || '/', `http://localhost:${process.env.PORT || 3001}`);
    
    // POST /api/tools/execute - 执行工具调用
    if (url.pathname === '/api/tools/execute' && req.method === 'POST') {
      let body = '';
      req.on('data', chunk => body += chunk);
      req.on('end', async () => {
        try {
          const request: ToolProxyRequest = JSON.parse(body);
          const result = await executeTool(sandboxManager, request);
          res.setHeader('Content-Type', 'application/json');
          res.end(JSON.stringify(result));
        } catch (error) {
          logger.error('Tool proxy error:', error);
          res.setHeader('Content-Type', 'application/json');
          res.end(JSON.stringify({ success: false, error: String(error) }));
        }
      });
      return true;
    }

    // GET /api/tools/capabilities - 获取系统隔离能力
    if (url.pathname === '/api/tools/capabilities' && req.method === 'GET') {
      try {
        const capabilities = await sandboxManager.checkCapabilities();
        res.setHeader('Content-Type', 'application/json');
        res.end(JSON.stringify({ success: true, capabilities }));
      } catch (error) {
        res.setHeader('Content-Type', 'application/json');
        res.end(JSON.stringify({ success: false, error: String(error) }));
      }
      return true;
    }

    // 沙箱路由匹配
    const sandboxMatch = url.pathname.match(/^\/api\/tools\/sandbox\/([^/]+)(\/.*)?$/);
    if (sandboxMatch) {
      const sessionId = sandboxMatch[1];
      const subPath = sandboxMatch[2] || '';
      
      // GET /api/tools/sandbox/:sessionId/files - 列出沙箱文件
      if (subPath === '/files' && req.method === 'GET') {
        const path = url.searchParams.get('path') || '.';
        try {
          const result = await listFiles(sandboxManager, sessionId, path);
          res.setHeader('Content-Type', 'application/json');
          res.end(JSON.stringify(result));
        } catch (error) {
          res.setHeader('Content-Type', 'application/json');
          res.end(JSON.stringify({ success: false, error: String(error) }));
        }
        return true;
      }

      // GET /api/tools/sandbox/:sessionId/file - 读取文件内容
      if (subPath === '/file' && req.method === 'GET') {
        const filePath = url.searchParams.get('path');
        if (!filePath) {
          res.setHeader('Content-Type', 'application/json');
          res.end(JSON.stringify({ success: false, error: 'Missing path parameter' }));
          return true;
        }
        try {
          const result = await readFile(sandboxManager, sessionId, filePath);
          res.setHeader('Content-Type', 'application/json');
          res.end(JSON.stringify(result));
        } catch (error) {
          res.setHeader('Content-Type', 'application/json');
          res.end(JSON.stringify({ success: false, error: String(error) }));
        }
        return true;
      }

      // POST /api/tools/sandbox/:sessionId/file - 写入文件内容
      if (subPath === '/file' && req.method === 'POST') {
        let body = '';
        req.on('data', chunk => body += chunk);
        req.on('end', async () => {
          try {
            const { path, content } = JSON.parse(body);
            const result = await writeFile(sandboxManager, sessionId, path, content);
            res.setHeader('Content-Type', 'application/json');
            res.end(JSON.stringify(result));
          } catch (error) {
            res.setHeader('Content-Type', 'application/json');
            res.end(JSON.stringify({ success: false, error: String(error) }));
          }
        });
        return true;
      }

      // POST /api/tools/sandbox/:sessionId/terminal - 执行终端命令
      if (subPath === '/terminal' && req.method === 'POST') {
        let body = '';
        req.on('data', chunk => body += chunk);
        req.on('end', async () => {
          try {
            const { command, timeoutMs } = JSON.parse(body);
            const result = await executeTerminal(sandboxManager, sessionId, command, timeoutMs);
            res.setHeader('Content-Type', 'application/json');
            res.end(JSON.stringify(result));
          } catch (error) {
            res.setHeader('Content-Type', 'application/json');
            res.end(JSON.stringify({ success: false, error: String(error) }));
          }
        });
        return true;
      }

      // POST /api/tools/sandbox/:sessionId/git - 执行 Git 命令
      if (subPath === '/git' && req.method === 'POST') {
        let body = '';
        req.on('data', chunk => body += chunk);
        req.on('end', async () => {
          try {
            const { command, message } = JSON.parse(body);
            const result = await executeGit(sandboxManager, sessionId, command, message);
            res.setHeader('Content-Type', 'application/json');
            res.end(JSON.stringify(result));
          } catch (error) {
            res.setHeader('Content-Type', 'application/json');
            res.end(JSON.stringify({ success: false, error: String(error) }));
          }
        });
        return true;
      }

      // GET /api/tools/sandbox/:sessionId/usage - 获取沙箱资源使用情况
      if (subPath === '/usage' && req.method === 'GET') {
        try {
          const result = await sandboxManager.getSandboxUsage(sessionId);
          res.setHeader('Content-Type', 'application/json');
          res.end(JSON.stringify({ success: true, ...result }));
        } catch (error) {
          res.setHeader('Content-Type', 'application/json');
          res.end(JSON.stringify({ success: false, error: String(error) }));
        }
        return true;
      }

      // DELETE /api/tools/sandbox/:sessionId - 删除沙箱
      if (subPath === '' && req.method === 'DELETE') {
        try {
          await sandboxManager.deleteSandbox(sessionId);
          res.setHeader('Content-Type', 'application/json');
          res.end(JSON.stringify({ success: true, message: 'Sandbox deleted' }));
        } catch (error) {
          res.setHeader('Content-Type', 'application/json');
          res.end(JSON.stringify({ success: false, error: String(error) }));
        }
        return true;
      }
    }

    return false;
  };
}

/**
 * 执行工具调用
 */
async function executeTool(
  sandboxManager: SandboxManager,
  request: ToolProxyRequest
): Promise<ToolProxyResponse> {
  const sandbox = sandboxManager.getSandbox(request.sessionId);
  if (!sandbox) {
    return { success: false, error: `Sandbox not found: ${request.sessionId}` };
  }

  try {
    switch (request.tool) {
      case 'terminal_run':
        return await executeTerminal(sandboxManager, request.sessionId, request.params.command as string);
      
      case 'read_file':
        return await readFile(sandboxManager, request.sessionId, request.params.path as string);
      
      case 'write_file':
        return await writeFile(
          sandboxManager,
          request.sessionId,
          request.params.path as string,
          request.params.content as string
        );
      
      case 'ls':
        return await listFiles(sandboxManager, request.sessionId, request.params.path as string || '.');
      
      case 'search':
        return await searchFiles(
          sandboxManager,
          request.sessionId,
          request.params.regex as string,
          request.params.glob as string
        );
      
      case 'git':
        return await executeGit(
          sandboxManager,
          request.sessionId,
          request.params.command as string,
          request.params.message as string
        );
      
      default:
        return { success: false, error: `Unknown tool: ${request.tool}` };
    }
  } catch (error) {
    return { success: false, error: String(error) };
  }
}

/**
 * 执行终端命令
 */
async function executeTerminal(
  sandboxManager: SandboxManager,
  sessionId: string,
  command: string,
  timeoutMs: number = 60000
): Promise<ToolProxyResponse> {
  const dangerousCommands = ['rm -rf /', 'mkfs', 'dd if=', ':(){:|:&};:', '> /dev/sda'];
  for (const dangerous of dangerousCommands) {
    if (command.includes(dangerous)) {
      return { success: false, error: `Dangerous command blocked: ${dangerous}` };
    }
  }

  try {
    const result = await sandboxManager.executeInSandbox(sessionId, command, { timeoutMs });
    let output = stripAnsi(result.stdout + result.stderr);
    
    if (output.length > 10000) {
      output = output.substring(0, 10000) + '\n... (output truncated)';
    }

    return {
      success: result.exitCode === 0,
      output: `Exit ${result.exitCode}\n${output}`,
      error: result.exitCode !== 0 ? `Exit code: ${result.exitCode}` : undefined
    };
  } catch (error) {
    return { success: false, error: String(error) };
  }
}

/**
 * 读取文件
 */
async function readFile(
  sandboxManager: SandboxManager,
  sessionId: string,
  filePath: string
): Promise<ToolProxyResponse> {
  if (filePath.includes('..') || filePath.startsWith('/etc') || filePath.startsWith('/root')) {
    return { success: false, error: 'Access denied' };
  }

  try {
    const sandbox = sandboxManager.getSandbox(sessionId);
    if (!sandbox) {
      return { success: false, error: 'Sandbox not found' };
    }

    const fullPath = `${sandbox.path}/${filePath}`;
    const file = Bun.file(fullPath);
    
    if (!(await file.exists())) {
      return { success: false, error: `File not found: ${filePath}` };
    }

    const content = await file.text();
    return { success: true, output: content };
  } catch (error) {
    return { success: false, error: String(error) };
  }
}

/**
 * 写入文件
 */
async function writeFile(
  sandboxManager: SandboxManager,
  sessionId: string,
  filePath: string,
  content: string
): Promise<ToolProxyResponse> {
  if (filePath.includes('..') || filePath.startsWith('/etc') || filePath.startsWith('/root')) {
    return { success: false, error: 'Access denied' };
  }

  try {
    const sandbox = sandboxManager.getSandbox(sessionId);
    if (!sandbox) {
      return { success: false, error: 'Sandbox not found' };
    }

    const fullPath = `${sandbox.path}/${filePath}`;
    
    // 确保目录存在
    const dir = fullPath.substring(0, fullPath.lastIndexOf('/'));
    await $`mkdir -p ${dir}`.quiet();

    await Bun.write(fullPath, content);
    return { success: true, output: `Written to ${filePath}` };
  } catch (error) {
    return { success: false, error: String(error) };
  }
}

/**
 * 列出目录
 */
async function listFiles(
  sandboxManager: SandboxManager,
  sessionId: string,
  path: string
): Promise<ToolProxyResponse> {
  if (path.includes('..') || path.startsWith('/etc') || path.startsWith('/root')) {
    return { success: false, error: 'Access denied' };
  }

  try {
    const result = await sandboxManager.executeInSandbox(sessionId, `ls -la ${path}`);
    return {
      success: result.exitCode === 0,
      output: stripAnsi(result.stdout),
      error: result.exitCode !== 0 ? stripAnsi(result.stderr) : undefined
    };
  } catch (error) {
    return { success: false, error: String(error) };
  }
}

/**
 * 搜索文件
 */
async function searchFiles(
  sandboxManager: SandboxManager,
  sessionId: string,
  regex: string,
  glob?: string
): Promise<ToolProxyResponse> {
  try {
    const globArg = glob ? `-g "${glob}"` : '';
    const result = await sandboxManager.executeInSandbox(
      sessionId,
      `rg --no-heading --line-number --max-count=200 ${globArg} "${regex}" .`
    );
    
    let output = stripAnsi(result.stdout);
    if (!output) {
      return { success: true, output: 'No matches found' };
    }
    
    return { success: true, output };
  } catch (error) {
    if (String(error).includes('exit code: 1')) {
      return { success: true, output: 'No matches found' };
    }
    return { success: false, error: String(error) };
  }
}

/**
 * 执行 Git 命令
 */
async function executeGit(
  sandboxManager: SandboxManager,
  sessionId: string,
  command: string,
  message?: string
): Promise<ToolProxyResponse> {
  const sandbox = sandboxManager.getSandbox(sessionId);
  if (!sandbox) {
    return { success: false, error: 'Sandbox not found' };
  }

  try {
    let fullCommand = `git ${command}`;
    
    if (command === 'commit') {
      fullCommand = `git ${command} -m "${message || 'Update'}"`;
    }

    const result = await sandboxManager.executeInSandbox(sessionId, fullCommand);
    return {
      success: result.exitCode === 0,
      output: stripAnsi(result.stdout + result.stderr),
      error: result.exitCode !== 0 ? stripAnsi(result.stderr) : undefined
    };
  } catch (error) {
    return { success: false, error: String(error) };
  }
}