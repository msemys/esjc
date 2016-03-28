package com.github.msemys.esjc;

public class ITAppendingToImplicitlyCreatedStreamUsingTransactionSsl extends ITAppendingToImplicitlyCreatedStreamUsingTransaction {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSslSupplier.get();
    }

}
