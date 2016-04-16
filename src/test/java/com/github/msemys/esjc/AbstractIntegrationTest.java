package com.github.msemys.esjc;

import com.github.msemys.esjc.util.Throwables;
import org.junit.After;
import org.junit.Before;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public abstract class AbstractIntegrationTest {

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

    protected static <T> List<T> reverse(List<T> list) {
        List<T> reversedList = new ArrayList<>(list);
        Collections.reverse(reversedList);
        return reversedList;
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

}
