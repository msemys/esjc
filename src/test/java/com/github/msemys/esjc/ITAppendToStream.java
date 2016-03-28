package com.github.msemys.esjc;

import com.github.msemys.esjc.operation.StreamDeletedException;
import com.github.msemys.esjc.operation.WrongExpectedVersionException;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.nCopies;
import static java.util.stream.IntStream.range;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class ITAppendToStream extends AbstractIntegrationTest {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSupplier.get();
    }

    @Test
    public void allowsAppendZeroEventsToStream() throws Exception {
        final String stream1 = generateStreamName();

        assertEquals(-1, eventstore.appendToStream(stream1, ExpectedVersion.any(), emptyList()).get().nextExpectedVersion);
        assertEquals(-1, eventstore.appendToStream(stream1, ExpectedVersion.noStream(), emptyList()).get().nextExpectedVersion);
        assertEquals(-1, eventstore.appendToStream(stream1, ExpectedVersion.any(), emptyList()).get().nextExpectedVersion);
        assertEquals(-1, eventstore.appendToStream(stream1, ExpectedVersion.noStream(), emptyList()).get().nextExpectedVersion);

        StreamEventsSlice read1 = eventstore.readStreamEventsForward(stream1, 0, 2, false).get();
        assertThat(read1.events.size(), is(0));


        final String stream2 = generateStreamName();

        assertEquals(-1, eventstore.appendToStream(stream2, ExpectedVersion.noStream(), emptyList()).get().nextExpectedVersion);
        assertEquals(-1, eventstore.appendToStream(stream2, ExpectedVersion.any(), emptyList()).get().nextExpectedVersion);
        assertEquals(-1, eventstore.appendToStream(stream2, ExpectedVersion.noStream(), emptyList()).get().nextExpectedVersion);
        assertEquals(-1, eventstore.appendToStream(stream2, ExpectedVersion.any(), emptyList()).get().nextExpectedVersion);

        StreamEventsSlice read2 = eventstore.readStreamEventsForward(stream2, 0, 2, false).get();
        assertThat(read2.events.size(), is(0));
    }

    @Test
    public void createsStreamWithNoStreamExpectedVersionOnFirstWriteIfDoesNotExist() throws Exception {
        final String stream = generateStreamName();

        assertEquals(0, eventstore.appendToStream(stream, ExpectedVersion.noStream(), asList(newTestEvent())).get().nextExpectedVersion);

        StreamEventsSlice read = eventstore.readStreamEventsForward(stream, 0, 2, false).get();
        assertThat(read.events.size(), is(1));
    }

    @Test
    public void createsStreamWithAnyExpectedVersionOnFirstWriteIfDoesNotExist() throws Exception {
        final String stream = generateStreamName();

        assertEquals(0, eventstore.appendToStream(stream, ExpectedVersion.any(), asList(newTestEvent())).get().nextExpectedVersion);

        StreamEventsSlice read = eventstore.readStreamEventsForward(stream, 0, 2, false).get();
        assertThat(read.events.size(), is(1));
    }

    @Test
    public void multipleIdempotentWrites() throws Exception {
        final String stream = generateStreamName();

        List<EventData> events = asList(newTestEvent(), newTestEvent(), newTestEvent(), newTestEvent());

        assertEquals(3, eventstore.appendToStream(stream, ExpectedVersion.any(), events).get().nextExpectedVersion);
        assertEquals(3, eventstore.appendToStream(stream, ExpectedVersion.any(), events).get().nextExpectedVersion);
    }

    @Test
    public void multipleIdempotentWritesWithSameIdBugCase() throws Exception {
        final String stream = generateStreamName();

        EventData event = newTestEvent();
        List<EventData> events = nCopies(6, event);

        assertEquals(5, eventstore.appendToStream(stream, ExpectedVersion.any(), events).get().nextExpectedVersion);
    }

    @Test
    public void inWtfMultipleCaseOfMultipleWritesExpectedVersionAnyPerAllSameId() throws Exception {
        final String stream = generateStreamName();

        EventData event = newTestEvent();
        List<EventData> events = nCopies(6, event);

        assertEquals(5, eventstore.appendToStream(stream, ExpectedVersion.any(), events).get().nextExpectedVersion);
        WriteResult result = eventstore.appendToStream(stream, ExpectedVersion.any(), events).get();
        assertEquals(0, result.nextExpectedVersion);
    }

    @Test
    public void inSlightlyReasonableMultipleCaseOfMultipleWritesWithExpectedVersionPerAllSameId() throws Exception {
        final String stream = generateStreamName();

        EventData event = newTestEvent();
        List<EventData> events = nCopies(6, event);

        assertEquals(5, eventstore.appendToStream(stream, ExpectedVersion.noStream(), events).get().nextExpectedVersion);
        WriteResult result = eventstore.appendToStream(stream, ExpectedVersion.noStream(), events).get();
        assertEquals(5, result.nextExpectedVersion);
    }

    @Test
    public void failsWritingWithCorrectExpectedVersionToDeletedStream() throws Exception {
        final String stream = generateStreamName();

        eventstore.deleteStream(stream, ExpectedVersion.noStream(), true).get();

        try {
            eventstore.appendToStream(stream, ExpectedVersion.noStream(), asList(newTestEvent())).get();
            fail("append should fail with 'StreamDeletedException'");
        } catch (Exception e) {
            assertThat(e.getCause(), instanceOf(StreamDeletedException.class));
        }
    }

    @Test
    public void returnsLogPositionWhenWriting() throws Exception {
        final String stream = generateStreamName();

        WriteResult result = eventstore.appendToStream(stream, ExpectedVersion.noStream(), asList(newTestEvent())).get();
        assertTrue(0 < result.logPosition.preparePosition);
        assertTrue(0 < result.logPosition.commitPosition);
    }

    @Test
    public void failsWritingWithAnyExpectedVersionToDeletedStream() throws Exception {
        final String stream = generateStreamName();

        eventstore.deleteStream(stream, ExpectedVersion.noStream(), true).get();

        try {
            eventstore.appendToStream(stream, ExpectedVersion.any(), asList(newTestEvent())).get();
            fail("append should fail with 'StreamDeletedException'");
        } catch (Exception e) {
            assertThat(e.getCause(), instanceOf(StreamDeletedException.class));
        }
    }

    @Test
    public void failsWritingWithInvalidExpectedVersionToDeletedStream() throws Exception {
        final String stream = generateStreamName();

        eventstore.deleteStream(stream, ExpectedVersion.noStream(), true).get();

        try {
            eventstore.appendToStream(stream, ExpectedVersion.of(5), asList(newTestEvent())).get();
            fail("append should fail with 'StreamDeletedException'");
        } catch (Exception e) {
            assertThat(e.getCause(), instanceOf(StreamDeletedException.class));
        }
    }

    @Test
    public void appendsWithCorrectExpectedVersionToExistingStream() throws Exception {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.noStream(), asList(newTestEvent())).get();

        eventstore.appendToStream(stream, ExpectedVersion.of(0), asList(newTestEvent())).get();
    }

    @Test
    public void appendsWithAnyExpectedVersionToExistingStream() throws Exception {
        final String stream = generateStreamName();

        assertEquals(0, eventstore.appendToStream(stream, ExpectedVersion.noStream(), asList(newTestEvent())).get().nextExpectedVersion);
        assertEquals(1, eventstore.appendToStream(stream, ExpectedVersion.any(), asList(newTestEvent())).get().nextExpectedVersion);
    }

    @Test
    public void failsAppendingWithWrongExpectedVersionToExistingStream() {
        final String stream = generateStreamName();

        try {
            eventstore.appendToStream(stream, ExpectedVersion.of(1), asList(newTestEvent())).get();
            fail("append should fail with 'WrongExpectedVersionException'");
        } catch (Exception e) {
            assertThat(e.getCause(), instanceOf(WrongExpectedVersionException.class));
        }
    }

    @Test
    public void appendsMultipleEventsAtOnce() throws Exception {
        final String stream = generateStreamName();

        List<EventData> events = range(0, 100).mapToObj(i -> EventData.newBuilder()
            .type("test")
            .data(String.valueOf(i))
            .metadata(String.valueOf(i))
            .build()).collect(Collectors.toList());

        assertEquals(99, eventstore.appendToStream(stream, ExpectedVersion.noStream(), events).get().nextExpectedVersion);
    }

}
