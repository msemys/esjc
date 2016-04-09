package com.github.msemys.esjc;

public class ITReadEventSsl extends ITReadEvent {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSslSupplier.get();
    }

}
