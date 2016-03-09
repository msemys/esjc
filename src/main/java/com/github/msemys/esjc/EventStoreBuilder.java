package com.github.msemys.esjc;

import com.github.msemys.esjc.node.cluster.ClusterNodeSettings;
import com.github.msemys.esjc.node.cluster.GossipSeed;
import com.github.msemys.esjc.node.static_.StaticNodeSettings;
import com.github.msemys.esjc.ssl.SslSettings;
import com.github.msemys.esjc.tcp.TcpSettings;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executor;

import static com.github.msemys.esjc.util.Preconditions.checkArgument;
import static com.github.msemys.esjc.util.Strings.isNullOrEmpty;
import static java.util.stream.Collectors.toList;

/**
 * Event Store client builder.
 */
public class EventStoreBuilder {
    private final Settings.Builder settingsBuilder = Settings.newBuilder();
    private final TcpSettings.Builder tcpSettingsBuilder = TcpSettings.newBuilder();
    private InetSocketAddress singleNodeAddress;
    private Duration clusterNodeGossipTimeout;
    private Duration clusterNodeDiscoverAttemptInterval;
    private Integer clusterNodeMaxDiscoverAttempts;
    private String clusterNodeDiscoveryFromDns;
    private Integer clusterNodeDiscoveryFromDnsOnGosipPort;
    private List<GossipSeed> clusterNodeDiscoveryFromGossipSeeds;

    private EventStoreBuilder() {
    }

    /**
     * Creates a new Event Store client builder.
     *
     * @return Event Store client builder.
     */
    public static EventStoreBuilder newBuilder() {
        return new EventStoreBuilder();
    }

    /**
     * Sets connection establishment timeout.
     *
     * @param duration connection establishment timeout.
     * @return the builder reference
     */
    public EventStoreBuilder tcpConnectTimeout(Duration duration) {
        tcpSettingsBuilder.connectTimeout(duration);
        return this;
    }

    /**
     * Sets connection closing timeout.
     *
     * @param duration connection closing timeout.
     * @return the builder reference
     */
    public EventStoreBuilder tcpCloseTimeout(Duration duration) {
        tcpSettingsBuilder.closeTimeout(duration);
        return this;
    }

    /**
     * Enables socket keep-alive option.
     *
     * @return the builder reference
     */
    public EventStoreBuilder tcpKeepAliveEnabled() {
        tcpSettingsBuilder.keepAlive(true);
        return this;
    }

    /**
     * Disables socket keep-alive option.
     *
     * @return the builder reference
     */
    public EventStoreBuilder tcpKeepAliveDisabled() {
        tcpSettingsBuilder.keepAlive(false);
        return this;
    }

    /**
     * Enables socket no-delay option (Nagle's algorithm will not be used).
     *
     * @return the builder reference
     */
    public EventStoreBuilder tcpNoDelayEnabled() {
        tcpSettingsBuilder.tcpNoDelay(true);
        return this;
    }

    /**
     * Disables socket no-delay option (Nagle's algorithm will be used).
     *
     * @return the builder reference
     */
    public EventStoreBuilder tcpNoDelayDisabled() {
        tcpSettingsBuilder.tcpNoDelay(false);
        return this;
    }

    /**
     * Sets the maximum socket send buffer in bytes.
     *
     * @param size the maximum socket send buffer in bytes.
     * @return the builder reference
     */
    public EventStoreBuilder tcpSendBufferSize(int size) {
        tcpSettingsBuilder.sendBufferSize(size);
        return this;
    }

    /**
     * Sets the maximum socket receive buffer in bytes.
     *
     * @param size the maximum socket receive buffer in bytes.
     * @return the builder reference
     */
    public EventStoreBuilder tcpReceiveBufferSize(int size) {
        tcpSettingsBuilder.receiveBufferSize(size);
        return this;
    }

    /**
     * Sets write buffer high watermark in bytes.
     *
     * @param size write buffer high watermark in bytes.
     * @return the builder reference
     */
    public EventStoreBuilder tcpWriteBufferHighWaterMark(int size) {
        tcpSettingsBuilder.writeBufferHighWaterMark(size);
        return this;
    }

