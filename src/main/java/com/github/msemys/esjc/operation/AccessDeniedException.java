package com.github.msemys.esjc.operation;

import com.github.msemys.esjc.EventStoreException;

public class AccessDeniedException extends EventStoreException {

    public AccessDeniedException(String message) {
        super(message);
    }

}
