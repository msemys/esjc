package com.github.msemys.esjc;

import com.github.msemys.esjc.node.cluster.ClusterNodeSettings;
import com.github.msemys.esjc.node.single.SingleNodeSettings;
import com.github.msemys.esjc.operation.manager.OperationTimeoutException;
import com.github.msemys.esjc.operation.manager.RetriesLimitReachedException;
import com.github.msemys.esjc.ssl.SslSettings;
import com.github.msemys.esjc.tcp.TcpSettings;
import com.github.msemys.esjc.util.concurrent.DefaultThreadFactory;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.github.msemys.esjc.util.Numbers.isPositive;
import static com.github.msemys.esjc.util.Preconditions.checkArgument;
import static com.github.msemys.esjc.util.Ranges.ATTEMPTS_RANGE;
import static com.github.msemys.esjc.util.Strings.isNullOrEmpty;

/**
 * Client settings
 */
public class Settings {

    /**
     * Client connection name.
     */
    public final String connectionName;

    /**
     * TCP settings.
     */
    public final TcpSettings tcpSettings;

    /**
     * Single node settings (optional).
     */
    public final SingleNodeSettings singleNodeSettings;

    /**
     * Cluster node settings (optional).
     */
    public final ClusterNodeSettings clusterNodeSettings;

    /**
     * SSL settings.
     */
    public final SslSettings sslSettings;

    /**
     * The amount of time to delay before attempting to reconnect.
     */
    public final Duration reconnectionDelay;

    /**
     * The interval at which to send heartbeat messages.
     * <p>
     * <b>Note:</b> heartbeat request will be sent only if connection is idle (no writes) for the specified time.
     * </p>
     */
    public final Duration heartbeatInterval;

    /**
     * The interval after which an unacknowledged heartbeat will cause
     * the connection to be considered faulted and disconnect.
     */
    public final Duration heartbeatTimeout;

    /**
     * Whether or not all write and read requests to be served only by master.
     * <p>
     * <b>Note:</b> this option is used for cluster only.
     * </p>
     */
    public final boolean requireMaster;

    /**
     * The default user credentials to use for operations where other user credentials are not explicitly supplied.
     */
    public final UserCredentials userCredentials;

    /**
     * The amount of time before an operation is considered to have timed out.
     */
    public final Duration operationTimeout;

    /**
     * The amount of time that timeouts are checked in the system.
     */
    public final Duration operationTimeoutCheckInterval;

    /**
     * The maximum number of outstanding items allowed in the operation queue.
     */
    public final int maxOperationQueueSize;

    /**
     * The maximum number of allowed asynchronous operations to be in process.
     */
    public final int maxConcurrentOperations;

    /**
     * The maximum number of operation retry attempts.
     */
    public final int maxOperationRetries;

    /**
     * The maximum number of times to allow for reconnection.
     */
    public final int maxReconnections;

    /**
     * The default buffer size to use for the persistent subscription.
     */
    public final int persistentSubscriptionBufferSize;

    /**
     * Whether by default, the persistent subscription should automatically acknowledge messages processed.
     */
    public final boolean persistentSubscriptionAutoAck;

    /**
     * Whether or not to raise an error if no response is received from the server for an operation.
     */
    public final boolean failOnNoServerResponse;

    /**
     * Whether to disconnect on a channel error or to reconnect.
     */
    public final boolean disconnectOnTcpChannelError;

    /**
     * Time to wait for congestion in action queue to clear before failing
     */
    public final Duration actionQueueCongestionTimeout;

    /**
     * The executor to execute client internal tasks (such as establish-connection, start-operation) and run subscriptions.
     */
    public final Executor executor;

