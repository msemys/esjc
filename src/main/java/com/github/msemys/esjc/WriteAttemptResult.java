package com.github.msemys.esjc;

/**
 * Result type with operation status returned after writing to a stream.
 */
public class WriteAttemptResult extends WriteResult {

    /**
     * The status of the write operation.
     */
    public final WriteStatus status;

    /**
     * Creates a new instance with the specified next expected stream version, log position and operation status.
     *
     * @param nextExpectedVersion the next expected version for the stream.
     * @param logPosition         the position of the write in the log.
     * @param status              the status of the write operation.
     */
    public WriteAttemptResult(int nextExpectedVersion, Position logPosition, WriteStatus status) {
        super(nextExpectedVersion, logPosition);
        this.status = status;
    }

}
