package com.umg.subasta.config;

import com.umg.subasta.security.JwtService;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Tiempo real con STOMP sobre WebSocket.
 *  /topic/subastas          -> cambios de cualquier subasta (para el inventario)
 *  /topic/subasta/{id}      -> cambios de una subasta (vista de detalle)
 *  /user/queue/estado       -> avisos privados: "vas ganando" / "te superaron"
 * Los mensajes públicos NUNCA incluyen la identidad del postor.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwt;
    private final SecurityConfig security;

    public WebSocketConfig(JwtService jwt, SecurityConfig security) {
        this.jwt = jwt;
        this.security = security;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(security.origenes().toArray(String[]::new));
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        ThreadPoolTaskScheduler hb = new ThreadPoolTaskScheduler();
        hb.setPoolSize(1);
        hb.setThreadNamePrefix("ws-heartbeat-");
        hb.initialize();

        registry.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[]{10000, 10000})
                .setTaskScheduler(hb);
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    /** En el CONNECT, si viene "Authorization: Bearer ..." asociamos el usuario a la sesión. */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor acc = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (acc != null && StompCommand.CONNECT.equals(acc.getCommand())) {
                    jwt.autenticar(acc.getFirstNativeHeader("Authorization")).ifPresent(acc::setUser);
                }
                return message;
            }
        });
    }
}
