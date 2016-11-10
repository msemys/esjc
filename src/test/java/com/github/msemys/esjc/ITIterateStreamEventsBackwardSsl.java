package com.github.msemys.esjc;

public class ITIterateStreamEventsBackwardSsl extends ITIterateStreamEventsBackward {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSslSupplier.get();
    }

}
