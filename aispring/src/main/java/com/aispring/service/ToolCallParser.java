package com.aispring.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 工具调用解析器（参考 void-main 的 extractXMLToolsWrapper 和 parseXMLPrefixToToolCall）
 * 
 * 负责从 LLM 响应中解析 XML 格式的工具调用
 * 
 * @author AISpring Team
 * @since 2025-12-23
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ToolCallParser {
    
    private final ToolsService toolsService;
    
    /**
     * 解析的工具调用结果
     */
    public static class ParsedToolCall {
        private final String toolName;
        private final Map<String, Object> rawParams;
        private final boolean isComplete; // 是否完整（找到了闭合标签）
        private final String toolId;
        
        public ParsedToolCall(String toolName, Map<String, Object> rawParams, boolean isComplete, String toolId) {
            this.toolName = toolName;
            this.rawParams = rawParams;
            this.isComplete = isComplete;
            this.toolId = toolId;
        }
        
        public String getToolName() {
            return toolName;
        }
        
        public Map<String, Object> getRawParams() {
            return rawParams;
        }
        
        public boolean isComplete() {
            return isComplete;
        }
        
        public String getToolId() {
            return toolId;
        }
    }
    
    /**
     * 流式工具调用检测器（参考 void-main 的 extractXMLToolsWrapper）
     * 用于在流式响应中实时检测工具调用
     */
    public static class StreamingToolCallDetector {
        private final List<String> availableTools;
        private final Map<String, String> toolOpenTags; // toolName -> openTag
        private String fullText = "";
        private String plainText = ""; // 移除工具调用后的纯文本
        private String openToolTagBuffer = ""; // 部分写入的工具标签缓冲区
        private ParsedToolCall latestToolCall = null;
        private String foundToolName = null;
        private int toolStartIdx = -1;
        private final String toolId;
        
        public StreamingToolCallDetector(List<String> availableTools) {
            this.availableTools = availableTools;
            this.toolOpenTags = new HashMap<>();
            for (String toolName : availableTools) {
                toolOpenTags.put(toolName, "<" + toolName + ">");
            }
            this.toolId = UUID.randomUUID().toString();
        }
        
        /**
         * 处理新的文本片段（参考 void-main 的 newOnText）
         */
        public void appendText(String newText, ToolCallParser parser) {
            fullText += newText;
            
            // 如果还没有找到工具标签，尝试查找
            if (foundToolName == null) {
                String searchText = openToolTagBuffer + newText;
                
                // 检查是否有部分写入的标签
                boolean isPartial = false;
                for (String openTag : toolOpenTags.values()) {
                    if (searchText.endsWith("<") || 
                        (searchText.length() >= 2 && searchText.endsWith("<" + openTag.charAt(1))) ||
                        searchText.contains("<") && !searchText.contains(">")) {
                        isPartial = true;
                        break;
                    }
                }
                
                if (isPartial) {
                    openToolTagBuffer += newText;
                } else {
                    plainText += openToolTagBuffer;
                    openToolTagBuffer = "";
                    plainText += newText;
                    
                    // 查找工具标签
                    for (Map.Entry<String, String> entry : toolOpenTags.entrySet()) {
                        String toolName = entry.getKey();
                        String openTag = entry.getValue();
                        int idx = searchText.indexOf(openTag);
                        if (idx != -1) {
                            foundToolName = toolName;
                            toolStartIdx = fullText.length() - searchText.length() + idx;
                            plainText = plainText.substring(0, plainText.length() - searchText.length() + idx);
                            break;
                        }
                    }
                }
            }
            
            // 如果找到了工具标签，解析工具调用
            if (foundToolName != null) {
                latestToolCall = parser.parseXMLToolCall(foundToolName, fullText.substring(toolStartIdx));
            }
        }
        
        public String getPlainText() {
            return plainText.trim();
        }
        
        public ParsedToolCall getLatestToolCall() {
            return latestToolCall;
        }
        
        public boolean hasToolCall() {
            return latestToolCall != null && latestToolCall.isComplete();
        }
    }
    
    /**
     * 从流式文本中提取工具调用（参考 void-main 的 extractXMLToolsWrapper）
     * 
     * @param fullText LLM 的完整响应文本
     * @param availableTools 可用工具列表
     * @return 解析的工具调用，如果没有则返回 null
     */
    public ParsedToolCall extractToolCall(String fullText, List<String> availableTools) {
        if (fullText == null || fullText.isEmpty() || availableTools == null || availableTools.isEmpty()) {
            return null;
        }
        
        // 查找所有工具的开始标签
        String foundToolName = null;
        int toolStartIdx = -1;
        
        for (String toolName : availableTools) {
            String openTag = "<" + toolName + ">";
            int idx = fullText.indexOf(openTag);
            if (idx != -1 && (toolStartIdx == -1 || idx < toolStartIdx)) {
                toolStartIdx = idx;
                foundToolName = toolName;
            }
        }
        
        if (foundToolName == null || toolStartIdx == -1) {
            return null;
        }
        
        // 解析工具调用（参考 void-main 的 parseXMLPrefixToToolCall）
        return parseXMLToolCall(foundToolName, fullText.substring(toolStartIdx));
    }
    
    /**
     * 解析 XML 格式的工具调用（参考 void-main 的 parseXMLPrefixToToolCall）
     */
    ParsedToolCall parseXMLToolCall(String toolName, String xmlText) {
        String toolId = UUID.randomUUID().toString();
        Map<String, Object> params = new HashMap<>();
        boolean isComplete = false;
        
        // 查找工具标签
        String openTag = "<" + toolName + ">";
        String closeTag = "</" + toolName + ">";
        
        int openIdx = xmlText.indexOf(openTag);
        if (openIdx == -1) {
            return null;
        }
        
        int closeIdx = xmlText.indexOf(closeTag, openIdx);
        if (closeIdx != -1) {
            isComplete = true;
        }
        
        // 提取标签内容
        String content = xmlText.substring(openIdx + openTag.length(), 
                closeIdx != -1 ? closeIdx : xmlText.length());
        
        // 获取工具的参数定义
        ToolsService.ToolInfo toolInfo = toolsService.getToolInfo(toolName);
        if (toolInfo == null) {
            log.warn("工具信息不存在: {}", toolName);
            return new ParsedToolCall(toolName, params, isComplete, toolId);
        }
        
        Map<String, String> paramDefinitions = toolInfo.getParams();
        List<String> allowedParams = new ArrayList<>(paramDefinitions.keySet());
        
        // 解析参数（参考 void-main 的参数解析逻辑）
        for (String paramName : allowedParams) {
            String paramOpenTag = "<" + paramName + ">";
            String paramCloseTag = "</" + paramName + ">";
            
            int paramOpenIdx = content.indexOf(paramOpenTag);
            if (paramOpenIdx != -1) {
                int paramCloseIdx = content.indexOf(paramCloseTag, paramOpenIdx);
                if (paramCloseIdx != -1) {
                    String paramValue = content.substring(
                            paramOpenIdx + paramOpenTag.length(), 
                            paramCloseIdx
                    ).trim();
                    params.put(paramName, paramValue);
                } else {
                    // 参数未闭合，但可以提取部分值
                    String paramValue = content.substring(paramOpenIdx + paramOpenTag.length()).trim();
                    params.put(paramName, paramValue);
                }
            }
        }
        
        return new ParsedToolCall(toolName, params, isComplete, toolId);
    }
    
    /**
     * 从完整响应中提取纯文本（移除工具调用部分）
     * 参考 void-main 的 fullText 处理逻辑
     */
    public String extractPlainText(String fullText, List<String> availableTools) {
        if (fullText == null || fullText.isEmpty()) {
            return fullText;
        }
        
        // 查找第一个工具调用的位置
        int firstToolIdx = -1;
        for (String toolName : availableTools) {
            String openTag = "<" + toolName + ">";
            int idx = fullText.indexOf(openTag);
            if (idx != -1 && (firstToolIdx == -1 || idx < firstToolIdx)) {
                firstToolIdx = idx;
            }
        }
        
        if (firstToolIdx == -1) {
            return fullText.trim();
        }
        
        // 返回工具调用之前的部分
        return fullText.substring(0, firstToolIdx).trim();
    }
    
    /**
     * 格式化工具结果为 XML（参考 void-main 的 reParsedToolXMLString）
     * 格式：<toolName_result>...</toolName_result>
     */
    public String formatToolResult(String toolName, String result) {
        return String.format("<%s_result>\n%s\n</%s_result>", toolName, result, toolName);
    }
}

