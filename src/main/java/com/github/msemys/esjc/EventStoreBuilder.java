package com.github.msemys.esjc;

import com.github.msemys.esjc.node.cluster.ClusterNodeSettings;
import com.github.msemys.esjc.node.cluster.ClusterNodeSettings.BuilderForDnsDiscoverer;
import com.github.msemys.esjc.node.cluster.ClusterNodeSettings.BuilderForGossipSeedDiscoverer;
import com.github.msemys.esjc.node.static_.StaticNodeSettings;
import com.github.msemys.esjc.ssl.SslSettings;
import com.github.msemys.esjc.tcp.TcpSettings;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.function.Function;

import static com.github.msemys.esjc.util.Preconditions.checkNotNull;

/**
 * Event Store client builder.
 */
public class EventStoreBuilder {
    private final Settings.Builder settingsBuilder;
    private TcpSettings.Builder tcpSettingsBuilder;
    private StaticNodeSettings.Builder singleNodeSettingsBuilder;
    private BuilderForDnsDiscoverer clusterNodeUsingDnsSettingsBuilder;
    private BuilderForGossipSeedDiscoverer clusterNodeUsingGossipSeedSettingsBuilder;

    private EventStoreBuilder(Settings.Builder settingsBuilder,
                              TcpSettings.Builder tcpSettingsBuilder,
                              StaticNodeSettings.Builder singleNodeSettingsBuilder,
                              BuilderForDnsDiscoverer clusterNodeUsingDnsSettingsBuilder,
                              BuilderForGossipSeedDiscoverer clusterNodeUsingGossipSeedSettingsBuilder) {
        checkNotNull(settingsBuilder, "settingsBuilder is null");

        this.settingsBuilder = settingsBuilder;
        this.tcpSettingsBuilder = tcpSettingsBuilder;
        this.singleNodeSettingsBuilder = singleNodeSettingsBuilder;
        this.clusterNodeUsingDnsSettingsBuilder = clusterNodeUsingDnsSettingsBuilder;
        this.clusterNodeUsingGossipSeedSettingsBuilder = clusterNodeUsingGossipSeedSettingsBuilder;
    }

    /**
     * Creates a new Event Store client builder populated with the specified settings.
     *
     * @param settings client settings
     * @return Event Store client builder.
     */
    public static EventStoreBuilder newBuilder(Settings settings) {
        checkNotNull(settings, "settings is null");

        // populate settings builder
        Settings.Builder settingsBuilder = Settings.newBuilder()
            .sslSettings(settings.sslSettings)
            .reconnectionDelay(settings.reconnectionDelay)
            .heartbeatInterval(settings.heartbeatInterval)
            .heartbeatTimeout(settings.heartbeatTimeout)
            .requireMaster(settings.requireMaster)
            .operationTimeout(settings.operationTimeout)
            .operationTimeoutCheckInterval(settings.operationTimeoutCheckInterval)
            .maxOperationQueueSize(settings.maxOperationQueueSize)
            .maxConcurrentOperations(settings.maxConcurrentOperations)
            .maxOperationRetries(settings.maxOperationRetries)
            .maxReconnections(settings.maxReconnections)
            .persistentSubscriptionBufferSize(settings.persistentSubscriptionBufferSize)
            .persistentSubscriptionAutoAckEnabled(settings.persistentSubscriptionAutoAckEnabled)
            .failOnNoServerResponse(settings.failOnNoServerResponse)
            .executor(settings.executor);

        settings.userCredentials.ifPresent(u -> settingsBuilder.userCredentials(u.username, u.password));

        // populate single-node settings builder
        StaticNodeSettings.Builder singleNodeSettingsBuilder = null;

        if (settings.staticNodeSettings.isPresent()) {
            singleNodeSettingsBuilder = StaticNodeSettings.newBuilder().address(settings.staticNodeSettings.get().address);
        }

        // populate cluster-node settings builders
        BuilderForDnsDiscoverer clusterNodeUsingDnsSettingsBuilder = null;
        BuilderForGossipSeedDiscoverer clusterNodeUsingGossipSeedSettingsBuilder = null;

        if (settings.clusterNodeSettings.isPresent()) {
            if (!settings.clusterNodeSettings.get().gossipSeeds.isEmpty()) {
                clusterNodeUsingGossipSeedSettingsBuilder = ClusterNodeSettings.forGossipSeedDiscoverer()
                    .maxDiscoverAttempts(settings.clusterNodeSettings.get().maxDiscoverAttempts)
                    .discoverAttemptInterval(settings.clusterNodeSettings.get().discoverAttemptInterval)
                    .gossipSeeds(settings.clusterNodeSettings.get().gossipSeeds)
                    .gossipTimeout(settings.clusterNodeSettings.get().gossipTimeout);
            } else {
                clusterNodeUsingDnsSettingsBuilder = ClusterNodeSettings.forDnsDiscoverer()
                    .maxDiscoverAttempts(settings.clusterNodeSettings.get().maxDiscoverAttempts)
                    .discoverAttemptInterval(settings.clusterNodeSettings.get().discoverAttemptInterval)
                    .clusterDns(settings.clusterNodeSettings.get().clusterDns)
                    .externalGossipPort(settings.clusterNodeSettings.get().externalGossipPort)
                    .gossipTimeout(settings.clusterNodeSettings.get().gossipTimeout);
            }
        }

        // populate tcp settings builder
        TcpSettings.Builder tcpSettingsBuilder = TcpSettings.newBuilder()
            .connectTimeout(settings.tcpSettings.connectTimeout)
            .closeTimeout(settings.tcpSettings.closeTimeout)
            .keepAlive(settings.tcpSettings.keepAlive)
            .noDelay(settings.tcpSettings.noDelay)
            .sendBufferSize(settings.tcpSettings.sendBufferSize)
            .receiveBufferSize(settings.tcpSettings.receiveBufferSize)
            .writeBufferHighWaterMark(settings.tcpSettings.writeBufferHighWaterMark)
            .writeBufferLowWaterMark(settings.tcpSettings.writeBufferLowWaterMark);

        return new EventStoreBuilder(
            settingsBuilder,
            tcpSettingsBuilder,
            singleNodeSettingsBuilder,
            clusterNodeUsingDnsSettingsBuilder,
            clusterNodeUsingGossipSeedSettingsBuilder);
    }

