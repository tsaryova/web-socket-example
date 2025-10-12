package com.example.websocketexample.config;

import com.example.websocketexample.config.security.AuthChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.RequestUpgradeStrategy;
import org.springframework.web.socket.server.standard.TomcatRequestUpgradeStrategy;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

@Configuration
@RequiredArgsConstructor
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Value("${websocket.use-rabbitmq:false}")
    private boolean useRabbitMq;

    private final AuthChannelInterceptor channelInterceptor;
    private final LoggingChannelInterceptor loggingChannelInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        RequestUpgradeStrategy upgradeStrategy = new TomcatRequestUpgradeStrategy();
        registry
                .addEndpoint("/ws")
                .setAllowedOriginPatterns("*") //тут указываются домены
                .setHandshakeHandler(new DefaultHandshakeHandler(upgradeStrategy))
                .withSockJS();
    }
//    @Override
//    public void configureMessageBroker(MessageBrokerRegistry registry) {
//        registry.setApplicationDestinationPrefixes("/app");
//        registry.enableSimpleBroker("/topic");
//
////        registry.enableSimpleBroker("/topic", "/queue"); // "/queue" для приватных сообщений
////        registry.setApplicationDestinationPrefixes("/app");
////        registry.setUserDestinationPrefix("/user"); // Важно для @SendToUser
//    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(channelInterceptor, loggingChannelInterceptor);
    }

    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        // Use this for enabling a Full featured broker like RabbitMQ
        if (useRabbitMq) {
            registry.enableStompBrokerRelay("/topic", "/queue")
                    .setRelayHost("localhost")
                    .setRelayPort(61613)
                    .setClientLogin("guest")
                    .setClientPasscode("guest")
                    .setSystemLogin("guest")
                    .setSystemPasscode("guest");
        } else {
            registry.enableSimpleBroker("/topic");
        }
    }
}
