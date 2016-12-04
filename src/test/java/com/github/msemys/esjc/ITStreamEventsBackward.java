package com.github.msemys.esjc;

import org.junit.Test;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;

public class ITStreamEventsBackward extends AbstractIntegrationTest {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSupplier.get();
    }

    @Test(expected = IllegalArgumentException.class)
    public void failsToProcessWhenBatchSizeIsZero() {
        eventstore.streamEventsBackward(generateStreamName(), 0, 0, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void failsToProcessWhenBatchSizeOutOfRange() {
        eventstore.streamEventsBackward("foo", StreamPosition.START, Integer.MAX_VALUE, false);
    }

    @Test
    public void failsToProcessNonExistingStream() {
        final String stream = generateStreamName();

        Stream<ResolvedEvent> eventStream = eventstore.streamEventsBackward(stream, 0, 5, false);

        try {
            eventStream.findFirst();
            fail("should fail with 'IllegalStateException'");
        } catch (Exception e) {
            assertThat(e, instanceOf(IllegalStateException.class));
            assertEquals("Unexpected read status: StreamNotFound", e.getMessage());
        }
    }

    @Test
    public void failsToProcessDeletedStream() {
        final String stream = generateStreamName();

        eventstore.deleteStream(stream, ExpectedVersion.NO_STREAM, true).join();

        Stream<ResolvedEvent> eventStream = eventstore.streamEventsBackward(stream, 0, 5, false);

        try {
            eventStream.findFirst();
            fail("should fail with 'IllegalStateException'");
        } catch (Exception e) {
            assertThat(e, instanceOf(IllegalStateException.class));
            assertEquals("Unexpected read status: StreamDeleted", e.getMessage());
        }
    }


    @Test
    public void readsStreamEventsToBegging() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(10);
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        List<ResolvedEvent> result = eventstore.streamEventsBackward(stream, 8, 5, false).collect(toList());

        List<EventData> reversedEvents = reverse(events.stream().limit(9).collect(toList()));

        assertEquals(9, result.size());
        range(0, 9).forEach(i -> assertEquals(reversedEvents.get(i).eventId, result.get(i).event.eventId));
    }

    @Test
    public void readsStreamEventsFromEndToStartWithSmallBatchSize() {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, newTestEvents(10)).join();

        assertEquals(10, eventstore.streamEventsBackward(stream, StreamPosition.END, 2, false).count());
    }

    @Test
    public void readsStreamEventsFromEndToStartWithLargeBatchSize() {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, newTestEvents(10)).join();

        assertEquals(10, eventstore.streamEventsBackward(stream, StreamPosition.END, 20, false).count());
    }

    @Test
    public void readsStreamEventsFromEndToStartWithMaxBatchSize() {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, newTestEvents(10)).join();

        assertEquals(10, eventstore.streamEventsBackward(stream, StreamPosition.END, 4096, false).count());
    }

    @Test
    public void readsEventsReversedComparedToWritten() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(10);
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        List<ResolvedEvent> result = eventstore.streamEventsBackward(stream, StreamPosition.END, 3, false).collect(toList());

        List<EventData> reversedEvents = reverse(events);

        assertEquals(10, result.size());
        range(0, 10).forEach(i -> assertEquals(reversedEvents.get(i).eventId, result.get(i).event.eventId));
    }

    @Test
    public void parallelReadsEventsReversedComparedToWritten() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(100);
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        List<ResolvedEvent> result = eventstore.streamEventsBackward(stream, StreamPosition.END, 7, false)
            .parallel()
            .collect(toList());

        List<EventData> reversedEvents = reverse(events);

        assertEquals(100, result.size());
        range(0, 100).forEach(i -> assertEquals(reversedEvents.get(i).eventId, result.get(i).event.eventId));
    }

    @Test
    public void aggregatesEvents() {
        final String stream = generateStreamName();

        List<EventData> events = range(0, 101)
            .mapToObj(i -> EventData.newBuilder()
                .type(i % 2 == 0 ? "even" : "odd")
                .data(String.valueOf(i))
                .build())
            .collect(toList());

        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        assertEquals(51, eventstore.streamEventsBackward(stream, StreamPosition.END, 10, false)
            .filter(e -> e.event.eventType.equals("even"))
            .count());

        assertEquals(50, eventstore.streamEventsBackward(stream, StreamPosition.END, 10, false)
            .filter(e -> e.event.eventType.equals("odd"))
            .count());
    }

    @Test
    public void parallelAggregatesEvents() {
        final String stream = generateStreamName();

        List<EventData> events = range(0, 101)
            .mapToObj(i -> EventData.newBuilder()
                .type(i % 2 == 0 ? "even" : "odd")
                .data(String.valueOf(i))
                .build())
            .collect(toList());

        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        assertEquals(51, eventstore.streamEventsBackward(stream, StreamPosition.END, 10, false)
            .parallel()
            .filter(e -> e.event.eventType.equals("even"))
            .count());

        assertEquals(50, eventstore.streamEventsBackward(stream, StreamPosition.END, 10, false)
            .parallel()
            .filter(e -> e.event.eventType.equals("odd"))
            .count());
    }

    @Test
    public void readsFirstEvent() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(10);
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        Optional<ResolvedEvent> event = eventstore.streamEventsBackward(stream, StreamPosition.START, 4, false).findFirst();

        assertTrue(event.isPresent());
        assertEquals(events.get(0).eventId, event.get().event.eventId);
    }

    @Test
    public void readsLastEvent() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(10);
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        Optional<ResolvedEvent> event = eventstore.streamEventsBackward(stream, StreamPosition.END, 1, false).findFirst();

        assertTrue(event.isPresent());
        assertEquals(events.get(9).eventId, event.get().event.eventId);
    }

}
