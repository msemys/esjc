package com.github.msemys.esjc.operation;

import com.github.msemys.esjc.EventStoreException;
import com.github.msemys.esjc.tcp.TcpCommand;

public class CommandNotExpectedException extends EventStoreException {

    public CommandNotExpectedException(TcpCommand expected, TcpCommand actual) {
        super(String.format("Expected : %s. Actual : %s.", expected, actual));
    }

    public CommandNotExpectedException(String unexpectedCommand) {
        super(String.format("Unexpected command: %s.", unexpectedCommand));
    }

}
