package com.github.msemys.esjc;

public class ITCreatePersistentSubscriptionSsl extends ITCreatePersistentSubscription {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSslSupplier.get();
    }

}
