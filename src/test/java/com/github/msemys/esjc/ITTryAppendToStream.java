package com.github.msemys.esjc;

import org.junit.Test;

import static org.junit.Assert.*;

public class ITTryAppendToStream extends AbstractIntegrationTest {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSupplier.get();
    }

    @Test
    public void returnsWrongExpectedVersionStatusWhenAttemptsToWriteWithInvalidExpectedVersion() {
        final String stream = generateStreamName();

        WriteAttemptResult result = eventstore.tryAppendToStream(stream, ExpectedVersion.of(17), newTestEvent()).join();

        assertEquals(WriteStatus.WrongExpectedVersion, result.status);
        assertEquals(ExpectedVersion.ANY.value, result.nextExpectedVersion);
        assertNull(result.logPosition);
    }

    @Test
    public void returnsStreamDeletedStatusWhenAttemptsToWriteToDeletedStream() {
        final String stream = generateStreamName();

        eventstore.appendToStream(stream, ExpectedVersion.ANY, newTestEvent()).join();
        eventstore.deleteStream(stream, ExpectedVersion.ANY, true).join();

        WriteAttemptResult result = eventstore.tryAppendToStream(stream, ExpectedVersion.ANY, newTestEvent()).join();

        assertEquals(WriteStatus.StreamDeleted, result.status);
        assertEquals(ExpectedVersion.ANY.value, result.nextExpectedVersion);
        assertNull(result.logPosition);
    }

    @Test
    public void returnsSuccessStatusWhenAttemptsToWriteWithCorrectExpectedVersion() {
        final String stream = generateStreamName();

        WriteAttemptResult result = eventstore.tryAppendToStream(stream, ExpectedVersion.NO_STREAM, newTestEvent()).join();

        assertEquals(WriteStatus.Success, result.status);
        assertEquals(0, result.nextExpectedVersion);
        assertNotNull(result.logPosition);
    }

}
