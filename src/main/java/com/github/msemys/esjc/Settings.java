package com.github.msemys.esjc;

import com.github.msemys.esjc.node.cluster.ClusterNodeSettings;
import com.github.msemys.esjc.node.static_.StaticNodeSettings;
import com.github.msemys.esjc.operation.UserCredentials;
import com.github.msemys.esjc.tcp.TcpSettings;

import java.time.Duration;
import java.util.Optional;

import static com.github.msemys.esjc.util.Numbers.isNegative;
import static com.github.msemys.esjc.util.Numbers.isPositive;
import static com.github.msemys.esjc.util.Preconditions.checkArgument;

public class Settings {
    public final TcpSettings tcpSettings;
    public final Optional<StaticNodeSettings> staticNodeSettings;
    public final Optional<ClusterNodeSettings> clusterNodeSettings;
    public final boolean ssl;
    public final Duration reconnectionDelay;
    public final Duration heartbeatInterval;
    public final Duration heartbeatTimeout;
    public final boolean requireMaster;
    public final Optional<UserCredentials> userCredentials;
    public final Duration operationTimeout;
    public final Duration operationTimeoutCheckInterval;
    public final int maxOperationQueueSize;
    public final int maxConcurrentOperations;
    public final int maxOperationRetries;
    public final int maxReconnections;
    public final int maxPushQueueSize;
    public final int readBatchSize;
    public final int persistentSubscriptionBufferSize;
    public final boolean persistentSubscriptionAutoAckEnabled;
    public final boolean failOnNoServerResponse;
    public final int minThreadPoolSize;
    public final int maxThreadPoolSize;

