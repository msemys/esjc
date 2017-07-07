package com.github.msemys.esjc;

import com.github.msemys.esjc.operation.*;
import com.github.msemys.esjc.subscription.MaximumSubscribersReachedException;
import com.github.msemys.esjc.subscription.PersistentSubscriptionDeletedException;

import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static com.github.msemys.esjc.util.Preconditions.checkNotNull;
import static com.github.msemys.esjc.util.Strings.toBytes;
import static java.util.Collections.singletonList;

/**
 * An Event Store client with full duplex connection to server.
 * It is recommended that only one instance per application is created.
 */
public interface EventStore {

    /**
     * Gets client settings
     *
     * @return client settings
     */
    Settings settings();

    /**
     * Connects to server asynchronously.
     */
    void connect();

    /**
     * Disconnects client from server.
     */
    void disconnect();

    /**
     * Disconnects client from server and initiates executor services shutdown.
     * That causes client to reject any new operations and it will be not possible
     * to establish connection to server.
     */
    void shutdown();

    /**
     * Deletes a stream from the Event Store asynchronously using soft-deletion mode and default user credentials.
     *
     * @param stream          the name of the stream to delete.
     * @param expectedVersion the expected version that the streams should have when being deleted.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link WrongExpectedVersionException},
     * {@link StreamDeletedException}, {@link InvalidTransactionException}, {@link CommandNotExpectedException},
     * {@link NotAuthenticatedException}, {@link AccessDeniedException} or {@link ServerErrorException}
     * on exceptional completion.
     * @see #deleteStream(String, ExpectedVersion, boolean, UserCredentials)
     */
    default CompletableFuture<DeleteResult> deleteStream(String stream, ExpectedVersion expectedVersion) {
        return deleteStream(stream, expectedVersion, false, null);
    }

    /**
     * Deletes a stream from the Event Store asynchronously using soft-deletion mode.
     *
     * @param stream          the name of the stream to delete.
     * @param expectedVersion the expected version that the streams should have when being deleted.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link WrongExpectedVersionException},
     * {@link StreamDeletedException}, {@link InvalidTransactionException}, {@link CommandNotExpectedException},
     * {@link NotAuthenticatedException}, {@link AccessDeniedException} or {@link ServerErrorException}
     * on exceptional completion.
     * @see #deleteStream(String, ExpectedVersion, boolean, UserCredentials)
     */
    default CompletableFuture<DeleteResult> deleteStream(String stream,
                                                         ExpectedVersion expectedVersion,
                                                         UserCredentials userCredentials) {
        return deleteStream(stream, expectedVersion, false, userCredentials);
    }

    /**
     * Deletes a stream from the Event Store asynchronously using default user credentials.
     * There are two available deletion modes:
     * <ul>
     * <li>hard delete - streams can never be recreated</li>
     * <li>soft delete - streams can be written to again, but the event number sequence will not start from 0</li>
     * </ul>
     *
     * @param stream          the name of the stream to delete.
     * @param expectedVersion the expected version that the streams should have when being deleted.
     * @param hardDelete      use {@code true} for "hard delete" or {@code false} for "soft delete" mode.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link WrongExpectedVersionException},
     * {@link StreamDeletedException}, {@link InvalidTransactionException}, {@link CommandNotExpectedException},
     * {@link NotAuthenticatedException}, {@link AccessDeniedException} or {@link ServerErrorException}
     * on exceptional completion.
     * @see #deleteStream(String, ExpectedVersion, boolean, UserCredentials)
     */
    default CompletableFuture<DeleteResult> deleteStream(String stream,
                                                         ExpectedVersion expectedVersion,
                                                         boolean hardDelete) {
        return deleteStream(stream, expectedVersion, hardDelete, null);
    }

    /**
     * Deletes a stream from the Event Store asynchronously.
     * There are two available deletion modes:
     * <ul>
     * <li>hard delete - streams can never be recreated</li>
     * <li>soft delete - streams can be written to again, but the event number sequence will not start from 0</li>
     * </ul>
     *
     * @param stream          the name of the stream to delete.
     * @param expectedVersion the expected version that the streams should have when being deleted.
     * @param hardDelete      use {@code true} for "hard delete" or {@code false} for "soft delete" mode.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link WrongExpectedVersionException},
     * {@link StreamDeletedException}, {@link InvalidTransactionException}, {@link CommandNotExpectedException},
     * {@link NotAuthenticatedException}, {@link AccessDeniedException} or {@link ServerErrorException}
     * on exceptional completion.
     */
    CompletableFuture<DeleteResult> deleteStream(String stream,
                                                 ExpectedVersion expectedVersion,
                                                 boolean hardDelete,
                                                 UserCredentials userCredentials);

    /**
     * Appends single event to a stream asynchronously using default user credentials.
     *
     * @param stream          the name of the stream to append event to.
     * @param expectedVersion the version at which we currently expect the stream to be,
     *                        in order that an optimistic concurrency check can be performed.
     * @param event           the event to append.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link WrongExpectedVersionException},
     * {@link StreamDeletedException}, {@link InvalidTransactionException}, {@link CommandNotExpectedException},
     * {@link NotAuthenticatedException}, {@link AccessDeniedException} or {@link ServerErrorException}
     * on exceptional completion.
     * @see #appendToStream(String, ExpectedVersion, EventData, UserCredentials)
     */
    default CompletableFuture<WriteResult> appendToStream(String stream,
                                                          ExpectedVersion expectedVersion,
                                                          EventData event) {
        return appendToStream(stream, expectedVersion, event, null);
    }

    /**
     * Appends single event to a stream asynchronously.
     *
     * @param stream          the name of the stream to append event to.
     * @param expectedVersion the version at which we currently expect the stream to be,
     *                        in order that an optimistic concurrency check can be performed.
     * @param event           the event to append.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link WrongExpectedVersionException},
     * {@link StreamDeletedException}, {@link InvalidTransactionException}, {@link CommandNotExpectedException},
     * {@link NotAuthenticatedException}, {@link AccessDeniedException} or {@link ServerErrorException}
     * on exceptional completion.
     */
    default CompletableFuture<WriteResult> appendToStream(String stream,
                                                          ExpectedVersion expectedVersion,
                                                          EventData event,
                                                          UserCredentials userCredentials) {
        return appendToStream(stream, expectedVersion, singletonList(event), userCredentials);
    }

    /**
     * Appends events to a stream asynchronously using default user credentials.
     *
     * @param stream          the name of the stream to append events to.
     * @param expectedVersion the version at which we currently expect the stream to be,
     *                        in order that an optimistic concurrency check can be performed.
     * @param events          the events to append.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link WrongExpectedVersionException},
     * {@link StreamDeletedException}, {@link InvalidTransactionException}, {@link CommandNotExpectedException},
     * {@link NotAuthenticatedException}, {@link AccessDeniedException} or {@link ServerErrorException}
     * on exceptional completion.
     * @see #appendToStream(String, ExpectedVersion, Iterable, UserCredentials)
     */
    default CompletableFuture<WriteResult> appendToStream(String stream,
                                                          ExpectedVersion expectedVersion,
                                                          Iterable<EventData> events) {
        return appendToStream(stream, expectedVersion, events, null);
    }

    /**
     * Appends events to a stream asynchronously.
     *
     * @param stream          the name of the stream to append events to.
     * @param expectedVersion the version at which we currently expect the stream to be,
     *                        in order that an optimistic concurrency check can be performed.
     * @param events          the events to append.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link WrongExpectedVersionException},
     * {@link StreamDeletedException}, {@link InvalidTransactionException}, {@link CommandNotExpectedException},
     * {@link NotAuthenticatedException}, {@link AccessDeniedException} or {@link ServerErrorException}
     * on exceptional completion.
     */
    CompletableFuture<WriteResult> appendToStream(String stream,
                                                  ExpectedVersion expectedVersion,
                                                  Iterable<EventData> events,
                                                  UserCredentials userCredentials);

