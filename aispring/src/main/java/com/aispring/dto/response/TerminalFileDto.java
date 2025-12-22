package com.aispring.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TerminalFileDto {
    private String name;
    private String path; // Relative path
    private boolean isDirectory;
    private long size;
    private long lastModified;
}
