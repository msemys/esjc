package com.github.msemys.esjc.operation;

import com.github.msemys.esjc.EventStoreException;

/**
 * Exception thrown if an operation is attempted on a stream which has been deleted.
 */
public class StreamDeletedException extends EventStoreException {
    public final String stream;

    /**
     * Creates a new instance.
     */
    public StreamDeletedException() {
        super("Transaction failed due to underlying stream being deleted.");
        this.stream = null;
    }

    /**
     * Creates a new instance with the specified name of deleted stream.
     *
     * @param stream the name of the deleted stream.
     */
    public StreamDeletedException(String stream) {
        super(String.format("Event stream '%s' is deleted.", stream));
        this.stream = stream;
    }
}
