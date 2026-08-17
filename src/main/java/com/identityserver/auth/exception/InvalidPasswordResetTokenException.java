package com.identityserver.auth.exception;

public class InvalidPasswordResetTokenException extends RuntimeException {

    public InvalidPasswordResetTokenException(String message) {
        super(message);
    }
}
