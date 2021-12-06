package com.github.msemys.esjc.subscription;

import com.github.msemys.esjc.*;
import com.github.msemys.esjc.operation.StreamDeletedException;
import com.github.msemys.esjc.operation.StreamNotFoundException;

import java.util.concurrent.Executor;

import static com.github.msemys.esjc.util.Preconditions.checkArgument;
import static com.github.msemys.esjc.util.Strings.isNullOrEmpty;
import static com.github.msemys.esjc.util.Threads.sleepUninterruptibly;

public class StreamCatchUpSubscription extends CatchUpSubscription {
    private long nextReadEventNumber;
    private long lastProcessedEventNumber;

    public StreamCatchUpSubscription(EventStore eventstore,
                                     String streamId,
                                     Long eventNumber,
                                     boolean resolveLinkTos,
                                     CatchUpSubscriptionListener listener,
                                     UserCredentials userCredentials,
                                     int readBatchSize,
                                     int maxPushQueueSize,
                                     boolean resubscribeOnReconnect,
                                     Executor executor) {
        super(eventstore, streamId, resolveLinkTos, listener, userCredentials, readBatchSize, maxPushQueueSize, resubscribeOnReconnect, executor);
        checkArgument(!isNullOrEmpty(streamId), "streamId is null or empty");
        lastProcessedEventNumber = (eventNumber == null) ? StreamPosition.END : eventNumber;
        nextReadEventNumber = (eventNumber == null) ? StreamPosition.START : eventNumber;
    }

    @Override
    protected void readEventsTill(EventStore eventstore,
                                  boolean resolveLinkTos,
                                  UserCredentials userCredentials,
                                  Long lastCommitPosition,
                                  Long lastEventNumber) throws Exception {
        boolean done;

        do {
            StreamEventsSlice slice = eventstore.readStreamEventsForward(streamId, nextReadEventNumber, readBatchSize, resolveLinkTos, userCredentials).get();

            switch (slice.status) {
                case Success:
                    slice.events.forEach(this::tryProcess);
                    nextReadEventNumber = slice.nextEventNumber;
                    done = (lastEventNumber == null) ? slice.isEndOfStream : (slice.nextEventNumber > lastEventNumber);
                    break;
                case StreamNotFound:
                    if (lastEventNumber != null && lastEventNumber != StreamPosition.END) {
                        throw new StreamNotFoundException("Impossible: stream %s disappeared in the middle of catching up subscription.", streamId);
                    }
                    done = true;
                    break;
                case StreamDeleted:
                    throw new StreamDeletedException(streamId);
                default:
                    throw new IllegalStateException(String.format("Unexpected StreamEventsSlice.Status: %s.", slice.status));
            }

            if (!done && slice.isEndOfStream) {
                sleepUninterruptibly(1); // we are waiting for server to flush its data
            }
        } while (!done && !shouldStop);

        logger.trace("Catch-up subscription to {}: finished reading events, nextReadEventNumber = {}.", streamId(), nextReadEventNumber);
    }

    @Override
    protected void tryProcess(ResolvedEvent event) {
        boolean processed = false;

        if (event.originalEventNumber() > lastProcessedEventNumber) {
            listener.onEvent(this, event);
            lastProcessedEventNumber = event.originalEventNumber();
            processed = true;
        }

        logger.trace("Catch-up subscription to {}: {} event ({}, {}, {} @ {}).", streamId(), processed ? "processed" : "skipping",
            event.originalEvent().eventStreamId, event.originalEvent().eventNumber, event.originalEvent().eventType, event.originalEventNumber());
    }

    @Override
    public long lastProcessedEventNumber() {
        return lastProcessedEventNumber;
    }

    @Override
    public Position lastProcessedPosition() {
        throw new UnsupportedOperationException("The last processed position is not available to a specific stream catch-up subscriptions.");
    }
}