    /**
     * Appends single event to a stream and returns the status of this operation asynchronously using default user credentials.
     *
     * @param stream          the name of the stream to append event to.
     * @param expectedVersion the version at which we currently expect the stream to be,
     *                        in order that an optimistic concurrency check can be performed.
     * @param event           the event to append.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link InvalidTransactionException},
     * {@link CommandNotExpectedException}, {@link NotAuthenticatedException}, {@link AccessDeniedException} or
     * {@link ServerErrorException} on exceptional completion.
     * @see #tryAppendToStream(String, ExpectedVersion, EventData, UserCredentials)
     */
    default CompletableFuture<WriteAttemptResult> tryAppendToStream(String stream,
                                                                    ExpectedVersion expectedVersion,
                                                                    EventData event) {
        return tryAppendToStream(stream, expectedVersion, event, null);
    }

    /**
     * Appends single event to a stream and returns the status of this operation asynchronously.
     *
     * @param stream          the name of the stream to append event to.
     * @param expectedVersion the version at which we currently expect the stream to be,
     *                        in order that an optimistic concurrency check can be performed.
     * @param event           the event to append.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link InvalidTransactionException},
     * {@link CommandNotExpectedException}, {@link NotAuthenticatedException}, {@link AccessDeniedException} or
     * {@link ServerErrorException} on exceptional completion.
     */
    default CompletableFuture<WriteAttemptResult> tryAppendToStream(String stream,
                                                                    ExpectedVersion expectedVersion,
                                                                    EventData event,
                                                                    UserCredentials userCredentials) {
        return tryAppendToStream(stream, expectedVersion, singletonList(event), userCredentials);
    }

    /**
     * Appends events to a stream and returns the status of this operation asynchronously using default user credentials.
     *
     * @param stream          the name of the stream to append events to.
     * @param expectedVersion the version at which we currently expect the stream to be,
     *                        in order that an optimistic concurrency check can be performed.
     * @param events          the events to append.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link InvalidTransactionException},
     * {@link CommandNotExpectedException}, {@link NotAuthenticatedException}, {@link AccessDeniedException} or
     * {@link ServerErrorException} on exceptional completion.
     * @see #tryAppendToStream(String, ExpectedVersion, Iterable, UserCredentials)
     */
    default CompletableFuture<WriteAttemptResult> tryAppendToStream(String stream,
                                                                    ExpectedVersion expectedVersion,
                                                                    Iterable<EventData> events) {
        return tryAppendToStream(stream, expectedVersion, events, null);
    }

    /**
     * Appends events to a stream and returns the status of this operation asynchronously.
     *
     * @param stream          the name of the stream to append events to.
     * @param expectedVersion the version at which we currently expect the stream to be,
     *                        in order that an optimistic concurrency check can be performed.
     * @param events          the events to append.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link InvalidTransactionException},
     * {@link CommandNotExpectedException}, {@link NotAuthenticatedException}, {@link AccessDeniedException} or
     * {@link ServerErrorException} on exceptional completion.
     */
    CompletableFuture<WriteAttemptResult> tryAppendToStream(String stream,
                                                            ExpectedVersion expectedVersion,
                                                            Iterable<EventData> events,
                                                            UserCredentials userCredentials);

    /**
     * Starts a transaction in the Event Store on a given stream asynchronously using default user credentials.
     *
     * @param stream          the name of the stream to start a transaction on.
     * @param expectedVersion the expected version of the stream at the time of starting the transaction.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link WrongExpectedVersionException},
     * {@link StreamDeletedException}, {@link InvalidTransactionException}, {@link CommandNotExpectedException},
     * {@link NotAuthenticatedException}, {@link AccessDeniedException} or {@link ServerErrorException}
     * on exceptional completion.
     * @see #startTransaction(String, ExpectedVersion, UserCredentials)
     */
    default CompletableFuture<Transaction> startTransaction(String stream, ExpectedVersion expectedVersion) {
        return startTransaction(stream, expectedVersion, null);
    }

    /**
     * Starts a transaction in the Event Store on a given stream asynchronously.
     *
     * @param stream          the name of the stream to start a transaction on.
     * @param expectedVersion the expected version of the stream at the time of starting the transaction.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link WrongExpectedVersionException},
     * {@link StreamDeletedException}, {@link InvalidTransactionException}, {@link CommandNotExpectedException},
     * {@link NotAuthenticatedException}, {@link AccessDeniedException} or {@link ServerErrorException}
     * on exceptional completion.
     */
    CompletableFuture<Transaction> startTransaction(String stream,
                                                    ExpectedVersion expectedVersion,
                                                    UserCredentials userCredentials);

    /**
     * Continues transaction by the specified transaction ID using default user credentials.
     *
     * @param transactionId the transaction ID that needs to be continued.
     * @return transaction
     * @see #continueTransaction(long, UserCredentials)
     */
    default Transaction continueTransaction(long transactionId) {
        return continueTransaction(transactionId, null);
    }

    /**
     * Continues transaction by the specified transaction ID.
     *
     * @param transactionId   the transaction ID that needs to be continued.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return transaction
     */
    Transaction continueTransaction(long transactionId, UserCredentials userCredentials);

    /**
     * Reads a single event from a stream asynchronously using default user credentials.
     *
     * @param stream         the name of the stream to read from.
     * @param eventNumber    the event number to read (use {@link StreamPosition#END} to read the last event in the stream).
     * @param resolveLinkTos whether to resolve link events automatically.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link CommandNotExpectedException},
     * {@link NotAuthenticatedException}, {@link AccessDeniedException} or {@link ServerErrorException}
     * on exceptional completion.
     * @see #readEvent(String, long, boolean, UserCredentials)
     */
    default CompletableFuture<EventReadResult> readEvent(String stream, long eventNumber, boolean resolveLinkTos) {
        return readEvent(stream, eventNumber, resolveLinkTos, null);
    }

    /**
     * Reads a single event from a stream asynchronously.
     *
     * @param stream          the name of the stream to read from.
     * @param eventNumber     the event number to read (use {@link StreamPosition#END} to read the last event in the stream).
     * @param resolveLinkTos  whether to resolve link events automatically.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link CommandNotExpectedException},
     * {@link NotAuthenticatedException}, {@link AccessDeniedException} or {@link ServerErrorException}
     * on exceptional completion.
     */
    CompletableFuture<EventReadResult> readEvent(String stream,
                                                 long eventNumber,
                                                 boolean resolveLinkTos,
                                                 UserCredentials userCredentials);

    /**
     * Reads events from a stream forwards (e.g. oldest to newest) starting from the
     * specified start position asynchronously using default user credentials.
     *
     * @param stream         the name of the stream to read from.
     * @param eventNumber    the event number (inclusive) to read from.
     * @param maxCount       the maximum count of events to read, allowed range [1..4096].
     * @param resolveLinkTos whether to resolve link events automatically.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link CommandNotExpectedException},
     * {@link NotAuthenticatedException}, {@link AccessDeniedException} or {@link ServerErrorException}
     * on exceptional completion.
     * @see #readStreamEventsForward(String, long, int, boolean, UserCredentials)
     */
    default CompletableFuture<StreamEventsSlice> readStreamEventsForward(String stream,
                                                                         long eventNumber,
                                                                         int maxCount,
                                                                         boolean resolveLinkTos) {
        return readStreamEventsForward(stream, eventNumber, maxCount, resolveLinkTos, null);
    }

    /**
     * Reads events from a stream forwards (e.g. oldest to newest) starting from the
     * specified start position asynchronously.
     *
     * @param stream          the name of the stream to read from.
     * @param eventNumber     the event number (inclusive) to read from.
     * @param maxCount        the maximum count of events to read, allowed range [1..4096].
     * @param resolveLinkTos  whether to resolve link events automatically.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link CommandNotExpectedException},
     * {@link NotAuthenticatedException}, {@link AccessDeniedException} or {@link ServerErrorException}
     * on exceptional completion.
     */
    CompletableFuture<StreamEventsSlice> readStreamEventsForward(String stream,
                                                                 long eventNumber,
                                                                 int maxCount,
                                                                 boolean resolveLinkTos,
                                                                 UserCredentials userCredentials);

