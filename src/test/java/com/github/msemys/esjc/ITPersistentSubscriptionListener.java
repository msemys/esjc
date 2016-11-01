package com.github.msemys.esjc;

import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.IntStream.range;
import static org.junit.Assert.assertTrue;

public class ITPersistentSubscriptionListener extends AbstractIntegrationTest {
    private static final int BUFFER_SIZE = 10;
    private static final int EVENT_COUNT = BUFFER_SIZE * 2;

    @Override
    protected EventStore createEventStore() {
        return eventstoreSupplier.get();
    }

    @Test
    public void acknowledgeEventManually() throws InterruptedException {
        final String stream = generateStreamName();
        final String group = UUID.randomUUID().toString();

        CountDownLatch eventSignal = new CountDownLatch(EVENT_COUNT);

        PersistentSubscriptionSettings settings = PersistentSubscriptionSettings.newBuilder()
            .resolveLinkTos(true)
            .startFromCurrent()
            .build();

        eventstore.createPersistentSubscription(stream, group, settings).join();

        eventstore.subscribeToPersistent(stream, group, (s, e) -> {
            s.acknowledge(e);
            eventSignal.countDown();
        }, BUFFER_SIZE, false).join();

        range(0, EVENT_COUNT).forEach(i -> eventstore.appendToStream(stream, ExpectedVersion.any(), newTestEvent()));

        assertTrue("onEvent timeout", eventSignal.await(10, SECONDS));
    }

    @Test
    public void acknowledgeEventAutomatically() throws InterruptedException {
        final String stream = generateStreamName();
        final String group = UUID.randomUUID().toString();

        CountDownLatch eventSignal = new CountDownLatch(EVENT_COUNT);

        PersistentSubscriptionSettings settings = PersistentSubscriptionSettings.newBuilder()
            .resolveLinkTos(true)
            .startFromCurrent()
            .build();

        eventstore.createPersistentSubscription(stream, group, settings).join();

        eventstore.subscribeToPersistent(stream, group, (s, e) -> eventSignal.countDown(), BUFFER_SIZE, true).join();

        range(0, EVENT_COUNT).forEach(i -> eventstore.appendToStream(stream, ExpectedVersion.any(), newTestEvent()));

        assertTrue("onEvent timeout", eventSignal.await(10, SECONDS));
    }

    @Test
    public void acknowledgeEventManuallyStartingFromBeginning() throws InterruptedException {
        final String stream = generateStreamName();
        final String group = UUID.randomUUID().toString();

        CountDownLatch eventSignal = new CountDownLatch(EVENT_COUNT);

        PersistentSubscriptionSettings settings = PersistentSubscriptionSettings.newBuilder()
            .resolveLinkTos(true)
            .startFromBeginning()
            .build();

        range(0, EVENT_COUNT).forEach(i -> eventstore.appendToStream(stream, ExpectedVersion.any(), newTestEvent()).join());

        eventstore.createPersistentSubscription(stream, group, settings).join();

        eventstore.subscribeToPersistent(stream, group, (s, e) -> {
            s.acknowledge(e);
            eventSignal.countDown();
        }, BUFFER_SIZE, false).join();

        assertTrue("onEvent timeout", eventSignal.await(10, SECONDS));
    }

    @Test
    public void acknowledgeEventAutomaticallyStartingFromBeginning() throws InterruptedException {
        final String stream = generateStreamName();
        final String group = UUID.randomUUID().toString();

        CountDownLatch eventSignal = new CountDownLatch(EVENT_COUNT);

        PersistentSubscriptionSettings settings = PersistentSubscriptionSettings.newBuilder()
            .resolveLinkTos(true)
            .startFromBeginning()
            .build();

        range(0, EVENT_COUNT).forEach(i -> eventstore.appendToStream(stream, ExpectedVersion.any(), newTestEvent()).join());

        eventstore.createPersistentSubscription(stream, group, settings).join();

        eventstore.subscribeToPersistent(stream, group, (s, e) -> eventSignal.countDown(), BUFFER_SIZE, true).join();

        assertTrue("onEvent timeout", eventSignal.await(10, SECONDS));
    }

    @Test
    public void acknowledgeLinkedEventManuallyStartingFromBeginning() throws InterruptedException {
        final String stream1 = generateStreamName();
        final String stream2 = generateStreamName();
        final String group = UUID.randomUUID().toString();

        CountDownLatch eventSignal = new CountDownLatch(EVENT_COUNT);

        PersistentSubscriptionSettings settings = PersistentSubscriptionSettings.newBuilder()
            .resolveLinkTos(true)
            .startFromBeginning()
            .build();

        range(0, EVENT_COUNT).forEach(i -> eventstore.appendToStream(stream2, ExpectedVersion.any(), newTestEvent()).join());

        eventstore.createPersistentSubscription(stream1, group, settings).join();

        eventstore.subscribeToPersistent(stream1, group, (s, e) -> {
            if (e.isResolved()) {
                s.acknowledge(e);
                eventSignal.countDown();
            }
        }, BUFFER_SIZE, false).join();

        range(0, EVENT_COUNT).forEach(i ->
            eventstore.appendToStream(stream1, ExpectedVersion.any(),
                EventData.newBuilder()
                    .linkTo(i, stream2)
                    .build()
            ).join());

        assertTrue("onEvent timeout", eventSignal.await(10, SECONDS));
    }

    @Test
    public void acknowledgeLinkedEventAutomaticallyStartingFromBeginning() throws InterruptedException {
        final String stream1 = generateStreamName();
        final String stream2 = generateStreamName();
        final String group = UUID.randomUUID().toString();

        CountDownLatch eventSignal = new CountDownLatch(EVENT_COUNT);

        PersistentSubscriptionSettings settings = PersistentSubscriptionSettings.newBuilder()
            .resolveLinkTos(true)
            .startFromBeginning()
            .build();

        range(0, EVENT_COUNT).forEach(i -> eventstore.appendToStream(stream2, ExpectedVersion.any(), newTestEvent()).join());

        eventstore.createPersistentSubscription(stream1, group, settings).join();

        eventstore.subscribeToPersistent(stream1, group, (s, e) -> {
            if (e.isResolved()) {
                eventSignal.countDown();
            }
        }, BUFFER_SIZE, true).join();

        range(0, EVENT_COUNT).forEach(i -> eventstore.appendToStream(stream1, ExpectedVersion.any(),
            EventData.newBuilder()
                .linkTo(i, stream2)
                .build()
        ).join());

        assertTrue("onEvent timeout", eventSignal.await(10, SECONDS));
    }

}
