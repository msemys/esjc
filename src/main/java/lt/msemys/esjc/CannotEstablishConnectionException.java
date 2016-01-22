package lt.msemys.esjc;

public class CannotEstablishConnectionException extends EventStoreException {
    public CannotEstablishConnectionException(String message) {
        super(message);
    }

    public CannotEstablishConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
