package lt.msemys.esjc.node.cluster;

import lt.msemys.esjc.EventStoreException;

public class ClusterException extends EventStoreException {
    public ClusterException(String message) {
        super(message);
    }

    public ClusterException(String message, Throwable cause) {
        super(message, cause);
    }
}
