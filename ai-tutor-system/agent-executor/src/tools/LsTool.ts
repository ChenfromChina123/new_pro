import { $ } from 'bun';
import type { Tool, ToolResult } from './types';

/**
 * 目录列表工具
 */
export class LsTool implements Tool {
  name = 'ls';
  description = '列出目录内容';
  parameters = {
    path: {
      type: 'string',
      description: '目录路径（相对路径）',
      required: false
    }
  };

  async execute(params: Record<string, unknown>, sandboxPath: string): Promise<ToolResult> {
    const path = params.path as string || '.';

    if (path.includes('..') || path.startsWith('/etc') || path.startsWith('/root')) {
      return { success: false, output: 'Error: Access denied - invalid path' };
    }

    try {
      const result = await $`ls -la ${path}`.cwd(sandboxPath);
      const output = result.stdout.toString();

      return { success: true, output };
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : String(error);
      return { success: false, output: `Error: ${errorMessage}` };
    }
  }
}
