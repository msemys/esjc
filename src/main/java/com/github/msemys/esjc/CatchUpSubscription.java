package com.github.msemys.esjc;


import com.github.msemys.esjc.event.ClientConnected;
import com.github.msemys.esjc.util.Strings;
import com.github.msemys.esjc.util.Subscriptions.DropData;
import com.github.msemys.esjc.util.concurrent.ResettableLatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.github.msemys.esjc.util.Numbers.isPositive;
import static com.github.msemys.esjc.util.Preconditions.checkArgument;
import static com.github.msemys.esjc.util.Preconditions.checkNotNull;
import static com.github.msemys.esjc.util.Ranges.BATCH_SIZE_RANGE;
import static com.github.msemys.esjc.util.Strings.defaultIfEmpty;
import static com.github.msemys.esjc.util.Strings.isNullOrEmpty;
import static com.github.msemys.esjc.util.Subscriptions.DROP_SUBSCRIPTION_EVENT;
import static com.github.msemys.esjc.util.Subscriptions.UNKNOWN_DROP_DATA;

/**
 * Catch-up subscription.
 */
public abstract class CatchUpSubscription implements AutoCloseable {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * The name of the stream to which the subscription is subscribed (empty if subscribed to $all stream).
     */
    public final String streamId;

    private final EventStore eventstore;
    private final boolean resolveLinkTos;
    private final UserCredentials userCredentials;
    protected final CatchUpSubscriptionListener listener;
    protected final int readBatchSize;
    private final Executor executor;

    private final BlockingQueue<ResolvedEvent> liveQueue;
    private Subscription subscription;
    private final AtomicReference<DropData> dropData = new AtomicReference<>();
    private volatile boolean allowProcessing;
    private final AtomicBoolean isProcessing = new AtomicBoolean();
    protected volatile boolean shouldStop;
    private final AtomicBoolean isDropped = new AtomicBoolean();
    private final ResettableLatch stopped = new ResettableLatch(true);

    private final EventStoreListener reconnectionHook;
    private Duration maxWaitTime;

    protected CatchUpSubscription(EventStore eventstore,
                                  String streamId,
                                  boolean resolveLinkTos,
                                  CatchUpSubscriptionListener listener,
                                  UserCredentials userCredentials,
                                  int readBatchSize,
                                  int maxPushQueueSize,
                                  Duration maxWaitForPushQueue,
                                  Executor executor) {
        checkNotNull(eventstore, "eventstore is null");
        checkNotNull(listener, "listener is null");
        checkNotNull(listener, "executor is null");
        checkArgument(BATCH_SIZE_RANGE.contains(readBatchSize), "readBatchSize is out of range. Allowed range: %s.", BATCH_SIZE_RANGE.toString());
        checkArgument(isPositive(maxPushQueueSize), "maxPushQueueSize should be positive");
        checkArgument(!maxWaitForPushQueue.isNegative(), "maxWaitTime is negative");
        this.maxWaitTime = maxWaitForPushQueue;
        this.liveQueue = new LinkedBlockingQueue<>(maxPushQueueSize);
        this.eventstore = eventstore;
        this.streamId = defaultIfEmpty(streamId, Strings.EMPTY);
        this.resolveLinkTos = resolveLinkTos;
        this.listener = listener;
        this.userCredentials = userCredentials;
        this.readBatchSize = readBatchSize;
        this.executor = executor;

        reconnectionHook = event -> {
            if (event instanceof ClientConnected) {
                onReconnect();
            }
        };
    }

    protected abstract void readEventsTill(EventStore eventstore,
                                           boolean resolveLinkTos,
                                           UserCredentials userCredentials,
                                           Long lastCommitPosition,
                                           Long lastEventNumber) throws Exception;

    protected abstract void tryProcess(ResolvedEvent event);

    void start() {
        logger.trace("Catch-up subscription to {}: starting...", streamId());
        runSubscription();
    }

