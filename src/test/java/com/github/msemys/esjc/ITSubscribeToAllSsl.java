package com.github.msemys.esjc;

public class ITSubscribeToAllSsl extends ITSubscribeToAll {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSslSupplier.get();
    }

}
