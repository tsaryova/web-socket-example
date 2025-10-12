package com.example.websocketexample.config.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry;
import org.springframework.security.config.annotation.web.socket.AbstractSecurityWebSocketMessageBrokerConfigurer;

@Configuration
public class WebSocketSecurityConfig extends AbstractSecurityWebSocketMessageBrokerConfigurer {
    @Override
    protected void configureInbound(MessageSecurityMetadataSourceRegistry messages) {
        messages

                .nullDestMatcher().permitAll() // Разрешаем подключение к эндпоинту /ws без аутентификации для подключения
                .simpDestMatchers("/app/**").authenticated() // Сообщения, отправляемые на сервер по адресам /app/..., требуют аутентификации
                .simpSubscribeDestMatchers("/topic/public/**").hasRole("USER") // Подписка на публичные топики разрешена всем
                .anyMessage().denyAll(); // Блокируем все остальное по умолчанию
    }

    // Отключаем CSRF для WebSocket соединения, т.к. он не применим в рамках протокола WS.
    @Override
    protected boolean sameOriginDisabled() {
        return true;
    }
}
