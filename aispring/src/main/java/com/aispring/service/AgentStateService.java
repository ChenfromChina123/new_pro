package com.aispring.service;

import com.aispring.entity.agent.AgentState;
import com.aispring.entity.agent.AgentStatus;
import com.aispring.entity.agent.TaskState;

public interface AgentStateService {
    AgentState getAgentState(String sessionId, Long userId);
    void saveAgentState(AgentState state);
    void updateAgentStatus(String sessionId, AgentStatus status);
    void initializeAgentState(String sessionId, Long userId);
    void updateTaskState(String sessionId, TaskState taskState);
    AgentState getAgentStateByUserId(Long userId);
}
