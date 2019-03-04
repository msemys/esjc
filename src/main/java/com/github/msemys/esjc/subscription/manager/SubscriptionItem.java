package com.github.msemys.esjc.subscription.manager;

import com.github.msemys.esjc.subscription.SubscriptionOperation;
import com.github.msemys.esjc.util.SystemTime;
import io.netty.channel.ChannelId;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static com.github.msemys.esjc.util.Preconditions.checkNotNull;

public class SubscriptionItem {
    public final SubscriptionOperation operation;
    public final int maxRetries;
    public final Duration timeout;
    public final Instant createdTime;

    public ChannelId connectionId;
    public UUID correlationId;
    public boolean isSubscribed;
    public int retryCount;
    public final SystemTime lastUpdated;

    public SubscriptionItem(SubscriptionOperation operation, int maxRetries, Duration timeout) {
        checkNotNull(operation, "operation is null");

        this.operation = operation;
        this.maxRetries = maxRetries;
        this.timeout = timeout;
        this.createdTime = Instant.now();

        correlationId = UUID.randomUUID();
        retryCount = 0;
        lastUpdated = SystemTime.now();
    }

    @Override
    public String toString() {
        return new StringBuilder()
            .append("Subscription ").append(operation.getClass().getSimpleName())
            .append(" (").append(correlationId).append("): ").append(operation)
            .append(", is subscribed: ").append(isSubscribed)
            .append(", retry count: ").append(retryCount)
            .append(", created: ").append(createdTime)
            .append(", last updated: ").append(lastUpdated)
            .toString();
    }
}
