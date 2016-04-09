package com.github.msemys.esjc;

public class ITReadAllEventsForwardWithHardDeletedStreamSsl extends ITReadAllEventsForwardWithHardDeletedStream {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSslSupplier.get();
    }

}
