package com.deependra.talksy.exception;

public class WebSocketAuthException extends RuntimeException {

    public WebSocketAuthException(String message) {
        super(message);
    }

    public WebSocketAuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
