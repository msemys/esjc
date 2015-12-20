package lt.msemys.esjc.operation.manager;

import lt.msemys.esjc.operation.EventStoreException;

public class ConnectionClosedException extends EventStoreException {

    public ConnectionClosedException(String message) {
        super(message);
    }

}
