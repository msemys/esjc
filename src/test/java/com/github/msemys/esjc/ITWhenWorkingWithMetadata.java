package com.github.msemys.esjc;

import com.github.msemys.esjc.util.EmptyArrays;
import org.junit.Test;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ITWhenWorkingWithMetadata extends AbstractIntegrationTest {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSupplier.get();
    }

    @Test
    public void getsMetadataForAnExistingStreamAndNoMetadataExists() {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.noStream(), asList(newTestEvent())).join();

        RawStreamMetadataResult metadata = eventstore.getStreamMetadataAsRawBytes(stream).join();
        assertEquals(stream, metadata.stream);
        assertFalse(metadata.isStreamDeleted);
        assertEquals(-1, metadata.metastreamVersion);
        assertArrayEquals(EmptyArrays.EMPTY_BYTES, metadata.streamMetadata);
    }

}
