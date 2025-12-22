package com.aispring.service;

import com.aispring.entity.agent.AgentState;
import com.aispring.entity.agent.Task;
import com.aispring.entity.agent.TaskState;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AgentPromptBuilder {

    private final ObjectMapper objectMapper;

    public String buildPromptContext(AgentState state) {
        try {
            // Lightweight Snapshot
            StringBuilder sb = new StringBuilder();
            sb.append("# World State\n");
            sb.append("Project Root: ").append(state.getWorldState().getProjectRoot()).append("\n");
            sb.append("Tracked Files: ").append(state.getWorldState().getTrackedPaths()).append("\n");
            
            sb.append("\n# Task State\n");
            TaskState ts = state.getTaskState();
            if (ts != null) {
                sb.append("Current Task ID: ").append(ts.getCurrentTaskId()).append("\n");
                sb.append("Tasks:\n");
                for (Task t : ts.getTasks()) {
                    sb.append("- [").append(t.getStatus()).append("] ").append(t.getName())
                      .append(" (ID: ").append(t.getId()).append(")\n");
                    if (t.getId().equals(ts.getCurrentTaskId())) {
                         sb.append("  GOAL: ").append(t.getGoal()).append("\n");
                         if (t.getSubsteps() != null) {
                             t.getSubsteps().forEach(sub -> 
                                 sb.append("  - [").append(sub.getStatus()).append("] ").append(sub.getGoal()).append("\n")
                             );
                         }
                    }
                }
            }
            
            return sb.toString();
        } catch (Exception e) {
            return "Error building context: " + e.getMessage();
        }
    }
    
    public String buildJsonSnapshot(AgentState state) {
         try {
            return objectMapper.writeValueAsString(state); 
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
