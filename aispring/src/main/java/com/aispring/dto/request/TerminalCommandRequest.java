package com.aispring.dto.request;

import lombok.Data;

@Data
public class TerminalCommandRequest {
    private String command;
    private String cwd; // Optional: specific subdirectory, relative to user's root
    private String sessionId;
}
