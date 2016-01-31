package lt.msemys.esjc;

import lt.msemys.esjc.event.Event;

import java.util.EventListener;

public interface EventStoreListener extends EventListener {

    void onEvent(Event event);

}
