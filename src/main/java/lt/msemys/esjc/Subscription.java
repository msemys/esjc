package lt.msemys.esjc;

import static lt.msemys.esjc.util.Strings.isNullOrEmpty;

public abstract class Subscription implements AutoCloseable {
    public final long lastCommitPosition;
    public final Integer lastEventNumber;
    public final String streamId;

    public Subscription(long lastCommitPosition, Integer lastEventNumber, String streamId) {
        this.lastCommitPosition = lastCommitPosition;
        this.lastEventNumber = lastEventNumber;
        this.streamId = streamId;
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
