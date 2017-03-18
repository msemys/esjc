package com.github.msemys.esjc.projection;

import com.github.msemys.esjc.UserCredentials;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

/**
 * Projection manager for managing projections in the Event Store.
 * It is recommended that only one instance per application is created.
 */
public interface ProjectionManager {

    /**
     * Enables a projection using default user credentials.
     *
     * @param name the name of the projection
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionConflictException}
     * or {@link ProjectionException} on exceptional completion. In case of successful completion,
     * the future's methods {@code get} and {@code join} returns {@code null}.
     * @see #enable(String, UserCredentials)
     */
    default CompletableFuture<Void> enable(String name) {
        return enable(name, null);
    }

    /**
     * Enables a projection.
     *
     * @param name            the name of the projection.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionConflictException}
     * or {@link ProjectionException} on exceptional completion. In case of successful completion,
     * the future's methods {@code get} and {@code join} returns {@code null}.
     */
    CompletableFuture<Void> enable(String name, UserCredentials userCredentials);

    /**
     * Disables a projection using default user credentials.
     *
     * @param name the name of the projection.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionConflictException}
     * or {@link ProjectionException} on exceptional completion. In case of successful completion,
     * the future's methods {@code get} and {@code join} returns {@code null}.
     * @see #disable(String, UserCredentials)
     */
    default CompletableFuture<Void> disable(String name) {
        return disable(name, null);
    }

    /**
     * Disables a projection.
     *
     * @param name            the name of the projection.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionConflictException}
     * or {@link ProjectionException} on exceptional completion. In case of successful completion,
     * the future's methods {@code get} and {@code join} returns {@code null}.
     */
    CompletableFuture<Void> disable(String name, UserCredentials userCredentials);

    /**
     * Aborts a projection using default user credentials.
     *
     * @param name the name of the projection.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionConflictException}
     * or {@link ProjectionException} on exceptional completion. In case of successful completion,
     * the future's methods {@code get} and {@code join} returns {@code null}.
     * @see #abort(String, UserCredentials)
     */
    default CompletableFuture<Void> abort(String name) {
        return abort(name, null);
    }

    /**
     * Aborts a projection.
     *
     * @param name            the name of the projection.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionConflictException}
     * or {@link ProjectionException} on exceptional completion. In case of successful completion,
     * the future's methods {@code get} and {@code join} returns {@code null}.
     */
    CompletableFuture<Void> abort(String name, UserCredentials userCredentials);

    /**
     * Creates a one-time projection, that will run until completion and then stops.
     * Default user credentials is used for this operation.
     *
     * @param query the JavaScript source code for the query.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionConflictException}
     * or {@link ProjectionException} on exceptional completion. In case of successful completion,
     * the future's methods {@code get} and {@code join} returns {@code null}.
     * @see #createOneTime(String, String)
     */
    default CompletableFuture<Void> createOneTime(String query) {
        return createOneTime(null, query);
    }

    /**
     * Creates a one-time projection, that will run until completion and then stops.
     *
     * @param query           the JavaScript source code for the query.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionConflictException}
     * or {@link ProjectionException} on exceptional completion. In case of successful completion,
     * the future's methods {@code get} and {@code join} returns {@code null}.
     * @see #createOneTime(String, String, UserCredentials)
     */
    default CompletableFuture<Void> createOneTime(String query, UserCredentials userCredentials) {
        return createOneTime(null, query, userCredentials);
    }

    /**
     * Creates a named one-time projection, that will run until completion and then stops.
     * Default user credentials is used for this operation.
     *
     * @param name  the name of the projection (may be {@code null}).
     * @param query the JavaScript source code for the query.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionConflictException}
     * or {@link ProjectionException} on exceptional completion. In case of successful completion,
     * the future's methods {@code get} and {@code join} returns {@code null}.
     * @see #createOneTime(String, String, UserCredentials)
     */
    default CompletableFuture<Void> createOneTime(String name, String query) {
        return createOneTime(name, query, null);
    }

    /**
     * Creates a named one-time projection, that will run until completion and then stops.
     *
     * @param name            the name of the projection (may be {@code null}).
     * @param query           the JavaScript source code for the query.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionConflictException}
     * or {@link ProjectionException} on exceptional completion. In case of successful completion,
     * the future's methods {@code get} and {@code join} returns {@code null}.
     */
    CompletableFuture<Void> createOneTime(String name, String query, UserCredentials userCredentials);

