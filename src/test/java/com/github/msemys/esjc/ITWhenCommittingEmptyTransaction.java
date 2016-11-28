package com.github.msemys.esjc;

import com.github.msemys.esjc.operation.WrongExpectedVersionException;
import org.junit.Test;

import static java.util.Arrays.asList;
import static java.util.stream.IntStream.range;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;

public class ITWhenCommittingEmptyTransaction extends AbstractIntegrationTest {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSupplier.get();
    }

    @Test
    public void followingAppendWithCorrectExpectedVersionAreCommittedCorrectly() {
        final String stream = generateStreamName();

        EventData firstEvent = newTestEvent();

        assertEquals(2, eventstore.appendToStream(stream,
            ExpectedVersion.NO_STREAM,
            asList(firstEvent, newTestEvent(), newTestEvent()))
            .join().nextExpectedVersion);

        Transaction transaction = eventstore.startTransaction(stream, ExpectedVersion.of(2)).join();
        assertEquals(2, transaction.commit().join().nextExpectedVersion);

        assertEquals(4, eventstore.appendToStream(stream, ExpectedVersion.of(2), asList(newTestEvent(), newTestEvent())).join().nextExpectedVersion);

        StreamEventsSlice slice = eventstore.readStreamEventsForward(stream, 0, 100, false).join();
        assertEquals(SliceReadStatus.Success, slice.status);
        assertEquals(5, slice.events.size());
        range(0, 5).forEach(i -> assertEquals(i, slice.events.get(i).originalEventNumber()));
    }

    @Test
    public void followingAppendWithExpectedVersionAnyAreCommittedCorrectly() {
        final String stream = generateStreamName();

        EventData firstEvent = newTestEvent();

        assertEquals(2, eventstore.appendToStream(stream,
            ExpectedVersion.NO_STREAM,
            asList(firstEvent, newTestEvent(), newTestEvent()))
            .join().nextExpectedVersion);

        Transaction transaction = eventstore.startTransaction(stream, ExpectedVersion.of(2)).join();
        assertEquals(2, transaction.commit().join().nextExpectedVersion);

        assertEquals(4, eventstore.appendToStream(stream, ExpectedVersion.ANY, asList(newTestEvent(), newTestEvent())).join().nextExpectedVersion);

        StreamEventsSlice slice = eventstore.readStreamEventsForward(stream, 0, 100, false).join();
        assertEquals(SliceReadStatus.Success, slice.status);
        assertEquals(5, slice.events.size());
        range(0, 5).forEach(i -> assertEquals(i, slice.events.get(i).originalEventNumber()));
    }

    @Test
    public void committingFirstEventWithExpectedVersionNoStreamIsIdempotent() {
        final String stream = generateStreamName();

        EventData firstEvent = newTestEvent();

        assertEquals(2, eventstore.appendToStream(stream,
            ExpectedVersion.NO_STREAM,
            asList(firstEvent, newTestEvent(), newTestEvent()))
            .join().nextExpectedVersion);

        Transaction transaction = eventstore.startTransaction(stream, ExpectedVersion.of(2)).join();
        assertEquals(2, transaction.commit().join().nextExpectedVersion);

        assertEquals(0, eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, firstEvent).join().nextExpectedVersion);

        StreamEventsSlice slice = eventstore.readStreamEventsForward(stream, 0, 100, false).join();
        assertEquals(SliceReadStatus.Success, slice.status);
        assertEquals(3, slice.events.size());
        range(0, 3).forEach(i -> assertEquals(i, slice.events.get(i).originalEventNumber()));
    }

    @Test
    public void failsToAppendNewEventsWithExpectedVersionNoStream() {
        final String stream = generateStreamName();

        EventData firstEvent = newTestEvent();

        assertEquals(2, eventstore.appendToStream(stream,
            ExpectedVersion.NO_STREAM,
            asList(firstEvent, newTestEvent(), newTestEvent()))
            .join().nextExpectedVersion);

        Transaction transaction = eventstore.startTransaction(stream, ExpectedVersion.of(2)).join();
        assertEquals(2, transaction.commit().join().nextExpectedVersion);

        try {
            eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, newTestEvent()).join();
            fail("should fail with 'WrongExpectedVersionException'");
        } catch (Exception e) {
            assertThat(e.getCause(), instanceOf(WrongExpectedVersionException.class));
        }
    }

}
