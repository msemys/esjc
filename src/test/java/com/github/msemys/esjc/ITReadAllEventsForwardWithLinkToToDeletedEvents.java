package com.github.msemys.esjc;

import org.junit.Test;

import static org.junit.Assert.*;

public class ITReadAllEventsForwardWithLinkToToDeletedEvents extends AbstractEventStoreTest {

    public ITReadAllEventsForwardWithLinkToToDeletedEvents(EventStore eventstore) {
        super(eventstore);
    }

    @Test
    public void readsOneEvent() {
        final String deletedStreamName = generateStreamName();
        final String linkedStreamName = generateStreamName();

        eventstore.appendToStream(deletedStreamName, ExpectedVersion.ANY, newTestEvent()).join();
        eventstore.appendToStream(linkedStreamName, ExpectedVersion.ANY, newLinkEvent(deletedStreamName, 0)).join();
        eventstore.deleteStream(deletedStreamName, ExpectedVersion.ANY).join();

        StreamEventsSlice slice = eventstore.readStreamEventsForward(linkedStreamName, 0, 1, true).join();

        assertEquals(1, slice.events.size());

        ResolvedEvent resolvedEvent = slice.events.get(0);
        assertNull("Linked event was resolved", resolvedEvent.event);
        assertNotNull("Linked event is not included", resolvedEvent.originalEvent());
        assertFalse("Event was resolved", resolvedEvent.isResolved());
    }

}
