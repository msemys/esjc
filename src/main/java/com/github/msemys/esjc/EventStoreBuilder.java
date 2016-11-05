package com.github.msemys.esjc;

import com.github.msemys.esjc.node.cluster.ClusterNodeSettings;
import com.github.msemys.esjc.node.cluster.ClusterNodeSettings.BuilderForDnsDiscoverer;
import com.github.msemys.esjc.node.cluster.ClusterNodeSettings.BuilderForGossipSeedDiscoverer;
import com.github.msemys.esjc.node.single.SingleNodeSettings;
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
    private SingleNodeSettings.Builder singleNodeSettingsBuilder;
    private BuilderForDnsDiscoverer clusterNodeUsingDnsSettingsBuilder;
    private BuilderForGossipSeedDiscoverer clusterNodeUsingGossipSeedSettingsBuilder;

    private EventStoreBuilder(Settings.Builder settingsBuilder,
                              TcpSettings.Builder tcpSettingsBuilder,
                              SingleNodeSettings.Builder singleNodeSettingsBuilder,
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
            .persistentSubscriptionAutoAck(settings.persistentSubscriptionAutoAck)
            .failOnNoServerResponse(settings.failOnNoServerResponse)
            .executor(settings.executor);

        settings.userCredentials.ifPresent(u -> settingsBuilder.userCredentials(u.username, u.password));

        // populate single-node settings builder
        SingleNodeSettings.Builder singleNodeSettingsBuilder = null;

        if (settings.singleNodeSettings.isPresent()) {
            singleNodeSettingsBuilder = SingleNodeSettings.newBuilder().address(settings.singleNodeSettings.get().address);
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
                    .dns(settings.clusterNodeSettings.get().dns)
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
            singleNodeSettingsBuilder = SingleNodeSettings.newBuilder();
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
            singleNodeSettingsBuilder = SingleNodeSettings.newBuilder();
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
     * Disables connection encryption.
     *
     * @return the builder reference
     */
    public EventStoreBuilder noSslConnection() {
        settingsBuilder.sslSettings(SslSettings.noSsl());
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
     * Sets whether or not to require Event Store to refuse serving read or write request if it is not master (cluster version only).
     *
     * @param requireMaster {@code true} to require master.
     * @return the builder reference
     */
    public EventStoreBuilder requireMaster(boolean requireMaster) {
        settingsBuilder.requireMaster(requireMaster);
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
    public EventStoreBuilder noUserCredentials() {
        settingsBuilder.noUserCredentials();
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
     * Sets whether or not by default, the persistent subscription should automatically acknowledge messages processed.
     *
     * @param persistentSubscriptionAutoAck {@code true} to enable auto-acknowledge.
     * @return the builder reference
     */
    public EventStoreBuilder persistentSubscriptionAutoAck(boolean persistentSubscriptionAutoAck) {
        settingsBuilder.persistentSubscriptionAutoAck(persistentSubscriptionAutoAck);
        return this;
    }

    /**
     * Sets whether or not to raise an error if no response is received from the server for an operation.
     *
     * @param failOnNoServerResponse {@code true} to raise an error or {@code false} to schedule operation retry.
     * @return the builder reference
     */
    public EventStoreBuilder failOnNoServerResponse(boolean failOnNoServerResponse) {
        settingsBuilder.failOnNoServerResponse(failOnNoServerResponse);
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
