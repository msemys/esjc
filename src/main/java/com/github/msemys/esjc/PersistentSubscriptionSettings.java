package com.github.msemys.esjc;

import com.github.msemys.esjc.system.SystemConsumerStrategy;

import java.time.Duration;

import static com.github.msemys.esjc.util.Numbers.isNegative;
import static com.github.msemys.esjc.util.Numbers.isPositive;
import static com.github.msemys.esjc.util.Preconditions.checkArgument;

/**
 * Persistent subscription settings.
 */
public class PersistentSubscriptionSettings {

    /**
     * Persistent subscription default settings.
     */
    public static final PersistentSubscriptionSettings DEFAULT = newBuilder().build();

    /**
     * Whether or not the subscription should resolve linkTo events to their linked events.
     */
    public final boolean resolveLinkTos;

    /**
     * The event number from which to start.
     */
    public final int startFrom;

    /**
     * Whether or not in depth latency statistics should be tracked on this subscription.
     */
    public final boolean timingStatistics;

    /**
     * The amount of time after which a message should be considered to be timedout and retried.
     */
    public final Duration messageTimeout;

    /**
     * The number of events read at a time when paging in history.
     */
    public final int readBatchSize;

    /**
     * The maximum number of retries (due to timeout) before a message get considered to be parked.
     */
    public final int maxRetryCount;

    /**
     * The size of the buffer listening to live messages as they happen.
     */
    public final int liveBufferSize;

    /**
     * The number of events to cache when paging through history.
     */
    public final int historyBufferSize;

    /**
     * The amount of time to try to checkpoint after.
     */
    public final Duration checkPointAfter;

    /**
     * The minimum number of messages to checkpoint.
     */
    public final int minCheckPointCount;

    /**
     * The maximum number of messages to checkpoint. If this number is a reached, a checkpoint will be forced.
     */
    public final int maxCheckPointCount;

    /**
     * The maximum number of subscribers allowed.
     */
    public final int maxSubscriberCount;

    /**
     * The strategy to use for distributing events to client consumers.
     *
     * @see SystemConsumerStrategy
     */
    public final SystemConsumerStrategy namedConsumerStrategy;

    private PersistentSubscriptionSettings(Builder builder) {
        resolveLinkTos = builder.resolveLinkTos;
        startFrom = builder.startFrom;
        timingStatistics = builder.timingStatistics;
        messageTimeout = builder.messageTimeout;
        readBatchSize = builder.readBatchSize;
        maxRetryCount = builder.maxRetryCount;
        liveBufferSize = builder.liveBufferSize;
        historyBufferSize = builder.historyBufferSize;
        checkPointAfter = builder.checkPointAfter;
        minCheckPointCount = builder.minCheckPointCount;
        maxCheckPointCount = builder.maxCheckPointCount;
        maxSubscriberCount = builder.maxSubscriberCount;
        namedConsumerStrategy = builder.namedConsumerStrategy;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("PersistentSubscriptionSettings{");
        sb.append("resolveLinkTos=").append(resolveLinkTos);
        sb.append(", startFrom=").append(startFrom);
        sb.append(", timingStatistics=").append(timingStatistics);
        sb.append(", messageTimeout=").append(messageTimeout);
        sb.append(", readBatchSize=").append(readBatchSize);
        sb.append(", maxRetryCount=").append(maxRetryCount);
        sb.append(", liveBufferSize=").append(liveBufferSize);
        sb.append(", historyBufferSize=").append(historyBufferSize);
        sb.append(", checkPointAfter=").append(checkPointAfter);
        sb.append(", minCheckPointCount=").append(minCheckPointCount);
        sb.append(", maxCheckPointCount=").append(maxCheckPointCount);
        sb.append(", maxSubscriberCount=").append(maxSubscriberCount);
        sb.append(", namedConsumerStrategy='").append(namedConsumerStrategy).append('\'');
        sb.append('}');
        return sb.toString();
    }

    /**
     * Creates a new persistent subscription settings builder.
     *
     * @return persistent subscription settings builder
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Persistent subscription settings builder.
     */
    public static class Builder {
        private Boolean resolveLinkTos;
        private Integer startFrom;
        private Boolean timingStatistics;
        private Duration messageTimeout;
        private Integer readBatchSize;
        private Integer maxRetryCount;
        private Integer liveBufferSize;
        private Integer historyBufferSize;
        private Duration checkPointAfter;
        private Integer minCheckPointCount;
        private Integer maxCheckPointCount;
        private Integer maxSubscriberCount;
        private SystemConsumerStrategy namedConsumerStrategy;

