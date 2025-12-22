package com.aispring.entity.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DecisionEnvelope {
    private String decisionId;
    private String type; // TOOL_CALL, TASK_COMPLETE, PAUSE, ERROR
    private String action;
    private Map<String, Object> params;
    private DecisionScope scope;
    private DecisionExpectation expectation;
    private long timeoutMs;
}