    /**
     * Reads events from a stream backwards (e.g. newest to oldest) from the
     * specified start position asynchronously using default user credentials.
     *
     * @param stream         the name of the stream to read from.
     * @param eventNumber    the event number (inclusive) to read from.
     * @param maxCount       the maximum count of events to read, allowed range [1..4096].
     * @param resolveLinkTos whether to resolve link events automatically.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link CommandNotExpectedException},
     * {@link NotAuthenticatedException}, {@link AccessDeniedException} or {@link ServerErrorException}
     * on exceptional completion.
     * @see #readStreamEventsBackward(String, long, int, boolean, UserCredentials)
     */
    default CompletableFuture<StreamEventsSlice> readStreamEventsBackward(String stream,
                                                                          long eventNumber,
                                                                          int maxCount,
                                                                          boolean resolveLinkTos) {
        return readStreamEventsBackward(stream, eventNumber, maxCount, resolveLinkTos, null);
    }

    /**
     * Reads events from a stream backwards (e.g. newest to oldest) from the
     * specified start position asynchronously.
     *
     * @param stream          the name of the stream to read from.
     * @param eventNumber     the event number (inclusive) to read from.
     * @param maxCount        the maximum count of events to read, allowed range [1..4096].
     * @param resolveLinkTos  whether to resolve link events automatically.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link CommandNotExpectedException},
     * {@link NotAuthenticatedException}, {@link AccessDeniedException} or {@link ServerErrorException}
     * on exceptional completion.
     */
    CompletableFuture<StreamEventsSlice> readStreamEventsBackward(String stream,
                                                                  long eventNumber,
                                                                  int maxCount,
                                                                  boolean resolveLinkTos,
                                                                  UserCredentials userCredentials);

    /**
     * Reads all events in the node forward (e.g. beginning to end) asynchronously using default user credentials.
     *
     * @param position       the position (inclusive) to start reading from.
     * @param maxCount       the maximum count of events to read, allowed range [1..4096].
     * @param resolveLinkTos whether to resolve link events automatically.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link CommandNotExpectedException},
     * {@link NotAuthenticatedException}, {@link AccessDeniedException} or {@link ServerErrorException}
     * on exceptional completion.
     * @see #readAllEventsForward(Position, int, boolean, UserCredentials)
     */
    default CompletableFuture<AllEventsSlice> readAllEventsForward(Position position,
                                                                   int maxCount,
                                                                   boolean resolveLinkTos) {
        return readAllEventsForward(position, maxCount, resolveLinkTos, null);
    }

    /**
     * Reads all events in the node forward (e.g. beginning to end) asynchronously.
     *
     * @param position        the position (inclusive) to start reading from.
     * @param maxCount        the maximum count of events to read, allowed range [1..4096].
     * @param resolveLinkTos  whether to resolve link events automatically.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link CommandNotExpectedException},
     * {@link NotAuthenticatedException}, {@link AccessDeniedException} or {@link ServerErrorException}
     * on exceptional completion.
     */
    CompletableFuture<AllEventsSlice> readAllEventsForward(Position position,
                                                           int maxCount,
                                                           boolean resolveLinkTos,
                                                           UserCredentials userCredentials);

    /**
     * Reads all events in the node backwards (e.g. end to beginning) asynchronously using default user credentials.
     *
     * @param position       the position (exclusive) to start reading from.
     * @param maxCount       the maximum count of events to read, allowed range [1..4096].
     * @param resolveLinkTos whether to resolve link events automatically.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link CommandNotExpectedException},
     * {@link NotAuthenticatedException}, {@link AccessDeniedException} or {@link ServerErrorException}
     * on exceptional completion.
     * @see #readAllEventsBackward(Position, int, boolean, UserCredentials)
     */
    default CompletableFuture<AllEventsSlice> readAllEventsBackward(Position position,
                                                                    int maxCount,
                                                                    boolean resolveLinkTos) {
        return readAllEventsBackward(position, maxCount, resolveLinkTos, null);
    }

    /**
     * Reads all events in the node backwards (e.g. end to beginning) asynchronously.
     *
     * @param position        the position (exclusive) to start reading from.
     * @param maxCount        the maximum count of events to read, allowed range [1..4096].
     * @param resolveLinkTos  whether to resolve link events automatically.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link CommandNotExpectedException},
     * {@link NotAuthenticatedException}, {@link AccessDeniedException} or {@link ServerErrorException}
     * on exceptional completion.
     */
    CompletableFuture<AllEventsSlice> readAllEventsBackward(Position position,
                                                            int maxCount,
                                                            boolean resolveLinkTos,
                                                            UserCredentials userCredentials);

    /**
     * Iterates over events in a stream from the specified start position to the end of stream using default user credentials.
     * <p>
     * Events are read in batches on demand.
     *
     * @param stream         the name of the stream to iterate.
     * @param eventNumber    the event number (inclusive) to iterate from.
     * @param batchSize      the number of events to return per batch, allowed range [1..4096].
     * @param resolveLinkTos whether to resolve link events automatically.
     * @return an iterator over the events in the stream
     * @see #iterateStreamEventsForward(String, long, int, boolean, UserCredentials)
     */
    default Iterator<ResolvedEvent> iterateStreamEventsForward(String stream,
                                                               long eventNumber,
                                                               int batchSize,
                                                               boolean resolveLinkTos) {
        return iterateStreamEventsForward(stream, eventNumber, batchSize, resolveLinkTos, null);
    }

    /**
     * Iterates over events in a stream from the specified start position to the end of stream.
     * <p>
     * Events are read in batches on demand.
     *
     * @param stream          the name of the stream to iterate.
     * @param eventNumber     the event number (inclusive) to iterate from.
     * @param batchSize       the number of events to return per batch, allowed range [1..4096].
     * @param resolveLinkTos  whether to resolve link events automatically.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return an iterator over the events in the stream
     */
    Iterator<ResolvedEvent> iterateStreamEventsForward(String stream,
                                                       long eventNumber,
                                                       int batchSize,
                                                       boolean resolveLinkTos,
                                                       UserCredentials userCredentials);

    /**
     * Iterates over events in a stream backwards from the specified start position to the beginning of stream using default user credentials.
     * <p>
     * Events are read in batches on demand.
     *
     * @param stream         the name of the stream to iterate.
     * @param eventNumber    the event number (inclusive) to iterate from.
     * @param batchSize      the number of events to return per batch, allowed range [1..4096].
     * @param resolveLinkTos whether to resolve link events automatically.
     * @return an iterator over the events in the stream
     * @see #iterateStreamEventsBackward(String, long, int, boolean, UserCredentials)
     */
    default Iterator<ResolvedEvent> iterateStreamEventsBackward(String stream,
                                                                long eventNumber,
                                                                int batchSize,
                                                                boolean resolveLinkTos) {
        return iterateStreamEventsBackward(stream, eventNumber, batchSize, resolveLinkTos, null);
    }

    /**
     * Iterates over events in a stream backwards from the specified start position to the beginning of stream.
     * <p>
     * Events are read in batches on demand.
     *
     * @param stream          the name of the stream to iterate.
     * @param eventNumber     the event number (inclusive) to iterate from.
     * @param batchSize       the number of events to return per batch, allowed range [1..4096].
     * @param resolveLinkTos  whether to resolve link events automatically.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return an iterator over the events in the stream
     */
    Iterator<ResolvedEvent> iterateStreamEventsBackward(String stream,
                                                        long eventNumber,
                                                        int batchSize,
                                                        boolean resolveLinkTos,
                                                        UserCredentials userCredentials);

