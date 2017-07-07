package com.github.msemys.esjc.subscription;

import com.github.msemys.esjc.Subscription;

import java.util.List;
import java.util.UUID;

public class PersistentSubscriptionChannel extends Subscription implements PersistentSubscriptionProtocol {

    private final PersistentSubscriptionProtocol protocol;

    public PersistentSubscriptionChannel(PersistentSubscriptionProtocol protocol,
                                         String streamId,
                                         long lastCommitPosition,
                                         Long lastEventNumber) {
        super(streamId, lastCommitPosition, lastEventNumber);
        this.protocol = protocol;
    }

    @Override
    public void notifyEventsProcessed(List<UUID> processedEvents) {
        protocol.notifyEventsProcessed(processedEvents);
    }

    @Override
    public void notifyEventsFailed(List<UUID> processedEvents, PersistentSubscriptionNakEventAction action, String reason) {
        protocol.notifyEventsFailed(processedEvents, action, reason);
    }

    @Override
    public void unsubscribe() {
        protocol.unsubscribe();
    }

}
