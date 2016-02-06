package lt.msemys.esjc;


import com.google.protobuf.ByteString;
import lt.msemys.esjc.event.ClientConnected;
import lt.msemys.esjc.operation.UserCredentials;
import lt.msemys.esjc.proto.EventStoreClientMessages.EventRecord;
import lt.msemys.esjc.util.Strings;
import lt.msemys.esjc.util.concurrent.ResettableLatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static lt.msemys.esjc.util.Preconditions.checkArgument;
import static lt.msemys.esjc.util.Preconditions.checkNotNull;
import static lt.msemys.esjc.util.Strings.defaultIfEmpty;
import static lt.msemys.esjc.util.Strings.isNullOrEmpty;
import static lt.msemys.esjc.util.UUIDConverter.toBytes;

public abstract class CatchUpSubscription {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private static final ResolvedEvent DROP_SUBSCRIPTION_EVENT =
        new ResolvedEvent(lt.msemys.esjc.proto.EventStoreClientMessages.ResolvedEvent.newBuilder()
            .setEvent(EventRecord.newBuilder()
                .setEventId(ByteString.copyFrom(toBytes(new UUID(0, 0))))
                .setEventStreamId("dummy")
                .setEventNumber(-1)
                .setEventType("dummy")
                .setDataContentType(0)
                .setMetadataContentType(0)
                .setData(ByteString.EMPTY)
                .build())
            .setCommitPosition(-1)
            .setPreparePosition(-1)
            .build());

    private static final DropData UNKNOWN_DROP_DATA = new DropData(SubscriptionDropReason.Unknown, new Exception("Drop reason not specified."));

    private final EventStore eventstore;
    public final String streamId;
    private final boolean resolveLinkTos;
    private final UserCredentials userCredentials;
    protected final CatchUpSubscriptionListener listener;
    protected final int readBatchSize;
    protected final int maxPushQueueSize;
    private final Executor executor;

    private final Queue<ResolvedEvent> liveQueue = new ConcurrentLinkedQueue<>();
    private Subscription subscription;
    private final AtomicReference<DropData> dropData = new AtomicReference<>();
    private volatile boolean allowProcessing;
    private final AtomicBoolean isProcessing = new AtomicBoolean();
    protected volatile boolean shouldStop;
    private final AtomicBoolean isDropped = new AtomicBoolean();
    private final ResettableLatch stopped = new ResettableLatch(true);

    private final EventStoreListener reconnectionHook;

    protected CatchUpSubscription(EventStore eventstore,
                                  String streamId,
                                  boolean resolveLinkTos,
                                  CatchUpSubscriptionListener listener,
                                  UserCredentials userCredentials,
                                  int readBatchSize,
                                  int maxPushQueueSize,
                                  Executor executor) {
        checkNotNull(eventstore, "eventstore");
        checkNotNull(listener, "listener");
        checkNotNull(listener, "executor");
        checkArgument(readBatchSize > 0, "readBatchSize should be positive");
        checkArgument(readBatchSize < EventStore.MAX_READ_SIZE, String.format("Read batch size should be less than %d. For larger reads you should page.", EventStore.MAX_READ_SIZE));
        checkArgument(maxPushQueueSize > 0, "maxPushQueueSize should be positive");

        this.eventstore = eventstore;
        this.streamId = isNullOrEmpty(streamId) ? Strings.EMPTY : streamId;
        this.resolveLinkTos = resolveLinkTos;
        this.listener = listener;
        this.userCredentials = userCredentials;
        this.readBatchSize = readBatchSize;
        this.maxPushQueueSize = maxPushQueueSize;
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
                                           Integer lastEventNumber) throws Exception;

    protected abstract void tryProcess(ResolvedEvent event);

    void start() {
        logger.trace("Catch-up Subscription to {}: starting...", streamId());
        runSubscription();
    }

    public void stop(Duration timeout) throws TimeoutException {
        stop();
        logger.trace("Waiting on subscription to stop");
        if (!stopped.await(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
            throw new TimeoutException(String.format("Could not stop %s in time.", getClass().getSimpleName()));
        }
    }

    public void stop() {
        logger.trace("Catch-up Subscription to {}: requesting stop...", streamId());

        logger.trace("Catch-up Subscription to {}: unhooking from connection. Connected.", streamId());
        eventstore.removeListener(reconnectionHook);

        shouldStop = true;
        enqueueSubscriptionDropNotification(SubscriptionDropReason.UserInitiated, null);
    }

    private void onReconnect() {
        logger.trace("Catch-up Subscription to {}: recovering after reconnection.", streamId());

        logger.trace("Catch-up Subscription to {}: unhooking from connection. Connected.", streamId());
        eventstore.removeListener(reconnectionHook);

        runSubscription();
    }

    private void runSubscription() {
        executor.execute(() -> {
            logger.trace("Catch-up Subscription to {}: running...", streamId());

            stopped.reset();

            try {
                if (!shouldStop) {
                    logger.trace("Catch-up Subscription to {}: pulling events...", streamId());
                    readEventsTill(eventstore, resolveLinkTos, userCredentials, null, null);
                }

                if (!shouldStop) {
                    logger.trace("Catch-up Subscription to {}: subscribing...", streamId());

                    SubscriptionListener subscriptionListener = new SubscriptionListener() {
                        @Override
                        public void onEvent(ResolvedEvent event) {
                            logger.trace("Catch-up Subscription to {}: event appeared ({}, {}, {} @ {}).",
                                streamId(), event.originalStreamId(), event.originalEventNumber(),
                                event.originalEvent().eventType, event.originalPosition);

                            if (liveQueue.size() >= maxPushQueueSize) {
                                enqueueSubscriptionDropNotification(SubscriptionDropReason.ProcessingQueueOverflow, null);
                                subscription.unsubscribe();
                            } else {
                                liveQueue.offer(event);
                                if (allowProcessing) {
                                    ensureProcessingPushQueue();
                                }
                            }
                        }

                        @Override
                        public void onClose(SubscriptionDropReason reason, Exception exception) {
                            enqueueSubscriptionDropNotification(reason, exception);
                        }
                    };

                    subscription = isSubscribedToAll() ?
                        eventstore.subscribeToAll(resolveLinkTos, subscriptionListener, userCredentials).get() :
                        eventstore.subscribeToStream(streamId, resolveLinkTos, subscriptionListener, userCredentials).get();

                    logger.trace("Catch-up Subscription to {}: pulling events (if left)...", streamId());
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

            logger.trace("Catch-up Subscription to {}: processing live events...", streamId());
            listener.onLiveProcessingStarted();

            logger.trace("Catch-up Subscription to {}: hooking to connection. Connected", streamId());
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
            logger.trace("Catch-up Subscription to {}: dropping subscription, reason: {}.", streamId(), reason, exception);

            if (subscription != null) {
                subscription.unsubscribe();
            }

            listener.onClose(reason, exception);

            stopped.release();
        }
    }

    public boolean isSubscribedToAll() {
        return isNullOrEmpty(streamId);
    }

    protected String streamId() {
        return defaultIfEmpty(streamId, "<all>");
    }

    private static class DropData {
        private final SubscriptionDropReason reason;
        private final Exception exception;

        private DropData(SubscriptionDropReason reason, Exception exception) {
            this.reason = reason;
            this.exception = exception;
        }
    }

}
