package com.github.msemys.esjc;

public class ITReadStreamEventsBackwardSsl extends ITReadStreamEventsBackward {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSslSupplier.get();
    }

}
