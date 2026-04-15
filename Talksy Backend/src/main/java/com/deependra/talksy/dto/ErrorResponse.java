package com.deependra.talksy.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ErrorResponse {
    private int status;
    private String error;
    private String message;
    private LocalDateTime timestamp;

    public static ErrorResponse of(int status, String error, String message) {
        return ErrorResponse.builder()
            .status(status)
            .error(error)
            .message(message)
            .timestamp(LocalDateTime.now())
            .build();
    }
}
