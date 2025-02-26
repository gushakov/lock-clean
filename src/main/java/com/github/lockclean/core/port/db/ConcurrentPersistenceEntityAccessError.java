package com.github.lockclean.core.port.db;

/*
    POINT OF INTEREST
    -----------------
    Error thrown when an optimistic concurrency lock has been detected.
 */

public class ConcurrentPersistenceEntityAccessError extends PersistenceOperationError {
    public ConcurrentPersistenceEntityAccessError(Exception e) {
        super(e);
    }
}
