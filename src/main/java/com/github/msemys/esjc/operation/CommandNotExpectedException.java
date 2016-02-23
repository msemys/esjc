package com.github.msemys.esjc.operation;

import com.github.msemys.esjc.EventStoreException;
import com.github.msemys.esjc.tcp.TcpCommand;

/**
 * Exception thrown if an unexpected command is received.
 */
public class CommandNotExpectedException extends EventStoreException {

    /**
     * Creates new a new instance.
     *
     * @param expected expected tcp command.
     * @param actual   actual tcp command.
     */
    public CommandNotExpectedException(TcpCommand expected, TcpCommand actual) {
        super(String.format("Expected : %s. Actual : %s.", expected, actual));
    }

    /**
     * Creates a new instance.
     *
     * @param unexpectedCommand unexpected tcp command.
     */
    public CommandNotExpectedException(String unexpectedCommand) {
        super(String.format("Unexpected command: %s.", unexpectedCommand));
    }

}
