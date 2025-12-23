package com.aispring.entity.terminal;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import java.util.List;

/**
 * 读取文件上下文请求（支持批量）
 */
@Data
public class FileContextRequest {
    /**
     * 文件读取列表
     */
    private List<FileRange> files;
    
    @Data
    public static class FileRange {
        /**
         * 文件路径
         */
        @NotBlank(message = "文件路径不能为空")
        private String path;
        
        /**
         * 起始行（从1开始）
         */
        @Min(value = 1, message = "起始行必须大于0")
        private int startLine;
        
        /**
         * 结束行（包含）
         */
        @Min(value = 1, message = "结束行必须大于0")
        private int endLine;
    }
}

