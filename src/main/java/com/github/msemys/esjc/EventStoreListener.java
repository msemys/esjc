package com.github.msemys.esjc;

import com.github.msemys.esjc.event.Event;

import java.util.EventListener;

public interface EventStoreListener extends EventListener {

    void onEvent(Event event);

}