    /**
     * Creates an ad-hoc projection, that runs until completion and is automatically deleted afterwards.
     * Default user credentials is used for this operation.
     *
     * @param name  the name of the projection.
     * @param query the JavaScript source code for the query.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionConflictException}
     * or {@link ProjectionException} on exceptional completion. In case of successful completion,
     * the future's methods {@code get} and {@code join} returns {@code null}.
     * @see #createTransient(String, String, UserCredentials)
     */
    default CompletableFuture<Void> createTransient(String name, String query) {
        return createTransient(name, query, null);
    }

    /**
     * Creates an ad-hoc projection, that runs until completion and is automatically deleted afterwards.
     *
     * @param name            the name of the projection.
     * @param query           the JavaScript source code for the query.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionConflictException}
     * or {@link ProjectionException} on exceptional completion. In case of successful completion,
     * the future's methods {@code get} and {@code join} returns {@code null}.
     */
    CompletableFuture<Void> createTransient(String name, String query, UserCredentials userCredentials);

    /**
     * Creates a continuous projection (without tracking the streams emitted by this projection),
     * that will continuously run unless disabled or an unrecoverable error has been encountered.
     * Default user credentials is used for this operation.
     *
     * @param name  the name of the projection.
     * @param query the JavaScript source code for the query.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionConflictException}
     * or {@link ProjectionException} on exceptional completion. In case of successful completion,
     * the future's methods {@code get} and {@code join} returns {@code null}.
     * @see #createContinuous(String, String, boolean, UserCredentials)
     */
    default CompletableFuture<Void> createContinuous(String name, String query) {
        return createContinuous(name, query, false, null);
    }

    /**
     * Creates a continuous projection (without tracking the streams emitted by this projection),
     * that will continuously run unless disabled or an unrecoverable error has been encountered.
     *
     * @param name            the name of the projection.
     * @param query           the JavaScript source code for the query.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionConflictException}
     * or {@link ProjectionException} on exceptional completion. In case of successful completion,
     * the future's methods {@code get} and {@code join} returns {@code null}.
     * @see #createContinuous(String, String, boolean, UserCredentials)
     */
    default CompletableFuture<Void> createContinuous(String name, String query, UserCredentials userCredentials) {
        return createContinuous(name, query, false, userCredentials);
    }

    /**
     * Creates a continuous projection, that will continuously run unless disabled or an unrecoverable error has been encountered.
     * Default user credentials is used for this operation.
     *
     * @param name                the name of the projection.
     * @param query               the JavaScript source code for the query.
     * @param trackEmittedStreams whether the streams emitted by this projection should be tracked.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionConflictException}
     * or {@link ProjectionException} on exceptional completion. In case of successful completion,
     * the future's methods {@code get} and {@code join} returns {@code null}.
     * @see #createContinuous(String, String, boolean, UserCredentials)
     */
    default CompletableFuture<Void> createContinuous(String name, String query, boolean trackEmittedStreams) {
        return createContinuous(name, query, trackEmittedStreams, null);
    }

    /**
     * Creates a continuous projection, that will continuously run unless disabled or an unrecoverable error has been encountered.
     *
     * @param name                the name of the projection.
     * @param query               the JavaScript source code for the query.
     * @param trackEmittedStreams whether the streams emitted by this projection should be tracked.
     * @param userCredentials     user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionConflictException}
     * or {@link ProjectionException} on exceptional completion. In case of successful completion,
     * the future's methods {@code get} and {@code join} returns {@code null}.
     */
    CompletableFuture<Void> createContinuous(String name, String query, boolean trackEmittedStreams, UserCredentials userCredentials);

    /**
     * Gets all projections using default user credentials.
     *
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionException}
     * on exceptional completion.
     * @see #listAll(UserCredentials)
     */
    default CompletableFuture<List<Projection>> listAll() {
        return listAll(null);
    }

    /**
     * Gets all projections.
     *
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionException}
     * on exceptional completion.
     */
    CompletableFuture<List<Projection>> listAll(UserCredentials userCredentials);

    /**
     * Gets all one-time projections using default user credentials.
     *
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionException}
     * on exceptional completion.
     * @see #listOneTime(UserCredentials)
     */
    default CompletableFuture<List<Projection>> listOneTime() {
        return listOneTime(null);
    }

