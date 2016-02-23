package com.github.msemys.esjc;

import com.github.msemys.esjc.operation.UserCredentials;
import com.github.msemys.esjc.transaction.TransactionManager;

import java.util.concurrent.CompletableFuture;

import static com.github.msemys.esjc.util.Numbers.isNegative;
import static com.github.msemys.esjc.util.Preconditions.checkArgument;

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
        checkArgument(!isNegative(transactionId), "transactionId should not be negative.");

        this.transactionId = transactionId;
        this.userCredentials = userCredentials;
        this.transactionManager = transactionManager;
    }

    /**
     * Commits this transaction asynchronously.
     *
     * @return write result
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
     * Writes events to a transaction in the Event Store asynchronously.
     *
     * @param events the events to write.
     * @return the future that retrieves {@code null} as success value, when it completes.
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
     *
     * @throws Exception if any error occurs
     */
    @Override
    public void close() throws Exception {
        if (!isCommitted) {
            isRolledBack = true;
        }
    }

}
