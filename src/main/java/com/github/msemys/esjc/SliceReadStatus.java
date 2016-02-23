package com.github.msemys.esjc;

/**
 * Stream slice read status.
 */
public enum SliceReadStatus {

    /**
     * The read was successful.
     */
    Success,

    /**
     * The stream was not found.
     */
    StreamNotFound,

    /**
     * The stream has previously existed but is deleted.
     */
    StreamDeleted
}
