package com.github.msemys.esjc;

import com.github.msemys.esjc.node.DefaultEndpointDiscovererFactory;
import com.github.msemys.esjc.node.DelegatedEndpointDiscovererFactory;
import com.github.msemys.esjc.node.EndpointDiscoverer;
import com.github.msemys.esjc.node.EndpointDiscovererFactory;
import com.github.msemys.esjc.node.cluster.ClusterNodeSettings;
import com.github.msemys.esjc.node.cluster.ClusterNodeSettings.BuilderForDnsDiscoverer;
import com.github.msemys.esjc.node.cluster.ClusterNodeSettings.BuilderForGossipSeedDiscoverer;
import com.github.msemys.esjc.node.single.SingleNodeSettings;
import com.github.msemys.esjc.operation.manager.OperationTimeoutException;
import com.github.msemys.esjc.operation.manager.RetriesLimitReachedException;
import com.github.msemys.esjc.ssl.SslSettings;
import com.github.msemys.esjc.tcp.TcpSettings;

import java.io.File;
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
            .connectionName(settings.connectionName)
            .sslSettings(settings.sslSettings)
            .endpointDiscovererFactory(settings.endpointDiscovererFactory)
            .reconnectionDelay(settings.reconnectionDelay)
            .heartbeatInterval(settings.heartbeatInterval)
            .heartbeatTimeout(settings.heartbeatTimeout)
            .requireMaster(settings.requireMaster)
            .userCredentials(settings.userCredentials)
            .operationTimeout(settings.operationTimeout)
            .operationTimeoutCheckInterval(settings.operationTimeoutCheckInterval)
            .maxOperationQueueSize(settings.maxOperationQueueSize)
            .maxConcurrentOperations(settings.maxConcurrentOperations)
            .maxOperationRetries(settings.maxOperationRetries)
            .maxReconnections(settings.maxReconnections)
            .persistentSubscriptionBufferSize(settings.persistentSubscriptionBufferSize)
            .persistentSubscriptionAutoAck(settings.persistentSubscriptionAutoAck)
            .failOnNoServerResponse(settings.failOnNoServerResponse)
            .disconnectOnTcpChannelError(settings.disconnectOnTcpChannelError)
            .executor(settings.executor);

        // populate single-node settings builder
        SingleNodeSettings.Builder singleNodeSettingsBuilder = null;

        if (settings.singleNodeSettings != null) {
            singleNodeSettingsBuilder = SingleNodeSettings.newBuilder().address(settings.singleNodeSettings.address);
        }

        // populate cluster-node settings builders
        BuilderForDnsDiscoverer clusterNodeUsingDnsSettingsBuilder = null;
        BuilderForGossipSeedDiscoverer clusterNodeUsingGossipSeedSettingsBuilder = null;

        if (settings.clusterNodeSettings != null) {
            if (!settings.clusterNodeSettings.gossipSeeds.isEmpty()) {
                clusterNodeUsingGossipSeedSettingsBuilder = ClusterNodeSettings.forGossipSeedDiscoverer()
                    .maxDiscoverAttempts(settings.clusterNodeSettings.maxDiscoverAttempts)
                    .discoverAttemptInterval(settings.clusterNodeSettings.discoverAttemptInterval)
                    .gossipSeeds(settings.clusterNodeSettings.gossipSeeds)
                    .gossipTimeout(settings.clusterNodeSettings.gossipTimeout)
                    .nodePreference(settings.clusterNodeSettings.nodePreference);
            } else {
                clusterNodeUsingDnsSettingsBuilder = ClusterNodeSettings.forDnsDiscoverer()
                    .maxDiscoverAttempts(settings.clusterNodeSettings.maxDiscoverAttempts)
                    .discoverAttemptInterval(settings.clusterNodeSettings.discoverAttemptInterval)
                    .dns(settings.clusterNodeSettings.dns)
                    .externalGossipPort(settings.clusterNodeSettings.externalGossipPort)
                    .gossipTimeout(settings.clusterNodeSettings.gossipTimeout)
                    .nodePreference(settings.clusterNodeSettings.nodePreference);
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
            .writeBufferLowWaterMark(settings.tcpSettings.writeBufferLowWaterMark)
            .maxFrameLength(settings.tcpSettings.maxFrameLength);

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
     * Enables connection encryption using SSL and trusts an X.509 server certificate whose certificate is trusted
     * by the given certificate file (in PEM form).
     *
     * @param certificateFile server certificate
     * @return the builder reference
     */
    public EventStoreBuilder useSslConnection(File certificateFile) {
        settingsBuilder.sslSettings(SslSettings.trustCertificate(certificateFile));
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
     * Sets client connection name.
     *
     * @param connectionName client connection name.
     * @return the builder reference
     */
    public EventStoreBuilder connectionName(String connectionName) {
        settingsBuilder.connectionName(connectionName);
        return this;
    }

    /**
     * Sets endpoint discoverer factory (by default, {@link DefaultEndpointDiscovererFactory} is used).
     *
     * @param endpointDiscovererFactory endpoint discoverer factory.
     * @return the builder reference
     */
    public EventStoreBuilder endpointDiscovererFactory(EndpointDiscovererFactory endpointDiscovererFactory) {
        settingsBuilder.endpointDiscovererFactory(endpointDiscovererFactory);
        return this;
    }

    /**
     * Sets endpoint discoverer using {@link DelegatedEndpointDiscovererFactory}.
     *
     * @param endpointDiscoverer endpoint discoverer.
     * @return the builder reference
     * @see #endpointDiscovererFactory(EndpointDiscovererFactory)
     */
    public EventStoreBuilder endpointDiscoverer(EndpointDiscoverer endpointDiscoverer) {
        settingsBuilder.endpointDiscoverer(endpointDiscoverer);
        return this;
    }

    /**
     * Sets the amount of time to delay before attempting to reconnect (by default, 1 second).
     *
     * @param duration the amount of time to delay before attempting to reconnect.
     * @return the builder reference
     */
    public EventStoreBuilder reconnectionDelay(Duration duration) {
        settingsBuilder.reconnectionDelay(duration);
        return this;
    }

    /**
     * Sets the maximum number of times to allow for reconnection (by default, 10 times).
     *
     * @param count the maximum number of times to allow for reconnection (use {@code -1} for unlimited).
     * @return the builder reference
     */
    public EventStoreBuilder maxReconnections(int count) {
        settingsBuilder.maxReconnections(count);
        return this;
    }

    /**
     * Sets the interval at which to send heartbeat messages (by default, 500 milliseconds).
     * <p>
     * <b>Note:</b> heartbeat request will be sent only if connection is idle (no writes) for the specified time.
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
     * the connection to be considered faulted and disconnect (by default, 1500 milliseconds).
     *
     * @param duration heartbeat timeout.
     * @return the builder reference
     */
    public EventStoreBuilder heartbeatTimeout(Duration duration) {
        settingsBuilder.heartbeatTimeout(duration);
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
    public EventStoreBuilder requireMaster(boolean requireMaster) {
        settingsBuilder.requireMaster(requireMaster);
        return this;
    }

    /**
     * Sets the default user credentials to be used for operations.
     * If user credentials are not given for an operation, these credentials will be used.
     *
     * @param userCredentials user credentials.
     * @return the builder reference
     */
    public EventStoreBuilder userCredentials(UserCredentials userCredentials) {
        settingsBuilder.userCredentials(userCredentials);
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
     * @see #userCredentials(UserCredentials)
     */
    public EventStoreBuilder noUserCredentials() {
        settingsBuilder.noUserCredentials();
        return this;
    }

    /**
     * Sets the amount of time before an operation is considered to have timed out (by default, 7 seconds).
     *
     * @param duration the amount of time before an operation is considered to have timed out.
     * @return the builder reference
     */
    public EventStoreBuilder operationTimeout(Duration duration) {
        settingsBuilder.operationTimeout(duration);
        return this;
    }

    /**
     * Sets the amount of time that timeouts are checked in the system (by default, 1 second).
     *
     * @param duration the amount of time that timeouts are checked in the system.
     * @return the builder reference
     */
    public EventStoreBuilder operationTimeoutCheckInterval(Duration duration) {
        settingsBuilder.operationTimeoutCheckInterval(duration);
        return this;
    }

    /**
     * Sets the maximum number of outstanding items allowed in the operation queue (by default, 5000 items).
     *
     * @param size the maximum number of outstanding items allowed in the operation queue.
     * @return the builder reference
     */
    public EventStoreBuilder maxOperationQueueSize(int size) {
        settingsBuilder.maxOperationQueueSize(size);
        return this;
    }

    /**
     * Sets the maximum number of allowed asynchronous operations to be in process (by default, 5000 operations).
     *
     * @param count the maximum number of allowed asynchronous operations to be in process.
     * @return the builder reference
     */
    public EventStoreBuilder maxConcurrentOperations(int count) {
        settingsBuilder.maxConcurrentOperations(count);
        return this;
    }

    /**
     * Sets the maximum number of operation retry attempts (by default, 10 attempts).
     * When the specified number of retries for an operation is reached, then operation completes
     * exceptionally with cause {@link RetriesLimitReachedException}.
     *
     * @param count the maximum number of operation retry attempts (use {@code -1} for unlimited).
     * @return the builder reference
     */
    public EventStoreBuilder maxOperationRetries(int count) {
        settingsBuilder.maxOperationRetries(count);
        return this;
    }

    /**
     * Sets the default buffer size to use for the persistent subscription (by default, 10 messages).
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
     * By default, it is enabled.
     *
     * @param persistentSubscriptionAutoAck {@code true} to enable auto-acknowledge.
     * @return the builder reference
     */
    public EventStoreBuilder persistentSubscriptionAutoAck(boolean persistentSubscriptionAutoAck) {
        settingsBuilder.persistentSubscriptionAutoAck(persistentSubscriptionAutoAck);
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
    public EventStoreBuilder failOnNoServerResponse(boolean failOnNoServerResponse) {
        settingsBuilder.failOnNoServerResponse(failOnNoServerResponse);
        return this;
    }

    /**
     * Sets whether or not to disconnect the client on detecting a channel error. By default, it is disabled and the client
     * tries to reconnect according to {@link #maxReconnections(int)}. If it is enabled the client disconnects immediately.
     *
     * @param disconnectOnTcpChannelError {@code true} to disconnect or {@code false} to try to reconnect.
     * @return the builder reference
     */
    public EventStoreBuilder disconnectOnTcpChannelError(boolean disconnectOnTcpChannelError) {
        settingsBuilder.disconnectOnTcpChannelError(disconnectOnTcpChannelError);
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
     * Sets default executor to execute client internal tasks (such as establish-connection, start-operation) and run subscriptions.
     *
     * @return the builder reference
     * @see #executor(Executor)
     */
    public EventStoreBuilder defaultExecutor() {
        this.settingsBuilder.defaultExecutor();
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

        return new EventStoreTcp(settingsBuilder.build());
    }

}
