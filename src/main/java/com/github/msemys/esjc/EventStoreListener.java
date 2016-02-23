package com.github.msemys.esjc;

import com.github.msemys.esjc.event.Event;

import java.util.EventListener;

/**
 * The listener interface for receiving client events.
 */
public interface EventStoreListener extends EventListener {

    /**
     * Invoked when the client event occurs.
     *
     * @param event client event.
     */
    void onEvent(Event event);

}
