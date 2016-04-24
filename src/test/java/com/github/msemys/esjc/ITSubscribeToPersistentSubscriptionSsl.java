package com.github.msemys.esjc;

public class ITSubscribeToPersistentSubscriptionSsl extends ITSubscribeToPersistentSubscription {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSslSupplier.get();
    }

}
