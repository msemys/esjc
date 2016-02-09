package lt.msemys.esjc;

import lt.msemys.esjc.operation.UserCredentials;
import lt.msemys.esjc.transaction.TransactionManager;

import java.util.concurrent.CompletableFuture;

import static lt.msemys.esjc.util.Numbers.isNegative;
import static lt.msemys.esjc.util.Preconditions.checkArgument;

public class Transaction implements AutoCloseable {
    public final long transactionId;

    private final UserCredentials userCredentials;
    private final TransactionManager transactionManager;
    private boolean isRolledBack;
    private boolean isCommitted;

    public Transaction(long transactionId, UserCredentials userCredentials, TransactionManager transactionManager) {
        checkArgument(!isNegative(transactionId), "transactionId should not be negative.");

        this.transactionId = transactionId;
        this.userCredentials = userCredentials;
        this.transactionManager = transactionManager;
    }

    public CompletableFuture<WriteResult> commit() {
        if (isRolledBack) {
            throw new InvalidOperationException("Cannot commit a rolled-back transaction");
        } else if (isCommitted) {
            throw new InvalidOperationException("Transaction is already committed");
        } else {
            isCommitted = true;
            return transactionManager.commit(this, userCredentials);
        }
    }

    public CompletableFuture<Void> write(Iterable<EventData> events) {
        if (isRolledBack) {
            throw new InvalidOperationException("Cannot write to a rolled-back transaction");
        } else if (isCommitted) {
            throw new InvalidOperationException("Transaction is already committed");
        } else {
            return transactionManager.write(this, events);
        }
    }

    public void rollback() {
        if (isCommitted) {
            throw new InvalidOperationException("Transaction is already committed");
        } else {
            isRolledBack = true;
        }
    }

    @Override
    public void close() throws Exception {
        if (!isCommitted) {
            isRolledBack = true;
        }
    }

}
