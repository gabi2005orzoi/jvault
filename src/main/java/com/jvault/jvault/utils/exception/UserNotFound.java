package com.jvault.jvault.utils.exception;


public class UserNotFound extends RuntimeException {
    public UserNotFound() {
        super("User not found");
    }
}
