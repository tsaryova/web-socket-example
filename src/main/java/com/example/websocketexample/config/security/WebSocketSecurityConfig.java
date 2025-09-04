package com.example.websocketexample.config.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry;
import org.springframework.security.config.annotation.web.socket.AbstractSecurityWebSocketMessageBrokerConfigurer;
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;

@Configuration
@EnableWebSocketMessageBroker
@EnableWebSocketSecurity
public class WebSocketSecurityConfig extends AbstractSecurityWebSocketMessageBrokerConfigurer {
    @Override
    protected void configureInbound(MessageSecurityMetadataSourceRegistry messages) {
        messages
                // Разрешаем подключение к эндпоинту /ws без аутентификации?
                // Обычно подключение аутентифицируется ДО handshake.
                .simpDestMatchers("/app/**").authenticated() // Сообщения, отправляемые на сервер по адресам /app/..., требуют аутентификации
                .simpSubscribeDestMatchers("/topic/public/**").permitAll() // Подписка на публичные топики разрешена всем
                .simpSubscribeDestMatchers("/topic/private/**", "/user/**").authenticated() // Подписка на приватные топики и топики для конкретного пользователя требует аутентификации
                .anyMessage().denyAll(); // Блокируем все остальное по умолчанию
    }

    // Отключаем CSRF для WebSocket соединения, т.к. он не применим в рамках протокола WS.
    // Защита от CSWSH обеспечивается проверкой Origin и аутентификацией во время handshake.
    @Override
    protected boolean sameOriginDisabled() {
        return true;
    }
}
