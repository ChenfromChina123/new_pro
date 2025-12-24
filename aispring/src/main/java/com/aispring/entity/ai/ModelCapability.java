package com.aispring.entity.ai;

import lombok.Builder;
import lombok.Data;

/**
 * 模型能力定义（参考 void-main 的 VoidStaticModelInfo）
 * 
 * 描述模型支持的功能特性
 * 
 * @author AISpring Team
 * @since 2025-12-23
 */
@Data
@Builder
public class ModelCapability {
    /**
     * 上下文窗口大小（输入 token 数）
     */
    @Builder.Default
    private Integer contextWindow = 4096;
    
    /**
     * 为输出保留的 token 空间
     */
    @Builder.Default
    private Integer reservedOutputTokenSpace = 4096;
    
    /**
     * 是否支持系统消息
     * false: 不支持
     * system-role: 使用 system role
     * developer-role: 使用 developer role
     * separated: 作为独立字段传递
     */
    @Builder.Default
    private String supportsSystemMessage = "system-role";
    
    /**
     * 特殊工具格式
     * null: 使用 XML 格式（默认）
     * openai-style: OpenAI 函数调用格式
     * anthropic-style: Anthropic 工具使用格式
     * gemini-style: Gemini 函数调用格式
     */
    private String specialToolFormat;
    
    /**
     * 是否支持 FIM (Fill-In-The-Middle)
     * 用于代码编辑功能（Codex）
     */
    @Builder.Default
    private Boolean supportsFIM = false;
    
    /**
     * 是否支持推理模式（Reasoning）
     */
    @Builder.Default
    private Boolean supportsReasoning = false;
    
    /**
     * 是否可以关闭推理模式
     */
    @Builder.Default
    private Boolean canTurnOffReasoning = false;
    
    /**
     * 是否可以输出推理过程
     */
    @Builder.Default
    private Boolean canIOReasoning = false;
    
    /**
     * 默认能力配置（适用于大多数模型）
     */
    public static ModelCapability defaultCapability() {
        return ModelCapability.builder()
                .contextWindow(4096)
                .reservedOutputTokenSpace(4096)
                .supportsSystemMessage("system-role")
                .supportsFIM(false)
                .supportsReasoning(false)
                .build();
    }
    
    /**
     * DeepSeek Chat 模型能力
     */
    public static ModelCapability deepseekChat() {
        return ModelCapability.builder()
                .contextWindow(64000)
                .reservedOutputTokenSpace(4096)
                .supportsSystemMessage("system-role")
                .supportsFIM(false)
                .supportsReasoning(false)
                .build();
    }
    
    /**
     * DeepSeek Reasoner 模型能力
     */
    public static ModelCapability deepseekReasoner() {
        return ModelCapability.builder()
                .contextWindow(64000)
                .reservedOutputTokenSpace(4096)
                .supportsSystemMessage("system-role")
                .supportsFIM(false)
                .supportsReasoning(true)
                .canTurnOffReasoning(false)
                .canIOReasoning(true)
                .build();
    }
    
    /**
     * 支持 FIM 的模型能力（用于 Codex 功能）
     */
    public static ModelCapability fimCapable() {
        return ModelCapability.builder()
                .contextWindow(32000)
                .reservedOutputTokenSpace(4096)
                .supportsSystemMessage("system-role")
                .supportsFIM(true)
                .supportsReasoning(false)
                .build();
    }
}

