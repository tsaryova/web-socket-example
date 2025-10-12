package com.example.websocketexample.config.security;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Map;

//@Component
@RequiredArgsConstructor
public class AuthHandshakeInterceptor implements HandshakeInterceptor {
    private final JwtTokenProvider tokenProvider;

    //@Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        // Преобразуем запрос в ServletRequest чтобы получить параметры
        ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
        HttpServletRequest httpServletRequest = servletRequest.getServletRequest();

        // Извлекаем токен из параметра запроса
        String token = httpServletRequest.getParameter("token");

        if (token != null && tokenProvider.validateToken(token)) {
            // Если токен валиден, извлекаем имя пользователя и аутентифицируем
            String username = tokenProvider.getUsernameFromToken(token);
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    username, null, Collections.emptyList());
            // Кладем объект аутентификации в атрибуты, чтобы потом связать с WS-сессией
            attributes.put("user", auth);
            return true; // Разрешаем handshake
        } else {
            // Если токен невалиден, разрываем соединение
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    //@Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {

    }
}
