package com.github.msemys.esjc.operation.manager;

import com.github.msemys.esjc.EventStoreException;

public class OperationTimedOutException extends EventStoreException {

    public OperationTimedOutException(String message) {
        super(message);
    }

}
