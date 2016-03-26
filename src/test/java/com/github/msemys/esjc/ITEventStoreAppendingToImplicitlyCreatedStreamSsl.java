package com.github.msemys.esjc;

public class ITEventStoreAppendingToImplicitlyCreatedStreamSsl extends ITEventStoreAppendingToImplicitlyCreatedStream {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSslSupplier.get();
    }

}
