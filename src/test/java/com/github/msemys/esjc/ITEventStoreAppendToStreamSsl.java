package com.github.msemys.esjc;

public class ITEventStoreAppendToStreamSsl extends ITEventStoreAppendToStream {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSslSupplier.get();
    }

}
