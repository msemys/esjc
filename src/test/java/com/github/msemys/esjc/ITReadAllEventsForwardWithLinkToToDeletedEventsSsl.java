package com.github.msemys.esjc;

public class ITReadAllEventsForwardWithLinkToToDeletedEventsSsl extends ITReadAllEventsForwardWithLinkToToDeletedEvents {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSslSupplier.get();
    }

}
