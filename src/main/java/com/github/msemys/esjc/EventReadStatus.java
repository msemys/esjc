package com.github.msemys.esjc;

/**
 * Single event read operation status.
 */
public enum EventReadStatus {

    /**
     * The read operation was successful.
     */
    Success,

    /**
     * The event was not found.
     */
    NotFound,

    /**
     * The stream was not found.
     */
    NoStream,

    /**
     * The stream previously existed but was deleted.
     */
    StreamDeleted;
}
