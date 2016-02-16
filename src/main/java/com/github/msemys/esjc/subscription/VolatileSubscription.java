package com.github.msemys.esjc.subscription;

import com.github.msemys.esjc.Subscription;

public class VolatileSubscription extends Subscription {

    private final VolatileSubscriptionOperation operation;

    public VolatileSubscription(VolatileSubscriptionOperation operation,
                                String streamId,
                                long lastCommitPosition,
                                Integer lastEventNumber) {
        super(streamId, lastCommitPosition, lastEventNumber);
        this.operation = operation;
    }

    @Override
    public void unsubscribe() {
        operation.unsubscribe();
    }

}
