package com.github.msemys.esjc;

import static com.github.msemys.esjc.util.Strings.isNullOrEmpty;

public abstract class Subscription {
    public final String streamId;
    public final long lastCommitPosition;
    public final Integer lastEventNumber;

    public Subscription(String streamId, long lastCommitPosition, Integer lastEventNumber) {
        this.streamId = streamId;
        this.lastCommitPosition = lastCommitPosition;
        this.lastEventNumber = lastEventNumber;
    }

    public boolean isSubscribedToAll() {
        return isNullOrEmpty(streamId);
    }

    public abstract void unsubscribe();

}