    /**
     * Creates a new Event Store client builder.
     *
     * @return Event Store client builder.
     */
    public static EventStoreBuilder newBuilder() {
        return new EventStoreBuilder(Settings.newBuilder(), null, null, null, null);
    }

    /**
     * Sets single-node server address.
     *
     * @param address the server address.
     * @return the builder reference
     */
    public EventStoreBuilder singleNodeAddress(InetSocketAddress address) {
        if (singleNodeSettingsBuilder == null) {
            singleNodeSettingsBuilder = StaticNodeSettings.newBuilder();
        }
        singleNodeSettingsBuilder = singleNodeSettingsBuilder.address(address);
        clusterNodeUsingDnsSettingsBuilder = null;
        clusterNodeUsingGossipSeedSettingsBuilder = null;
        return this;
    }

    /**
     * Sets single-node server address.
     *
     * @param host the host name.
     * @param port The port number.
     * @return the builder reference
     */
    public EventStoreBuilder singleNodeAddress(String host, int port) {
        if (singleNodeSettingsBuilder == null) {
            singleNodeSettingsBuilder = StaticNodeSettings.newBuilder();
        }
        singleNodeSettingsBuilder = singleNodeSettingsBuilder.address(host, port);
        clusterNodeUsingDnsSettingsBuilder = null;
        clusterNodeUsingGossipSeedSettingsBuilder = null;
        return this;
    }

    /**
     * Sets the client to discover cluster-nodes using a DNS name and a well-known port.
     *
     * @param modifier function to apply to cluster-node settings builder
     * @return the builder reference
     * @see #clusterNodeUsingGossipSeeds(Function)
     */
    public EventStoreBuilder clusterNodeUsingDns(Function<BuilderForDnsDiscoverer, BuilderForDnsDiscoverer> modifier) {
        checkNotNull(modifier, "modifier is null");
        clusterNodeUsingDnsSettingsBuilder = modifier.apply(clusterNodeUsingDnsSettingsBuilder == null ?
            ClusterNodeSettings.forDnsDiscoverer() : clusterNodeUsingDnsSettingsBuilder);
        clusterNodeUsingGossipSeedSettingsBuilder = null;
        singleNodeSettingsBuilder = null;
        return this;
    }

