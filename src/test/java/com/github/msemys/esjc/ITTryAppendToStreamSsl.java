package com.github.msemys.esjc;

public class ITTryAppendToStreamSsl extends ITTryAppendToStream {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSslSupplier.get();
    }

}
