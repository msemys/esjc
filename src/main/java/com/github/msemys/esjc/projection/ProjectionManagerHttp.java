package com.github.msemys.esjc.projection;

import com.github.msemys.esjc.UserCredentials;
import com.github.msemys.esjc.http.HttpClient;
import com.github.msemys.esjc.util.concurrent.DefaultThreadFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringEncoder;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import static com.github.msemys.esjc.http.HttpClient.newRequest;
import static com.github.msemys.esjc.util.Preconditions.checkArgument;
import static com.github.msemys.esjc.util.Preconditions.checkNotNull;
import static com.github.msemys.esjc.util.Strings.EMPTY;
import static com.github.msemys.esjc.util.Strings.isNullOrEmpty;
import static io.netty.handler.codec.http.HttpHeaders.Values.APPLICATION_JSON;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Projection manager for managing projections in the Event Store, that
 * communicates with server over the RESTful API.
 */
public class ProjectionManagerHttp implements ProjectionManager {
    private static final Logger logger = LoggerFactory.getLogger(ProjectionManagerHttp.class);
    private static final Gson gson = new GsonBuilder().create();

    private final HttpClient client;
    private final UserCredentials userCredentials;
    private final Timer timer = new HashedWheelTimer(new DefaultThreadFactory("es-pm-timer"), 200, MILLISECONDS);

    public ProjectionManagerHttp(HttpClient client) {
        this(client, null);
    }

    public ProjectionManagerHttp(HttpClient client, UserCredentials userCredentials) {
        checkNotNull(client, "client is null");
        this.client = client;
        this.userCredentials = userCredentials;
    }

    @Override
    public CompletableFuture<Void> enable(String name, UserCredentials userCredentials) {
        checkArgument(!isNullOrEmpty(name), "name is null or empty");

        return post(projectionUri(name) + "/command/enable", EMPTY, userCredentials, HttpResponseStatus.OK);
    }

    @Override
    public CompletableFuture<Void> disable(String name, UserCredentials userCredentials) {
        checkArgument(!isNullOrEmpty(name), "name is null or empty");

        return post(projectionUri(name) + "/command/disable", EMPTY, userCredentials, HttpResponseStatus.OK);
    }

    @Override
    public CompletableFuture<Void> abort(String name, UserCredentials userCredentials) {
        checkArgument(!isNullOrEmpty(name), "name is null or empty");

        return post(projectionUri(name) + "/command/abort", EMPTY, userCredentials, HttpResponseStatus.OK);
    }

    @Override
    public CompletableFuture<Void> createOneTime(String query, UserCredentials userCredentials) {
        checkArgument(!isNullOrEmpty(query), "query is null or empty");

        return post("/projections/onetime?type=JS", query, userCredentials, HttpResponseStatus.CREATED);
    }

    @Override
    public CompletableFuture<Void> createTransient(String name, String query, UserCredentials userCredentials) {
        checkArgument(!isNullOrEmpty(name), "name is null or empty");
        checkArgument(!isNullOrEmpty(query), "query is null or empty");

        QueryStringEncoder queryStringEncoder = new QueryStringEncoder("/projections/transient");
        queryStringEncoder.addParam("name", name);
        queryStringEncoder.addParam("type", "JS");

        return post(queryStringEncoder.toString(), query, userCredentials, HttpResponseStatus.CREATED);
    }

    @Override
    public CompletableFuture<Void> createContinuous(String name, String query, boolean trackEmittedStreams, UserCredentials userCredentials) {
        checkArgument(!isNullOrEmpty(name), "name is null or empty");
        checkArgument(!isNullOrEmpty(query), "query is null or empty");

        QueryStringEncoder queryStringEncoder = new QueryStringEncoder("/projections/continuous");
        queryStringEncoder.addParam("name", name);
        queryStringEncoder.addParam("type", "JS");
        queryStringEncoder.addParam("emit", "1");
        queryStringEncoder.addParam("trackemittedstreams", Boolean.toString(trackEmittedStreams));

        return post(queryStringEncoder.toString(), query, userCredentials, HttpResponseStatus.CREATED);
    }

