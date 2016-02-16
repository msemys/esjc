package lt.msemys.esjc;

import lt.msemys.esjc.system.SystemConsumerStrategies;

import java.time.Duration;

import static lt.msemys.esjc.util.Numbers.isNegative;
import static lt.msemys.esjc.util.Numbers.isPositive;
import static lt.msemys.esjc.util.Preconditions.checkArgument;
import static lt.msemys.esjc.util.Strings.isNullOrEmpty;

public class PersistentSubscriptionSettings {
    public static final PersistentSubscriptionSettings DEFAULT = newBuilder().build();

    public boolean resolveLinkTos;
    public int startFrom;
    public boolean timingStatistics;
    public Duration messageTimeout;
    public int readBatchSize;
    public int maxRetryCount;
    public int liveBufferSize;
    public int historyBufferSize;
    public Duration checkPointAfter;
    public int minCheckPointCount;
    public int maxCheckPointCount;
    public int maxSubscriberCount;
    public String namedConsumerStrategies;

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
        namedConsumerStrategies = builder.namedConsumerStrategies;
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
        sb.append(", namedConsumerStrategies='").append(namedConsumerStrategies).append('\'');
        sb.append('}');
        return sb.toString();
    }

    public static Builder newBuilder() {
        return new Builder();
    }

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
        private String namedConsumerStrategies;

        public Builder resolveLinkTos(boolean resolveLinkTos) {
            this.resolveLinkTos = resolveLinkTos;
            return this;
        }

        public Builder startFromBeginning() {
            return startFrom(StreamPosition.START);
        }

        public Builder startFromCurrent() {
            return startFrom(StreamPosition.END);
        }

        public Builder startFrom(int startFrom) {
            this.startFrom = startFrom;
            return this;
        }

        public Builder timingStatistics(boolean timingStatistics) {
            this.timingStatistics = timingStatistics;
            return this;
        }

        public Builder messageTimeout(Duration messageTimeout) {
            this.messageTimeout = messageTimeout;
            return this;
        }

        public Builder readBatchSize(int readBatchSize) {
            this.readBatchSize = readBatchSize;
            return this;
        }

        public Builder maxRetryCount(int maxRetryCount) {
            this.maxRetryCount = maxRetryCount;
            return this;
        }

        public Builder liveBufferSize(int liveBufferSize) {
            this.liveBufferSize = liveBufferSize;
            return this;
        }

        public Builder historyBufferSize(int historyBufferSize) {
            this.historyBufferSize = historyBufferSize;
            return this;
        }

        public Builder checkPointAfter(Duration checkPointAfter) {
            this.checkPointAfter = checkPointAfter;
            return this;
        }

        public Builder minCheckPointCount(int minCheckPointCount) {
            this.minCheckPointCount = minCheckPointCount;
            return this;
        }

        public Builder maxCheckPointCount(int maxCheckPointCount) {
            this.maxCheckPointCount = maxCheckPointCount;
            return this;
        }

        public Builder maxSubscriberCount(int maxSubscriberCount) {
            this.maxSubscriberCount = maxSubscriberCount;
            return this;
        }

        public Builder namedConsumerStrategies(String namedConsumerStrategies) {
            this.namedConsumerStrategies = namedConsumerStrategies;
            return this;
        }

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
                readBatchSize = 500;
            } else {
                checkArgument(isPositive(readBatchSize), "readBatchSize should be positive");
            }

            if (maxRetryCount == null) {
                maxRetryCount = 500;
            } else {
                checkArgument(isPositive(maxRetryCount), "maxRetryCount should be positive");
            }

            if (liveBufferSize == null) {
                liveBufferSize = 10;
            } else {
                checkArgument(isPositive(liveBufferSize), "liveBufferSize should be positive");
            }

            if (historyBufferSize == null) {
                historyBufferSize = 20;
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
                checkArgument(!isNegative(maxSubscriberCount), "maxSubscriberCount should not be negative.");
            }

            if (namedConsumerStrategies == null) {
                namedConsumerStrategies = SystemConsumerStrategies.ROUND_ROBIN;
            } else {
                checkArgument(!isNullOrEmpty(namedConsumerStrategies), "namedConsumerStrategies");
            }

            return new PersistentSubscriptionSettings(this);
        }
    }

}