        /**
         * Specifies whether or not to resolve link events automatically (by default, it is disabled).
         *
         * @param resolveLinkTos whether to resolve link events automatically.
         * @return the builder reference
         */
        public Builder resolveLinkTos(boolean resolveLinkTos) {
            this.resolveLinkTos = resolveLinkTos;
            return this;
        }

        /**
         * Sets that the subscription should start from the beginning of the stream.
         *
         * @return the builder reference
         */
        public Builder startFromBeginning() {
            return startFrom(StreamPosition.START);
        }

        /**
         * Sets that the subscription should start from where the stream is, when the subscription is first connected.
         *
         * @return the builder reference
         */
        public Builder startFromCurrent() {
            return startFrom(StreamPosition.END);
        }

        /**
         * Sets that the subscription should start from a specified location of the stream (by default, starts from {@link StreamPosition#END}).
         *
         * @param startFrom the event number from which to start.
         * @return the builder reference
         */
        public Builder startFrom(int startFrom) {
            this.startFrom = startFrom;
            return this;
        }

        /**
         * Specifies whether or not to include further latency statistics (by default, it is disabled).
         * <p><b>Note:</b> these statistics have a cost and should not be used in high performance situations.</p>
         *
         * @param timingStatistics {@code true} to include further latency statistics.
         * @return the builder reference
         */
        public Builder timingStatistics(boolean timingStatistics) {
            this.timingStatistics = timingStatistics;
            return this;
        }

        /**
         * Sets the timeout for a message (by default, 30 seconds).
         * Message will be retried if an ack is not received within the specified duration.
         *
         * @param messageTimeout the maximum wait time before it should timeout.
         * @return the builder reference
         */
        public Builder messageTimeout(Duration messageTimeout) {
            this.messageTimeout = messageTimeout;
            return this;
        }

        /**
         * Sets the size of the read batch used when paging in history for the subscription (by default, 20 messages).
         * <p>Size should not be too big.</p>
         *
         * @param readBatchSize read batch size.
         * @return the builder reference
         */
        public Builder readBatchSize(int readBatchSize) {
            this.readBatchSize = readBatchSize;
            return this;
        }

        /**
         * Sets the number of times a message should be retried before being considered a bad message (by default, 10 times).
         *
         * @param maxRetryCount the maximum retry count.
         * @return the builder reference
         */
        public Builder maxRetryCount(int maxRetryCount) {
            this.maxRetryCount = maxRetryCount;
            return this;
        }

        /**
         * Sets the size of the live buffer for the subscription (by default, 500 messages).
         * This is the buffer used to cache messages while sending messages as they happen.
         * The count is in terms of the number of messages to cache.
         *
         * @param liveBufferSize number of messages in live buffer.
         * @return the builder reference
         */
        public Builder liveBufferSize(int liveBufferSize) {
            this.liveBufferSize = liveBufferSize;
            return this;
        }

        /**
         * Sets the size of the history buffer for the subscription (by default, 500 messages).
         *
         * @param historyBufferSize number of messages in history buffer.
         * @return the builder reference
         */
        public Builder historyBufferSize(int historyBufferSize) {
            this.historyBufferSize = historyBufferSize;
            return this;
        }

        /**
         * Sets that the backend should try to checkpoint the subscription after some
         * period of time (by default, 2 seconds). Note that if the increment of the checkpoint
         * would be below the minimum the stream will not be checkpointed at this time.
         * <p>
         * It is important to tweak checkpointing for high performance streams as they cause
         * writes to happen back in the system. There is a trade off between the number of
         * writes that can happen in varying failure scenarios and the frequency of
         * writing out the checkpoints within the system. Normally settings such
         * as once per second with a minimum of 5-10 messages and a high max to checkpoint should
         * be a good compromise for most streams though you may want to change this if you
         * for instance are doing hundreds of messages/second through the subscription.
         * </p>
         *
         * @param checkPointAfter the amount of time to try checkpointing after.
         * @return the builder reference
         */
        public Builder checkPointAfter(Duration checkPointAfter) {
            this.checkPointAfter = checkPointAfter;
            return this;
        }

