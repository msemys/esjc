package com.github.msemys.esjc.operation;

import com.github.msemys.esjc.EventStoreException;

public class WrongExpectedVersionException extends EventStoreException {
    public WrongExpectedVersionException(String message) {
        super(message);
    }
}
