package com.github.msemys.esjc;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;

import static com.github.msemys.esjc.util.Threads.sleepUninterruptibly;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.IntStream.range;
import static org.junit.Assert.*;

public class ITSubscribeToStreamFrom extends AbstractIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(ITSubscribeToStreamFrom.class);

    @Override
    protected EventStore createEventStore() {
        return eventstoreSupplier.get();
    }

    @Test
    public void subscribesToNonExistingStream() throws InterruptedException, TimeoutException {
        final String stream = generateStreamName();

        CountDownLatch eventSignal = new CountDownLatch(1);
        CountDownLatch closeSignal = new CountDownLatch(1);

        CatchUpSubscription subscription = eventstore.subscribeToStreamFrom(stream, null, false, new CatchUpSubscriptionListener() {
            @Override
            public void onEvent(CatchUpSubscription subscription, ResolvedEvent event) {
                eventSignal.countDown();
            }

            @Override
            public void onLiveProcessingStarted(CatchUpSubscription subscription) {
                logger.info("Live processing started.");
            }

            @Override
            public void onClose(CatchUpSubscription subscription, SubscriptionDropReason reason, Exception exception) {
                closeSignal.countDown();
            }
        });

        sleepUninterruptibly(100); // give time for first pull phase

        eventstore.subscribeToStream(stream, false, (s, e) -> {
        }).join();

        sleepUninterruptibly(100);

        assertFalse("Some event appeared.", eventSignal.await(0, SECONDS));

        assertFalse("Subscription was dropped prematurely.", closeSignal.await(0, SECONDS));
        subscription.stop(Duration.ofSeconds(10));
        assertTrue("onClose timeout", closeSignal.await(10, SECONDS));
    }

    @Test
    public void subscribesToNonExistingStreamAndThenCatchesEvent() throws InterruptedException, TimeoutException {
        final String stream = generateStreamName();

        CountDownLatch eventSignal = new CountDownLatch(1);
        CountDownLatch closeSignal = new CountDownLatch(1);

        CatchUpSubscription subscription = eventstore.subscribeToStreamFrom(stream, null, false, new CatchUpSubscriptionListener() {
            @Override
            public void onEvent(CatchUpSubscription subscription, ResolvedEvent event) {
                eventSignal.countDown();
            }

            @Override
            public void onLiveProcessingStarted(CatchUpSubscription subscription) {
                logger.info("Live processing started.");
            }

            @Override
            public void onClose(CatchUpSubscription subscription, SubscriptionDropReason reason, Exception exception) {
                closeSignal.countDown();
            }
        });

        eventstore.appendToStream(stream, ExpectedVersion.noStream(), asList(newTestEvent())).join();

        assertTrue("onEvent timeout", eventSignal.await(10, SECONDS));

        assertFalse("Subscription was dropped prematurely.", closeSignal.await(0, SECONDS));
        subscription.stop(Duration.ofSeconds(10));
        assertTrue("onClose timeout", closeSignal.await(10, SECONDS));
    }

    @Test
    public void allowsMultipleSubscriptionsToSameStream() throws InterruptedException, TimeoutException {
        final String stream = generateStreamName();

        CountDownLatch eventSignal = new CountDownLatch(2);
        CountDownLatch closeSignal1 = new CountDownLatch(1);
        CountDownLatch closeSignal2 = new CountDownLatch(1);

        CatchUpSubscription subscription1 = eventstore.subscribeToStreamFrom(stream, null, false, new CatchUpSubscriptionListener() {
            @Override
            public void onEvent(CatchUpSubscription subscription, ResolvedEvent event) {
                eventSignal.countDown();
            }

            @Override
            public void onLiveProcessingStarted(CatchUpSubscription subscription) {
                logger.info("Live processing started.");
            }

            @Override
            public void onClose(CatchUpSubscription subscription, SubscriptionDropReason reason, Exception exception) {
                closeSignal1.countDown();
            }
        });

        CatchUpSubscription subscription2 = eventstore.subscribeToStreamFrom(stream, null, false, new CatchUpSubscriptionListener() {
            @Override
            public void onEvent(CatchUpSubscription subscription, ResolvedEvent event) {
                eventSignal.countDown();
            }

            @Override
            public void onLiveProcessingStarted(CatchUpSubscription subscription) {
                logger.info("Live processing started.");
            }

            @Override
            public void onClose(CatchUpSubscription subscription, SubscriptionDropReason reason, Exception exception) {
                closeSignal2.countDown();
            }
        });

        eventstore.appendToStream(stream, ExpectedVersion.noStream(), asList(newTestEvent())).join();

        assertTrue("onEvent timeout", eventSignal.await(10, SECONDS));

        assertFalse("Subscription1 was dropped prematurely.", closeSignal1.await(0, SECONDS));
        subscription1.stop(Duration.ofSeconds(10));
        assertTrue("Subscription1 onClose timeout", closeSignal1.await(10, SECONDS));

        assertFalse("Subscription2 was dropped prematurely.", closeSignal2.await(0, SECONDS));
        subscription2.stop(Duration.ofSeconds(10));
        assertTrue("Subscription2 onClose timeout", closeSignal2.await(10, SECONDS));
    }

    @Test
    public void triggersOnCloseCallbackAfterStopMethodCall() throws InterruptedException, TimeoutException {
        final String stream = generateStreamName();

        CountDownLatch closeSignal = new CountDownLatch(1);

        CatchUpSubscription subscription = eventstore.subscribeToStreamFrom(stream, null, false, new CatchUpSubscriptionListener() {
            @Override
            public void onEvent(CatchUpSubscription subscription, ResolvedEvent event) {
            }

            @Override
            public void onLiveProcessingStarted(CatchUpSubscription subscription) {
                logger.info("Live processing started.");
            }

            @Override
            public void onClose(CatchUpSubscription subscription, SubscriptionDropReason reason, Exception exception) {
                closeSignal.countDown();
            }
        });

        assertFalse("Subscription was dropped prematurely.", closeSignal.await(0, SECONDS));
        subscription.stop(Duration.ofSeconds(10));
        assertTrue("onClose timeout", closeSignal.await(10, SECONDS));
    }

    @Test
    public void readsAllExistingEventsAndKeepListeningToNewOnes() throws InterruptedException, TimeoutException {
        final String stream = generateStreamName();

        List<ResolvedEvent> events = new ArrayList<>();

        CountDownLatch eventSignal = new CountDownLatch(20);
        CountDownLatch closeSignal = new CountDownLatch(1);

        range(0, 10).forEach(i -> eventstore.appendToStream(stream, ExpectedVersion.of(i - 1),
            asList(EventData.newBuilder().type("et-" + i).build())
        ).join());

        CatchUpSubscription subscription = eventstore.subscribeToStreamFrom(stream, null, false, new CatchUpSubscriptionListener() {
            @Override
            public void onEvent(CatchUpSubscription subscription, ResolvedEvent event) {
                events.add(event);
                eventSignal.countDown();
            }

            @Override
            public void onLiveProcessingStarted(CatchUpSubscription subscription) {
                logger.info("Live processing started.");
            }

            @Override
            public void onClose(CatchUpSubscription subscription, SubscriptionDropReason reason, Exception exception) {
                closeSignal.countDown();
            }
        });

        range(10, 20).forEach(i -> eventstore.appendToStream(stream, ExpectedVersion.of(i - 1),
            asList(EventData.newBuilder().type("et-" + i).build())
        ).join());

        assertTrue("onEvent timeout", eventSignal.await(10, SECONDS));

        range(0, 20).forEach(i -> assertEquals("et-" + i, events.get(i).originalEvent().eventType));

        assertFalse("Subscription was dropped prematurely.", closeSignal.await(0, SECONDS));
        subscription.stop(Duration.ofSeconds(10));
        assertTrue("onClose timeout", closeSignal.await(10, SECONDS));
    }

    @Test
    public void filtersEventsAndKeepListeningToNewOnes() throws InterruptedException, TimeoutException {
        final String stream = generateStreamName();

        List<ResolvedEvent> events = new ArrayList<>();

        CountDownLatch eventSignal = new CountDownLatch(20);
        CountDownLatch closeSignal = new CountDownLatch(1);

        range(0, 20).forEach(i -> eventstore.appendToStream(stream, ExpectedVersion.of(i - 1),
            asList(EventData.newBuilder().type("et-" + i).build())
        ).join());

        CatchUpSubscription subscription = eventstore.subscribeToStreamFrom(stream, 9, false, new CatchUpSubscriptionListener() {
            @Override
            public void onEvent(CatchUpSubscription subscription, ResolvedEvent event) {
                events.add(event);
                eventSignal.countDown();
            }

            @Override
            public void onLiveProcessingStarted(CatchUpSubscription subscription) {
                logger.info("Live processing started.");
            }

            @Override
            public void onClose(CatchUpSubscription subscription, SubscriptionDropReason reason, Exception exception) {
                closeSignal.countDown();
            }
        });

        range(20, 30).forEach(i -> eventstore.appendToStream(stream, ExpectedVersion.of(i - 1),
            asList(EventData.newBuilder().type("et-" + i).build())
        ).join());

        assertTrue("onEvent timeout", eventSignal.await(10, SECONDS));

        assertEquals(20, events.size());
        range(0, 20).forEach(i -> assertEquals("et-" + (i + 10), events.get(i).originalEvent().eventType));

        assertFalse("Subscription was dropped prematurely.", closeSignal.await(0, SECONDS));
        subscription.stop(Duration.ofSeconds(10));
        assertTrue("onClose timeout", closeSignal.await(10, SECONDS));

        assertEquals(events.get(events.size() - 1).originalEventNumber(), subscription.lastProcessedEventNumber());
    }

    @Test
    public void filtersEventsAndWorkIfNothingWasWrittenAfterSubscription() throws InterruptedException, TimeoutException {
        final String stream = generateStreamName();

        List<ResolvedEvent> events = new ArrayList<>();

        CountDownLatch eventSignal = new CountDownLatch(10);
        CountDownLatch closeSignal = new CountDownLatch(1);

        range(0, 20).forEach(i -> eventstore.appendToStream(stream, ExpectedVersion.of(i - 1),
            asList(EventData.newBuilder().type("et-" + i).build())
        ).join());

        CatchUpSubscription subscription = eventstore.subscribeToStreamFrom(stream, 9, false, new CatchUpSubscriptionListener() {
            @Override
            public void onEvent(CatchUpSubscription subscription, ResolvedEvent event) {
                events.add(event);
                eventSignal.countDown();
            }

            @Override
            public void onLiveProcessingStarted(CatchUpSubscription subscription) {
                logger.info("Live processing started.");
            }

            @Override
            public void onClose(CatchUpSubscription subscription, SubscriptionDropReason reason, Exception exception) {
                closeSignal.countDown();
            }
        });

        assertTrue("onEvent timeout", eventSignal.await(10, SECONDS));

        assertEquals(10, events.size());
        range(0, 10).forEach(i -> assertEquals("et-" + (i + 10), events.get(i).originalEvent().eventType));

        assertFalse("Subscription was dropped prematurely.", closeSignal.await(0, SECONDS));
        subscription.stop(Duration.ofSeconds(10));
        assertTrue("onClose timeout", closeSignal.await(10, SECONDS));

        assertEquals(events.get(events.size() - 1).originalEventNumber(), subscription.lastProcessedEventNumber());
    }

}
