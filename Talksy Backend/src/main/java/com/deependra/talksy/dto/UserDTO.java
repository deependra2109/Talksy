package com.deependra.talksy.dto;

import com.deependra.talksy.entity.User;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserDTO {
    private Long id;
    private String username;
    private String email;

    public static UserDTO from(User u) {
        return UserDTO.builder()
            .id(u.getId())
            .username(u.getUsername())
            .email(u.getEmail())
            .build();
    }
}
