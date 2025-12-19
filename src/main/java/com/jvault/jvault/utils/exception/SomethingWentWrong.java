package com.jvault.jvault.utils.exception;

public class SomethingWentWrong extends RuntimeException {
    public SomethingWentWrong() {
        super("Something went wrong");
    }
}