    /**
     * Iterates over all events in the node forward from the specified start position to the end using default user credentials.
     * <p>
     * Events are read in batches on demand.
     *
     * @param position       the position (inclusive) to start iterating from.
     * @param batchSize      the number of events to return per batch, allowed range [1..4096].
     * @param resolveLinkTos whether to resolve link events automatically.
     * @return an iterator over the events in the $all stream
     * @see #iterateAllEventsForward(Position, int, boolean, UserCredentials)
     */
    default Iterator<ResolvedEvent> iterateAllEventsForward(Position position,
                                                            int batchSize,
                                                            boolean resolveLinkTos) {
        return iterateAllEventsForward(position, batchSize, resolveLinkTos, null);
    }

    /**
     * Iterates over all events in the node forward from the specified start position to the end.
     * <p>
     * Events are read in batches on demand.
     *
     * @param position        the position (inclusive) to start iterating from.
     * @param batchSize       the number of events to return per batch, allowed range [1..4096].
     * @param resolveLinkTos  whether to resolve link events automatically.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return an iterator over the events in the $all stream
     */
    Iterator<ResolvedEvent> iterateAllEventsForward(Position position,
                                                    int batchSize,
                                                    boolean resolveLinkTos,
                                                    UserCredentials userCredentials);

    /**
     * Iterates over all events in the node backwards from the specified start position to the beginning using default user credentials.
     * <p>
     * Events are read in batches on demand.
     *
     * @param position       the position (exclusive) to start iterating from.
     * @param batchSize      the number of events to return per batch, allowed range [1..4096].
     * @param resolveLinkTos whether to resolve link events automatically.
     * @return an iterator over the events in the $all stream
     * @see #iterateAllEventsBackward(Position, int, boolean, UserCredentials)
     */
    default Iterator<ResolvedEvent> iterateAllEventsBackward(Position position,
                                                             int batchSize,
                                                             boolean resolveLinkTos) {
        return iterateAllEventsBackward(position, batchSize, resolveLinkTos, null);
    }

    /**
     * Iterates over all events in the node backwards from the specified start position to the beginning.
     * <p>
     * Events are read in batches on demand.
     *
     * @param position        the position (exclusive) to start iterating from.
     * @param batchSize       the number of events to return per batch, allowed range [1..4096].
     * @param resolveLinkTos  whether to resolve link events automatically.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return an iterator over the events in the $all stream
     */
    Iterator<ResolvedEvent> iterateAllEventsBackward(Position position,
                                                     int batchSize,
                                                     boolean resolveLinkTos,
                                                     UserCredentials userCredentials);

    /**
     * Sequentially processes events in a stream from the specified start position to the end of stream using default user credentials.
     * <p>
     * Events are read in batches on demand.
     *
     * @param stream         the name of the stream to process.
     * @param eventNumber    the event number (inclusive) to process from.
     * @param batchSize      the number of events to return per batch, allowed range [1..4096].
     * @param resolveLinkTos whether to resolve link events automatically.
     * @return a sequential {@code Stream} over the events in the stream
     * @see #streamEventsForward(String, long, int, boolean, UserCredentials)
     */
    default Stream<ResolvedEvent> streamEventsForward(String stream,
                                                      long eventNumber,
                                                      int batchSize,
                                                      boolean resolveLinkTos) {
        return streamEventsForward(stream, eventNumber, batchSize, resolveLinkTos, null);
    }

    /**
     * Sequentially processes events in a stream from the specified start position to the end of stream.
     * <p>
     * Events are read in batches on demand.
     *
     * @param stream          the name of the stream to process.
     * @param eventNumber     the event number (inclusive) to process from.
     * @param batchSize       the number of events to return per batch, allowed range [1..4096].
     * @param resolveLinkTos  whether to resolve link events automatically.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return a sequential {@code Stream} over the events in the stream
     */
    Stream<ResolvedEvent> streamEventsForward(String stream,
                                              long eventNumber,
                                              int batchSize,
                                              boolean resolveLinkTos,
                                              UserCredentials userCredentials);

    /**
     * Sequentially processes events in a stream backwards from the specified start position to the beginning of stream using default user credentials.
     * <p>
     * Events are read in batches on demand.
     *
     * @param stream         the name of the stream to process.
     * @param eventNumber    the event number (inclusive) to process from.
     * @param batchSize      the number of events to return per batch, allowed range [1..4096].
     * @param resolveLinkTos whether to resolve link events automatically.
     * @return a sequential {@code Stream} over the events in the stream
     * @see #streamEventsBackward(String, long, int, boolean, UserCredentials)
     */
    default Stream<ResolvedEvent> streamEventsBackward(String stream,
                                                       long eventNumber,
                                                       int batchSize,
                                                       boolean resolveLinkTos) {
        return streamEventsBackward(stream, eventNumber, batchSize, resolveLinkTos, null);
    }

    /**
     * Sequentially processes events in a stream backwards from the specified start position to the beginning of stream.
     * <p>
     * Events are read in batches on demand.
     *
     * @param stream          the name of the stream to process.
     * @param eventNumber     the event number (inclusive) to process from.
     * @param batchSize       the number of events to return per batch, allowed range [1..4096].
     * @param resolveLinkTos  whether to resolve link events automatically.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return a sequential {@code Stream} over the events in the stream
     */
    Stream<ResolvedEvent> streamEventsBackward(String stream,
                                               long eventNumber,
                                               int batchSize,
                                               boolean resolveLinkTos,
                                               UserCredentials userCredentials);

    /**
     * Sequentially processes all events in the node forward from the specified start position to the end using default user credentials.
     * <p>
     * Events are read in batches on demand.
     *
     * @param position       the position (inclusive) to start processing from.
     * @param batchSize      the number of events to return per batch, allowed range [1..4096].
     * @param resolveLinkTos whether to resolve link events automatically.
     * @return a sequential {@code Stream} over the events in the $all stream
     * @see #streamAllEventsForward(Position, int, boolean, UserCredentials)
     */
    default Stream<ResolvedEvent> streamAllEventsForward(Position position,
                                                         int batchSize,
                                                         boolean resolveLinkTos) {
        return streamAllEventsForward(position, batchSize, resolveLinkTos, null);
    }

    /**
     * Sequentially processes all events in the node forward from the specified start position to the end.
     * <p>
     * Events are read in batches on demand.
     *
     * @param position        the position (inclusive) to start processing from.
     * @param batchSize       the number of events to return per batch, allowed range [1..4096].
     * @param resolveLinkTos  whether to resolve link events automatically.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return a sequential {@code Stream} over the events in the $all stream
     */
    Stream<ResolvedEvent> streamAllEventsForward(Position position,
                                                 int batchSize,
                                                 boolean resolveLinkTos,
                                                 UserCredentials userCredentials);

    /**
     * Sequentially processes all events in the node backwards from the specified start position to the beginning using default user credentials.
     * <p>
     * Events are read in batches on demand.
     *
     * @param position       the position (exclusive) to start processing from.
     * @param batchSize      the number of events to return per batch, allowed range [1..4096].
     * @param resolveLinkTos whether to resolve link events automatically.
     * @return a sequential {@code Stream} over the events in the $all stream
     * @see #streamAllEventsBackward(Position, int, boolean, UserCredentials)
     */
    default Stream<ResolvedEvent> streamAllEventsBackward(Position position,
                                                          int batchSize,
                                                          boolean resolveLinkTos) {
        return streamAllEventsBackward(position, batchSize, resolveLinkTos, null);
    }

    /**
     * Sequentially processes all events in the node backwards from the specified start position to the beginning.
     * <p>
     * Events are read in batches on demand.
     *
     * @param position        the position (exclusive) to start processing from.
     * @param batchSize       the number of events to return per batch, allowed range [1..4096].
     * @param resolveLinkTos  whether to resolve link events automatically.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return a sequential {@code Stream} over the events in the $all stream
     */
    Stream<ResolvedEvent> streamAllEventsBackward(Position position,
                                                  int batchSize,
                                                  boolean resolveLinkTos,
                                                  UserCredentials userCredentials);

