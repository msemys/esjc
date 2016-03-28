package com.github.msemys.esjc;

public class ITEventStoreAppendingToImplicitlyCreatedStreamUsingTransactionSsl extends ITEventStoreAppendingToImplicitlyCreatedStreamUsingTransaction {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSslSupplier.get();
    }

}
