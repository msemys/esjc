package lt.msemys.esjc.operation;

/**
 * @see <a href="https://github.com/EventStore/EventStore/blob/dev/src/EventStore.ClientAPI/Exceptions/NotAuthenticatedException.cs">EventStore.ClientAPI/Exceptions/NotAuthenticatedException.cs</a>
 */
public class NotAuthenticatedException extends EventStoreException {

    public NotAuthenticatedException(String message) {
        super(message);
    }

}
