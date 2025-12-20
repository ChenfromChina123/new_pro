package com.aispring.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ChatRecordControllerTest {

    /**
     * 验证 SaveRecordRequest 同时兼容 snake_case 与 camelCase 字段，避免 Jackson getter 冲突。
     */
    @Test
    void saveRecordRequest_should_deserialize_snake_case_fields() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        String json = """
                {
                  "session_id": "s1",
                  "user_message": "u",
                  "ai_response": "a",
                  "model": "doubao"
                }
                """;

        ChatRecordController.SaveRecordRequest req = objectMapper.readValue(
                json, ChatRecordController.SaveRecordRequest.class);

        assertEquals("s1", req.getSessionId());
        assertEquals("u", req.getUserMessage());
        assertEquals("a", req.getAiResponse());
        assertEquals("doubao", req.getModel());
    }
}

