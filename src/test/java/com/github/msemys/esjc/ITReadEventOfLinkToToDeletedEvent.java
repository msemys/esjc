package com.github.msemys.esjc;

import com.github.msemys.esjc.system.SystemEventType;
import org.junit.Test;

import static java.util.Arrays.asList;
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

        eventstore.appendToStream(deletedStreamName, ExpectedVersion.any(), asList(newTestEvent())).join();
        eventstore.appendToStream(linkedStreamName, ExpectedVersion.any(), asList(newLinkEvent(deletedStreamName, 0))).join();
        eventstore.deleteStream(deletedStreamName, ExpectedVersion.any()).join();

        EventReadResult result = eventstore.readEvent(linkedStreamName, 0, true).join();

        assertNotNull("Missing linked event", result.event.link);
        assertNull("Deleted event was resolved", result.event.event);
        assertEquals(EventReadStatus.Success, result.status);
    }

    private static EventData newLinkEvent(String stream, int eventNumber) {
        return EventData.newBuilder()
            .type(SystemEventType.LINK_TO.value)
            .data(eventNumber + "@" + stream)
            .build();
    }

}