    /**
     * Subscribes to a stream asynchronously using default user credentials. New events written to the stream
     * while the subscription is active will be pushed to the client.
     *
     * @param stream         the name of the stream to subscribe to.
     * @param resolveLinkTos whether to resolve link events automatically.
     * @param listener       subscription listener.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link IllegalArgumentException},
     * {@link CommandNotExpectedException}, {@link NotAuthenticatedException}, {@link AccessDeniedException}
     * or {@link ServerErrorException} on exceptional completion.
     * @see #subscribeToStream(String, boolean, VolatileSubscriptionListener, UserCredentials)
     */
    default CompletableFuture<Subscription> subscribeToStream(String stream,
                                                              boolean resolveLinkTos,
                                                              VolatileSubscriptionListener listener) {
        return subscribeToStream(stream, resolveLinkTos, listener, null);
    }

    /**
     * Subscribes to a stream asynchronously. New events written to the stream while the subscription is active
     * will be pushed to the client.
     *
     * @param stream          the name of the stream to subscribe to.
     * @param resolveLinkTos  whether to resolve link events automatically.
     * @param listener        subscription listener.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link IllegalArgumentException},
     * {@link CommandNotExpectedException}, {@link NotAuthenticatedException}, {@link AccessDeniedException}
     * or {@link ServerErrorException} on exceptional completion.
     */
    CompletableFuture<Subscription> subscribeToStream(String stream,
                                                      boolean resolveLinkTos,
                                                      VolatileSubscriptionListener listener,
                                                      UserCredentials userCredentials);

    /**
     * Subscribes to the $all stream asynchronously using default user credentials. New events written to the stream
     * while the subscription is active will be pushed to the client.
     *
     * @param resolveLinkTos whether to resolve link events automatically.
     * @param listener       subscription listener.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link IllegalArgumentException},
     * {@link CommandNotExpectedException}, {@link NotAuthenticatedException}, {@link AccessDeniedException}
     * or {@link ServerErrorException} on exceptional completion.
     * @see #subscribeToAll(boolean, VolatileSubscriptionListener, UserCredentials)
     */
    default CompletableFuture<Subscription> subscribeToAll(boolean resolveLinkTos,
                                                           VolatileSubscriptionListener listener) {
        return subscribeToAll(resolveLinkTos, listener, null);
    }

    /**
     * Subscribes to the $all stream asynchronously. New events written to the stream while the subscription is active
     * will be pushed to the client.
     *
     * @param resolveLinkTos  whether to resolve link events automatically.
     * @param listener        subscription listener.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link IllegalArgumentException},
     * {@link CommandNotExpectedException}, {@link NotAuthenticatedException}, {@link AccessDeniedException}
     * or {@link ServerErrorException} on exceptional completion.
     */
    CompletableFuture<Subscription> subscribeToAll(boolean resolveLinkTos,
                                                   VolatileSubscriptionListener listener,
                                                   UserCredentials userCredentials);

    /**
     * Subscribes to a stream from the specified event number (exclusive) asynchronously using default user credentials.
     * Existing events from {@code eventNumber} onwards are read from the stream and presented to the user
     * by invoking subscription listener {@code .onEvent()} method as if they had been pushed.
     * Once the end of the stream is read, the subscription is transparently (to the user)
     * switched to push new events as they are written.
     * <p>
     * If events have already been received and resubscription from the same point is desired,
     * use the event number of the last event processed which appeared on the subscription.
     * </p>
     * <p>
     * <b>Note:</b> using {@link StreamPosition#START} for {@code eventNumber} will result in missing
     * the first event in the stream.
     * </p>
     *
     * @param stream      the name of the stream to subscribe to.
     * @param eventNumber the event number (exclusive) from which to start (use {@code null} to receive all events).
     * @param settings    subscription settings.
     * @param listener    subscription listener.
     * @return catch-up subscription
     * @see #subscribeToStreamFrom(String, Long, CatchUpSubscriptionSettings, CatchUpSubscriptionListener, UserCredentials)
     */
    default CatchUpSubscription subscribeToStreamFrom(String stream,
                                                      Long eventNumber,
                                                      CatchUpSubscriptionSettings settings,
                                                      CatchUpSubscriptionListener listener) {
        return subscribeToStreamFrom(stream, eventNumber, settings, listener, null);
    }

    /**
     * Subscribes to a stream from the specified event number (exclusive) asynchronously
     * using default catch-up subscription settings and default user credentials.
     * Existing events from {@code eventNumber} onwards are read from the stream and presented to the user
     * by invoking subscription listener {@code .onEvent()} method as if they had been pushed.
     * Once the end of the stream is read, the subscription is transparently (to the user)
     * switched to push new events as they are written.
     * <p>
     * If events have already been received and resubscription from the same point is desired,
     * use the event number of the last event processed which appeared on the subscription.
     * </p>
     * <p>
     * <b>Note:</b> using {@link StreamPosition#START} for {@code eventNumber} will result in missing
     * the first event in the stream.
     * </p>
     *
     * @param stream      the name of the stream to subscribe to.
     * @param eventNumber the event number (exclusive) from which to start (use {@code null} to receive all events).
     * @param listener    subscription listener.
     * @return catch-up subscription
     * @see #subscribeToStreamFrom(String, Long, CatchUpSubscriptionSettings, CatchUpSubscriptionListener, UserCredentials)
     */
    default CatchUpSubscription subscribeToStreamFrom(String stream,
                                                      Long eventNumber,
                                                      CatchUpSubscriptionListener listener) {
        return subscribeToStreamFrom(stream, eventNumber, CatchUpSubscriptionSettings.DEFAULT, listener, null);
    }

    /**
     * Subscribes to a stream from the specified event number (exclusive) asynchronously using default catch-up subscription settings.
     * Existing events from {@code eventNumber} onwards are read from the stream and presented to the user
     * by invoking subscription listener {@code .onEvent()} method as if they had been pushed.
     * Once the end of the stream is read, the subscription is transparently (to the user)
     * switched to push new events as they are written.
     * <p>
     * If events have already been received and resubscription from the same point is desired,
     * use the event number of the last event processed which appeared on the subscription.
     * </p>
     * <p>
     * <b>Note:</b> using {@link StreamPosition#START} for {@code eventNumber} will result in missing
     * the first event in the stream.
     * </p>
     *
     * @param stream          the name of the stream to subscribe to.
     * @param eventNumber     the event number (exclusive) from which to start (use {@code null} to receive all events).
     * @param listener        subscription listener.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return catch-up subscription
     * @see #subscribeToStreamFrom(String, Long, CatchUpSubscriptionSettings, CatchUpSubscriptionListener, UserCredentials)
     */
    default CatchUpSubscription subscribeToStreamFrom(String stream,
                                                      Long eventNumber,
                                                      CatchUpSubscriptionListener listener,
                                                      UserCredentials userCredentials) {
        return subscribeToStreamFrom(stream, eventNumber, CatchUpSubscriptionSettings.DEFAULT, listener, userCredentials);
    }

    /**
     * Subscribes to a stream from the specified event number (exclusive) asynchronously.
     * Existing events from {@code eventNumber} onwards are read from the stream and presented to the user
     * by invoking subscription listener {@code .onEvent()} method as if they had been pushed.
     * Once the end of the stream is read, the subscription is transparently (to the user)
     * switched to push new events as they are written.
     * <p>
     * If events have already been received and resubscription from the same point is desired,
     * use the event number of the last event processed which appeared on the subscription.
     * </p>
     * <p>
     * <b>Note:</b> using {@link StreamPosition#START} for {@code eventNumber} will result in missing
     * the first event in the stream.
     * </p>
     *
     * @param stream          the name of the stream to subscribe to.
     * @param eventNumber     the event number (exclusive) from which to start (use {@code null} to receive all events).
     * @param settings        subscription settings.
     * @param listener        subscription listener.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return catch-up subscription
     */
    CatchUpSubscription subscribeToStreamFrom(String stream,
                                              Long eventNumber,
                                              CatchUpSubscriptionSettings settings,
                                              CatchUpSubscriptionListener listener,
                                              UserCredentials userCredentials);

