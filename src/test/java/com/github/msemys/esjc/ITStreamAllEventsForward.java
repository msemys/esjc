package com.github.msemys.esjc;

import org.junit.Test;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ITStreamAllEventsForward extends AbstractIntegrationTest {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSupplier.get();
    }

    @Test(expected = IllegalArgumentException.class)
    public void failsToProcessWhenBatchSizeIsZero() {
        eventstore.streamAllEventsForward(Position.START, 0, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void failsToProcessWhenBatchSizeOutOfRange() {
        eventstore.streamAllEventsForward(Position.START, 4097, false);
    }

    @Test
    public void returnsEmptyStreamIfAskedToProcessFromEnd() {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.noStream(), newTestEvents(20)).join();

        assertFalse(eventstore.streamAllEventsForward(Position.END, 1, false).findFirst().isPresent());
    }

    @Test
    public void readsEventsInSameOrderAsWritten() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(20);
        Position position = eventstore.appendToStream(stream, ExpectedVersion.noStream(), events.get(0)).join().logPosition;
        eventstore.appendToStream(stream, ExpectedVersion.of(0), events.stream().skip(1).collect(toList())).join();

        List<ResolvedEvent> result = eventstore.streamAllEventsForward(position, 3, false).collect(toList());

        assertEquals(20, result.size());
        range(0, 20).forEach(i -> assertEquals(events.get(i).eventId, result.get(i).event.eventId));
    }

    @Test
    public void parallelReadsEventsInSameOrderAsWritten() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(20);
        Position position = eventstore.appendToStream(stream, ExpectedVersion.noStream(), events.get(0)).join().logPosition;
        eventstore.appendToStream(stream, ExpectedVersion.of(0), events.stream().skip(1).collect(toList())).join();

        List<ResolvedEvent> result = eventstore.streamAllEventsForward(position, 3, false)
            .parallel()
            .collect(toList());

        assertEquals(20, result.size());
        range(0, 20).forEach(i -> assertEquals(events.get(i).eventId, result.get(i).event.eventId));
    }

    @Test
    public void readsAllEventsOneByOneUntilEndOfStream() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(20);
        eventstore.appendToStream(stream, ExpectedVersion.noStream(), events).join();

        List<ResolvedEvent> allEvents = eventstore.streamAllEventsForward(Position.START, 1, false).collect(toList());

        int index = allEvents.indexOf(allEvents.stream()
            .filter(e -> e.event.eventId.equals(events.get(0).eventId))
            .findFirst()
            .get());

        List<ResolvedEvent> allEventsSlice = allEvents.stream().skip(index).limit(events.size()).collect(toList());

        assertEquals(20, allEventsSlice.size());
        range(0, 20).forEach(i -> assertEquals(events.get(i).eventId, allEventsSlice.get(i).event.eventId));
    }

    @Test
    public void parallelReadsAllEventsOneByOneUntilEndOfStream() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(20);
        eventstore.appendToStream(stream, ExpectedVersion.noStream(), events).join();

        List<ResolvedEvent> allEvents = eventstore.streamAllEventsForward(Position.START, 1, false)
            .parallel()
            .collect(toList());

        int index = allEvents.indexOf(allEvents.stream()
            .filter(e -> e.event.eventId.equals(events.get(0).eventId))
            .findFirst()
            .get());

        List<ResolvedEvent> allEventsSlice = allEvents.stream().skip(index).limit(events.size()).collect(toList());

        assertEquals(20, allEventsSlice.size());
        range(0, 20).forEach(i -> assertEquals(events.get(i).eventId, allEventsSlice.get(i).event.eventId));
    }

    @Test
    public void readsAllEventsUntilEndOfStream() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(20);
        eventstore.appendToStream(stream, ExpectedVersion.noStream(), events).join();

        List<ResolvedEvent> allEvents = eventstore.streamAllEventsForward(Position.START, 5, false).collect(toList());

        int index = allEvents.indexOf(allEvents.stream()
            .filter(e -> e.event.eventId.equals(events.get(0).eventId))
            .findFirst()
            .get());

        List<ResolvedEvent> allEventsSlice = allEvents.stream().skip(index).limit(events.size()).collect(toList());

        assertEquals(20, allEventsSlice.size());
        range(0, 20).forEach(i -> assertEquals(events.get(i).eventId, allEventsSlice.get(i).event.eventId));
    }

    @Test
    public void parallelReadsAllEventsUntilEndOfStream() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(20);
        eventstore.appendToStream(stream, ExpectedVersion.noStream(), events).join();

        List<ResolvedEvent> allEvents = eventstore.streamAllEventsForward(Position.START, 5, false)
            .parallel()
            .collect(toList());

        int index = allEvents.indexOf(allEvents.stream()
            .filter(e -> e.event.eventId.equals(events.get(0).eventId))
            .findFirst()
            .get());

        List<ResolvedEvent> allEventsSlice = allEvents.stream().skip(index).limit(events.size()).collect(toList());

        assertEquals(20, allEventsSlice.size());
        range(0, 20).forEach(i -> assertEquals(events.get(i).eventId, allEventsSlice.get(i).event.eventId));
    }

    @Test
    public void readsAllEventsUntilEndOfStreamUsingMaxBatchSize() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(20);
        eventstore.appendToStream(stream, ExpectedVersion.noStream(), events).join();

        List<ResolvedEvent> allEvents = eventstore.streamAllEventsForward(Position.START, 4096, false).collect(toList());

        int index = allEvents.indexOf(allEvents.stream()
            .filter(e -> e.event.eventId.equals(events.get(0).eventId))
            .findFirst()
            .get());

        List<ResolvedEvent> allEventsSlice = allEvents.stream().skip(index).limit(events.size()).collect(toList());

        assertEquals(20, allEventsSlice.size());
        range(0, 20).forEach(i -> assertEquals(events.get(i).eventId, allEventsSlice.get(i).event.eventId));
    }

    @Test
    public void parallelReadsAllEventsUntilEndOfStreamUsingMaxBatchSize() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(20);
        eventstore.appendToStream(stream, ExpectedVersion.noStream(), events).join();

        List<ResolvedEvent> allEvents = eventstore.streamAllEventsForward(Position.START, 4096, false)
            .parallel()
            .collect(toList());

        int index = allEvents.indexOf(allEvents.stream()
            .filter(e -> e.event.eventId.equals(events.get(0).eventId))
            .findFirst()
            .get());

        List<ResolvedEvent> allEventsSlice = allEvents.stream().skip(index).limit(events.size()).collect(toList());

        assertEquals(20, allEventsSlice.size());
        range(0, 20).forEach(i -> assertEquals(events.get(i).eventId, allEventsSlice.get(i).event.eventId));
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

        eventstore.appendToStream(stream, ExpectedVersion.noStream(), events).join();

        assertEquals(51, eventstore.streamAllEventsForward(Position.START, 17, false)
            .filter(e -> e.event.eventStreamId.equals(stream))
            .filter(e -> e.event.eventType.equals("even"))
            .count());

        assertEquals(50, eventstore.streamAllEventsForward(Position.START, 17, false)
            .filter(e -> e.event.eventStreamId.equals(stream))
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

        eventstore.appendToStream(stream, ExpectedVersion.noStream(), events).join();

        assertEquals(51, eventstore.streamAllEventsForward(Position.START, 82, false)
            .parallel()
            .filter(e -> e.event.eventStreamId.equals(stream))
            .filter(e -> e.event.eventType.equals("even"))
            .count());

        assertEquals(50, eventstore.streamAllEventsForward(Position.START, 82, false)
            .parallel()
            .filter(e -> e.event.eventStreamId.equals(stream))
            .filter(e -> e.event.eventType.equals("odd"))
            .count());
    }

    private static List<EventData> newTestEvents(int count) {
        return range(0, count).mapToObj(i -> newTestEvent()).collect(toList());
    }

}
