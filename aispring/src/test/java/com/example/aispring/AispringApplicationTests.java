package com.example.aispring;

import com.aispring.AiTutorApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        classes = AiTutorApplication.class,
        properties = {
                "spring.ai.openai.api-key=dummy",
                "spring.ai.openai.base-url=http://localhost"
        }
)
class AispringApplicationTests {

    /**
     * 验证 Spring 应用上下文可正常启动（测试环境用 dummy OpenAI key 避免自动配置失败）。
     */
    @Test
    void contextLoads() {
    }

}
