package com.github.msemys.esjc;

import org.junit.After;
import org.junit.Before;

import java.util.UUID;
import java.util.function.Supplier;

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

    protected String generateStreamName() {
        return new StringBuilder()
            .append(getClass().getSimpleName()).append("-")
            .append(Thread.currentThread().getStackTrace()[2].getMethodName()).append("-")
            .append(UUID.randomUUID().toString())
            .toString();
    }

}
