package com.github.msemys.esjc;

import com.github.msemys.esjc.system.SystemEventType;
import org.junit.Test;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;

public class ITReadAllEventsForwardWithLinkToToDeletedEvents extends AbstractIntegrationTest {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSupplier.get();
    }

    @Test
    public void readsOneEvent() {
        final String deletedStreamName = generateStreamName();
        final String linkedStreamName = generateStreamName();

        eventstore.appendToStream(deletedStreamName, ExpectedVersion.any(), asList(newTestEvent())).join();
        eventstore.appendToStream(linkedStreamName, ExpectedVersion.any(), asList(newLinkEvent(deletedStreamName, 0))).join();
        eventstore.deleteStream(deletedStreamName, ExpectedVersion.any()).join();

        StreamEventsSlice slice = eventstore.readStreamEventsForward(linkedStreamName, 0, 1, true).join();

        assertEquals(1, slice.events.size());
    }

    @Test
    public void linkedEventIsNotResolved() {
        final String deletedStreamName = generateStreamName();
        final String linkedStreamName = generateStreamName();

        eventstore.appendToStream(deletedStreamName, ExpectedVersion.any(), asList(newTestEvent())).join();
        eventstore.appendToStream(linkedStreamName, ExpectedVersion.any(), asList(newLinkEvent(deletedStreamName, 0))).join();
        eventstore.deleteStream(deletedStreamName, ExpectedVersion.any()).join();

        StreamEventsSlice slice = eventstore.readStreamEventsForward(linkedStreamName, 0, 1, true).join();

        assertNull(slice.events.get(0).event);
    }

    @Test
    public void linkedEventIsIncluded() {
        final String deletedStreamName = generateStreamName();
        final String linkedStreamName = generateStreamName();

        eventstore.appendToStream(deletedStreamName, ExpectedVersion.any(), asList(newTestEvent())).join();
        eventstore.appendToStream(linkedStreamName, ExpectedVersion.any(), asList(newLinkEvent(deletedStreamName, 0))).join();
        eventstore.deleteStream(deletedStreamName, ExpectedVersion.any()).join();

        StreamEventsSlice slice = eventstore.readStreamEventsForward(linkedStreamName, 0, 1, true).join();

        assertNotNull(slice.events.get(0).originalEvent());
    }

    @Test
    public void eventIsNotResolved() {
        final String deletedStreamName = generateStreamName();
        final String linkedStreamName = generateStreamName();

        eventstore.appendToStream(deletedStreamName, ExpectedVersion.any(), asList(newTestEvent())).join();
        eventstore.appendToStream(linkedStreamName, ExpectedVersion.any(), asList(newLinkEvent(deletedStreamName, 0))).join();
        eventstore.deleteStream(deletedStreamName, ExpectedVersion.any()).join();

        StreamEventsSlice slice = eventstore.readStreamEventsForward(linkedStreamName, 0, 1, true).join();

        assertFalse(slice.events.get(0).isResolved());
    }

    private static EventData newLinkEvent(String stream, int eventNumber) {
        return EventData.newBuilder()
            .type(SystemEventType.LINK_TO.value)
            .data(eventNumber + "@" + stream)
            .build();
    }

}
