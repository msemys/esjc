package lt.msemys.esjc;

import lt.msemys.esjc.node.cluster.ClusterNodeSettings;
import lt.msemys.esjc.node.static_.StaticNodeSettings;
import lt.msemys.esjc.operation.UserCredentials;
import lt.msemys.esjc.tcp.TcpSettings;

import java.time.Duration;
import java.util.Optional;

import static lt.msemys.esjc.util.Preconditions.checkArgument;

public class Settings {

    public static final int MAX_READ_SIZE = 4 * 1024;

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
    public final int maxQueueSize;
    public final int maxConcurrentOperations;
    public final int maxOperationRetries;
    public final int maxReconnections;
    public final boolean failOnNoServerResponse;

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
        sb.append(", maxQueueSize=").append(maxQueueSize);
        sb.append(", maxConcurrentOperations=").append(maxConcurrentOperations);
        sb.append(", maxOperationRetries=").append(maxOperationRetries);
        sb.append(", maxReconnections=").append(maxReconnections);
        sb.append(", failOnNoServerResponse=").append(failOnNoServerResponse);
        sb.append('}');
        return sb.toString();
    }

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
        maxQueueSize = builder.maxQueueSize;
        maxConcurrentOperations = builder.maxConcurrentOperations;
        maxOperationRetries = builder.maxOperationRetries;
        maxReconnections = builder.maxReconnections;
        failOnNoServerResponse = builder.failOnNoServerResponse;
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
        private Integer maxQueueSize;
        private Integer maxConcurrentOperations;
        private Integer maxOperationRetries;
        private Integer maxReconnections;
        private Boolean failOnNoServerResponse;

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

        public Builder maxQueueSize(int maxQueueSize) {
            this.maxQueueSize = maxQueueSize;
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

        public Builder failOnNoServerResponse(boolean failOnNoServerResponse) {
            this.failOnNoServerResponse = failOnNoServerResponse;
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

            if (maxQueueSize == null) {
                maxQueueSize = 5000;
            } else {
                checkArgument(maxQueueSize > 0, "maxQueueSize should be positive");
            }

            if (maxConcurrentOperations == null) {
                maxConcurrentOperations = 5000;
            }

            if (maxOperationRetries == null) {
                maxOperationRetries = 10;
            }

            if (maxReconnections == null) {
                maxReconnections = 10;
            }

            if (failOnNoServerResponse == null) {
                failOnNoServerResponse = false;
            }

            return new Settings(this);
        }
    }

}
