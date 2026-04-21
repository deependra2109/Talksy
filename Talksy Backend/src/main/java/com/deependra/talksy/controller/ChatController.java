package com.deependra.talksy.controller;

import com.deependra.talksy.dto.InboundMessageDTO;
import com.deependra.talksy.dto.MessageDTO;
import com.deependra.talksy.entity.Message;
import com.deependra.talksy.exception.CustomExceptions.*;
import com.deependra.talksy.exception.WebSocketAuthException;
import com.deependra.talksy.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Map;


@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;


    @MessageMapping("/chat.group")
    public void sendGroupMessage(
        @Payload InboundMessageDTO dto,
        Principal principal
    ) {
        requirePrincipal(principal);
        requirePayload(dto);

        String senderUsername = principal.getName();
        MessageDTO saved = messageService.saveGroupMessage(dto, senderUsername);

        messagingTemplate.convertAndSend("/topic/chat/" + saved.getRoom(), saved);
        log.debug("Group msg → room={}, from={}", saved.getRoom(), senderUsername);
    }


    @MessageMapping("/chat.private")
    public void sendPrivateMessage(
        @Payload InboundMessageDTO dto,
        Principal principal
    ) {
        requirePrincipal(principal);
        requirePayload(dto);

        String senderUsername = principal.getName();
        MessageDTO saved = messageService.savePrivateMessage(dto, senderUsername);

        messagingTemplate.convertAndSendToUser(
            saved.getRecipientUsername(), "/queue/messages", saved);

        messagingTemplate.convertAndSendToUser(
            senderUsername, "/queue/messages", saved);

        log.debug("Private msg → from={}, to={}", senderUsername, saved.getRecipientUsername());
    }


    @MessageMapping("/chat.join")
    public void joinRoom(
        @Payload InboundMessageDTO dto,
        Principal principal,
        SimpMessageHeaderAccessor headerAccessor
    ) {
        requirePrincipal(principal);
        requirePayload(dto);

        if (dto.getRoom() == null || dto.getRoom().isBlank()) {
            throw new InvalidMessageException("Room name is required to join.");
        }

        String username = principal.getName();
        String room = dto.getRoom().trim().toLowerCase();

        Map<String, Object> attrs = headerAccessor.getSessionAttributes();
        if (attrs != null) {
            attrs.put("username", username);
            attrs.put("room", room);
        } else {
            log.warn("Session attributes null on join: user={}, room={}", username, room);
        }

        MessageDTO joinEvent = MessageDTO.builder()
            .senderUsername(username)
            .content(username + " joined the room.")
            .room(room)
            .type(Message.MessageType.JOIN)
            .sentAt(LocalDateTime.now())
            .build();

        messagingTemplate.convertAndSend("/topic/chat/" + room, joinEvent);
        log.info("User '{}' joined room '{}'", username, room);
    }


    @MessageExceptionHandler(InvalidMessageException.class)
    @SendToUser("/queue/errors")
    public MessageDTO handleInvalidMessage(InvalidMessageException ex) {
        log.warn("WS invalid message: {}", ex.getMessage());

        return errorDTO(ex.getMessage());
    }

    @MessageExceptionHandler(UserNotFoundException.class)
    @SendToUser("/queue/errors")
    public MessageDTO handleUserNotFound(UserNotFoundException ex) {
        log.warn("WS user not found: {}", ex.getMessage());
        return errorDTO(ex.getMessage());
    }

    @MessageExceptionHandler(InvalidRoomException.class)
    @SendToUser("/queue/errors")
    public MessageDTO handleInvalidRoom(InvalidRoomException ex) {
        log.warn("WS invalid room: {}", ex.getMessage());
        return errorDTO(ex.getMessage());
    }

    @MessageExceptionHandler(WebSocketAuthException.class)
    @SendToUser("/queue/errors")
    public MessageDTO handleAuthError(WebSocketAuthException ex) {
        log.warn("WS auth error: {}", ex.getMessage());
        return errorDTO("Authentication error: " + ex.getMessage());
    }

    @MessageExceptionHandler(Exception.class)
    @SendToUser("/queue/errors")
    public MessageDTO handleGeneral(Exception ex) {
        log.error("WS unhandled exception: {}", ex.getMessage(), ex);
        return errorDTO("An unexpected server error occurred.");
    }


    private void requirePrincipal(Principal principal) {
        if (principal == null) {
            throw new WebSocketAuthException(
                "Not authenticated. Please reconnect with a valid token.");
        }
    }

    private void requirePayload(InboundMessageDTO dto) {
        if (dto == null) {
            throw new InvalidMessageException("Message payload is missing or malformed JSON.");
        }
    }

    private MessageDTO errorDTO(String message) {
        return MessageDTO.builder()
            .content(message)
            .type(Message.MessageType.ERROR)  
            .sentAt(LocalDateTime.now())
            .build();
    }
}