    private Settings(Builder builder) {
        connectionName = builder.connectionName;
        tcpSettings = builder.tcpSettings;
        singleNodeSettings = builder.singleNodeSettings;
        clusterNodeSettings = builder.clusterNodeSettings;
        sslSettings = builder.sslSettings;
        reconnectionDelay = builder.reconnectionDelay;
        heartbeatInterval = builder.heartbeatInterval;
        heartbeatTimeout = builder.heartbeatTimeout;
        requireMaster = builder.requireMaster;
        userCredentials = builder.userCredentials;
        operationTimeout = builder.operationTimeout;
        operationTimeoutCheckInterval = builder.operationTimeoutCheckInterval;
        maxOperationQueueSize = builder.maxOperationQueueSize;
        maxConcurrentOperations = builder.maxConcurrentOperations;
        maxOperationRetries = builder.maxOperationRetries;
        maxReconnections = builder.maxReconnections;
        persistentSubscriptionBufferSize = builder.persistentSubscriptionBufferSize;
        persistentSubscriptionAutoAck = builder.persistentSubscriptionAutoAck;
        failOnNoServerResponse = builder.failOnNoServerResponse;
        disconnectOnTcpChannelError = builder.disconnectOnTcpChannelError;
        executor = builder.executor;
        actionQueueCongestionTimeout = builder.actionQueueCongestionTimeout;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Settings{");
        sb.append("connectionName='").append(connectionName).append('\'');
        sb.append(", tcpSettings=").append(tcpSettings);
        sb.append(", singleNodeSettings=").append(singleNodeSettings);
        sb.append(", clusterNodeSettings=").append(clusterNodeSettings);
        sb.append(", sslSettings=").append(sslSettings);
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
        sb.append(", persistentSubscriptionBufferSize=").append(persistentSubscriptionBufferSize);
        sb.append(", persistentSubscriptionAutoAck=").append(persistentSubscriptionAutoAck);
        sb.append(", failOnNoServerResponse=").append(failOnNoServerResponse);
        sb.append(", disconnectOnTcpChannelError=").append(disconnectOnTcpChannelError);
        sb.append(", actionQueueCongestionTimeout=").append(actionQueueCongestionTimeout);
        sb.append(", executor=").append(executor);
        sb.append('}');
        return sb.toString();
    }

    /**
     * Creates a new client settings builder.
     *
     * @return client settings builder
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Client settings builder.
     */
    public static class Builder {
        private String connectionName;
        private TcpSettings tcpSettings;
        private SingleNodeSettings singleNodeSettings;
        private ClusterNodeSettings clusterNodeSettings;
        private SslSettings sslSettings;
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
        private Integer persistentSubscriptionBufferSize;
        private Boolean persistentSubscriptionAutoAck;
        private Boolean failOnNoServerResponse;
        private Boolean disconnectOnTcpChannelError;
        private Duration actionQueueCongestionTimeout;
        private Executor executor;

        private Builder() {
        }

        /**
         * Sets client connection name.
         *
         * @param connectionName client connection name.
         * @return the builder reference
         */
        public Builder connectionName(String connectionName) {
            this.connectionName = connectionName;
            return this;
        }

        /**
         * Sets TCP settings.
         *
         * @param tcpSettings TCP settings.
         * @return the builder reference
         */
        public Builder tcpSettings(TcpSettings tcpSettings) {
            this.tcpSettings = tcpSettings;
            return this;
        }

        /**
         * Sets single node settings.
         *
         * @param singleNodeSettings single node settings.
         * @return the builder reference
         */
        public Builder nodeSettings(SingleNodeSettings singleNodeSettings) {
            this.singleNodeSettings = singleNodeSettings;
            return this;
        }

        /**
         * Sets cluster node settings.
         *
         * @param clusterNodeSettings cluster node settings.
         * @return the builder reference
         */
        public Builder nodeSettings(ClusterNodeSettings clusterNodeSettings) {
            this.clusterNodeSettings = clusterNodeSettings;
            return this;
        }

        /**
         * Sets SSL settings (by default, SSL is not used).
         *
         * @param sslSettings ssl settings.
         * @return the builder reference
         */
        public Builder sslSettings(SslSettings sslSettings) {
            this.sslSettings = sslSettings;
            return this;
        }

        /**
         * Sets the amount of time to delay before attempting to reconnect (by default, 1 second).
         *
         * @param duration the amount of time to delay before attempting to reconnect.
         * @return the builder reference
         */
        public Builder reconnectionDelay(Duration duration) {
            this.reconnectionDelay = duration;
            return this;
        }

        /**
         * Sets the interval at which to send heartbeat messages (by default, 500 milliseconds).
         * <p>
         * <b>Note:</b> heartbeat request will be sent only if connection is idle (no writes) for the specified time.
         * </p>
         *
         * @param heartbeatInterval the interval at which to send heartbeat messages.
         * @return the builder reference
         */
        public Builder heartbeatInterval(Duration heartbeatInterval) {
            this.heartbeatInterval = heartbeatInterval;
            return this;
        }

        /**
         * Sets the interval after which an unacknowledged heartbeat will cause
         * the connection to be considered faulted and disconnect (by default, 1500 milliseconds).
         *
         * @param heartbeatTimeout heartbeat timeout.
         * @return the builder reference
         */
        public Builder heartbeatTimeout(Duration heartbeatTimeout) {
            this.heartbeatTimeout = heartbeatTimeout;
            return this;
        }

        /**
         * Specifies whether or not all write and read requests to be served only by master.
         * By default, it is enabled - master node required.
         * <p>
         * <b>Note:</b> this option is used for cluster only.
         * </p>
         *
         * @param requireMaster {@code true} to require master.
         * @return the builder reference
         */
        public Builder requireMaster(boolean requireMaster) {
            this.requireMaster = requireMaster;
            return this;
        }

        /**
         * Sets the default user credentials to be used for operations.
         * If user credentials are not given for an operation, these credentials will be used.
         *
         * @param userCredentials user credentials.
         * @return the builder reference
         */
        public Builder userCredentials(UserCredentials userCredentials) {
            this.userCredentials = userCredentials;
            return this;
        }

        /**
         * Sets the default user credentials to be used for operations.
         * If user credentials are not given for an operation, these credentials will be used.
         *
         * @param username user name.
         * @param password user password.
         * @return the builder reference
         */
        public Builder userCredentials(String username, String password) {
            return userCredentials(new UserCredentials(username, password));
        }

        /**
         * Sets no default user credentials for operations.
         *
         * @return the builder reference
         * @see #userCredentials(String, String)
         * @see #userCredentials(UserCredentials)
         */
        public Builder noUserCredentials() {
            return userCredentials(null);
        }

        /**
         * Sets the amount of time before an operation is considered to have timed out (by default, 7 seconds).
         *
         * @param operationTimeout the amount of time before an operation is considered to have timed out.
         * @return the builder reference
         */
        public Builder operationTimeout(Duration operationTimeout) {
            this.operationTimeout = operationTimeout;
            return this;
        }

        /**
         * Sets the amount of time that timeouts are checked in the system (by default, 1 second).
         *
         * @param operationTimeoutCheckInterval the amount of time that timeouts are checked in the system.
         * @return the builder reference
         */
        public Builder operationTimeoutCheckInterval(Duration operationTimeoutCheckInterval) {
            this.operationTimeoutCheckInterval = operationTimeoutCheckInterval;
            return this;
        }

        /**
         * Sets the maximum number of outstanding items allowed in the operation queue (by default, 5000 items).
         *
         * @param maxOperationQueueSize the maximum number of outstanding items allowed in the operation queue.
         * @return the builder reference
         */
        public Builder maxOperationQueueSize(int maxOperationQueueSize) {
            this.maxOperationQueueSize = maxOperationQueueSize;
            return this;
        }

        /**
         * Sets the maximum number of allowed asynchronous operations to be in process (by default, 5000 operations).
         *
         * @param maxConcurrentOperations the maximum number of allowed asynchronous operations to be in process.
         * @return the builder reference
         */
        public Builder maxConcurrentOperations(int maxConcurrentOperations) {
            this.maxConcurrentOperations = maxConcurrentOperations;
            return this;
        }

        /**
         * Sets the maximum number of operation retry attempts (by default, 10 attempts).
         * When the specified number of retries for an operation is reached, then operation completes
         * exceptionally with cause {@link RetriesLimitReachedException}.
         *
         * @param maxOperationRetries the maximum number of operation retry attempts (use {@code -1} for unlimited).
         * @return the builder reference
         */
        public Builder maxOperationRetries(int maxOperationRetries) {
            this.maxOperationRetries = maxOperationRetries;
            return this;
        }

        /**
         * Sets the maximum number of times to allow for reconnection (by default, 10 times).
         *
         * @param maxReconnections the maximum number of times to allow for reconnection (use {@code -1} for unlimited).
         * @return the builder reference
         */
        public Builder maxReconnections(int maxReconnections) {
            this.maxReconnections = maxReconnections;
            return this;
        }

        /**
         * Sets the default buffer size to use for the persistent subscription (by default, 10 messages).
         *
         * @param persistentSubscriptionBufferSize the default buffer size to use for the persistent subscription.
         * @return the builder reference
         */
        public Builder persistentSubscriptionBufferSize(int persistentSubscriptionBufferSize) {
            this.persistentSubscriptionBufferSize = persistentSubscriptionBufferSize;
            return this;
        }

        /**
         * Sets whether or not by default, the persistent subscription should automatically acknowledge messages processed.
         * By default, it is enabled.
         *
         * @param persistentSubscriptionAutoAck {@code true} to enable auto-acknowledge.
         * @return the builder reference
         */
        public Builder persistentSubscriptionAutoAck(boolean persistentSubscriptionAutoAck) {
            this.persistentSubscriptionAutoAck = persistentSubscriptionAutoAck;
            return this;
        }

        /**
         * Sets whether or not to complete operation exceptionally with cause {@link OperationTimeoutException}
         * if no response is received from the server for an operation. By default, it is disabled - operations are
         * scheduled to be retried.
         *
         * @param failOnNoServerResponse {@code true} to raise an error or {@code false} to schedule operation retry.
         * @return the builder reference
         */
        public Builder failOnNoServerResponse(boolean failOnNoServerResponse) {
            this.failOnNoServerResponse = failOnNoServerResponse;
            return this;
        }

        /**
         * Sets whether or not to disconnect the client on detecting a channel error. By default, it is disabled and the client
         * tries to reconnect according to {@link #maxReconnections(int)}. If it is enabled the client disconnects immediately.
         *
         * @param disconnectOnTcpChannelError {@code true} to disconnect or {@code false} to try to reconnect.
         * @return the builder reference
         */
        public Builder disconnectOnTcpChannelError(boolean disconnectOnTcpChannelError) {
            this.disconnectOnTcpChannelError = disconnectOnTcpChannelError;
            return this;
        }

        public Builder actionQueueCongestionTimeout(Duration actionQueueCongestionTimeout) {
            this.actionQueueCongestionTimeout = actionQueueCongestionTimeout;
            return this;
        }

        /**
         * Sets the executor to execute client internal tasks (such as establish-connection, start-operation) and run subscriptions.
         *
         * @param executor the executor to execute client internal tasks and run subscriptions.
         * @return the builder reference
         */
        public Builder executor(Executor executor) {
            this.executor = executor;
            return this;
        }

        /**
         * Sets default executor to execute client internal tasks (such as establish-connection, start-operation) and run subscriptions.
         *
         * @return the builder reference
         * @see #executor(Executor)
         */
        public Builder defaultExecutor() {
            return executor(null);
        }

        /**
         * Builds a client settings.
         *
         * @return client settings
         */
        public Settings build() {
            checkArgument(singleNodeSettings != null || clusterNodeSettings != null, "Missing node settings");
            checkArgument(singleNodeSettings == null || clusterNodeSettings == null, "Usage of 'single-node' and 'cluster-node' settings at once is not allowed");

            if (isNullOrEmpty(connectionName)) {
                connectionName = "ESJC-" + UUID.randomUUID().toString();
            }

            if (tcpSettings == null) {
                tcpSettings = TcpSettings.newBuilder().build();
            }

            if (sslSettings == null) {
                sslSettings = SslSettings.noSsl();
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
                checkArgument(ATTEMPTS_RANGE.contains(maxOperationRetries), "maxOperationRetries value is out of range. Allowed range: %s.", ATTEMPTS_RANGE.toString());
            }

            if (maxReconnections == null) {
                maxReconnections = 10;
            } else {
                checkArgument(ATTEMPTS_RANGE.contains(maxReconnections), "maxReconnections value is out of range. Allowed range: %s.", ATTEMPTS_RANGE.toString());
            }

            if (persistentSubscriptionBufferSize == null) {
                persistentSubscriptionBufferSize = 10;
            } else {
                checkArgument(isPositive(persistentSubscriptionBufferSize), "persistentSubscriptionBufferSize should be positive");
            }

            if (persistentSubscriptionAutoAck == null) {
                persistentSubscriptionAutoAck = true;
            }

            if (failOnNoServerResponse == null) {
                failOnNoServerResponse = false;
            }

            if (disconnectOnTcpChannelError == null) {
                disconnectOnTcpChannelError = false;
            }

            if (actionQueueCongestionTimeout == null) {
                actionQueueCongestionTimeout = Duration.ofSeconds(30);
            } else {
                checkArgument(!actionQueueCongestionTimeout.isNegative(), "actionQueueCongestionTimeout is negative");
            }

            if (executor == null) {
                executor = new ThreadPoolExecutor(2, Integer.MAX_VALUE,
                    60L, TimeUnit.SECONDS,
                    new SynchronousQueue<>(),
                    new DefaultThreadFactory("es"));
            }

            return new Settings(this);
        }
    }

}
