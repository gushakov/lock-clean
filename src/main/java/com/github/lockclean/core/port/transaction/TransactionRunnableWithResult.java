package com.github.lockclean.core.port.transaction;

/**
 * Functional interface which will be executed by {@linkplain TransactionOperationsOutputPort}
 * returning some result.
 *
 * @param <R> any type
 */
@FunctionalInterface
public interface TransactionRunnableWithResult<R> {

    R run();

}
