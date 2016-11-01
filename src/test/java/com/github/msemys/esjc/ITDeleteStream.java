package com.github.msemys.esjc;

import com.github.msemys.esjc.operation.StreamDeletedException;
import com.github.msemys.esjc.operation.WrongExpectedVersionException;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class ITDeleteStream extends AbstractIntegrationTest {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSupplier.get();
    }

    @Test
    public void succeedsToDeleteNonExistentStreamWithNoStreamExpectedVersion() {
        final String stream = generateStreamName();
        eventstore.deleteStream(stream, ExpectedVersion.noStream(), true).join();
    }

    @Test
    public void succeedsToDeleteNonExistentStreamWithAnyExpectedVersion() {
        final String stream = generateStreamName();
        eventstore.deleteStream(stream, ExpectedVersion.any(), true).join();
    }

    @Test
    public void succeedsToDeleteExistingStream() {
        final String stream = generateStreamName();
        eventstore.appendToStream(stream, ExpectedVersion.noStream(), newTestEvent()).join();
        eventstore.deleteStream(stream, ExpectedVersion.of(0), true).join();
    }

    @Test
    public void failsToDeleteNonExistentStreamWithInvalidExpectedVersion() {
        final String stream = generateStreamName();
        try {
            eventstore.deleteStream(stream, ExpectedVersion.of(1), true).join();
            fail("delete should fail with 'WrongExpectedVersionException'");
        } catch (Exception e) {
            assertThat(e.getCause(), instanceOf(WrongExpectedVersionException.class));
        }
    }

    @Test
    public void failsToDeleteAlreadyDeletedStream() {
        final String stream = generateStreamName();

        eventstore.deleteStream(stream, ExpectedVersion.noStream(), true).join();

        try {
            eventstore.deleteStream(stream, ExpectedVersion.any(), true).join();
            fail("delete should fail with 'StreamDeletedException'");
        } catch (Exception e) {
            assertThat(e.getCause(), instanceOf(StreamDeletedException.class));
        }
    }

}
