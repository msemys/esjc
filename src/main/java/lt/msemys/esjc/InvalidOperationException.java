package lt.msemys.esjc;

public class InvalidOperationException extends EventStoreException {

    public InvalidOperationException(String message) {
        super(message);
    }

}
