import { $ } from 'bun';
import type { Tool, ToolResult } from './types';

/**
 * Git 操作工具
 */
export class GitTool implements Tool {
  name = 'undo_last_action';
  description = '撤销上一步操作（通过 Git 回滚）';
  parameters = {};

  async execute(params: Record<string, unknown>, sandboxPath: string): Promise<ToolResult> {
    try {
      await $`git reset --hard HEAD^`.cwd(sandboxPath);

      const result = await $`git log -1 --oneline`.cwd(sandboxPath);
      const currentCommit = result.text().trim();

      return {
        success: true,
        output: `Successfully rolled back to: ${currentCommit}`
      };
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : String(error);
      return { success: false, output: `Error: ${errorMessage}` };
    }
  }
}
