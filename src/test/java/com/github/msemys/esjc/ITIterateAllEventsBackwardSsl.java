package com.github.msemys.esjc;

public class ITIterateAllEventsBackwardSsl extends ITIterateAllEventsBackward {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSslSupplier.get();
    }

}
