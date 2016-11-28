package com.github.msemys.esjc;

import com.github.msemys.esjc.operation.StreamDeletedException;
import com.github.msemys.esjc.operation.WrongExpectedVersionException;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.github.msemys.esjc.util.Strings.newString;
import static java.util.Collections.emptyList;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;

public class ITTransaction extends AbstractIntegrationTest {

    @Override
    protected EventStore createEventStore() {
        return eventstoreSupplier.get();
    }

    @Test
    public void startsOnNonExistingStreamWithCorrectExpectedVersionAndCreatesStreamOnCommit() {
        final String stream = generateStreamName();

        Transaction transaction = eventstore.startTransaction(stream, ExpectedVersion.NO_STREAM).join();
        transaction.write(newTestEvent()).join();

        assertEquals(0, transaction.commit().join().nextExpectedVersion);
    }

    @Test
    public void startsOnNonExistingStreamWithExpectedVersionAnyAndCreatesStreamOnCommit() {
        final String stream = generateStreamName();

        Transaction transaction = eventstore.startTransaction(stream, ExpectedVersion.ANY).join();
        transaction.write(newTestEvent()).join();

        assertEquals(0, transaction.commit().join().nextExpectedVersion);
    }

    @Test
    public void failsToCommitNonExistingStreamWithWrongExpectedVersion() {
        final String stream = generateStreamName();

        Transaction transaction = eventstore.startTransaction(stream, ExpectedVersion.of(1)).join();
        transaction.write(newTestEvent()).join();

        try {
            transaction.commit().join();
            fail("should fail with 'WrongExpectedVersionException'");
        } catch (Exception e) {
            assertThat(e.getCause(), instanceOf(WrongExpectedVersionException.class));
        }
    }

    @Test
    public void doesNothingIfCommitsNoEventsToEmptyStream() {
        final String stream = generateStreamName();

        Transaction transaction = eventstore.startTransaction(stream, ExpectedVersion.NO_STREAM).join();
        assertEquals(-1, transaction.commit().join().nextExpectedVersion);

        StreamEventsSlice slice = eventstore.readStreamEventsForward(stream, 0, 1, false).join();
        assertTrue(slice.events.isEmpty());
    }

    @Test
    public void doesNothingIfTransactionallyWritingNoEventsToEmptyStream() {
        final String stream = generateStreamName();

        Transaction transaction = eventstore.startTransaction(stream, ExpectedVersion.NO_STREAM).join();
        transaction.write(emptyList()).join();
        assertEquals(-1, transaction.commit().join().nextExpectedVersion);

        StreamEventsSlice slice = eventstore.readStreamEventsForward(stream, 0, 1, false).join();
        assertTrue(slice.events.isEmpty());
    }

    @Test
    public void validatesExpectationsOnCommit() {
        final String stream = generateStreamName();

        Transaction transaction = eventstore.startTransaction(stream, ExpectedVersion.of(100500)).join();
        transaction.write(newTestEvent()).join();

        try {
            transaction.commit().join();
            fail("should fail with 'WrongExpectedVersionException'");
        } catch (Exception e) {
            assertThat(e.getCause(), instanceOf(WrongExpectedVersionException.class));
        }
    }

