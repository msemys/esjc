package com.github.msemys.esjc;

/**
 * Result type returned after writing to a stream.
 */
public class WriteResult {

    /**
     * The next expected version for the stream.
     */
    public final int nextExpectedVersion;

    /**
     * The position of the write in the log.
     */
    public final Position logPosition;

    /**
     * Creates a new instance with the specified next expected stream version and log position.
     *
     * @param nextExpectedVersion the next expected version for the stream.
     * @param logPosition         the position of the write in the log.
     */
    public WriteResult(int nextExpectedVersion, Position logPosition) {
        this.nextExpectedVersion = nextExpectedVersion;
        this.logPosition = logPosition;
    }
}
