package com.github.msemys.esjc;

public class ITEventStoreListenerSsl extends ITEventStoreListener {

    @Override
    protected EventStore createEventStore() {
        return applyTestListener(eventstoreSslSupplier.get());
    }

}
