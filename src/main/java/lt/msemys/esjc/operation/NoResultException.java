package lt.msemys.esjc.operation;

import lt.msemys.esjc.EventStoreException;

/**
 * @see <a href="https://github.com/EventStore/EventStore/blob/dev/src/EventStore.ClientAPI/Exceptions/NoResultException.cs">EventStore.ClientAPI/Exceptions/NoResultException.cs</a>
 */
public class NoResultException extends EventStoreException {

    public NoResultException() {
    }

}
