package com.github.msemys.esjc.projection;

import com.github.msemys.esjc.UserCredentials;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import static com.github.msemys.esjc.util.Preconditions.checkNotNull;

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
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionNotFoundException},
     * {@link ProjectionConflictException} or {@link ProjectionException} on exceptional completion.
     * In case of successful completion, the future's methods {@code get} and {@code join} returns {@code null}.
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
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionNotFoundException},
     * {@link ProjectionConflictException} or {@link ProjectionException} on exceptional completion.
     * In case of successful completion, the future's methods {@code get} and {@code join} returns {@code null}.
     */
    CompletableFuture<Void> enable(String name, UserCredentials userCredentials);

    /**
     * Disables a projection using default user credentials.
     *
     * @param name the name of the projection.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionNotFoundException},
     * {@link ProjectionConflictException} or {@link ProjectionException} on exceptional completion.
     * In case of successful completion, the future's methods {@code get} and {@code join} returns {@code null}.
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
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionNotFoundException},
     * {@link ProjectionConflictException} or {@link ProjectionException} on exceptional completion.
     * In case of successful completion, the future's methods {@code get} and {@code join} returns {@code null}.
     */
    CompletableFuture<Void> disable(String name, UserCredentials userCredentials);

    /**
     * Aborts a projection using default user credentials.
     *
     * @param name the name of the projection.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionNotFoundException},
     * {@link ProjectionConflictException} or {@link ProjectionException} on exceptional completion.
     * In case of successful completion, the future's methods {@code get} and {@code join} returns {@code null}.
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
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionNotFoundException},
     * {@link ProjectionConflictException} or {@link ProjectionException} on exceptional completion.
     * In case of successful completion, the future's methods {@code get} and {@code join} returns {@code null}.
     */
    CompletableFuture<Void> abort(String name, UserCredentials userCredentials);

    /**
     * Resets a projection using default user credentials.
     * <p>
     * <b>Note:</b> this will re-emit events, streams that are written to from the projection will also be soft deleted.
     *
     * @param name the name of the projection.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionNotFoundException},
     * {@link ProjectionConflictException} or {@link ProjectionException} on exceptional completion.
     * In case of successful completion, the future's methods {@code get} and {@code join} returns {@code null}.
     * @see #reset(String, UserCredentials)
     */
    default CompletableFuture<Void> reset(String name) {
        return reset(name, null);
    }

    /**
     * Resets a projection.
     * <p>
     * <b>Note:</b> this will re-emit events, streams that are written to from the projection will also be soft deleted.
     *
     * @param name            the name of the projection.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionNotFoundException},
     * {@link ProjectionConflictException} or {@link ProjectionException} on exceptional completion.
     * In case of successful completion, the future's methods {@code get} and {@code join} returns {@code null}.
     */
    CompletableFuture<Void> reset(String name, UserCredentials userCredentials);

    /**
     * Creates a projection with default settings of the specified mode using default user credentials.
     *
     * @param name  the name of the projection.
     * @param query the JavaScript source code for the query.
     * @param mode  projection mode.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionConflictException}
     * or {@link ProjectionException} on exceptional completion. In case of successful completion,
     * the future's methods {@code get} and {@code join} returns {@code null}.
     * @see #create(String, String, ProjectionMode, UserCredentials)
     */
    default CompletableFuture<Void> create(String name, String query, ProjectionMode mode) {
        return create(name, query, mode, null);
    }

    /**
     * Creates a projection with default settings of the specified mode.
     *
     * @param name            the name of the projection.
     * @param query           the JavaScript source code for the query.
     * @param mode            projection mode.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionConflictException}
     * or {@link ProjectionException} on exceptional completion. In case of successful completion,
     * the future's methods {@code get} and {@code join} returns {@code null}.
     * @see #create(String, String, ProjectionSettings, UserCredentials)
     */
    default CompletableFuture<Void> create(String name, String query, ProjectionMode mode, UserCredentials userCredentials) {
        checkNotNull(mode, "mode is null");

        ProjectionSettings settings;

        switch (mode) {
            case TRANSIENT:
                settings = ProjectionSettings.DEFAULT_TRANSIENT;
                break;
            case ONE_TIME:
                settings = ProjectionSettings.DEFAULT_ONE_TIME;
                break;
            case CONTINUOUS:
                settings = ProjectionSettings.DEFAULT_CONTINUOUS;
                break;
            default:
                throw new IllegalArgumentException("Unsupported projection mode: " + mode);
        }

        return create(name, query, settings, userCredentials);
    }

    /**
     * Creates a projection using default user credentials.
     *
     * @param name     the name of the projection.
     * @param query    the JavaScript source code for the query.
     * @param settings projection settings.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionConflictException}
     * or {@link ProjectionException} on exceptional completion. In case of successful completion,
     * the future's methods {@code get} and {@code join} returns {@code null}.
     * @see #create(String, String, ProjectionSettings, UserCredentials)
     */
    default CompletableFuture<Void> create(String name, String query, ProjectionSettings settings) {
        return create(name, query, settings, null);
    }

    /**
     * Creates a projection.
     *
     * @param name            the name of the projection.
     * @param query           the JavaScript source code for the query.
     * @param settings        projection settings.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionConflictException}
     * or {@link ProjectionException} on exceptional completion. In case of successful completion,
     * the future's methods {@code get} and {@code join} returns {@code null}.
     */
    CompletableFuture<Void> create(String name, String query, ProjectionSettings settings, UserCredentials userCredentials);

    /**
     * Finds all projections using default user credentials.
     *
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionException}
     * on exceptional completion.
     * @see #findAll(UserCredentials)
     */
    default CompletableFuture<List<Projection>> findAll() {
        return findAll(null);
    }

    /**
     * Finds all projections.
     *
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionException}
     * on exceptional completion.
     */
    CompletableFuture<List<Projection>> findAll(UserCredentials userCredentials);

    /**
     * Finds all projections with the specified mode using default user credentials.
     *
     * @param mode projection mode.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionException}
     * on exceptional completion.
     * @see #findByMode(ProjectionMode, UserCredentials)
     */
    default CompletableFuture<List<Projection>> findByMode(ProjectionMode mode) {
        return findByMode(mode, null);
    }

    /**
     * Finds all projections with the specified mode.
     *
     * @param mode            projection mode.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionException}
     * on exceptional completion.
     */
    CompletableFuture<List<Projection>> findByMode(ProjectionMode mode, UserCredentials userCredentials);

    /**
     * Gets the status of a projection using default user credentials.
     *
     * @param name the name of the projection.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionNotFoundException}  or
     * {@link ProjectionException} on exceptional completion.
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
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionNotFoundException} or
     * {@link ProjectionException} on exceptional completion.
     */
    CompletableFuture<Projection> getStatus(String name, UserCredentials userCredentials);

    /**
     * Gets the state of a projection using default user credentials.
     *
     * @param name the name of the projection.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionNotFoundException} or
     * {@link ProjectionException} on exceptional completion.
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
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionNotFoundException} or
     * {@link ProjectionException} on exceptional completion.
     */
    CompletableFuture<String> getState(String name, UserCredentials userCredentials);

    /**
     * Gets the state of a projection for a specified partition using default user credentials.
     *
     * @param name      the name of the projection.
     * @param partition the id of the partition.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionNotFoundException} or
     * {@link ProjectionException} on exceptional completion.
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
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionNotFoundException} or
     * {@link ProjectionException} on exceptional completion.
     */
    CompletableFuture<String> getPartitionState(String name, String partition, UserCredentials userCredentials);

    /**
     * Gets the result of a projection using default user credentials.
     *
     * @param name the name of the projection.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionNotFoundException} or
     * {@link ProjectionException} on exceptional completion.
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
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionNotFoundException} or
     * {@link ProjectionException} on exceptional completion.
     */
    CompletableFuture<String> getResult(String name, UserCredentials userCredentials);

    /**
     * Gets the result of a projection for a specified partition using default user credentials.
     *
     * @param name      the name of the projection.
     * @param partition the id of the partition.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionNotFoundException} or
     * {@link ProjectionException} on exceptional completion.
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
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionNotFoundException} or
     * {@link ProjectionException} on exceptional completion.
     */
    CompletableFuture<String> getPartitionResult(String name, String partition, UserCredentials userCredentials);

    /**
     * Gets the statistics of a projection using default user credentials.
     *
     * @param name the name of the projection.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionNotFoundException} or
     * {@link ProjectionException} on exceptional completion.
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
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionNotFoundException} or
     * {@link ProjectionException} on exceptional completion.
     */
    CompletableFuture<String> getStatistics(String name, UserCredentials userCredentials);

    /**
     * Gets a projection's query using default user credentials.
     *
     * @param name the name of the projection.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionNotFoundException} or
     * {@link ProjectionException} on exceptional completion.
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
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionNotFoundException} or
     * {@link ProjectionException} on exceptional completion.
     */
    CompletableFuture<String> getQuery(String name, UserCredentials userCredentials);

    /**
     * Updates a projection's query using default user credentials.
     *
     * @param name  the name of the projection.
     * @param query the JavaScript source code for the query.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionNotFoundException} or
     * {@link ProjectionException} on exceptional completion. In case of successful completion,
     * the future's methods {@code get} and {@code join} returns {@code null}.
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
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionNotFoundException} or
     * {@link ProjectionException} on exceptional completion. In case of successful completion,
     * the future's methods {@code get} and {@code join} returns {@code null}.
     */
    CompletableFuture<Void> updateQuery(String name, String query, UserCredentials userCredentials);

    /**
     * Deletes a projection without deleting the streams that were emitted by this projection.
     * Default user credentials is used for this operation.
     *
     * @param name the name of the projection.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionNotFoundException} or
     * {@link ProjectionException} on exceptional completion. In case of successful completion,
     * the future's methods {@code get} and {@code join} returns {@code null}.
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
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionNotFoundException} or
     * {@link ProjectionException} on exceptional completion. In case of successful completion,
     * the future's methods {@code get} and {@code join} returns {@code null}.
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
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionNotFoundException} or
     * {@link ProjectionException} on exceptional completion. In case of successful completion,
     * the future's methods {@code get} and {@code join} returns {@code null}.
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
     * {@code get} and {@code join} can throw an exception with cause {@link ProjectionNotFoundException} or
     * {@link ProjectionException} on exceptional completion. In case of successful completion,
     * the future's methods {@code get} and {@code join} returns {@code null}.
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
        return awaitStatus(name, matcher, Duration.ofSeconds(1), timeout);
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
     * Waits for projection status that matches the specified {@code matcher} using default user credentials.
     *
     * @param name     the name of the projection.
     * @param matcher  the matcher to apply to received projection status.
     * @param interval the time interval to pull the projection status.
     * @param timeout  the maximum wait time before it should timeout.
     * @return {@code true} if status that matches the specified {@code matcher} was received before the waiting time elapsed,
     * otherwise {@code false}.
     * @see #awaitStatus(String, Predicate, Duration, Duration, UserCredentials)
     */
    default boolean awaitStatus(String name, Predicate<Projection> matcher, Duration interval, Duration timeout) {
        return awaitStatus(name, matcher, interval, timeout, null);
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
