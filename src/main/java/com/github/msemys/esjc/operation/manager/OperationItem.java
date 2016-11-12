package com.github.msemys.esjc.operation.manager;

import com.github.msemys.esjc.operation.Operation;
import com.github.msemys.esjc.tcp.ChannelId;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static com.github.msemys.esjc.util.Preconditions.checkNotNull;

public class OperationItem implements Comparable<OperationItem> {
    private static final AtomicLong sequencer = new AtomicLong(-1);

    public final long sequenceNo = sequencer.incrementAndGet();

    public final Operation operation;
    public final int maxRetries;
    public final Duration timeout;
    public final Instant createdTime;

    public ChannelId connectionId;
    public UUID correlationId;
    public int retryCount;
    public Instant lastUpdated;

    public OperationItem(Operation operation, int maxRetries, Duration timeout) {
        checkNotNull(operation, "operation is null");

        this.operation = operation;
        this.maxRetries = maxRetries;
        this.timeout = timeout;
        this.createdTime = Instant.now();

        correlationId = UUID.randomUUID();
        retryCount = 0;
        lastUpdated = Instant.now();
    }

    @Override
    public int compareTo(OperationItem o) {
        if (sequenceNo < o.sequenceNo) {
            return -1;
        } else if (sequenceNo > o.sequenceNo) {
            return 1;
        } else {
            return 0;
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Operation (").append(operation.getClass().getSimpleName()).append("): ");
        sb.append(correlationId).append(", retry count: ").append(retryCount).append(", ");
        sb.append("created: ").append(createdTime).append(", ");
        sb.append("last updated: ").append(lastUpdated);
        return sb.toString();
    }

}