    /**
     * Sets the client to discover cluster-nodes by specifying the IP endpoints (gossip seeds) of one or more of the nodes.
     *
     * @param modifier function to apply to cluster-node settings builder
     * @return the builder reference
     * @see #clusterNodeUsingDns(Function)
     */
    public EventStoreBuilder clusterNodeUsingGossipSeeds(Function<BuilderForGossipSeedDiscoverer, BuilderForGossipSeedDiscoverer> modifier) {
        checkNotNull(modifier, "modifier is null");
        clusterNodeUsingGossipSeedSettingsBuilder = modifier.apply(clusterNodeUsingGossipSeedSettingsBuilder == null ?
            ClusterNodeSettings.forGossipSeedDiscoverer() : clusterNodeUsingGossipSeedSettingsBuilder);
        clusterNodeUsingDnsSettingsBuilder = null;
        singleNodeSettingsBuilder = null;
        return this;
    }

    /**
     * Sets TCP settings.
     *
     * @param modifier function to apply to TCP settings builder
     * @return the builder reference
     */
    public EventStoreBuilder tcpSettings(Function<TcpSettings.Builder, TcpSettings.Builder> modifier) {
        checkNotNull(modifier, "modifier is null");
        tcpSettingsBuilder = modifier.apply(tcpSettingsBuilder == null ? TcpSettings.newBuilder() : tcpSettingsBuilder);
        return this;
    }

    /**
     * Enables connection encryption using SSL and trusts an X.509 server certificate whose Common Name (CN) matches.
     *
     * @param certificateCommonName server certificate common name (CN)
     * @return the builder reference
     */
    public EventStoreBuilder useSslConnection(String certificateCommonName) {
        settingsBuilder.sslSettings(SslSettings.trustCertificateCN(certificateCommonName));
        return this;
    }

    /**
     * Enables connection encryption using SSL and trusts all X.509 server certificates without any verification.
     *
     * @return the builder reference
     */
    public EventStoreBuilder useSslConnection() {
        settingsBuilder.sslSettings(SslSettings.trustAllCertificates());
        return this;
    }

    /**
     * Sets the amount of time to delay before attempting to reconnect.
     *
     * @param duration the amount of time to delay before attempting to reconnect.
     * @return the builder reference
     */
    public EventStoreBuilder reconnectionDelay(Duration duration) {
        settingsBuilder.reconnectionDelay(duration);
        return this;
    }

    /**
     * Sets the maximum number of times to allow for reconnection.
     *
     * @param count the maximum number of times to allow for reconnection (use {@code -1} for unlimited).
     * @return the builder reference
     */
    public EventStoreBuilder maxReconnections(int count) {
        settingsBuilder.maxReconnections(count);
        return this;
    }

    /**
     * Sets the interval at which to send heartbeat messages.
     * <p>
     * <u>NOTE</u>: heartbeat request will be sent only if connection is idle (no writes) for the specified time.
     * </p>
     *
     * @param duration the interval at which to send heartbeat messages.
     * @return the builder reference
     */
    public EventStoreBuilder heartbeatInterval(Duration duration) {
        settingsBuilder.heartbeatInterval(duration);
        return this;
    }

    /**
     * Sets the interval after which an unacknowledged heartbeat will cause
     * the connection to be considered faulted and disconnect.
     *
     * @param duration heartbeat timeout.
     * @return the builder reference
     */
    public EventStoreBuilder heartbeatTimeout(Duration duration) {
        settingsBuilder.heartbeatTimeout(duration);
        return this;
    }

    /**
     * Requires all write and read requests to be served only by master (cluster version only).
     *
     * @return the builder reference
     */
    public EventStoreBuilder requireMasterEnabled() {
        settingsBuilder.requireMaster(true);
        return this;
    }

