package org.example.expert.domain.common.exception;

public class InvalidAuthorizationException extends RuntimeException {
    public InvalidAuthorizationException(String message) {
        super(message);
    }
}
