package com.github.msemys.esjc;

import org.junit.Test;

import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;

public class ITReadEvent extends AbstractIntegrationTest {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSupplier.get();
    }

    @Test(expected = IllegalArgumentException.class)
    public void failsWhenStreamIdIsNull() {
        eventstore.readEvent(null, 0, false).join();
    }

    @Test(expected = IllegalArgumentException.class)
    public void failsWhenStreamIdIsEmpty() {
        eventstore.readEvent("", 0, false).join();
    }

    @Test(expected = IllegalArgumentException.class)
    public void failsWhenEventNumberIsLessThanMinusOne() {
        eventstore.readEvent("stream", -2, false).join();
    }

    @Test
    public void returnsNoStreamIfStreamNotFound() {
        final String stream = generateStreamName();

        EventReadResult result = eventstore.readEvent(stream, 5, false).join();

        assertEquals(EventReadStatus.NoStream, result.status);
        assertNull(result.event);
        assertEquals(stream, result.stream);
        assertEquals(5, result.eventNumber);
    }

    @Test
    public void returnsNoStreamIfRequestedLastEventInEmptyStream() {
        final String stream = generateStreamName();

        EventReadResult result = eventstore.readEvent(stream, StreamPosition.END, false).join();

        assertEquals(EventReadStatus.NoStream, result.status);
    }

    @Test
    public void returnsStreamDeletedIfStreamWasDeleted() {
        final String stream = generateStreamName();

        eventstore.deleteStream(stream, ExpectedVersion.NO_STREAM, true).join();

        EventReadResult result = eventstore.readEvent(stream, 5, false).join();

        assertEquals(EventReadStatus.StreamDeleted, result.status);
        assertNull(result.event);
        assertEquals(stream, result.stream);
        assertEquals(5, result.eventNumber);
    }

    @Test
    public void returnsNotFoundIfStreamDoesNotHaveEvent() {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, newTestEvents()).join();

        EventReadResult result = eventstore.readEvent(stream, 5, false).join();

        assertEquals(EventReadStatus.NotFound, result.status);
        assertNull(result.event);
        assertEquals(stream, result.stream);
        assertEquals(5, result.eventNumber);
    }

    @Test
    public void returnsExistingEvent() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents();
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        EventReadResult result = eventstore.readEvent(stream, 0, false).join();

        assertEquals(EventReadStatus.Success, result.status);
        assertEquals(events.get(0).eventId, result.event.originalEvent().eventId);
        assertEquals(stream, result.stream);
        assertEquals(0, result.eventNumber);
        assertNotNull(result.event.originalEvent().created);
    }

    @Test
    public void retrievesJsonFlagProperly() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents();
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        EventReadResult result = eventstore.readEvent(stream, 1, false).join();

        assertEquals(EventReadStatus.Success, result.status);
        assertEquals(events.get(1).eventId, result.event.originalEvent().eventId);
        assertTrue(result.event.originalEvent().isJson);
    }

    @Test
    public void returnsLastEventInStreamIfEventNumberIsMinusOne() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents();
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        EventReadResult result = eventstore.readEvent(stream, -1, false).join();

        assertEquals(EventReadStatus.Success, result.status);
        assertEquals(events.get(1).eventId, result.event.originalEvent().eventId);
        assertEquals(stream, result.stream);
        assertEquals(-1, result.eventNumber);
        assertNotNull(result.event.originalEvent().created);
    }

    private static List<EventData> newTestEvents() {
        return asList(
            EventData.newBuilder()
                .type("event0")
                .data(new byte[3])
                .metadata(new byte[2])
                .build(),
            EventData.newBuilder()
                .type("event1")
                .jsonData(new byte[7])
                .metadata(new byte[10])
                .build()
        );
    }

}
