package com.github.msemys.esjc.operation;

import com.github.msemys.esjc.EventStoreException;

/**
 * Exception thrown if an operation is attempted on non-existent stream.
 */
public class StreamNotFoundException extends EventStoreException {
    public final String stream;

    /**
     * Creates a new instance with the specified name of non-existent stream.
     *
     * @param stream the name of the non-existent stream.
     */
    public StreamNotFoundException(String stream) {
        this("Event stream '%s' not found.", stream);
    }

    /**
     * Creates a new instance with the specified error message and name of non-existent stream.
     *
     * @param message error message
     * @param stream  the name of the non-existent stream.
     */
    public StreamNotFoundException(String message, String stream) {
        super(String.format(message, stream));
        this.stream = stream;
    }

}
