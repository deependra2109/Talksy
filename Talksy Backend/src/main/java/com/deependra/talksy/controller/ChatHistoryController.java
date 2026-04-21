package com.deependra.talksy.controller;

import com.deependra.talksy.dto.MessageDTO;
import com.deependra.talksy.dto.UserDTO;
import com.deependra.talksy.entity.User;
import com.deependra.talksy.exception.CustomExceptions.InvalidMessageException;
import com.deependra.talksy.exception.CustomExceptions.UserNotFoundException;
import com.deependra.talksy.repository.UserRepository;
import com.deependra.talksy.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;



@RestController
@RequiredArgsConstructor
public class ChatHistoryController {

    private final MessageService messageService;
    private final UserRepository userRepository;


    @GetMapping("/api/chat/history/group/{room}")
    public ResponseEntity<List<MessageDTO>> groupHistory(
        @PathVariable String room,
        @RequestParam(defaultValue = "50") int limit
    ) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        List<MessageDTO> messages = messageService.getGroupHistory(room, safeLimit);
        return ResponseEntity.ok(messages);
    }


    @GetMapping("/api/chat/history/private/{otherUsername}")
    public ResponseEntity<List<MessageDTO>> privateHistory(
        @PathVariable String otherUsername,
        @RequestParam(defaultValue = "50") int limit,
        Principal principal
    ) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        List<MessageDTO> messages = messageService.getPrivateHistory(
            principal.getName(), otherUsername, safeLimit);
        return ResponseEntity.ok(messages);
    }


    @GetMapping("/api/users/search")
    public ResponseEntity<List<UserDTO>> searchUsers(@RequestParam String q) {
        if (q == null || q.isBlank() || q.trim().length() < 2) {
            throw new InvalidMessageException(
                "Search query must be at least 2 characters.");
        }
        List<UserDTO> users = userRepository
            .findByUsernameContainingIgnoreCase(q.trim())
            .stream()
            .map(UserDTO::from)
            .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }


    @GetMapping("/api/users/me")
    public ResponseEntity<UserDTO> me(Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
            .orElseThrow(() -> new UserNotFoundException(principal.getName()));
        return ResponseEntity.ok(UserDTO.from(user));
    }
}
