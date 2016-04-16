package com.github.msemys.esjc;

public class ITWhenCommittingEmptyTransactionSsl extends ITWhenCommittingEmptyTransaction {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSslSupplier.get();
    }

}
