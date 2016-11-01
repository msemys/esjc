package com.github.msemys.esjc;

import com.github.msemys.esjc.system.SystemEventType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ITReadAllEventsForwardWithLinkToPassedMaxCount extends AbstractIntegrationTest {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSupplier.get();
    }

    @Test
    public void readsOneEvent() {
        final String deletedStreamName = generateStreamName();
        final String linkedStreamName = generateStreamName();

        eventstore.appendToStream(deletedStreamName, ExpectedVersion.any(),
            EventData.newBuilder()
                .type("testing1")
                .jsonData("{'foo' : 4}")
                .build()
        ).join();

        eventstore.setStreamMetadata(deletedStreamName, ExpectedVersion.any(),
            StreamMetadata.newBuilder()
                .maxCount(2)
                .build()
        ).join();

        eventstore.appendToStream(deletedStreamName, ExpectedVersion.any(),
            EventData.newBuilder()
                .type("testing2")
                .jsonData("{'foo' : 4}")
                .build()
        ).join();

        eventstore.appendToStream(deletedStreamName, ExpectedVersion.any(),
            EventData.newBuilder()
                .type("testing3")
                .jsonData("{'foo' : 4}")
                .build()
        ).join();

        eventstore.appendToStream(linkedStreamName, ExpectedVersion.any(),
            EventData.newBuilder()
                .type(SystemEventType.LINK_TO.value)
                .data("0@" + deletedStreamName)
                .build()
        ).join();

        StreamEventsSlice slice = eventstore.readStreamEventsForward(linkedStreamName, 0, 1, true).join();

        assertEquals(1, slice.events.size());
    }

}
