package com.github.msemys.esjc;

import com.github.msemys.esjc.subscription.PersistentSubscriptionChannel;
import com.github.msemys.esjc.subscription.PersistentSubscriptionNakEventAction;
import com.github.msemys.esjc.util.Subscriptions.DropData;
import com.github.msemys.esjc.util.concurrent.ResettableLatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.github.msemys.esjc.util.Preconditions.checkArgument;
import static com.github.msemys.esjc.util.Subscriptions.DROP_SUBSCRIPTION_EVENT;
import static com.github.msemys.esjc.util.Subscriptions.UNKNOWN_DROP_DATA;
import static com.github.msemys.esjc.util.Threads.sleepUninterruptibly;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toCollection;

/**
 * Persistent subscription.
 */
public abstract class PersistentSubscription implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(PersistentSubscription.class);

    private static final int MAX_EVENTS = 2000;

    private final String subscriptionId;
    private final String streamId;
    private final PersistentSubscriptionListener listener;
    private final UserCredentials userCredentials;
    private final boolean autoAck;

    private PersistentSubscriptionChannel subscription;
    private final Queue<ResolvedEvent> queue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean isProcessing = new AtomicBoolean();
    private final AtomicReference<DropData> dropData = new AtomicReference<>();
    private final AtomicBoolean isDropped = new AtomicBoolean();
    private final ResettableLatch stopped = new ResettableLatch(true);
    private final int bufferSize;
    private final Executor executor;

    protected PersistentSubscription(String subscriptionId,
                                     String streamId,
                                     PersistentSubscriptionListener listener,
                                     UserCredentials userCredentials,
                                     int bufferSize,
                                     boolean autoAck,
                                     Executor executor) {
        this.subscriptionId = subscriptionId;
        this.streamId = streamId;
        this.listener = listener;
        this.userCredentials = userCredentials;
        this.bufferSize = bufferSize;
        this.autoAck = autoAck;
        this.executor = executor;
    }

    protected CompletableFuture<PersistentSubscription> start() {
        stopped.reset();

        SubscriptionListener<PersistentSubscriptionChannel> subscriptionListener = new SubscriptionListener<PersistentSubscriptionChannel>() {
            @Override
            public void onEvent(PersistentSubscriptionChannel subscription, ResolvedEvent event) {
                enqueue(event);
            }

            @Override
            public void onClose(PersistentSubscriptionChannel subscription, SubscriptionDropReason reason, Exception exception) {
                enqueueSubscriptionDropNotification(reason, exception);
            }
        };

        return startSubscription(subscriptionId, streamId, bufferSize, subscriptionListener, userCredentials).thenApply(s -> {
            subscription = (PersistentSubscriptionChannel) s;
            return PersistentSubscription.this;
        });
    }

    protected abstract CompletableFuture<Subscription> startSubscription(String subscriptionId,
                                                                         String streamId,
                                                                         int bufferSize,
                                                                         SubscriptionListener<PersistentSubscriptionChannel> listener,
                                                                         UserCredentials userCredentials);

    /**
     * Acknowledge that the specified message have completed processing (this will tell the server it has been processed).
     * <p><b>Note:</b> there is no need to ack a message if you have Auto Ack enabled.</p>
     *
     * @param event the event to acknowledge.
     */
    public void acknowledge(ResolvedEvent event) {
        subscription.notifyEventsProcessed(singletonList(event.originalEvent().eventId));
    }

    /**
     * Acknowledge that the specified messages have completed processing (this will tell the server it has been processed).
     * <p><b>Note:</b> there is no need to ack a message if you have Auto Ack enabled.</p>
     *
     * @param events the events to acknowledge.
     */
    public void acknowledge(List<ResolvedEvent> events) {
        checkArgument(events.size() <= MAX_EVENTS, "events is limited to %d to ack at a time", MAX_EVENTS);
        subscription.notifyEventsProcessed(events.stream()
            .map(e -> e.originalEvent().eventId)
            .collect(toCollection(() -> new ArrayList<>(events.size()))));
    }

    /**
     * Marks that the specified message failed processing. The server will be take action based upon the action parameter.
     *
     * @param event  the event to mark as failed.
     * @param action the action to take.
     * @param reason an error message as to why the failure is occurring.
     */
    public void fail(ResolvedEvent event, PersistentSubscriptionNakEventAction action, String reason) {
        subscription.notifyEventsFailed(singletonList(event.originalEvent().eventId), action, reason);
    }

    /**
     * Marks that the specified messages have failed processing. The server will take action based upon the action parameter.
     *
     * @param events the events to mark as failed.
     * @param action the action to take.
     * @param reason an error message as to why the failure is occurring.
     */
    public void fail(List<ResolvedEvent> events, PersistentSubscriptionNakEventAction action, String reason) {
        checkArgument(events.size() <= MAX_EVENTS, "events is limited to %d to ack at a time", MAX_EVENTS);
        subscription.notifyEventsFailed(events.stream()
                .map(e -> e.originalEvent().eventId)
                .collect(toCollection(() -> new ArrayList<>(events.size()))),
            action,
            reason);
    }

    /**
     * Unsubscribes from the persistent subscription.
     *
     * @param timeout the maximum wait time before it should timeout.
     * @throws TimeoutException when timeouts
     */
    public void stop(Duration timeout) throws TimeoutException {
        logger.trace("Persistent subscription to {}: requesting stop...", streamId);
        enqueueSubscriptionDropNotification(SubscriptionDropReason.UserInitiated, null);
        if (!stopped.await(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
            throw new TimeoutException(String.format("Could not stop %s in time.", getClass().getSimpleName()));
        }
    }

    /**
     * Unsubscribes from the persistent subscription (using 2 seconds wait time before it should timeout).
     *
     * @throws TimeoutException when timeouts
     * @see #stop(Duration)
     */
    @Override
    public void close() throws TimeoutException {
        stop(Duration.ofSeconds(2));
    }

    private void enqueueSubscriptionDropNotification(SubscriptionDropReason reason, Exception exception) {
        // if drop data was already set -- no need to enqueue drop again, somebody did that already
        if (dropData.compareAndSet(null, new DropData(reason, exception))) {
            enqueue(DROP_SUBSCRIPTION_EVENT);
        }
    }

    private void enqueue(ResolvedEvent event) {
        queue.offer(event);
        if (isProcessing.compareAndSet(false, true)) {
            executor.execute(this::processQueue);
        }
    }

    private void processQueue() {
        do {
            if (subscription == null) {
                sleepUninterruptibly(1);
            } else {
                ResolvedEvent event;
                while ((event = queue.poll()) != null) {
                    // drop subscription artificial ResolvedEvent
                    if (event.equals(DROP_SUBSCRIPTION_EVENT)) {
                        DropData previousDropData = dropData.getAndAccumulate(UNKNOWN_DROP_DATA,
                            (current, update) -> (current == null) ? update : current);

                        if (previousDropData == null) {
                            previousDropData = UNKNOWN_DROP_DATA;
                        }

                        dropSubscription(previousDropData.reason, previousDropData.exception);
                        return;
                    }

                    DropData currentDropData = dropData.get();
                    if (currentDropData != null) {
                        dropSubscription(currentDropData.reason, currentDropData.exception);
                        return;
                    }

                    try {
                        listener.onEvent(this, event);

                        if (autoAck) {
                            subscription.notifyEventsProcessed(singletonList(event.originalEvent().eventId));
                        }

                        logger.trace("Persistent subscription to {}: processed event ({}, {}, {} @ {}).", streamId,
                            event.originalEvent().eventStreamId, event.originalEvent().eventNumber,
                            event.originalEvent().eventType, event.originalEventNumber());
                    } catch (Exception e) {
                        dropSubscription(SubscriptionDropReason.EventHandlerException, e);
                        return;
                    }
                }
            }
            isProcessing.compareAndSet(true, false);
        } while (!queue.isEmpty() && isProcessing.compareAndSet(false, true));
    }

    private void dropSubscription(SubscriptionDropReason reason, Exception exception) {
        if (isDropped.compareAndSet(false, true)) {
            logger.trace("Persistent subscription to {}: dropping subscription, reason: {}", streamId, reason, exception);

            if (subscription != null) {
                subscription.unsubscribe();
            }

            listener.onClose(this, reason, exception);

            stopped.release();
        }
    }

}
