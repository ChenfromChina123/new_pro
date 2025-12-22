package com.aispring.entity.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentState {
    private String sessionId;
    private AgentMeta meta;
    private WorldState worldState;
    private TaskState taskState;
    private AgentStatus status;
    private DecisionEnvelope lastDecision;
    private long version;
    private Instant updatedAt;
}