    /**
     * Subscribes to the $all stream from the specified event position (exclusive) asynchronously using
     * default catch-up subscription settings and default user credentials.
     * Existing events from {@code position} onwards are read from the stream and presented to the user
     * by invoking subscription listener {@code .onEvent()} method as if they had been pushed.
     * Once the end of the stream is read, the subscription is transparently (to the user)
     * switched to push new events as they are written.
     * <p>
     * If events have already been received and resubscription from the same point is desired,
     * use the position representing the last event processed which appeared on the subscription.
     * </p>
     * <p>
     * <b>Note:</b> using {@link Position#START} for {@code position} will result in missing
     * the first event in the stream.
     * </p>
     *
     * @param position the position (exclusive) from which to start (use {@code null} to receive all events).
     * @param listener subscription listener.
     * @return catch-up subscription
     * @see #subscribeToAllFrom(Position, CatchUpSubscriptionSettings, CatchUpSubscriptionListener, UserCredentials)
     */
    default CatchUpSubscription subscribeToAllFrom(Position position, CatchUpSubscriptionListener listener) {
        return subscribeToAllFrom(position, CatchUpSubscriptionSettings.DEFAULT, listener, null);
    }

    /**
     * Subscribes to the $all stream from the specified event position (exclusive) asynchronously using default user credentials.
     * Existing events from {@code position} onwards are read from the stream and presented to the user
     * by invoking subscription listener {@code .onEvent()} method as if they had been pushed.
     * Once the end of the stream is read, the subscription is transparently (to the user)
     * switched to push new events as they are written.
     * <p>
     * If events have already been received and resubscription from the same point is desired,
     * use the position representing the last event processed which appeared on the subscription.
     * </p>
     * <p>
     * <b>Note:</b> using {@link Position#START} for {@code position} will result in missing
     * the first event in the stream.
     * </p>
     *
     * @param position the position (exclusive) from which to start (use {@code null} to receive all events).
     * @param settings subscription settings.
     * @param listener subscription listener.
     * @return catch-up subscription
     * @see #subscribeToAllFrom(Position, CatchUpSubscriptionSettings, CatchUpSubscriptionListener, UserCredentials)
     */
    default CatchUpSubscription subscribeToAllFrom(Position position,
                                                   CatchUpSubscriptionSettings settings,
                                                   CatchUpSubscriptionListener listener) {
        return subscribeToAllFrom(position, settings, listener, null);
    }

    /**
     * Subscribes to the $all stream from the specified event position (exclusive) asynchronously
     * using default catch-up subscription settings.
     * Existing events from {@code position} onwards are read from the stream and presented to the user
     * by invoking subscription listener {@code .onEvent()} method as if they had been pushed.
     * Once the end of the stream is read, the subscription is transparently (to the user)
     * switched to push new events as they are written.
     * <p>
     * If events have already been received and resubscription from the same point is desired,
     * use the position representing the last event processed which appeared on the subscription.
     * </p>
     * <p>
     * <b>Note:</b> using {@link Position#START} for {@code position} will result in missing
     * the first event in the stream.
     * </p>
     *
     * @param position        the position (exclusive) from which to start (use {@code null} to receive all events).
     * @param listener        subscription listener.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return catch-up subscription
     * @see #subscribeToAllFrom(Position, CatchUpSubscriptionSettings, CatchUpSubscriptionListener, UserCredentials)
     */
    default CatchUpSubscription subscribeToAllFrom(Position position,
                                                   CatchUpSubscriptionListener listener,
                                                   UserCredentials userCredentials) {
        return subscribeToAllFrom(position, CatchUpSubscriptionSettings.DEFAULT, listener, userCredentials);
    }

    /**
     * Subscribes to the $all stream from the specified event position (exclusive) asynchronously.
     * Existing events from {@code position} onwards are read from the stream and presented to the user
     * by invoking subscription listener {@code .onEvent()} method as if they had been pushed.
     * Once the end of the stream is read, the subscription is transparently (to the user)
     * switched to push new events as they are written.
     * <p>
     * If events have already been received and resubscription from the same point is desired,
     * use the position representing the last event processed which appeared on the subscription.
     * </p>
     * <p>
     * <b>Note:</b> using {@link Position#START} for {@code position} will result in missing
     * the first event in the stream.
     * </p>
     *
     * @param position        the position (exclusive) from which to start (use {@code null} to receive all events).
     * @param settings        subscription settings.
     * @param listener        subscription listener.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return catch-up subscription
     */
    CatchUpSubscription subscribeToAllFrom(Position position,
                                           CatchUpSubscriptionSettings settings,
                                           CatchUpSubscriptionListener listener,
                                           UserCredentials userCredentials);

    /**
     * Subscribes to a persistent subscription asynchronously using default buffer size, auto-ack setting and default user credentials.
     * <p>
     * This will connect you to a persistent subscription group for a stream. The subscription group must first be created.
     * Many connections can connect to the same group and they will be treated as competing consumers within the group.
     * If one connection dies, work will be balanced across the rest of the consumers in the group.
     * If you attempt to connect to a group that does not exist you will be given an exception.
     * </p>
     * <p>
     * When auto-ack is disabled, the receiver is required to explicitly acknowledge messages through the subscription.
     * </p>
     *
     * @param stream    the name of the stream to subscribe to.
     * @param groupName the subscription group to connect to.
     * @param listener  subscription listener.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link IllegalArgumentException},
     * {@link PersistentSubscriptionDeletedException}, {@link MaximumSubscribersReachedException},
     * {@link CommandNotExpectedException}, {@link NotAuthenticatedException}, {@link AccessDeniedException}
     * or {@link ServerErrorException} on exceptional completion.
     * @see #subscribeToPersistent(String, String, PersistentSubscriptionListener, UserCredentials, int, boolean)
     */
    default CompletableFuture<PersistentSubscription> subscribeToPersistent(String stream,
                                                                            String groupName,
                                                                            PersistentSubscriptionListener listener) {
        return subscribeToPersistent(stream, groupName, listener, null, settings().persistentSubscriptionBufferSize, settings().persistentSubscriptionAutoAck);
    }

    /**
     * Subscribes to a persistent subscription asynchronously using default buffer size and auto-ack setting.
     * <p>
     * This will connect you to a persistent subscription group for a stream. The subscription group must first be created.
     * Many connections can connect to the same group and they will be treated as competing consumers within the group.
     * If one connection dies, work will be balanced across the rest of the consumers in the group.
     * If you attempt to connect to a group that does not exist you will be given an exception.
     * </p>
     * <p>
     * When auto-ack is disabled, the receiver is required to explicitly acknowledge messages through the subscription.
     * </p>
     *
     * @param stream          the name of the stream to subscribe to.
     * @param groupName       the subscription group to connect to.
     * @param listener        subscription listener.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link IllegalArgumentException},
     * {@link PersistentSubscriptionDeletedException}, {@link MaximumSubscribersReachedException},
     * {@link CommandNotExpectedException}, {@link NotAuthenticatedException}, {@link AccessDeniedException}
     * or {@link ServerErrorException} on exceptional completion.
     * @see #subscribeToPersistent(String, String, PersistentSubscriptionListener, UserCredentials, int, boolean)
     */
    default CompletableFuture<PersistentSubscription> subscribeToPersistent(String stream,
                                                                            String groupName,
                                                                            PersistentSubscriptionListener listener,
                                                                            UserCredentials userCredentials) {
        return subscribeToPersistent(stream, groupName, listener, userCredentials, settings().persistentSubscriptionBufferSize, settings().persistentSubscriptionAutoAck);
    }

