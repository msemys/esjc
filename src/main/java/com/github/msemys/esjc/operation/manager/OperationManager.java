package com.github.msemys.esjc.operation.manager;

import com.github.msemys.esjc.ConnectionClosedException;
import com.github.msemys.esjc.Settings;
import com.github.msemys.esjc.tcp.ChannelId;
import com.github.msemys.esjc.tcp.TcpPackage;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.github.msemys.esjc.util.Preconditions.checkNotNull;
import static java.util.stream.Stream.concat;

public class OperationManager {
    private static final Logger logger = LoggerFactory.getLogger(OperationManager.class);

    private final Map<UUID, OperationItem> activeOperations = new ConcurrentHashMap<>();
    private final Queue<OperationItem> waitingOperations = new ArrayDeque<>();
    private final List<OperationItem> retryPendingOperations = new ArrayList<>();
    private int totalOperationCount;

    private final Settings settings;

    public OperationManager(Settings settings) {
        this.settings = settings;
    }

    public Optional<OperationItem> getActiveOperation(UUID correlationId) {
        return Optional.ofNullable(activeOperations.get(correlationId));
    }

    public int totalOperationCount() {
        return totalOperationCount;
    }

    public void cleanUp(Throwable cause) {
        if (!activeOperations.isEmpty() || !waitingOperations.isEmpty() || !retryPendingOperations.isEmpty()) {
            ConnectionClosedException connectionClosedException = new ConnectionClosedException("Connection was closed.", cause);

            concat(activeOperations.values().stream(), concat(waitingOperations.stream(), retryPendingOperations.stream()))
                .forEach(item -> item.operation.fail(connectionClosedException));
        }

        activeOperations.clear();
        waitingOperations.clear();
        retryPendingOperations.clear();
        totalOperationCount = 0;
    }

    public void checkTimeoutsAndRetry(Channel connection) {
        checkNotNull(connection, "connection is null");

        List<OperationItem> retryOperations = new ArrayList<>();
        List<OperationItem> removeOperations = new ArrayList<>();

        final ChannelId connectionId = ChannelId.of(connection);

        activeOperations.values().forEach(item -> {
            if (!item.connectionId.equals(connectionId)) {
                retryOperations.add(item);
            } else if (!item.timeout.isZero() && item.lastUpdated.isElapsed(settings.operationTimeout)) {
                String error = String.format("Operation never got response from server. UTC now: %s, operation: %s.",
                    Instant.now(), item.toString());

                logger.debug(error);

                if (settings.failOnNoServerResponse) {
                    item.operation.fail(new OperationTimedOutException(error));
                    removeOperations.add(item);
                } else {
                    retryOperations.add(item);
                }
            }
        });

        retryOperations.forEach(this::scheduleOperationRetry);
        removeOperations.forEach(this::removeOperation);

        if (!retryPendingOperations.isEmpty()) {
            retryPendingOperations.stream().sorted().forEach(item -> {
                UUID oldCorrelationId = item.correlationId;
                item.correlationId = UUID.randomUUID();
                item.retryCount += 1;

                logger.debug("retrying, old correlationId {}, operation {}.", oldCorrelationId, item.toString());

                scheduleOperation(item, connection);
            });

            retryPendingOperations.clear();
        }

        scheduleWaitingOperations(connection);
    }

    public void scheduleOperationRetry(OperationItem item) {
        if (removeOperation(item)) {
            logger.debug("scheduleOperationRetry for {}", item);

            if (item.maxRetries >= 0 && item.retryCount >= item.maxRetries) {
                item.operation.fail(new RetriesLimitReachedException(item.toString(), item.retryCount));
            } else {
                retryPendingOperations.add(item);
            }
        }
    }

    public boolean removeOperation(OperationItem item) {
        if (activeOperations.remove(item.correlationId) == null) {
            logger.debug("removeOperation FAILED for {}", item);
            return false;
        } else {
            logger.debug("removeOperation SUCCEEDED for {}", item);
            totalOperationCount = activeOperations.size() + waitingOperations.size();
            return true;
        }
    }

    public void scheduleWaitingOperations(Channel connection) {
        checkNotNull(connection, "connection is null");

        while (!waitingOperations.isEmpty() && activeOperations.size() < settings.maxConcurrentOperations) {
            scheduleOperation(waitingOperations.poll(), connection);
        }

        totalOperationCount = activeOperations.size() + waitingOperations.size();
    }

    public void enqueueOperation(OperationItem item) {
        logger.debug("enqueueOperation WAITING for {}.", item);
        waitingOperations.offer(item);
    }

    public void scheduleOperation(OperationItem item, Channel connection) {
        checkNotNull(connection, "connection is null");

        if (activeOperations.size() >= settings.maxConcurrentOperations) {
            logger.debug("scheduleOperation WAITING for {}.", item);
            waitingOperations.offer(item);
        } else {
            item.connectionId = ChannelId.of(connection);
            item.lastUpdated.update();
            activeOperations.put(item.correlationId, item);

            TcpPackage tcpPackage = item.operation.create(item.correlationId);

            logger.debug("scheduleOperation package {}, {}, {}.", tcpPackage.command, tcpPackage.correlationId, item);

            connection.writeAndFlush(tcpPackage);
        }

        totalOperationCount = activeOperations.size() + waitingOperations.size();
    }

}
