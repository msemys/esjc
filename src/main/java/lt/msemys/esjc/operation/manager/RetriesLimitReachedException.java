package lt.msemys.esjc.operation.manager;

import lt.msemys.esjc.EventStoreException;

public class RetriesLimitReachedException extends EventStoreException {

    public RetriesLimitReachedException(int retries) {
        super(String.format("Reached retries limit : %d", retries));
    }

    public RetriesLimitReachedException(OperationItem item, int retries) {
        super(String.format("Item %s reached retries limit : %d", item.toString(), retries));
    }
}
