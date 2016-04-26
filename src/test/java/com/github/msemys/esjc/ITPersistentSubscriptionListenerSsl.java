package com.github.msemys.esjc;

public class ITPersistentSubscriptionListenerSsl extends ITPersistentSubscriptionListener {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSslSupplier.get();
    }

}
