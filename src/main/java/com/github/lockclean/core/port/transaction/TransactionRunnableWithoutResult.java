package com.github.lockclean.core.port.transaction;

@FunctionalInterface
public interface TransactionRunnableWithoutResult {
    void run();
}