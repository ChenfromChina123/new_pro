package com.aispring.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TerminalCommandResponse {
    private String stdout;
    private String stderr;
    private int exitCode;
    private String cwd; // Current working directory after execution
}
