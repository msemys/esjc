package com.github.msemys.esjc;

public class ITStreamAllEventsForwardSsl extends ITStreamAllEventsForward {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSslSupplier.get();
    }

}
