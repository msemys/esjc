package com.github.msemys.esjc;

public class ITSubscribeToAllFromSsl extends ITSubscribeToAllFrom {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSslSupplier.get();
    }

}
