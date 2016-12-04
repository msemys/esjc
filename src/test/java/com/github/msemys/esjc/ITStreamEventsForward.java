package com.github.msemys.esjc;

import org.junit.Test;

import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;

public class ITStreamEventsForward extends AbstractIntegrationTest {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSupplier.get();
    }

    @Test(expected = IllegalArgumentException.class)
    public void failsToProcessWhenBatchSizeIsZero() {
        eventstore.streamEventsForward(generateStreamName(), 0, 0, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void failsToProcessWhenBatchSizeOutOfRange() {
        eventstore.streamEventsForward("foo", StreamPosition.START, Integer.MAX_VALUE, false);
    }

    @Test
    public void failsToProcessNonExistingStream() {
        final String stream = generateStreamName();

        Stream<ResolvedEvent> eventStream = eventstore.streamEventsForward(stream, 0, 5, false);

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

        Stream<ResolvedEvent> eventStream = eventstore.streamEventsForward(stream, 0, 5, false);

        try {
            eventStream.findFirst();
            fail("should fail with 'IllegalStateException'");
        } catch (Exception e) {
            assertThat(e, instanceOf(IllegalStateException.class));
            assertEquals("Unexpected read status: StreamDeleted", e.getMessage());
        }
    }

    @Test
    public void readsStreamEventsToEnd() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(10);
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        List<ResolvedEvent> result = eventstore.streamEventsForward(stream, 8, 5, false).collect(toList());

        assertEquals(2, result.size());
        range(0, 2).forEach(i -> assertEquals(events.get(8 + i).eventId, result.get(i).event.eventId));
    }

    @Test
    public void readsStreamEventsFromStartToEndWithSmallBatchSize() {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, newTestEvents(10)).join();

        assertEquals(10, eventstore.streamEventsForward(stream, StreamPosition.START, 2, false).count());
    }

    @Test
    public void readsStreamEventsFromStartToEndWithLargeBatchSize() {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, newTestEvents(10)).join();

        assertEquals(10, eventstore.streamEventsForward(stream, StreamPosition.START, 20, false).count());
    }

    @Test
    public void readsStreamEventsFromStartToEndWithMaxBatchSize() {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, newTestEvents(10)).join();

        assertEquals(10, eventstore.streamEventsForward(stream, StreamPosition.START, 4096, false).count());
    }

    @Test
    public void readsEventsInSameOrderAsWritten() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(10);
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        List<ResolvedEvent> result = eventstore.streamEventsForward(stream, StreamPosition.START, 3, false).collect(toList());

        assertEquals(10, result.size());
        range(0, 10).forEach(i -> assertEquals(events.get(i).eventId, result.get(i).event.eventId));
    }

    @Test
    public void parallelReadsEventsInSameOrderAsWritten() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(100);
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        List<ResolvedEvent> result = eventstore.streamEventsForward(stream, StreamPosition.START, 6, false)
            .parallel()
            .collect(toList());

        assertEquals(100, result.size());
        range(0, 100).forEach(i -> assertEquals(events.get(i).eventId, result.get(i).event.eventId));
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

        assertEquals(51, eventstore.streamEventsForward(stream, StreamPosition.START, 11, false)
            .filter(e -> e.event.eventType.equals("even"))
            .count());

        assertEquals(50, eventstore.streamEventsForward(stream, StreamPosition.START, 11, false)
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

        assertEquals(51, eventstore.streamEventsForward(stream, StreamPosition.START, 11, false)
            .parallel()
            .filter(e -> e.event.eventType.equals("even"))
            .count());

        assertEquals(50, eventstore.streamEventsForward(stream, StreamPosition.START, 11, false)
            .parallel()
            .filter(e -> e.event.eventType.equals("odd"))
            .count());
    }

}
