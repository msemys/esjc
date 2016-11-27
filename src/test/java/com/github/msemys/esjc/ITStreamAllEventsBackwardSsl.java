package com.github.msemys.esjc;

public class ITStreamAllEventsBackwardSsl extends ITStreamAllEventsBackward {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSslSupplier.get();
    }

}
