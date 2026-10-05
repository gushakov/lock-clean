package com.github.lockclean.core.usecase;

import com.github.lockclean.core.port.concurrency.OptimisticLockingError;
import com.github.lockclean.core.port.transaction.TransactionOperationsOutputPort;

import java.util.function.Supplier;

/**
 * Test double for {@link TransactionOperationsOutputPort} used by the use case interaction tests.
 * There is no transaction: actions run inline, after-commit callbacks run immediately (fail-loud, as
 * the real adapter), and the {@code (action, onLockDetected)} overload honours the port's contract —
 * an {@link OptimisticLockingError} thrown by the action is routed to the handler, or propagates when
 * the handler is {@code null}.
 */
public class InlineTransactionOperations implements TransactionOperationsOutputPort {

    @Override
    public void doInTransaction(boolean readOnly, Runnable action) {
        action.run();
    }

    @Override
    public void doInTransaction(Runnable action, Runnable onLockDetected) {
        try {
            action.run();
        } catch (OptimisticLockingError e) {
            if (onLockDetected == null) {
                throw e;
            }
            onLockDetected.run();
        }
    }

    @Override
    public <T> T doInTransactionWithResult(boolean readOnly, Supplier<T> action) {
        return action.get();
    }

    @Override
    public void doAfterCommit(Runnable action) {
        action.run();
    }
}
