package com.github.msemys.esjc.subscription;

import com.github.msemys.esjc.*;
import com.github.msemys.esjc.UserCredentials;
import com.github.msemys.esjc.util.Strings;

import java.util.concurrent.Executor;

import static com.github.msemys.esjc.util.Threads.sleepUninterruptibly;

public class AllCatchUpSubscription extends CatchUpSubscription {
    private Position nextReadPosition;
    private Position lastProcessedPosition;

    public AllCatchUpSubscription(EventStore eventstore,
                                  Position fromPositionExclusive,
                                  boolean resolveLinkTos,
                                  CatchUpSubscriptionListener listener,
                                  UserCredentials userCredentials,
                                  int readBatchSize,
                                  int maxPushQueueSize,
                                  Executor executor) {
        super(eventstore, Strings.EMPTY, resolveLinkTos, listener, userCredentials, readBatchSize, maxPushQueueSize, executor);
        lastProcessedPosition = (fromPositionExclusive == null) ? Position.END : fromPositionExclusive;
        nextReadPosition = (fromPositionExclusive == null) ? Position.START : fromPositionExclusive;
    }

    @Override
    protected void readEventsTill(EventStore eventstore,
                                  boolean resolveLinkTos,
                                  UserCredentials userCredentials,
                                  Long lastCommitPosition,
                                  Integer lastEventNumber) throws Exception {
        boolean done;

        do {
            AllEventsSlice slice = eventstore.readAllEventsForward(nextReadPosition, readBatchSize, resolveLinkTos, userCredentials).get();

            for (ResolvedEvent e : slice.events) {
                if (e.originalPosition == null) {
                    throw new Exception("Subscription event came up with no OriginalPosition.");
                } else {
                    tryProcess(e);
                }
            }

            nextReadPosition = slice.nextPosition;

            done = (lastCommitPosition == null) ?
                slice.isEndOfStream() : slice.nextPosition.compareTo(new Position(lastCommitPosition, lastCommitPosition)) >= 0;

            if (!done && slice.isEndOfStream()) {
                sleepUninterruptibly(1); // we are waiting for server to flush its data
            }
        } while (!done && !shouldStop);

        logger.trace("Catch-up Subscription to {}: finished reading events, nextReadPosition = {}.", streamId(), nextReadPosition);
    }

    @Override
    protected void tryProcess(ResolvedEvent event) {
        boolean processed = false;

        if (event.originalPosition.compareTo(lastProcessedPosition) > 0) {
            listener.onEvent(event);
            lastProcessedPosition = event.originalPosition;
            processed = true;
        }

        logger.trace("Catch-up Subscription to {}: {} event ({}, {}, {} @ {}).", streamId(), processed ? "processed" : "skipping",
            event.originalEvent().eventStreamId, event.originalEvent().eventNumber, event.originalEvent().eventType, event.originalPosition);
    }

    public Position lastProcessedPosition() {
        Position oldPosition = lastProcessedPosition;
        Position currentPosition;
        while (oldPosition != (currentPosition = lastProcessedPosition)) {
            oldPosition = currentPosition;
        }
        return currentPosition;
    }

}
