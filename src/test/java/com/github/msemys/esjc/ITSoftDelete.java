package com.github.msemys.esjc;

import com.github.msemys.esjc.operation.StreamDeletedException;
import com.github.msemys.esjc.operation.WrongExpectedVersionException;
import org.junit.Test;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;

public class ITSoftDelete extends AbstractIntegrationTest {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSupplier.get();
    }

    @Test
    public void softDeletedStreamReturnsNoStreamAndNoEventsOnRead() {
        final String stream = generateStreamName();

        assertEquals(1, eventstore.appendToStream(stream, ExpectedVersion.noStream(), newTestEvents()).join().nextExpectedVersion);

        eventstore.deleteStream(stream, ExpectedVersion.of(1)).join();

        StreamEventsSlice slice = eventstore.readStreamEventsForward(stream, 0, 100, false).join();
        assertEquals(SliceReadStatus.StreamNotFound, slice.status);
        assertTrue(slice.events.isEmpty());
        assertEquals(1, slice.lastEventNumber);
    }

    @Test
    public void softDeletedStreamAllowsRecreationWhenExpectedVersionAny() {
        final String stream = generateStreamName();

        assertEquals(1, eventstore.appendToStream(stream, ExpectedVersion.noStream(), newTestEvents()).join().nextExpectedVersion);

        eventstore.deleteStream(stream, ExpectedVersion.of(1)).join();

        List<EventData> events = asList(newTestEvent(), newTestEvent(), newTestEvent());
        assertEquals(4, eventstore.appendToStream(stream, ExpectedVersion.any(), events).join().nextExpectedVersion);

        StreamEventsSlice slice = eventstore.readStreamEventsForward(stream, 0, 100, false).join();
        assertEquals(SliceReadStatus.Success, slice.status);
        assertEquals(4, slice.lastEventNumber);
        assertEquals(3, slice.events.size());
        assertArrayEquals(events.stream().map(e -> e.eventId).toArray(UUID[]::new),
            slice.events.stream().map(e -> e.originalEvent().eventId).toArray(UUID[]::new));
        assertArrayEquals(new Integer[]{2, 3, 4},
            slice.events.stream().map(e -> e.originalEvent().eventNumber).toArray(Integer[]::new));

        StreamMetadataResult streamMetadataResult = eventstore.getStreamMetadata(stream).join();
        assertEquals(2, streamMetadataResult.streamMetadata.truncateBefore.intValue());
        assertEquals(1, streamMetadataResult.metastreamVersion);
    }

    @Test
    public void softDeletedStreamAllowsRecreationWhenExpectedVersionNoStream() {
        final String stream = generateStreamName();

        assertEquals(1, eventstore.appendToStream(stream, ExpectedVersion.noStream(), newTestEvents()).join().nextExpectedVersion);

        eventstore.deleteStream(stream, ExpectedVersion.of(1)).join();

        List<EventData> events = asList(newTestEvent(), newTestEvent(), newTestEvent());
        assertEquals(4, eventstore.appendToStream(stream, ExpectedVersion.noStream(), events).join().nextExpectedVersion);

        StreamEventsSlice slice = eventstore.readStreamEventsForward(stream, 0, 100, false).join();
        assertEquals(SliceReadStatus.Success, slice.status);
        assertEquals(4, slice.lastEventNumber);
        assertEquals(3, slice.events.size());
        assertArrayEquals(events.stream().map(e -> e.eventId).toArray(UUID[]::new),
            slice.events.stream().map(e -> e.originalEvent().eventId).toArray(UUID[]::new));
        assertArrayEquals(new Integer[]{2, 3, 4},
            slice.events.stream().map(e -> e.originalEvent().eventNumber).toArray(Integer[]::new));

        StreamMetadataResult streamMetadataResult = eventstore.getStreamMetadata(stream).join();
        assertEquals(2, streamMetadataResult.streamMetadata.truncateBefore.intValue());
        assertEquals(1, streamMetadataResult.metastreamVersion);
    }

    @Test
    public void softDeletedStreamAllowsRecreationWhenExpectedVersionIsExact() {
        final String stream = generateStreamName();

        assertEquals(1, eventstore.appendToStream(stream, ExpectedVersion.noStream(), newTestEvents()).join().nextExpectedVersion);

        eventstore.deleteStream(stream, ExpectedVersion.of(1)).join();

        List<EventData> events = asList(newTestEvent(), newTestEvent(), newTestEvent());
        assertEquals(4, eventstore.appendToStream(stream, ExpectedVersion.of(1), events).join().nextExpectedVersion);

        StreamEventsSlice slice = eventstore.readStreamEventsForward(stream, 0, 100, false).join();
        assertEquals(SliceReadStatus.Success, slice.status);
        assertEquals(4, slice.lastEventNumber);
        assertEquals(3, slice.events.size());
        assertArrayEquals(events.stream().map(e -> e.eventId).toArray(UUID[]::new),
            slice.events.stream().map(e -> e.originalEvent().eventId).toArray(UUID[]::new));
        assertArrayEquals(new Integer[]{2, 3, 4},
            slice.events.stream().map(e -> e.originalEvent().eventNumber).toArray(Integer[]::new));

        StreamMetadataResult streamMetadataResult = eventstore.getStreamMetadata(stream).join();
        assertEquals(2, streamMetadataResult.streamMetadata.truncateBefore.intValue());
        assertEquals(1, streamMetadataResult.metastreamVersion);
    }

    @Test
    public void softDeletedStreamWhenRecreatedPreservesMetadataExceptTruncateBefore() {
        final String stream = generateStreamName();

        assertEquals(1, eventstore.appendToStream(stream, ExpectedVersion.noStream(), newTestEvents()).join().nextExpectedVersion);

        WriteResult writeResult = eventstore.setStreamMetadata(stream, ExpectedVersion.noStream(),
            StreamMetadata.newBuilder()
                .truncateBefore(Integer.MAX_VALUE)
                .maxCount(100)
                .aclDeleteRoles(asList("some-role"))
                .customProperty("key1", true)
                .customProperty("key2", 17)
                .customProperty("key3", "some value")
                .build()
        ).join();
        assertEquals(0, writeResult.nextExpectedVersion);

        List<EventData> events = asList(newTestEvent(), newTestEvent(), newTestEvent());
        assertEquals(4, eventstore.appendToStream(stream, ExpectedVersion.of(1), events).join().nextExpectedVersion);

        StreamEventsSlice slice = eventstore.readStreamEventsForward(stream, 0, 100, false).join();
        assertEquals(SliceReadStatus.Success, slice.status);
        assertEquals(4, slice.lastEventNumber);
        assertEquals(3, slice.events.size());
        assertArrayEquals(events.stream().map(e -> e.eventId).toArray(UUID[]::new),
            slice.events.stream().map(e -> e.originalEvent().eventId).toArray(UUID[]::new));
        assertArrayEquals(new Integer[]{2, 3, 4},
            slice.events.stream().map(e -> e.originalEvent().eventNumber).toArray(Integer[]::new));

        StreamMetadataResult streamMetadataResult = eventstore.getStreamMetadata(stream).join();
        assertEquals(1, streamMetadataResult.metastreamVersion);
        assertEquals(2, streamMetadataResult.streamMetadata.truncateBefore.intValue());
        assertEquals(100, streamMetadataResult.streamMetadata.maxCount.intValue());
        assertEquals("some-role", streamMetadataResult.streamMetadata.acl.deleteRoles.get(0));
        assertTrue(streamMetadataResult.streamMetadata.getCustomProperty("key1").toBoolean());
        assertEquals(Integer.valueOf(17), streamMetadataResult.streamMetadata.getCustomProperty("key2").toInteger());
        assertEquals("some value", streamMetadataResult.streamMetadata.getCustomProperty("key3").toString());
    }

    @Test
    public void softDeletedStreamCanBeHardDeleted() {
        final String stream = generateStreamName();

        assertEquals(1, eventstore.appendToStream(stream, ExpectedVersion.noStream(), newTestEvents()).join().nextExpectedVersion);

        eventstore.deleteStream(stream, ExpectedVersion.of(1)).join();
        eventstore.deleteStream(stream, ExpectedVersion.any(), true).join();

        StreamEventsSlice slice = eventstore.readStreamEventsForward(stream, 0, 100, false).join();
        assertEquals(SliceReadStatus.StreamDeleted, slice.status);

        StreamMetadataResult streamMetadataResult = eventstore.getStreamMetadata(stream).join();
        assertTrue(streamMetadataResult.isStreamDeleted);

        try {
            eventstore.appendToStream(stream, ExpectedVersion.any(), newTestEvent()).join();
            fail("append should fail with 'StreamDeletedException'");
        } catch (Exception e) {
            assertThat(e.getCause(), instanceOf(StreamDeletedException.class));
        }
    }

    @Test
    public void softDeletedStreamAllowsRecreationOnlyForFirstWrite() {
        final String stream = generateStreamName();

        assertEquals(1, eventstore.appendToStream(stream, ExpectedVersion.noStream(), newTestEvents()).join().nextExpectedVersion);

        eventstore.deleteStream(stream, ExpectedVersion.of(1)).join();

        List<EventData> events = asList(newTestEvent(), newTestEvent(), newTestEvent());
        assertEquals(4, eventstore.appendToStream(stream, ExpectedVersion.noStream(), events).join().nextExpectedVersion);

        try {
            eventstore.appendToStream(stream, ExpectedVersion.noStream(), newTestEvent()).join();
            fail("append should fail with 'WrongExpectedVersionException'");
        } catch (Exception e) {
            assertThat(e.getCause(), instanceOf(WrongExpectedVersionException.class));
        }

        StreamEventsSlice slice = eventstore.readStreamEventsForward(stream, 0, 100, false).join();
        assertEquals(SliceReadStatus.Success, slice.status);
        assertEquals(4, slice.lastEventNumber);
        assertEquals(3, slice.events.size());
        assertArrayEquals(events.stream().map(e -> e.eventId).toArray(UUID[]::new),
            slice.events.stream().map(e -> e.originalEvent().eventId).toArray(UUID[]::new));
        assertArrayEquals(new Integer[]{2, 3, 4},
            slice.events.stream().map(e -> e.originalEvent().eventNumber).toArray(Integer[]::new));

        StreamMetadataResult streamMetadataResult = eventstore.getStreamMetadata(stream).join();
        assertEquals(2, streamMetadataResult.streamMetadata.truncateBefore.intValue());
        assertEquals(1, streamMetadataResult.metastreamVersion);
    }

    @Test
    public void softDeletedStreamAppendsBothWritesWhenExpectedVersionAny() {
        final String stream = generateStreamName();

        assertEquals(1, eventstore.appendToStream(stream, ExpectedVersion.noStream(), newTestEvents()).join().nextExpectedVersion);

        eventstore.deleteStream(stream, ExpectedVersion.of(1)).join();

        List<EventData> events1 = asList(newTestEvent(), newTestEvent(), newTestEvent());
        List<EventData> events2 = asList(newTestEvent(), newTestEvent());
        assertEquals(4, eventstore.appendToStream(stream, ExpectedVersion.any(), events1).join().nextExpectedVersion);
        assertEquals(6, eventstore.appendToStream(stream, ExpectedVersion.any(), events2).join().nextExpectedVersion);

        StreamEventsSlice slice = eventstore.readStreamEventsForward(stream, 0, 100, false).join();
        assertEquals(SliceReadStatus.Success, slice.status);
        assertEquals(6, slice.lastEventNumber);
        assertEquals(5, slice.events.size());
        assertArrayEquals(Stream.concat(events1.stream(), events2.stream()).map(e -> e.eventId).toArray(UUID[]::new),
            slice.events.stream().map(e -> e.originalEvent().eventId).toArray(UUID[]::new));
        assertArrayEquals(new Integer[]{2, 3, 4, 5, 6},
            slice.events.stream().map(e -> e.originalEvent().eventNumber).toArray(Integer[]::new));

        StreamMetadataResult streamMetadataResult = eventstore.getStreamMetadata(stream).join();
        assertEquals(2, streamMetadataResult.streamMetadata.truncateBefore.intValue());
        assertEquals(1, streamMetadataResult.metastreamVersion);
    }

    @Test
    public void settingJsonMetadataOnEmptySoftDeletedStreamRecreatesStreamNotOverridingMetadata() {
        final String stream = generateStreamName();

        eventstore.deleteStream(stream, ExpectedVersion.noStream()).join();

        assertEquals(1, eventstore.setStreamMetadata(stream, ExpectedVersion.of(0),
            StreamMetadata.newBuilder()
                .maxCount(100)
                .aclDeleteRoles(asList("some-role"))
                .customProperty("key1", true)
                .customProperty("key2", 17)
                .customProperty("key3", "some value")
                .build())
            .join().nextExpectedVersion);

        StreamEventsSlice slice = eventstore.readStreamEventsForward(stream, 0, 100, false).join();
        assertEquals(SliceReadStatus.StreamNotFound, slice.status);
        assertEquals(-1, slice.lastEventNumber);
        assertTrue(slice.events.isEmpty());

        StreamMetadataResult streamMetadataResult = eventstore.getStreamMetadata(stream).join();
        assertEquals(2, streamMetadataResult.metastreamVersion);
        assertEquals(0, streamMetadataResult.streamMetadata.truncateBefore.intValue());
        assertEquals(100, streamMetadataResult.streamMetadata.maxCount.intValue());
        assertEquals("some-role", streamMetadataResult.streamMetadata.acl.deleteRoles.get(0));
        assertTrue(streamMetadataResult.streamMetadata.getCustomProperty("key1").toBoolean());
        assertEquals(Integer.valueOf(17), streamMetadataResult.streamMetadata.getCustomProperty("key2").toInteger());
        assertEquals("some value", streamMetadataResult.streamMetadata.getCustomProperty("key3").toString());
    }

    @Test
    public void settingJsonMetadataOnNonEmptySoftDeletedStreamRecreatesStreamNotOverridingMetadata() {
        final String stream = generateStreamName();

        assertEquals(1, eventstore.appendToStream(stream, ExpectedVersion.noStream(), newTestEvents()).join().nextExpectedVersion);

        eventstore.deleteStream(stream, ExpectedVersion.of(1)).join();

        assertEquals(1, eventstore.setStreamMetadata(stream, ExpectedVersion.of(0),
            StreamMetadata.newBuilder()
                .maxCount(100)
                .aclDeleteRoles(asList("some-role"))
                .customProperty("key1", true)
                .customProperty("key2", 17)
                .customProperty("key3", "some value")
                .build())
            .join().nextExpectedVersion);

        StreamEventsSlice slice = eventstore.readStreamEventsForward(stream, 0, 100, false).join();
        assertEquals(SliceReadStatus.Success, slice.status);
        assertEquals(1, slice.lastEventNumber);
        assertTrue(slice.events.isEmpty());

        StreamMetadataResult streamMetadataResult = eventstore.getStreamMetadata(stream).join();
        assertEquals(2, streamMetadataResult.metastreamVersion);
        assertEquals(2, streamMetadataResult.streamMetadata.truncateBefore.intValue());
        assertEquals(100, streamMetadataResult.streamMetadata.maxCount.intValue());
        assertEquals("some-role", streamMetadataResult.streamMetadata.acl.deleteRoles.get(0));
        assertTrue(streamMetadataResult.streamMetadata.getCustomProperty("key1").toBoolean());
        assertEquals(Integer.valueOf(17), streamMetadataResult.streamMetadata.getCustomProperty("key2").toInteger());
        assertEquals("some value", streamMetadataResult.streamMetadata.getCustomProperty("key3").toString());
    }

    @Test
    public void settingNonJsonMetadataOnEmptySoftDeletedStreamRecreatesStreamKeepingOriginalMetadata() {
        final String stream = generateStreamName();

        eventstore.deleteStream(stream, ExpectedVersion.noStream()).join();

        assertEquals(1, eventstore.setStreamMetadata(stream, ExpectedVersion.of(0), new byte[256]).join().nextExpectedVersion);

        StreamEventsSlice slice = eventstore.readStreamEventsForward(stream, 0, 100, false).join();
        assertEquals(SliceReadStatus.StreamNotFound, slice.status);
        assertEquals(-1, slice.lastEventNumber);
        assertTrue(slice.events.isEmpty());

        RawStreamMetadataResult streamMetadataResult = eventstore.getStreamMetadataAsRawBytes(stream).join();
        assertEquals(1, streamMetadataResult.metastreamVersion);
        assertArrayEquals(new byte[256], streamMetadataResult.streamMetadata);
    }

    @Test
    public void settingNonJsonMetadataOnNonEmptySoftDeletedStreamRecreatesStreamKeepingOriginalMetadata() {
        final String stream = generateStreamName();

        assertEquals(1, eventstore.appendToStream(stream, ExpectedVersion.noStream(), newTestEvents()).join().nextExpectedVersion);

        eventstore.deleteStream(stream, ExpectedVersion.of(1)).join();

        assertEquals(1, eventstore.setStreamMetadata(stream, ExpectedVersion.of(0), new byte[256]).join().nextExpectedVersion);

        StreamEventsSlice slice = eventstore.readStreamEventsForward(stream, 0, 100, false).join();
        assertEquals(SliceReadStatus.Success, slice.status);
        assertEquals(1, slice.lastEventNumber);
        assertEquals(2, slice.events.size());
        assertArrayEquals(new Integer[]{0, 1},
            slice.events.stream().map(e -> e.originalEvent().eventNumber).toArray(Integer[]::new));

        RawStreamMetadataResult streamMetadataResult = eventstore.getStreamMetadataAsRawBytes(stream).join();
        assertEquals(1, streamMetadataResult.metastreamVersion);
        assertArrayEquals(new byte[256], streamMetadataResult.streamMetadata);
    }

    private static List<EventData> newTestEvents() {
        return asList(newTestEvent(), newTestEvent());
    }

}
