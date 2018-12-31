package com.github.msemys.esjc;

import org.junit.Test;

import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class ITJsonFlagOnEventData extends AbstractEventStoreTest {

    public ITJsonFlagOnEventData(EventStore eventstore) {
        super(eventstore);
    }

    @Test
    public void setsJsonFlag() {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, asList(
            EventData.newBuilder()
                .type("some-type")
                .jsonData("{\"some\":\"json\"}")
                .jsonMetadata((String) null)
                .build(),
            EventData.newBuilder()
                .type("some-type")
                .jsonData((String) null)
                .jsonMetadata("{\"some\":\"json\"}")
                .build(),
            EventData.newBuilder()
                .type("some-type")
                .jsonData("{\"some\":\"json\"}")
                .jsonMetadata("{\"some\":\"json\"}")
                .build()
        )).join();

        try (Transaction transaction = eventstore.startTransaction(stream, ExpectedVersion.ANY).join()) {
            transaction.write(asList(
                EventData.newBuilder()
                    .type("some-type")
                    .jsonData("{\"some\":\"json\"}")
                    .jsonMetadata((String) null)
                    .build(),
                EventData.newBuilder()
                    .type("some-type")
                    .jsonData((String) null)
                    .jsonMetadata("{\"some\":\"json\"}")
                    .build(),
                EventData.newBuilder()
                    .type("some-type")
                    .jsonData("{\"some\":\"json\"}")
                    .jsonMetadata("{\"some\":\"json\"}")
                    .build()
            )).join();
            transaction.commit().join();
        } catch (Exception e) {
            fail(e.getMessage());
        }

        StreamEventsSlice result = eventstore.readStreamEventsForward(stream, 0, 100, false).join();

        assertThat(result.events.size(), is(6));
        result.events.forEach(e -> assertTrue("Event #" + e.originalEvent().eventNumber, e.originalEvent().isJson));
    }

}
