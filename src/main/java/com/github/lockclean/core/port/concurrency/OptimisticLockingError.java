package com.github.lockclean.core.port.concurrency;

import com.github.lockclean.core.GenericLockCleanError;

/*
    POINT OF INTEREST
    -----------------
    Error raised when a versioned write loses a concurrent race: the row's "version"
    had already moved past the one the aggregate carried, so the store rejected the
    update instead of overwriting the newer state.
 */

/**
 * Unchecked boundary error signalling that a persisted aggregate was <b>modified concurrently</b>.
 *
 * <p><b>Why it lives in its own {@code concurrency} package — raiser is not owner.</b> The
 * <em>aggregate</em> owns the concept (it carries the optimistic {@code version}); the
 * <em>persistence</em> adapter <em>detects</em> the conflict (it wraps Spring's
 * {@code OptimisticLockingFailureException} so a use case never catches a raw framework type); and
 * the <em>transaction</em> adapter is what <em>rolls back</em> and, through
 * {@code doInTransaction(action, onLockDetected)}, routes the loss to the use case's handler. Housing
 * the type in either port's package would couple the other adapter to a sibling port's currency; a
 * neutral package both depend on symmetrically removes that coupling.
 *
 * <p><b>Not a fault — an expected outcome under concurrency.</b> Deliberately a sibling of
 * {@code PersistenceOperationError} and {@code TransactionOperationsError}, not a subtype of either:
 * an optimistic-lock conflict is not "the store is broken" or "demarcation failed", it is "someone
 * else got there first" — an outcome the use case presents as a warning, not an error.
 */
public class OptimisticLockingError extends GenericLockCleanError {

    public OptimisticLockingError(String message, Throwable cause) {
        super(message, cause);
    }
}
