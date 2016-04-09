package com.github.msemys.esjc;

public class ITReadEventOfLinkToToDeletedEventSsl extends ITReadEventOfLinkToToDeletedEvent {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSslSupplier.get();
    }

}
