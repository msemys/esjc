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
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.IntStream.range;
import static org.junit.Assert.*;

public class ITSubscribeToAllFrom extends AbstractEventStoreTest {
    private static final Logger logger = LoggerFactory.getLogger(ITSubscribeToAllFrom.class);

    public ITSubscribeToAllFrom(EventStore eventstore) {
        super(eventstore);
    }

    @Test
    public void triggersOnCloseCallbackAfterStopMethodCall() throws TimeoutException, InterruptedException {
        CountDownLatch closeSignal = new CountDownLatch(1);

        CatchUpSubscription subscription = eventstore.subscribeToAllFrom(null, new CatchUpSubscriptionListener() {
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
    public void triggersOnCloseCallbackWhenAnErrorOccursWhileProcessingAnEvent() throws TimeoutException, InterruptedException {
        CountDownLatch closeSignal = new CountDownLatch(1);

        eventstore.appendToStream(generateStreamName(), ExpectedVersion.ANY, newTestEvent()).join();

        eventstore.subscribeToAllFrom(null, new CatchUpSubscriptionListener() {
            @Override
            public void onEvent(CatchUpSubscription subscription, ResolvedEvent event) {
                throw new RuntimeException("test");
            }

            @Override
            public void onLiveProcessingStarted(CatchUpSubscription subscription) {
                logger.info("Live processing started.");
            }

            @Override
            public void onClose(CatchUpSubscription subscription, SubscriptionDropReason reason, Exception exception) {
                if (exception != null && "test".equals(exception.getMessage())) {
                    closeSignal.countDown();
                }
            }
        });

        assertTrue("onClose timeout", closeSignal.await(10, SECONDS));
    }

    @Test
    public void subscribesToAllStream() throws InterruptedException, TimeoutException {
        CountDownLatch eventSignal = new CountDownLatch(1);
        CountDownLatch liveSignal = new CountDownLatch(1);
        CountDownLatch closeSignal = new CountDownLatch(1);

        CatchUpSubscription subscription = eventstore.subscribeToAllFrom(null, new CatchUpSubscriptionListener() {
            @Override
            public void onEvent(CatchUpSubscription subscription, ResolvedEvent event) {
                if (liveSignal.getCount() == 0) {
                    eventSignal.countDown();
                }
            }

            @Override
            public void onLiveProcessingStarted(CatchUpSubscription subscription) {
                logger.info("Live processing started.");
                liveSignal.countDown();
            }

            @Override
            public void onClose(CatchUpSubscription subscription, SubscriptionDropReason reason, Exception exception) {
                closeSignal.countDown();
            }
        });

        assertTrue("onLiveProcessingStarted timeout", liveSignal.await(10, SECONDS)); // give time for first pull phase

        eventstore.subscribeToAll(false, (s, e) -> {
        }).join();

        sleepUninterruptibly(100);

        assertFalse("Some event appeared.", eventSignal.await(0, SECONDS));

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

        AllEventsSlice slice = eventstore.readAllEventsBackward(Position.END, 1, false).join();
        Position lastPosition = slice.events.get(0).originalPosition;

        range(0, 10).forEach(i -> eventstore.appendToStream(stream + "-" + i, ExpectedVersion.NO_STREAM,
            EventData.newBuilder().type("et-" + i).build()
        ).join());

        CatchUpSubscription subscription = eventstore.subscribeToAllFrom(null, new CatchUpSubscriptionListener() {
            @Override
            public void onEvent(CatchUpSubscription subscription, ResolvedEvent event) {
                if (event.originalPosition.compareTo(lastPosition) > 0) {
                    events.add(event);
                    eventSignal.countDown();
                }
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

        range(10, 20).forEach(i -> eventstore.appendToStream(stream + "-" + i, ExpectedVersion.NO_STREAM,
            EventData.newBuilder().type("et-" + i).build()
        ).join());

        assertTrue("onEvent timeout", eventSignal.await(10, SECONDS));

        assertEquals(20, events.size());
        range(0, 20).forEach(i -> assertEquals("et-" + i, events.get(i).originalEvent().eventType));

        assertFalse("Subscription was dropped prematurely.", closeSignal.await(0, SECONDS));
        subscription.stop(Duration.ofSeconds(10));
        assertTrue("onClose timeout", closeSignal.await(10, SECONDS));
    }

    @Test
    public void filtersEventsAndKeepListeningToNewOnes() throws InterruptedException, TimeoutException {
        final String stream = generateStreamName();

        List<ResolvedEvent> events = new ArrayList<>();

        CountDownLatch eventSignal = new CountDownLatch(10);
        CountDownLatch closeSignal = new CountDownLatch(1);

        range(0, 10).forEach(i -> eventstore.appendToStream(stream + "-" + i, ExpectedVersion.NO_STREAM,
            EventData.newBuilder().type("et-" + i).build()
        ).join());

        AllEventsSlice slice = eventstore.readAllEventsBackward(Position.END, 1, false).join();
        Position lastPosition = slice.events.get(0).originalPosition;

        CatchUpSubscription subscription = eventstore.subscribeToAllFrom(lastPosition, new CatchUpSubscriptionListener() {
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

        range(10, 20).forEach(i -> eventstore.appendToStream(stream + "-" + i, ExpectedVersion.NO_STREAM,
            EventData.newBuilder().type("et-" + i).build()
        ).join());

        assertTrue("onEvent timeout", eventSignal.await(10, SECONDS));

        assertEquals(10, events.size());
        range(0, 10).forEach(i -> assertEquals("et-" + (i + 10), events.get(i).originalEvent().eventType));

        assertFalse("Subscription was dropped prematurely.", closeSignal.await(0, SECONDS));
        subscription.stop(Duration.ofSeconds(10));
        assertTrue("onClose timeout", closeSignal.await(10, SECONDS));

        assertEquals(events.get(events.size() - 1).originalPosition, subscription.lastProcessedPosition());
    }

    @Test
    public void filtersEventsAndWorkIfNothingWasWrittenAfterSubscription() throws InterruptedException, TimeoutException {
        final String stream = generateStreamName();

        List<ResolvedEvent> events = new ArrayList<>();

        CountDownLatch eventSignal = new CountDownLatch(1);
        CountDownLatch closeSignal = new CountDownLatch(1);

        range(0, 10).forEach(i -> eventstore.appendToStream(stream + "-" + i, ExpectedVersion.NO_STREAM,
            EventData.newBuilder().type("et-" + i).build()
        ).join());

        AllEventsSlice slice = eventstore.readAllEventsBackward(Position.END, 2, false).join();
        Position position = slice.events.get(1).originalPosition;

        CatchUpSubscription subscription = eventstore.subscribeToAllFrom(position, new CatchUpSubscriptionListener() {
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

        assertEquals(1, events.size());
        assertEquals("et-9", events.get(0).originalEvent().eventType);

        assertFalse("Subscription was dropped prematurely.", closeSignal.await(0, SECONDS));
        subscription.stop(Duration.ofSeconds(10));
        assertTrue("onClose timeout", closeSignal.await(10, SECONDS));

        assertEquals(events.get(events.size() - 1).originalPosition, subscription.lastProcessedPosition());
    }

}