    @Override
    public CompletableFuture<List<Projection>> listAll(UserCredentials userCredentials) {
        return get("/projections/any", userCredentials, HttpResponseStatus.OK).thenApply(ProjectionManagerHttp::asProjectionList);
    }

    @Override
    public CompletableFuture<List<Projection>> listOneTime(UserCredentials userCredentials) {
        return get("/projections/onetime", userCredentials, HttpResponseStatus.OK).thenApply(ProjectionManagerHttp::asProjectionList);
    }

    @Override
    public CompletableFuture<List<Projection>> listContinuous(UserCredentials userCredentials) {
        return get("/projections/continuous", userCredentials, HttpResponseStatus.OK).thenApply(ProjectionManagerHttp::asProjectionList);
    }

    @Override
    public CompletableFuture<Projection> getStatus(String name, UserCredentials userCredentials) {
        checkArgument(!isNullOrEmpty(name), "name is null or empty");

        return get(projectionUri(name), userCredentials, HttpResponseStatus.OK).thenApply(ProjectionManagerHttp::asProjection);
    }

    @Override
    public CompletableFuture<String> getState(String name, UserCredentials userCredentials) {
        checkArgument(!isNullOrEmpty(name), "name is null or empty");

        return get(projectionUri(name) + "/state", userCredentials, HttpResponseStatus.OK);
    }

    @Override
    public CompletableFuture<String> getPartitionState(String name, String partition, UserCredentials userCredentials) {
        checkArgument(!isNullOrEmpty(name), "name is null or empty");
        checkArgument(!isNullOrEmpty(partition), "partition is null or empty");

        return get(projectionUri(name) + "/state?partition=" + partition, userCredentials, HttpResponseStatus.OK);
    }

    @Override
    public CompletableFuture<String> getResult(String name, UserCredentials userCredentials) {
        checkArgument(!isNullOrEmpty(name), "name is null or empty");

        return get(projectionUri(name) + "/result", userCredentials, HttpResponseStatus.OK);
    }

    @Override
    public CompletableFuture<String> getPartitionResult(String name, String partition, UserCredentials userCredentials) {
        checkArgument(!isNullOrEmpty(name), "name is null or empty");
        checkArgument(!isNullOrEmpty(partition), "partition is null or empty");

        return get(projectionUri(name) + "/result?partition=" + partition, userCredentials, HttpResponseStatus.OK);
    }

    @Override
    public CompletableFuture<String> getStatistics(String name, UserCredentials userCredentials) {
        checkArgument(!isNullOrEmpty(name), "name is null or empty");

        return get(projectionUri(name) + "/statistics", userCredentials, HttpResponseStatus.OK);
    }

    @Override
    public CompletableFuture<String> getQuery(String name, UserCredentials userCredentials) {
        checkArgument(!isNullOrEmpty(name), "name is null or empty");

        return get(projectionUri(name) + "/query", userCredentials, HttpResponseStatus.OK);
    }

    @Override
    public CompletableFuture<Void> updateQuery(String name, String query, UserCredentials userCredentials) {
        checkArgument(!isNullOrEmpty(name), "name is null or empty");
        checkArgument(!isNullOrEmpty(query), "query is null or empty");

        return put(projectionUri(name) + "/query?type=JS", query, userCredentials, HttpResponseStatus.OK);
    }

    @Override
    public CompletableFuture<Void> delete(String name, boolean deleteEmittedStreams, UserCredentials userCredentials) {
        checkArgument(!isNullOrEmpty(name), "name is null or empty");

        return delete(projectionUri(name) + "?deleteEmittedStreams=" + Boolean.toString(deleteEmittedStreams), userCredentials, HttpResponseStatus.OK);
    }

