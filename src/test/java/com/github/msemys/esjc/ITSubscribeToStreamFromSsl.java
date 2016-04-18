package com.github.msemys.esjc;

public class ITSubscribeToStreamFromSsl extends ITSubscribeToStreamFrom {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSslSupplier.get();
    }

}
