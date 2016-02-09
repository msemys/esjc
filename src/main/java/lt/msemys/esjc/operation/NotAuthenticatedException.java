package lt.msemys.esjc.operation;

import lt.msemys.esjc.EventStoreException;

public class NotAuthenticatedException extends EventStoreException {

    public NotAuthenticatedException(String message) {
        super(message);
    }

}
