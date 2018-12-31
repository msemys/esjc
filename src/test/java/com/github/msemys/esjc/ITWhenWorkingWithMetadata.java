package com.github.msemys.esjc;

import com.github.msemys.esjc.util.EmptyArrays;
import org.junit.Test;

import static org.junit.Assert.*;

public class ITWhenWorkingWithMetadata extends AbstractEventStoreTest {

    public ITWhenWorkingWithMetadata(EventStore eventstore) {
        super(eventstore);
    }

    @Test
    public void getsMetadataForAnExistingStreamAndNoMetadataExists() {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, newTestEvent()).join();

        RawStreamMetadataResult metadata = eventstore.getStreamMetadataAsRawBytes(stream).join();
        assertEquals(stream, metadata.stream);
        assertFalse(metadata.isStreamDeleted);
        assertEquals(-1, metadata.metastreamVersion);
        assertArrayEquals(EmptyArrays.EMPTY_BYTES, metadata.streamMetadata);
    }

}
