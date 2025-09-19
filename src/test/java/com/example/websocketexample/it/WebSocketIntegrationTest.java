package com.example.websocketexample.it;

import com.example.config.TestSecurityConfig;
import com.example.websocketexample.config.security.JwtTokenProvider;
import com.example.websocketexample.model.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebSocketIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testWebSocketConnection() throws Exception {
        //WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        //stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        WebSocketClient webSocketClient = new StandardWebSocketClient();
        SockJsClient sockJsClient = new SockJsClient(
                Arrays.asList(new WebSocketTransport(webSocketClient))
        );

        WebSocketStompClient stompClient = new WebSocketStompClient(sockJsClient);

        CompletableFuture<ChatMessage> future = new CompletableFuture<>();
        // Создаем Authentication объект
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "user",
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
        // Подключаемся с токеном аутентификации
        String token = jwtTokenProvider.generateToken(authentication);

        StompSession session = stompClient.connectAsync(
                "ws://localhost:" + port + "/ws?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8),
                new StompSessionHandlerAdapter() {}
        ).get(5, TimeUnit.SECONDS);

        assertTrue(session.isConnected());
        session.disconnect();

//        session.subscribe("/topic/chat", new StompFrameHandler() {
//            @Override
//            public Type getPayloadType(StompHeaders headers) {
//                return ChatMessage.class;
//            }
//
//            @Override
//            public void handleFrame(StompHeaders headers, Object payload) {
//                future.complete((ChatMessage) payload);
//            }
//        });
//
//        // Ждем немного перед отправкой сообщения
//        Thread.sleep(1000);
//
//        ChatMessage message = new ChatMessage();
//        message.setSender("user");
//        message.setContent("Test message");
//        message.setType(ChatMessage.MessageType.CHAT);
//
//        session.send("/app/chat", message);
//
//        ChatMessage received = future.get(10, TimeUnit.SECONDS);
//        assertNotNull(received);
    }

//    @Test
//    public void testWebSocketConnection1() throws Exception {
//        WebSocketClient client = new StandardWebSocketClient();
//
//        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
//        headers.add("Origin", "http://localhost:" + port);
//
//        String url = "ws://localhost:" + port + "/ws";
//
//        WebSocketSession session = client.execute(
//                new WebSocketHandler() {
//                    @Override
//                    public void afterConnectionEstablished(WebSocketSession session) {
//                        System.out.println("Connected!");
//                    }
//
//                    @Override
//                    public void handleMessage(WebSocketSession session,
//                                              WebSocketMessage<?> message) {
//                        System.out.println("Received: " + message.getPayload());
//                    }
//                },
//                headers,
//                new URI(url)
//        ).get();
//
//        session.sendMessage(new TextMessage("Test message"));
//        Thread.sleep(1000); // Дать время для обработки
//        session.close();
//    }
}
