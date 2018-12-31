package com.github.msemys.esjc;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertTrue;

public class ITSubscribeToStream extends AbstractEventStoreTest {

    public ITSubscribeToStream(EventStore eventstore) {
        super(eventstore);
    }

    @Test
    public void subscribesToNonExistingStreamAndThenCatchesNewEvent() throws InterruptedException {
        final String stream = generateStreamName();

        CountDownLatch eventSignal = new CountDownLatch(1);

        eventstore.subscribeToStream(stream, false, (s, e) -> eventSignal.countDown()).join();
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, newTestEvent()).join();

        assertTrue("onEvent timeout", eventSignal.await(10, SECONDS));
    }

    @Test
    public void allowsMultipleSubscriptionsToSameStream() throws InterruptedException {
        final String stream = generateStreamName();

        CountDownLatch eventSignal = new CountDownLatch(2);

        eventstore.subscribeToStream(stream, false, (s, e) -> eventSignal.countDown()).join();
        eventstore.subscribeToStream(stream, false, (s, e) -> eventSignal.countDown()).join();
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, newTestEvent()).join();

        assertTrue("onEvent timeout", eventSignal.await(10, SECONDS));
    }

    @Test
    public void triggersOnCloseCallbackAfterUnsubscribeMethodCall() throws InterruptedException {
        final String stream = generateStreamName();

        CountDownLatch closeSignal = new CountDownLatch(1);

        Subscription subscription = eventstore.subscribeToStream(stream, false, new VolatileSubscriptionListener() {
            @Override
            public void onEvent(Subscription subscription, ResolvedEvent event) {

            }

            @Override
            public void onClose(Subscription subscription, SubscriptionDropReason reason, Exception exception) {
                closeSignal.countDown();
            }
        }).join();

        subscription.unsubscribe();

        assertTrue("onClose timeout", closeSignal.await(10, SECONDS));
    }

    @Test
    public void catchesDeletedEventsAsWell() throws InterruptedException {
        final String stream = generateStreamName();

        CountDownLatch eventSignal = new CountDownLatch(1);

        eventstore.subscribeToStream(stream, false, (s, e) -> eventSignal.countDown()).join();
        eventstore.deleteStream(stream, ExpectedVersion.NO_STREAM, true).join();

        assertTrue("onEvent timeout", eventSignal.await(10, SECONDS));
    }

}
