package com.aispring.service;

import com.aispring.entity.agent.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class StateMutator {
    
    private final ObjectMapper objectMapper;

    public MutatorResult applyToolResult(AgentState state, ToolResult result) {
        // 1. Check if we are waiting for a tool result
        if (state.getStatus() != AgentStatus.WAITING_TOOL) {
             return MutatorResult.builder()
                    .accepted(false)
                    .reason("当前 Agent 不在等待工具结果状态，当前状态：" + state.getStatus())
                    .newAgentStatus(state.getStatus())
                    .build();
        }

        DecisionEnvelope lastDecision = state.getLastDecision();
        if (lastDecision == null) {
             return MutatorResult.builder()
                    .accepted(false)
                    .reason("状态中没有待处理的决策")
                    .newAgentStatus(AgentStatus.ERROR)
                    .build();
        }

        // 2. Validate Decision ID
        if (!lastDecision.getDecisionId().equals(result.getDecisionId())) {
            return MutatorResult.builder()
                    .accepted(false)
                    .reason("决策ID不匹配，期望：" + lastDecision.getDecisionId() + "，实际：" + result.getDecisionId())
                    .newAgentStatus(state.getStatus())
                    .build();
        }

        // 3. Check Tool Execution Status
        if (result.getExitCode() != 0) {
            // Tool failed
            state.setStatus(AgentStatus.ERROR);
            // We might want to keep lastDecision to allow retry? 
            // For now, we transition to ERROR.
            return MutatorResult.builder()
                    .accepted(true) // Accepted the failure report
                    .reason("工具执行失败，退出码：" + result.getExitCode())
                    .newAgentStatus(AgentStatus.ERROR)
                    .build();
        }

        // 4. Update World State (Tracked Paths)
        if (result.getArtifacts() != null) {
            for (String path : result.getArtifacts()) {
                if (state.getWorldState().getTrackedPaths() != null) {
                    state.getWorldState().getTrackedPaths().add(path);
                    
                    FileMeta meta = FileMeta.builder()
                            .path(path)
                            .source(FileSource.AGENT)
                            .lastModified(System.currentTimeMillis())
                            .build();
                    state.getWorldState().getFileSystem().put(path, meta);
                }
            }
        }

        // 5. Update Task State if needed
        // If the decision was explicitly TASK_COMPLETE, handle it. 
        // But usually TASK_COMPLETE is a decision type, not a tool call.
        // If we are here, it was a TOOL_CALL that finished.
        
        state.setUpdatedAt(Instant.now());
        
        // Clear last decision as it is completed
        state.setLastDecision(null);
        
        // Transition back to RUNNING to continue the loop
        state.setStatus(AgentStatus.RUNNING);

        return MutatorResult.builder()
                .accepted(true)
                .reason("执行成功")
                .newAgentStatus(AgentStatus.RUNNING)
                .build();
    }
    
    // Separate method to handle TASK_COMPLETE decision directly (no tool result needed usually)
    public void markTaskComplete(AgentState state) {
        updateTaskProgress(state);
    }

    private void updateTaskProgress(AgentState state) {
        TaskState ts = state.getTaskState();
        if (ts == null || ts.getCurrentTaskId() == null) return;

        // Find current task
        for (int i = 0; i < ts.getTasks().size(); i++) {
            Task t = ts.getTasks().get(i);
            if (t.getId().equals(ts.getCurrentTaskId())) {
                t.setStatus(TaskStatus.DONE);
                // Move to next
                if (i + 1 < ts.getTasks().size()) {
                    ts.setCurrentTaskId(ts.getTasks().get(i + 1).getId());
                    state.setStatus(AgentStatus.RUNNING);
                } else {
                    ts.setCurrentTaskId(null);
                    state.setStatus(AgentStatus.COMPLETED);
                }
                break;
            }
        }
    }
}
