package com.github.msemys.esjc;

public class ITDeletePersistentSubscriptionSsl extends ITDeletePersistentSubscription {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSslSupplier.get();
    }

}
