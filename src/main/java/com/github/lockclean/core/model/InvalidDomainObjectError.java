package com.github.lockclean.core.model;

import com.github.lockclean.core.GenericLockCleanError;

public class InvalidDomainObjectError extends GenericLockCleanError {
    public InvalidDomainObjectError(String message) {
        super(message);
    }
}