    /**
     * Gets all one-time projections.
     *
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionException}
     * on exceptional completion.
     */
    CompletableFuture<List<Projection>> listOneTime(UserCredentials userCredentials);

    /**
     * Gets all continuous projections using default user credentials.
     *
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionException}
     * on exceptional completion.
     * @see #listContinuous(UserCredentials)
     */
    default CompletableFuture<List<Projection>> listContinuous() {
        return listContinuous(null);
    }

    /**
     * Gets all continuous projections.
     *
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionException}
     * on exceptional completion.
     */
    CompletableFuture<List<Projection>> listContinuous(UserCredentials userCredentials);

    /**
     * Gets the status of a projection using default user credentials.
     *
     * @param name the name of the projection.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionException}
     * on exceptional completion.
     * @see #getStatus(String, UserCredentials)
     */
    default CompletableFuture<Projection> getStatus(String name) {
        return getStatus(name, null);
    }

    /**
     * Gets the status of a projection.
     *
     * @param name            the name of the projection.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionException}
     * on exceptional completion.
     */
    CompletableFuture<Projection> getStatus(String name, UserCredentials userCredentials);

    /**
     * Gets the state of a projection using default user credentials.
     *
     * @param name the name of the projection.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionException}
     * on exceptional completion.
     * @see #getState(String, UserCredentials)
     */
    default CompletableFuture<String> getState(String name) {
        return getState(name, null);
    }

    /**
     * Gets the state of a projection.
     *
     * @param name            the name of the projection.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionException}
     * on exceptional completion.
     */
    CompletableFuture<String> getState(String name, UserCredentials userCredentials);

    /**
     * Gets the state of a projection for a specified partition using default user credentials.
     *
     * @param name      the name of the projection.
     * @param partition the id of the partition.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionException}
     * on exceptional completion.
     * @see #getPartitionState(String, String, UserCredentials)
     */
    default CompletableFuture<String> getPartitionState(String name, String partition) {
        return getPartitionState(name, partition, null);
    }

    /**
     * Gets the state of a projection for a specified partition.
     *
     * @param name            the name of the projection.
     * @param partition       the id of the partition.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionException}
     * on exceptional completion.
     */
    CompletableFuture<String> getPartitionState(String name, String partition, UserCredentials userCredentials);

    /**
     * Gets the result of a projection using default user credentials.
     *
     * @param name the name of the projection.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionException}
     * on exceptional completion.
     * @see #getResult(String, UserCredentials)
     */
    default CompletableFuture<String> getResult(String name) {
        return getResult(name, null);
    }

    /**
     * Gets the result of a projection.
     *
     * @param name            the name of the projection.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionException}
     * on exceptional completion.
     */
    CompletableFuture<String> getResult(String name, UserCredentials userCredentials);

    /**
     * Gets the result of a projection for a specified partition using default user credentials.
     *
     * @param name      the name of the projection.
     * @param partition the id of the partition.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionException}
     * on exceptional completion.
     * @see #getPartitionResult(String, String, UserCredentials)
     */
    default CompletableFuture<String> getPartitionResult(String name, String partition) {
        return getPartitionResult(name, partition, null);
    }

    /**
     * Gets the result of a projection for a specified partition.
     *
     * @param name            the name of the projection.
     * @param partition       the id of the partition.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionException}
     * on exceptional completion.
     */
    CompletableFuture<String> getPartitionResult(String name, String partition, UserCredentials userCredentials);

    /**
     * Gets the statistics of a projection using default user credentials.
     *
     * @param name the name of the projection.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionException}
     * on exceptional completion.
     * @see #getStatistics(String, UserCredentials)
     */
    default CompletableFuture<String> getStatistics(String name) {
        return getStatistics(name, null);
    }

    /**
     * Gets the statistics of a projection.
     *
     * @param name            the name of the projection.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionException}
     * on exceptional completion.
     */
    CompletableFuture<String> getStatistics(String name, UserCredentials userCredentials);

    /**
     * Gets a projection's query using default user credentials.
     *
     * @param name the name of the projection.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionException}
     * on exceptional completion.
     * @see #getQuery(String, UserCredentials)
     */
    default CompletableFuture<String> getQuery(String name) {
        return getQuery(name, null);
    }

    /**
     * Gets a projection's query.
     *
     * @param name            the name of the projection.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionException}
     * on exceptional completion.
     */
    CompletableFuture<String> getQuery(String name, UserCredentials userCredentials);

