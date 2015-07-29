package lt.msemys.esjc.operation;

/**
 * @see <a href="https://github.com/EventStore/EventStore/blob/dev/src/EventStore.ClientAPI/Exceptions/AccessDeniedException.cs">EventStore.ClientAPI/Exceptions/AccessDeniedException.cs</a>
 */
public class AccessDeniedException extends EventStoreException {

    public AccessDeniedException(String message) {
        super(message);
    }

}
