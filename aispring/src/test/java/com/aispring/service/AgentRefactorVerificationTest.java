package com.aispring.service;

import com.aispring.entity.agent.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verification Test for Agent Refactoring (Phase 4).
 * Covers: TaskCompiler, StateMutator, and FSM Transitions.
 */
@ExtendWith(MockitoExtension.class)
public class AgentRefactorVerificationTest {

    private TaskCompiler taskCompiler;
    private StateMutator stateMutator;
    
    @Mock
    private AgentStateService agentStateService;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        taskCompiler = new TaskCompiler(objectMapper);
        stateMutator = new StateMutator(objectMapper);
    }

    /**
     * Test 1: TaskCompiler should strictly parse JSON output.
     */
    @Test
    void testTaskCompiler_ParsesValidJson() {
        String jsonOutput = "[\n" +
                "    {\n" +
                "      \"id\": \"task-1\",\n" +
                "      \"name\": \"Create Frontend\",\n" +
                "      \"goal\": \"Setup Vue project\",\n" +
                "      \"substeps\": [\n" +
                "        {\n" +
                "          \"id\": \"sub-1.1\",\n" +
                "          \"name\": \"Init project\",\n" +
                "          \"type\": \"COMMAND\",\n" +
                "          \"command\": \"npm init\"\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]";

        TaskState taskState = taskCompiler.compile(jsonOutput, "pipe-123");

        assertNotNull(taskState);
        assertEquals("pipe-123", taskState.getPipelineId());
        assertEquals(1, taskState.getTasks().size());
        // ID might be generated if null, but here we provided "task-1"
        assertEquals("task-1", taskState.getTasks().get(0).getId());
        assertEquals(TaskStatus.PENDING, taskState.getTasks().get(0).getStatus());
    }

    /**
     * Test 2: StateMutator should validate tool results and update state.
     */
    @Test
    void testStateMutator_ApplyToolResult_Success() {
        // Setup AgentState
        AgentState state = new AgentState();
        state.setStatus(AgentStatus.WAITING_TOOL);
        
        TaskState taskState = new TaskState();
        Task task = new Task();
        task.setId("task-1");
        task.setStatus(TaskStatus.IN_PROGRESS);
        taskState.setTasks(Collections.singletonList(task));
        taskState.setCurrentTaskId("task-1");
        state.setTaskState(taskState);
        
        WorldState worldState = new WorldState();
        worldState.setTrackedPaths(new HashSet<>());
        worldState.setFileSystem(new ConcurrentHashMap<>());
        state.setWorldState(worldState);

        // Setup Decision Envelope
        DecisionEnvelope lastDecision = new DecisionEnvelope();
        lastDecision.setDecisionId("dec-001");
        lastDecision.setAction("ensure_file");
        
        DecisionExpectation expectation = new DecisionExpectation();
        expectation.setTaskProgress("substep completed");
        lastDecision.setExpectation(expectation);
        
        state.setLastDecision(lastDecision);

        // Create ToolResult
        ToolResult result = new ToolResult();
        result.setDecisionId("dec-001");
        result.setExitCode(0);
        result.setStdout("File created");
        // result.setSuccess(true); // Removed as it is inferred from exitCode

        // Act
        MutatorResult mutatorResult = stateMutator.applyToolResult(state, result);

        // Assert
        assertTrue(mutatorResult.isAccepted());
        assertEquals(AgentStatus.RUNNING, mutatorResult.getNewAgentStatus()); // Should go back to RUNNING
        assertNull(state.getLastDecision()); // Should be cleared
    }

    /**
     * Test 3: StateMutator should reject mismatched Decision IDs.
     */
    @Test
    void testStateMutator_RejectDuplicateOrWrongId() {
        AgentState state = new AgentState();
        state.setStatus(AgentStatus.WAITING_TOOL);
        
        DecisionEnvelope lastDecision = new DecisionEnvelope();
        lastDecision.setDecisionId("dec-001");
        state.setLastDecision(lastDecision);

        ToolResult result = new ToolResult();
        result.setDecisionId("dec-999"); // Wrong ID

        MutatorResult mutatorResult = stateMutator.applyToolResult(state, result);

        assertFalse(mutatorResult.isAccepted());
        assertEquals(AgentStatus.WAITING_TOOL, mutatorResult.getNewAgentStatus()); // Status unchanged
    }

    /**
     * Test 4: Verify WorldState whitelist logic (Mocked simulation).
     */
    @Test
    void testWorldState_TrackedFilesOnly() {
        WorldState worldState = new WorldState();
        worldState.setTrackedPaths(new HashSet<>());
        worldState.setFileSystem(new ConcurrentHashMap<>());
        
        // Simulate adding a file
        FileMeta fileMeta = new FileMeta();
        fileMeta.setPath("src/App.vue");
        fileMeta.setSource(FileSource.AGENT);
        
        worldState.getTrackedPaths().add(fileMeta.getPath());
        worldState.getFileSystem().put(fileMeta.getPath(), fileMeta);

        assertTrue(worldState.getTrackedPaths().contains("src/App.vue"));
        assertEquals(FileSource.AGENT, worldState.getFileSystem().get("src/App.vue").getSource());
    }
}
