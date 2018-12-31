package com.github.msemys.esjc;

import com.github.msemys.esjc.operation.StreamDeletedException;
import com.github.msemys.esjc.operation.WrongExpectedVersionException;
import com.github.msemys.esjc.util.EmptyArrays;
import com.github.msemys.esjc.util.UUIDConverter;
import org.junit.Test;

import java.util.UUID;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;

public class ITWhenWorkingWithStreamMetadataAsByteArray extends AbstractEventStoreTest {

    public ITWhenWorkingWithStreamMetadataAsByteArray(EventStore eventstore) {
        super(eventstore);
    }

    @Test
    public void setsEmptyMetadata() {
        final String stream = generateStreamName();

        eventstore.setStreamMetadata(stream, ExpectedVersion.NO_STREAM, (byte[]) null).join();

        RawStreamMetadataResult metadata = eventstore.getStreamMetadataAsRawBytes(stream).join();
        assertEquals(stream, metadata.stream);
        assertFalse(metadata.isStreamDeleted);
        assertEquals(0, metadata.metastreamVersion);
        assertArrayEquals(EmptyArrays.EMPTY_BYTES, metadata.streamMetadata);
    }

    @Test
    public void settingMetadataFewTimesReturnsLastMetadata() {
        final String stream = generateStreamName();

        byte[] metadataBytes1 = UUIDConverter.toBytes(UUID.randomUUID());
        eventstore.setStreamMetadata(stream, ExpectedVersion.NO_STREAM, metadataBytes1).join();
        RawStreamMetadataResult metadata1 = eventstore.getStreamMetadataAsRawBytes(stream).join();
        assertEquals(stream, metadata1.stream);
        assertFalse(metadata1.isStreamDeleted);
        assertEquals(0, metadata1.metastreamVersion);
        assertArrayEquals(metadataBytes1, metadata1.streamMetadata);

        byte[] metadataBytes2 = UUIDConverter.toBytes(UUID.randomUUID());
        eventstore.setStreamMetadata(stream, 0, metadataBytes2).join();
        RawStreamMetadataResult metadata2 = eventstore.getStreamMetadataAsRawBytes(stream).join();
        assertEquals(stream, metadata2.stream);
        assertFalse(metadata2.isStreamDeleted);
        assertEquals(1, metadata2.metastreamVersion);
        assertArrayEquals(metadataBytes2, metadata2.streamMetadata);
    }

    @Test
    public void failsToSetMetadataWithWrongExpectedVersion() {
        final String stream = generateStreamName();

        try {
            eventstore.setStreamMetadata(stream, 5, new byte[100]).join();
            fail("should fail with 'WrongExpectedVersionException'");
        } catch (Exception e) {
            assertThat(e.getCause(), instanceOf(WrongExpectedVersionException.class));
        }
    }

    @Test
    public void setsMetadataWithExpectedVersionAny() {
        final String stream = generateStreamName();

        byte[] metadataBytes1 = UUIDConverter.toBytes(UUID.randomUUID());
        eventstore.setStreamMetadata(stream, ExpectedVersion.ANY, metadataBytes1).join();
        RawStreamMetadataResult metadata1 = eventstore.getStreamMetadataAsRawBytes(stream).join();
        assertEquals(stream, metadata1.stream);
        assertFalse(metadata1.isStreamDeleted);
        assertEquals(0, metadata1.metastreamVersion);
        assertArrayEquals(metadataBytes1, metadata1.streamMetadata);

        byte[] metadataBytes2 = UUIDConverter.toBytes(UUID.randomUUID());
        eventstore.setStreamMetadata(stream, ExpectedVersion.ANY, metadataBytes2).join();
        RawStreamMetadataResult metadata2 = eventstore.getStreamMetadataAsRawBytes(stream).join();
        assertEquals(stream, metadata2.stream);
        assertFalse(metadata2.isStreamDeleted);
        assertEquals(1, metadata2.metastreamVersion);
        assertArrayEquals(metadataBytes2, metadata2.streamMetadata);
    }

    @Test
    public void setsMetadataForNonExistingStreamWorks() {
        final String stream = generateStreamName();

        byte[] metadataBytes = UUIDConverter.toBytes(UUID.randomUUID());
        eventstore.setStreamMetadata(stream, ExpectedVersion.NO_STREAM, metadataBytes).join();

        RawStreamMetadataResult metadata = eventstore.getStreamMetadataAsRawBytes(stream).join();
        assertEquals(stream, metadata.stream);
        assertFalse(metadata.isStreamDeleted);
        assertEquals(0, metadata.metastreamVersion);
        assertArrayEquals(metadataBytes, metadata.streamMetadata);
    }

    @Test
    public void setsMetadataForExistingStream() {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, asList(newTestEvent(), newTestEvent())).join();

        byte[] metadataBytes = UUIDConverter.toBytes(UUID.randomUUID());
        eventstore.setStreamMetadata(stream, ExpectedVersion.NO_STREAM, metadataBytes).join();

        RawStreamMetadataResult metadata = eventstore.getStreamMetadataAsRawBytes(stream).join();
        assertEquals(stream, metadata.stream);
        assertFalse(metadata.isStreamDeleted);
        assertEquals(0, metadata.metastreamVersion);
        assertArrayEquals(metadataBytes, metadata.streamMetadata);
    }

    @Test
    public void failsToSetMetadataForDeletedStream() {
        final String stream = generateStreamName();

        eventstore.deleteStream(stream, ExpectedVersion.NO_STREAM, true).join();

        byte[] metadataBytes = UUIDConverter.toBytes(UUID.randomUUID());
        try {
            eventstore.setStreamMetadata(stream, ExpectedVersion.NO_STREAM, metadataBytes).join();
            fail("should fail with 'StreamDeletedException'");
        } catch (Exception e) {
            assertThat(e.getCause(), instanceOf(StreamDeletedException.class));
        }
    }

    @Test
    public void gettingMetadataForNonExistingStreamReturnsEmptyByteArray() {
        final String stream = generateStreamName();

        RawStreamMetadataResult metadata = eventstore.getStreamMetadataAsRawBytes(stream).join();
        assertEquals(stream, metadata.stream);
        assertFalse(metadata.isStreamDeleted);
        assertEquals(-1, metadata.metastreamVersion);
        assertArrayEquals(EmptyArrays.EMPTY_BYTES, metadata.streamMetadata);
    }

    @Test
    public void gettingMetadataForDeletedStreamReturnsEmptyByteArrayAndSignalsStreamDeletion() {
        final String stream = generateStreamName();

        byte[] metadataBytes = UUIDConverter.toBytes(UUID.randomUUID());
        eventstore.setStreamMetadata(stream, ExpectedVersion.NO_STREAM, metadataBytes).join();

        eventstore.deleteStream(stream, ExpectedVersion.NO_STREAM, true).join();

        RawStreamMetadataResult metadata = eventstore.getStreamMetadataAsRawBytes(stream).join();
        assertEquals(stream, metadata.stream);
        assertTrue(metadata.isStreamDeleted);
        assertEquals(Long.MAX_VALUE, metadata.metastreamVersion);
        assertArrayEquals(EmptyArrays.EMPTY_BYTES, metadata.streamMetadata);
    }

}
