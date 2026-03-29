package com.vision.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

/**
 * WebSocket 配置
 * 用于实时推送告警信息到前端
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * 配置消息代理
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 启用简单消息代理，用于向客户端推送消息
        registry.enableSimpleBroker("/topic", "/queue");
        // 设置客户端发送消息的前缀
        registry.setApplicationDestinationPrefixes("/app");
    }

    /**
     * 配置 STOMP 端点
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 注册 WebSocket 端点，允许跨域
        registry.addEndpoint("/ws/alerts")
                .setAllowedOriginPatterns("*")
                .withSockJS(); // 启用 SockJS 降级支持
    }
}
