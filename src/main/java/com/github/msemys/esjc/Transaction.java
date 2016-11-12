package com.github.msemys.esjc;

import com.github.msemys.esjc.operation.*;
import com.github.msemys.esjc.transaction.TransactionManager;

import java.util.concurrent.CompletableFuture;

import static com.github.msemys.esjc.util.Numbers.isNegative;
import static com.github.msemys.esjc.util.Preconditions.checkArgument;
import static java.util.Collections.singletonList;

/**
 * Represents a multi-request transaction with the Event Store.
 * It allows the calling of multiple writes with multiple round trips
 * over long periods of time between the client and server.
 */
public class Transaction implements AutoCloseable {

    /**
     * The ID of the transaction. This can be used to recover a transaction later.
     */
    public final long transactionId;

    private final UserCredentials userCredentials;
    private final TransactionManager transactionManager;
    private boolean isRolledBack;
    private boolean isCommitted;

    /**
     * Creates a new instance with the specified transaction id, user credentials and transaction manager.
     *
     * @param transactionId      the transaction id.
     * @param userCredentials    user credentials under which transaction is committed (use {@code null} for default user credentials).
     * @param transactionManager transaction manager, that performs commit and write operations.
     */
    public Transaction(long transactionId, UserCredentials userCredentials, TransactionManager transactionManager) {
        checkArgument(!isNegative(transactionId), "transactionId should not be negative");

        this.transactionId = transactionId;
        this.userCredentials = userCredentials;
        this.transactionManager = transactionManager;
    }

    /**
     * Commits this transaction asynchronously.
     *
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link WrongExpectedVersionException},
     * {@link StreamDeletedException}, {@link InvalidTransactionException}, {@link CommandNotExpectedException},
     * {@link NotAuthenticatedException}, {@link AccessDeniedException} or {@link ServerErrorException}
     * on exceptional completion.
     */
    public CompletableFuture<WriteResult> commit() {
        if (isRolledBack) {
            throw new IllegalStateException("Cannot commit a rolled-back transaction");
        } else if (isCommitted) {
            throw new IllegalStateException("Transaction is already committed");
        } else {
            isCommitted = true;
            return transactionManager.commit(this, userCredentials);
        }
    }

    /**
     * Writes single event to a transaction in the Event Store asynchronously.
     *
     * @param event the event to write.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link CommandNotExpectedException},
     * {@link NotAuthenticatedException}, {@link AccessDeniedException} or {@link ServerErrorException}
     * on exceptional completion. In case of successful completion, the future's methods {@code get} and {@code join}
     * returns {@code null}.
     */
    public CompletableFuture<Void> write(EventData event) {
        return write(singletonList(event));
    }

    /**
     * Writes events to a transaction in the Event Store asynchronously.
     *
     * @param events the events to write.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link CommandNotExpectedException},
     * {@link NotAuthenticatedException}, {@link AccessDeniedException} or {@link ServerErrorException}
     * on exceptional completion. In case of successful completion, the future's methods {@code get} and {@code join}
     * returns {@code null}.
     */
    public CompletableFuture<Void> write(Iterable<EventData> events) {
        if (isRolledBack) {
            throw new IllegalStateException("Cannot write to a rolled-back transaction");
        } else if (isCommitted) {
            throw new IllegalStateException("Transaction is already committed");
        } else {
            return transactionManager.write(this, events);
        }
    }

    /**
     * Rollbacks this transaction.
     */
    public void rollback() {
        if (isCommitted) {
            throw new IllegalStateException("Transaction is already committed");
        } else {
            isRolledBack = true;
        }
    }

    /**
     * Closes this transaction by rolling it back if not already committed.
     */
    @Override
    public void close() {
        if (!isCommitted) {
            isRolledBack = true;
        }
    }

}
