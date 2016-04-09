package com.github.msemys.esjc;

public class ITReadAllEventsBackwardSsl extends ITReadAllEventsBackward {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSslSupplier.get();
    }

}
