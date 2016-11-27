package com.github.msemys.esjc;

public class ITStreamEventsBackwardSsl extends ITStreamEventsBackward {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSslSupplier.get();
    }

}
