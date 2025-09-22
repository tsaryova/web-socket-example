package com.example.websocketexample.it;

import com.example.config.TestSecurityConfig;
import com.example.websocketexample.auth.AuthRequest;
import com.example.websocketexample.config.security.JwtTokenProvider;
import com.example.websocketexample.model.ChatMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.RestTemplateXhrTransport;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class WebSocketIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserDetailsService userDetailsService;

    @Test
    public void testWebSocketConnection() throws Exception {
        // 1. Сначала проверим, что можем получить токен
        String jwtToken = obtainJwtToken();
        System.out.println("JWT Token obtained: " + jwtToken);
        assertThat(jwtToken).isNotBlank();

        // 2. Проверим валидность токена
        assertThat(jwtTokenProvider.validateToken(jwtToken)).isTrue();

        // 3. Проверим аутентификацию
        Authentication auth = jwtTokenProvider.getAuthentication(jwtToken, userDetailsService);
        System.out.println("Authenticated user: " + auth.getName());
        assertThat(auth.isAuthenticated()).isTrue();

        // 4. Теперь пробуем подключиться к WebSocket
        try {
            WebSocketStompClient stompClient = new WebSocketStompClient(new SockJsClient(
                    List.of(new WebSocketTransport(new StandardWebSocketClient()))
            ));
            stompClient.setMessageConverter(new MappingJackson2MessageConverter());

            StompHeaders connectHeaders = new StompHeaders();
            connectHeaders.add("Authorization", "Bearer " + jwtToken);

            String websocketUrl = "ws://localhost:" + port + "/ws?token=" + jwtToken;

            CompletableFuture<StompSession> sessionFuture = stompClient.connectAsync(
                    websocketUrl,
                    new WebSocketHttpHeaders(),
                    connectHeaders,
                    new StompSessionHandlerAdapter() {
                        @Override
                        public void handleException(StompSession session, StompCommand command,
                                                    StompHeaders headers, byte[] payload, Throwable exception) {
                            System.out.println("Exception: " + exception.getMessage());
                        }

                        @Override
                        public void handleTransportError(StompSession session, Throwable exception) {
                            System.out.println("Transport error: " + exception.getMessage());
                        }

                        @Override
                        public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                            System.out.println("Connected! Session ID: " + session.getSessionId());
                        }
                    }
            );

            // Ждем подключения
            StompSession session = sessionFuture.get(30, TimeUnit.SECONDS);
            assertThat(session).isNotNull();
            assertThat(session.isConnected()).isTrue();
            System.out.println("Connected successfully! Session ID: " + session.getSessionId());

            // Подписываемся на тему
            CountDownLatch latch = new CountDownLatch(1);
            session.subscribe("/topic/public", new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return String.class;
                }

                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    System.out.println("Received message: " + payload);
                    latch.countDown();
                }
            });

            // Отправляем тестовое сообщение
            session.send("/chat.sendMessage", "Test message from test");
            System.out.println("Message sent");

            // Ждем получения сообщения
            assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();

            session.disconnect();

        } catch (Exception e) {
            System.out.println("WebSocket connection failed: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    // Вспомогательный метод для получения JWT токена
    private String obtainJwtToken() throws Exception {
        // Создаем запрос на логин
        AuthRequest authRequest = new AuthRequest("user", "password"); // Используйте реальные credentials

        // Отправляем POST запрос на /auth/login
        MockHttpServletRequestBuilder loginRequest = MockMvcRequestBuilders
                .post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"user\",\"password\":\"password\"}"); // Замените на реальные данные

        MvcResult result = mockMvc.perform(loginRequest)
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();

        // Извлекаем токен из ответа (предполагается, что возвращается JSON с полем "token")
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(response);
        return jsonNode.get("token").asText();
    }


}
