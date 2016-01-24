package lt.msemys.esjc.transaction;

import lt.msemys.esjc.EventData;
import lt.msemys.esjc.Transaction;
import lt.msemys.esjc.WriteResult;
import lt.msemys.esjc.operation.UserCredentials;

import java.util.concurrent.CompletableFuture;

public interface TransactionManager {

    default CompletableFuture<Void> write(Transaction transaction, Iterable<EventData> events) {
        return write(transaction, events, null);
    }

    CompletableFuture<Void> write(Transaction transaction, Iterable<EventData> events, UserCredentials userCredentials);

    CompletableFuture<WriteResult> commit(Transaction transaction, UserCredentials userCredentials);

}
