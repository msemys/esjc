package com.github.msemys.esjc;

import com.github.msemys.esjc.event.ClientConnected;
import com.github.msemys.esjc.event.ClientDisconnected;
import com.github.msemys.esjc.util.Throwables;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;

import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static org.junit.Assert.assertTrue;

public abstract class AbstractIntegrationTest {

    protected static final Supplier<EventStore> eventstoreSupplier = () -> EventStoreBuilder.newBuilder()
        .singleNodeAddress("127.0.0.1", 7773)
        .userCredentials("admin", "changeit")
        .maxReconnections(2)
        .build();

    protected static final Supplier<EventStore> eventstoreSslSupplier = () -> EventStoreBuilder.newBuilder()
        .singleNodeAddress("127.0.0.1", 7779)
        .useSslConnection()
        .userCredentials("admin", "changeit")
        .maxReconnections(2)
        .build();

    protected EventStore eventstore;

    @Rule
    public TestName name = new TestName();

    @Before
    public void setUp() throws Exception {
        eventstore = createEventStore();

        CountDownLatch clientConnectedSignal = new CountDownLatch(1);

        eventstore.addListener(event -> {
            if (event instanceof ClientConnected) {
                clientConnectedSignal.countDown();
            }
        });

        eventstore.connect();

        assertTrue("client connect timeout", clientConnectedSignal.await(15, SECONDS));
    }

    @After
    public void tearDown() throws Exception {
        awaitDisconnected(eventstore);
    }

    protected abstract EventStore createEventStore();

    protected static EventData newTestEvent() {
        return EventData.newBuilder()
            .type("test")
            .build();
    }

    protected static List<EventData> newTestEvents(int count) {
        return range(0, count).mapToObj(i -> newTestEvent()).collect(toList());
    }

    protected static <T> List<T> reverse(List<T> list) {
        List<T> reversedList = new ArrayList<>(list);
        Collections.reverse(reversedList);
        return reversedList;
    }

    protected static List<RecordedEvent> recordedEventsFrom(List<ResolvedEvent> events) {
        return events.stream().map(e -> e.event).collect(toList());
    }

    protected static List<UUID> eventIdsFrom(List<ResolvedEvent> events) {
        return events.stream().map(e -> e.event.eventId).collect(toList());
    }

    /**
     * Generates unique stream name based on test class and current method name.
     *
     * @return unique stream name
     */
    protected String generateStreamName() {
        return new StringBuilder()
            .append(getClass().getSimpleName()).append("-")
            .append(name.getMethodName()).append("-")
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
            StreamEventsSlice slice = eventstore.readStreamEventsForward(stream, result, 10, false).join();
            result += slice.events.size();

            if (slice.isEndOfStream) {
                break;
            }
        }

        return result;
    }

    protected static void awaitDisconnected(EventStore eventstore) {
        CountDownLatch clientDisconnectedSignal = new CountDownLatch(1);

        eventstore.addListener(event -> {
            if (event instanceof ClientDisconnected) {
                clientDisconnectedSignal.countDown();
            }
        });

        eventstore.disconnect();

        try {
            assertTrue("client disconnect timeout", clientDisconnectedSignal.await(15, SECONDS));
        } catch (InterruptedException e) {
            throw Throwables.propagate(e);
        }
    }

}
