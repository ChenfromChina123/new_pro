package com.aispring.entity.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolResult {
    private String decisionId;
    private int exitCode;
    private String stdout;
    private String stderr;
    private List<String> artifacts;
}
