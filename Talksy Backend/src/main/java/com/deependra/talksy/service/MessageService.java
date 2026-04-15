package com.deependra.talksy.service;

import com.deependra.talksy.dto.InboundMessageDTO;
import com.deependra.talksy.dto.MessageDTO;
import com.deependra.talksy.entity.Message;
import com.deependra.talksy.entity.Message.MessageType;
import com.deependra.talksy.entity.User;
import com.deependra.talksy.exception.CustomExceptions.InvalidMessageException;
import com.deependra.talksy.exception.CustomExceptions.InvalidRoomException;
import com.deependra.talksy.exception.CustomExceptions.UserNotFoundException;
import com.deependra.talksy.repository.MessageRepository;
import com.deependra.talksy.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    private static final int MAX_HISTORY_SIZE = 100;


    @Transactional
    public MessageDTO saveGroupMessage(InboundMessageDTO dto, String senderUsername) {
        validateContent(dto.getContent());
        validateRoom(dto.getRoom());

        User sender = loadUser(senderUsername);

        Message message = Message.builder()
            .sender(sender)
            .content(dto.getContent().trim())
            .room(dto.getRoom().trim().toLowerCase())
            .type(MessageType.GROUP)
            .build();

        Message saved = messageRepository.save(message);
        log.debug("Group message saved: room={}, sender={}", saved.getRoom(), senderUsername);
        return MessageDTO.from(saved);
    }


    @Transactional
    public MessageDTO savePrivateMessage(InboundMessageDTO dto, String senderUsername) {
        validateContent(dto.getContent());

        if (dto.getRecipientUsername() == null || dto.getRecipientUsername().isBlank()) {
            throw new InvalidMessageException(
                "recipientUsername is required for private messages.");
        }

        User sender = loadUser(senderUsername);
        User recipient = loadUser(dto.getRecipientUsername());

        if (sender.getId().equals(recipient.getId())) {
            throw new InvalidMessageException("Cannot send a private message to yourself.");
        }

        Message message = Message.builder()
            .sender(sender)
            .recipient(recipient)
            .content(dto.getContent().trim())
            .type(MessageType.PRIVATE)
            .build();

        Message saved = messageRepository.save(message);
        log.debug("Private message saved: from={}, to={}",
            senderUsername, dto.getRecipientUsername());
        return MessageDTO.from(saved);
    }


    @Transactional(readOnly = true)
    public List<MessageDTO> getGroupHistory(String room, int limit) {
        validateRoom(room);
        int size = Math.min(limit, MAX_HISTORY_SIZE);


        List<Message> messages = messageRepository.findByRoom(
            room.trim().toLowerCase(),
            PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "sentAt"))
        );

        Collections.reverse(messages);

        return messages.stream()
            .map(MessageDTO::from)
            .collect(Collectors.toList());
    }


    @Transactional(readOnly = true)
    public List<MessageDTO> getPrivateHistory(String usernameA, String usernameB, int limit) {
        User userA = loadUser(usernameA);
        User userB = loadUser(usernameB);
        int size = Math.min(limit, MAX_HISTORY_SIZE);

        List<Message> messages = messageRepository.findConversation(
            userA, userB,
            PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "sentAt"))
        );

        Collections.reverse(messages);

        return messages.stream()
            .map(MessageDTO::from)
            .collect(Collectors.toList());
    }


    private void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new InvalidMessageException("Message content cannot be empty.");
        }
        if (content.length() > 2000) {
            throw new InvalidMessageException(
                "Message content exceeds 2000 characters (got " + content.length() + ").");
        }
    }

    private void validateRoom(String room) {
        if (room == null || room.isBlank()) {
            throw new InvalidRoomException(room == null ? "null" : room);
        }
    }

    private User loadUser(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new UserNotFoundException(username));
    }
}
