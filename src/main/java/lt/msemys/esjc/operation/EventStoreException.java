package lt.msemys.esjc.operation;

/**
 * @see <a href="https://github.com/EventStore/EventStore/blob/dev/src/EventStore.ClientAPI/Exceptions/EventStoreConnectionException.cs">EventStore.ClientAPI/Exceptions/EventStoreConnectionException.cs</a>
 */
public abstract class EventStoreException extends RuntimeException {

    public EventStoreException() {
    }

    public EventStoreException(String message) {
        super(message);
    }

    public EventStoreException(String message, Throwable cause) {
        super(message, cause);
    }

    public EventStoreException(Throwable cause) {
        super(cause);
    }

}
