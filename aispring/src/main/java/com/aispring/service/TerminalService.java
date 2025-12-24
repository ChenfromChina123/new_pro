package com.aispring.service;

import com.aispring.dto.response.TerminalCommandResponse;
import com.aispring.dto.response.TerminalFileDto;
import com.aispring.entity.terminal.FileSearchRequest;
import com.aispring.entity.terminal.FileContextRequest;
import com.aispring.entity.terminal.FileModifyRequest;

import java.util.List;

public interface TerminalService {
    TerminalCommandResponse executeCommand(Long userId, String command, String relativeCwd);
    String getUserTerminalRoot(Long userId);
    TerminalCommandResponse writeFile(Long userId, String relativePath, String content, String relativeCwd, boolean overwrite);
    List<TerminalFileDto> listFiles(Long userId, String relativePath);
    String readFile(Long userId, String relativePath);
    
    // 新增工具方法
    TerminalCommandResponse searchFiles(Long userId, FileSearchRequest request, String relativeCwd);
    TerminalCommandResponse readFileContext(Long userId, FileContextRequest request, String relativeCwd);
    TerminalCommandResponse modifyFile(Long userId, FileModifyRequest request, String relativeCwd);
    
    // Phase 4: ToolsService 需要的辅助方法
    /**
     * 列出目录内容（返回文件名列表）
     */
    List<String> listDirectory(Long userId, String relativePath, String relativeCwd);
    
    /**
     * 获取目录树
     */
    String getDirectoryTree(Long userId, String relativePath, String relativeCwd);
    
    /**
     * 创建目录
     */
    void createDirectory(Long userId, String relativePath, String relativeCwd);
    
    /**
     * 删除文件或文件夹
     */
    void deleteFileOrFolder(Long userId, String relativePath, String relativeCwd);
    
    /**
     * 搜索文件名
     */
    List<String> searchFileNames(Long userId, String pattern, String relativeCwd);
    
    /**
     * 在文件中搜索内容
     */
    java.util.Map<String, List<String>> searchInFiles(Long userId, String pattern, String relativeCwd);
}
