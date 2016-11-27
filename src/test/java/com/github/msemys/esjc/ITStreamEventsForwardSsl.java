package com.github.msemys.esjc;

public class ITStreamEventsForwardSsl extends ITStreamEventsForward {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSslSupplier.get();
    }

}
