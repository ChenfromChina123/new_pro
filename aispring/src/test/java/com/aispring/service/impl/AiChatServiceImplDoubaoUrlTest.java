package com.aispring.service.impl;

import com.aispring.repository.ChatRecordRepository;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.StreamingChatClient;
import org.springframework.beans.factory.ObjectProvider;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

public class AiChatServiceImplDoubaoUrlTest {

    /**
     * 验证豆包 baseUrl 已包含 `/api/v3` 时，生成的 chat/completions URL 不重复拼接 `/v1`。
     */
    @Test
    void buildDoubaoChatCompletionsUrl_should_support_api_v3_base() throws Exception {
        ObjectProvider<ChatClient> chatClientProvider = mock(ObjectProvider.class);
        ObjectProvider<StreamingChatClient> streamingChatClientProvider = mock(ObjectProvider.class);
        ChatRecordRepository chatRecordRepository = mock(ChatRecordRepository.class);

        AiChatServiceImpl service = new AiChatServiceImpl(
                chatClientProvider,
                streamingChatClientProvider,
                chatRecordRepository,
                "k",
                "https://ark.cn-beijing.volces.com/api/v3",
                "ep-xxx",
                "dk",
                "https://api.deepseek.com"
        );

        Method method = AiChatServiceImpl.class.getDeclaredMethod("buildDoubaoChatCompletionsUrl");
        method.setAccessible(true);

        assertEquals(
                "https://ark.cn-beijing.volces.com/api/v3/chat/completions",
                method.invoke(service)
        );
    }

    /**
     * 验证豆包 baseUrl 为根域名时，默认补齐 `/v1/chat/completions`。
     */
    @Test
    void buildDoubaoChatCompletionsUrl_should_default_to_v1_base() throws Exception {
        ObjectProvider<ChatClient> chatClientProvider = mock(ObjectProvider.class);
        ObjectProvider<StreamingChatClient> streamingChatClientProvider = mock(ObjectProvider.class);
        ChatRecordRepository chatRecordRepository = mock(ChatRecordRepository.class);

        AiChatServiceImpl service = new AiChatServiceImpl(
                chatClientProvider,
                streamingChatClientProvider,
                chatRecordRepository,
                "k",
                "https://example.com",
                "ep-xxx",
                "dk",
                "https://api.deepseek.com"
        );

        Method method = AiChatServiceImpl.class.getDeclaredMethod("buildDoubaoChatCompletionsUrl");
        method.setAccessible(true);

        assertEquals(
                "https://example.com/v1/chat/completions",
                method.invoke(service)
        );
    }
}

