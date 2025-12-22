package com.aispring.entity.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Substep {
    private String id;
    private String name;
    private String goal;
    private String type; // COMMAND, FILE_EDIT, etc.
    private String command; // If type is COMMAND
    private TaskStatus status;
}
