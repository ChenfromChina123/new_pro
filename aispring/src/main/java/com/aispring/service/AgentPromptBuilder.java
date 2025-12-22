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
            sb.append("# 世界状态\n");
            sb.append("项目根目录：").append(state.getWorldState().getProjectRoot()).append("\n");
            sb.append("已追踪文件：").append(state.getWorldState().getTrackedPaths()).append("\n");
            
            sb.append("\n# 任务状态\n");
            TaskState ts = state.getTaskState();
            if (ts != null) {
                sb.append("当前任务ID：").append(ts.getCurrentTaskId()).append("\n");
                sb.append("任务列表：\n");
                for (Task t : ts.getTasks()) {
                    sb.append("- [").append(t.getStatus()).append("] ").append(t.getName())
                      .append(" (ID: ").append(t.getId()).append(")\n");
                    if (t.getId().equals(ts.getCurrentTaskId())) {
                         sb.append("  目标：").append(t.getGoal()).append("\n");
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
            return "构建上下文失败：" + e.getMessage();
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
