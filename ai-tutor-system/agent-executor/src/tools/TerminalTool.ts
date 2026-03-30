import { $ } from 'bun';
import type { Tool, ToolResult } from './types';
import { stripAnsi } from '../utils/ansiStripper';

/**
 * 终端执行工具
 */
export class TerminalTool implements Tool {
  name = 'terminal_run';
  description = '执行终端命令';
  parameters = {
    command: {
      type: 'string',
      description: '要执行的命令',
      required: true
    },
    is_long_running: {
      type: 'boolean',
      description: '是否为长时间运行的任务',
      required: false
    }
  };

  private readonly timeoutMs = 60000;
  private readonly maxOutputLength = 10000;

  async execute(params: Record<string, unknown>, sandboxPath: string): Promise<ToolResult> {
    const command = params.command as string;
    const isLongRunning = params.is_long_running as boolean || false;

    if (!command) {
      return { success: false, output: 'Error: command is required' };
    }

    const dangerousCommands = ['rm -rf /', 'mkfs', 'dd if=', ':(){:|:&};:'];
    for (const dangerous of dangerousCommands) {
      if (command.includes(dangerous)) {
        return { success: false, output: `Error: Dangerous command blocked: ${dangerous}` };
      }
    }

    try {
      const timeout = isLongRunning ? this.timeoutMs * 2 : this.timeoutMs;

      const result = await $`${command}`.cwd(sandboxPath).timeout(timeout);

      let output = result.stdout.toString() + result.stderr.toString();
      output = stripAnsi(output);

      if (output.length > this.maxOutputLength) {
        const lines = output.split('\n');
        if (lines.length > 150) {
          const firstLines = lines.slice(0, 50).join('\n');
          const lastLines = lines.slice(-100).join('\n');
          output = `${firstLines}\n\n... (${lines.length - 150} lines omitted) ...\n\n${lastLines}`;
        } else {
          output = output.substring(0, this.maxOutputLength) + '\n... (output truncated)';
        }
      }

      const exitCode = result.exitCode;
      const status = exitCode === 0 ? 'Success' : `Exit Code: ${exitCode}`;

      return {
        success: exitCode === 0,
        output: `${status}\n${output}`
      };
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : String(error);
      return { success: false, output: `Error: ${errorMessage}` };
    }
  }
}
