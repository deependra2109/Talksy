package com.deependra.talksy.config;

import com.deependra.talksy.exception.WebSocketAuthException;
import com.deependra.talksy.security.JwtUtil;
import com.deependra.talksy.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@Slf4j
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
            MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();

        if (StompCommand.DISCONNECT.equals(command)) {
            return message;
        }

        if (StompCommand.CONNECT.equals(command)) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {

                log.warn("WS CONNECT rejected: missing Authorization header");
                throw new WebSocketAuthException("Missing Authorization header. Send 'Bearer <token>' in STOMP connect headers.");
            }

            String token = authHeader.substring(7).trim();
            if (token.isEmpty()) {
                throw new WebSocketAuthException("JWT token is empty.");
            }

            if (!jwtUtil.isTokenStructurallyValid(token)) {
                log.warn("WS CONNECT rejected: invalid or expired token");
                throw new WebSocketAuthException(
                    "Invalid or expired JWT token. Please log in again.");
            }

            try {
                String username = jwtUtil.extractUsername(token);
                if (username == null || username.isBlank()) {
                    throw new WebSocketAuthException("Token contains no subject claim.");
                }
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (!jwtUtil.isTokenValid(token, userDetails.getUsername())) {
                    throw new WebSocketAuthException("Token validation failed.");
                }

                UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                accessor.setUser(auth);

                log.info("WS CONNECT accepted: user={}", username);

            } catch (UsernameNotFoundException ex) {
                log.warn("WS CONNECT rejected: user not found — {}", ex.getMessage());
                throw new WebSocketAuthException("User account not found or deleted.");
            } catch (WebSocketAuthException ex) {
                throw ex;
            } catch (Exception ex) {
                log.error("WS CONNECT unexpected error: {}", ex.getMessage(), ex);
                throw new WebSocketAuthException("Authentication failed: server error.");
            }
        }

        if (StompCommand.SEND.equals(command) || StompCommand.SUBSCRIBE.equals(command)) {
            if (accessor.getUser() == null) {
                log.warn("WS {} blocked: no authenticated principal", command);
                throw new WebSocketAuthException(
                    "Not authenticated. Please reconnect with a valid token.");
            }
        }

        return message;
    }
}
