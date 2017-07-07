package com.github.msemys.esjc;

import org.junit.Test;

import static org.junit.Assert.*;

public class ITReadEventOfLinkToToDeletedEvent extends AbstractIntegrationTest {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSupplier.get();
    }

    @Test
    public void readsLinkedEvent() {
        final String deletedStreamName = generateStreamName();
        final String linkedStreamName = generateStreamName();

        eventstore.appendToStream(deletedStreamName, ExpectedVersion.ANY, newTestEvent()).join();
        eventstore.appendToStream(linkedStreamName, ExpectedVersion.ANY, newLinkEvent(deletedStreamName, 0)).join();
        eventstore.deleteStream(deletedStreamName, ExpectedVersion.ANY).join();

        EventReadResult result = eventstore.readEvent(linkedStreamName, 0, true).join();

        assertNotNull("Missing linked event", result.event.link);
        assertNull("Deleted event was resolved", result.event.event);
        assertEquals(EventReadStatus.Success, result.status);
    }

    private static EventData newLinkEvent(String stream, long eventNumber) {
        return EventData.newBuilder()
            .linkTo(eventNumber, stream)
            .build();
    }

}
