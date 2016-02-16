package com.github.msemys.esjc;

public class ConnectionClosedException extends EventStoreException {

    public ConnectionClosedException(String message) {
        super(message);
    }

}
