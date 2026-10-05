package com.github.lockclean.infrastructure.adapter.transaction;

import com.github.lockclean.core.port.concurrency.OptimisticLockingError;
import com.github.lockclean.core.port.transaction.TransactionOperationsError;
import com.github.lockclean.core.port.transaction.TransactionOperationsOutputPort;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

/**
 * Default implementation of {@link TransactionOperationsOutputPort} over Spring's transaction SPI.
 *
 * <p>Two {@link TransactionTemplate}s are injected — a read-write one and a read-only one — so the
 * {@code readOnly} flag selects the right template without per-call configuration.
 *
 * <p>Two failure modes, kept strictly apart. A runtime error thrown by the <em>action</em> (a
 * {@code PersistenceOperationError}) propagates out of the template untouched, which triggers the
 * rollback; it is already a port type, so the use case's outermost checkpoint sees it as-is. A failure
 * of the transaction <em>machinery</em> itself — begin (no connection), commit, or an unexpected
 * rollback — surfaces as a Spring {@link TransactionException}, which this adapter catches
 * <em>narrowly</em> and wraps into the port's own {@link TransactionOperationsError}. The catch is
 * deliberately {@code TransactionException}, never {@code Exception}: a broad catch would re-wrap the
 * action's already-translated error (double-wrapping) and blur the two failure modes into one.
 *
 * <p>The {@code (action, onLockDetected)} overload adds a third, opt-in reaction. An
 * {@link OptimisticLockingError} — raised by the <em>persistence</em> adapter when a versioned write
 * loses a concurrent race, and propagated through the template as a rollback-triggering runtime
 * exception — is caught <em>outside</em> {@code execute()}, i.e. after the template has rolled back,
 * and routed to the supplied handler instead of to the use case's outermost checkpoint. The conflict
 * is <em>detected</em> by persistence but <em>reacted to</em> here, which is why its type sits in the
 * neutral {@code core.port.concurrency} package both adapters depend on. With a {@code null} handler
 * the overload is a plain read-write transaction and the error propagates as usual.
 *
 * <p>The after-commit hook registers a {@link TransactionSynchronization} overriding {@code afterCommit()}
 * — <em>never</em> {@code afterCompletion(STATUS_COMMITTED)}. The two look interchangeable and are not:
 * Spring runs {@code afterCompletion(int)} callbacks inside a {@code catch (Throwable)} that logs at ERROR
 * and carries on, so a failing presenter would be a log line and nothing more while the caller saw a normal
 * return. {@code afterCommit()} has no such catch — the exception propagates to the caller of
 * {@code commit()} (the transaction staying committed) and the callbacks queued behind the throwing one are
 * skipped. Since {@link TransactionTemplate} commits <em>outside</em> its own try block, that exception
 * leaves {@code execute()} raw, misses the narrow {@code TransactionException} catch above and reaches the
 * use case's outermost checkpoint. With no transaction active the action simply runs immediately, so both
 * branches share one contract — whatever the action throws, the caller sees.
 *
 * <p>There is deliberately no after-rollback hook: Spring offers no {@code afterRollback()} at all, only
 * {@code afterCompletion(STATUS_ROLLED_BACK)} — the swallowing form just ruled out. The rollback-side
 * reaction of this project (a lost optimistic-lock race) is the {@code onLockDetected} handler, which by
 * construction runs after the rollback.
 */
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
@Slf4j
@Service
public class SpringTransactionAdapter implements TransactionOperationsOutputPort {

    TransactionTemplate transactionTemplate;

    @Qualifier("read-only")
    TransactionTemplate readOnlyTransactionTemplate;

    @Override
    public void doInTransaction(boolean readOnly, Runnable action) {
        log.debug("[Transaction] Running action in a transaction, readOnly={}", readOnly);
        try {
            template(readOnly).executeWithoutResult(status -> action.run());
        } catch (TransactionException e) {
            throw new TransactionOperationsError("Transaction could not be completed", e);
        }
    }

    @Override
    public void doInTransaction(Runnable action, Runnable onLockDetected) {
        if (onLockDetected == null) {
            // No handler supplied: behave exactly like a plain read-write transaction — a lost race
            // propagates as OptimisticLockingError to the use case's outermost checkpoint.
            doInTransaction(action);
            return;
        }
        try {
            doInTransaction(action);
        } catch (OptimisticLockingError e) {
            // A versioned write inside the action lost a concurrent race: the detector fired and the
            // transaction has already been rolled back. That is an expected outcome under concurrency,
            // not a fault, so the caller's handler reacts to it instead of it reaching presentError.
            log.debug("[Transaction] Optimistic-lock conflict detected; running the onLockDetected handler", e);
            onLockDetected.run();
        }
    }

    @Override
    public <T> T doInTransactionWithResult(boolean readOnly, Supplier<T> action) {
        log.debug("[Transaction] Running action (with result) in a transaction, readOnly={}", readOnly);
        try {
            return template(readOnly).execute(status -> action.get());
        } catch (TransactionException e) {
            throw new TransactionOperationsError("Transaction could not be completed", e);
        }
    }

    @Override
    public void doAfterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            log.debug("[Transaction] No active transaction; running doAfterCommit action immediately");
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                log.debug("[Transaction] Running action after commit");
                action.run();
            }
        });
    }

    private TransactionTemplate template(boolean readOnly) {
        return readOnly ? readOnlyTransactionTemplate : transactionTemplate;
    }
}
