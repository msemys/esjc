package lt.msemys.esjc.subscription;

import lt.msemys.esjc.Subscription;

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
