package com.github.msemys.esjc.node.cluster;

import com.github.msemys.esjc.EventStoreException;

public class ClusterException extends EventStoreException {
    public ClusterException(String message) {
        super(message);
    }

    public ClusterException(String message, Throwable cause) {
        super(message, cause);
    }
}
