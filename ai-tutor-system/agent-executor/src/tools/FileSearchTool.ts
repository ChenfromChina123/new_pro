import { $ } from 'bun';
import type { Tool, ToolResult } from './types';

/**
 * 文件搜索工具
 */
export class FileSearchTool implements Tool {
  name = 'search_in_files';
  description = '在文件中搜索内容';
  parameters = {
    regex: {
      type: 'string',
      description: '搜索的正则表达式',
      required: true
    },
    glob: {
      type: 'string',
      description: '文件匹配模式（如 **/*.ts）',
      required: false
    },
    max_matches: {
      type: 'number',
      description: '最大匹配数',
      required: false
    }
  };
  
  private readonly maxMatches = 200;
  
  async execute(params: Record<string, unknown>, sandboxPath: string): Promise<ToolResult> {
    const regex = params.regex as string;
    const glob = params.glob as string || '**/*';
    const maxMatches = (params.max_matches as number) || this.maxMatches;
    
    if (!regex) {
      return { success: false, output: 'Error: regex is required' };
    }
    
    try {
      const result = await $`rg --no-heading --line-number --max-count=${maxMatches} -g ${glob} ${regex} .`
        .cwd(sandboxPath)
        .quiet();
      
      let output = result.stdout.toString();
      
      if (!output) {
        return { success: true, output: 'No matches found' };
      }
      
      const lines = output.split('\n');
      if (lines.length > maxMatches) {
        output = lines.slice(0, maxMatches).join('\n');
        output += `\n\n... (${lines.length - maxMatches} more matches, showing first ${maxMatches})`;
      }
      
      return { success: true, output };
    } catch (error) {
      if (error instanceof Error && error.message.includes('exit code: 1')) {
        return { success: true, output: 'No matches found' };
      }
      const errorMessage = error instanceof Error ? error.message : String(error);
      return { success: false, output: `Error: ${errorMessage}` };
    }
  }
}
