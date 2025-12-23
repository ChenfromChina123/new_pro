package com.aispring.entity.terminal;

import lombok.Data;
import java.util.List;

/**
 * 文件搜索请求
 */
@Data
public class FileSearchRequest {
    /**
     * 搜索模式（正则表达式或普通文本）
     */
    private String pattern;
    
    /**
     * 文件模式（如 *.java, *.ts）
     */
    private String filePattern;
    
    /**
     * 是否区分大小写
     */
    private boolean caseSensitive = false;
    
    /**
     * 上下文行数（显示匹配行前后各多少行）
     */
    private int contextLines = 20;
}