        /**
         * Sets the minimum checkpoint count (by default, 10). The subscription will not increment
         * a checkpoint below this value eg if there is one item to checkpoint and it is set to five it
         * will not checkpoint.
         * <p>
         * It is important to tweak checkpointing for high performance streams as they cause
         * writes to happen back in the system. There is a trade off between the number of
         * writes that can happen in varying failure scenarios and the frequency of
         * writing out the checkpoints within the system. Normally settings such
         * as once per second with a minimum of 5-10 messages and a high max to checkpoint should
         * be a good compromise for most streams though you may want to change this if you
         * for instance are doing hundreds of messages/second through the subscription.
         * </p>
         *
         * @param minCheckPointCount the minimum count to checkpoint.
         * @return the builder reference
         */
        public Builder minCheckPointCount(int minCheckPointCount) {
            this.minCheckPointCount = minCheckPointCount;
            return this;
        }

        /**
         * Sets the largest increment the subscription will checkpoint (by default, 1000).
         * If this value is reached the subscription will immediately write a checkpoint.
         * As such this value should normally be reasonably large so as not to cause
         * too many writes to occur in the subscription.
         * <p>
         * It is important to tweak checkpointing for high performance streams as they cause
         * writes to happen back in the system. There is a trade off between the number of
         * writes that can happen in varying failure scenarios and the frequency of
         * writing out the checkpoints within the system. Normally settings such
         * as once per second with a minimum of 5-10 messages and a high max to checkpoint should
         * be a good compromise for most streams though you may want to change this if you
         * for instance are doing hundreds of messages/second through the subscription.
         * </p>
         *
         * @param maxCheckPointCount the maximum count to checkpoint.
         * @return the builder reference
         */
        public Builder maxCheckPointCount(int maxCheckPointCount) {
            this.maxCheckPointCount = maxCheckPointCount;
            return this;
        }

        /**
         * Sets the maximum number of subscribers allowed to connect (by default, 0 subscribers).
         *
         * @param maxSubscriberCount the maximum number of subscribers.
         * @return the builder reference
         */
        public Builder maxSubscriberCount(int maxSubscriberCount) {
            this.maxSubscriberCount = maxSubscriberCount;
            return this;
        }

        /**
         * Sets the consumer strategy for distributing event to clients (by default, {@link SystemConsumerStrategy#ROUND_ROBIN}).
         *
         * @param namedConsumerStrategy the consumer strategy name.
         * @return the builder reference
         * @see SystemConsumerStrategy
         */
        public Builder namedConsumerStrategy(SystemConsumerStrategy namedConsumerStrategy) {
            this.namedConsumerStrategy = namedConsumerStrategy;
            return this;
        }

        /**
         * Builds a persistent subscription settings.
         *
         * @return persistent subscription settings
         */
        public PersistentSubscriptionSettings build() {
            if (resolveLinkTos == null) {
                resolveLinkTos = false;
            }

            if (startFrom == null) {
                startFrom = StreamPosition.END;
            } else {
                checkArgument(startFrom >= -1, "startFrom should be >= -1");
            }

            if (timingStatistics == null) {
                timingStatistics = false;
            }

            if (messageTimeout == null) {
                messageTimeout = Duration.ofSeconds(30);
            }

            if (readBatchSize == null) {
                readBatchSize = 20;
            } else {
                checkArgument(isPositive(readBatchSize), "readBatchSize should be positive");
            }

            if (maxRetryCount == null) {
                maxRetryCount = 10;
            } else {
                checkArgument(isPositive(maxRetryCount), "maxRetryCount should be positive");
            }

            if (liveBufferSize == null) {
                liveBufferSize = 500;
            } else {
                checkArgument(isPositive(liveBufferSize), "liveBufferSize should be positive");
            }

            if (historyBufferSize == null) {
                historyBufferSize = 500;
            } else {
                checkArgument(isPositive(historyBufferSize), "historyBufferSize should be positive");
            }

            if (checkPointAfter == null) {
                checkPointAfter = Duration.ofSeconds(2);
            }

            if (minCheckPointCount == null) {
                minCheckPointCount = 10;
            } else {
                checkArgument(isPositive(minCheckPointCount), "minCheckPointCount should be positive");
            }

            if (maxCheckPointCount == null) {
                maxCheckPointCount = 1000;
            } else {
                checkArgument(isPositive(maxCheckPointCount), "maxCheckPointCount should be positive");
            }

            if (maxSubscriberCount == null) {
                maxSubscriberCount = 0;
            } else {
                checkArgument(!isNegative(maxSubscriberCount), "maxSubscriberCount should not be negative");
            }

            if (namedConsumerStrategy == null) {
                namedConsumerStrategy = SystemConsumerStrategy.ROUND_ROBIN;
            }

            return new PersistentSubscriptionSettings(this);
        }
    }

}
