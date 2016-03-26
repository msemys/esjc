package com.github.msemys.esjc;

import com.github.msemys.esjc.util.Throwables;
import org.junit.After;
import org.junit.Before;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

public abstract class EventStoreIntegrationTest {

    protected static final Supplier<EventStore> eventstoreSupplier = () -> EventStoreBuilder.newBuilder()
        .singleNodeAddress("127.0.0.1", 7773)
        .userCredentials("admin", "changeit")
        .maxClientReconnections(2)
        .build();

    protected static final Supplier<EventStore> eventstoreSslSupplier = () -> EventStoreBuilder.newBuilder()
        .singleNodeAddress("127.0.0.1", 7779)
        .useSslConnectionWithAnyCertificate()
        .userCredentials("admin", "changeit")
        .maxClientReconnections(2)
        .build();

    protected EventStore eventstore;

    @Before
    public void setUp() throws Exception {
        eventstore = createEventStore();
        eventstore.connect();
    }

    @After
    public void tearDown() throws Exception {
        eventstore.disconnect();
    }

    protected abstract EventStore createEventStore();

    protected static EventData newTestEvent() {
        return EventData.newBuilder()
            .type("test")
            .build();
    }

    /**
     * Generates unique stream name based on test class and current method name.
     *
     * @return unique stream name
     */
    protected String generateStreamName() {
        return new StringBuilder()
            .append(getClass().getSimpleName()).append("-")
            .append(Thread.currentThread().getStackTrace()[2].getMethodName()).append("-")
            .append(UUID.randomUUID().toString())
            .toString();
    }

    /**
     * Returns the number of events in the specified stream.
     *
     * @param stream stream name
     * @return the number of events in the specified stream
     */
    protected int size(String stream) {
        int result = 0;

        while (true) {
            try {
                StreamEventsSlice slice = eventstore.readStreamEventsForward(stream, result, 10, false).get();
                result += slice.events.size();

                if (slice.isEndOfStream) {
                    break;
                }
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }
        }

        return result;
    }

    /**
     * Creates sequential stream writer.
     *
     * @param stream  stream name
     * @param version expected version
     * @return sequential stream writer
     */
    protected StreamWriter newStreamWriter(String stream, ExpectedVersion version) {
        return new StreamWriter(eventstore, stream, version);
    }

    /**
     * Sequential stream writer
     */
    protected static class StreamWriter {
        private final EventStore eventstore;
        private final String stream;
        private final ExpectedVersion version;

        private StreamWriter(EventStore eventstore, String stream, ExpectedVersion version) {
            this.eventstore = eventstore;
            this.stream = stream;
            this.version = version;
        }

        protected TailWriter append(List<EventData> events) {
            for (int i = 0; i < events.size(); i++) {
                ExpectedVersion expectedVersion = (ExpectedVersion.any().equals(version)) ?
                    version : ExpectedVersion.of(version.value + i);

                try {
                    ExpectedVersion nextExpectedVersion = ExpectedVersion.of(eventstore
                        .appendToStream(stream, expectedVersion, asList(events.get(i)))
                        .get().nextExpectedVersion);

                    if (!ExpectedVersion.any().equals(nextExpectedVersion)) {
                        assertEquals(expectedVersion.value + 1, nextExpectedVersion.value);
                    }
                } catch (Exception e) {
                    throw Throwables.propagate(e);
                }
            }

            return new TailWriter(eventstore, stream);
        }
    }

    /**
     * Sequential stream tail writer.
     */
    protected static class TailWriter {
        private final EventStore eventstore;
        private final String stream;

        private TailWriter(EventStore eventstore, String stream) {
            this.eventstore = eventstore;
            this.stream = stream;
        }

        protected TailWriter append(EventData event, ExpectedVersion version) {
            try {
                eventstore.appendToStream(stream, version, asList(event)).get();
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }
            return this;
        }
    }
}
