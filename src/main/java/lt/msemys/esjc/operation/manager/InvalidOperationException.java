package lt.msemys.esjc.operation.manager;

import lt.msemys.esjc.operation.EventStoreException;

public class InvalidOperationException extends EventStoreException {

    public InvalidOperationException(String message) {
        super(message);
    }

}
