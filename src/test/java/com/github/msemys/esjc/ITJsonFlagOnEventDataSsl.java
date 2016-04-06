package com.github.msemys.esjc;

public class ITJsonFlagOnEventDataSsl extends ITJsonFlagOnEventData {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSslSupplier.get();
    }

}
