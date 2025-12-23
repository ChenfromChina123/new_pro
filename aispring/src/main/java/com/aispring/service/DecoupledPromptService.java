package com.aispring.service;

import com.aispring.entity.agent.AgentState;
import com.aispring.entity.agent.Task;
import com.aispring.entity.agent.TaskState;
import com.aispring.service.decoupling.IdentityManager;
import com.aispring.service.decoupling.InformationManager;
import com.aispring.service.decoupling.PromptCompiler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 解耦的 Prompt 服务 - 使用 4 个解耦系统构建 Prompt
 * 
 * 替代原有的 TerminalPromptManager 和 AgentPromptBuilder
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DecoupledPromptService {

    private final PromptCompiler promptCompiler;
    private final IdentityManager identityManager;
    private final InformationManager informationManager;
    private final ObjectMapper objectMapper;

    /**
     * 构建执行器 Prompt（使用解耦系统）
     */
    public String buildExecutorPrompt(AgentState state, String userInput) {
        // 1. Information: 从 AgentState 构建状态切片
        buildStateSlices(state, userInput);

        // 2. Identity: 设置任务身份
        if (state.getTaskState() != null && state.getTaskState().getCurrentTaskId() != null) {
            TaskState ts = state.getTaskState();
            Task currentTask = ts.getTasks().stream()
                    .filter(t -> t.getId().equals(ts.getCurrentTaskId()))
                    .findFirst()
                    .orElse(null);
            
            if (currentTask != null) {
                IdentityManager.TaskIdentity taskIdentity = new IdentityManager.TaskIdentity();
                taskIdentity.setTaskId(currentTask.getId());
                taskIdentity.setRole(PromptCompiler.TaskRole.EXECUTOR);
                taskIdentity.setGoal(currentTask.getGoal());
                identityManager.setTask(taskIdentity);
            }
        }

        // 3. Information: 获取当前状态
        Map<String, Object> currentState = informationManager.getCurrentState();
        String contextJson;
        try {
            contextJson = objectMapper.writeValueAsString(currentState);
        } catch (Exception e) {
            contextJson = "{}";
        }

        // 4. Prompt: 构建配置
        PromptCompiler.PromptConfig config = PromptCompiler.PromptConfig.builder()
                .identity(PromptCompiler.IdentityType.IDE_ENGINEER)
                .role(PromptCompiler.TaskRole.EXECUTOR)
                .objective("完成当前任务")
                .constraints(List.of("保持行为不变", "不添加新依赖"))
                .bias(List.of(
                        PromptCompiler.BehavioralBias.MINIMAL_CHANGE,
                        PromptCompiler.BehavioralBias.AVOID_SPECULATION
                ))
                .context(buildContextString(state, contextJson))
                .build();

        // 5. Prompt: 编译为最终 Prompt
        return promptCompiler.render(config);
    }

    /**
     * 构建规划器 Prompt
     */
    public String buildPlannerPrompt(String userPrompt) {
        PromptCompiler.PromptConfig config = PromptCompiler.PromptConfig.builder()
                .identity(PromptCompiler.IdentityType.IDE_ENGINEER)
                .role(PromptCompiler.TaskRole.PLANNER)
                .objective("分析用户意图，生成结构化的任务流水线")
                .constraints(List.of("任务必须是工程可执行步骤", "避免空泛表述"))
                .bias(List.of(PromptCompiler.BehavioralBias.AVOID_SPECULATION))
                .context("用户意图：" + userPrompt)
                .build();

        return promptCompiler.render(config);
    }

    /**
     * 构建对话 Prompt
     */
    public String buildChatPrompt(AgentState state, String userInput) {
        buildStateSlices(state, userInput);
        Map<String, Object> currentState = informationManager.getCurrentState();
        
        String contextJson;
        try {
            contextJson = objectMapper.writeValueAsString(currentState);
        } catch (Exception e) {
            contextJson = "{}";
        }

        PromptCompiler.PromptConfig config = PromptCompiler.PromptConfig.builder()
                .identity(PromptCompiler.IdentityType.TERMINAL_ASSISTANT)
                .role(PromptCompiler.TaskRole.CHATTER)
                .objective("回答用户的问题，提供工程建议")
                .constraints(List.of("保持专业、简洁、准确", "优先使用中文回复"))
                .context(contextJson)
                .build();

        return promptCompiler.render(config);
    }

    /**
     * 构建状态切片
     */
    private void buildStateSlices(AgentState state, String userInput) {
        informationManager.clearSlices();

        // 用户输入切片
        if (userInput != null && !userInput.isEmpty()) {
            InformationManager.StateSlice inputSlice = new InformationManager.StateSlice();
            inputSlice.setSource(InformationManager.InformationSource.USER_INPUT);
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("text", userInput);
            inputSlice.setData(inputData);
            informationManager.addSlice(inputSlice);
        }

        // 文件系统切片
        if (state.getWorldState() != null && state.getWorldState().getFileSystem() != null) {
            InformationManager.StateSlice fileSlice = new InformationManager.StateSlice();
            fileSlice.setSource(InformationManager.InformationSource.FILE_SYSTEM);
            Map<String, Object> fileData = new HashMap<>();
            state.getWorldState().getFileSystem().forEach((path, meta) -> {
                fileData.put(path, Map.of(
                        "source", meta.getSource().name(),
                        "lastModified", meta.getLastModified()
                ));
            });
            fileSlice.setData(fileData);
            informationManager.addSlice(fileSlice);
        }

        // Agent 状态切片
        InformationManager.StateSlice agentSlice = new InformationManager.StateSlice();
        agentSlice.setSource(InformationManager.InformationSource.AGENT_STATE);
        Map<String, Object> agentData = new HashMap<>();
        agentData.put("status", state.getStatus().name());
        agentData.put("projectRoot", state.getWorldState() != null ? 
                state.getWorldState().getProjectRoot() : null);
        agentData.put("trackedPaths", state.getWorldState() != null && 
                state.getWorldState().getTrackedPaths() != null ?
                new ArrayList<>(state.getWorldState().getTrackedPaths()) : new ArrayList<>());
        agentSlice.setData(agentData);
        informationManager.addSlice(agentSlice);
    }

    /**
     * 构建上下文字符串（包含任务信息）
     */
    private String buildContextString(AgentState state, String stateJson) {
        StringBuilder sb = new StringBuilder();
        sb.append("# 世界状态\n");
        sb.append(stateJson).append("\n\n");

        if (state.getTaskState() != null) {
            sb.append("# 任务状态\n");
            TaskState ts = state.getTaskState();
            if (ts.getCurrentTaskId() != null) {
                sb.append("当前任务ID：").append(ts.getCurrentTaskId()).append("\n");
                Task currentTask = ts.getTasks().stream()
                        .filter(t -> t.getId().equals(ts.getCurrentTaskId()))
                        .findFirst()
                        .orElse(null);
                if (currentTask != null) {
                    sb.append("任务名称：").append(currentTask.getName()).append("\n");
                    sb.append("任务目标：").append(currentTask.getGoal()).append("\n");
                }
            }
        }

        return sb.toString();
    }
}