    private Settings(Builder builder) {
        tcpSettings = builder.tcpSettings;
        staticNodeSettings = Optional.ofNullable(builder.staticNodeSettings);
        clusterNodeSettings = Optional.ofNullable(builder.clusterNodeSettings);
        ssl = builder.ssl;
        reconnectionDelay = builder.reconnectionDelay;
        heartbeatInterval = builder.heartbeatInterval;
        heartbeatTimeout = builder.heartbeatTimeout;
        requireMaster = builder.requireMaster;
        userCredentials = Optional.ofNullable(builder.userCredentials);
        operationTimeout = builder.operationTimeout;
        operationTimeoutCheckInterval = builder.operationTimeoutCheckInterval;
        maxOperationQueueSize = builder.maxOperationQueueSize;
        maxConcurrentOperations = builder.maxConcurrentOperations;
        maxOperationRetries = builder.maxOperationRetries;
        maxReconnections = builder.maxReconnections;
        maxPushQueueSize = builder.maxPushQueueSize;
        readBatchSize = builder.readBatchSize;
        persistentSubscriptionBufferSize = builder.persistentSubscriptionBufferSize;
        persistentSubscriptionAutoAckEnabled = builder.persistentSubscriptionAutoAckEnabled;
        failOnNoServerResponse = builder.failOnNoServerResponse;
        minThreadPoolSize = builder.minThreadPoolSize;
        maxThreadPoolSize = builder.maxThreadPoolSize;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Settings{");
        sb.append("tcpSettings=").append(tcpSettings);
        sb.append(", staticNodeSettings=").append(staticNodeSettings);
        sb.append(", clusterNodeSettings=").append(clusterNodeSettings);
        sb.append(", ssl=").append(ssl);
        sb.append(", reconnectionDelay=").append(reconnectionDelay);
        sb.append(", heartbeatInterval=").append(heartbeatInterval);
        sb.append(", heartbeatTimeout=").append(heartbeatTimeout);
        sb.append(", requireMaster=").append(requireMaster);
        sb.append(", userCredentials=").append(userCredentials);
        sb.append(", operationTimeout=").append(operationTimeout);
        sb.append(", operationTimeoutCheckInterval=").append(operationTimeoutCheckInterval);
        sb.append(", maxOperationQueueSize=").append(maxOperationQueueSize);
        sb.append(", maxConcurrentOperations=").append(maxConcurrentOperations);
        sb.append(", maxOperationRetries=").append(maxOperationRetries);
        sb.append(", maxReconnections=").append(maxReconnections);
        sb.append(", maxPushQueueSize=").append(maxPushQueueSize);
        sb.append(", readBatchSize=").append(readBatchSize);
        sb.append(", persistentSubscriptionBufferSize=").append(persistentSubscriptionBufferSize);
        sb.append(", persistentSubscriptionAutoAckEnabled=").append(persistentSubscriptionAutoAckEnabled);
        sb.append(", failOnNoServerResponse=").append(failOnNoServerResponse);
        sb.append(", minThreadPoolSize=").append(minThreadPoolSize);
        sb.append(", maxThreadPoolSize=").append(maxThreadPoolSize);
        sb.append('}');
        return sb.toString();
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private TcpSettings tcpSettings;
        private StaticNodeSettings staticNodeSettings;
        private ClusterNodeSettings clusterNodeSettings;
        private Boolean ssl;
        private Duration reconnectionDelay;
        private Duration heartbeatInterval;
        private Duration heartbeatTimeout;
        private Boolean requireMaster;
        private UserCredentials userCredentials;
        private Duration operationTimeout;
        private Duration operationTimeoutCheckInterval;
        private Integer maxOperationQueueSize;
        private Integer maxConcurrentOperations;
        private Integer maxOperationRetries;
        private Integer maxReconnections;
        private Integer maxPushQueueSize;
        private Integer readBatchSize;
        private Integer persistentSubscriptionBufferSize;
        private Boolean persistentSubscriptionAutoAckEnabled;
        private Boolean failOnNoServerResponse;
        private Integer minThreadPoolSize;
        private Integer maxThreadPoolSize;

        private Builder() {
        }

        public Builder tcpSettings(TcpSettings tcpSettings) {
            this.tcpSettings = tcpSettings;
            return this;
        }

        public Builder nodeSettings(StaticNodeSettings staticNodeSettings) {
            this.staticNodeSettings = staticNodeSettings;
            return this;
        }

        public Builder nodeSettings(ClusterNodeSettings clusterNodeSettings) {
            this.clusterNodeSettings = clusterNodeSettings;
            return this;
        }

        public Builder ssl(boolean ssl) {
            this.ssl = ssl;
            return this;
        }

        public Builder reconnectionDelay(Duration duration) {
            this.reconnectionDelay = duration;
            return this;
        }

        public Builder heartbeatInterval(Duration heartbeatInterval) {
            this.heartbeatInterval = heartbeatInterval;
            return this;
        }

        public Builder heartbeatTimeout(Duration heartbeatTimeout) {
            this.heartbeatTimeout = heartbeatTimeout;
            return this;
        }

        public Builder requireMaster(boolean requireMaster) {
            this.requireMaster = requireMaster;
            return this;
        }

        public Builder userCredentials(String username, String password) {
            this.userCredentials = new UserCredentials(username, password);
            return this;
        }

        public Builder operationTimeout(Duration operationTimeout) {
            this.operationTimeout = operationTimeout;
            return this;
        }

        public Builder operationTimeoutCheckInterval(Duration operationTimeoutCheckInterval) {
            this.operationTimeoutCheckInterval = operationTimeoutCheckInterval;
            return this;
        }

        public Builder maxQueueSize(int maxOperationQueueSize) {
            this.maxOperationQueueSize = maxOperationQueueSize;
            return this;
        }

        public Builder maxConcurrentOperations(int maxConcurrentOperations) {
            this.maxConcurrentOperations = maxConcurrentOperations;
            return this;
        }

        public Builder maxOperationRetries(int maxOperationRetries) {
            this.maxOperationRetries = maxOperationRetries;
            return this;
        }

        public Builder maxReconnections(int maxReconnections) {
            this.maxReconnections = maxReconnections;
            return this;
        }

        public Builder maxPushQueueSize(int maxPushQueueSize) {
            this.maxPushQueueSize = maxPushQueueSize;
            return this;
        }

        public Builder readBatchSize(int readBatchSize) {
            this.readBatchSize = readBatchSize;
            return this;
        }

        public Builder persistentSubscriptionBufferSize(int persistentSubscriptionBufferSize) {
            this.persistentSubscriptionBufferSize = persistentSubscriptionBufferSize;
            return this;
        }

        public Builder persistentSubscriptionAutoAckEnabled(boolean persistentSubscriptionAutoAckEnabled) {
            this.persistentSubscriptionAutoAckEnabled = persistentSubscriptionAutoAckEnabled;
            return this;
        }

        public Builder failOnNoServerResponse(boolean failOnNoServerResponse) {
            this.failOnNoServerResponse = failOnNoServerResponse;
            return this;
        }

        public Builder minThreadPoolSize(int minThreadPoolSize) {
            this.minThreadPoolSize = minThreadPoolSize;
            return this;
        }

        public Builder maxThreadPoolSize(int maxThreadPoolSize) {
            this.maxThreadPoolSize = maxThreadPoolSize;
            return this;
        }

        public Settings build() {
            checkArgument(staticNodeSettings != null || clusterNodeSettings != null, "Missing node settings");
            checkArgument(staticNodeSettings == null || clusterNodeSettings == null, "Usage of 'static' and 'cluster' settings at once is not allowed");

            if (tcpSettings == null) {
                tcpSettings = TcpSettings.newBuilder().build();
            }

            if (ssl == null) {
                ssl = false;
            }

            if (reconnectionDelay == null) {
                reconnectionDelay = Duration.ofSeconds(1);
            }

            if (heartbeatInterval == null) {
                heartbeatInterval = Duration.ofMillis(500);
            }

            if (heartbeatTimeout == null) {
                heartbeatTimeout = Duration.ofMillis(1500);
            }

            if (requireMaster == null) {
                requireMaster = true;
            }

            if (operationTimeout == null) {
                operationTimeout = Duration.ofSeconds(7);
            }

            if (operationTimeoutCheckInterval == null) {
                operationTimeoutCheckInterval = Duration.ofSeconds(1);
            }

            if (maxOperationQueueSize == null) {
                maxOperationQueueSize = 5000;
            } else {
                checkArgument(isPositive(maxOperationQueueSize), "maxOperationQueueSize should be positive");
            }

            if (maxConcurrentOperations == null) {
                maxConcurrentOperations = 5000;
            } else {
                checkArgument(isPositive(maxConcurrentOperations), "maxConcurrentOperations should be positive");
            }

            if (maxOperationRetries == null) {
                maxOperationRetries = 10;
            } else {
                checkArgument(maxOperationRetries >= -1, "maxOperationRetries value is out of range: %d. Allowed range: [-1, infinity].", maxOperationRetries);
            }

            if (maxReconnections == null) {
                maxReconnections = 10;
            } else {
                checkArgument(maxReconnections >= -1, "maxReconnections value is out of range: %d. Allowed range: [-1, infinity].", maxReconnections);
            }

            if (maxPushQueueSize == null) {
                maxPushQueueSize = 10000;
            } else {
                checkArgument(isPositive(maxPushQueueSize), "maxPushQueueSize should be positive");
            }

            if (readBatchSize == null) {
                readBatchSize = 500;
            } else {
                checkArgument(isPositive(readBatchSize), "readBatchSize should be positive");
            }

            if (persistentSubscriptionBufferSize == null) {
                persistentSubscriptionBufferSize = 10;
            } else {
                checkArgument(isPositive(persistentSubscriptionBufferSize), "persistentSubscriptionBufferSize should be positive");
            }

            if (persistentSubscriptionAutoAckEnabled == null) {
                persistentSubscriptionAutoAckEnabled = true;
            }

            if (failOnNoServerResponse == null) {
                failOnNoServerResponse = false;
            }

            if (minThreadPoolSize == null) {
                minThreadPoolSize = 2;
            } else {
                checkArgument(!isNegative(minThreadPoolSize), "minThreadPoolSize should not be negative");
            }

            if (maxThreadPoolSize == null) {
                maxThreadPoolSize = Runtime.getRuntime().availableProcessors() * 2;
            } else {
                checkArgument(isPositive(maxThreadPoolSize), "maxThreadPoolSize should be positive");
            }

            return new Settings(this);
        }
    }

}
