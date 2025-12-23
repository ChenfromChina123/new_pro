package com.aispring.service.decoupling;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

/**
 * 信息管理器 - 管理状态切片
 * 
 * 核心思想：
 * - Information ≠ Memory
 * - Information = Reconstructable State
 * - 只给模型它"应该看到的世界"
 */
@Component
public class InformationManager {

    public enum InformationSource {
        FILE_SYSTEM,
        CURSOR_POSITION,
        USER_INPUT,
        AST_SYMBOL,
        AGENT_STATE
    }

    @Data
    public static class StateSlice {
        private InformationSource source;
        private List<String> scope = new ArrayList<>();
        private Map<String, Object> data = new HashMap<>();
        private Instant timestamp = Instant.now();
        private String authority = "fact"; // "fact" | "model_output"
    }

    private final List<StateSlice> slices = new ArrayList<>();
    private List<String> visibleFiles = new ArrayList<>();
    private List<String> visibleFunctions = new ArrayList<>();

    /**
     * 添加状态切片
     */
    public void addSlice(StateSlice slice) {
        slices.add(slice);
    }

    /**
     * 获取当前状态 - 从切片重构
     */
    public Map<String, Object> getCurrentState() {
        Map<String, Object> state = new HashMap<>();
        state.put("files", new HashMap<>());
        state.put("cursor", null);
        state.put("user_input", null);
        state.put("symbols", new HashMap<>());
        state.put("agent_state", new HashMap<>());

        for (StateSlice slice : slices) {
            switch (slice.getSource()) {
                case FILE_SYSTEM:
                    @SuppressWarnings("unchecked")
                    Map<String, Object> files = (Map<String, Object>) state.get("files");
                    if (slice.getData() != null) {
                        files.putAll(slice.getData());
                    }
                    break;
                case CURSOR_POSITION:
                    state.put("cursor", slice.getData());
                    break;
                case USER_INPUT:
                    state.put("user_input", slice.getData());
                    break;
                case AST_SYMBOL:
                    @SuppressWarnings("unchecked")
                    Map<String, Object> symbols = (Map<String, Object>) state.get("symbols");
                    if (slice.getData() != null) {
                        symbols.putAll(slice.getData());
                    }
                    break;
                case AGENT_STATE:
                    @SuppressWarnings("unchecked")
                    Map<String, Object> agentState = (Map<String, Object>) state.get("agent_state");
                    if (slice.getData() != null) {
                        agentState.putAll(slice.getData());
                    }
                    break;
            }
        }

        return state;
    }

    /**
     * 设置作用域 - 只给模型应该看到的世界
     */
    public void setScope(List<String> visibleFiles, List<String> visibleFunctions) {
        this.visibleFiles = visibleFiles != null ? new ArrayList<>(visibleFiles) : new ArrayList<>();
        this.visibleFunctions = visibleFunctions != null ? new ArrayList<>(visibleFunctions) : new ArrayList<>();
    }

    /**
     * 根据作用域过滤状态
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> filterByScope(Map<String, Object> state) {
        Map<String, Object> filtered = new HashMap<>(state);

        // 只保留可见文件
        if (!visibleFiles.isEmpty()) {
            Map<String, Object> files = (Map<String, Object>) filtered.get("files");
            if (files != null) {
                Map<String, Object> filteredFiles = new HashMap<>();
                for (String file : visibleFiles) {
                    if (files.containsKey(file)) {
                        filteredFiles.put(file, files.get(file));
                    }
                }
                filtered.put("files", filteredFiles);
            }
        }

        // 只保留可见函数
        if (!visibleFunctions.isEmpty()) {
            Map<String, Object> symbols = (Map<String, Object>) filtered.get("symbols");
            if (symbols != null) {
                Map<String, Object> filteredSymbols = new HashMap<>();
                for (String func : visibleFunctions) {
                    if (symbols.containsKey(func)) {
                        filteredSymbols.put(func, symbols.get(func));
                    }
                }
                filtered.put("symbols", filteredSymbols);
            }
        }

        return filtered;
    }

    /**
     * 清空所有切片（用于重置状态）
     */
    public void clearSlices() {
        slices.clear();
    }
}

