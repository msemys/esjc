package com.github.msemys.esjc;

public class ITSubscribeToStreamSsl extends ITSubscribeToStream {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSslSupplier.get();
    }

}