    /**
     * Updates a projection's query using default user credentials.
     *
     * @param name  the name of the projection.
     * @param query the JavaScript source code for the query.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionException}
     * on exceptional completion.
     * @see #updateQuery(String, String, UserCredentials)
     */
    default CompletableFuture<Void> updateQuery(String name, String query) {
        return updateQuery(name, query, null);
    }

    /**
     * Updates a projection's query.
     *
     * @param name            the name of the projection.
     * @param query           the JavaScript source code for the query.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionException}
     * on exceptional completion.
     */
    CompletableFuture<Void> updateQuery(String name, String query, UserCredentials userCredentials);

    /**
     * Deletes a projection without deleting the streams that were emitted by this projection.
     * Default user credentials is used for this operation.
     *
     * @param name the name of the projection.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionException}
     * on exceptional completion. In case of successful completion, the future's methods {@code get} and {@code join}
     * returns {@code null}.
     * @see #delete(String, boolean, UserCredentials)
     */
    default CompletableFuture<Void> delete(String name) {
        return delete(name, false, null);
    }

    /**
     * Deletes a projection without deleting the streams that were emitted by this projection.
     *
     * @param name            the name of the projection.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionException}
     * on exceptional completion. In case of successful completion, the future's methods {@code get} and {@code join}
     * returns {@code null}.
     * @see #delete(String, boolean, UserCredentials)
     */
    default CompletableFuture<Void> delete(String name, UserCredentials userCredentials) {
        return delete(name, false, userCredentials);
    }

    /**
     * Deletes a projection using default user credentials.
     *
     * @param name                 the name of the projection.
     * @param deleteEmittedStreams whether to delete the streams that were emitted by this projection.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionException}
     * on exceptional completion. In case of successful completion, the future's methods {@code get} and {@code join}
     * returns {@code null}.
     * @see #delete(String, boolean, UserCredentials)
     */
    default CompletableFuture<Void> delete(String name, boolean deleteEmittedStreams) {
        return delete(name, deleteEmittedStreams, null);
    }

    /**
     * Deletes a projection.
     *
     * @param name                 the name of the projection.
     * @param deleteEmittedStreams whether to delete the streams that were emitted by this projection.
     * @param userCredentials      user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionException}
     * on exceptional completion. In case of successful completion, the future's methods {@code get} and {@code join}
     * returns {@code null}.
     */
    CompletableFuture<Void> delete(String name, boolean deleteEmittedStreams, UserCredentials userCredentials);

    /**
     * Waits for projection status that matches the specified {@code matcher} using 1s interval for status check
     * and default user credentials.
     *
     * @param name    the name of the projection.
     * @param matcher the matcher to apply to received projection status.
     * @param timeout the maximum wait time before it should timeout.
     * @return {@code true} if status that matches the specified {@code matcher} was received before the waiting time elapsed,
     * otherwise {@code false}.
     * @see #awaitStatus(String, Predicate, Duration, Duration, UserCredentials)
     */
    default boolean awaitStatus(String name, Predicate<Projection> matcher, Duration timeout) {
        return awaitStatus(name, matcher, timeout, null);
    }

    /**
     * Waits for projection status that matches the specified {@code matcher} using 1s interval for status check.
     *
     * @param name            the name of the projection.
     * @param matcher         the matcher to apply to received projection status.
     * @param timeout         the maximum wait time before it should timeout.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return {@code true} if status that matches the specified {@code matcher} was received before the waiting time elapsed,
     * otherwise {@code false}.
     * @see #awaitStatus(String, Predicate, Duration, Duration, UserCredentials)
     */
    default boolean awaitStatus(String name, Predicate<Projection> matcher, Duration timeout, UserCredentials userCredentials) {
        return awaitStatus(name, matcher, Duration.ofSeconds(1), timeout, userCredentials);
    }

    /**
     * Waits for projection status that matches the specified {@code matcher}.
     *
     * @param name            the name of the projection.
     * @param matcher         the matcher to apply to received projection status.
     * @param interval        the time interval to pull the projection status.
     * @param timeout         the maximum wait time before it should timeout.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return {@code true} if status that matches the specified {@code matcher} was received before the waiting time elapsed,
     * otherwise {@code false}.
     */
    boolean awaitStatus(String name, Predicate<Projection> matcher, Duration interval, Duration timeout, UserCredentials userCredentials);

    /**
     * Shut down this projection manager.
     */
    void shutdown();

}