    @Override
    public boolean awaitStatus(String name,
                               Predicate<Projection> matcher,
                               Duration interval,
                               Duration timeout,
                               UserCredentials userCredentials) {
        checkArgument(!isNullOrEmpty(name), "name is null or empty");
        checkNotNull(matcher, "matcher is null");
        checkNotNull(interval, "interval is null");
        checkNotNull(timeout, "timeout is null");
        checkArgument(interval.compareTo(timeout) < 0, "interval can not be longer than timeout");

        if (getStatus(name, userCredentials).thenApply(matcher::test).join()) {
            return true;
        } else {
            final CountDownLatch barrier = new CountDownLatch(1);
            final AtomicBoolean waitingTimeElapsed = new AtomicBoolean();
            final AtomicReference<Timeout> scheduledTimeout = new AtomicReference<>();

            final TimerTask timerTask = new TimerTask() {
                @Override
                public void run(Timeout timeout) throws Exception {
                    if (!waitingTimeElapsed.get()) {
                        getStatus(name, userCredentials)
                            .thenApply(matcher::test)
                            .exceptionally(e -> {
                                logger.error("Error occurred while pulling '{}' projection status", name, e);
                                return false;
                            })
                            .thenAccept(matches -> {
                                if (matches) {
                                    barrier.countDown();
                                } else if (!waitingTimeElapsed.get()) {
                                    scheduledTimeout.set(timer.newTimeout(this, interval.toMillis(), MILLISECONDS));
                                }
                            });
                    }
                }
            };

            scheduledTimeout.set(timer.newTimeout(timerTask, interval.toMillis(), MILLISECONDS));

            try {
                return barrier.await(timeout.toMillis(), MILLISECONDS);
            } catch (InterruptedException e) {
                logger.error("Interrupted while waiting '{}' projection status", name, e);
                return false;
            } finally {
                waitingTimeElapsed.set(true);
                scheduledTimeout.get().cancel();
            }
        }
    }

    @Override
    public void shutdown() {
        timer.stop();
        client.close();
    }

    private CompletableFuture<String> get(String uri, UserCredentials userCredentials, HttpResponseStatus expectedStatus) {
        FullHttpRequest request = newRequest(HttpMethod.GET, uri, defaultIfNull(userCredentials));

        return client.send(request).thenApply(response -> {
            if (response.getStatus().code() == expectedStatus.code()) {
                return response.content().toString(UTF_8);
            } else {
                throw new ProjectionException(request, response);
            }
        });
    }

    private CompletableFuture<Void> delete(String uri, UserCredentials userCredentials, HttpResponseStatus expectedStatus) {
        FullHttpRequest request = newRequest(HttpMethod.DELETE, uri, defaultIfNull(userCredentials));

        return client.send(request).thenAccept(response -> {
            if (response.getStatus().code() != expectedStatus.code()) {
                throw new ProjectionException(request, response);
            }
        });
    }

    private CompletableFuture<Void> put(String uri, String content, UserCredentials userCredentials, HttpResponseStatus expectedStatus) {
        FullHttpRequest request = newRequest(HttpMethod.PUT, uri, content, APPLICATION_JSON, defaultIfNull(userCredentials));

        return client.send(request).thenAccept(response -> {
            if (response.getStatus().code() != expectedStatus.code()) {
                throw new ProjectionException(request, response);
            }
        });
    }

    private CompletableFuture<Void> post(String uri, String content, UserCredentials userCredentials, HttpResponseStatus expectedStatus) {
        FullHttpRequest request = newRequest(HttpMethod.POST, uri, content, APPLICATION_JSON, defaultIfNull(userCredentials));

        return client.send(request).thenAccept(response -> {
            if (response.getStatus().code() == HttpResponseStatus.CONFLICT.code()) {
                throw new ProjectionConflictException(request, response);
            } else if (response.getStatus().code() != expectedStatus.code()) {
                throw new ProjectionException(request, response);
            }
        });
    }

    private UserCredentials defaultIfNull(UserCredentials userCredentials) {
        return (userCredentials == null) ? this.userCredentials : userCredentials;
    }

    private static List<Projection> asProjectionList(String json) {
        if (isNullOrEmpty(json)) {
            return emptyList();
        } else {
            Projections projections = gson.fromJson(json, Projections.class);
            return (projections.projections != null) ? projections.projections : emptyList();
        }
    }

    private static Projection asProjection(String json) {
        return (isNullOrEmpty(json)) ? null : gson.fromJson(json, Projection.class);
    }

    private static String projectionUri(String name) {
        return "/projection/" + name.trim();
    }

}
