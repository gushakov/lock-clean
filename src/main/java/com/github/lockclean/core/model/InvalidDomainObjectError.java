package com.github.lockclean.core.model;

public class InvalidDomainObjectError extends RuntimeException {
    public InvalidDomainObjectError(String message) {
        super(message);
    }
}
