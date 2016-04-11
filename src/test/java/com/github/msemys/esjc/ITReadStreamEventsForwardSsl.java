package com.github.msemys.esjc;

public class ITReadStreamEventsForwardSsl extends ITReadStreamEventsForward {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSslSupplier.get();
    }

}
