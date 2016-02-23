package com.github.msemys.esjc.event;

/**
 * Fired when an internal error occurs.
 */
public class ErrorOccurred implements Event {

    public final Throwable throwable;

    public ErrorOccurred(Throwable throwable) {
        this.throwable = throwable;
    }

}
