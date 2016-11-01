package com.github.msemys.esjc;

import com.github.msemys.esjc.operation.WrongExpectedVersionException;
import org.junit.Test;

import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static java.util.stream.Stream.concat;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;

public class ITAppendingToImplicitlyCreatedStreamUsingTransaction extends AbstractIntegrationTest {

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

        TransactionalWriter writer = newTransactionalWriter(stream);

        assertEquals(5, writer.startTransaction(ExpectedVersion.of(-1)).write(events).commit().nextExpectedVersion);
        assertEquals(0, writer.startTransaction(ExpectedVersion.of(-1)).write(events.get(0)).commit().nextExpectedVersion);

        assertEquals(events.size(), size(stream));
    }

    @Test
    public void appends_0em1_1e0_2e1_3e2_4e3_5e4_0any_idempotent() {
        final String stream = generateStreamName();

        List<EventData> events = range(0, 6).mapToObj(i -> newTestEvent()).collect(toList());

        TransactionalWriter writer = newTransactionalWriter(stream);

        assertEquals(5, writer.startTransaction(ExpectedVersion.of(-1)).write(events).commit().nextExpectedVersion);
        assertEquals(0, writer.startTransaction(ExpectedVersion.any()).write(events.get(0)).commit().nextExpectedVersion);

        assertEquals(events.size(), size(stream));
    }

    @Test
    public void appends_0em1_1e0_2e1_3e2_4e3_5e4_0e5_non_idempotent() {
        final String stream = generateStreamName();

        List<EventData> events = range(0, 6).mapToObj(i -> newTestEvent()).collect(toList());

        TransactionalWriter writer = newTransactionalWriter(stream);

        assertEquals(5, writer.startTransaction(ExpectedVersion.of(-1)).write(events).commit().nextExpectedVersion);
        assertEquals(6, writer.startTransaction(ExpectedVersion.of(5)).write(events.get(0)).commit().nextExpectedVersion);

        assertEquals(events.size() + 1, size(stream));
    }

    @Test
    public void appends_0em1_1e0_2e1_3e2_4e3_5e4_0e6_wev() {
        final String stream = generateStreamName();

        List<EventData> events = range(0, 6).mapToObj(i -> newTestEvent()).collect(toList());

        TransactionalWriter writer = newTransactionalWriter(stream);

        assertEquals(5, writer.startTransaction(ExpectedVersion.of(-1)).write(events).commit().nextExpectedVersion);

        try {
            writer.startTransaction(ExpectedVersion.of(6)).write(events.get(0)).commit();
            fail("append should fail with 'WrongExpectedVersionException'");
        } catch (Exception e) {
            assertThat(e.getCause(), instanceOf(WrongExpectedVersionException.class));
        }
    }

    @Test
    public void appends_0em1_1e0_2e1_3e2_4e3_5e4_0e4_wev() {
        final String stream = generateStreamName();

        List<EventData> events = range(0, 6).mapToObj(i -> newTestEvent()).collect(toList());

        TransactionalWriter writer = newTransactionalWriter(stream);

        assertEquals(5, writer.startTransaction(ExpectedVersion.of(-1)).write(events).commit().nextExpectedVersion);

        try {
            writer.startTransaction(ExpectedVersion.of(4)).write(events.get(0)).commit();
            fail("append should fail with 'WrongExpectedVersionException'");
        } catch (Exception e) {
            assertThat(e.getCause(), instanceOf(WrongExpectedVersionException.class));
        }
    }

    @Test
    public void appends_0em1_0e0_non_idempotent() {
        final String stream = generateStreamName();

        List<EventData> events = range(0, 1).mapToObj(i -> newTestEvent()).collect(toList());

        TransactionalWriter writer = newTransactionalWriter(stream);

        assertEquals(0, writer.startTransaction(ExpectedVersion.of(-1)).write(events).commit().nextExpectedVersion);
        assertEquals(1, writer.startTransaction(ExpectedVersion.of(0)).write(events.get(0)).commit().nextExpectedVersion);

        assertEquals(events.size() + 1, size(stream));
    }

    @Test
    public void appends_0em1_0any_idempotent() {
        final String stream = generateStreamName();

        List<EventData> events = range(0, 1).mapToObj(i -> newTestEvent()).collect(toList());

        TransactionalWriter writer = newTransactionalWriter(stream);

        assertEquals(0, writer.startTransaction(ExpectedVersion.of(-1)).write(events).commit().nextExpectedVersion);
        assertEquals(0, writer.startTransaction(ExpectedVersion.any()).write(events.get(0)).commit().nextExpectedVersion);

        assertEquals(events.size(), size(stream));
    }

    @Test
    public void appends_0em1_0em1_idempotent() {
        final String stream = generateStreamName();

        List<EventData> events = range(0, 1).mapToObj(i -> newTestEvent()).collect(toList());

        TransactionalWriter writer = newTransactionalWriter(stream);

        assertEquals(0, writer.startTransaction(ExpectedVersion.of(-1)).write(events).commit().nextExpectedVersion);
        assertEquals(0, writer.startTransaction(ExpectedVersion.of(-1)).write(events.get(0)).commit().nextExpectedVersion);

        assertEquals(events.size(), size(stream));
    }

    @Test
    public void appends_0em1_1e0_2e1_1any_1any_idempotent() {
        final String stream = generateStreamName();

        List<EventData> events = range(0, 3).mapToObj(i -> newTestEvent()).collect(toList());

        TransactionalWriter writer = newTransactionalWriter(stream);

        assertEquals(2, writer.startTransaction(ExpectedVersion.of(-1)).write(events).commit().nextExpectedVersion);
        assertEquals(0, writer.startTransaction(ExpectedVersion.any())
            .write(events.get(0))
            .write(events.get(0))
            .commit().nextExpectedVersion);

        assertEquals(events.size(), size(stream));
    }

    @Test
    public void appends_S_0em1_1em1_E_S_0em1_1em1_2em1_E_idempotancy_fail() {
        final String stream = generateStreamName();

        List<EventData> events = range(0, 2).mapToObj(i -> newTestEvent()).collect(toList());

        TransactionalWriter writer = newTransactionalWriter(stream);

        assertEquals(1, writer.startTransaction(ExpectedVersion.of(-1)).write(events).commit().nextExpectedVersion);

        try {
            writer.startTransaction(ExpectedVersion.of(-1))
                .write(concat(events.stream(), Stream.of(newTestEvent())).collect(toList()))
                .commit();
            fail("append should fail with 'WrongExpectedVersionException'");
        } catch (Exception e) {
            assertThat(e.getCause(), instanceOf(WrongExpectedVersionException.class));
        }
    }

    private TransactionalWriter newTransactionalWriter(String stream) {
        return new TransactionalWriter(eventstore, stream);
    }

    private static class TransactionalWriter {
        private final EventStore eventstore;
        private final String stream;

        private TransactionalWriter(EventStore eventstore, String stream) {
            this.eventstore = eventstore;
            this.stream = stream;
        }

        private OngoingTransaction startTransaction(ExpectedVersion version) {
            return new OngoingTransaction(eventstore.startTransaction(stream, version).join());
        }
    }

    private static class OngoingTransaction {
        private final Transaction transaction;

        private OngoingTransaction(Transaction transaction) {
            this.transaction = transaction;
        }

        private OngoingTransaction write(EventData event) {
            transaction.write(event).join();
            return this;
        }

        private OngoingTransaction write(List<EventData> events) {
            transaction.write(events).join();
            return this;
        }

        private WriteResult commit() {
            return transaction.commit().join();
        }
    }

}
