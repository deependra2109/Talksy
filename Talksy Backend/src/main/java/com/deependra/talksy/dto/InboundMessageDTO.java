package com.deependra.talksy.dto;

import lombok.*;



@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InboundMessageDTO {
    private String content;
    private String room;
    private String recipientUsername;
}
