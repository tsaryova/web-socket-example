package com.example.websocketexample.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry;
import org.springframework.security.config.annotation.web.socket.AbstractSecurityWebSocketMessageBrokerConfigurer;
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketSecurity
public class WebSocketSecurityConfig extends AbstractSecurityWebSocketMessageBrokerConfigurer {
    @Override
    protected void configureInbound(MessageSecurityMetadataSourceRegistry messages) {
        messages
                // Разрешаем подключение к эндпоинту /ws без аутентификации?
                // Обычно подключение аутентифицируется ДО handshake.
                .nullDestMatcher().permitAll()
                //.simpDestMatchers("/app/**").authenticated() // Сообщения, отправляемые на сервер по адресам /app/..., требуют аутентификации
                //.simpSubscribeDestMatchers("/topic/public/**").permitAll() // Подписка на публичные топики разрешена всем
                //.simpSubscribeDestMatchers("/topic/private/**", "/user/**").authenticated() // Подписка на приватные топики и топики для конкретного пользователя требует аутентификации
                .anyMessage().permitAll(); // Блокируем все остальное по умолчанию
    }
//
    // Отключаем CSRF для WebSocket соединения, т.к. он не применим в рамках протокола WS.
    @Override
    protected boolean sameOriginDisabled() {
        return true;
    }

//    @Bean
//    AuthorizationManager<Message<?>> messageAuthorizationManager(MessageMatcherDelegatingAuthorizationManager.Builder messages) {
//        messages
//                //.simpDestMatchers("/**").permitAll() // Сообщения, отправляемые на сервер по адресам /app/..., требуют аутентификации
//                //.simpSubscribeDestMatchers("/**").permitAll() // Подписка на публичные топики разрешена всем
//                //.simpSubscribeDestMatchers("/topic/public/**").permitAll() // Подписка на публичные топики разрешена всем
//                //.simpSubscribeDestMatchers("/topic/private/**", "/user/**").authenticated() // Подписка на приватные топики и топики для конкретного пользователя требует аутентификации
//                .nullDestMatcher().permitAll()
//                .anyMessage().permitAll(); // Блокируем все остальное по умолчанию
//
//        return messages.build();
//    }
}
