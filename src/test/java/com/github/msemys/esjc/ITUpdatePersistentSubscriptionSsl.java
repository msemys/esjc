package com.github.msemys.esjc;

public class ITUpdatePersistentSubscriptionSsl extends ITUpdatePersistentSubscription {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSslSupplier.get();
    }

}
