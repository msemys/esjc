package com.github.msemys.esjc;

public class ITIterateStreamEventsForwardSsl extends ITIterateStreamEventsForward {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSslSupplier.get();
    }

}
