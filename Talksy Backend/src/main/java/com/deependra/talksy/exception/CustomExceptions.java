package com.deependra.talksy.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;


public final class CustomExceptions {

    private CustomExceptions() {}


    @ResponseStatus(HttpStatus.CONFLICT)
    public static class UserAlreadyExistsException extends RuntimeException {
        public UserAlreadyExistsException(String field, String value) {
            super("A user with " + field + " '" + value + "' already exists.");
        }
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    public static class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(String username) {
            super("User not found: " + username);
        }
        public UserNotFoundException(Long id) {
            super("User not found with id: " + id);
        }
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public static class InvalidCredentialsException extends RuntimeException {
        public InvalidCredentialsException() {
            super("Invalid username or password.");
        }
    }


    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public static class InvalidMessageException extends RuntimeException {
        public InvalidMessageException(String reason) {
            super("Invalid message: " + reason);
        }
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    public static class MessageNotFoundException extends RuntimeException {
        public MessageNotFoundException(Long id) {
            super("Message not found with id: " + id);
        }
    }


    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public static class InvalidRoomException extends RuntimeException {
        public InvalidRoomException(String room) {
            super("Room name is invalid or empty: '" + room + "'");
        }
    }
}
