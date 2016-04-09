package com.github.msemys.esjc;

public class ITReadAllEventsForwardWithSoftDeletedStreamSsl extends ITReadAllEventsForwardWithSoftDeletedStream {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSslSupplier.get();
    }

}
