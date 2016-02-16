package com.github.msemys.esjc.operation;

import com.github.msemys.esjc.EventStoreException;

public class ServerErrorException extends EventStoreException {

    public ServerErrorException(String message) {
        super(message);
    }

}