    @Test
    public void commitsWhenWritingWithExpectedVersionAnyEvenWhileSomeoneIsWritingInParallel() throws InterruptedException {
        final String stream = generateStreamName();

        ExecutorService threadPool = Executors.newCachedThreadPool();

        CountDownLatch transactionalWritesSignal = new CountDownLatch(1);
        CountDownLatch plainWritesSignal = new CountDownLatch(1);

        final int totalTransactionalWrites = 500;
        final int totalPlainWrites = 500;

        //500 events during transaction
        threadPool.execute(() -> {
            Transaction transaction = eventstore.startTransaction(stream, ExpectedVersion.ANY).join();

            CompletableFuture[] writes = new CompletableFuture[totalTransactionalWrites];
            for (int i = 0; i < totalTransactionalWrites; i++) {
                writes[i] = transaction.write(
                    EventData.newBuilder()
                        .type("test")
                        .data(String.valueOf(i))
                        .metadata("transactional write")
                        .build()
                );
            }

            allOf(writes).join();
            transaction.commit().join();
            transactionalWritesSignal.countDown();
        });

        //500 events to same stream in parallel
        threadPool.execute(() -> {
            CompletableFuture[] writes = new CompletableFuture[totalPlainWrites];
            for (int i = 0; i < totalPlainWrites; i++) {
                writes[i] = eventstore.appendToStream(stream, ExpectedVersion.ANY,
                    EventData.newBuilder()
                        .type("test")
                        .data(String.valueOf(i))
                        .metadata("plain write")
                        .build()
                );
            }

            allOf(writes).join();
            plainWritesSignal.countDown();
        });

        assertTrue("Transactional write timeout", transactionalWritesSignal.await(30, SECONDS));
        assertTrue("Plain write timeout", plainWritesSignal.await(30, SECONDS));

        StreamEventsSlice slice = eventstore.readStreamEventsForward(stream, 0, totalTransactionalWrites + totalPlainWrites, false).join();
        assertEquals(totalTransactionalWrites + totalPlainWrites, slice.events.size());
        assertEquals(totalTransactionalWrites, slice.events.stream().filter(e -> newString(e.event.metadata).equals("transactional write")).count());
        assertEquals(totalPlainWrites, slice.events.stream().filter(e -> newString(e.event.metadata).equals("plain write")).count());
    }

    @Test
    public void failsToCommitIfStartedWithCorrectVersionButCommittingWithBad() {
        final String stream = generateStreamName();

        Transaction transaction = eventstore.startTransaction(stream, ExpectedVersion.NO_STREAM).join();
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, newTestEvent()).join();
        transaction.write(newTestEvent()).join();

        try {
            transaction.commit().join();
            fail("should fail with 'WrongExpectedVersionException'");
        } catch (Exception e) {
            assertThat(e.getCause(), instanceOf(WrongExpectedVersionException.class));
        }
    }

    @Test
    public void succeedsToCommitIfStartedWithWrongVersionButCommittingWithCorrectVersion() {
        final String stream = generateStreamName();

        Transaction transaction = eventstore.startTransaction(stream, ExpectedVersion.of(0)).join();
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, newTestEvent()).join();
        transaction.write(newTestEvent()).join();

        assertEquals(1, transaction.commit().join().nextExpectedVersion);
    }

    @Test
    public void failsToCommitIfStartedWithCorrectVersionButOnCommitStreamWasDeleted() {
        final String stream = generateStreamName();

        Transaction transaction = eventstore.startTransaction(stream, ExpectedVersion.NO_STREAM).join();
        transaction.write(newTestEvent()).join();
        eventstore.deleteStream(stream, ExpectedVersion.NO_STREAM, true).join();

        try {
            transaction.commit().join();
            fail("should fail with 'StreamDeletedException'");
        } catch (Exception e) {
            assertThat(e.getCause(), instanceOf(StreamDeletedException.class));
        }
    }

    @Test
    public void idempotencyIsCorrectForExplicitTransactionsWithExpectedVersionAny() {
        final String stream = generateStreamName();

        EventData event = EventData.newBuilder()
            .type("SomethingHappened")
            .jsonData("{Value:42}")
            .build();

        Transaction transaction1 = eventstore.startTransaction(stream, ExpectedVersion.ANY).join();
        transaction1.write(event).join();
        assertEquals(0, transaction1.commit().join().nextExpectedVersion);

        Transaction transaction2 = eventstore.startTransaction(stream, ExpectedVersion.ANY).join();
        transaction2.write(event).join();
        assertEquals(0, transaction2.commit().join().nextExpectedVersion);

        StreamEventsSlice slice = eventstore.readStreamEventsForward(stream, 0, 100, false).join();
        assertEquals(event.eventId, slice.events.get(0).event.eventId);
    }

}
