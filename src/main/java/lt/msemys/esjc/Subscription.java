package lt.msemys.esjc;

import static lt.msemys.esjc.util.Strings.isNullOrEmpty;

public abstract class Subscription implements AutoCloseable {
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

    @Override
    public void close() throws Exception {
        unsubscribe();
    }

    public abstract void unsubscribe();

}
