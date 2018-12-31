package com.github.msemys.esjc;

import com.github.msemys.esjc.runner.EventStoreRunnerWithParametersFactory;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.Parameterized.UseParametersRunnerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;

@RunWith(Parameterized.class)
@UseParametersRunnerFactory(EventStoreRunnerWithParametersFactory.class)
public abstract class AbstractEventStoreTest {

    @Parameters
    public static Object[] parameters() {
        return new Object[]{
            EventStoreBuilder.newBuilder()
                .singleNodeAddress("127.0.0.1", 7773)
                .userCredentials("admin", "changeit")
                .maxReconnections(2)
                .build(),

            EventStoreBuilder.newBuilder()
                .singleNodeAddress("127.0.0.1", 7779)
                .useSslConnection()
                .userCredentials("admin", "changeit")
                .maxReconnections(2)
                .build()
        };
    }

    protected final EventStore eventstore;

    @Rule
    public TestName name = new TestName();

    protected AbstractEventStoreTest(EventStore eventstore) {
        this.eventstore = eventstore;
    }

    protected static EventData newLinkEvent(String stream, long eventNumber) {
        return EventData.newBuilder()
            .linkTo(eventNumber, stream)
            .build();
    }

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

}