    /**
     * Unsubscribes from the catch-up subscription.
     *
     * @param timeout the maximum wait time before it should timeout.
     * @throws TimeoutException when timeouts
     */
    public void stop(Duration timeout) throws TimeoutException {
        stop();
        logger.trace("Waiting on subscription to stop");
        if (!stopped.await(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
            throw new TimeoutException(String.format("Could not stop %s in time.", getClass().getSimpleName()));
        }
    }

    /**
     * Unsubscribes from the catch-up subscription asynchronously.
     */
    public void stop() {
        logger.trace("Catch-up subscription to {}: requesting stop...", streamId());

        logger.trace("Catch-up subscription to {}: unhooking from connection. Connected.", streamId());
        eventstore.removeListener(reconnectionHook);

        shouldStop = true;
        enqueueSubscriptionDropNotification(SubscriptionDropReason.UserInitiated, null);
    }

    /**
     * Unsubscribes from the catch-up subscription (using 2 seconds wait time before it should timeout).
     *
     * @throws TimeoutException when timeouts
     * @see #stop(Duration)
     */
    @Override
    public void close() throws TimeoutException {
        stop(Duration.ofSeconds(2));
    }

    private void onReconnect() {
        logger.trace("Catch-up subscription to {}: recovering after reconnection.", streamId());

        logger.trace("Catch-up subscription to {}: unhooking from connection. Connected.", streamId());
        eventstore.removeListener(reconnectionHook);

        runSubscription();
    }

    private void runSubscription() {
        executor.execute(() -> {
            logger.trace("Catch-up subscription to {}: running...", streamId());

            stopped.reset();
            allowProcessing = false;
            isDropped.set(false);
            dropData.set(null);

            try {
                if (!shouldStop) {
                    logger.trace("Catch-up subscription to {}: pulling events...", streamId());
                    readEventsTill(eventstore, resolveLinkTos, userCredentials, null, null);
                }

                if (!shouldStop) {
                    logger.trace("Catch-up subscription to {}: subscribing...", streamId());

                    VolatileSubscriptionListener subscriptionListener = new VolatileSubscriptionListener() {
                        @Override
                        public void onEvent(Subscription s, ResolvedEvent event) {
                            if (dropData.get() == null) {
                                logger.trace("Catch-up subscription to {}: event appeared ({}, {}, {} @ {}).",
                                    streamId(), event.originalStreamId(), event.originalEventNumber(),
                                    event.originalEvent().eventType, event.originalPosition);

                                try {
                                    if (!liveQueue.offer(event, maxWaitTime.toMillis(), TimeUnit.MILLISECONDS)) {
                                        enqueueSubscriptionDropNotification(SubscriptionDropReason.ProcessingQueueOverflow, null);
                                        subscription.unsubscribe();
                                    } else {
                                        if (allowProcessing) {
                                            ensureProcessingPushQueue();
                                        }
                                    }
                                } catch (InterruptedException ex) {
                                    enqueueSubscriptionDropNotification(SubscriptionDropReason.ProcessingQueueOverflow, ex);
                                    throw new RuntimeException(ex);
                                }
                            }
                        }

                        @Override
                        public void onClose(Subscription s, SubscriptionDropReason reason, Exception exception) {
                            enqueueSubscriptionDropNotification(reason, exception);
                        }
                    };

                    subscription = isSubscribedToAll() ?
                        eventstore.subscribeToAll(resolveLinkTos, subscriptionListener, userCredentials).get() :
                        eventstore.subscribeToStream(streamId, resolveLinkTos, subscriptionListener, userCredentials).get();

                    logger.trace("Catch-up subscription to {}: pulling events (if left)...", streamId());
                    readEventsTill(eventstore, resolveLinkTos, userCredentials, subscription.lastCommitPosition, subscription.lastEventNumber);
                }
            } catch (Exception e) {
                dropSubscription(SubscriptionDropReason.CatchUpError, e);
                return;
            }

            if (shouldStop) {
                dropSubscription(SubscriptionDropReason.UserInitiated, null);
                return;
            }

            logger.trace("Catch-up subscription to {}: processing live events...", streamId());
            listener.onLiveProcessingStarted(this);

            logger.trace("Catch-up subscription to {}: hooking to connection. Connected", streamId());
            eventstore.addListener(reconnectionHook);

            allowProcessing = true;
            ensureProcessingPushQueue();
        });
    }

    private void enqueueSubscriptionDropNotification(SubscriptionDropReason reason, Exception exception) {
        // if drop data was already set -- no need to enqueue drop again, somebody did that already
        if (dropData.compareAndSet(null, new DropData(reason, exception))) {
            liveQueue.offer(DROP_SUBSCRIPTION_EVENT);
            if (allowProcessing) {
                ensureProcessingPushQueue();
            }
        }
    }

    private void ensureProcessingPushQueue() {
        if (isProcessing.compareAndSet(false, true)) {
            executor.execute(this::processLiveQueue);
        }
    }

    private void processLiveQueue() {
        do {
            ResolvedEvent event;
            while ((event = liveQueue.poll()) != null) {
                // drop subscription artificial ResolvedEvent
                if (event.equals(DROP_SUBSCRIPTION_EVENT)) {
                    DropData previousDropData = dropData.getAndAccumulate(UNKNOWN_DROP_DATA,
                        (current, update) -> (current == null) ? update : current);

                    if (previousDropData == null) {
                        previousDropData = UNKNOWN_DROP_DATA;
                    }

                    dropSubscription(previousDropData.reason, previousDropData.exception);
                    isProcessing.compareAndSet(true, false);
                    return;
                }

                try {
                    tryProcess(event);
                } catch (Exception e) {
                    dropSubscription(SubscriptionDropReason.EventHandlerException, e);
                    return;
                }
            }
            isProcessing.compareAndSet(true, false);
        } while (!liveQueue.isEmpty() && isProcessing.compareAndSet(false, true));
    }

    private void dropSubscription(SubscriptionDropReason reason, Exception exception) {
        if (isDropped.compareAndSet(false, true)) {
            logger.trace("Catch-up subscription to {}: dropping subscription, reason: {}.", streamId(), reason, exception);

            if (subscription != null) {
                subscription.unsubscribe();
            }

            listener.onClose(this, reason, exception);

            stopped.release();
        }
    }

    /**
     * Determines whether or not this subscription is to $all stream or to a specific stream.
     *
     * @return {@code true} if this subscription is to $all stream, otherwise {@code false}
     */
    public boolean isSubscribedToAll() {
        return isNullOrEmpty(streamId);
    }

    /**
     * The last event number processed on the subscription.
     *
     * @return event number
     */
    public abstract long lastProcessedEventNumber();

    /**
     * The last position processed on the subscription.
     *
     * @return position
     */
    public abstract Position lastProcessedPosition();

    protected String streamId() {
        return defaultIfEmpty(streamId, "<all>");
    }

}
