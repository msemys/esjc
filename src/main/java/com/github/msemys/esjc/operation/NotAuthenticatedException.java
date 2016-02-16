package com.github.msemys.esjc.operation;

import com.github.msemys.esjc.EventStoreException;

public class NotAuthenticatedException extends EventStoreException {

    public NotAuthenticatedException(String message) {
        super(message);
    }

}
