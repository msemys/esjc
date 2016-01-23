package lt.msemys.esjc.operation;

import lt.msemys.esjc.EventStoreException;

public class StreamDeletedException extends EventStoreException {
    public final String stream;

    public StreamDeletedException() {
        super("Transaction failed due to underlying stream being deleted.");
        this.stream = null;
    }

    public StreamDeletedException(String stream) {
        super(String.format("Event stream '%s' is deleted.", stream));
        this.stream = stream;
    }
}