    /**
     * Allows for writes to be forwarded and read requests served locally if node is not master (cluster version only).
     *
     * @return the builder reference
     */
    public EventStoreBuilder requireMasterDisabled() {
        settingsBuilder.requireMaster(false);
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
    public EventStoreBuilder userCredentials(String username, String password) {
        settingsBuilder.userCredentials(username, password);
        return this;
    }

    /**
     * Sets no default user credentials for operations.
     *
     * @return the builder reference
     * @see #userCredentials(String, String)
     */
    public EventStoreBuilder withoutUserCredentials() {
        settingsBuilder.withoutUserCredentials();
        return this;
    }

    /**
     * Sets the amount of time before an operation is considered to have timed out.
     *
     * @param duration the amount of time before an operation is considered to have timed out.
     * @return the builder reference
     */
    public EventStoreBuilder operationTimeout(Duration duration) {
        settingsBuilder.operationTimeout(duration);
        return this;
    }

    /**
     * Sets the amount of time that timeouts are checked in the system.
     *
     * @param duration the amount of time that timeouts are checked in the system.
     * @return the builder reference
     */
    public EventStoreBuilder operationTimeoutCheckInterval(Duration duration) {
        settingsBuilder.operationTimeoutCheckInterval(duration);
        return this;
    }

    /**
     * Sets the maximum number of outstanding items allowed in the operation queue.
     *
     * @param size the maximum number of outstanding items allowed in the operation queue.
     * @return the builder reference
     */
    public EventStoreBuilder maxOperationQueueSize(int size) {
        settingsBuilder.maxOperationQueueSize(size);
        return this;
    }

    /**
     * Sets the maximum number of allowed asynchronous operations to be in process.
     *
     * @param count the maximum number of allowed asynchronous operations to be in process.
     * @return the builder reference
     */
    public EventStoreBuilder maxConcurrentOperations(int count) {
        settingsBuilder.maxConcurrentOperations(count);
        return this;
    }

    /**
     * Sets the maximum number of operation retry attempts.
     *
     * @param count the maximum number of operation retry attempts (use {@code -1} for unlimited).
     * @return the builder reference
     */
    public EventStoreBuilder maxOperationRetries(int count) {
        settingsBuilder.maxOperationRetries(count);
        return this;
    }

    /**
     * Sets the default buffer size to use for the persistent subscription.
     *
     * @param size the default buffer size to use for the persistent subscription.
     * @return the builder reference
     */
    public EventStoreBuilder persistentSubscriptionBufferSize(int size) {
        settingsBuilder.persistentSubscriptionBufferSize(size);
        return this;
    }

    /**
     * Enables auto-acknowledge for persistent subscriptions.
     *
     * @return the builder reference
     */
    public EventStoreBuilder persistentSubscriptionAutoAckEnabled() {
        settingsBuilder.persistentSubscriptionAutoAckEnabled(true);
        return this;
    }

    /**
     * Disables auto-acknowledge for persistent subscriptions.
     *
     * @return the builder reference
     */
    public EventStoreBuilder persistentSubscriptionAutoAckDisabled() {
        settingsBuilder.persistentSubscriptionAutoAckEnabled(false);
        return this;
    }

    /**
     * Operation timeout checker will raise an error if no response is received from the server for an operation.
     *
     * @return the builder reference
     */
    public EventStoreBuilder failOnNoServerResponseEnabled() {
        settingsBuilder.failOnNoServerResponse(true);
        return this;
    }

    /**
     * Operation timeout checker will schedule operation retry, if no response is received from the server for an operation.
     *
     * @return the builder reference
     */
    public EventStoreBuilder failOnNoServerResponseDisabled() {
        settingsBuilder.failOnNoServerResponse(false);
        return this;
    }

    /**
     * Sets the executor to execute client internal tasks (such as establish-connection, start-operation) and run subscriptions.
     *
     * @param executor the executor to execute client internal tasks and run subscriptions.
     * @return the builder reference
     */
    public EventStoreBuilder executor(Executor executor) {
        this.settingsBuilder.executor(executor);
        return this;
    }

    /**
     * Builds an Event Store client.
     *
     * @return Event Store client
     */
    public EventStore build() {
        if (singleNodeSettingsBuilder != null) {
            settingsBuilder.nodeSettings(singleNodeSettingsBuilder.build());
        }

        if (clusterNodeUsingDnsSettingsBuilder != null) {
            settingsBuilder.nodeSettings(clusterNodeUsingDnsSettingsBuilder.build());
        }

        if (clusterNodeUsingGossipSeedSettingsBuilder != null) {
            settingsBuilder.nodeSettings(clusterNodeUsingGossipSeedSettingsBuilder.build());
        }

        if (tcpSettingsBuilder != null) {
            settingsBuilder.tcpSettings(tcpSettingsBuilder.build());
        }

        return new EventStoreImpl(settingsBuilder.build());
    }

}
