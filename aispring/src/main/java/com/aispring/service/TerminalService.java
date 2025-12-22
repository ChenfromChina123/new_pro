package com.aispring.service;

import com.aispring.dto.response.TerminalCommandResponse;
import com.aispring.dto.response.TerminalFileDto;
import java.util.List;

public interface TerminalService {
    TerminalCommandResponse executeCommand(Long userId, String command, String relativeCwd);
    String getUserTerminalRoot(Long userId);
    TerminalCommandResponse writeFile(Long userId, String relativePath, String content, String relativeCwd, boolean overwrite);
    List<TerminalFileDto> listFiles(Long userId, String relativePath);
    String readFile(Long userId, String relativePath);
}