    /**
     * Sets write buffer low watermark in bytes.
     *
     * @param size write buffer low watermark in bytes.
     * @return the builder reference
     */
    public EventStoreBuilder tcpWriteBufferLowWaterMark(int size) {
        tcpSettingsBuilder.writeBufferLowWaterMark(size);
        return this;
    }

    /**
     * Sets single-node server address.
     *
     * @param address the server address.
     * @return the builder reference
     */
    public EventStoreBuilder singleNodeAddress(InetSocketAddress address) {
        singleNodeAddress = address;
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
        singleNodeAddress = new InetSocketAddress(host, port);
        return this;
    }

    /**
     * Sets the period after which gossip times out if none is received.
     *
     * @param duration the period after which gossip times out if none is received.
     * @return the builder reference
     */
    public EventStoreBuilder clusterNodeGossipTimeout(Duration duration) {
        clusterNodeGossipTimeout = duration;
        return this;
    }

    /**
     * Sets the interval between discovering endpoint attempts.
     *
     * @param duration the interval between discovering endpoint attempts.
     * @return the builder reference
     */
    public EventStoreBuilder clusterNodeDiscoverAttemptInterval(Duration duration) {
        clusterNodeDiscoverAttemptInterval = duration;
        return this;
    }

    /**
     * Sets the maximum number of attempts for discovery.
     *
     * @param count the maximum number of attempts for discovery.
     * @return the builder reference
     * @see #clusterNodeUnlimitedDiscoverAttempts()
     */
    public EventStoreBuilder clusterNodeMaxDiscoverAttempts(int count) {
        clusterNodeMaxDiscoverAttempts = count;
        return this;
    }

    /**
     * Sets the unlimited number of attempts for discovery.
     *
     * @return the builder reference
     */
    public EventStoreBuilder clusterNodeUnlimitedDiscoverAttempts() {
        clusterNodeMaxDiscoverAttempts = -1;
        return this;
    }

    /**
     * Sets the DNS name under which cluster nodes are listed.
     *
     * @param dns the DNS name under which cluster nodes are listed.
     * @return the builder reference
     */
    public EventStoreBuilder clusterNodeDiscoveryFromDns(String dns) {
        clusterNodeDiscoveryFromDns = dns;
        return this;
    }

    /**
     * Sets the well-known port on which the cluster gossip is taking place.
     * <p>
     * If you are using the commercial edition of Event Store HA, with Manager nodes in
     * place, this should be the port number of the External HTTP port on which the
     * managers are running.
     * </p>
     * <p>
     * If you are using the open source edition of Event Store HA, this should be the
     * External HTTP port that the nodes are running on. If you cannot use a well-known
     * port for this across all nodes, you can instead use gossip seed discovery and set
     * the endpoint of some seed nodes instead.
     * </p>
     *
     * @param port the cluster gossip port.
     * @return the builder reference
     */
    public EventStoreBuilder clusterNodeDiscoveryFromDnsOnGosipPort(int port) {
        clusterNodeDiscoveryFromDnsOnGosipPort = port;
        return this;
    }

