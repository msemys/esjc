package lt.msemys.esjc.operation;

import lt.msemys.esjc.EventStoreException;

public class ServerErrorException extends EventStoreException {

    public ServerErrorException(String message) {
        super(message);
    }

}
