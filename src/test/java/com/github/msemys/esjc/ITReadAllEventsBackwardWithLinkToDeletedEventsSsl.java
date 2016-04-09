package com.github.msemys.esjc;

public class ITReadAllEventsBackwardWithLinkToDeletedEventsSsl extends ITReadAllEventsBackwardWithLinkToDeletedEvents {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSslSupplier.get();
    }

}
