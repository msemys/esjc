package com.github.msemys.esjc;

import static com.github.msemys.esjc.util.Numbers.isPositive;
import static com.github.msemys.esjc.util.Preconditions.checkArgument;
import static com.github.msemys.esjc.util.Ranges.BATCH_SIZE_RANGE;

/**
 * Catch-up subscription settings.
 */
public class CatchUpSubscriptionSettings {

    /**
     * Catch-up subscription default settings.
     */
    public static final CatchUpSubscriptionSettings DEFAULT = newBuilder().build();

    /**
     * The maximum number of events allowed to be cached when processing from live subscription.
     * Going above will drop the subscription.
     */
    public final int maxLiveQueueSize;

    /**
     * Whether or not the subscription should resolve linkTo events to their linked events.
     */
    public final boolean resolveLinkTos;

    /**
     * The number of events to read per batch when reading history.
     */
    public final int readBatchSize;

    private CatchUpSubscriptionSettings(Builder builder) {
        maxLiveQueueSize = builder.maxLiveQueueSize;
        resolveLinkTos = builder.resolveLinkTos;
        readBatchSize = builder.readBatchSize;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CatchUpSubscriptionSettings{");
        sb.append("maxLiveQueueSize=").append(maxLiveQueueSize);
        sb.append(", resolveLinkTos=").append(resolveLinkTos);
        sb.append(", readBatchSize=").append(readBatchSize);
        sb.append('}');
        return sb.toString();
    }

    /**
     * Creates a new catch-up subscription settings builder.
     *
     * @return catch-up subscription settings builder
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Catch-up subscription settings builder.
     */
    public static class Builder {
        private Integer maxLiveQueueSize;
        private Boolean resolveLinkTos;
        private Integer readBatchSize;

        /**
         * Specifies the maximum number of events allowed to be cached when processing from live subscription (by default, 10000 events).
         * Going above will drop the subscription.
         *
         * @param maxLiveQueueSize the maximum number of events allowed to be cached when processing from live subscription.
         * @return the builder reference
         */
        public Builder maxLiveQueueSize(int maxLiveQueueSize) {
            this.maxLiveQueueSize = maxLiveQueueSize;
            return this;
        }

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
         * Sets the size of the read batch used when reading history (by default, 500 events).
         *
         * @param readBatchSize read batch size.
         * @return the builder reference
         */
        public Builder readBatchSize(int readBatchSize) {
            this.readBatchSize = readBatchSize;
            return this;
        }

        /**
         * Builds a catch-up subscription settings.
         *
         * @return catch-up subscription settings
         */
        public CatchUpSubscriptionSettings build() {
            if (maxLiveQueueSize == null) {
                maxLiveQueueSize = 10000;
            } else {
                checkArgument(isPositive(maxLiveQueueSize), "maxLiveQueueSize should be positive");
            }

            if (resolveLinkTos == null) {
                resolveLinkTos = false;
            }

            if (readBatchSize == null) {
                readBatchSize = 500;
            } else {
                checkArgument(BATCH_SIZE_RANGE.contains(readBatchSize), "readBatchSize is out of range. Allowed range: %s.", BATCH_SIZE_RANGE.toString());
            }

            return new CatchUpSubscriptionSettings(this);
        }
    }

}
