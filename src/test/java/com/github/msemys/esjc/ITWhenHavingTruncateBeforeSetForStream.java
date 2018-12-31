package com.github.msemys.esjc;

import org.junit.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class ITWhenHavingTruncateBeforeSetForStream extends AbstractEventStoreTest {

    public ITWhenHavingTruncateBeforeSetForStream(EventStore eventstore) {
        super(eventstore);
    }

    @Test
    public void readEventRespectsTruncateBefore() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(5);
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        eventstore.setStreamMetadata(stream, ExpectedVersion.NO_STREAM, StreamMetadata.newBuilder().truncateBefore(2L).build()).join();

        EventReadResult result1 = eventstore.readEvent(stream, 1, false).join();
        assertEquals(EventReadStatus.NotFound, result1.status);

        EventReadResult result2 = eventstore.readEvent(stream, 2, false).join();
        assertEquals(EventReadStatus.Success, result2.status);
        assertEquals(events.get(2).eventId, result2.event.originalEvent().eventId);
    }

    @Test
    public void readStreamForwardRespectsTruncateBefore() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(5);
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        eventstore.setStreamMetadata(stream, ExpectedVersion.NO_STREAM, StreamMetadata.newBuilder().truncateBefore(2L).build()).join();

        StreamEventsSlice slice = eventstore.readStreamEventsForward(stream, 0, 100, false).join();
        assertEquals(SliceReadStatus.Success, slice.status);
        assertEquals(3, slice.events.size());
        assertArrayEquals(events.stream().skip(2).map(e -> e.eventId).toArray(UUID[]::new),
            slice.events.stream().map(e -> e.event.eventId).toArray(UUID[]::new));
    }

    @Test
    public void readStreamBackwardRespectsTruncateBefore() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(5);
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        eventstore.setStreamMetadata(stream, ExpectedVersion.NO_STREAM, StreamMetadata.newBuilder().truncateBefore(2L).build()).join();

        StreamEventsSlice slice = eventstore.readStreamEventsBackward(stream, -1, 100, false).join();
        assertEquals(SliceReadStatus.Success, slice.status);
        assertEquals(3, slice.events.size());
        assertArrayEquals(events.stream().skip(2).map(e -> e.eventId).toArray(UUID[]::new),
            reverse(slice.events).stream().map(e -> e.event.eventId).toArray(UUID[]::new));
    }

    @Test
    public void afterSettingLessStrictTruncateBeforeReadEventReadsMoreEvents() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(5);
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        eventstore.setStreamMetadata(stream, ExpectedVersion.NO_STREAM, StreamMetadata.newBuilder().truncateBefore(2L).build()).join();

        EventReadResult result1 = eventstore.readEvent(stream, 1, false).join();
        assertEquals(EventReadStatus.NotFound, result1.status);

        EventReadResult result2 = eventstore.readEvent(stream, 2, false).join();
        assertEquals(EventReadStatus.Success, result2.status);
        assertEquals(events.get(2).eventId, result2.event.originalEvent().eventId);

        eventstore.setStreamMetadata(stream, 0, StreamMetadata.newBuilder().truncateBefore(1L).build()).join();

        EventReadResult result3 = eventstore.readEvent(stream, 0, false).join();
        assertEquals(EventReadStatus.NotFound, result3.status);

        EventReadResult result4 = eventstore.readEvent(stream, 1, false).join();
        assertEquals(EventReadStatus.Success, result4.status);
        assertEquals(events.get(1).eventId, result4.event.originalEvent().eventId);
    }

    @Test
    public void afterSettingMoreStrictTruncateBeforeReadEventReadsLessEvents() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(5);
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        eventstore.setStreamMetadata(stream, ExpectedVersion.NO_STREAM, StreamMetadata.newBuilder().truncateBefore(2L).build()).join();

        EventReadResult result1 = eventstore.readEvent(stream, 1, false).join();
        assertEquals(EventReadStatus.NotFound, result1.status);

        EventReadResult result2 = eventstore.readEvent(stream, 2, false).join();
        assertEquals(EventReadStatus.Success, result2.status);
        assertEquals(events.get(2).eventId, result2.event.originalEvent().eventId);

        eventstore.setStreamMetadata(stream, 0, StreamMetadata.newBuilder().truncateBefore(3L).build()).join();

        EventReadResult result3 = eventstore.readEvent(stream, 2, false).join();
        assertEquals(EventReadStatus.NotFound, result3.status);

        EventReadResult result4 = eventstore.readEvent(stream, 3, false).join();
        assertEquals(EventReadStatus.Success, result4.status);
        assertEquals(events.get(3).eventId, result4.event.originalEvent().eventId);
    }

    @Test
    public void lessStrictMaxCountDoesntChangeAnythingForEventRead() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(5);
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        eventstore.setStreamMetadata(stream, ExpectedVersion.NO_STREAM, StreamMetadata.newBuilder().truncateBefore(2L).build()).join();

        EventReadResult result1 = eventstore.readEvent(stream, 1, false).join();
        assertEquals(EventReadStatus.NotFound, result1.status);

        EventReadResult result2 = eventstore.readEvent(stream, 2, false).join();
        assertEquals(EventReadStatus.Success, result2.status);
        assertEquals(events.get(2).eventId, result2.event.originalEvent().eventId);

        eventstore.setStreamMetadata(stream, 0, StreamMetadata.newBuilder().truncateBefore(2L).maxCount(4L).build()).join();

        EventReadResult result3 = eventstore.readEvent(stream, 1, false).join();
        assertEquals(EventReadStatus.NotFound, result3.status);

        EventReadResult result4 = eventstore.readEvent(stream, 2, false).join();
        assertEquals(EventReadStatus.Success, result4.status);
        assertEquals(events.get(2).eventId, result4.event.originalEvent().eventId);
    }

    @Test
    public void moreStrictMaxCountGivesLessEventsForEventRead() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(5);
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        eventstore.setStreamMetadata(stream, ExpectedVersion.NO_STREAM, StreamMetadata.newBuilder().truncateBefore(2L).build()).join();

        EventReadResult result1 = eventstore.readEvent(stream, 1, false).join();
        assertEquals(EventReadStatus.NotFound, result1.status);

        EventReadResult result2 = eventstore.readEvent(stream, 2, false).join();
        assertEquals(EventReadStatus.Success, result2.status);
        assertEquals(events.get(2).eventId, result2.event.originalEvent().eventId);

        eventstore.setStreamMetadata(stream, 0, StreamMetadata.newBuilder().truncateBefore(2L).maxCount(2L).build()).join();

        EventReadResult result3 = eventstore.readEvent(stream, 2, false).join();
        assertEquals(EventReadStatus.NotFound, result3.status);

        EventReadResult result4 = eventstore.readEvent(stream, 3, false).join();
        assertEquals(EventReadStatus.Success, result4.status);
        assertEquals(events.get(3).eventId, result4.event.originalEvent().eventId);
    }

    @Test
    public void afterSettingLessStrictTruncateBeforeReadStreamForwardReadsMoreEvents() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(5);
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        eventstore.setStreamMetadata(stream, ExpectedVersion.NO_STREAM, StreamMetadata.newBuilder().truncateBefore(2L).build()).join();

        StreamEventsSlice slice1 = eventstore.readStreamEventsForward(stream, 0, 100, false).join();
        assertEquals(SliceReadStatus.Success, slice1.status);
        assertEquals(3, slice1.events.size());
        assertArrayEquals(events.stream().skip(2).map(e -> e.eventId).toArray(UUID[]::new),
            slice1.events.stream().map(e -> e.event.eventId).toArray(UUID[]::new));

        eventstore.setStreamMetadata(stream, 0, StreamMetadata.newBuilder().truncateBefore(1L).build()).join();

        StreamEventsSlice slice2 = eventstore.readStreamEventsForward(stream, 0, 100, false).join();
        assertEquals(SliceReadStatus.Success, slice2.status);
        assertEquals(4, slice2.events.size());
        assertArrayEquals(events.stream().skip(1).map(e -> e.eventId).toArray(UUID[]::new),
            slice2.events.stream().map(e -> e.event.eventId).toArray(UUID[]::new));
    }

    @Test
    public void afterSettingMoreStrictTruncateBeforeReadStreamForwardReadsLessEvents() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(5);
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        eventstore.setStreamMetadata(stream, ExpectedVersion.NO_STREAM, StreamMetadata.newBuilder().truncateBefore(2L).build()).join();

        StreamEventsSlice slice1 = eventstore.readStreamEventsForward(stream, 0, 100, false).join();
        assertEquals(SliceReadStatus.Success, slice1.status);
        assertEquals(3, slice1.events.size());
        assertArrayEquals(events.stream().skip(2).map(e -> e.eventId).toArray(UUID[]::new),
            slice1.events.stream().map(e -> e.event.eventId).toArray(UUID[]::new));

        eventstore.setStreamMetadata(stream, 0, StreamMetadata.newBuilder().truncateBefore(3L).build()).join();

        StreamEventsSlice slice2 = eventstore.readStreamEventsForward(stream, 0, 100, false).join();
        assertEquals(SliceReadStatus.Success, slice2.status);
        assertEquals(2, slice2.events.size());
        assertArrayEquals(events.stream().skip(3).map(e -> e.eventId).toArray(UUID[]::new),
            slice2.events.stream().map(e -> e.event.eventId).toArray(UUID[]::new));
    }

    @Test
    public void lessStrictMaxCountDoesntChangeAnythingForStreamForwardRead() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(5);
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        eventstore.setStreamMetadata(stream, ExpectedVersion.NO_STREAM, StreamMetadata.newBuilder().truncateBefore(2L).build()).join();

        StreamEventsSlice slice1 = eventstore.readStreamEventsForward(stream, 0, 100, false).join();
        assertEquals(SliceReadStatus.Success, slice1.status);
        assertEquals(3, slice1.events.size());
        assertArrayEquals(events.stream().skip(2).map(e -> e.eventId).toArray(UUID[]::new),
            slice1.events.stream().map(e -> e.event.eventId).toArray(UUID[]::new));

        eventstore.setStreamMetadata(stream, 0, StreamMetadata.newBuilder().truncateBefore(2L).maxCount(4L).build()).join();

        StreamEventsSlice slice2 = eventstore.readStreamEventsForward(stream, 0, 100, false).join();
        assertEquals(SliceReadStatus.Success, slice2.status);
        assertEquals(3, slice2.events.size());
        assertArrayEquals(events.stream().skip(2).map(e -> e.eventId).toArray(UUID[]::new),
            slice2.events.stream().map(e -> e.event.eventId).toArray(UUID[]::new));
    }

    @Test
    public void moreStrictMaxCountGivesLessEventsForStreamForwardRead() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(5);
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        eventstore.setStreamMetadata(stream, ExpectedVersion.NO_STREAM, StreamMetadata.newBuilder().truncateBefore(2L).build()).join();

        StreamEventsSlice slice1 = eventstore.readStreamEventsForward(stream, 0, 100, false).join();
        assertEquals(SliceReadStatus.Success, slice1.status);
        assertEquals(3, slice1.events.size());
        assertArrayEquals(events.stream().skip(2).map(e -> e.eventId).toArray(UUID[]::new),
            slice1.events.stream().map(e -> e.event.eventId).toArray(UUID[]::new));

        eventstore.setStreamMetadata(stream, 0, StreamMetadata.newBuilder().truncateBefore(2L).maxCount(2L).build()).join();

        StreamEventsSlice slice2 = eventstore.readStreamEventsForward(stream, 0, 100, false).join();
        assertEquals(SliceReadStatus.Success, slice2.status);
        assertEquals(2, slice2.events.size());
        assertArrayEquals(events.stream().skip(3).map(e -> e.eventId).toArray(UUID[]::new),
            slice2.events.stream().map(e -> e.event.eventId).toArray(UUID[]::new));
    }

    @Test
    public void afterSettingLessStrictTruncateBeforeReadStreamBackwardReadsMoreEvents() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(5);
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        eventstore.setStreamMetadata(stream, ExpectedVersion.NO_STREAM, StreamMetadata.newBuilder().truncateBefore(2L).build()).join();

        StreamEventsSlice slice1 = eventstore.readStreamEventsBackward(stream, -1, 100, false).join();
        assertEquals(SliceReadStatus.Success, slice1.status);
        assertEquals(3, slice1.events.size());
        assertArrayEquals(events.stream().skip(2).map(e -> e.eventId).toArray(UUID[]::new),
            reverse(slice1.events).stream().map(e -> e.event.eventId).toArray(UUID[]::new));

        eventstore.setStreamMetadata(stream, 0, StreamMetadata.newBuilder().truncateBefore(1L).build()).join();

        StreamEventsSlice slice2 = eventstore.readStreamEventsBackward(stream, -1, 100, false).join();
        assertEquals(SliceReadStatus.Success, slice2.status);
        assertEquals(4, slice2.events.size());
        assertArrayEquals(events.stream().skip(1).map(e -> e.eventId).toArray(UUID[]::new),
            reverse(slice2.events).stream().map(e -> e.event.eventId).toArray(UUID[]::new));
    }

    @Test
    public void afterSettingMoreStrictTruncateBeforeReadStreamBackwardReadsLessEvents() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(5);
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        eventstore.setStreamMetadata(stream, ExpectedVersion.NO_STREAM, StreamMetadata.newBuilder().truncateBefore(2L).build()).join();

        StreamEventsSlice slice1 = eventstore.readStreamEventsBackward(stream, -1, 100, false).join();
        assertEquals(SliceReadStatus.Success, slice1.status);
        assertEquals(3, slice1.events.size());
        assertArrayEquals(events.stream().skip(2).map(e -> e.eventId).toArray(UUID[]::new),
            reverse(slice1.events).stream().map(e -> e.event.eventId).toArray(UUID[]::new));

        eventstore.setStreamMetadata(stream, 0, StreamMetadata.newBuilder().truncateBefore(3L).build()).join();

        StreamEventsSlice slice2 = eventstore.readStreamEventsBackward(stream, -1, 100, false).join();
        assertEquals(SliceReadStatus.Success, slice2.status);
        assertEquals(2, slice2.events.size());
        assertArrayEquals(events.stream().skip(3).map(e -> e.eventId).toArray(UUID[]::new),
            reverse(slice2.events).stream().map(e -> e.event.eventId).toArray(UUID[]::new));
    }

    @Test
    public void lessStrictMaxCountDoesntChangeAnythingForStreamBackwardRead() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(5);
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        eventstore.setStreamMetadata(stream, ExpectedVersion.NO_STREAM, StreamMetadata.newBuilder().truncateBefore(2L).build()).join();

        StreamEventsSlice slice1 = eventstore.readStreamEventsBackward(stream, -1, 100, false).join();
        assertEquals(SliceReadStatus.Success, slice1.status);
        assertEquals(3, slice1.events.size());
        assertArrayEquals(events.stream().skip(2).map(e -> e.eventId).toArray(UUID[]::new),
            reverse(slice1.events).stream().map(e -> e.event.eventId).toArray(UUID[]::new));

        eventstore.setStreamMetadata(stream, 0, StreamMetadata.newBuilder().truncateBefore(2L).maxCount(4L).build()).join();

        StreamEventsSlice slice2 = eventstore.readStreamEventsBackward(stream, -1, 100, false).join();
        assertEquals(SliceReadStatus.Success, slice2.status);
        assertEquals(3, slice2.events.size());
        assertArrayEquals(events.stream().skip(2).map(e -> e.eventId).toArray(UUID[]::new),
            reverse(slice2.events).stream().map(e -> e.event.eventId).toArray(UUID[]::new));
    }

    @Test
    public void moreStrictMaxCountGivesLessEventsForStreamBackwardRead() {
        final String stream = generateStreamName();

        List<EventData> events = newTestEvents(5);
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, events).join();

        eventstore.setStreamMetadata(stream, ExpectedVersion.NO_STREAM, StreamMetadata.newBuilder().truncateBefore(2L).build()).join();

        StreamEventsSlice slice1 = eventstore.readStreamEventsBackward(stream, -1, 100, false).join();
        assertEquals(SliceReadStatus.Success, slice1.status);
        assertEquals(3, slice1.events.size());
        assertArrayEquals(events.stream().skip(2).map(e -> e.eventId).toArray(UUID[]::new),
            reverse(slice1.events).stream().map(e -> e.event.eventId).toArray(UUID[]::new));

        eventstore.setStreamMetadata(stream, 0, StreamMetadata.newBuilder().truncateBefore(2L).maxCount(2L).build()).join();

        StreamEventsSlice slice2 = eventstore.readStreamEventsBackward(stream, -1, 100, false).join();
        assertEquals(SliceReadStatus.Success, slice2.status);
        assertEquals(2, slice2.events.size());
        assertArrayEquals(events.stream().skip(3).map(e -> e.eventId).toArray(UUID[]::new),
            reverse(slice2.events).stream().map(e -> e.event.eventId).toArray(UUID[]::new));
    }

}
