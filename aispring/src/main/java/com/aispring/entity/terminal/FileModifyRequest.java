package com.aispring.entity.terminal;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * 文件修改请求
 */
@Data
public class FileModifyRequest {
    /**
     * 文件路径
     */
    @NotBlank(message = "文件路径不能为空")
    private String path;
    
    /**
     * 修改操作列表（按顺序执行）
     */
    private List<ModifyOperation> operations;
    
    @Data
    public static class ModifyOperation {
        /**
         * 操作类型：delete（删除）、insert（插入）、replace（替换）
         */
        private String type;
        
        /**
         * 起始行（从1开始）
         */
        private int startLine;
        
        /**
         * 结束行（对于delete和replace）
         */
        private Integer endLine;
        
        /**
         * 插入或替换的内容
         */
        private String content;
    }
}

