package com.github.msemys.esjc;

public class ITAppendingToImplicitlyCreatedStreamSsl extends ITAppendingToImplicitlyCreatedStream {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSslSupplier.get();
    }

}
