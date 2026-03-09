package com.aispring.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebSocketConfig.class);

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        log.info("正在注册 WebSocket 处理器到路径：/ws/terminal/{{serverId}}");
        registry.addHandler(new SSHWebSocketHandler(), "/ws/terminal/{serverId}")
                .setAllowedOrigins("*");
        log.info("WebSocket 处理器注册成功");
    }
}
