package lt.msemys.esjc.operation.manager;

import lt.msemys.esjc.EventStoreException;

public class OperationTimedOutException extends EventStoreException {

    public OperationTimedOutException(String message) {
        super(message);
    }

}
