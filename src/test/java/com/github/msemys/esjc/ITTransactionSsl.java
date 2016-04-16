package com.github.msemys.esjc;

public class ITTransactionSsl extends ITTransaction {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSslSupplier.get();
    }

}
