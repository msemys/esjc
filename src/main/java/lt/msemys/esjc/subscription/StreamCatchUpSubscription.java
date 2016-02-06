package lt.msemys.esjc.subscription;

import lt.msemys.esjc.*;
import lt.msemys.esjc.operation.StreamDeletedException;
import lt.msemys.esjc.operation.UserCredentials;

import java.util.concurrent.Executor;

import static lt.msemys.esjc.util.Preconditions.checkArgument;
import static lt.msemys.esjc.util.Strings.isNullOrEmpty;
import static lt.msemys.esjc.util.Threads.sleepUninterruptibly;

public class StreamCatchUpSubscription extends CatchUpSubscription {
    private int nextReadEventNumber;
    private int lastProcessedEventNumber;

    public StreamCatchUpSubscription(EventStore eventstore,
                                     String streamId,
                                     Integer fromEventNumberExclusive,
                                     boolean resolveLinkTos,
                                     CatchUpSubscriptionListener listener,
                                     UserCredentials userCredentials,
                                     int readBatchSize,
                                     int maxPushQueueSize,
                                     Executor executor) {
        super(eventstore, streamId, resolveLinkTos, listener, userCredentials, readBatchSize, maxPushQueueSize, executor);
        checkArgument(!isNullOrEmpty(streamId), "streamId");
        lastProcessedEventNumber = (fromEventNumberExclusive == null) ? -1 : fromEventNumberExclusive;
        nextReadEventNumber = (fromEventNumberExclusive == null) ? 0 : fromEventNumberExclusive;
    }

    @Override
    protected void readEventsTill(EventStore eventstore,
                                  boolean resolveLinkTos,
                                  UserCredentials userCredentials,
                                  Long lastCommitPosition,
                                  Integer lastEventNumber) throws Exception {
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
                    if (lastEventNumber != null && lastEventNumber != -1) {
                        throw new Exception(String.format("Impossible: stream %s disappeared in the middle of catching up subscription.", streamId));
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

        logger.trace("Catch-up Subscription to {}: finished reading events, nextReadEventNumber = {}.", streamId(), nextReadEventNumber);
    }

    @Override
    protected void tryProcess(ResolvedEvent event) {
        boolean processed = false;

        if (event.originalEventNumber() > lastProcessedEventNumber) {
            listener.onEvent(event);
            lastProcessedEventNumber = event.originalEventNumber();
            processed = true;
        }

        logger.trace("Catch-up Subscription to {}: {} event ({}, {}, {} @ {}).", streamId(), processed ? "processed" : "skipping",
            event.originalEvent().eventStreamId, event.originalEvent().eventNumber, event.originalEvent().eventType, event.originalEventNumber());
    }

    public int lastProcessedEventNumber() {
        return lastProcessedEventNumber;
    }
}
