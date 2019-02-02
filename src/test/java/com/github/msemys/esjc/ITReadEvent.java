package com.github.msemys.esjc;

import io.netty.handler.codec.http.FullHttpRequest;
import org.junit.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static com.github.msemys.esjc.http.HttpClient.newRequest;
import static io.netty.handler.codec.http.HttpMethod.POST;
import static java.util.Arrays.asList;
import static org.junit.Assert.*;

public class ITReadEvent extends AbstractEventStoreTest {

    public ITReadEvent(EventStore eventstore) {
        super(eventstore);
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
    public void returnsCorrectEventId() throws ExecutionException, InterruptedException {
        final String stream = generateStreamName();
        final UUID eventId = UUID.randomUUID();

        // create event with eventId
        final EventData event = EventData.newBuilder()
                .eventId(eventId)
                .type("event0")
                .data(new byte[3])
                .metadata(new byte[2])
                .build();

        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, event).join();

        // read event via tcp
        EventReadResult result = eventstore.readEvent(stream, 0, true).join();

        // compare event IDs
        assertEquals(EventReadStatus.Success, result.status);
        assertEquals(eventId, result.event.originalEvent().eventId);
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

    @Test
    public void returnsEventIfResolveLinkTosIsDisabledAndLinkedStreamIsHardDeleted() {
        final String stream1 = generateStreamName();
        final String stream2 = generateStreamName();
        final String stream3 = generateStreamName();

        eventstore.appendToStream(stream1, ExpectedVersion.NO_STREAM, newTestEvent()).join();
        eventstore.appendToStream(stream2, ExpectedVersion.NO_STREAM, newTestEvent()).join();
        eventstore.appendToStream(stream3, ExpectedVersion.NO_STREAM, asList(
            newTestEvent(),
            EventData.newBuilder()
                .linkTo(0, stream1)
                .build(),
            EventData.newBuilder()
                .linkTo(0, stream2)
                .build(),
            newTestEvent()
        )).join();

        eventstore.deleteStream(stream1, ExpectedVersion.ANY, true).join();

        EventReadResult result1 = eventstore.readEvent(stream3, 1, false).join();
        assertNotNull(result1.event.event);
        assertNull(result1.event.link);

        EventReadResult result2 = eventstore.readEvent(stream3, 2, false).join();
        assertNotNull(result2.event.event);
        assertNull(result2.event.link);
    }

    @Test
    public void returnsEventIfResolveLinkTosIsEnabledAndLinkedStreamIsHardDeleted() {
        final String stream1 = generateStreamName();
        final String stream2 = generateStreamName();
        final String stream3 = generateStreamName();

        eventstore.appendToStream(stream1, ExpectedVersion.NO_STREAM, newTestEvent()).join();
        eventstore.appendToStream(stream2, ExpectedVersion.NO_STREAM, newTestEvent()).join();
        eventstore.appendToStream(stream3, ExpectedVersion.NO_STREAM, asList(
            newTestEvent(),
            EventData.newBuilder()
                .linkTo(0, stream1)
                .build(),
            EventData.newBuilder()
                .linkTo(0, stream2)
                .build(),
            newTestEvent()
        )).join();

        eventstore.deleteStream(stream1, ExpectedVersion.ANY, true).join();

        EventReadResult result1 = eventstore.readEvent(stream3, 1, true).join();
        assertNull(result1.event.event);
        assertNotNull(result1.event.link);

        EventReadResult result2 = eventstore.readEvent(stream3, 2, true).join();
        assertNotNull(result2.event.event);
        assertNotNull(result2.event.link);
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
