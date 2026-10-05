package com.github.lockclean.core.port.db;

import com.github.lockclean.core.GenericLockCleanError;

public class PersistenceOperationError extends GenericLockCleanError {
    public PersistenceOperationError(String message) {
        super(message);
    }

    public PersistenceOperationError(String message, Throwable cause) {
        super(message, cause);
    }
}
