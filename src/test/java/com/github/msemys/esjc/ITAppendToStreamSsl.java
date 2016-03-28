package com.github.msemys.esjc;

public class ITAppendToStreamSsl extends ITAppendToStream {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSslSupplier.get();
    }

}
