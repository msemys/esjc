package lt.msemys.esjc.node.cluster;

import lt.msemys.esjc.internal.EventStoreConnectionException;

public class ClusterException extends EventStoreConnectionException {
    public ClusterException(String message) {
        super(message);
    }

    public ClusterException(String message, Throwable cause) {
        super(message, cause);
    }
}
