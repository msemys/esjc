package com.github.msemys.esjc;

public class ITSoftDeleteSsl extends ITSoftDelete {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSslSupplier.get();
    }

}
