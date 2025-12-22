package com.aispring.entity.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DecisionScope {
    private List<String> allowedPaths;
    private List<String> allowedTasks;
}
