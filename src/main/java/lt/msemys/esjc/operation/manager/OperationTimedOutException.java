package lt.msemys.esjc.operation.manager;

import lt.msemys.esjc.operation.EventStoreException;

public class OperationTimedOutException extends EventStoreException {

    public OperationTimedOutException(String message) {
        super(message);
    }

}
