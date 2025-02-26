package com.github.lockclean.core;

public class GenericLockCleanError extends RuntimeException {

    public GenericLockCleanError(String message) {
        super(message);
    }

    public GenericLockCleanError(String message, Throwable cause) {
        super(message, cause);
    }

    public GenericLockCleanError(Throwable cause) {
        super(cause);
    }
}
