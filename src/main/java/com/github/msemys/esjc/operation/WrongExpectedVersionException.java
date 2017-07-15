package com.github.msemys.esjc.operation;

import com.github.msemys.esjc.EventStoreException;

/**
 * Exception thrown if the expected version specified on an operation
 * does not match the version of the stream when the operation was attempted.
 */
public class WrongExpectedVersionException extends EventStoreException {
    public final String stream;
    public final Long expectedVersion;
    public final Long currentVersion;

    /**
     * Creates a new instance with the specified error message.
     *
     * @param message error message.
     */
    public WrongExpectedVersionException(String message) {
        super(message);
        this.stream = null;
        this.expectedVersion = null;
        this.currentVersion = null;
    }

    /**
     * Creates a new instance with the specified error message format and operation details.
     *
     * @param message         error message format.
     * @param stream          the name of the stream.
     * @param expectedVersion the expected version of the stream.
     */
    public WrongExpectedVersionException(String message, String stream, long expectedVersion) {
        super(String.format(message, stream, expectedVersion));
        this.stream = stream;
        this.expectedVersion = expectedVersion;
        this.currentVersion = null;
    }

    /**
     * Creates a new instance with the specified error message format and operation details.
     *
     * @param message         error message format.
     * @param stream          the name of the stream.
     * @param expectedVersion the expected version of the stream.
     * @param currentVersion  the current version of the stream.
     */
    public WrongExpectedVersionException(String message, String stream, long expectedVersion, long currentVersion) {
        super(String.format(message, stream, expectedVersion, currentVersion));
        this.stream = stream;
        this.expectedVersion = expectedVersion;
        this.currentVersion = currentVersion;
    }

}
