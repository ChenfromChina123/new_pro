package com.aispring.service.impl;

import com.aispring.entity.agent.*;
import com.aispring.service.AgentStateService;
import com.aispring.service.TerminalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentStateServiceImpl implements AgentStateService {

    private final TerminalService terminalService;
    
    // In-memory storage for now. In production, this should be Redis or DB.
    private final Map<String, AgentState> stateStore = new ConcurrentHashMap<>();

    @Override
    public AgentState getAgentState(String sessionId, Long userId) {
        String key = (sessionId == null || sessionId.isBlank()) ? ("user-" + userId) : sessionId;
        AgentState existing = stateStore.get(key);
        if (existing != null) return existing;

        AgentState created = buildInitialAgentState(key, userId);
        AgentState raced = stateStore.putIfAbsent(key, created);
        return raced != null ? raced : created;
    }

    @Override
    public void saveAgentState(AgentState state) {
        state.setUpdatedAt(Instant.now());
        state.setVersion(state.getVersion() + 1);
        stateStore.put(state.getSessionId(), state);
    }

    @Override
    public void updateAgentStatus(String sessionId, AgentStatus status) {
        AgentState state = stateStore.get(sessionId);
        if (state != null) {
            // Strict FSM Check can be added here
            state.setStatus(status);
            saveAgentState(state);
        }
    }

    @Override
    public void initializeAgentState(String sessionId, Long userId) {
        String key = (sessionId == null || sessionId.isBlank()) ? ("user-" + userId) : sessionId;
        stateStore.put(key, buildInitialAgentState(key, userId));
    }

    private AgentState buildInitialAgentState(String sessionId, Long userId) {
        String userRoot = terminalService.getUserTerminalRoot(userId);

        return AgentState.builder()
                .sessionId(sessionId)
                .meta(AgentMeta.builder()
                        .agentId("ai_terminal_assistant")
                        .version("1.0.0")
                        .mode("autonomous_engineering")
                        .build())
                .worldState(WorldState.builder()
                        .projectRoot(userRoot)
                        .fileSystem(new ConcurrentHashMap<>())
                        .trackedPaths(Collections.newSetFromMap(new ConcurrentHashMap<>()))
                        .services(new ConcurrentHashMap<>())
                        .build())
                .taskState(TaskState.builder()
                        .tasks(new ArrayList<>())
                        .build())
                .status(AgentStatus.IDLE)
                .version(0)
                .updatedAt(Instant.now())
                .build();
    }

    @Override
    public void updateTaskState(String sessionId, TaskState taskState) {
        AgentState state = stateStore.get(sessionId);
        if (state != null) {
            state.setTaskState(taskState);
            saveAgentState(state);
        }
    }

    @Override
    public AgentState getAgentStateByUserId(Long userId) {
        // This is a simplification. In reality, we need to know the active session.
        // For now, we return the first found state or create a default one.
        return stateStore.values().stream().findFirst().orElse(null);
    }
}