    /**
     * Sets gossip seed endpoints for the client.
     * <p>
     * Note that this should be the external HTTP endpoint of the server, as it is required
     * for the client to exchange gossip with the server. The standard port which should be
     * used here is 2113.
     * </p>
     *
     * @param endpoints the endpoints of nodes from which to seed gossip.
     * @return the builder reference
     */
    public EventStoreBuilder clusterNodeDiscoveryFromGossipSeeds(List<InetSocketAddress> endpoints) {
        clusterNodeDiscoveryFromGossipSeeds = endpoints.stream().map(GossipSeed::new).collect(toList());
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
    public EventStoreBuilder useSslConnectionWithAnyCertificate() {
        settingsBuilder.sslSettings(SslSettings.trustAllCertificates());
        return this;
    }

    /**
     * Sets the amount of time to delay before attempting to reconnect.
     *
     * @param duration the amount of time to delay before attempting to reconnect.
     * @return the builder reference
     */
    public EventStoreBuilder clientReconnectionDelay(Duration duration) {
        settingsBuilder.reconnectionDelay(duration);
        return this;
    }

    /**
     * Sets the maximum number of times to allow for reconnection.
     *
     * @param count the maximum number of times to allow for reconnection.
     * @return the builder reference
     * @see #unlimitedClientReconnections()
     */
    public EventStoreBuilder maxClientReconnections(int count) {
        settingsBuilder.maxReconnections(count);
        return this;
    }

    /**
     * Sets the unilmited number of times to allow for reconnection.
     *
     * @return the builder reference
     */
    public EventStoreBuilder unlimitedClientReconnections() {
        settingsBuilder.maxReconnections(-1);
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
     * @param count the maximum number of operation retry attempts.
     * @return the builder reference
     * @see #unlimitedOperationRetries()
     */
    public EventStoreBuilder maxOperationRetries(int count) {
        settingsBuilder.maxOperationRetries(count);
        return this;
    }

    /**
     * Sets the unlimited number of operation retry attempts.
     *
     * @return the builder reference
     */
    public EventStoreBuilder unlimitedOperationRetries() {
        settingsBuilder.maxOperationRetries(-1);
        return this;
    }

    /**
     * Sets the maximum number of events allowed to be pushed in the catchup subscription live queue.
     *
     * @param size the maximum number of events allowed to be pushed in the catchup subscription live queue.
     * @return the builder reference
     */
    public EventStoreBuilder catchupSubscriptionMaxPushQueueSize(int size) {
        settingsBuilder.maxPushQueueSize(size);
        return this;
    }

    /**
     * Sets the batch size to use during the read phase of the catchup subscription.
     *
     * @param size the batch size to use during the read phase of the catchup subscription.
     * @return the builder reference
     */
    public EventStoreBuilder catchupSubscriptionReadBatchSize(int size) {
        settingsBuilder.readBatchSize(size);
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
        if (singleNodeAddress != null) {
            settingsBuilder.nodeSettings(StaticNodeSettings.newBuilder()
                .address(singleNodeAddress)
                .build());
        }

        if (clusterNodeDiscoveryFromGossipSeeds != null && !clusterNodeDiscoveryFromGossipSeeds.isEmpty()) {
            checkArgument(isNullOrEmpty(clusterNodeDiscoveryFromDns), "Usage of gossip-seed and DNS discoverer at once is not allowed.");

            ClusterNodeSettings.BuilderForGossipSeedDiscoverer clusterNodeSettingsBuilder = ClusterNodeSettings.forGossipSeedDiscoverer();

            clusterNodeSettingsBuilder.gossipSeeds(clusterNodeDiscoveryFromGossipSeeds);
            clusterNodeSettingsBuilder.gossipTimeout(clusterNodeGossipTimeout);
            clusterNodeSettingsBuilder.discoverAttemptInterval(clusterNodeDiscoverAttemptInterval);
            if (clusterNodeMaxDiscoverAttempts != null) {
                clusterNodeSettingsBuilder.maxDiscoverAttempts(clusterNodeMaxDiscoverAttempts);
            }

            settingsBuilder.nodeSettings(clusterNodeSettingsBuilder.build());
        }

        if (!isNullOrEmpty(clusterNodeDiscoveryFromDns)) {
            checkArgument(clusterNodeDiscoveryFromGossipSeeds == null || clusterNodeDiscoveryFromGossipSeeds.isEmpty(),
                "Usage of gossip-seed and DNS discoverer at once is not allowed.");

            ClusterNodeSettings.BuilderForDnsDiscoverer clusterNodeSettingsBuilder = ClusterNodeSettings.forDnsDiscoverer();

            clusterNodeSettingsBuilder.clusterDns(clusterNodeDiscoveryFromDns);
            if (clusterNodeDiscoveryFromDnsOnGosipPort != null) {
                clusterNodeSettingsBuilder.externalGossipPort(clusterNodeDiscoveryFromDnsOnGosipPort);
            }
            clusterNodeSettingsBuilder.gossipTimeout(clusterNodeGossipTimeout);
            clusterNodeSettingsBuilder.discoverAttemptInterval(clusterNodeDiscoverAttemptInterval);
            if (clusterNodeMaxDiscoverAttempts != null) {
                clusterNodeSettingsBuilder.maxDiscoverAttempts(clusterNodeMaxDiscoverAttempts);
            }

            settingsBuilder.nodeSettings(clusterNodeSettingsBuilder.build());
        }

        settingsBuilder.tcpSettings(tcpSettingsBuilder.build());

        return new EventStore(settingsBuilder.build());
    }

}
