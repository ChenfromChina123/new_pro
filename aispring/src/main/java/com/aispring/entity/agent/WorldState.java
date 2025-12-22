package com.aispring.entity.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorldState {
    private String projectRoot;
    @Builder.Default
    private Map<String, FileMeta> fileSystem = new HashMap<>();
    private Set<String> trackedPaths;
    private Map<String, Object> services;
}
