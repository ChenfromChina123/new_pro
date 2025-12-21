package com.aispring.service;

import com.aispring.dto.response.TerminalCommandResponse;

public interface TerminalService {
    TerminalCommandResponse executeCommand(Long userId, String command, String relativeCwd);
    String getUserTerminalRoot(Long userId);
}
