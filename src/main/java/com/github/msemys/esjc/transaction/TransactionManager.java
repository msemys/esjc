package com.github.msemys.esjc.transaction;

import com.github.msemys.esjc.EventData;
import com.github.msemys.esjc.Transaction;
import com.github.msemys.esjc.WriteResult;
import com.github.msemys.esjc.UserCredentials;

import java.util.concurrent.CompletableFuture;

public interface TransactionManager {

    default CompletableFuture<Void> write(Transaction transaction, Iterable<EventData> events) {
        return write(transaction, events, null);
    }

    CompletableFuture<Void> write(Transaction transaction, Iterable<EventData> events, UserCredentials userCredentials);

    CompletableFuture<WriteResult> commit(Transaction transaction, UserCredentials userCredentials);

}
