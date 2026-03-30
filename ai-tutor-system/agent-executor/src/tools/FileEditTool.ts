import type { Tool, ToolResult } from './types';

/**
 * 文件编辑工具（精确行号锚点）
 */
export class FileEditTool implements Tool {
  name = 'edit_file_by_anchor';
  description = '通过锚点精确编辑文件';
  parameters = {
    path: {
      type: 'string',
      description: '文件路径（相对路径）',
      required: true
    },
    anchor: {
      type: 'string',
      description: '目标行起始的10个关键字符',
      required: true
    },
    range: {
      type: 'array',
      description: '[起始行, 结束行]',
      required: true
    },
    content: {
      type: 'string',
      description: '要替换的内容',
      required: true
    }
  };
  
  async execute(params: Record<string, unknown>, sandboxPath: string): Promise<ToolResult> {
    const path = params.path as string;
    const anchor = params.anchor as string;
    const range = params.range as [number, number];
    const content = params.content as string;
    
    if (!path || !anchor || !range || content === undefined) {
      return { success: false, output: 'Error: Missing required parameters' };
    }
    
    if (path.includes('..') || path.startsWith('/etc') || path.startsWith('/root')) {
      return { success: false, output: 'Error: Access denied - invalid path' };
    }
    
    try {
      const fullPath = `${sandboxPath}/${path}`;
      const file = Bun.file(fullPath);
      
      if (!(await file.exists())) {
        return { success: false, output: `Error: File not found: ${path}` };
      }
      
      const fileContent = await file.text();
      const lines = fileContent.split('\n');
      
      const [startLine, endLine] = range;
      const targetLine = lines[startLine - 1];
      
      if (!targetLine) {
        return { success: false, output: `Error: Line ${startLine} not found` };
      }
      
      if (!targetLine.startsWith(anchor)) {
        const fuzzyResult = this.fuzzyMatch(lines, anchor, startLine);
        if (fuzzyResult) {
          return fuzzyResult;
        }
        return { 
          success: false, 
          output: `Error: Anchor mismatch. Expected "${anchor}" at line ${startLine}, found "${targetLine.substring(0, 10)}"` 
        };
      }
      
      const newLines = [...lines];
      const linesToDelete = endLine - startLine + 1;
      newLines.splice(startLine - 1, linesToDelete, content);
      
      const newContent = newLines.join('\n');
      await Bun.write(fullPath, newContent);
      
      return {
        success: true,
        output: `Successfully edited ${path}: replaced lines ${startLine}-${endLine}`
      };
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : String(error);
      return { success: false, output: `Error: ${errorMessage}` };
    }
  }
  
  /**
   * 模糊匹配锚点
   */
  private fuzzyMatch(lines: string[], anchor: string, targetLine: number): ToolResult | null {
    const searchRange = 10;
    const start = Math.max(0, targetLine - searchRange - 1);
    const end = Math.min(lines.length, targetLine + searchRange);
    
    for (let i = start; i < end; i++) {
      if (lines[i].startsWith(anchor)) {
        return {
          success: false,
          output: `Hint: Anchor found at line ${i + 1} instead of ${targetLine}. Please adjust the range.`
        };
      }
    }
    
    return null;
  }
}
