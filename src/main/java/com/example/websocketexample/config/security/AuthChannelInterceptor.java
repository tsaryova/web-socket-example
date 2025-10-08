package com.example.websocketexample.config.security;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class AuthChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor.wrap(message);
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && accessor.getCommand() != null) {

            // ОБРАБОТКА CONNECT - ИЗВЛЕКАЕМ JWT И СОЗДАЕМ АУТЕНТИФИКАЦИЮ
            if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                String token = extractJwtToken(accessor);

                if (token != null && jwtTokenProvider.validateToken(token)) {
                    Authentication auth = jwtTokenProvider.getAuthentication(token, userDetailsService);
                    // СОХРАНЯЕМ В СЕССИЮ ДЛЯ БУДУЩИХ ЗАПРОСОВ
                    accessor.getSessionAttributes().put("user", auth);
                    accessor.setUser(auth);
                    System.out.println("User authenticated: " + auth.getName());
                } else {
                    System.out.println("Invalid JWT token for WebSocket connection");
                    return null; // Разрываем соединение при невалидном токене
                }
            }

            // ДЛЯ ВСЕХ ОСТАЛЬНЫХ КОМАНД БЕРЕМ АУТЕНТИФИКАЦИЮ ИЗ СЕССИИ
            else if (accessor.getCommand() != StompCommand.DISCONNECT) {
                Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
                if (sessionAttributes != null && sessionAttributes.containsKey("user")) {
                    Object userObj = sessionAttributes.get("user");
                    if (userObj instanceof Authentication) {
                        accessor.setUser((Authentication) userObj);
                    }
                }
            }
        }
        return message;
    }

    private String extractJwtToken(StompHeaderAccessor accessor) {
        // Пробуем получить из заголовка Authorization
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // Пробуем получить из кастомного заголовка
        String tokenHeader = accessor.getFirstNativeHeader("token");
        if (tokenHeader != null) {
            return tokenHeader;
        }

        return null;
    }
}
