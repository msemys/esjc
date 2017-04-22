package com.github.msemys.esjc;

/**
 * Write operation status.
 */
public enum WriteStatus {

    /**
     * The write operation was successful.
     */
    Success,

    /**
     * The expected version does not match actual stream version.
     */
    WrongExpectedVersion,

    /**
     * The stream has previously existed but is deleted.
     */
    StreamDeleted

}
