package com.github.msemys.esjc;

/**
 * Result type returned after deleting a stream.
 */
public class DeleteResult {

    /**
     * The position of the write in the log.
     */
    public final Position logPosition;

    /**
     * Creates a new instance with the specified log position.
     *
     * @param logPosition the position of the write in the log.
     */
    public DeleteResult(Position logPosition) {
        this.logPosition = logPosition;
    }
}
