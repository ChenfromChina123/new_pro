import type { Tool, ToolResult } from './types';

/**
 * 文件读取工具
 */
export class FileReadTool implements Tool {
  name = 'read_file';
  description = '分页读取文件内容';
  parameters = {
    path: {
      type: 'string',
      description: '文件路径（相对路径）',
      required: true
    },
    start_line: {
      type: 'number',
      description: '起始行号（从1开始）',
      required: true
    },
    end_line: {
      type: 'number',
      description: '结束行号',
      required: true
    }
  };
  
  private readonly maxLines = 500;
  
  async execute(params: Record<string, unknown>, sandboxPath: string): Promise<ToolResult> {
    const path = params.path as string;
    const startLine = params.start_line as number || 1;
    const endLine = params.end_line as number || 100;
    
    if (!path) {
      return { success: false, output: 'Error: path is required' };
    }
    
    if (path.includes('..') || path.startsWith('/etc') || path.startsWith('/root')) {
      return { success: false, output: 'Error: Access denied - invalid path' };
    }
    
    const lineCount = endLine - startLine + 1;
    if (lineCount > this.maxLines) {
      return { success: false, output: `Error: Cannot read more than ${this.maxLines} lines at once` };
    }
    
    try {
      const fullPath = `${sandboxPath}/${path}`;
      const file = Bun.file(fullPath);
      
      if (!(await file.exists())) {
        return { success: false, output: `Error: File not found: ${path}` };
      }
      
      const content = await file.text();
      const lines = content.split('\n');
      
      const selectedLines = lines.slice(startLine - 1, endLine);
      
      let output = `File: ${path} (Lines ${startLine}-${Math.min(endLine, lines.length)} of ${lines.length})\n\n`;
      
      selectedLines.forEach((line, index) => {
        const lineNum = startLine + index;
        output += `${lineNum.toString().padStart(6)}→${line}\n`;
      });
      
      return { success: true, output };
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : String(error);
      return { success: false, output: `Error: ${errorMessage}` };
    }
  }
}
