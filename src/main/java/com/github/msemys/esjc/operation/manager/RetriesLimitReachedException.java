package com.github.msemys.esjc.operation.manager;

import com.github.msemys.esjc.EventStoreException;

public class RetriesLimitReachedException extends EventStoreException {

    public RetriesLimitReachedException(int retries) {
        super(String.format("Reached retries limit : %d", retries));
    }

    public RetriesLimitReachedException(String item, int retries) {
        super(String.format("Item %s reached retries limit : %d", item, retries));
    }
}
