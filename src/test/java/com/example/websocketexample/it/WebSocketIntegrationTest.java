package com.example.websocketexample.it;

import com.example.websocketexample.auth.AuthRequest;
import com.example.websocketexample.config.security.JwtTokenProvider;
import com.example.websocketexample.model.ChatMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "websocket.use-rabbitmq=true"
        })
class WebSocketIntegrationTest {
    private final static String CONTENT_MESSAGE = "Angelina test message";

    @LocalServerPort
    private int port;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Autowired
    private UserDetailsService userDetailsService;

    @Test
    public void sendAndGetWebSocketMessage_success() throws Exception {
        // 1. Сначала проверим, что можем получить токен
        String jwtToken = obtainJwtToken("user", "password");
        assertThat(jwtToken).isNotBlank();
        assertThat(jwtTokenProvider.validateToken(jwtToken)).isTrue();

        // 2. Проверим аутентификацию
        Authentication auth = jwtTokenProvider.getAuthentication(jwtToken, userDetailsService);
        System.out.println("Authenticated user: " + auth.getName());
        assertThat(auth.isAuthenticated()).isTrue();

        // 3. Теперь пробуем подключиться к WebSocket
        WebSocketStompClient stompClient = new WebSocketStompClient(new SockJsClient(
                List.of(new WebSocketTransport(new StandardWebSocketClient()))
        ));
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + jwtToken);

        String websocketUrl = "ws://localhost:" + port + "/ws";

        ListenableFuture<StompSession> sessionFuture = stompClient.connect(
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
        StompSession session = sessionFuture.get(20, TimeUnit.SECONDS);
        assertThat(session).isNotNull();
        assertThat(session.isConnected()).isTrue();
        System.out.println("Connected successfully! Session ID: " + session.getSessionId());

        // Подписываемся на тему
        AtomicReference<ChatMessage> receivedMessage = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        session.subscribe("/topic/public", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatMessage.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                ChatMessage message = (ChatMessage) payload;
                System.out.println("Received message: " + message.getContent());
                receivedMessage.set(message);
                latch.countDown();
            }
        });
        ChatMessage message = ChatMessage.builder()
                .sender(auth.getName())
                .content(CONTENT_MESSAGE)
                .type(ChatMessage.MessageType.CHAT)
                .build();
        // Отправляем тестовое сообщение
        session.send("/app/sendMessage", message);
        System.out.println("Message sent");

        // Ждем получения сообщения
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        ChatMessage actual = receivedMessage.get();
        assertThat(actual.getContent())
                .as("Содержимое сообщения должно совпадать")
                .isEqualTo(CONTENT_MESSAGE);

        session.disconnect();
    }

    @Test
    public void sendAndGetWebSocketMessage_fail() throws Exception {
        // 1. Сначала проверим, что можем получить токен
        String jwtToken = obtainJwtToken("angelina", "angelina");
        assertThat(jwtToken).isNotBlank();
        assertThat(jwtTokenProvider.validateToken(jwtToken)).isTrue();

        // 2. Проверим аутентификацию
        Authentication auth = jwtTokenProvider.getAuthentication(jwtToken, userDetailsService);
        System.out.println("Authenticated user: " + auth.getName());
        assertThat(auth.isAuthenticated()).isTrue();

        // 3. Теперь пробуем подключиться к WebSocket
        WebSocketStompClient stompClient = new WebSocketStompClient(new SockJsClient(
                List.of(new WebSocketTransport(new StandardWebSocketClient()))
        ));
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + jwtToken);

        String websocketUrl = "ws://localhost:" + port + "/ws";

        ListenableFuture<StompSession> sessionFuture = stompClient.connect(
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
        StompSession session = sessionFuture.get(20, TimeUnit.SECONDS);
        assertThat(session).isNotNull();
        assertThat(session.isConnected()).isTrue();
        System.out.println("Connected successfully! Session ID: " + session.getSessionId());

        // Подписываемся на тему
        AtomicReference<ChatMessage> receivedMessage = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        session.subscribe("/topic/public", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatMessage.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                ChatMessage message = (ChatMessage) payload;
                System.out.println("Received message: " + message.getContent());
                receivedMessage.set(message);
                latch.countDown();
            }
        });
        ChatMessage message = ChatMessage.builder()
                .sender(auth.getName())
                .content(CONTENT_MESSAGE)
                .type(ChatMessage.MessageType.CHAT)
                .build();
        // Отправляем тестовое сообщение
        session.send("/app/sendMessage", message);
        System.out.println("Message sent");

        // Сообщение не получаем
        assertThat(latch.await(5, TimeUnit.SECONDS)).isFalse();
    }

    // Вспомогательный метод для получения JWT токена
    private String obtainJwtToken(String username, String password) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        // Создаем запрос на логин
        AuthRequest authRequest = new AuthRequest(username, password); // Используйте реальные credentials
        MockHttpServletRequestBuilder loginRequest = MockMvcRequestBuilders
                .post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(authRequest));

        MvcResult result = mockMvc.perform(loginRequest)
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        // Извлекаем токен из ответа (предполагается, что возвращается JSON с полем "token")
        JsonNode jsonNode = mapper.readTree(response);
        return jsonNode.get("token").asText();
    }
}
