package com.github.msemys.esjc.subscription.manager;

import com.github.msemys.esjc.ConnectionClosedException;
import com.github.msemys.esjc.Settings;
import com.github.msemys.esjc.SubscriptionDropReason;
import com.github.msemys.esjc.operation.manager.OperationTimeoutException;
import com.github.msemys.esjc.operation.manager.RetriesLimitReachedException;
import io.netty.channel.Channel;
import io.netty.channel.ChannelId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

import static com.github.msemys.esjc.util.Iterables.consume;
import static com.github.msemys.esjc.util.Preconditions.checkNotNull;

public class SubscriptionManager {
    private static final Logger logger = LoggerFactory.getLogger(SubscriptionManager.class);

    private final Map<UUID, SubscriptionItem> activeSubscriptions = new ConcurrentHashMap<>();
    private final Queue<SubscriptionItem> waitingSubscriptions = new ConcurrentLinkedQueue<>();
    private final Queue<SubscriptionItem> retryPendingSubscriptions = new ConcurrentLinkedQueue<>();

    private final Settings settings;

    public SubscriptionManager(Settings settings) {
        checkNotNull(settings, "settings is null");
        this.settings = settings;
    }

    public Optional<SubscriptionItem> getActiveSubscription(UUID correlationId) {
        return Optional.ofNullable(activeSubscriptions.get(correlationId));
    }

    public void cleanUp(Throwable cause) {
        ConnectionClosedException exception = new ConnectionClosedException("Connection was closed.", cause);
        Consumer<SubscriptionItem> dropSubscription = item -> item.operation.drop(SubscriptionDropReason.ConnectionClosed, exception);

        consume(activeSubscriptions.values(), dropSubscription);
        consume(waitingSubscriptions, dropSubscription);
        consume(retryPendingSubscriptions, dropSubscription);
    }

    public void purgeSubscribedAndDropped(ChannelId connectionId) {
        List<SubscriptionItem> subscriptionsToRemove = new ArrayList<>();

        activeSubscriptions.values().stream()
            .filter(s -> s.isSubscribed && s.connectionId.equals(connectionId))
            .forEach(s -> {
                s.operation.connectionClosed();
                subscriptionsToRemove.add(s);
            });

        subscriptionsToRemove.forEach(s -> activeSubscriptions.remove(s.correlationId));
    }

    public void checkTimeoutsAndRetry(Channel connection) {
        List<SubscriptionItem> retrySubscriptions = new ArrayList<>();
        List<SubscriptionItem> removeSubscriptions = new ArrayList<>();

        activeSubscriptions.values().stream()
            .filter(s -> !s.isSubscribed)
            .forEach(s -> {
                if (connection != null && !s.connectionId.equals(connection.id())) {
                    retrySubscriptions.add(s);
                } else if (!s.timeout.isZero() && s.lastUpdated.isElapsed(settings.operationTimeout)) {
                    String error = String.format("Subscription never got confirmation from server. UTC now: %s, operation: %s.",
                        Instant.now(), s);

                    logger.error(error);

                    if (settings.failOnNoServerResponse) {
                        s.operation.drop(SubscriptionDropReason.SubscribingError, new OperationTimeoutException(error));
                        removeSubscriptions.add(s);
                    } else {
                        retrySubscriptions.add(s);
                    }
                }
            });

        removeSubscriptions.forEach(this::removeSubscription);

        if (connection != null) {
            retrySubscriptions.forEach(this::scheduleSubscriptionRetry);

            consume(retryPendingSubscriptions, item -> {
                item.retryCount += 1;
                startSubscription(item, connection);
            });

            consume(waitingSubscriptions, item -> startSubscription(item, connection));
        }
    }

    public boolean removeSubscription(SubscriptionItem item) {
        boolean removed = activeSubscriptions.remove(item.correlationId) != null;
        logger.debug("RemoveSubscription {}, result {}.", item, removed);
        return removed;
    }

    public void scheduleSubscriptionRetry(SubscriptionItem item) {
        if (!removeSubscription(item)) {
            logger.debug("RemoveSubscription failed when trying to retry {}.", item);
            return;
        }

        if (item.maxRetries >= 0 && item.retryCount >= item.maxRetries) {
            logger.debug("RETRIES LIMIT REACHED when trying to retry {}.", item);
            item.operation.drop(SubscriptionDropReason.SubscribingError,
                new RetriesLimitReachedException(item.toString(), item.retryCount));
            return;
        }

        logger.debug("retrying subscription {}.", item);
        retryPendingSubscriptions.add(item);
    }

    public void enqueueSubscription(SubscriptionItem item) {
        waitingSubscriptions.offer(item);
    }

    public void startSubscription(SubscriptionItem item, Channel connection) {
        checkNotNull(connection, "connection is null");

        if (item.isSubscribed) {
            logger.debug("StartSubscription REMOVING due to already subscribed {}.", item);
            removeSubscription(item);
            return;
        }

        item.correlationId = UUID.randomUUID();
        item.connectionId = connection.id();
        item.lastUpdated.update();

        activeSubscriptions.put(item.correlationId, item);

        if (!item.operation.subscribe(item.correlationId, connection)) {
            logger.debug("StartSubscription REMOVING AS COULD NOT SUBSCRIBE {}.", item);
            removeSubscription(item);
        } else {
            logger.debug("StartSubscription SUBSCRIBING {}.", item);
        }
    }
}
