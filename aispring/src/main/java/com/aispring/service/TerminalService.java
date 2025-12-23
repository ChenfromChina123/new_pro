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
}
