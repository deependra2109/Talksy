package com.deependra.talksy.dto;

import com.deependra.talksy.entity.Message;
import lombok.*;

import java.time.LocalDateTime;


@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MessageDTO {

    private Long id;
    private String senderUsername;
    private String recipientUsername;
    private String content;
    private String room;
    private Message.MessageType type;
    private LocalDateTime sentAt;

    public static MessageDTO from(Message m) {
        return MessageDTO.builder()
            .id(m.getId())
            .senderUsername(m.getSender().getUsername())
            .recipientUsername(m.getRecipient() != null
                ? m.getRecipient().getUsername() : null)
            .content(m.getContent())
            .room(m.getRoom())
            .type(m.getType())
            .sentAt(m.getSentAt())
            .build();
    }
}