    /**
     * Subscribes to a persistent subscription asynchronously using default user credentials.
     * <p>
     * This will connect you to a persistent subscription group for a stream. The subscription group must first be created.
     * Many connections can connect to the same group and they will be treated as competing consumers within the group.
     * If one connection dies, work will be balanced across the rest of the consumers in the group.
     * If you attempt to connect to a group that does not exist you will be given an exception.
     * </p>
     * <p>
     * When auto-ack is disabled, the receiver is required to explicitly acknowledge messages through the subscription.
     * </p>
     *
     * @param stream     the name of the stream to subscribe to.
     * @param groupName  the subscription group to connect to.
     * @param listener   subscription listener.
     * @param bufferSize the buffer size to use for the persistent subscription.
     * @param autoAck    whether the subscription should automatically acknowledge messages processed.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link IllegalArgumentException},
     * {@link PersistentSubscriptionDeletedException}, {@link MaximumSubscribersReachedException},
     * {@link CommandNotExpectedException}, {@link NotAuthenticatedException}, {@link AccessDeniedException}
     * or {@link ServerErrorException} on exceptional completion.
     * @see #subscribeToPersistent(String, String, PersistentSubscriptionListener, UserCredentials, int, boolean)
     */
    default CompletableFuture<PersistentSubscription> subscribeToPersistent(String stream,
                                                                            String groupName,
                                                                            PersistentSubscriptionListener listener,
                                                                            int bufferSize,
                                                                            boolean autoAck) {
        return subscribeToPersistent(stream, groupName, listener, null, bufferSize, autoAck);
    }

    /**
     * Subscribes to a persistent subscription asynchronously.
     * <p>
     * This will connect you to a persistent subscription group for a stream. The subscription group must first be created.
     * Many connections can connect to the same group and they will be treated as competing consumers within the group.
     * If one connection dies, work will be balanced across the rest of the consumers in the group.
     * If you attempt to connect to a group that does not exist you will be given an exception.
     * </p>
     * <p>
     * When auto-ack is disabled, the receiver is required to explicitly acknowledge messages through the subscription.
     * </p>
     *
     * @param stream          the name of the stream to subscribe to.
     * @param groupName       the subscription group to connect to.
     * @param listener        subscription listener.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @param bufferSize      the buffer size to use for the persistent subscription.
     * @param autoAck         whether the subscription should automatically acknowledge messages processed.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link IllegalArgumentException},
     * {@link PersistentSubscriptionDeletedException}, {@link MaximumSubscribersReachedException},
     * {@link CommandNotExpectedException}, {@link NotAuthenticatedException}, {@link AccessDeniedException}
     * or {@link ServerErrorException} on exceptional completion.
     */
    CompletableFuture<PersistentSubscription> subscribeToPersistent(String stream,
                                                                    String groupName,
                                                                    PersistentSubscriptionListener listener,
                                                                    UserCredentials userCredentials,
                                                                    int bufferSize,
                                                                    boolean autoAck);

    /**
     * Creates a persistent subscription group on a stream asynchronously using
     * default persistent subscription settings and default user credentials.
     *
     * @param stream    the name of the stream to create the persistent subscription on.
     * @param groupName the name of the group to create.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link IllegalStateException},
     * {@link CommandNotExpectedException}, {@link NotAuthenticatedException}, {@link AccessDeniedException}
     * or {@link ServerErrorException} on exceptional completion.
     * @see #createPersistentSubscription(String, String, PersistentSubscriptionSettings, UserCredentials)
     */
    default CompletableFuture<PersistentSubscriptionCreateResult> createPersistentSubscription(String stream,
                                                                                               String groupName) {
        return createPersistentSubscription(stream, groupName, PersistentSubscriptionSettings.DEFAULT, null);
    }

    /**
     * Creates a persistent subscription group on a stream asynchronously using default persistent subscription settings.
     *
     * @param stream          the name of the stream to create the persistent subscription on.
     * @param groupName       the name of the group to create.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link IllegalStateException},
     * {@link CommandNotExpectedException}, {@link NotAuthenticatedException}, {@link AccessDeniedException}
     * or {@link ServerErrorException} on exceptional completion.
     * @see #createPersistentSubscription(String, String, PersistentSubscriptionSettings, UserCredentials)
     */
    default CompletableFuture<PersistentSubscriptionCreateResult> createPersistentSubscription(String stream,
                                                                                               String groupName,
                                                                                               UserCredentials userCredentials) {
        return createPersistentSubscription(stream, groupName, PersistentSubscriptionSettings.DEFAULT, userCredentials);
    }

    /**
     * Creates a persistent subscription group on a stream asynchronously using default user credentials.
     *
     * @param stream    the name of the stream to create the persistent subscription on.
     * @param groupName the name of the group to create.
     * @param settings  persistent subscription settings.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link IllegalStateException},
     * {@link CommandNotExpectedException}, {@link NotAuthenticatedException}, {@link AccessDeniedException}
     * or {@link ServerErrorException} on exceptional completion.
     * @see #createPersistentSubscription(String, String, PersistentSubscriptionSettings, UserCredentials)
     */
    default CompletableFuture<PersistentSubscriptionCreateResult> createPersistentSubscription(String stream,
                                                                                               String groupName,
                                                                                               PersistentSubscriptionSettings settings) {
        return createPersistentSubscription(stream, groupName, settings, null);
    }

    /**
     * Creates a persistent subscription on a stream asynchronously.
     *
     * @param stream          the name of the stream to create the persistent subscription on.
     * @param groupName       the name of the group to create.
     * @param settings        persistent subscription settings.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link IllegalStateException},
     * {@link CommandNotExpectedException}, {@link NotAuthenticatedException}, {@link AccessDeniedException}
     * or {@link ServerErrorException} on exceptional completion.
     */
    CompletableFuture<PersistentSubscriptionCreateResult> createPersistentSubscription(String stream,
                                                                                       String groupName,
                                                                                       PersistentSubscriptionSettings settings,
                                                                                       UserCredentials userCredentials);

    /**
     * Updates a persistent subscription on a stream asynchronously using default user credentials.
     *
     * @param stream    the name of the stream to update the persistent subscription on.
     * @param groupName the name of the group to update.
     * @param settings  persistent subscription settings.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link IllegalStateException},
     * {@link CommandNotExpectedException}, {@link NotAuthenticatedException}, {@link AccessDeniedException}
     * or {@link ServerErrorException} on exceptional completion.
     * @see #updatePersistentSubscription(String, String, PersistentSubscriptionSettings, UserCredentials)
     */
    default CompletableFuture<PersistentSubscriptionUpdateResult> updatePersistentSubscription(String stream,
                                                                                               String groupName,
                                                                                               PersistentSubscriptionSettings settings) {
        return updatePersistentSubscription(stream, groupName, settings, null);
    }

    /**
     * Updates a persistent subscription on a stream asynchronously.
     *
     * @param stream          the name of the stream to update the persistent subscription on.
     * @param groupName       the name of the group to update.
     * @param settings        persistent subscription settings.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link IllegalStateException},
     * {@link CommandNotExpectedException}, {@link NotAuthenticatedException}, {@link AccessDeniedException}
     * or {@link ServerErrorException} on exceptional completion.
     */
    CompletableFuture<PersistentSubscriptionUpdateResult> updatePersistentSubscription(String stream,
                                                                                       String groupName,
                                                                                       PersistentSubscriptionSettings settings,
                                                                                       UserCredentials userCredentials);

    /**
     * Deletes a persistent subscription on a stream asynchronously using default user credentials.
     *
     * @param stream    the name of the stream to delete the persistent subscription on.
     * @param groupName the name of the group to delete.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link IllegalStateException},
     * {@link CommandNotExpectedException}, {@link NotAuthenticatedException}, {@link AccessDeniedException}
     * or {@link ServerErrorException} on exceptional completion.
     * @see #deletePersistentSubscription(String, String, UserCredentials)
     */
    default CompletableFuture<PersistentSubscriptionDeleteResult> deletePersistentSubscription(String stream,
                                                                                               String groupName) {
        return deletePersistentSubscription(stream, groupName, null);
    }

