package lt.msemys.esjc.operation;

import lt.msemys.esjc.EventStoreException;

public class WrongExpectedVersionException extends EventStoreException {
    public WrongExpectedVersionException(String message) {
        super(message);
    }
}
