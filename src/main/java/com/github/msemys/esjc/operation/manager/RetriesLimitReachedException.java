package com.github.msemys.esjc.operation.manager;

import com.github.msemys.esjc.EventStoreException;

/**
 * Exception thrown if the number of retries for an operation is reached.
 */
public class RetriesLimitReachedException extends EventStoreException {

    /**
     * Creates a new instance.
     *
     * @param retries the number of retries attempted.
     */
    public RetriesLimitReachedException(int retries) {
        super(String.format("Reached retries limit : %d", retries));
    }

    /**
     * Creates a new instance.
     *
     * @param item    the name of the item for which retries were attempted.
     * @param retries the number of retries attempted.
     */
    public RetriesLimitReachedException(String item, int retries) {
        super(String.format("Item %s reached retries limit : %d", item, retries));
    }
}
