package com.aispring.service;

import com.aispring.entity.agent.Task;
import com.aispring.entity.agent.TaskState;
import com.aispring.entity.agent.TaskStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskCompiler {

    private final ObjectMapper objectMapper;

    public TaskState compile(String llmOutput, String pipelineId) {
        try {
            // Assume llmOutput is the JSON array of tasks or the full object
            List<Task> tasks;
            String json = llmOutput;
            if (!json.trim().startsWith("[")) {
                 json = extractJson(llmOutput);
            }
            
            if (json == null || json.isEmpty()) {
                throw new RuntimeException("No JSON found in output");
            }
            
            tasks = objectMapper.readValue(json, new TypeReference<List<Task>>() {});

            // Normalize and Validate
            for (Task task : tasks) {
                if (task.getId() == null) task.setId(UUID.randomUUID().toString());
                if (task.getStatus() == null) task.setStatus(TaskStatus.PENDING);
                if (task.getSubsteps() != null) {
                    task.getSubsteps().forEach(sub -> {
                        if (sub.getId() == null) sub.setId(UUID.randomUUID().toString());
                        if (sub.getStatus() == null) sub.setStatus(TaskStatus.PENDING);
                    });
                }
            }

            return TaskState.builder()
                    .pipelineId(pipelineId)
                    .tasks(tasks)
                    .currentTaskId(tasks.isEmpty() ? null : tasks.get(0).getId())
                    .build();

        } catch (Exception e) {
            log.error("Failed to compile tasks", e);
            throw new RuntimeException("Task Compilation Failed: " + e.getMessage());
        }
    }

    private String extractJson(String text) {
        int start = text.indexOf("[");
        int end = text.lastIndexOf("]");
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return null;
    }
}