    /**
     * Deletes a persistent subscription on a stream asynchronously.
     *
     * @param stream          the name of the stream to delete the persistent subscription on.
     * @param groupName       the name of the group to delete.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link IllegalStateException},
     * {@link CommandNotExpectedException}, {@link NotAuthenticatedException}, {@link AccessDeniedException}
     * or {@link ServerErrorException} on exceptional completion.
     */
    CompletableFuture<PersistentSubscriptionDeleteResult> deletePersistentSubscription(String stream,
                                                                                       String groupName,
                                                                                       UserCredentials userCredentials);

    /**
     * Sets the metadata for a stream asynchronously using default user credentials.
     *
     * @param stream                    the name of the stream for which to set metadata.
     * @param expectedMetastreamVersion the expected version for the write to the metadata stream.
     * @param metadata                  metadata to set.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link WrongExpectedVersionException},
     * {@link StreamDeletedException}, {@link InvalidTransactionException}, {@link CommandNotExpectedException},
     * {@link NotAuthenticatedException}, {@link AccessDeniedException} or {@link ServerErrorException}
     * on exceptional completion.
     * @see #setStreamMetadata(String, ExpectedVersion, byte[], UserCredentials)
     */
    default CompletableFuture<WriteResult> setStreamMetadata(String stream,
                                                             ExpectedVersion expectedMetastreamVersion,
                                                             StreamMetadata metadata) {
        checkNotNull(metadata, "metadata is null");
        return setStreamMetadata(stream, expectedMetastreamVersion, toBytes(metadata.toJson()), null);
    }

    /**
     * Sets the metadata for a stream asynchronously.
     *
     * @param stream                    the name of the stream for which to set metadata.
     * @param expectedMetastreamVersion the expected version for the write to the metadata stream.
     * @param metadata                  metadata to set.
     * @param userCredentials           user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link WrongExpectedVersionException},
     * {@link StreamDeletedException}, {@link InvalidTransactionException}, {@link CommandNotExpectedException},
     * {@link NotAuthenticatedException}, {@link AccessDeniedException} or {@link ServerErrorException}
     * on exceptional completion.
     * @see #setStreamMetadata(String, ExpectedVersion, byte[], UserCredentials)
     */
    default CompletableFuture<WriteResult> setStreamMetadata(String stream,
                                                             ExpectedVersion expectedMetastreamVersion,
                                                             StreamMetadata metadata,
                                                             UserCredentials userCredentials) {
        checkNotNull(metadata, "metadata is null");
        return setStreamMetadata(stream, expectedMetastreamVersion, toBytes(metadata.toJson()), userCredentials);
    }

    /**
     * Sets the metadata for a stream asynchronously using default user credentials.
     *
     * @param stream                    the name of the stream for which to set metadata.
     * @param expectedMetastreamVersion the expected version for the write to the metadata stream.
     * @param metadata                  metadata to set.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link WrongExpectedVersionException},
     * {@link StreamDeletedException}, {@link InvalidTransactionException}, {@link CommandNotExpectedException},
     * {@link NotAuthenticatedException}, {@link AccessDeniedException} or {@link ServerErrorException}
     * on exceptional completion.
     * @see #setStreamMetadata(String, ExpectedVersion, byte[], UserCredentials)
     */
    default CompletableFuture<WriteResult> setStreamMetadata(String stream,
                                                             ExpectedVersion expectedMetastreamVersion,
                                                             byte[] metadata) {
        return setStreamMetadata(stream, expectedMetastreamVersion, metadata, null);
    }

    /**
     * Sets the metadata for a stream asynchronously.
     *
     * @param stream                    the name of the stream for which to set metadata.
     * @param expectedMetastreamVersion the expected version for the write to the metadata stream.
     * @param metadata                  metadata to set.
     * @param userCredentials           user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link WrongExpectedVersionException},
     * {@link StreamDeletedException}, {@link InvalidTransactionException}, {@link CommandNotExpectedException},
     * {@link NotAuthenticatedException}, {@link AccessDeniedException} or {@link ServerErrorException}
     * on exceptional completion.
     */
    CompletableFuture<WriteResult> setStreamMetadata(String stream,
                                                     ExpectedVersion expectedMetastreamVersion,
                                                     byte[] metadata,
                                                     UserCredentials userCredentials);

    /**
     * Gets the metadata for a stream asynchronously using default user credentials.
     *
     * @param stream the name of the stream for which to read metadata.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link CommandNotExpectedException},
     * {@link NotAuthenticatedException}, {@link AccessDeniedException} or {@link ServerErrorException}
     * on exceptional completion.
     * @see #getStreamMetadata(String, UserCredentials)
     */
    default CompletableFuture<StreamMetadataResult> getStreamMetadata(String stream) {
        return getStreamMetadata(stream, null);
    }

    /**
     * Gets the metadata for a stream asynchronously.
     *
     * @param stream          the name of the stream for which to read metadata.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link CommandNotExpectedException},
     * {@link NotAuthenticatedException}, {@link AccessDeniedException} or {@link ServerErrorException}
     * on exceptional completion.
     */
    CompletableFuture<StreamMetadataResult> getStreamMetadata(String stream, UserCredentials userCredentials);

    /**
     * Gets the metadata for a stream as a byte array asynchronously using default user credentials.
     *
     * @param stream the name of the stream for which to read metadata.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link CommandNotExpectedException},
     * {@link NotAuthenticatedException}, {@link AccessDeniedException} or {@link ServerErrorException}
     * on exceptional completion.
     * @see #getStreamMetadataAsRawBytes(String, UserCredentials)
     */
    default CompletableFuture<RawStreamMetadataResult> getStreamMetadataAsRawBytes(String stream) {
        return getStreamMetadataAsRawBytes(stream, null);
    }

    /**
     * Gets the metadata for a stream as a byte array asynchronously.
     *
     * @param stream          the name of the stream for which to read metadata.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link CommandNotExpectedException},
     * {@link NotAuthenticatedException}, {@link AccessDeniedException} or {@link ServerErrorException}
     * on exceptional completion.
     */
    CompletableFuture<RawStreamMetadataResult> getStreamMetadataAsRawBytes(String stream, UserCredentials userCredentials);

    /**
     * Sets the global settings for the server or cluster asynchronously using default user credentials.
     *
     * @param settings system settings to apply.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link WrongExpectedVersionException},
     * {@link StreamDeletedException}, {@link InvalidTransactionException}, {@link CommandNotExpectedException},
     * {@link NotAuthenticatedException}, {@link AccessDeniedException} or {@link ServerErrorException}
     * on exceptional completion.
     * @see #setSystemSettings(SystemSettings, UserCredentials)
     */
    default CompletableFuture<WriteResult> setSystemSettings(SystemSettings settings) {
        return setSystemSettings(settings, null);
    }

    /**
     * Sets the global settings for the server or cluster asynchronously.
     *
     * @param settings        system settings to apply.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link WrongExpectedVersionException},
     * {@link StreamDeletedException}, {@link InvalidTransactionException}, {@link CommandNotExpectedException},
     * {@link NotAuthenticatedException}, {@link AccessDeniedException} or {@link ServerErrorException}
     * on exceptional completion.
     */
    CompletableFuture<WriteResult> setSystemSettings(SystemSettings settings, UserCredentials userCredentials);

    /**
     * Adds the specified listener to this client.
     *
     * @param listener client event listener.
     */
    void addListener(EventStoreListener listener);

    /**
     * Removes the specified listener from this client.
     *
     * @param listener client event listener.
     */
    void removeListener(EventStoreListener listener);

}
