package com.github.msemys.esjc;

public class ITReadStreamEventsForwardWithUnresolvedLinkToSsl extends ITReadStreamEventsForwardWithUnresolvedLinkTo {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSslSupplier.get();
    }

}
