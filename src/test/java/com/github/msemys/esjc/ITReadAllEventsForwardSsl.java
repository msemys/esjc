package com.github.msemys.esjc;

public class ITReadAllEventsForwardSsl extends ITReadAllEventsForward {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSslSupplier.get();
    }

}
