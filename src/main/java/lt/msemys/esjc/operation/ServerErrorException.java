package lt.msemys.esjc.operation;

/**
 * @see <a href="https://github.com/EventStore/EventStore/blob/dev/src/EventStore.ClientAPI/Exceptions/ServerErrorException.cs">EventStore.ClientAPI/Exceptions/ServerErrorException.cs</a>
 */
public class ServerErrorException extends EventStoreException {

    public ServerErrorException(String message) {
        super(message);
    }

}
