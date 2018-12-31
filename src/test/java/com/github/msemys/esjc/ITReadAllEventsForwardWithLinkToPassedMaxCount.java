package com.github.msemys.esjc;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ITReadAllEventsForwardWithLinkToPassedMaxCount extends AbstractEventStoreTest {

    public ITReadAllEventsForwardWithLinkToPassedMaxCount(EventStore eventstore) {
        super(eventstore);
    }

    @Test
    public void readsOneEvent() {
        final String deletedStreamName = generateStreamName();
        final String linkedStreamName = generateStreamName();

        eventstore.appendToStream(deletedStreamName, ExpectedVersion.ANY,
            EventData.newBuilder()
                .type("testing1")
                .jsonData("{'foo' : 4}")
                .build()
        ).join();

        eventstore.setStreamMetadata(deletedStreamName, ExpectedVersion.ANY,
            StreamMetadata.newBuilder()
                .maxCount(2L)
                .build()
        ).join();

        eventstore.appendToStream(deletedStreamName, ExpectedVersion.ANY,
            EventData.newBuilder()
                .type("testing2")
                .jsonData("{'foo' : 4}")
                .build()
        ).join();

        eventstore.appendToStream(deletedStreamName, ExpectedVersion.ANY,
            EventData.newBuilder()
                .type("testing3")
                .jsonData("{'foo' : 4}")
                .build()
        ).join();

        eventstore.appendToStream(linkedStreamName, ExpectedVersion.ANY,
            EventData.newBuilder()
                .linkTo(0, deletedStreamName)
                .build()
        ).join();

        StreamEventsSlice slice = eventstore.readStreamEventsForward(linkedStreamName, 0, 1, true).join();

        assertEquals(1, slice.events.size());
    }

}
