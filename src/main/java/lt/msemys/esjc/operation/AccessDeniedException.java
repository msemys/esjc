package lt.msemys.esjc.operation;

import lt.msemys.esjc.EventStoreException;

public class AccessDeniedException extends EventStoreException {

    public AccessDeniedException(String message) {
        super(message);
    }

}
