package com.deependra.talksy.service;

import com.deependra.talksy.dto.AuthResponse;
import com.deependra.talksy.dto.LoginRequest;
import com.deependra.talksy.dto.RegisterRequest;
import com.deependra.talksy.entity.User;
import com.deependra.talksy.exception.CustomExceptions.InvalidCredentialsException;
import com.deependra.talksy.exception.CustomExceptions.UserAlreadyExistsException;
import com.deependra.talksy.repository.UserRepository;
import com.deependra.talksy.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;


    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("username", request.getUsername());
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("email", request.getEmail());
        }

        User user = User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .build();

        user = userRepository.save(user);
        log.info("New user registered: {}", user.getUsername());

        String token = jwtUtil.generateToken(user.getUsername());

        return AuthResponse.builder()
            .token(token)
                 .username(user.getUsername())
                .email(user.getEmail())
              .userId(user.getId())
            .build();
    }


    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())

            .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        log.info("User logged in: {}", user.getUsername());

        String token = jwtUtil.generateToken(user.getUsername());

        return AuthResponse.builder()
            .token(token)
             .username(user.getUsername())
            .email(user.getEmail())
            .userId(user.getId())
              .build();
    }
}
