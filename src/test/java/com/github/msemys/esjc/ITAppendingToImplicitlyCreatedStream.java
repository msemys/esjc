package com.github.msemys.esjc;

import com.github.msemys.esjc.operation.WrongExpectedVersionException;
import com.github.msemys.esjc.util.Throwables;
import org.junit.Test;

import java.util.List;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static java.util.stream.Stream.concat;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;

public class ITAppendingToImplicitlyCreatedStream extends AbstractIntegrationTest {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSupplier.get();
    }

    // method naming:
    //   0em1 - event number 0 written with expected version -1 (minus 1)
    //   1any - event number 1 written with expected version any
    //   S_0em1_1em1_E - START bucket, two events in bucket, END bucket

    @Test
    public void appends_0em1_1e0_2e1_3e2_4e3_5e4_0em1_idempotent() {
        final String stream = generateStreamName();

        List<EventData> events = range(0, 6).mapToObj(i -> newTestEvent()).collect(toList());

        newStreamWriter(stream, ExpectedVersion.of(-1))
            .append(events)
            .append(events.get(0), ExpectedVersion.of(-1));

        assertEquals(events.size(), size(stream));
    }

    @Test
    public void appends_0em1_1e0_2e1_3e2_4e3_4e4_0any_idempotent() {
        final String stream = generateStreamName();

        List<EventData> events = range(0, 6).mapToObj(i -> newTestEvent()).collect(toList());

        newStreamWriter(stream, ExpectedVersion.of(-1))
            .append(events)
            .append(events.get(0), ExpectedVersion.any());

        assertEquals(events.size(), size(stream));
    }

    @Test
    public void appends_0em1_1e0_2e1_3e2_4e3_5e4_0e5_non_idempotent() {
        final String stream = generateStreamName();

        List<EventData> events = range(0, 6).mapToObj(i -> newTestEvent()).collect(toList());

        newStreamWriter(stream, ExpectedVersion.of(-1))
            .append(events)
            .append(events.get(0), ExpectedVersion.of(5));

        assertEquals(events.size() + 1, size(stream));
    }

    @Test
    public void appends_0em1_1e0_2e1_3e2_4e3_5e4_0e6_wev() {
        final String stream = generateStreamName();

        List<EventData> events = range(0, 6).mapToObj(i -> newTestEvent()).collect(toList());

        TailWriter writer = newStreamWriter(stream, ExpectedVersion.of(-1)).append(events);

        try {
            writer.append(events.get(0), ExpectedVersion.of(6));
            fail("append should fail with 'WrongExpectedVersionException'");
        } catch (Exception e) {
            assertThat(e.getCause().getCause(), instanceOf(WrongExpectedVersionException.class));
        }
    }

    @Test
    public void append_0em1_1e0_2e1_3e2_4e3_5e4_0e4_wev() {
        final String stream = generateStreamName();

        List<EventData> events = range(0, 6).mapToObj(i -> newTestEvent()).collect(toList());

        TailWriter writer = newStreamWriter(stream, ExpectedVersion.of(-1)).append(events);

        try {
            writer.append(events.get(0), ExpectedVersion.of(4));
            fail("append should fail with 'WrongExpectedVersionException'");
        } catch (Exception e) {
            assertThat(e.getCause().getCause(), instanceOf(WrongExpectedVersionException.class));
        }
    }

    @Test
    public void append_0em1_0e0_non_idempotent() {
        final String stream = generateStreamName();

        List<EventData> events = range(0, 1).mapToObj(i -> newTestEvent()).collect(toList());

        newStreamWriter(stream, ExpectedVersion.of(-1))
            .append(events)
            .append(events.get(0), ExpectedVersion.of(0));

        assertEquals(events.size() + 1, size(stream));
    }

    @Test
    public void appends_0em1_0any_idempotent() {
        final String stream = generateStreamName();

        List<EventData> events = range(0, 1).mapToObj(i -> newTestEvent()).collect(toList());

        newStreamWriter(stream, ExpectedVersion.of(-1))
            .append(events)
            .append(events.get(0), ExpectedVersion.any());

        assertEquals(events.size(), size(stream));
    }

    @Test
    public void appends_0em1_0em1_idempotent() {
        final String stream = generateStreamName();

        List<EventData> events = range(0, 1).mapToObj(i -> newTestEvent()).collect(toList());

        newStreamWriter(stream, ExpectedVersion.of(-1))
            .append(events)
            .append(events.get(0), ExpectedVersion.of(-1));

        assertEquals(events.size(), size(stream));
    }

    @Test
    public void appends_0em1_1e0_2e1_1any_1any_idempotent() {
        final String stream = generateStreamName();

        List<EventData> events = range(0, 3).mapToObj(i -> newTestEvent()).collect(toList());

        newStreamWriter(stream, ExpectedVersion.of(-1))
            .append(events)
            .append(events.get(1), ExpectedVersion.any())
            .append(events.get(1), ExpectedVersion.any());

        assertEquals(events.size(), size(stream));
    }

    @Test
    public void appends_S_0em1_1em1_E_S_0em1_E_idempotent() {
        final String stream = generateStreamName();

        List<EventData> events = range(0, 2).mapToObj(i -> newTestEvent()).collect(toList());

        try {
            eventstore.appendToStream(stream, ExpectedVersion.of(-1), events).get();
            eventstore.appendToStream(stream, ExpectedVersion.of(-1), asList(events.get(0))).get();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }

        assertEquals(events.size(), size(stream));
    }

    @Test
    public void appends_S_0em1_1em1_E_S_0any_E_idempotent() {
        final String stream = generateStreamName();

        List<EventData> events = range(0, 2).mapToObj(i -> newTestEvent()).collect(toList());

        try {
            eventstore.appendToStream(stream, ExpectedVersion.of(-1), events).get();
            eventstore.appendToStream(stream, ExpectedVersion.any(), asList(events.get(0))).get();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }

        assertEquals(events.size(), size(stream));
    }

    @Test
    public void appends_S_0em1_1em1_E_S_1e0_E_idempotent() {
        final String stream = generateStreamName();

        List<EventData> events = range(0, 2).mapToObj(i -> newTestEvent()).collect(toList());

        try {
            eventstore.appendToStream(stream, ExpectedVersion.of(-1), events).get();
            eventstore.appendToStream(stream, ExpectedVersion.of(0), asList(events.get(1))).get();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }

        assertEquals(events.size(), size(stream));
    }

    @Test
    public void appends_S_0em1_1em1_E_S_1any_E_idempotent() {
        final String stream = generateStreamName();

        List<EventData> events = range(0, 2).mapToObj(i -> newTestEvent()).collect(toList());

        try {
            eventstore.appendToStream(stream, ExpectedVersion.of(-1), events).get();
            eventstore.appendToStream(stream, ExpectedVersion.any(), asList(events.get(1))).get();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }

        assertEquals(events.size(), size(stream));
    }

    @Test
    public void appends_S_0em1_1em1_E_S_0em1_1em1_2em1_E_idempotancy_fail() {
        final String stream = generateStreamName();

        List<EventData> events = range(0, 2).mapToObj(i -> newTestEvent()).collect(toList());

        try {
            eventstore.appendToStream(stream, ExpectedVersion.of(-1), events).get();
            eventstore.appendToStream(stream, ExpectedVersion.any(),
                concat(events.stream(), Stream.of(newTestEvent())).collect(toList())).get();
            fail("append should fail with 'WrongExpectedVersionException'");
        } catch (Exception e) {
            assertThat(e.getCause(), instanceOf(WrongExpectedVersionException.class));
        }
    }

    /**
     * Creates sequential stream writer.
     *
     * @param stream  stream name
     * @param version expected version
     * @return sequential stream writer
     */
    private StreamWriter newStreamWriter(String stream, ExpectedVersion version) {
        return new StreamWriter(eventstore, stream, version);
    }

    /**
     * Sequential stream writer
     */
    private static class StreamWriter {
        private final EventStore eventstore;
        private final String stream;
        private final ExpectedVersion version;

        private StreamWriter(EventStore eventstore, String stream, ExpectedVersion version) {
            this.eventstore = eventstore;
            this.stream = stream;
            this.version = version;
        }

        private TailWriter append(List<EventData> events) {
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
    private static class TailWriter {
        private final EventStore eventstore;
        private final String stream;

        private TailWriter(EventStore eventstore, String stream) {
            this.eventstore = eventstore;
            this.stream = stream;
        }

        private TailWriter append(EventData event, ExpectedVersion version) {
            try {
                eventstore.appendToStream(stream, version, asList(event)).get();
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }
            return this;
        }
    }
}
