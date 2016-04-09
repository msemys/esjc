package com.github.msemys.esjc;

public class ITReadAllEventsForwardWithLinkToPassedMaxCountSsl extends ITReadAllEventsForwardWithLinkToPassedMaxCount {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSslSupplier.get();
    }

}
