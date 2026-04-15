package com.deependra.talksy.controller;

import com.deependra.talksy.dto.MessageDTO;
import com.deependra.talksy.entity.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.LocalDateTime;
import java.util.Map;


@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        try {
            StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

            Map<String, Object> attrs = accessor.getSessionAttributes();
            if (attrs == null) return;

            String username = (String) attrs.get("username");
            String room     = (String) attrs.get("room");

            if (username == null || username.isBlank()) return;
            if (room == null || room.isBlank()) return;

            log.info("User '{}' disconnected from room '{}'", username, room);

            MessageDTO leaveEvent = MessageDTO.builder()
                .senderUsername(username)
                .content(username + " left the room.")
                .room(room)
                .type(Message.MessageType.LEAVE)
                .sentAt(LocalDateTime.now())
                .build();

            messagingTemplate.convertAndSend("/topic/chat/" + room, leaveEvent);

        } catch (Exception ex) {
            log.warn("Failed to broadcast disconnect event: {}", ex.getMessage());
        }
    }
}
