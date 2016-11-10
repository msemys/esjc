package com.github.msemys.esjc;

public class ITIterateAllEventsForwardSsl extends ITIterateAllEventsForward {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSslSupplier.get();
    }

}
